package xyz.kejiyu.hongguo.hooks

import java.io.File
import java.util.zip.ZipFile

/**
 * 运行时 dex 方法表索引器（纯字节解析，**不做 Class.forName**）。
 *
 * 动机：混淆类名每次发版都会整体重排，但 R8 keep 的方法名（SDK 接口方法，例如
 * `onPlaybackStateChanged` / `onDoubleTap` / `setPlaySpeed`）跨版本稳定。
 * 以「方法名 + 签名」为锚点反查宿主类，可让映射表摆脱对混淆类名的依赖。
 *
 * 实现要点：
 * - 只解析 dex 头部四张表（string_ids / type_ids / proto_ids / method_ids）
 *   与 112 字节头部，**不解析 class_defs**。`method_id` 自带 `class_idx`，
 *   直接给出宿主类描述符，因此无需加载任何类、无类初始化副作用。
 * - 方法名字符串用「长度预筛」再解码：目标名只有个位数，先比对 uleb128 长度，
 *   长度不符直接跳过，避免对 100 万+ 方法名做 MUTF-8 解码。
 * - 类型统一归一为**简单名**（`com.ss.ttvideoengine.Resolution` → `Resolution`），
 *   使签名比对不受包路径变化影响。
 */
object DexMethodIndex {

    private const val DEX_MAGIC = 0x0A786564 // "dex\n" 小端读取结果
    private const val HEADER_SIZE = 112

    class Ref(val cls: String, val name: String, val params: List<String>, val ret: String) {
        /** 例如 `onDoubleTap(MotionEvent)boolean` */
        val shape: String get() = name + "(" + params.joinToString("") + ")" + ret
    }

    class ScanResult(
        val refs: List<Ref>,
        val dexCount: Int,
        val truncated: Boolean,
        val costMs: Long,
    )

    private class Len(val v: Int, val next: Int)

    private fun u2(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    private fun u4(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or
            ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or
            ((b[o + 3].toInt() and 0xFF) shl 24)

    private fun uleb128(b: ByteArray, off: Int): Len {
        var result = 0
        var shift = 0
        var i = off
        while (i < b.size) {
            val cur = b[i].toInt() and 0xFF
            result = result or ((cur and 0x7F) shl shift)
            i++
            if ((cur and 0x80) == 0) break
            shift += 7
            if (shift > 28) break
        }
        return Len(result, i)
    }

    /** 读取 string_id[idx] 指向的 MUTF-8 字符串长度（不解码内容） */
    private fun stringLen(b: ByteArray, stringIdsOff: Int, idx: Int): Int {
        val dataOff = u4(b, stringIdsOff + idx * 4)
        if (dataOff <= 0 || dataOff >= b.size) return -1
        return uleb128(b, dataOff).v
    }

    /** 完整解码 string_id[idx]（仅对通过长度预筛的少数候选调用） */
    private fun readString(b: ByteArray, stringIdsOff: Int, idx: Int): String {
        val dataOff = u4(b, stringIdsOff + idx * 4)
        if (dataOff <= 0 || dataOff >= b.size) return ""
        val len = uleb128(b, dataOff)
        val total = len.v
        val sb = StringBuilder(total)
        var i = len.next
        var produced = 0
        while (produced < total && i < b.size) {
            val a = b[i].toInt() and 0xFF
            when {
                a < 0x80 -> {
                    sb.append(a.toChar()); i += 1; produced += 1
                }
                (a and 0xE0) == 0xC0 -> {
                    if (i + 1 >= b.size) break
                    sb.append((((a and 0x1F) shl 6) or (b[i + 1].toInt() and 0x3F)).toChar())
                    i += 2; produced += 1
                }
                else -> {
                    if (i + 2 >= b.size) break
                    sb.append(
                        (((a and 0x0F) shl 12) or
                            ((b[i + 1].toInt() and 0x3F) shl 6) or
                            (b[i + 2].toInt() and 0x3F)).toChar(),
                    )
                    i += 3; produced += 1
                }
            }
        }
        return sb.toString()
    }

    /** `Lcom/ss/ttvideoengine/Resolution;` → `Resolution`；`[I` → `int[]`；`I` → `int` */
    private fun simpleType(desc: String): String {
        if (desc.isEmpty()) return desc
        if (desc[0] == '[') return simpleType(desc.substring(1)) + "[]"
        if (desc[0] == 'L' && desc.endsWith(";")) {
            val full = desc.substring(1, desc.length - 1).replace('/', '.')
            return full.substring(full.lastIndexOf('.') + 1)
        }
        return when (desc) {
            "V" -> "void"; "Z" -> "boolean"; "B" -> "byte"; "S" -> "short"
            "C" -> "char"; "I" -> "int"; "J" -> "long"; "F" -> "float"; "D" -> "double"
            else -> desc
        }
    }

    /**
     * 同上，但**保留完整包名** —— 用于宿主类名。
     * 类名绝不能简化：`pz4.w` 简化后只剩 `w`，既无法 Class.forName，
     * 也会让「前缀偏好」排序失效。
     */
    private fun fullType(desc: String): String {
        if (desc.isEmpty()) return desc
        if (desc[0] == '[') return fullType(desc.substring(1)) + "[]"
        if (desc[0] == 'L' && desc.endsWith(";")) return desc.substring(1, desc.length - 1).replace('/', '.')
        return simpleType(desc)
    }

    /** 参数 / 返回类型：归一为简单名，避免包路径变化导致签名比对失败 */
    private fun typeAt(b: ByteArray, stringIdsOff: Int, typeIdsOff: Int, idx: Int): String {
        if (idx < 0) return "?"
        return simpleType(readString(b, stringIdsOff, u4(b, typeIdsOff + idx * 4)))
    }

    /** 宿主类名：保留完整包名 */
    private fun classAt(b: ByteArray, stringIdsOff: Int, typeIdsOff: Int, idx: Int): String {
        if (idx < 0) return "?"
        return fullType(readString(b, stringIdsOff, u4(b, typeIdsOff + idx * 4)))
    }

    /** 读取并缓存某个 proto 的「参数+返回」形状，例如 `(booleanfloatboolean)void` */
    private fun protoShapeOf(
        b: ByteArray,
        stringIdsOff: Int,
        typeIdsOff: Int,
        protoIdsOff: Int,
        protoIdx: Int,
    ): String {
        val pOff = protoIdsOff + protoIdx * 12
        if (pOff + 12 > b.size) return ""
        val ret = typeAt(b, stringIdsOff, typeIdsOff, u4(b, pOff + 4))
        val paramsOff = u4(b, pOff + 8)
        val params: List<String> = if (paramsOff <= 0) {
            emptyList()
        } else {
            val n = u4(b, paramsOff)
            if (n <= 0 || n > 64) emptyList() else
                (0 until n).map { typeAt(b, stringIdsOff, typeIdsOff, u2(b, paramsOff + 4 + it * 2)) }
        }
        return "(" + params.joinToString("") + ")" + ret
    }

    /**
     * 解析单个 dex 的 method_id 表。
     *
     * 收录条件（满足其一）：
     *  - 方法名命中 [wantNames]（按名锚定）
     *  - 参数+返回形状命中 [wantShapes]（**与名字无关**，用于「任意方法名，只要签名匹配」
     *    这类锚点，例如倍速控制器的 `(boolean,float,boolean)` setter）
     *
     * 方法名先做长度预筛再解码，形状经 proto 缓存复用，避免对百万级方法名做 MUTF-8 解码。
     */
    private fun parseDex(
        b: ByteArray,
        wantLen: Set<Int>,
        wantNames: Set<String>,
        wantShapes: Set<String>,
        out: MutableList<Ref>,
    ) {
        if (b.size < HEADER_SIZE) return
        if (u4(b, 0) != DEX_MAGIC) return

        val stringIdsOff = u4(b, 60)
        val typeIdsOff = u4(b, 68)
        val protoIdsOff = u4(b, 76)
        val methodIdsSize = u4(b, 88)
        val methodIdsOff = u4(b, 92)
        if (methodIdsSize <= 0 || methodIdsOff <= 0) return

        val protoCache = HashMap<Int, String>()

        for (i in 0 until methodIdsSize) {
            val off = methodIdsOff + i * 8
            if (off + 8 > b.size) break

            val protoIdx = u2(b, off + 2)
            val shape = protoCache.getOrPut(protoIdx) {
                protoShapeOf(b, stringIdsOff, typeIdsOff, protoIdsOff, protoIdx)
            }
            val shapeHit = wantShapes.isNotEmpty() && shape in wantShapes

            val nameIdx = u4(b, off + 4)
            val len = stringLen(b, stringIdsOff, nameIdx)
            val lenOk = len >= 0 && len in wantLen
            if (!shapeHit && !lenOk) continue

            var name = ""
            if (lenOk) {
                val n = readString(b, stringIdsOff, nameIdx)
                if (n in wantNames) name = n
            }
            if (name.isEmpty() && !shapeHit) continue

            val paramsOff = protoIdsOff + protoIdx * 12
            val ret = typeAt(b, stringIdsOff, typeIdsOff, u4(b, paramsOff + 4))
            val paramsListOff = u4(b, paramsOff + 8)
            val params: List<String> = if (paramsListOff <= 0) {
                emptyList()
            } else {
                val n = u4(b, paramsListOff)
                if (n <= 0 || n > 64) emptyList() else
                    (0 until n).map { typeAt(b, stringIdsOff, typeIdsOff, u2(b, paramsListOff + 4 + it * 2)) }
            }
            out += Ref(classAt(b, stringIdsOff, typeIdsOff, u2(b, off)), name, params, ret)
        }
    }

    private fun dexIndexOf(entryName: String): Int {
        val digits = entryName.removePrefix("classes").removeSuffix(".dex")
        return digits.toIntOrNull() ?: 1
    }

    /**
     * 扫描 APK（可含 split）内的 classes*.dex，收集名字命中 [wantNames] 的方法引用。
     *
     * @param budgetMs 时间预算，超时即提前返回并置 truncated=true
     */
    fun scan(
        apkPaths: List<String>,
        wantNames: Set<String>,
        wantShapes: Set<String>,
        budgetMs: Long,
    ): ScanResult {
        val started = System.currentTimeMillis()
        val deadline = started + budgetMs
        val wantLen = wantNames.map { it.length }.toSet()
        val out = ArrayList<Ref>()
        var dexCount = 0
        var truncated = false
        var stop = false

        for (apk in apkPaths) {
            if (stop) break
            if (apk.isNullOrBlank()) continue
            val f = File(apk)
            if (!f.isFile) continue
            try {
                ZipFile(f).use { zip ->
                    val entries = zip.entries().asSequence()
                        .filter { !it.isDirectory && it.name.startsWith("classes") && it.name.endsWith(".dex") }
                        .sortedBy { dexIndexOf(it.name) }
                        .toList()
                    for (e in entries) {
                        if (System.currentTimeMillis() > deadline) {
                            truncated = true
                            stop = true
                            break
                        }
                        try {
                            val bytes = zip.getInputStream(e).use { it.readBytes() }
                            parseDex(bytes, wantLen, wantNames, wantShapes, out)
                            dexCount++
                        } catch (_: Throwable) {
                            // 单个 dex 读取/解析失败不影响其它 dex
                        }
                    }
                }
            } catch (_: Throwable) {
                // APK 打不开（权限/损坏）→ 跳过，交给调用方回退
            }
        }

        return ScanResult(out, dexCount, truncated, System.currentTimeMillis() - started)
    }
}

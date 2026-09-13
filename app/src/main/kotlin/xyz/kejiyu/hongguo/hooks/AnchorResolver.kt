package xyz.kejiyu.hongguo.hooks

import android.content.Context
import java.io.File

/**
 * 方法名锚点解析器。
 *
 * 把「存混淆类名」的映射表升级为「以 R8 keep 的方法名 + 签名为锚，运行时反查宿主类」。
 * 混淆类名每次发版整体重排，但 SDK 接口方法名（`onPlaybackStateChanged(TTVideoEngine,int)`、
 * `onDoubleTap(MotionEvent)`、`setPlaySpeed(int)` 等）被 R8 强制保留，因此可作为稳定锚点。
 *
 * 三级解析策略（成本从低到高，命中即止）：
 *  1. 缓存：`<日志目录>/anchors-<版本>.txt`，内容为该版本已解析出的锚点类名，
 *     读取时逐个校验类仍存在（版本升级后缓存 key 变化，不会误用旧结果）。
 *  2. 直接扫描：用 [DexMethodIndex] 解析目标 APK 的方法表，按锚点签名反查宿主类。
 *  3. 失败即返回空，调用方保留表内原值并打日志，不阻断 hook 安装。
 *
 * 整个过程不做 Class.forName 之外的重活，且只在**表内候选全部失效**时才被调用，
 * 因此对已适配版本零开销。
 */
object AnchorResolver {

    /** 一个方法签名要求；[name] 为 null 表示「任意方法名」，只要参数/返回类型匹配 */
    class MethodSpec(val name: String?, val params: List<String>, val ret: String?) {
        /** 与方法名无关的「参数+返回」形状，例如 `(booleanfloatboolean)void` */
        val shape: String get() = "(" + params.joinToString("") + ")" + (ret ?: "")

        fun matches(r: DexMethodIndex.Ref): Boolean =
            (name == null || r.name == name) &&
                r.params == params &&
                (ret == null || r.ret == ret)

        override fun toString(): String = (name ?: "*") + "(" + params.joinToString(",") + ")" + (ret ?: "*")
    }

    class Anchor(
        val id: String,
        val requires: List<MethodSpec>,
        /** 同名候选的排序偏好（例如短剧包优先） */
        val preferredPrefixes: List<String> = emptyList(),
        /** 结果为内部类时，取外部类（`A$d` → `A`） */
        val outerClass: Boolean = false,
        /** 排除内部类：签名相似但宿主是 Dialog/Fragment 内部类的假阳性较多时启用 */
        val excludeInner: Boolean = false,
    )

    class Resolution(
        /** 锚点 id → 候选宿主类（已按偏好排序） */
        val byId: Map<String, List<String>>,
        val source: String,
        val costMs: Long,
        val truncated: Boolean,
    ) {
        fun first(id: String): String? = byId[id]?.firstOrNull()
        fun all(id: String): List<String> = byId[id].orEmpty()
    }

    // ── 锚点定义 ──────────────────────────────────────────────────────────
    // 全部来源于 R8 keep 的 SDK 接口方法名，已在 7.3.7.32 上逐一验算唯一性：
    //   player      → pz4.w       （全 APK 唯一命中）
    //   doubleTap   → fullscreen.i$d
    //   holder      → fullscreen.i
    //   floatPlayer → autoplay.o  （全 APK 唯一命中）
    private val PLAYER = Anchor(
        id = "player",
        requires = listOf(
            MethodSpec("onPlaybackStateChanged", listOf("TTVideoEngine", "int"), "void"),
            MethodSpec("onVideoStreamBitrateChanged", listOf("Resolution", "int"), "void"),
            MethodSpec("setPlaySpeed", listOf("int"), "void"),
        ),
    )

    private val DOUBLE_TAP = Anchor(
        id = "doubleTap",
        requires = listOf(MethodSpec("onDoubleTap", listOf("MotionEvent"), "boolean")),
        preferredPrefixes = listOf("com.dragon.read.component.shortvideo"),
    )

    private val HOLDER = Anchor(
        id = "holder",
        requires = DOUBLE_TAP.requires,
        preferredPrefixes = listOf("com.dragon.read.component.shortvideo"),
        outerClass = true,
    )

    private val SPEED_CONTROLLER = Anchor(
        id = "speedController",
        requires = listOf(
            MethodSpec("getCurrentPlaySpeed", emptyList(), "int"),
            MethodSpec(null, listOf("boolean", "float", "boolean"), "void"),
            MethodSpec(null, listOf("String"), "float"),
        ),
        preferredPrefixes = listOf("com.dragon.read.component.shortvideo"),
        // 该签名组合会连带命中若干 Dialog/Fragment 的内部类（假阳性），排除之
        excludeInner = true,
    )

    private val FLOAT_PLAYER = Anchor(
        id = "floatPlayer",
        requires = listOf(
            MethodSpec("setSpeed", listOf("float"), "void"),
            MethodSpec("getTTVideoEngine", emptyList(), "TTVideoEngine"),
        ),
        preferredPrefixes = listOf("com.dragon.read.component.shortvideo"),
    )

    private val PERCENT_PLAYER = Anchor(
        id = "percentPlayer",
        requires = listOf(
            MethodSpec("setPlaySpeed", listOf("int"), "void"),
            MethodSpec("getResolution", emptyList(), "Resolution"),
        ),
    )

    val ALL: List<Anchor> = listOf(
        PLAYER, DOUBLE_TAP, HOLDER, SPEED_CONTROLLER, FLOAT_PLAYER, PERCENT_PLAYER,
    )

    // ── 对外接口 ──────────────────────────────────────────────────────────

    /** 目标 App 的 APK 路径（含 split），用于定位 classes*.dex；依次尝试候选 Context */
    fun apkPaths(contexts: List<Context>, pkg: String): List<String> {
        contexts.forEach { ctx ->
            try {
                val ai = ctx.packageManager.getPackageInfo(pkg, 0).applicationInfo ?: return@forEach
                val paths = buildList {
                    ai.sourceDir?.let { add(it) }
                    ai.splitSourceDirs?.forEach { if (!it.isNullOrBlank()) add(it) }
                }
                if (paths.isNotEmpty()) return paths
            } catch (_: Throwable) {
            }
        }
        return emptyList()
    }

    fun resolve(
        apkPaths: List<String>,
        classLoader: ClassLoader?,
        cacheFile: File?,
        anchors: List<Anchor> = ALL,
        budgetMs: Long = 6000L,
    ): Resolution {
        if (apkPaths.isEmpty()) return Resolution(emptyMap(), "no-apk", 0L, false)

        val cacheKey = anchors.joinToString("|") { it.id }

        // 1) 缓存
        if (cacheFile != null) {
            try {
                val cached = readCache(cacheFile)
                if (cached != null && cached.first == cacheKey && cached.second.isNotEmpty()) {
                    val alive = cached.second.values.flatten().all { exists(it, classLoader) }
                    if (alive) return Resolution(cached.second, "cache", 0L, false)
                }
            } catch (_: Throwable) {
            }
        }

        // 2) 直接扫描目标 APK 的方法表
        val specs = anchors.flatMap { it.requires }
        val wantNames = specs.mapNotNull { it.name }.toSet()
        val wantShapes = specs.filter { it.name == null }.map { it.shape }.toSet()
        if (wantNames.isEmpty() && wantShapes.isEmpty()) return Resolution(emptyMap(), "no-anchor", 0L, false)

        val scan = try {
            DexMethodIndex.scan(apkPaths, wantNames, wantShapes, budgetMs)
        } catch (_: Throwable) {
            return Resolution(emptyMap(), "scan-error", 0L, false)
        }

        val byClass: Map<String, List<DexMethodIndex.Ref>> = scan.refs.groupBy { it.cls }
        val out = LinkedHashMap<String, List<String>>()
        anchors.forEach { anchor ->
            val base = byClass
                .filterValues { refs -> anchor.requires.all { spec -> refs.any(spec::matches) } }
                .keys
                .filter { !anchor.excludeInner || !it.contains('$') }

            // 指定了包名偏好时，前缀不匹配即视为**未解析**。
            // 这类锚点（如 onDoubleTap）全 APK 有 30+ 个假阳性宿主，若允许跨包兜底，
            // 一旦目标类消失就会取到 GestureDetector 这类无关类，比留空更危险。
            val scoped = if (anchor.preferredPrefixes.isEmpty()) {
                base
            } else {
                base.filter { cls -> anchor.preferredPrefixes.any { cls.startsWith(it) } }
            }

            val hits = scoped
                .map { if (anchor.outerClass) outerOf(it) else it }
                .distinct()
                .sorted()
            if (hits.isNotEmpty()) out[anchor.id] = hits
        }

        val result = Resolution(out, "scan(${scan.dexCount}dex)", scan.costMs, scan.truncated)
        if (cacheFile != null && out.isNotEmpty()) writeCache(cacheFile, cacheKey, out)
        return result
    }

    // ── 内部工具 ──────────────────────────────────────────────────────────

    private fun exists(cls: String, classLoader: ClassLoader?): Boolean {
        if (cls.isBlank()) return false
        return try {
            Class.forName(cls, false, classLoader)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun outerOf(cls: String): String {
        val i = cls.lastIndexOf('$')
        return if (i > 0) cls.substring(0, i) else cls
    }

    private fun readCache(file: File): Pair<String, Map<String, List<String>>>? {
        if (!file.isFile) return null
        val lines = file.readLines()
        if (lines.isEmpty()) return null
        val key = lines.first()
        val map = LinkedHashMap<String, List<String>>()
        lines.drop(1).forEach { line ->
            if (line.isBlank()) return@forEach
            val parts = line.split('\t')
            if (parts.size >= 2) {
                val cls = parts[1].split(',').map { it.trim() }.filter { it.isNotBlank() }
                if (cls.isNotEmpty()) map[parts[0]] = cls
            }
        }
        return key to map
    }

    private fun writeCache(file: File, key: String, map: Map<String, List<String>>) {
        try {
            file.parentFile?.mkdirs()
            val sb = StringBuilder()
            sb.append(key).append('\n')
            map.forEach { (id, cls) -> sb.append(id).append('\t').append(cls.joinToString(",")).append('\n') }
            file.writeText(sb.toString())
        } catch (_: Throwable) {
        }
    }
}

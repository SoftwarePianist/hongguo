package xyz.kejiyu.hongguo

import android.util.Log
import android.os.Process
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

object LogUtil {

    private const val LOG_TAG = "ZongHe"
    private const val LOG_ROOT = "/storage/emulated/0/Android/media"
    private const val LOG_DIR_NAME = "K_红果logs"
    private const val LOG_RETENTION_MS = 24L * 60L * 60L * 1000L

    private var logFile: File? = null
    private var currentLogDir: String = "$LOG_ROOT/xyz.kejiyu.hongguo/$LOG_DIR_NAME"
    @Volatile private var writer: FileWriter? = null
    @Volatile private var initialized = false

    private val logQueue = LinkedBlockingQueue<String>(2048)
    private val counters = ConcurrentHashMap<String, AtomicInteger>()
    @Volatile private var lastDiagDump = 0L

    init {
        val worker = Thread({
            while (true) {
                try {
                    val line = logQueue.take()
                    val w = writer
                    if (w != null) {
                        w.write(line)
                        w.write("\n")
                        while (true) {
                            val next = logQueue.poll() ?: break
                            w.write(next)
                            w.write("\n")
                        }
                        w.flush()
                    }
                } catch (_: InterruptedException) {
                    break
                } catch (_: Throwable) {}
            }
        }, "LogUtil-Worker")
        worker.isDaemon = true
        worker.priority = Thread.MIN_PRIORITY
        worker.start()
    }

    /** 当前日志目录（供锚点解析缓存等旁路数据落盘复用，避免污染目标 App 私有目录） */
    fun logDir(): String = currentLogDir

    fun init(processName: String? = null) {
        if (initialized) return
        Thread({
            try {
                val processPackage = processName?.substringBefore(':')?.takeIf { it.isNotBlank() }
                val ownerPackage = when (processPackage) {
                    "com.phoenix.read", "com.phoenix.read.oversea.gp", "xyz.kejiyu.hongguo" -> processPackage
                    else -> "xyz.kejiyu.hongguo"
                }
                currentLogDir = "$LOG_ROOT/$ownerPackage/$LOG_DIR_NAME"
                val dir = File(currentLogDir)
                if (!dir.exists()) dir.mkdirs()
                val deletedOldLogs = cleanupExpiredLogs(dir)
                val safeProcess = (processName ?: "unknown")
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .takeLast(56)
                val fileFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val f = File(dir, "${fileFormat.format(Date())}_${safeProcess}_${Process.myPid()}.log")
                logFile = f
                writer = FileWriter(f, true)
                initialized = true
                info("═══════════════════════════════════")
                info("日志系统初始化 | 文件=${f.absolutePath}")
                info("日志保留=最近24小时 | 已清理过期日志=$deletedOldLogs")
                info("═══════════════════════════════════")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "日志文件初始化失败", e)
            }
        }, "LogUtil-Init").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
            start()
        }
    }

    fun info(msg: String) {
        write("INFO", msg)
        Log.i(LOG_TAG, msg)
    }

    fun warn(msg: String) {
        write("WARN", msg)
        Log.w(LOG_TAG, msg)
    }

    fun error(msg: String, t: Throwable? = null) {
        val full = if (t != null) "$msg | ${t.javaClass.simpleName}: ${t.message}" else msg
        write("ERROR", full)
        Log.e(LOG_TAG, full, t)
        if (t != null) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            logQueue.offer(sw.toString())
        }
    }

    fun debug(msg: String) {
        write("DEBUG", msg)
        Log.d(LOG_TAG, msg)
    }

    fun incr(key: String) {
        counters.computeIfAbsent(key) { AtomicInteger(0) }.incrementAndGet()
    }

    fun diagDump(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastDiagDump) < 10000) return
        lastDiagDump = now
        val sb = StringBuilder()
        sb.appendLine("DIAG ──────────────────────────────")
        sb.appendLine("DIAG " + counters.entries.joinToString(" | ") { "${it.key}=${it.value.get()}" })
        sb.appendLine("DIAG ──────────────────────────────")
        logQueue.offer(sb.toString())
    }

    fun flush() {
    }

    fun getFilePath(): String = logFile?.absolutePath ?: "(异步初始化中)"

    private fun cleanupExpiredLogs(dir: File): Int {
        val now = System.currentTimeMillis()
        var deleted = 0
        try {
            dir.listFiles()?.forEach { file ->
                if (!file.isFile || !file.name.endsWith(".log", ignoreCase = true)) return@forEach
                val modified = file.lastModified()
                if (modified > 0L && now - modified > LOG_RETENTION_MS) {
                    if (file.delete()) deleted++
                }
            }
        } catch (_: Throwable) {}
        return deleted
    }

    private fun write(level: String, msg: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val ts = timeFormat.format(Date())
        logQueue.offer("[$ts] [$level] $msg")
    }
}

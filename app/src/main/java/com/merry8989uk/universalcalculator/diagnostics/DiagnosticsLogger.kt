package com.merry8989uk.universalcalculator.diagnostics

import android.content.Context
import android.os.Build
import com.merry8989uk.universalcalculator.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Collects app logs into a single rolling file. Collection is ON by default
 * (Rule 7), so the app records what happened without the user having to
 * reproduce anything first.
 */
object DiagnosticsLogger {

    private const val MAX_BYTES = 1_000_000L   // rotate at ~1 MB
    private const val MAX_RING = 300           // lines kept in memory for the live box
    private const val DIR = "logs"
    private const val NAME = "app.log"
    private const val CRASH_NAME = "crashes.log"
    private const val MAX_CRASH_BYTES = 400_000L
    private const val CRUMBS = 25

    private val lock = Any()
    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private var logFile: File? = null
    private var appContext: Context? = null

    /** Recent lines, kept in memory so the app can show logs in real time. */
    private val ring = ArrayDeque<String>()

    /** Whether collection is on. Defaults to true (Rule 7). */
    @Volatile
    var enabled: Boolean = true
        private set

    /** Call once, from Application.onCreate. */
    fun init(context: Context) {
        synchronized(lock) {
            appContext = context.applicationContext
            val dir = File(context.filesDir, DIR).apply { mkdirs() }
            logFile = File(dir, NAME)
        }
    }

    fun setEnabled(value: Boolean) {
        if (value != enabled) {
            log("Diagnostics", if (value) "Log collection enabled" else "Log collection disabled")
        }
        enabled = value
    }

    fun log(tag: String, message: String) {
        if (!enabled) return
        val file = logFile ?: return
        val line = "${timeFmt.format(Date())} [$tag] $message"
        synchronized(lock) {
            runCatching {
                if (file.length() > MAX_BYTES) rotate(file)
                file.appendText(line + "\n")
            }
            ring.addLast(line)
            while (ring.size > MAX_RING) ring.removeFirst()
        }
    }

    /** Most recent log lines, oldest first — drives the live log box. */
    fun recent(): List<String> = synchronized(lock) { ring.toList() }

    fun readAll(): String = synchronized(lock) {
        val file = logFile ?: return ""
        if (file.exists()) file.readText() else ""
    }

    fun sizeBytes(): Long = synchronized(lock) {
        logFile?.takeIf { it.exists() }?.length() ?: 0L
    }

    fun clear() = synchronized(lock) {
        runCatching { logFile?.writeText("") }
        Unit
    }

    /** Wipes the log, the crash reports and the usage counters. */
    fun clearEverything(context: Context) {
        clear()
        clearCrashes()
        Analytics.reset(context)
    }

    /** Records uncaught crashes, then defers to the previous handler. */
    fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { recordCrash(thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    /**
     * A crash report: what broke, on what device, with the last few log lines as
     * breadcrumbs so the steps leading up to it are visible.
     */
    private fun recordCrash(thread: Thread, error: Throwable) {
        if (!enabled) return
        val context = appContext
        val device = context?.let {
            "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        } ?: "unknown device"

        val entry = buildString {
            appendLine("===== CRASH ${timeFmt.format(Date())} =====")
            appendLine("app    : ${BuildConfig.VERSION_NAME}")
            appendLine("device : $device")
            appendLine("thread : ${thread.name}")
            appendLine("error  : ${error.javaClass.name}: ${error.message}")
            appendLine("--- stack ---")
            appendLine(error.stackTraceToString())
            appendLine("--- last ${CRUMBS} log lines ---")
            appendLine(recent().takeLast(CRUMBS).joinToString("\n"))
            appendLine()
        }

        val file = crashFile()
        synchronized(lock) {
            runCatching {
                if (file != null) {
                    if (file.length() > MAX_CRASH_BYTES) file.writeText("")
                    file.appendText(entry)
                }
                // keep the marker in the ordinary log too
                val line = "${timeFmt.format(Date())} [CRASH] ${error.javaClass.name}: ${error.message}"
                logFile?.let { if (it.length() > MAX_BYTES) rotate(it); it.appendText(line + "\n") }
                ring.addLast(line)
                while (ring.size > MAX_RING) ring.removeFirst()
            }
        }
        context?.let { Analytics.event(it, "error:crash") }
    }

    private fun crashFile(): File? = logFile?.let { File(it.parentFile, CRASH_NAME) }

    /** How many crashes have been recorded. */
    fun crashCount(): Int {
        val text = crashReport()
        if (text.isEmpty()) return 0
        var count = 0
        var index = text.indexOf("===== CRASH")
        while (index >= 0) {
            count++
            index = text.indexOf("===== CRASH", index + 1)
        }
        return count
    }

    fun crashReport(): String = synchronized(lock) {
        val file = crashFile() ?: return ""
        if (file.exists()) file.readText() else ""
    }

    fun clearCrashes() = synchronized(lock) {
        runCatching { crashFile()?.writeText("") }
        Unit
    }

    /** Everything the diagnostics screen can export, in one text blob. */
    fun exportReport(context: Context): String = buildString {
        appendLine("Nexus Calc diagnostics report")
        appendLine("app    : ${BuildConfig.VERSION_NAME}")
        appendLine("device : ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("crashes: ${crashCount()}")
        appendLine("log    : ${sizeBytes()} bytes")
        appendLine()
        append(Analytics.report(context))
        appendLine()
        appendLine("--- crash reports ---")
        appendLine(crashReport().ifBlank { "(none)" })
        appendLine()
        appendLine("--- app log ---")
        append(readAll())
    }

    private fun rotate(file: File) {
        val old = File(file.parentFile, "$NAME.1")
        if (old.exists()) old.delete()
        file.renameTo(old)
        file.createNewFile()
    }
}

package com.merry8989uk.universalcalculator.diagnostics

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The last few hundred lines the system wrote about this app, read once at a
 * crash.
 *
 * On modern Android an app can only read its **own** process's logcat — since 4.1
 * the READ_LOGS permission is signature-level, so the promise of catching other
 * apps and system messages is not one that can be kept. Own-process output is
 * still worth having: it is where the framework and the libraries the app uses
 * explain what they were doing in the moments before the crash, and none of it is
 * in this app's own log file.
 *
 * Read once, at the crash, with a hard limit. Running logcat in the background
 * would cost battery all day for output that is only wanted when something breaks.
 */
object LogcatTail {

    private const val LINES = 300
    private const val TIMEOUT_SECONDS = 3L

    /**
     * The tail, or an empty string if it cannot be read.
     *
     * Safe to call from the crash handler: every failure is swallowed, because a
     * crash report that throws while being written is worse than a short one.
     */
    fun read(pid: Int = android.os.Process.myPid()): String = try {
        val process = ProcessBuilder(
            "logcat", "-d", "-t", LINES.toString(), "--pid=$pid",
        ).redirectErrorStream(true).start()

        val text = process.inputStream.bufferedReader().use { reader ->
            reader.readText().take(64_000)
        }
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
        }
        text.lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { Mask.apply(it) }
    } catch (e: Exception) {
        ""
    }

    /** Kept for the tests: the same read against a file, with no process involved. */
    fun maskLines(raw: String): String = raw.lines()
        .filter { it.isNotBlank() }
        .joinToString("\n") { Mask.apply(it) }

    /** Writes the tail into a file, bounded, for the crash report. */
    fun appendTo(file: File, text: String) {
        if (text.isBlank()) return
        runCatching {
            file.appendText("--- logcat (own process, last $LINES) ---\n")
            file.appendText(text.take(64_000))
            file.appendText("\n")
        }
    }
}

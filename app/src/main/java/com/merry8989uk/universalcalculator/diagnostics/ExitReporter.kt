package com.merry8989uk.universalcalculator.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build

/**
 * Picks up the exits the app could not report itself.
 *
 * An ANR (app not responding) never reaches an exception handler — Android kills
 * the process and only records it afterwards. From API 30 the system keeps a
 * short history of how this process ended, including the ANR trace, so the next
 * launch can read it and write it into the diagnostics log.
 *
 * The last timestamp seen is remembered, so each exit is reported once.
 */
object ExitReporter {

    private const val PREFS = "exit_history"
    private const val KEY_LAST = "last_timestamp"
    private const val MAX_EXITS = 12
    private const val MAX_TRACE_LINES = 120

    /** Call once at startup, after DiagnosticsLogger.init. */
    fun capture(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        if (!DiagnosticsLogger.enabled) return

        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val store = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastSeen = store.getLong(KEY_LAST, 0L)
        var newest = lastSeen

        val exits = try {
            manager.getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXITS)
        } catch (e: Exception) {
            emptyList()
        }

        exits.forEach { info ->
            val stamp = info.timestamp
            if (stamp <= lastSeen) return@forEach
            if (stamp > newest) newest = stamp

            val kind = when (info.reason) {
                ApplicationExitInfo.REASON_ANR -> "ANR"
                ApplicationExitInfo.REASON_CRASH -> "CRASH"
                ApplicationExitInfo.REASON_CRASH_NATIVE -> "NATIVE CRASH"
                ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "STARTUP FAILURE"
                ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW MEMORY KILL"
                // Swiping the app away and a clean exit are not faults.
                else -> return@forEach
            }

            DiagnosticsLogger.recordExit(
                context = context,
                kind = kind,
                timestamp = stamp,
                description = info.description.orEmpty(),
                trace = traceOf(info),
            )
        }

        store.edit().putLong(KEY_LAST, newest).apply()
    }

    /** The ANR trace, trimmed — Android writes a full thread dump. */
    private fun traceOf(info: ApplicationExitInfo): String = try {
        info.traceInputStream?.bufferedReader()?.use { reader ->
            reader.readText().lines().take(MAX_TRACE_LINES).joinToString("\n")
        }.orEmpty()
    } catch (e: Exception) {
        ""
    }
}

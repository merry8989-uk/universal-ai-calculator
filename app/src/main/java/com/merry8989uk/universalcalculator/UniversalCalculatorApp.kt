package com.merry8989uk.universalcalculator

import android.app.Application
import com.merry8989uk.universalcalculator.ai.AiClient
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import com.merry8989uk.universalcalculator.diagnostics.EventLog
import com.merry8989uk.universalcalculator.diagnostics.ExitReporter
import com.merry8989uk.universalcalculator.sound.SoundPlayer
import java.util.concurrent.Executors

/**
 * Application entry point. Starts diagnostics logging (on by default, Rule 7)
 * and captures crashes before the app is fully up.
 *
 * Nothing here touches the disk. A cold start is measured from this call to the
 * first frame the user sees, and every file-system round trip put in front of
 * that frame is a frame they spend looking at nothing: creating the log
 * directories, reading the install id out of SharedPreferences, reading the
 * system's exit history, and appending the first three log lines all used to
 * happen here. They now happen on [STARTUP], which the first frame does not
 * wait for. What stays is memory only, plus the crash handler — a crash during
 * those first milliseconds is exactly the one worth having.
 */
class UniversalCalculatorApp : Application() {

    override fun onCreate() {
        val launchStart = System.currentTimeMillis()
        super.onCreate()

        // Wiring, not writing: these only hold onto the context and the file
        // paths, so they cost nothing on the thread that is about to draw.
        DiagnosticsLogger.attach(this)
        EventLog.attach(this)
        EventLog.newSession()
        SoundPlayer.init(this)
        AiClient.init(this)
        // Installed before the worker runs, so an early crash is still recorded.
        DiagnosticsLogger.installCrashHandler()

        STARTUP.execute {
            // The directories and the ids the log files are named and stamped
            // with: both are disk reads, and neither is needed for a frame.
            DiagnosticsLogger.prepareStorage()
            EventLog.prepareStorage()
            // ANRs and native crashes never reach the handler above; the system
            // remembers them, so pick them up now.
            ExitReporter.capture(this)
            DiagnosticsLogger.log("App", "Started version ${BuildConfig.VERSION_NAME}")

            // D. Performance — how long a cold start took, and on what.
            EventLog.perf(
                "app.launch",
                System.currentTimeMillis() - launchStart,
                mapOf(
                    "app_version" to BuildConfig.VERSION_NAME,
                    "android" to android.os.Build.VERSION.RELEASE,
                    "sdk" to android.os.Build.VERSION.SDK_INT.toString(),
                    "device" to (android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL),
                    "cores" to Runtime.getRuntime().availableProcessors().toString(),
                ),
            )
            // E. Product — the app was opened at all, which is the denominator
            // for every other number.
            EventLog.product("app.opened", mapOf("app_version" to BuildConfig.VERSION_NAME))
        }
    }

    private companion object {
        /**
         * One worker, so the three writes land in the order they were made and
         * two of them cannot interleave inside a log file. Daemon, because a
         * thread that is only tidying up must never hold the process open.
         */
        private val STARTUP = Executors.newSingleThreadExecutor { work ->
            Thread(work, "startup-io").apply { isDaemon = true }
        }
    }
}

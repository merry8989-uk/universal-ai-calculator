package com.merry8989uk.universalcalculator

import android.app.Application
import com.merry8989uk.universalcalculator.ai.AiClient
import com.merry8989uk.universalcalculator.diag.DiagCategory
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import com.merry8989uk.universalcalculator.diagnostics.EventLog
import com.merry8989uk.universalcalculator.diagnostics.ExitReporter
import com.merry8989uk.universalcalculator.sound.SoundPlayer

/**
 * Application entry point. Starts diagnostics logging (on by default, Rule 7)
 * and captures crashes before the app is fully up.
 */
class UniversalCalculatorApp : Application() {

    override fun onCreate() {
        val launchStart = System.currentTimeMillis()
        super.onCreate()
        DiagnosticsLogger.init(this)
        EventLog.init(this)
        EventLog.newSession()
        SoundPlayer.init(this)
        AiClient.init(this)
        DiagnosticsLogger.installCrashHandler()
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
        // E. Product — the app was opened at all, which is the denominator for
        // every other number.
        EventLog.product("app.opened", mapOf("app_version" to BuildConfig.VERSION_NAME))
    }
}

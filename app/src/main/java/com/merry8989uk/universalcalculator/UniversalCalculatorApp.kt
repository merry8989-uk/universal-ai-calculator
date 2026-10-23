package com.merry8989uk.universalcalculator

import android.app.Application
import com.merry8989uk.universalcalculator.ai.AiClient
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import com.merry8989uk.universalcalculator.sound.SoundPlayer

/**
 * Application entry point. Starts diagnostics logging (on by default, Rule 7)
 * and captures crashes before the app is fully up.
 */
class UniversalCalculatorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DiagnosticsLogger.init(this)
        SoundPlayer.init(this)
        AiClient.init(this)
        DiagnosticsLogger.installCrashHandler()
        DiagnosticsLogger.log("App", "Started version ${BuildConfig.VERSION_NAME}")
    }
}

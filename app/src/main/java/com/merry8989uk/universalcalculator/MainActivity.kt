package com.merry8989uk.universalcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import com.merry8989uk.universalcalculator.locale.LocaleHelper
import com.merry8989uk.universalcalculator.data.DataPolicy
import com.merry8989uk.universalcalculator.settings.SettingsRepository
import com.merry8989uk.universalcalculator.settings.SettingsViewModel
import com.merry8989uk.universalcalculator.settings.ThemeMode
import com.merry8989uk.universalcalculator.ui.AppRoot
import com.merry8989uk.universalcalculator.ui.theme.AppTheme
import androidx.compose.runtime.remember
import com.merry8989uk.universalcalculator.ui.theme.ThemeStore
import com.merry8989uk.universalcalculator.ui.theme.UniversalCalculatorTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Applies the chosen language before the first view is built. */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enterImmersiveMode()

        // Apply the stored preferences before anything is logged. The master
        // switch wins: with data saving off, the log stays off whatever the
        // diagnostics switch says.
        lifecycleScope.launch {
            SettingsRepository(applicationContext).settings.collect { stored ->
                DiagnosticsLogger.setEnabled(
                    DataPolicy.mayLog(stored.dataSavingEnabled, stored.diagnosticsEnabled),
                )
            }
        }

        setContent { App() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    /** Hides the status and navigation bars; swipe reveals them transiently. */
    private fun enterImmersiveMode() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
private fun App() {
    val vm: SettingsViewModel = viewModel()
    val state by vm.state.collectAsState()

    val dark = when (state.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val base = LocalDensity.current
    val scaled = Density(base.density * state.density, base.fontScale * state.textScale)

    // A theme the user built wins over the built-in ones when one is selected.
    val customTheme = remember(state.customThemeId) {
        if (state.customThemeId.isBlank()) null
        else ThemeStore.byId(applicationContext, state.customThemeId)
    }
    UniversalCalculatorTheme(
        theme = AppTheme.byLabel(state.themeName),
        darkTheme = dark,
        custom = customTheme,
    ) {
        CompositionLocalProvider(LocalDensity provides scaled) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                AppRoot(settings = state)
            }
        }
    }
}

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
import com.merry8989uk.universalcalculator.settings.SettingsRepository
import com.merry8989uk.universalcalculator.settings.SettingsViewModel
import com.merry8989uk.universalcalculator.settings.ThemeMode
import com.merry8989uk.universalcalculator.ui.AppRoot
import com.merry8989uk.universalcalculator.ui.theme.AppTheme
import com.merry8989uk.universalcalculator.ui.theme.UniversalCalculatorTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enterImmersiveMode()

        // Apply the stored diagnostics preference (Rule 7).
        lifecycleScope.launch {
            val stored = SettingsRepository(applicationContext).settings.first()
            DiagnosticsLogger.setEnabled(stored.diagnosticsEnabled)
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

    UniversalCalculatorTheme(theme = AppTheme.byLabel(state.themeName), darkTheme = dark) {
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

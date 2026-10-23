package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.merry8989uk.universalcalculator.settings.SettingsScreen
import com.merry8989uk.universalcalculator.settings.SettingsState

private val TABS = listOf("Standard", "Scientific", "Convert", "Formulas", "Ask AI", "Settings")
private val TAB_GLYPHS = listOf("=", "\u03C0", "\u21C4", "\u0192", "\u2726", "\u2699")

private val SCIENTIFIC_KEYS = listOf(
    "sin", "cos", "tan", "asin", "acos", "atan",
    "ln", "log", "sqrt", "cbrt", "x^2", "x^y", "pi", "e", "!",
    "(", ")", "1/x", "abs", "nCr", "nPr", "round",
)

/**
 * Root composable: the six main modes. Settings and the AI assistant are one
 * tap away (Rule 4).
 */
@Composable
fun AppRoot(settings: SettingsState) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var aiContext by rememberSaveable { mutableStateOf("") }

    // "Keep screen on" — applied to the window for as long as it is switched on.
    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(TAB_GLYPHS[i]) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                0 -> CalculatorScreen(
                    title = "Standard",
                    functionKeys = emptyList(),
                    settings = settings,
                    onAskAi = { q -> aiContext = q; tab = 4 },
                )
                1 -> CalculatorScreen(
                    title = "Scientific",
                    functionKeys = SCIENTIFIC_KEYS,
                    settings = settings,
                    onAskAi = { q -> aiContext = q; tab = 4 },
                )
                2 -> ConvertScreen()
                3 -> FormulasScreen()
                4 -> AskAiScreen(
                    contextText = aiContext,
                    onClearContext = { aiContext = "" },
                )
                else -> SettingsScreen(onBack = { tab = 0 })
            }
        }
    }
}

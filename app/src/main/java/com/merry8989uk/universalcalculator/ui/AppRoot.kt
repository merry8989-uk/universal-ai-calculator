package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.merry8989uk.universalcalculator.settings.SettingsScreen
import com.merry8989uk.universalcalculator.settings.SettingsState

private val TABS = listOf("Standard", "Scientific", "Convert", "Formulas", "Settings")
private val TAB_GLYPHS = listOf("=", "π", "⇄", "ƒ", "⚙")

private val SCIENTIFIC_KEYS = listOf(
    "sin", "cos", "tan", "asin", "acos", "atan",
    "ln", "log", "sqrt", "x^2", "x^y", "pi", "e", "!",
    "(", ")", "1/x", "abs", "nCr", "nPr", "round",
)

/**
 * Root composable: the five main modes. Settings is a tab, so the Settings
 * screen is always one tap away (Rule 4).
 */
@Composable
fun AppRoot(settings: SettingsState) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

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
                )
                1 -> CalculatorScreen(
                    title = "Scientific",
                    functionKeys = SCIENTIFIC_KEYS,
                    settings = settings,
                )
                2 -> ConvertScreen()
                3 -> FormulasScreen()
                else -> SettingsScreen(onBack = { tab = 0 })
            }
        }
    }
}

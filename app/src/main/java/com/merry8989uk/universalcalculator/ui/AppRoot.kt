package com.merry8989uk.universalcalculator.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.settings.SettingsScreen
import com.merry8989uk.universalcalculator.settings.SettingsState

private val TABS = listOf("Standard", "Scientific", "Convert", "Formulas", "Ask AI", "Settings")
private val TAB_GLYPHS = listOf("=", "\u03C0", "\u21C4", "\u0192", "\u2726", "\u2699")

/** Two tabs on each side of the assistant button, so it sits dead centre. */
private val LEFT_TABS = listOf(0, 1)
private val RIGHT_TABS = listOf(2, 3)
private const val AI_TAB = 4
private const val SETTINGS_TAB = 5

/**
 * Slot weights. The leading spacer matches the settings gear on the right, so
 * the assistant lands exactly in the middle of the bar.
 */
private const val SIDE_WEIGHT = 0.7f
private const val TAB_WEIGHT = 1f
private const val AI_WEIGHT = 1.6f

private val SCIENTIFIC_KEYS = listOf(
    "sin", "cos", "tan", "asin", "acos", "atan",
    "ln", "log", "sqrt", "cbrt", "x^2", "x^y", "pi", "e", "!",
    "(", ")", "1/x", "abs", "nCr", "nPr", "round",
)

/**
 * Root composable: the six main modes. The assistant sits in the middle of the
 * bottom bar, and Settings and everything else are one tap away.
 */
@Composable
fun AppRoot(settings: SettingsState) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var aiContext by rememberSaveable { mutableStateOf("") }
    var aiAsk by rememberSaveable { mutableStateOf("") }

    // Pressing the tab you are already on takes that tab back to its own home.
    var resetTick by rememberSaveable { mutableIntStateOf(0) }
    // True while a tab is showing one of its inner screens.
    var innerOpen by remember { mutableStateOf(false) }

    fun selectTab(i: Int) {
        if (tab == i) resetTick++ else tab = i
    }

    // "Keep screen on" — applied to the window for as long as it is switched on.
    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // Back button: first it leaves an inner screen, then a second press within
    // two seconds leaves the app. One press never drops you straight out.
    val context = LocalContext.current
    var lastBack by remember { mutableLongStateOf(0L) }
    BackHandler {
        if (innerOpen) {
            resetTick++
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBack < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBack = now
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Balances the settings gear at the other end.
                Spacer(modifier = Modifier.weight(SIDE_WEIGHT))

                LEFT_TABS.forEach { i ->
                    NavItem(i, tab, TAB_GLYPHS[i], TABS[i], TAB_WEIGHT) { selectTab(i) }
                }

                // The assistant: centred, because the row is balanced either side.
                Box(modifier = Modifier.weight(AI_WEIGHT), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = "Ask AI tab"
                            role = Role.Tab
                            selected = tab == AI_TAB
                        },
                    ) {
                        MagicButton(onClick = { selectTab(AI_TAB) }, size = 56.dp)
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "Ask AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                RIGHT_TABS.forEach { i ->
                    NavItem(i, tab, TAB_GLYPHS[i], TABS[i], TAB_WEIGHT) { selectTab(i) }
                }

                // Settings is a gear at the end of the bar.
                Box(
                    modifier = Modifier.weight(SIDE_WEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            TAB_GLYPHS[SETTINGS_TAB],
                            style = MaterialTheme.typography.titleLarge,
                            color = if (tab == SETTINGS_TAB) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            TABS[SETTINGS_TAB],
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tab == SETTINGS_TAB) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                            .clickable { selectTab(SETTINGS_TAB) }
                            .clearAndSetSemantics {
                                contentDescription = "Settings tab"
                                role = Role.Tab
                                selected = tab == SETTINGS_TAB
                            },
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
                    onAskAi = { q -> aiContext = q; tab = AI_TAB },
                    onAskAiNow = { q -> aiAsk = q; aiContext = ""; tab = AI_TAB },
                )
                1 -> CalculatorScreen(
                    title = "Scientific",
                    functionKeys = SCIENTIFIC_KEYS,
                    settings = settings,
                    onAskAi = { q -> aiContext = q; tab = AI_TAB },
                    onAskAiNow = { q -> aiAsk = q; aiContext = ""; tab = AI_TAB },
                )
                2 -> ConvertScreen()
                3 -> FormulasScreen(
                    resetSignal = resetTick,
                    onInnerChange = { innerOpen = it },
                )
                AI_TAB -> AskAiScreen(
                    contextText = aiContext,
                    onClearContext = { aiContext = "" },
                    enabled = settings.aiEnabled,
                    initialQuestion = aiAsk,
                    onInitialQuestionUsed = { aiAsk = "" },
                    resetSignal = resetTick,
                )
                else -> SettingsScreen(
                    onBack = { tab = 0 },
                    resetSignal = resetTick,
                )
            }
        }
    }
}

/** One tab in the bottom bar, outlined like the keypad keys. */
@Composable
private fun RowScope.NavItem(
    index: Int,
    selectedTab: Int,
    glyph: String,
    label: String,
    weight: Float,
    onClick: () -> Unit,
) {
    val selected = selectedTab == index
    val shape = RoundedCornerShape(14.dp)
    val tint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(weight)
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent,
            )
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            // Only the name is read out, never the glyph.
            .clearAndSetSemantics {
                contentDescription = "$label tab"
                role = Role.Tab
                selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(glyph, style = MaterialTheme.typography.titleMedium, color = tint)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

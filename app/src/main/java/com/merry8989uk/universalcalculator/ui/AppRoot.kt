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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.settings.SettingsScreen
import com.merry8989uk.universalcalculator.diagnostics.Analytics
import com.merry8989uk.universalcalculator.sound.SoundPlayer
import com.merry8989uk.universalcalculator.settings.SettingsState

private val TAB_LABELS = listOf(
    R.string.tab_standard, R.string.tab_scientific, R.string.tab_convert,
    R.string.tab_formulas, R.string.tab_ask_ai, R.string.tab_settings,
    R.string.tab_sci_classic,
)
private val TAB_A11Y = listOf(
    R.string.tab_standard_a11y, R.string.tab_scientific_a11y, R.string.tab_convert_a11y,
    R.string.tab_formulas_a11y, R.string.tab_ask_ai_a11y, R.string.tab_settings_a11y,
    R.string.tab_sci_classic_a11y,
)
private val TAB_GLYPHS = listOf(
    "=", "\u03C0", "\u21C4", "\u0192", "\u2726", "\u2699", "\u2211",
)

private const val STANDARD_TAB = 0
private const val SCIENTIFIC_TAB = 1
private const val CONVERT_TAB = 2
private const val FORMULAS_TAB = 3
/** The classic scientific pad. It is its own tab only in minimal mode. */
private const val SCI_CLASSIC_TAB = 6

/**
 * The five the minimal bar shows, in order, with no assistant anywhere.
 */
private val MINIMAL_TABS = listOf(
    STANDARD_TAB, SCIENTIFIC_TAB, SCI_CLASSIC_TAB, CONVERT_TAB, FORMULAS_TAB,
)

/** Two tabs on each side of the assistant button, so it sits dead centre. */
private val LEFT_TABS = listOf(0, 1)
private val RIGHT_TABS = listOf(2, 3)
private const val AI_TAB = 4
private const val SETTINGS_TAB = 5

/**
 * Slot weights. The leading spacer matches the settings gear on the right, so
 * the assistant lands exactly in the middle of the bar.
 */
private const val SIDE_WEIGHT = 0.3f
private const val TAB_WEIGHT = 1f
private const val AI_WEIGHT = 1.5f

/**
 * Every function the drawer offers, grouped so a long list stays navigable.
 *
 * The keys are the names the evaluator understands, so a key always inserts
 * something that actually works. Nothing appears twice.
 */
internal val SCIENTIFIC_GROUPS: List<Pair<Int, List<String>>> = listOf(
    R.string.fn_group_trig to listOf("sin", "cos", "tan", "csc", "sec", "cot"),
    R.string.fn_group_invtrig to listOf("asin", "acos", "atan", "acot", "asec", "acsc", "atan2"),
    R.string.fn_group_hyper to listOf("sinh", "cosh", "tanh", "coth", "sech", "csch"),
    R.string.fn_group_invhyper to listOf("asinh", "acosh", "atanh", "acoth", "asech", "acsch"),
    R.string.fn_group_log to listOf("ln", "log", "log2", "log10", "exp", "exp2", "log1p", "expm1"),
    R.string.fn_group_power to listOf("x^2", "x^3", "x^y", "sqrt", "cbrt", "nthroot", "1/x", "hypot"),
    R.string.fn_group_round to listOf("round", "floor", "ceil", "trunc", "frac", "sign", "rint", "mround"),
    R.string.fn_group_number to listOf(
        "abs", "gcd", "lcm", "mod", "isprime", "nextprime", "totient", "isqrt", "fib", "catalan",
    ),
    R.string.fn_group_comb to listOf("!", "nCr", "nPr"),
    R.string.fn_group_stats to listOf(
        "sum", "prod", "mean", "median", "mode", "var", "sd",
        "min", "max", "range", "geomean", "harmmean",
    ),
    R.string.fn_group_special to listOf(
        "gamma", "lgamma", "beta", "erf", "erfc", "zeta", "lambertw", "digamma",
    ),
    R.string.fn_group_const to listOf("pi", "e", "tau", "phi", "i", "x", "deg", "(", ")", "swap"),
)

/**
 * Which pad a tab shows.
 *
 * With "Classic Scientific" chosen, the classic pad takes the Scientific tab's
 * place — the Standard tab always keeps the plain number pad, so the layout
 * setting must never reach it.
 *
 * In minimal mode it does not replace anything: there the Scientific tab stays
 * as it is and the classic pad gets a tab of its own, so all the calculators are
 * reachable side by side.
 */
/**
 * Whether the assistant is shown at all.
 *
 * It goes away for either reason: the Ask AI switch turned off, or minimal mode
 * on. Both mean the same thing to the user — no assistant anywhere.
 */
internal fun aiIsVisible(aiEnabled: Boolean, minimal: Boolean): Boolean = aiEnabled && !minimal

internal fun usesClassicPad(
    layout: String,
    isScientificTab: Boolean,
    minimal: Boolean = false,
): Boolean = !minimal && isScientificTab && layout == "Classic Scientific"

/** The flat list, for tests and for anything that just wants every key. */
internal val SCIENTIFIC_KEYS: List<String> = SCIENTIFIC_GROUPS.flatMap { it.second }

/**
 * Root composable: the six main modes. The assistant sits in the middle of the
 * bottom bar, and Settings and everything else are one tap away.
 */
@Composable
fun AppRoot(settings: SettingsState) {
    val labels = TAB_LABELS.map { stringResource(it) }
    val a11y = TAB_A11Y.map { stringResource(it) }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    // A tool the classic pad asked for, handed to the Formulas tab.
    var requestedTool by remember { mutableStateOf("") }
    val minimal = settings.minimalMode
    // One switch, one meaning: the classic pad replaces the modern one on the
    // Scientific tab. It never reaches the Standard tab.
    val classicScientific =
        usesClassicPad(settings.layoutStyle, isScientificTab = true, minimal = minimal)
    val aiVisible = aiIsVisible(settings.aiEnabled, minimal)
    // Leaving minimal mode must not strand the user on a tab that is gone.
    LaunchedEffect(minimal) {
        if (minimal && tab !in MINIMAL_TABS && tab != SETTINGS_TAB) tab = STANDARD_TAB
        // Turning Ask AI off must not leave the user stranded on its screen.
        if (!aiVisible && tab == AI_TAB) tab = STANDARD_TAB
    }
    var aiContext by rememberSaveable { mutableStateOf("") }
    var aiAsk by rememberSaveable { mutableStateOf("") }

    // Pressing the tab you are already on takes that tab back to its own home.
    var resetTick by rememberSaveable { mutableIntStateOf(0) }
    // True while a tab is showing one of its inner screens.
    var innerOpen by remember { mutableStateOf(false) }

    val analyticsContext = LocalContext.current
    fun selectTab(i: Int) {
        if (tab == i) resetTick++ else tab = i
        Analytics.event(analyticsContext, "tab:" + TAB_LABELS[i].toString())
    }

    // The key tone follows the setting.
    LaunchedEffect(settings.keySound) { SoundPlayer.setChoice(settings.keySound) }

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

    Box(modifier = Modifier.fillMaxSize()) {
    AmbientGlow()

    Scaffold(
        // Transparent, so the ambient glow behind shows through the glass.
        containerColor = Color.Transparent,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (minimal) {
                    // Five things and no assistant: the three calculators, the
                    // converters and the formula reference. Settings is still in
                    // the overflow menu, so minimal can be switched off again.
                    MINIMAL_TABS.forEach { i ->
                        NavItem(i, tab, TAB_GLYPHS[i], labels[i], a11y[i], TAB_WEIGHT) { selectTab(i) }
                    }
                } else if (!aiVisible) {
                    // Ask AI is off, so its button goes and the four remaining
                    // tabs share the whole bar evenly.
                    (LEFT_TABS + RIGHT_TABS).forEach { i ->
                        NavItem(i, tab, TAB_GLYPHS[i], labels[i], a11y[i], TAB_WEIGHT) { selectTab(i) }
                    }
                } else {
                    // Balances the settings gear at the other end.
                    Spacer(modifier = Modifier.weight(SIDE_WEIGHT))

                    LEFT_TABS.forEach { i ->
                        NavItem(i, tab, TAB_GLYPHS[i], labels[i], a11y[i], TAB_WEIGHT) { selectTab(i) }
                    }

                    // The assistant: centred, because the row is balanced either side.
                    Box(modifier = Modifier.weight(AI_WEIGHT), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = a11y[AI_TAB]
                                role = Role.Tab
                                selected = tab == AI_TAB
                            },
                        ) {
                            MagicButton(onClick = { selectTab(AI_TAB) }, size = 62.dp)
                            Spacer(Modifier.width(2.dp))
                            Text(
                                labels[AI_TAB],
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    RIGHT_TABS.forEach { i ->
                        NavItem(i, tab, TAB_GLYPHS[i], labels[i], a11y[i], TAB_WEIGHT) { selectTab(i) }
                    }

                    // Settings lives in the overflow menu now, so this end of the
                    // bar is an empty spacer that keeps the assistant centred.
                    Spacer(modifier = Modifier.weight(SIDE_WEIGHT))
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
                    onOpenSettings = { selectTab(SETTINGS_TAB) },
                    onOpenTool = { t -> requestedTool = t; selectTab(FORMULAS_TAB) },
                )
                SCIENTIFIC_TAB -> CalculatorScreen(
                    // With "Classic Scientific" chosen the classic pad takes this
                    // tab's place; there is never a second scientific tab.
                    title = if (classicScientific) "Sci Classic" else "Scientific",
                    functionKeys = SCIENTIFIC_KEYS,
                    settings = settings,
                    classicScientific = classicScientific,
                    onAskAi = { q -> aiContext = q; tab = AI_TAB },
                    onAskAiNow = { q -> aiAsk = q; aiContext = ""; tab = AI_TAB },
                    onOpenSettings = { selectTab(SETTINGS_TAB) },
                    onOpenTool = { t -> requestedTool = t; selectTab(FORMULAS_TAB) },
                )
                SCI_CLASSIC_TAB -> CalculatorScreen(
                    // Only reachable in minimal mode, where the classic pad sits
                    // beside the modern one instead of replacing it.
                    title = "Sci Classic",
                    functionKeys = SCIENTIFIC_KEYS,
                    settings = settings,
                    classicScientific = true,
                    onAskAi = { q -> aiContext = q; tab = AI_TAB },
                    onAskAiNow = { q -> aiAsk = q; aiContext = ""; tab = AI_TAB },
                    onOpenSettings = { selectTab(SETTINGS_TAB) },
                    onOpenTool = { t -> requestedTool = t; selectTab(FORMULAS_TAB) },
                )
                CONVERT_TAB -> ConvertScreen()
                FORMULAS_TAB -> FormulasScreen(
                    resetSignal = resetTick,
                    requestedTool = requestedTool,
                    onToolRequestUsed = { requestedTool = "" },
                    onInnerChange = { innerOpen = it },
                )
                AI_TAB -> if (!aiVisible) CalculatorScreen(
                    title = "Standard",
                    functionKeys = emptyList(),
                    settings = settings,
                    onAskAi = {},
                    onAskAiNow = {},
                    onOpenSettings = { selectTab(SETTINGS_TAB) },
                    onOpenTool = { t -> requestedTool = t; selectTab(FORMULAS_TAB) },
                ) else AskAiScreen(
                    contextText = aiContext,
                    onClearContext = { aiContext = "" },
                    enabled = aiVisible,
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
}

/** One tab in the bottom bar, outlined like the keypad keys. */
@Composable
private fun RowScope.NavItem(
    index: Int,
    selectedTab: Int,
    glyph: String,
    label: String,
    spoken: String,
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
            .padding(vertical = 7.dp)
            // Only the name is read out, never the glyph.
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Tab
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(glyph, style = MaterialTheme.typography.headlineSmall, color = tint)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

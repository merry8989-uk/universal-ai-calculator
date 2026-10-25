package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The classic scientific pad.
 *
 * Three layers, exactly as a real scientific calculator is printed:
 *
 *   - the big label is what the key does on its own,
 *   - the yellow label above it is what SHIFT does,
 *   - the blue label below it is what ALPHA does.
 *
 * SHIFT and ALPHA both lock: one tap holds the layer on, another tap lets it go,
 * so a run of shifted keys can be typed without holding anything down. MODE does
 * not lock — it opens the list of calculation kinds. 2nd turns the pad over to
 * its second page, where the app's tools live.
 *
 * Every action names something the calculator already understands, so a press
 * goes through exactly the same path as the modern pad.
 */
/**
 * The direction pad.
 *
 * Four arrows round a centre, the way the reference draws it. Left and right
 * move the cursor through the expression; up and down walk the history.
 */
@Composable
private fun DirectionPad(
    modifier: Modifier = Modifier,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    val grey = Color(0xFF2B2B30)
    val ink = Color(0xFFD8DCE6)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(grey),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(1.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Arrow("\u25B2", ink, Modifier.weight(1f), onUp)
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Arrow("\u25C0", ink, Modifier.weight(1f), onLeft)
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF141418)),
                )
                Arrow("\u25B6", ink, Modifier.weight(1f), onRight)
            }
            Arrow("\u25BC", ink, Modifier.weight(1f), onDown)
        }
    }
}

/** One arm of the direction pad. */
@Composable
private fun Arrow(label: String, ink: Color, modifier: Modifier, onPress: () -> Unit) {
    val spoken = when (label) {
        "\u25C0" -> "move the cursor left"
        "\u25B6" -> "move the cursor right"
        "\u25B2" -> "the previous calculation"
        else -> "the next calculation"
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = spoken }
            .clickable(onClick = onPress),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = ink, fontSize = 9.sp)
    }
}

/** One of the little state lights above the keys. */
@Composable
private fun Indicator(label: String, on: Boolean, colour: Color) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = if (on) colour else Color(0xFF4A4E58),
        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
    )
}

internal enum class ClassicTone { Digit, Operator, Function, Accent, Danger, Shift, Alpha }

/** Which printed layer a key press reads from. */
internal enum class ClassicLayer { NORMAL, SHIFT, ALPHA }

internal data class ClassicKey(
    val label: String,
    /** What the key does on its own, or "" when [tool] is set. */
    val action: String = "",
    /** The yellow label above the key, and what SHIFT does. */
    val shift: String? = null,
    val shiftAction: String? = null,
    val shiftTool: String? = null,
    /** The blue label below the key, and what ALPHA does. */
    val alpha: String? = null,
    val alphaAction: String? = null,
    val alphaTool: String? = null,
    val tone: ClassicTone = ClassicTone.Function,
    /** How many columns wide the key is. Only the direction pad needs more than one. */
    val span: Int = 1,
    /** Set when the key opens a screen instead of typing. */
    val tool: String? = null,
)

// keys that do something to the pad rather than type
internal const val CLASSIC_SHIFT = "@shift"
internal const val CLASSIC_ALPHA = "@alpha"
internal const val CLASSIC_MODE = "@mode"
internal const val CLASSIC_PAGE = "@page"

/** The direction pad: one key that carries four. */
internal const val CLASSIC_DPAD = "@dpad"
internal const val CLASSIC_DEG = "@deg"
internal const val CLASSIC_LEFT = "@left"
internal const val CLASSIC_RIGHT = "@right"

private fun modifierBar(pageLabel: String) = listOf(
    ClassicKey("SHIFT", CLASSIC_SHIFT, tone = ClassicTone.Accent),
    ClassicKey("ALPHA", CLASSIC_ALPHA, tone = ClassicTone.Shift),
    // the direction pad, which the reference puts in the middle of this row
    ClassicKey("\u271B", CLASSIC_DPAD, tone = ClassicTone.Function, span = 2),
    ClassicKey("MODE", CLASSIC_MODE, tone = ClassicTone.Function),
    ClassicKey(pageLabel, CLASSIC_PAGE, tone = ClassicTone.Function),
)

/** Page one: the arithmetic and function keys. */
internal val CLASSIC_PAGE_ONE: List<List<ClassicKey>> = listOf(
    modifierBar("2nd"),
    listOf(
        ClassicKey("CALC", "=", shift = "SOLVE", shiftAction = "=", tone = ClassicTone.Function),
        ClassicKey("\u222Bdx", tool = "calculus", shift = "lim", shiftTool = "limits",
            alpha = "partial", alphaTool = "partial", tone = ClassicTone.Function),
        ClassicKey("d/dx", tool = "calculus", shift = "graph", shiftTool = "graph",
            alpha = "table", alphaTool = "reference", tone = ClassicTone.Function),
        ClassicKey("x!", "!", shift = "nCr", shiftAction = "nCr",
            alpha = "nPr", alphaAction = "nPr", tone = ClassicTone.Function),
        ClassicKey("\u03A3", "sum", shift = "GCD", shiftAction = "gcd",
            alpha = "LCM", alphaAction = "lcm", tone = ClassicTone.Function),
        ClassicKey("\u03A0", "prod", shift = "isprime", shiftAction = "isprime",
            alpha = "totient", alphaAction = "totient", tone = ClassicTone.Function),
    ),
    listOf(
        ClassicKey("x\u207B\u00B9", "1/x", shift = "\u207F\u221A", shiftAction = "nthroot",
            alpha = "catalan", alphaAction = "catalan", tone = ClassicTone.Function),
        ClassicKey("\u221Ax", "sqrt", shift = "\u221Bx", shiftAction = "cbrt",
            alpha = "fib", alphaAction = "fib", tone = ClassicTone.Function),
        ClassicKey("x\u00B2", "x^2", shift = "x\u00B3", shiftAction = "x^3",
            alpha = "isqrt", alphaAction = "isqrt", tone = ClassicTone.Function),
        ClassicKey("x\u02B8", "x^y", shift = "y\u221Ax", shiftAction = "nthroot",
            alpha = "nextprime", alphaAction = "nextprime", tone = ClassicTone.Function),
        ClassicKey("Log", "log", shift = "10\u02E3", shiftAction = "exp2",
            alpha = "log\u2082", alphaAction = "log2", tone = ClassicTone.Function),
        ClassicKey("Ln", "ln", shift = "e\u02E3", shiftAction = "exp",
            alpha = "log\u2081\u2080", alphaAction = "log10", tone = ClassicTone.Function),
    ),
    listOf(
        ClassicKey("hyp", "hypot", shift = "abs", shiftAction = "abs",
            alpha = "gamma", alphaAction = "gamma", tone = ClassicTone.Function),
        ClassicKey("Sin", "sin", shift = "Sin\u207B\u00B9", shiftAction = "asin",
            alpha = "sinh", alphaAction = "sinh", tone = ClassicTone.Function),
        ClassicKey("Cos", "cos", shift = "Cos\u207B\u00B9", shiftAction = "acos",
            alpha = "cosh", alphaAction = "cosh", tone = ClassicTone.Function),
        ClassicKey("Tan", "tan", shift = "Tan\u207B\u00B9", shiftAction = "atan",
            alpha = "tanh", alphaAction = "tanh", tone = ClassicTone.Function),
        ClassicKey("Cot", "cot", shift = "csc", shiftAction = "csc",
            alpha = "sech", alphaAction = "sech", tone = ClassicTone.Function),
        ClassicKey("sec", "sec", shift = "asin", shiftAction = "asin",
            alpha = "csch", alphaAction = "csch", tone = ClassicTone.Function),
    ),
    listOf(
        ClassicKey("\u2220", CLASSIC_DEG, shift = "\u00B0", shiftAction = "deg",
            alpha = "tau", alphaAction = "tau", tone = ClassicTone.Function),
        ClassicKey("floor", "floor", shift = "ceil", shiftAction = "ceil",
            alpha = "mround", alphaAction = "mround", tone = ClassicTone.Function),
        ClassicKey("FACT", "!", shift = "coth", shiftAction = "coth",
            alpha = "erf", alphaAction = "erf", tone = ClassicTone.Function),
        ClassicKey("sign", "sign", shift = "rand", shiftAction = "rand",
            alpha = "zeta", alphaAction = "zeta", tone = ClassicTone.Function),
        ClassicKey("\u03C0", "pi", shift = "\u03C4", shiftAction = "tau",
            alpha = "phi", alphaAction = "phi", tone = ClassicTone.Function),
        ClassicKey("mod", "mod", shift = "gcd", shiftAction = "gcd",
            alpha = "lcm", alphaAction = "lcm", tone = ClassicTone.Function),
    ),
    listOf(
        ClassicKey("7", "7", shift = "nCr", shiftAction = "nCr",
            alpha = "asinh", alphaAction = "asinh", tone = ClassicTone.Digit),
        ClassicKey("8", "8", shift = "catalan", shiftAction = "catalan",
            alpha = "acosh", alphaAction = "acosh", tone = ClassicTone.Digit),
        ClassicKey("9", "9", shift = "fib", shiftAction = "fib",
            alpha = "atanh", alphaAction = "atanh", tone = ClassicTone.Digit),
        ClassicKey("(", "(", shift = "RCL", shiftAction = "MR",
            alpha = "STO", alphaAction = "MS", tone = ClassicTone.Function),
        ClassicKey(")", ")", shift = "STO", shiftAction = "MS",
            alpha = "RCL", alphaAction = "MR", tone = ClassicTone.Function),
        ClassicKey("\u00F7", "/", shift = "mod", shiftAction = "mod",
            alpha = "acot", alphaAction = "acot", tone = ClassicTone.Operator),
    ),
    listOf(
        ClassicKey("4", "4", shift = "isprime", shiftAction = "isprime",
            alpha = "asec", alphaAction = "asec", tone = ClassicTone.Digit),
        ClassicKey("5", "5", shift = "totient", shiftAction = "totient",
            alpha = "acsc", alphaAction = "acsc", tone = ClassicTone.Digit),
        ClassicKey("6", "6", shift = "isqrt", shiftAction = "isqrt",
            alpha = "atan2", alphaAction = "atan2", tone = ClassicTone.Digit),
        ClassicKey("\u00D7", "*", shift = "hypot", shiftAction = "hypot",
            alpha = "sinc", alphaAction = "sinc", tone = ClassicTone.Operator),
        ClassicKey("^", "^", shift = "nthroot", shiftAction = "nthroot",
            alpha = "expm1", alphaAction = "expm1", tone = ClassicTone.Operator),
        ClassicKey("%", "%", shift = "sum", shiftAction = "sum",
            alpha = "prod", alphaAction = "prod", tone = ClassicTone.Operator),
    ),
    listOf(
        ClassicKey("1", "1", shift = "gcd", shiftAction = "gcd",
            alpha = "mean", alphaAction = "mean", tone = ClassicTone.Digit),
        ClassicKey("2", "2", shift = "lcm", shiftAction = "lcm",
            alpha = "median", alphaAction = "median", tone = ClassicTone.Digit),
        ClassicKey("3", "3", shift = "nextprime", shiftAction = "nextprime",
            alpha = "mode", alphaAction = "mode", tone = ClassicTone.Digit),
        ClassicKey("+", "+", shift = "\u00B1", shiftAction = "neg",
            alpha = "abs", alphaAction = "abs", tone = ClassicTone.Operator),
        ClassicKey("\u2212", "-", shift = "sign", shiftAction = "sign",
            alpha = "min", alphaAction = "min", tone = ClassicTone.Operator),
        ClassicKey("\u2190", CLASSIC_LEFT, shift = "\u2192", shiftAction = CLASSIC_RIGHT,
            alpha = "max", alphaAction = "max", tone = ClassicTone.Function),
    ),
    listOf(
        ClassicKey("0", "0", shift = "M+", shiftAction = "M+",
            alpha = "sd", alphaAction = "sd", tone = ClassicTone.Digit),
        ClassicKey(".", ".", shift = "ran#", shiftAction = "rand",
            alpha = "var", alphaAction = "var", tone = ClassicTone.Digit),
        ClassicKey("x", "x", shift = "\u0393", shiftAction = "gamma",
            alpha = "erfc", alphaAction = "erfc", tone = ClassicTone.Function),
        ClassicKey("Ans", "ans", shift = "M\u2212", shiftAction = "MS",
            alpha = "M+", alphaAction = "M+", tone = ClassicTone.Function),
        // the two clearing keys sit here, as they do in the reference; the
        // equals sign is the CALC key on the first row
        ClassicKey("\u232B", "del", shift = "AC", shiftAction = "C", tone = ClassicTone.Danger),
        ClassicKey("AC", "C", tone = ClassicTone.Danger),
    ),
)

/** Page two: the app's tools, with the number pad still underneath. */
internal val CLASSIC_PAGE_TWO: List<List<ClassicKey>> = listOf(
    modifierBar("1st"),
    listOf(
        ClassicKey("MATRIX", tool = "matrix", tone = ClassicTone.Function),
        ClassicKey("VECTOR", tool = "reference", tone = ClassicTone.Function),
        ClassicKey("STAT", tool = "statistics", tone = ClassicTone.Function),
        ClassicKey("DISTR", tool = "distributions", tone = ClassicTone.Function),
        ClassicKey("CPLX", tool = "complex", tone = ClassicTone.Function),
        ClassicKey("RPN", tool = "rpn", tone = ClassicTone.Function),
    ),
    listOf(
        ClassicKey("SYMB", tool = "symbolic", tone = ClassicTone.Function),
        ClassicKey("LIMIT", tool = "limits", tone = ClassicTone.Function),
        ClassicKey("PARTIAL", tool = "partial", tone = ClassicTone.Function),
        ClassicKey("CALC", tool = "calculus", tone = ClassicTone.Function),
        ClassicKey("GRAPH", tool = "graph", tone = ClassicTone.Function),
        ClassicKey("TABLE", tool = "table", tone = ClassicTone.Function),
    ),
    listOf(
        ClassicKey("CONV", tool = "converter", tone = ClassicTone.Function),
        ClassicKey("DATE", tool = "date", tone = ClassicTone.Function),
        ClassicKey("PROG", tool = "programmer", tone = ClassicTone.Function),
        ClassicKey("CURR", tool = "currency", tone = ClassicTone.Function),
        ClassicKey("SOLVE", tool = "solver", tone = ClassicTone.Function),
        ClassicKey("MENU", CLASSIC_MODE, tone = ClassicTone.Function),
    ),
    // the counting functions, which page one has no room for
    listOf(
        ClassicKey("nCr", "nCr", shift = "nPr", shiftAction = "nPr",
            alpha = "!", alphaAction = "!", tone = ClassicTone.Function),
        ClassicKey("gcd", "gcd", shift = "lcm", shiftAction = "lcm",
            alpha = "mod", alphaAction = "mod", tone = ClassicTone.Function),
        ClassicKey("abs", "abs", shift = "sign", shiftAction = "sign",
            alpha = "hypot", alphaAction = "hypot", tone = ClassicTone.Function),
        ClassicKey("\u221Bx", "cbrt", shift = "n\u221Ax", shiftAction = "nthroot",
            alpha = "\u221Ax", alphaAction = "sqrt", tone = ClassicTone.Function),
        ClassicKey("floor", "floor", shift = "ceil", shiftAction = "ceil",
            alpha = "round", alphaAction = "round", tone = ClassicTone.Function),
        ClassicKey("mround", "mround", shift = "sinc", shiftAction = "sinc",
            alpha = "rand", alphaAction = "rand", tone = ClassicTone.Function),
    ),
    // the special functions
    listOf(
        ClassicKey("\u0393", "gamma", shift = "log\u0393", shiftAction = "lgamma",
            alpha = "\u03B2", alphaAction = "beta", tone = ClassicTone.Function),
        ClassicKey("erf", "erf", shift = "erfc", shiftAction = "erfc",
            alpha = "\u03B6", alphaAction = "zeta", tone = ClassicTone.Function),
        ClassicKey("W", "lambertw", shift = "\u03C8", shiftAction = "digamma",
            alpha = "\u0393'", alphaAction = "gamma", tone = ClassicTone.Function),
        ClassicKey("fib", "fib", shift = "catalan", shiftAction = "catalan",
            alpha = "golden", alphaAction = "golden", tone = ClassicTone.Function),
        ClassicKey("totient", "totient", shift = "isqrt", shiftAction = "isqrt",
            alpha = "isprime", alphaAction = "isprime", tone = ClassicTone.Function),
        ClassicKey("nextprime", "nextprime", shift = "tau", shiftAction = "tau",
            alpha = "pi", alphaAction = "pi", tone = ClassicTone.Function),
    ),
    // the summaries
    listOf(
        ClassicKey("sum", "sum", shift = "prod", shiftAction = "prod",
            alpha = "\u2211", alphaAction = "sum", tone = ClassicTone.Function),
        ClassicKey("mean", "mean", shift = "median", shiftAction = "median",
            alpha = "mode", alphaAction = "mode", tone = ClassicTone.Function),
        ClassicKey("sd", "sd", shift = "variance", shiftAction = "variance",
            alpha = "range", alphaAction = "range", tone = ClassicTone.Function),
        ClassicKey("min", "min", shift = "max", shiftAction = "max",
            alpha = "geomean", alphaAction = "geomean", tone = ClassicTone.Function),
        ClassicKey("harmean", "harmean", shift = "atan2", shiftAction = "atan2",
            alpha = "log2", alphaAction = "log2", tone = ClassicTone.Function),
        ClassicKey("log10", "log10", shift = "log1p", shiftAction = "log1p",
            alpha = "expm1", alphaAction = "expm1", tone = ClassicTone.Function),
    ),
)

/** Every key on both pages, for the tests. */
internal val CLASSIC_ROWS: List<List<ClassicKey>> = CLASSIC_PAGE_ONE

@Composable
internal fun ClassicScientificPad(
    layer: ClassicLayer,
    page: Int,
    onShift: () -> Unit,
    onAlpha: () -> Unit,
    onMode: () -> Unit,
    onPageToggle: () -> Unit,
    onKey: (String) -> Unit,
    onTool: (String) -> Unit,
    onCaret: (Int) -> Unit,
    /** Up and down on the direction pad: through the history, newest first. */
    onVertical: (Int) -> Unit = {},
    onToggleDegrees: () -> Unit,
    modifier: Modifier = Modifier,
    /** How tall one row of keys is. Kept compact on purpose. */
    rowHeight: Dp = 40.dp,
    /** What the divide key shows, so it can match the modern pad. */
    divisionSign: String = "\u00F7",
    /** What the multiply key shows. */
    multiplicationSign: String = "\u00D7",
) {
    val rows = if (page == 1) CLASSIC_PAGE_TWO else CLASSIC_PAGE_ONE
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(rowHeight),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                row.forEach { key ->
                    val active = when (layer) {
                        ClassicLayer.SHIFT -> Active(key.shift ?: key.label,
                            key.shiftAction ?: key.action, key.shiftTool ?: key.tool)
                        ClassicLayer.ALPHA -> Active(key.alpha ?: key.label,
                            key.alphaAction ?: key.action, key.alphaTool ?: key.tool)
                        ClassicLayer.NORMAL -> Active(key.label, key.action, key.tool)
                    }
                    // a locked layer lights its own label up
                    val lit = when (key.action) {
                        CLASSIC_SHIFT -> layer == ClassicLayer.SHIFT
                        CLASSIC_ALPHA -> layer == ClassicLayer.ALPHA
                        else -> false
                    }
                    val spoken = if (active.action.isEmpty() || active.action.startsWith("@")) {
                        spokenKey(active.label)
                    } else {
                        spokenKey(active.action)
                    }
                    // the two operator keys follow the keyboard setting
                    val shownLabel = when (key.action) {
                        "/" -> divisionSign
                        "*" -> multiplicationSign
                        else -> key.label
                    }
                    // the direction pad is one key that carries four
                    if (key.action == CLASSIC_DPAD) {
                        DirectionPad(
                            modifier = Modifier.weight(key.span.toFloat()).fillMaxSize(),
                            onLeft = { onCaret(-1) },
                            onRight = { onCaret(1) },
                            onUp = { onVertical(-1) },
                            onDown = { onVertical(1) },
                        )
                        return@forEach
                    }
                    ClassicKeyView(
                        label = shownLabel,
                        shift = key.shift,
                        alpha = key.alpha,
                        tone = key.tone,
                        lit = lit,
                        // the locked layer makes its label bold
                        emphasise = when (layer) {
                            ClassicLayer.SHIFT -> key.shift
                            ClassicLayer.ALPHA -> key.alpha
                            ClassicLayer.NORMAL -> null
                        },
                        spoken = spoken,
                        modifier = Modifier.weight(key.span.toFloat()).fillMaxSize(),
                    ) {
                        when (active.action) {
                            CLASSIC_SHIFT -> onShift()
                            CLASSIC_ALPHA -> onAlpha()
                            CLASSIC_MODE -> onMode()
                            CLASSIC_PAGE -> onPageToggle()
                            CLASSIC_DEG -> onToggleDegrees()
                            CLASSIC_LEFT -> onCaret(-1)
                            CLASSIC_RIGHT -> onCaret(1)
                            else ->
                                if (active.tool != null) onTool(active.tool) else onKey(active.action)
                        }
                    }
                }
            }
        }
    }
}

/** What a key does in the layer that is currently locked. */
private data class Active(val label: String, val action: String, val tool: String?)

@Composable
private fun ClassicKeyView(
    label: String,
    shift: String?,
    alpha: String?,
    tone: ClassicTone,
    lit: Boolean,
    emphasise: String?,
    spoken: String,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
) {
    // The classic pad is monochrome on purpose. Only the four keys the
    // reference colours are coloured: SHIFT, ALPHA, and the two that clear.
    // Everything else is the same grey, and the small labels above carry the
    // meaning — which is what makes it readable at this size.
    val background = when (tone) {
        ClassicTone.Accent -> Color(0xFFE8A33D)
        ClassicTone.Shift -> Color(0xFF6C4CF1)
        ClassicTone.Danger -> Color(0xFFE07A3C)
        else -> Color(0xFF2B2B30)
    }
    val textColor = when (tone) {
        ClassicTone.Accent -> Color(0xFF241503)
        else -> Color(0xFFF2F2F4)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(background)
            .clickable(onClick = onPress)
            .semantics { contentDescription = spoken }
            .padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // yellow, above the key
        ClassicMicroLabel(shift, YELLOW, emphasise == shift)
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AutoSizeText(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (emphasise != null) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                ),
                color = textColor,
                minScale = 0.6f,
                textAlign = TextAlign.Center,
                // a key label sits in the middle of its key, not at the right edge
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // blue, below the key
        ClassicMicroLabel(alpha, BLUE, emphasise == alpha)
    }
}

@Composable
private fun ClassicMicroLabel(text: String?, color: Color, emphasise: Boolean) {
    if (text == null) return
    AutoSizeText(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            fontWeight = if (emphasise) FontWeight.Bold else FontWeight.Normal,
        ),
        color = if (emphasise) Color.White else color,
        minScale = 0.7f,
        textAlign = TextAlign.Center,
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

private val YELLOW = Color(0xFFE8B84B)
private val BLUE = Color(0xFF7FB2E8)

/** One entry in the MODE list: a kind of calculation the app can do. */
internal data class CalcMode(val id: String, val name: String, val blurb: String)

/**
 * The kinds of calculation this app offers. MODE shows this list; it is not a
 * lock, it is a way of getting to the other calculators without hunting.
 */
internal val CALC_MODES: List<CalcMode> = listOf(
    CalcMode("standard", "Standard", "The plain number pad."),
    CalcMode("scientific", "Scientific", "The modern scientific pad."),
    CalcMode("classic", "Sci Classic", "This pad, with its printed second labels."),
    CalcMode("reference", "Formula reference", "Maths, physics and chemistry formulas."),
    CalcMode("matrix", "Matrix", "Add, multiply, determinant, inverse, eigenvalues."),
    CalcMode("statistics", "Statistics", "Mean, median, SD, quartiles, regression."),
    CalcMode("distributions", "Distributions & tests", "Normal, t, binomial, Poisson, t-tests."),
    CalcMode("complex", "Complex numbers", "Arithmetic, magnitude, angle, roots."),
    CalcMode("rpn", "RPN stack", "Reverse-Polish entry."),
    CalcMode("solver", "Equation solver", "Linear to quintic, several unknowns, inequalities."),
    CalcMode("table", "Table of values", "What f(x) and g(x) do across a range."),
    CalcMode("symbolic", "Symbolic algebra", "Simplify, differentiate, integrate, factor."),
    CalcMode("limits", "Limits", "Limits from one side or at infinity."),
    CalcMode("partial", "Partial fractions", "Split a rational function."),
    CalcMode("calculus", "Calculus", "Numerical derivative, integral, summation."),
    CalcMode("graph", "Graphing", "Plot y = f(x) on a live canvas."),
    CalcMode("converter", "Converters", "Roman, words, bases, colour, timestamps."),
    CalcMode("date", "Date calculator", "Difference, age, working days, week number."),
    CalcMode("programmer", "Programmer", "Base conversion and bitwise operations."),
    CalcMode("currency", "Currency", "Live exchange rates."),
    CalcMode("handwriting", "Handwriting", "Write a calculation and let the app read it."),
)

@Composable
internal fun ClassicModeDialog(
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What would you like to work out?") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                CALC_MODES.forEach { mode ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPick(mode.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(mode.name, style = MaterialTheme.typography.titleSmall)
                        Text(mode.blurb, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

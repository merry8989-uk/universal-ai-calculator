package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The classic scientific pad.
 *
 * Every key carries a small second label above it, the way a real scientific
 * calculator is printed: the function you get by pressing SHIFT first. The label
 * is always visible, so you can see what a key will do before reaching for the
 * modifier — that is the whole point of the layout.
 *
 * Each key names something the calculator already understands, so a press goes
 * through exactly the same path as the modern pad. [ClassicKey.tool] is for the
 * few keys that open a whole screen instead of typing.
 */
internal enum class ClassicTone { Digit, Operator, Function, Accent, Danger, Shift }

internal data class ClassicKey(
    /** The big label on the key. */
    val label: String,
    /** The key name the calculator handles, or "" when [tool] is set. */
    val action: String = "",
    /** The small label above the key, always on screen. */
    val second: String? = null,
    /** What SHIFT makes the key do. */
    val secondAction: String? = null,
    /** The colour of the small label, so families of functions read together. */
    val secondTone: ClassicTone = ClassicTone.Accent,
    val tone: ClassicTone = ClassicTone.Function,
    /** Set when the key opens a screen instead of typing. */
    val tool: String? = null,
    val secondTool: String? = null,
)

private const val CARET_LEFT = "@left"
private const val CARET_RIGHT = "@right"
private const val SHIFT = "@shift"
private const val MENU = "@menu"
private const val DEG_TOGGLE = "@deg"

private val YELLOW = ClassicTone.Accent
private val BLUE = ClassicTone.Shift

/**
 * Ten rows of six. The first row is the modifier bar, the next five are the
 * function block, and the last four are the number pad with its operators.
 */
internal val CLASSIC_ROWS: List<List<ClassicKey>> = listOf(
    // ---- modifier bar ----
    listOf(
        ClassicKey("SHIFT", SHIFT, tone = ClassicTone.Accent),
        ClassicKey("ALPHA", SHIFT, tone = ClassicTone.Shift),
        ClassicKey("MODE", MENU, tone = ClassicTone.Function),
        ClassicKey("2nd", SHIFT, tone = ClassicTone.Shift),
        ClassicKey("\u232B", "del", second = "AC", secondAction = "C", tone = ClassicTone.Danger,
            secondTone = ClassicTone.Danger),
        ClassicKey("AC", "C", tone = ClassicTone.Danger),
    ),
    // ---- functions, top block ----
    listOf(
        ClassicKey("CALC", "=", second = "SOLVE", secondAction = "=", tone = ClassicTone.Function),
        ClassicKey("\u222Bdx", tool = "calculus", second = "lim", secondTool = "limits",
            secondTone = YELLOW, tone = ClassicTone.Function),
        ClassicKey("d/dx", tool = "calculus", second = "partial", secondTool = "partial",
            secondTone = YELLOW, tone = ClassicTone.Function),
        ClassicKey("x!", "!", second = "nCr", secondAction = "nCr", tone = ClassicTone.Function),
        ClassicKey("\u03A3", "sum", second = "GCD", secondAction = "gcd", tone = ClassicTone.Function),
        ClassicKey("\u03A0", "prod", second = "LCM", secondAction = "lcm", tone = ClassicTone.Function),
    ),
    listOf(
        ClassicKey("x\u207B\u00B9", "1/x", second = "\u207F\u221A", secondAction = "nthroot",
            tone = ClassicTone.Function),
        ClassicKey("\u221Ax", "sqrt", second = "\u221Bx", secondAction = "cbrt", tone = ClassicTone.Function),
        ClassicKey("x\u00B2", "x^2", second = "x\u00B3", secondAction = "x^3", tone = ClassicTone.Function),
        ClassicKey("x\u02B8", "x^y", second = "y\u221Ax", secondAction = "nthroot", tone = ClassicTone.Function),
        ClassicKey("Log", "log", second = "10\u02E3", secondAction = "exp2", tone = ClassicTone.Function),
        ClassicKey("Ln", "ln", second = "e\u02E3", secondAction = "exp", tone = ClassicTone.Function),
    ),
    listOf(
        ClassicKey("hyp", "hypot", second = "abs", secondAction = "abs", tone = ClassicTone.Function),
        ClassicKey("Sin", "sin", second = "Sin\u207B\u00B9", secondAction = "asin", tone = ClassicTone.Function),
        ClassicKey("Cos", "cos", second = "Cos\u207B\u00B9", secondAction = "acos", tone = ClassicTone.Function),
        ClassicKey("Tan", "tan", second = "Tan\u207B\u00B9", secondAction = "atan", tone = ClassicTone.Function),
        ClassicKey("Cot", "cot", second = "csc", secondAction = "csc", tone = ClassicTone.Function),
        ClassicKey("sec", "sec", second = "sinh", secondAction = "sinh", tone = ClassicTone.Function),
    ),
    listOf(
        ClassicKey("\u2220", DEG_TOGGLE, second = "\u00B0", secondAction = "deg",
            secondTone = YELLOW, tone = ClassicTone.Function),
        ClassicKey("cosh", "cosh", second = "tanh", secondAction = "tanh", tone = ClassicTone.Function),
        ClassicKey("FACT", "!", second = "mround", secondAction = "mround", tone = ClassicTone.Function),
        ClassicKey("floor", "floor", second = "ceil", secondAction = "ceil", tone = ClassicTone.Function),
        ClassicKey("sign", "sign", second = "rand", secondAction = "rand", tone = ClassicTone.Function),
        ClassicKey("\u03C0", "pi", second = "\u03C4", secondAction = "tau", tone = ClassicTone.Function),
    ),
    // ---- number pad and operators ----
    listOf(
        ClassicKey("7", "7", second = "nPr", secondAction = "nPr", tone = ClassicTone.Digit),
        ClassicKey("8", "8", second = "catalan", secondAction = "catalan", tone = ClassicTone.Digit),
        ClassicKey("9", "9", second = "fib", secondAction = "fib", tone = ClassicTone.Digit),
        ClassicKey("(", "(", second = "RCL", secondAction = "MR", tone = ClassicTone.Function),
        ClassicKey(")", ")", second = "STO", secondAction = "MS", tone = ClassicTone.Function),
        ClassicKey("\u00F7", "/", second = "mod", secondAction = "mod", tone = ClassicTone.Operator),
    ),
    listOf(
        ClassicKey("4", "4", second = "isprime", secondAction = "isprime", tone = ClassicTone.Digit),
        ClassicKey("5", "5", second = "totient", secondAction = "totient", tone = ClassicTone.Digit),
        ClassicKey("6", "6", second = "isqrt", secondAction = "isqrt", tone = ClassicTone.Digit),
        ClassicKey("\u00D7", "*", second = "hypot", secondAction = "hypot", tone = ClassicTone.Operator),
        ClassicKey("\u00F7", "/", second = "mod", secondAction = "mod", tone = ClassicTone.Operator),
        ClassicKey("%", "%", second = "\u2211", secondAction = "sum", tone = ClassicTone.Operator),
    ),
    listOf(
        ClassicKey("1", "1", second = "gcd", secondAction = "gcd", tone = ClassicTone.Digit),
        ClassicKey("2", "2", second = "lcm", secondAction = "lcm", tone = ClassicTone.Digit),
        ClassicKey("3", "3", second = "nextprime", secondAction = "nextprime", tone = ClassicTone.Digit),
        ClassicKey("+", "+", second = "\u00B1", secondAction = "neg", tone = ClassicTone.Operator),
        ClassicKey("\u2212", "-", second = "sign", secondAction = "sign", tone = ClassicTone.Operator),
        ClassicKey("\u2190", CARET_LEFT, second = "\u2192", secondAction = CARET_RIGHT,
            secondTone = YELLOW, tone = ClassicTone.Function),
    ),
    listOf(
        ClassicKey("0", "0", second = "M+", secondAction = "M+", tone = ClassicTone.Digit),
        ClassicKey(".", ".", second = "ran#", secondAction = "rand", tone = ClassicTone.Digit),
        ClassicKey("Exp", "e", second = "10\u02E3", secondAction = "exp2", tone = ClassicTone.Function),
        ClassicKey("Ans", "ans", second = "M\u2212", secondAction = "MS", tone = ClassicTone.Function),
        ClassicKey("x", "x", second = "\u0393", secondAction = "gamma", tone = ClassicTone.Function),
        ClassicKey("=", "=", second = "=", secondAction = "=", tone = ClassicTone.Accent),
    ),
)

@Composable
internal fun ClassicScientificPad(
    shifted: Boolean,
    onShiftToggle: () -> Unit,
    onKey: (String) -> Unit,
    onTool: (String) -> Unit,
    onCaret: (Int) -> Unit,
    onMenu: () -> Unit,
    onToggleDegrees: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CLASSIC_ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                row.forEach { key ->
                    // The small label is always drawn; SHIFT only changes what a
                    // press does, so the key never looks different to the eye.
                    val activeAction = if (shifted) key.secondAction ?: key.action else key.action
                    val spoken = if (activeAction.isEmpty() || activeAction.startsWith("@")) {
                        spokenKey(key.label)
                    } else {
                        spokenKey(activeAction)
                    }
                    ClassicKeyView(
                        label = key.label,
                        second = key.second,
                        secondTone = key.secondTone,
                        tone = if (shifted && key.second != null) ClassicTone.Shift else key.tone,
                        shifted = shifted,
                        spoken = spoken,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    ) {
                        when {
                            activeAction == SHIFT -> onShiftToggle()
                            activeAction == CARET_LEFT -> onCaret(-1)
                            activeAction == CARET_RIGHT -> onCaret(1)
                            activeAction == MENU -> onMenu()
                            activeAction == DEG_TOGGLE -> onToggleDegrees()
                            else -> {
                                val tool = if (shifted) key.secondTool ?: key.tool else key.tool
                                if (tool != null) onTool(tool) else onKey(activeAction)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassicKeyView(
    label: String,
    second: String?,
    secondTone: ClassicTone,
    tone: ClassicTone,
    shifted: Boolean,
    spoken: String,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
) {
    val background = when (tone) {
        ClassicTone.Digit -> Color(0xFF23283A)
        ClassicTone.Operator -> Color(0xFF2F3550)
        ClassicTone.Function -> Color(0xFF1B2032)
        ClassicTone.Accent -> Color(0xFFE8A33D)
        ClassicTone.Danger -> Color(0xFFB4462F)
        ClassicTone.Shift -> Color(0xFF6C4CF1)
    }
    val textColor = when (tone) {
        ClassicTone.Digit -> Color(0xFFF2F4FA)
        ClassicTone.Operator -> Color(0xFFBFE3FF)
        ClassicTone.Function -> Color(0xFFD3DAEA)
        ClassicTone.Accent -> Color(0xFF2A1A05)
        ClassicTone.Danger -> Color.White
        ClassicTone.Shift -> Color.White
    }
    val secondColor = when (secondTone) {
        ClassicTone.Accent -> Color(0xFFE8B84B)
        ClassicTone.Shift -> Color(0xFF9C8BFF)
        ClassicTone.Danger -> Color(0xFFFF8A7A)
        else -> Color(0xFF7FB2E8)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(background)
            .clickable(onClick = onPress)
            .semantics { contentDescription = spoken }
            .padding(horizontal = 2.dp, vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // the small second-function label, printed above the key like the real thing
        if (second != null) {
            Text(
                text = second,
                color = secondColor,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = textColor,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (shifted && second != null) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The classic scientific keypad: six keys across, eight rows down, with a SHIFT
 * key that reveals a second function on each one.
 *
 * Every key carries the name the calculator already understands, so a key press
 * goes through exactly the same path as the modern pad — there is no second
 * arithmetic engine hiding in here. `onTool` is for the handful of keys that
 * open a whole screen rather than typing something (the integral, the
 * derivative, the limit).
 */
internal enum class ClassicTone { Digit, Operator, Function, Accent, Danger }

internal data class ClassicKey(
    val label: String,
    /** The key name the calculator handles, or a tool id when [tool] is set. */
    val action: String = "",
    val tone: ClassicTone = ClassicTone.Function,
    val shiftedLabel: String? = null,
    val shiftedAction: String? = null,
    val shiftedTone: ClassicTone = ClassicTone.Accent,
    /** Set when the key opens a screen instead of typing. */
    val tool: String? = null,
    val shiftedTool: String? = null,
)

private const val CARET_LEFT = "@left"
private const val CARET_RIGHT = "@right"
private const val SHIFT = "@shift"
private const val MENU = "@menu"
private const val DEG_TOGGLE = "@deg"

/** Eight rows of six, in the order they are drawn. */
internal val CLASSIC_ROWS: List<List<ClassicKey>> = listOf(
    listOf(
        ClassicKey("SHIFT", SHIFT, ClassicTone.Accent),
        ClassicKey("\u25C0", CARET_LEFT, ClassicTone.Function),
        ClassicKey("\u25B6", CARET_RIGHT, ClassicTone.Function),
        ClassicKey("\u232B", "del", ClassicTone.Danger, shiftedLabel = "AC", shiftedAction = "C"),
        ClassicKey("AC", "C", ClassicTone.Danger),
        ClassicKey("\u22EE", MENU, ClassicTone.Function),
    ),
    listOf(
        ClassicKey("DRG", DEG_TOGGLE, ClassicTone.Accent, shiftedLabel = "\u00B0", shiftedAction = "deg"),
        ClassicKey("x\u21D4E", "swap", ClassicTone.Accent, shiftedLabel = "x\u2192M", shiftedAction = "MS"),
        ClassicKey("\u03A3", "sum", ClassicTone.Accent, shiftedLabel = "\u03A0", shiftedAction = "prod"),
        ClassicKey("\u222Bdx", tone = ClassicTone.Accent, tool = "calculus",
            shiftedLabel = "d/dx", shiftedTool = "calculus"),
        ClassicKey("\u03C0", "pi", ClassicTone.Function, shiftedLabel = "\u03C4", shiftedAction = "tau"),
        ClassicKey("e", "e", ClassicTone.Function, shiftedLabel = "e\u02E3", shiftedAction = "exp"),
    ),
    listOf(
        ClassicKey("sin", "sin", shiftedLabel = "sin\u207B\u00B9", shiftedAction = "asin"),
        ClassicKey("cos", "cos", shiftedLabel = "cos\u207B\u00B9", shiftedAction = "acos"),
        ClassicKey("tan", "tan", shiftedLabel = "tan\u207B\u00B9", shiftedAction = "atan"),
        ClassicKey("i", "i", shiftedLabel = "sinh", shiftedAction = "sinh"),
        ClassicKey("log", "log", shiftedLabel = "log\u2081\u2080", shiftedAction = "log10"),
        ClassicKey("ln", "ln", shiftedLabel = "log\u2082", shiftedAction = "log2"),
    ),
    listOf(
        ClassicKey("x\u207B\u00B9", "1/x", shiftedLabel = "\u207F\u221A", shiftedAction = "nthroot"),
        ClassicKey("x\u00B2", "x^2", shiftedLabel = "x\u00B3", shiftedAction = "x^3"),
        ClassicKey("\u221A", "sqrt", shiftedLabel = "\u221B", shiftedAction = "cbrt"),
        ClassicKey("x\u02B8", "x^y", shiftedLabel = "hyp", shiftedAction = "hypot"),
        ClassicKey("(", "(", shiftedLabel = "abs", shiftedAction = "abs"),
        ClassicKey(")", ")", shiftedLabel = "\u230A\u230B", shiftedAction = "floor"),
    ),
    listOf(
        ClassicKey("7", "7", ClassicTone.Digit),
        ClassicKey("8", "8", ClassicTone.Digit),
        ClassicKey("9", "9", ClassicTone.Digit),
        ClassicKey("\u00D7", "*", ClassicTone.Operator),
        ClassicKey("\u00F7", "/", ClassicTone.Operator),
        ClassicKey("%", "%", ClassicTone.Operator, shiftedLabel = "mod", shiftedAction = "mod"),
    ),
    listOf(
        ClassicKey("4", "4", ClassicTone.Digit),
        ClassicKey("5", "5", ClassicTone.Digit),
        ClassicKey("6", "6", ClassicTone.Digit),
        ClassicKey("+", "+", ClassicTone.Operator),
        ClassicKey("\u2212", "-", ClassicTone.Operator),
        ClassicKey("nCr", "nCr", shiftedLabel = "cat", shiftedAction = "catalan"),
    ),
    listOf(
        ClassicKey("1", "1", ClassicTone.Digit),
        ClassicKey("2", "2", ClassicTone.Digit),
        ClassicKey("3", "3", ClassicTone.Digit),
        ClassicKey("\u00B1", "neg", ClassicTone.Operator, shiftedLabel = "sign", shiftedAction = "sign"),
        ClassicKey("EXP", "e", ClassicTone.Function, shiftedLabel = "10\u02E3", shiftedAction = "exp2"),
        ClassicKey("nPr", "nPr", shiftedLabel = "fib", shiftedAction = "fib"),
    ),
    listOf(
        ClassicKey("0", "0", ClassicTone.Digit),
        ClassicKey(".", ".", ClassicTone.Digit),
        ClassicKey("Ans", "ans", ClassicTone.Function, shiftedLabel = "ran#", shiftedAction = "rand"),
        ClassicKey("x", "x", ClassicTone.Function, shiftedLabel = "\u0393", shiftedAction = "gamma"),
        ClassicKey("=", "=", ClassicTone.Accent),
        ClassicKey("MR", "MR", ClassicTone.Function, shiftedLabel = "M+", shiftedAction = "M+"),
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
        modifier = modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        CLASSIC_ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                row.forEach { key ->
                    val label = if (shifted) key.shiftedLabel ?: key.label else key.label
                    val tone = if (shifted) key.shiftedTone else key.tone
                    // Say what the key does, using the action when there is one,
                    // so a glyph like ∫dx is never read out as itself.
                    val activeAction = if (shifted) key.shiftedAction ?: key.action else key.action
                    val spoken = if (activeAction.startsWith("@")) label else spokenKey(activeAction)
                    ClassicKeyView(
                        label = label,
                        tone = if (key.action == SHIFT && shifted) ClassicTone.Accent else tone,
                        highlighted = key.action == SHIFT && shifted,
                        spoken = spoken,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    ) {
                        when {
                            key.action == SHIFT -> onShiftToggle()
                            key.action == CARET_LEFT -> onCaret(-1)
                            key.action == CARET_RIGHT -> onCaret(1)
                            key.action == MENU -> onMenu()
                            key.action == DEG_TOGGLE && !shifted -> onToggleDegrees()
                            else -> {
                                val tool = if (shifted) key.shiftedTool ?: key.tool else key.tool
                                val action = if (shifted) key.shiftedAction ?: key.action else key.action
                                if (tool != null && (action.isEmpty() || action == key.action)) {
                                    onTool(tool)
                                } else {
                                    onKey(action)
                                }
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
    tone: ClassicTone,
    highlighted: Boolean,
    spoken: String,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
) {
    val background = when (tone) {
        ClassicTone.Digit -> Color(0xFF23283A)
        ClassicTone.Operator -> Color(0xFF2F3550)
        ClassicTone.Function -> Color(0xFF1B2032)
        ClassicTone.Accent -> Color(0xFF6C4CF1)
        ClassicTone.Danger -> Color(0xFF8A2F3F)
    }
    val textColor = when (tone) {
        ClassicTone.Digit -> Color(0xFFF2F4FA)
        ClassicTone.Operator -> Color(0xFFBFE3FF)
        ClassicTone.Function -> Color(0xFFC9D2E6)
        ClassicTone.Accent -> Color.White
        ClassicTone.Danger -> Color.White
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (highlighted) Color(0xFFB58BFF) else background)
            .clickable(onClick = onPress)
            .semantics { contentDescription = spoken },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (highlighted) Color(0xFF1B1030) else textColor,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

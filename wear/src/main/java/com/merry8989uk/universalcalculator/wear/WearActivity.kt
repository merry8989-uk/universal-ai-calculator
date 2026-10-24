package com.merry8989uk.universalcalculator.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.Format

/**
 * The watch app: one screen, a display and a keypad that fits a round face.
 *
 * The arithmetic comes from :core — the same engine the phone uses — so the two
 * always agree. There is no assistant, no sync and no history here: a watch is
 * for a quick sum.
 */
class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearApp() }
    }
}

private val Ink = Color(0xFF0A0C14)
private val Panel = Color(0xFF171A26)
private val Accent = Color(0xFF6C4CF1)
private val Cyan = Color(0xFF06B6D4)
private val Operator = Color(0xFF2A2F45)

/** The keypad, laid out for a small round screen: four across, five down. */
private val KEY_ROWS = listOf(
    listOf("C", "del", "(", ")"),
    listOf("7", "8", "9", "/"),
    listOf("4", "5", "6", "*"),
    listOf("1", "2", "3", "-"),
    listOf("0", ".", "+", "="),
)

@Composable
fun WearApp() {
    MaterialTheme(
        colorScheme = darkColorScheme(background = Ink, surface = Ink, primary = Accent),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            WatchCalculator()
        }
    }
}

@Composable
private fun WatchCalculator() {
    var expression by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf("0") }
    var message by remember { mutableStateOf("") }

    fun recompute() {
        message = ""
        if (expression.isBlank()) {
            shown = "0"
            return
        }
        shown = try {
            Format.number(Evaluator.evaluate(expression))
        } catch (e: Exception) {
            message = e.message.orEmpty().take(40)
            shown
        }
    }

    fun press(key: String) {
        when (key) {
            "C" -> { expression = ""; shown = "0"; message = "" }
            "del" -> { expression = expression.dropLast(1); recompute() }
            "=" -> {
                recompute()
                if (message.isEmpty()) expression = shown
            }
            else -> {
                expression += key
                recompute()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        // The display. Long results shrink rather than run off the edge.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Panel)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = expression.ifBlank { " " },
                    color = Color(0xFF8A93A6),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                )
                Text(
                    text = if (message.isEmpty()) shown else message,
                    color = if (message.isEmpty()) Cyan else Color(0xFFFF6B6B),
                    fontSize = if (shown.length > 9) 16.sp else 24.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                )
            }
        }

        KEY_ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                row.forEach { key ->
                    Key(key, modifier = Modifier.weight(1f).fillMaxSize()) { press(key) }
                }
            }
        }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier = Modifier, onPress: () -> Unit) {
    val isOperator = label in listOf("/", "*", "-", "+")
    val isAction = label == "=" || label == "C" || label == "del"
    val background = when {
        label == "=" -> Accent
        label == "C" || label == "del" -> Color(0xFF3A2530)
        isOperator -> Operator
        else -> Color(0xFF20243A)
    }
    // What a screen reader says. The glyphs alone mean nothing.
    val spoken = when (label) {
        "C" -> stringResource(R.string.clear)
        "del" -> stringResource(R.string.delete)
        "=" -> stringResource(R.string.equals)
        "*" -> "multiply"
        "/" -> "divide"
        "-" -> "minus"
        "+" -> "plus"
        else -> label
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onPress)
            .semantics { contentDescription = spoken },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when (label) {
                "del" -> "\u232B"
                "*" -> "\u00D7"
                "/" -> "\u00F7"
                "-" -> "\u2212"
                else -> label
            },
            color = if (isAction || isOperator) Color.White else Color(0xFFD7DCEA),
            fontSize = 17.sp,
            maxLines = 1,
        )
    }
}

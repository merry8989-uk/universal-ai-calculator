package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.settings.SettingsState

private val PAD = listOf(
    listOf("C", "del", "%", "/"),
    listOf("7", "8", "9", "*"),
    listOf("4", "5", "6", "-"),
    listOf("1", "2", "3", "+"),
    listOf("neg", "0", ".", "="),
)

private val LABELS = mapOf(
    "del" to "\u232B", "/" to "\u00F7", "*" to "\u00D7", "-" to "\u2212",
    "neg" to "\u00B1", "x^2" to "x\u00B2", "x^y" to "x\u02B8", "pi" to "\u03C0",
    "sqrt" to "\u221A",
)

private fun label(k: String) = LABELS[k] ?: k

private fun insertFor(key: String): String = when (key) {
    "sin", "cos", "tan", "asin", "acos", "atan" -> "$key("
    "ln" -> "ln("
    "log" -> "log("
    "sqrt" -> "sqrt("
    "x^2" -> "^2"
    "x^y" -> "^"
    "pi" -> "pi"
    "e" -> "e"
    "!" -> "!"
    "(" -> "("
    ")" -> ")"
    "1/x" -> "1/("
    "abs" -> "abs("
    "nCr" -> "nCr("
    "nPr" -> "nPr("
    "round" -> "round("
    else -> key
}

@Composable
fun CalculatorScreen(
    title: String,
    functionKeys: List<String>,
    settings: SettingsState,
) {
    var expr by rememberSaveable { mutableStateOf("") }
    var ans by rememberSaveable { mutableStateOf(0.0) }
    var degrees by rememberSaveable { mutableStateOf(settings.degreesMode) }
    var history by rememberSaveable { mutableStateOf("") }

    val preview: String = if (expr.isBlank()) "" else try {
        Format.number(Evaluator.evaluate(expr, degrees, mapOf("ans" to ans)))
    } catch (e: Exception) {
        ""
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { degrees = !degrees }) {
                Text(if (degrees) "DEG" else "RAD")
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End,
        ) {
            history.split("\n").filter { it.isNotBlank() }.takeLast(3).forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = expr.ifBlank { "0" },
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            if (settings.livePreview) {
                Text(
                    text = if (preview.isNotEmpty()) "= ${pretty(preview, settings.groupDigits)}" else "",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (functionKeys.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                functionKeys.forEach { k ->
                    FilledTonalButton(
                        onClick = { expr += insertFor(k) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(40.dp),
                    ) { Text(label(k)) }
                }
            }
        }

        PAD.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { key ->
                    val isOp = key in listOf("/", "*", "-", "+", "=", "%")
                    Button(
                        onClick = {
                            when (key) {
                                "C" -> expr = ""
                                "del" -> if (expr.isNotEmpty()) expr = expr.dropLast(1)
                                "=" -> {
                                    val shown = try {
                                        val value = Evaluator.evaluate(
                                            expr, degrees, mapOf("ans" to ans),
                                        )
                                        ans = value
                                        Format.number(value)
                                    } catch (e: Exception) {
                                        "error"
                                    }
                                    history = (history + "\n${expr.ifBlank { "0" }} = $shown")
                                        .trim().split("\n").takeLast(20).joinToString("\n")
                                    expr = shown
                                }
                                "neg" -> expr = if (expr.startsWith("-")) expr.drop(1) else "-$expr"
                                else -> expr += key
                            }
                        },
                        colors = if (key == "=") {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = if (isOp) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(label(key), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

/** Adds thousands separators to the integer part when the user wants them. */
private fun pretty(text: String, group: Boolean): String {
    if (!group) return text
    val neg = text.startsWith("-")
    val body = if (neg) text.substring(1) else text
    val dot = body.indexOf('.')
    val intPart = if (dot >= 0) body.substring(0, dot) else body
    val rest = if (dot >= 0) body.substring(dot) else ""
    if (intPart.isEmpty() || intPart.any { !it.isDigit() }) return text
    val grouped = intPart.reversed().chunked(3).joinToString(",").reversed()
    return (if (neg) "-" else "") + grouped + rest
}

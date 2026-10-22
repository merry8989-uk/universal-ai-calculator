package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import com.merry8989uk.universalcalculator.settings.SettingsState
import com.merry8989uk.universalcalculator.ui.theme.Amber
import com.merry8989uk.universalcalculator.ui.theme.Coral
import com.merry8989uk.universalcalculator.ui.theme.Cyan
import com.merry8989uk.universalcalculator.ui.theme.Emerald
import com.merry8989uk.universalcalculator.ui.theme.Indigo
import com.merry8989uk.universalcalculator.ui.theme.Orange
import com.merry8989uk.universalcalculator.ui.theme.Violet

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

private fun isOperator(k: String) = k in listOf("/", "*", "-", "+", "%")
private fun isClear(k: String) = k == "C" || k == "del"

private fun insertFor(key: String): String = when (key) {
    "sin", "cos", "tan", "asin", "acos", "atan" -> "$key("
    "ln" -> "ln("
    "log" -> "log("
    "sqrt" -> "sqrt("
    "cbrt" -> "cbrt("
    "x^2" -> "^2"
    "x^y" -> "^"
    "pi" -> "pi"
    "e" -> "e"
    "i" -> "i"
    "x" -> "x"
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

/** A keypad key with a gradient fill. */
@Composable
private fun KeyButton(
    label: String,
    colors: List<Color>,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
    }
}

@Composable
fun CalculatorScreen(
    title: String,
    functionKeys: List<String>,
    settings: SettingsState,
    onAskAi: (String) -> Unit,
) {
    var expr by rememberSaveable { mutableStateOf("") }
    var ans by rememberSaveable { mutableStateOf(0.0) }
    var degrees by rememberSaveable { mutableStateOf(settings.degreesMode) }
    var history by rememberSaveable { mutableStateOf("") }
    var memory by rememberSaveable { mutableStateOf(0.0) }
    var showHistory by rememberSaveable { mutableStateOf(false) }

    val preview: String = if (expr.isBlank()) "" else try {
        Format.number(Evaluator.evaluate(expr, degrees, mapOf("ans" to ans)))
    } catch (e: Exception) {
        ""
    }

    fun currentValue(): Double = try {
        if (expr.isBlank()) ans else Evaluator.evaluate(expr, degrees, mapOf("ans" to ans))
    } catch (e: Exception) {
        ans
    }

    if (showHistory) {
        HistoryScreen(
            history = history,
            onBack = { showHistory = false },
            onClear = { history = ""; showHistory = false },
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showHistory = true }) { Text("History") }
                TextButton(onClick = { degrees = !degrees }) {
                    Text(if (degrees) "DEG" else "RAD")
                }
            }
        }

        // Memory row (MC / M+ / M- / MR).
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = { memory = 0.0 }, modifier = Modifier.weight(1f)) { Text("MC") }
            FilledTonalButton(onClick = { memory += currentValue() }, modifier = Modifier.weight(1f)) { Text("M+") }
            FilledTonalButton(onClick = { memory -= currentValue() }, modifier = Modifier.weight(1f)) { Text("M\u2212") }
            FilledTonalButton(onClick = { expr = Format.number(memory) }, modifier = Modifier.weight(1f)) { Text("MR") }
        }
        if (memory != 0.0) {
            Text(
                "M = ${Format.number(memory)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Colourful display panel.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                .padding(14.dp),
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
            TextButton(
                onClick = {
                    val q = if (expr.isBlank()) "" else "Explain this calculation and the result: $expr = $preview"
                    onAskAi(q)
                },
                enabled = expr.isNotBlank(),
            ) { Text("Ask AI about this") }
        }

        if (functionKeys.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                functionKeys.forEach { k ->
                    KeyButton(
                        label = label(k),
                        colors = listOf(Color(0xFFFFD54F), Amber),
                        contentColor = Color(0xFF3A2600),
                        modifier = Modifier
                            .height(50.dp)
                            .defaultMinSize(minWidth = 68.dp),
                        onClick = { expr += insertFor(k) },
                    )
                }
            }
        }

        PAD.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { key ->
                    val colors: List<Color> = when {
                        key == "=" -> listOf(Cyan, Emerald)
                        isClear(key) -> listOf(Orange, Coral)
                        isOperator(key) -> listOf(Violet, Indigo)
                        else -> listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    val content = if (key == "=" || isClear(key) || isOperator(key)) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    KeyButton(
                        label = label(key),
                        colors = colors,
                        contentColor = content,
                        modifier = Modifier.weight(1f).height(56.dp),
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
                                    DiagnosticsLogger.log("Calc", "${expr.ifBlank { "0" }} = $shown")
                                    if (settings.historyEnabled) {
                                        history = (history + "\n${expr.ifBlank { "0" }} = $shown")
                                            .trim().split("\n").takeLast(200).joinToString("\n")
                                    }
                                    expr = shown
                                }
                                "neg" -> expr = if (expr.startsWith("-")) expr.drop(1) else "-$expr"
                                else -> expr += key
                            }
                        },
                    )
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

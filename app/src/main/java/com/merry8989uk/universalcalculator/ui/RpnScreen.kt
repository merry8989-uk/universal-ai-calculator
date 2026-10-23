package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.Rpn
import com.merry8989uk.universalcalculator.ui.theme.Amber
import com.merry8989uk.universalcalculator.ui.theme.Coral
import com.merry8989uk.universalcalculator.ui.theme.Cyan
import com.merry8989uk.universalcalculator.ui.theme.Emerald
import com.merry8989uk.universalcalculator.ui.theme.Indigo
import com.merry8989uk.universalcalculator.ui.theme.Orange
import com.merry8989uk.universalcalculator.ui.theme.Violet

private val RPN_PAD = listOf(
    listOf("7", "8", "9", "/"),
    listOf("4", "5", "6", "*"),
    listOf("1", "2", "3", "-"),
    listOf("0", ".", "neg", "+"),
    listOf("enter", "sqrt", "inv", "^"),
    listOf("swap", "drop", "dup", "clear"),
)

private fun rpnLabel(k: String): String = when (k) {
    "/" -> "\u00F7"
    "*" -> "\u00D7"
    "-" -> "\u2212"
    "sqrt" -> "\u221A"
    "inv" -> "1/x"
    "neg" -> "\u00B1"
    "enter" -> "Enter"
    "swap" -> "\u21C4"
    "drop" -> "Drop"
    "dup" -> "Dup"
    "clear" -> "Clear"
    else -> k
}

/** Reverse-Polish (stack) calculator. */
@Composable
fun RpnScreen(onBack: () -> Unit) {
    var stack by remember { mutableStateOf(emptyList<Double>()) }
    var entry by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun pushEntry() {
        val v = entry.toDoubleOrNull()
        if (v != null) {
            stack = Rpn.push(stack, v)
            entry = ""
        }
    }

    fun press(key: String) {
        error = ""
        try {
            when {
                key == "clear" -> { entry = ""; stack = emptyList() }
                key == "drop" -> { if (entry.isNotEmpty()) entry = "" else stack = Rpn.apply(stack, "drop") }
                key == "dup" -> { pushEntry(); stack = Rpn.apply(stack, "dup") }
                key == "swap" -> { pushEntry(); stack = Rpn.apply(stack, "swap") }
                key == "neg" -> entry = if (entry.startsWith("-")) entry.drop(1) else "-$entry"
                key == "enter" -> pushEntry()
                key.length == 1 && (key[0].isDigit() || key[0] == '.') -> entry += key
                else -> { pushEntry(); stack = Rpn.apply(stack, key) }
            }
        } catch (e: Exception) {
            error = e.message ?: "Error"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RPN (stack)", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("\u2190 Back") }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End,
        ) {
            Text("stack: ${stack.size}", style = MaterialTheme.typography.labelSmall)
            stack.takeLast(4).forEach {
                Text(
                    Format.number(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                entry.ifBlank { "0" },
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        Text(
            "Type a number, press Enter to push, then apply an operation.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 6.dp),
        )

        RPN_PAD.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { key ->
                    val colors: List<Color> = when (key) {
                        "enter" -> listOf(Cyan, Emerald)
                        "clear" -> listOf(Orange, Coral)
                        "swap", "drop", "dup" -> listOf(Amber, Orange)
                        "/", "*", "-", "+", "^" -> listOf(Violet, Indigo)
                        else -> listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    val content = if (key in listOf("enter", "clear", "swap", "drop", "dup", "/", "*", "-", "+", "^")) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(colors))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                            .clickable { press(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(rpnLabel(key), color = content, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

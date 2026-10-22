package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Converters

private val MODES = listOf(
    "Roman \u2192 number", "Number \u2192 Roman", "Number \u2192 words",
    "Text \u2192 binary", "Binary \u2192 text", "Text \u2192 hex", "Hex \u2192 text",
    "Text \u2192 ASCII", "ASCII \u2192 text",
    "RGB \u2192 hex", "Hex \u2192 RGB", "Timestamp \u2192 date", "Date \u2192 timestamp",
)

@Composable
fun ConverterScreen(onBack: () -> Unit) {
    var mode by remember { mutableStateOf(MODES.first()) }
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun convert() {
        error = null
        output = try {
            when (mode) {
                "Roman \u2192 number" -> Converters.fromRoman(input).toString()
                "Number \u2192 Roman" -> Converters.toRoman(input.trim().toIntOrNull() ?: 0)
                "Number \u2192 words" -> Converters.numberToWords(input.trim().toLongOrNull() ?: 0L)
                "Text \u2192 binary" -> Converters.textToBinary(input)
                "Binary \u2192 text" -> Converters.binaryToText(input)
                "Text \u2192 hex" -> Converters.textToHex(input)
                "Hex \u2192 text" -> Converters.hexToText(input)
                "Text \u2192 ASCII" -> Converters.textToAscii(input)
                "ASCII \u2192 text" -> Converters.asciiToText(input)
                "RGB \u2192 hex" -> {
                    val parts = input.split(',').map { it.trim().toIntOrNull() ?: 0 }
                    Converters.rgbToHex(parts.getOrElse(0) { 0 }, parts.getOrElse(1) { 0 }, parts.getOrElse(2) { 0 })
                }
                "Hex \u2192 RGB" -> {
                    val (r, g, b) = Converters.hexToRgb(input)
                    "R $r, G $g, B $b"
                }
                "Timestamp \u2192 date" -> Converters.timestampToDate(input.trim().toLongOrNull() ?: 0L)
                "Date \u2192 timestamp" -> Converters.dateToTimestamp(input).toString()
                else -> ""
            }
        } catch (e: Exception) {
            error = e.message ?: "Could not convert"
            ""
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Converters", style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MODES.forEach { m ->
                FilterChip(selected = mode == m, onClick = { mode = m; output = ""; error = null }, label = { Text(m) })
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(hintFor(mode)) },
            modifier = Modifier.fillMaxWidth(),
        )

        TextButton(onClick = { convert() }) { Text("Convert") }

        Text(
            output.ifEmpty { "\u2014" },
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

private fun hintFor(mode: String): String = when (mode) {
    "Roman \u2192 number" -> "e.g. XIV"
    "Number \u2192 Roman" -> "e.g. 2026"
    "Number \u2192 words" -> "e.g. 125000"
    "Text \u2192 binary", "Text \u2192 hex", "Text \u2192 ASCII" -> "Type text"
    "Binary \u2192 text" -> "01001000 01101001"
    "Hex \u2192 text" -> "48 69"
    "ASCII \u2192 text" -> "72 105"
    "RGB \u2192 hex" -> "21, 101, 192"
    "Hex \u2192 RGB" -> "#1565C0"
    "Timestamp \u2192 date" -> "1700000000"
    else -> "2026-01-31"
}

package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Unit
import com.merry8989uk.universalcalculator.calc.Units
import com.merry8989uk.universalcalculator.settings.SettingsState

/**
 * The unit converter.
 *
 * The answer is written by [displayValue], the same formatter the calculator
 * uses, so 3000000.5 becomes 3.000.000,5 here exactly when it does there. The
 * converter used to print its own way, which meant the same value looked like
 * two different numbers depending on which screen you were standing on.
 */
@Composable
fun ConvertScreen(
    settings: SettingsState = SettingsState(),
    onExplain: (String) -> kotlin.Unit = {},
) {
    var catIndex by remember { mutableIntStateOf(0) }
    val category = Units.categories[catIndex]
    var fromIndex by remember { mutableIntStateOf(0) }
    var toIndex by remember { mutableIntStateOf(if (category.units.size > 1) 1 else 0) }
    var input by remember { mutableStateOf("1") }

    // Keep unit indices valid when the category changes.
    val from = category.units.getOrElse(fromIndex) { category.units.first() }
    val to = category.units.getOrElse(toIndex) { category.units.first() }

    val value = input.toDoubleOrNull()
    val result = if (value == null) null else Units.convert(value, from, to)

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Unit converter", style = MaterialTheme.typography.titleLarge)
        Text(
            "${Units.categories.size} categories, ${Units.categories.sumOf { it.units.size }} units",
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Units.categories.forEachIndexed { i, c ->
                FilterChip(
                    selected = catIndex == i,
                    onClick = {
                        catIndex = i
                        fromIndex = 0
                        toIndex = if (c.units.size > 1) 1 else 0
                    },
                    label = { Text(c.name) },
                )
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Value") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UnitPicker("From", category.units, fromIndex, Modifier.weight(1f)) { fromIndex = it }
            OutlinedButton(onClick = {
                val t = fromIndex; fromIndex = toIndex; toIndex = t
            }) { Text("\u21C4") }
            UnitPicker("To", category.units, toIndex, Modifier.weight(1f)) { toIndex = it }
        }

        // the answer is the thing people came for; it has to be takeable
        Copyable(
            text = result?.let { settings.displayValue(it) } ?: "\u2014",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
        )
        if (result != null) {
            val line = "${settings.displayValue(value ?: 0.0)} ${from.symbol} = " +
                "${settings.displayValue(result)} ${to.symbol}"
            Copyable(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CopyShareRow(text = line, subject = "Unit conversion")
        }
    }

    // The assistant, draggable so it can be moved off whatever is being read.
    val shown = result?.let {
        "${settings.displayValue(value ?: 0.0)} ${from.symbol} = " +
            "${settings.displayValue(it)} ${to.symbol}"
    }.orEmpty()
    ExplainBubble(
        onExplain = { onExplain(explainQuestion("unit conversion", shown)) },
        modifier = Modifier.fillMaxSize(),
    )
    }
}

@Composable
private fun UnitPicker(
    label: String,
    units: List<Unit>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> kotlin.Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = units.getOrElse(selectedIndex) { units.first() }
    Box(modifier = modifier) {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${selected.symbol}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEachIndexed { i, u ->
                DropdownMenuItem(
                    text = { Text("${u.name} (${u.symbol})") },
                    onClick = { onSelect(i); expanded = false },
                )
            }
        }
    }
}

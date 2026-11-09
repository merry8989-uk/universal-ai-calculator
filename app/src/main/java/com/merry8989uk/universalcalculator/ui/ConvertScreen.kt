package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Unit
import com.merry8989uk.universalcalculator.calc.UnitHit
import com.merry8989uk.universalcalculator.calc.UnitSearch
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
    var query by remember { mutableStateOf("") }

    // Keep unit indices valid when the category changes.
    val from = category.units.getOrElse(fromIndex) { category.units.first() }
    val to = category.units.getOrElse(toIndex) { category.units.first() }

    val value = input.toDoubleOrNull()
    val result = if (value == null) null else Units.convert(value, from, to)

    // Typing a name, a symbol or a category is the fast way in; the chips and
    // the dropdowns stay underneath, so browsing is still there once the field
    // is cleared.
    val searching = query.isNotBlank()
    val hits = remember(query) { UnitSearch.hits(query, limit = SEARCH_LIMIT) }

    // Taking a result sets the "from" unit and leaves the other end where it
    // was, unless that would leave both ends on the same unit — a conversion of
    // a unit into itself is not a conversion.
    val pick: (UnitHit) -> kotlin.Unit = { hit ->
        val units = hit.category.units
        catIndex = hit.categoryIndex
        fromIndex = hit.unitIndex
        toIndex = when {
            units.size <= 1 -> 0
            toIndex in units.indices && toIndex != hit.unitIndex -> toIndex
            hit.unitIndex == 0 -> 1
            else -> 0
        }
        query = ""
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Unit converter", style = MaterialTheme.typography.titleLarge)
        Text(
            "${Units.categories.size} categories, ${Units.categories.sumOf { it.units.size }} units",
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Find a unit") },
                placeholder = { Text("molar, kg, \u00B0C, pressure") },
                singleLine = true,
                // Search on the keyboard's own key, so a match can be taken
                // without ever leaving the keyboard.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { hits.firstOrNull()?.let(pick) },
                    onDone = { hits.firstOrNull()?.let(pick) },
                ),
                modifier = Modifier.weight(1f),
            )
            if (query.isNotEmpty()) {
                TextButton(onClick = { query = "" }) { Text("Clear") }
            }
        }

        if (searching) {
            // What is selected stays on screen while the results scroll, so
            // searching can never lose the unit the user was already on.
            Text(
                "Now on: ${category.name} \u00B7 ${from.name} (${from.symbol})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (hits.isEmpty()) {
                Text(
                    "No unit matches \u201C$query\u201D. Clear the field to browse the categories.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                ) {
                    hits.forEach { hit -> SearchResultRow(hit, onPick = pick) }
                }
            }
        } else {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Value") },
                    singleLine = true,
                    // Decimal, not Number: a number keyboard has no decimal
                    // point on most IMEs, and the amount is often not whole.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                // The decimal keypad has no minus on most keyboards either, so
                // the sign gets a key of its own. Typing "-" still works.
                OutlinedButton(onClick = {
                    input = when {
                        input.startsWith("-") -> input.removePrefix("-")
                        input.isEmpty() -> "-"
                        else -> "-$input"
                    }
                }) { Text("\u00B1") }
            }

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

/** How many results a search shows before it stops: enough to scroll, not a list to page through. */
private const val SEARCH_LIMIT = 60

/**
 * One search result: the unit, its symbol, and the category it sits in, because
 * a symbol like "m" exists in more than one category and the category is what
 * tells the two apart.
 */
@Composable
private fun SearchResultRow(hit: UnitHit, onPick: (UnitHit) -> kotlin.Unit) {
    // clickable, so the row takes focus: the list can be walked with the
    // keyboard alone and taken with Enter, the same as with a tap.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick(hit) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${hit.unit.name} (${hit.unit.symbol})", style = MaterialTheme.typography.bodyLarge)
        Text(
            hit.category.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

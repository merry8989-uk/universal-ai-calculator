package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Formula
import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.OutputSpec

private data class Tool(val id: String, val title: String, val subtitle: String)

private val TOOLS = listOf(
    Tool("statistics", "Statistics", "Mean, median, SD, quartiles, skewness and regression."),
    Tool("matrix", "Matrix calculator", "Add, subtract, multiply, transpose, determinant and inverse."),
    Tool("calculus", "Calculus", "Numerical derivative, second derivative, integral and summation."),
    Tool("graph", "Graphing", "Plot y = f(x) on a live canvas."),
    Tool("converter", "Converters", "Roman numerals, words, binary/hex/ASCII text, colour codes, timestamps."),
    Tool("date", "Date calculator", "Difference, age, working days, week number, add/subtract, timestamps."),
    Tool("programmer", "Programmer (base & bitwise)", "Base conversion and bitwise operations."),
    Tool("currency", "Currency converter", "Live exchange rates between world currencies."),
)

@Composable
fun FormulasScreen() {
    var selected by remember { mutableStateOf<Formula?>(null) }
    var tool by remember { mutableStateOf("") }

    when {
        selected != null -> FormulaDetailScreen(selected!!) { selected = null }
        tool == "statistics" -> StatisticsScreen { tool = "" }
        tool == "matrix" -> MatrixScreen { tool = "" }
        tool == "calculus" -> CalculusScreen { tool = "" }
        tool == "graph" -> GraphScreen { tool = "" }
        tool == "converter" -> ConverterScreen { tool = "" }
        tool == "date" -> DateScreen { tool = "" }
        tool == "programmer" -> ProgrammerScreen { tool = "" }
        tool == "currency" -> CurrencyScreen { tool = "" }
        else -> FormulaList(onOpen = { selected = it }, onTool = { tool = it })
    }
}

@Composable
private fun FormulaList(onOpen: (Formula) -> Unit, onTool: (String) -> Unit) {
    var category by remember { mutableStateOf("Tools") }
    var query by remember { mutableStateOf("") }
    val searching = query.isNotBlank()
    val list = FormulaLibrary.byCategory(category)
    val results = if (searching) {
        FormulaLibrary.formulas.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
        }
    } else emptyList()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Calculators", style = MaterialTheme.typography.titleLarge)
        Text(
            "${FormulaLibrary.formulas.size} built-in calculators + ${TOOLS.size} tools",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search calculators") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        if (!searching) androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = category == "Tools",
                onClick = { category = "Tools" },
                label = { Text("Tools") },
            )
            FormulaLibrary.categories.forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = c },
                    label = { Text(c) },
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (searching) {
                if (results.isEmpty()) {
                    item {
                        Text(
                            "No calculator matches your search.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                items(results) { f -> FormulaCard(f, onOpen) }
            } else {
                if (category == "Tools") {
                    items(TOOLS) { t ->
                        ToolCard(t.title, t.subtitle) { onTool(t.id) }
                    }
                }
                items(list) { f -> FormulaCard(f, onOpen) }
            }
        }
    }
}

@Composable
private fun FormulaCard(f: Formula, onOpen: (Formula) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(f) }) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(f.title, style = MaterialTheme.typography.titleMedium)
            Text(f.description, style = MaterialTheme.typography.bodySmall)
            Text(
                f.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ToolCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FormulaDetailScreen(formula: Formula, onBack: () -> Unit) {
    var values by remember(formula.id) {
        mutableStateOf(formula.inputs.associate { it.key to it.default.toString() })
    }

    val parsed: Map<String, Double> = values.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
    val results: List<OutputSpec> = remember(parsed) {
        try {
            formula.compute(parsed)
        } catch (e: Exception) {
            listOf(OutputSpec("Error", 0.0, note = e.message ?: "Could not compute"))
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text(formula.title, style = MaterialTheme.typography.titleLarge)
        Text(formula.description, style = MaterialTheme.typography.bodyMedium)
        Text(formula.category, style = MaterialTheme.typography.bodySmall)

        LazyColumn(
            modifier = Modifier.weight(1f).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(formula.inputs) { spec ->
                OutlinedTextField(
                    value = values[spec.key] ?: "",
                    onValueChange = { values = values + (spec.key to it) },
                    label = { Text(spec.label) },
                    supportingText = if (spec.hint.isNotEmpty()) {
                        { Text(spec.hint) }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { HorizontalDivider() }
            items(results) { r ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(r.label, style = MaterialTheme.typography.bodySmall)
                    Text(
                        Format.number(r.value) + (if (r.unit.isNotEmpty()) " ${r.unit}" else ""),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                    )
                    if (r.note.isNotEmpty()) {
                        Text(r.note, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

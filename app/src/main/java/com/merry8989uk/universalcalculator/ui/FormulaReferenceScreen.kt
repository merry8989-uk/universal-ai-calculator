package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.data.FormulaReference
import com.merry8989uk.universalcalculator.formula.CalculationRequest
import com.merry8989uk.universalcalculator.formula.CalculationResult
import com.merry8989uk.universalcalculator.formula.FormulaCalculator
import com.merry8989uk.universalcalculator.formula.FormulaDefinition
import com.merry8989uk.universalcalculator.formula.FormulaType
import com.merry8989uk.universalcalculator.formula.RawFormula

/**
 * The formula reference, with a working calculator on every formula that has one.
 *
 * The distinction the engine makes is the one the screen shows: a formula that
 * can be worked out gets boxes to fill and a Calculate button, an identity gets
 * a Check button, and everything else stays a reference card. Nothing is offered
 * as a calculator that cannot actually calculate — that was the whole point of
 * classifying them in the first place.
 */
@Composable
fun FormulaReferenceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val all = remember { FormulaReference.load(context) }
    val cats = remember { FormulaReference.categories(all) }
    val definitions = remember {
        FormulaReference.definitions(context)
            .associateBy { it.category + "\u0000" + it.title }
    }
    val readyCount = remember { definitions.values.count { it.isReady() } }

    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<String?>(null) }
    var onlyReady by remember { mutableStateOf(false) }

    val shown = remember(query, cat, onlyReady) {
        FormulaReference.search(all, query, cat).filter { f ->
            !onlyReady || definitions[f.category + "\u0000" + f.title]?.isReady() == true
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Formula reference", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("\u2190 Back") }
        }
        Text(
            "${all.size} formulas — $readyCount of them can be worked out here.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search a formula, e.g. cos, integral, Bayes") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = onlyReady,
                onClick = { onlyReady = !onlyReady },
                label = { Text("Can calculate ($readyCount)") },
            )
            FilterChip(selected = cat == null, onClick = { cat = null }, label = { Text("All (${all.size})") })
            cats.forEach { c ->
                FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) })
            }
        }

        if (shown.isEmpty()) {
            Text("No formula matches that search.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown) { f ->
                    val definition = definitions[f.category + "\u0000" + f.title]
                    FormulaCard(f, definition)
                }
            }
        }
    }
}

/** True when this formula can be acted on rather than only read. */
private fun FormulaDefinition.isReady(): Boolean =
    type == FormulaType.DIRECT_CALCULATOR || type == FormulaType.CHECKER

@Composable
private fun FormulaCard(f: RawFormula, definition: FormulaDefinition?) {
    var open by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<CalculationResult?>(null) }
    val typed = remember { mutableStateMapOf<String, String>() }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(f.title, style = MaterialTheme.typography.titleMedium)
            Text(
                f.formula,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    f.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (definition != null && definition.isReady()) {
                    TextButton(onClick = {
                        open = !open
                        if (!open) result = null
                    }) {
                        Text(
                            when {
                                open -> "Close"
                                definition.type == FormulaType.CHECKER -> "Check"
                                else -> "Calculate"
                            },
                        )
                    }
                }
            }

            if (open && definition != null) {
                if (definition.type == FormulaType.CHECKER) {
                    Text(
                        "An identity: put the same values in both sides and see whether they agree.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                definition.inputs.forEach { spec ->
                    OutlinedTextField(
                        value = typed[spec.id] ?: spec.sample.toPlain(),
                        onValueChange = { typed[spec.id] = it },
                        label = { Text(spec.label) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    )
                }
                Button(onClick = {
                    val values = HashMap<String, Double>()
                    var bad: String? = null
                    for (spec in definition.inputs) {
                        val text = typed[spec.id] ?: spec.sample.toPlain()
                        val value = text.trim().toDoubleOrNull()
                        if (value == null) {
                            bad = "${spec.label} is not a number"
                            break
                        }
                        values[spec.id] = value
                    }
                    val problem = bad
                    result = if (problem != null) {
                        CalculationResult.failed(definition.id, problem)
                    } else {
                        FormulaCalculator.calculate(definition, CalculationRequest(definition.id, values))
                    }
                }) {
                    Text(if (definition.type == FormulaType.CHECKER) "Check it" else "Calculate")
                }

                result?.let { r ->
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        if (r.success) {
                            Text(
                                (r.exactValue ?: "") + (r.outputUnit?.let { " $it" } ?: ""),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            r.steps.forEach { step ->
                                Text(
                                    "${step.description}: ${step.expression}" +
                                        if (step.result.isNotEmpty()) " = ${step.result}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                            r.warnings.forEach { w ->
                                Text(w, style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Text(
                                r.error ?: "That did not work.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A number without a trailing ".0", so the box does not read 2.0 for 2. */
private fun Double.toPlain(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()

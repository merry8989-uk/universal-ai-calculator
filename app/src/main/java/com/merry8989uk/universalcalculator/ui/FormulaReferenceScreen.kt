package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.merry8989uk.universalcalculator.data.FormulaReference
import com.merry8989uk.universalcalculator.data.RefFormula
import com.merry8989uk.universalcalculator.formula.CalculationRequest
import com.merry8989uk.universalcalculator.formula.CalculationResult
import com.merry8989uk.universalcalculator.formula.FormulaCalculator
import com.merry8989uk.universalcalculator.formula.FormulaDefinition
import com.merry8989uk.universalcalculator.formula.FormulaType
import com.merry8989uk.universalcalculator.settings.Choice
import com.merry8989uk.universalcalculator.settings.ChoiceRow

/**
 * The formula library, arranged to be used rather than read.
 *
 * Every formula that can be worked out shows its inputs and answers as the
 * numbers are typed — there is no Calculate button to press, because a button
 * between the question and the answer is a step nobody wants. What a formula
 * prints stays exactly as the book prints it, in a box of its own, so it can be
 * read and copied without fighting the interface around it.
 *
 * The badge on each card is the honest one: green where the app can work it out,
 * amber where it is something to read, know or solve elsewhere. Nothing is
 * offered as a calculator that cannot calculate.
 */
@Composable
fun FormulaReferenceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val all = remember { FormulaReference.load(context) }
    val cats = remember { FormulaReference.categories(all) }
    val definitions = remember {
        FormulaReference.definitions(context).associateBy { it.category + "\u0000" + it.title }
    }
    val readyCount = remember { definitions.values.count { it.canWork() } }

    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<String?>(null) }
    var onlyReady by remember { mutableStateOf(false) }

    val shown = remember(query, cat, onlyReady) {
        FormulaReference.search(all, query, cat).filter { f ->
            !onlyReady || definitions[f.category + "\u0000" + f.title]?.canWork() == true
        }
    }

    // how many in each category, so the chips say something useful
    val counts = remember {
        val m = LinkedHashMap<String, Int>()
        cats.forEach { c -> m[c] = all.count { it.category == c } }
        m
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---- header ----
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text("Formulas", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${all.size} formulas \u00B7 $readyCount you can work out here",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onBack) { Text("\u2190 Back") }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search \u2014 try cos, Bayes, interest, variance") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }

        // ---- category chips ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = onlyReady,
                onClick = { onlyReady = !onlyReady },
                label = { Text("Can work out  $readyCount") },
            )
            FilterChip(
                selected = cat == null,
                onClick = { cat = null },
                label = { Text("All  ${all.size}") },
            )
            cats.forEach { c ->
                FilterChip(
                    selected = cat == c,
                    onClick = { cat = c },
                    label = { Text("$c  ${counts[c] ?: 0}") },
                )
            }
        }

        Text(
            "${shown.size} shown",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        // ---- the list ----
        if (shown.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Nothing matches that search.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 8.dp,
                    bottom = 28.dp,
                ),
            ) {
                items(shown, key = { it.category + "\u0000" + it.title }) { f ->
                    FormulaCard(f, definitions[f.category + "\u0000" + f.title])
                }
            }
        }
    }
}

/** True when this formula can be acted on rather than only read. */
private fun FormulaDefinition.canWork(): Boolean =
    type == FormulaType.DIRECT_CALCULATOR || type == FormulaType.CHECKER

/** Green for a calculator, amber for something to read. */
private val CalcsBadge = Color(0xFF2EA043)
private val ReferenceBadge = Color(0xFFD29922)

@Composable
private fun FormulaCard(f: RefFormula, definition: FormulaDefinition?) {
    val workable = definition?.canWork() == true && definition.expression != null
    val typed = remember { mutableStateMapOf<String, String>() }
    val subject = f.title

    // Worked out on every change, so there is nothing to press.
    val result: CalculationResult? = if (workable) {
        val values = HashMap<String, Double>()
        var bad: String? = null
        for (spec in definition!!.inputs) {
            val text = typed[spec.id] ?: spec.sample.toPlain()
            val v = text.trim().toDoubleOrNull()
            if (v == null) {
                bad = "${spec.label} is not a number"
                break
            }
            values[spec.id] = v
        }
        if (bad != null) {
            CalculationResult.failed(definition.id, bad)
        } else {
            FormulaCalculator.calculate(definition, CalculationRequest(definition.id, values))
        }
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(14.dp),
    ) {
        Text(
            f.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        // the formula exactly as the book prints it
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Copyable(
                text = f.formula,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Badge(
                text = if (workable) "works it out" else "to read",
                colour = if (workable) CalcsBadge else ReferenceBadge,
            )
            Text(
                f.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (workable && definition != null) {
            if (definition.type == FormulaType.CHECKER) {
                Text(
                    "An identity \u2014 both sides should agree.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            // two per row, so a phone does not have to scroll sideways
            definition.inputs.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    pair.forEach { spec ->
                        OutlinedTextField(
                            value = typed[spec.id] ?: spec.sample.toPlain(),
                            onValueChange = { typed[spec.id] = it },
                            label = { Text(spec.label, style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Box(modifier = Modifier.weight(1f))
                }
            }

            result?.let { r ->
                val ok = r.success
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Column {
                        Text(
                            definition.output.label +
                                (definition.output.unit?.let { " ($it)" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (ok) {
                            Copyable(
                                text = (r.exactValue ?: "") +
                                    (r.outputUnit?.let { " $it" } ?: ""),
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // the working, when the engine produced any
                            if (r.steps.isNotEmpty()) {
                                Text(
                                    r.steps.joinToString("  \u00B7  ") { it.description },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val context = LocalContext.current
            var copied by remember { mutableStateOf(false) }
            TextButton(onClick = {
                copyToClipboard(context, subject, f.formula)
                copied = true
            }) {
                Text(if (copied) "Copied" else "Copy", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = {
                val answer = result?.takeIf { it.success }?.exactValue
                shareText(
                    context,
                    subject,
                    if (answer.isNullOrBlank()) f.formula else "$f.formula\n= $answer",
                )
            }) {
                Text("Share", style = MaterialTheme.typography.labelMedium)
            }
            if (copied) {
                androidx.compose.runtime.LaunchedEffect(copied) {
                    kotlinx.coroutines.delay(1400)
                    copied = false
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, colour: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colour.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = colour,
        )
    }
}

/** A number without a trailing ".0", so the box does not read 2.0 for 2. */
private fun Double.toPlain(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()

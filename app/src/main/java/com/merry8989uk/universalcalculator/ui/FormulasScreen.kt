package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.calc.CustomCalculator
import com.merry8989uk.universalcalculator.data.CustomCalcStore
import com.merry8989uk.universalcalculator.diagnostics.Analytics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Formula
import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.OutputSpec
import com.merry8989uk.universalcalculator.ui.theme.CategoryColors
import com.merry8989uk.universalcalculator.ui.theme.Cyan
import com.merry8989uk.universalcalculator.ui.theme.Emerald
import com.merry8989uk.universalcalculator.ui.theme.Indigo
import com.merry8989uk.universalcalculator.ui.theme.Orange
import com.merry8989uk.universalcalculator.ui.theme.Pink
import com.merry8989uk.universalcalculator.ui.theme.Teal
import com.merry8989uk.universalcalculator.ui.theme.Violet

private data class Tool(val id: String, val title: String, val subtitle: String)

private val TOOLS = listOf(
    Tool("reference", "Formula reference", "Every formula the app knows \u2014 maths, physics and chemistry."),
    Tool("statistics", "Statistics", "Mean, median, SD, quartiles, skewness and regression."),
    Tool("distributions", "Distributions & tests", "Normal/t/binomial/Poisson, t-tests and confidence intervals."),
    Tool("complex", "Complex numbers", "Add, subtract, multiply, divide, magnitude, angle and roots of i."),
    Tool("rpn", "RPN (stack) calculator", "Reverse-Polish entry: push numbers, then apply operations."),
    Tool("matrix", "Matrix calculator", "Add, subtract, multiply, transpose, determinant and inverse."),
    Tool("handwriting", "Handwriting", "Write a calculation with a finger and let the app read it."),
    Tool("solver", "Equation solver", "Linear to quintic, several equations in several unknowns, and inequalities."),
    Tool("table", "Table of values", "What f(x) and g(x) do across a range, row by row."),
    Tool("symbolic", "Symbolic algebra", "Simplify, differentiate, integrate, factor and solve polynomials in x."),
    Tool("limits", "Limits", "The limit of an expression as x approaches a point, one side at a time, or at infinity."),
    Tool("partial", "Partial fractions", "Split a rational function into simple fractions."),
    Tool("calculus", "Calculus", "Numerical derivative, second derivative, integral and summation."),
    Tool("graph", "Graphing", "Plot y = f(x) on a live canvas."),
    Tool("converter", "Converters", "Roman numerals, words, binary/hex/ASCII text, colour codes, timestamps."),
    Tool("date", "Date calculator", "Difference, age, working days, week number, add/subtract, timestamps."),
    Tool("programmer", "Programmer (base & bitwise)", "Base conversion and bitwise operations."),
    Tool("currency", "Currency converter", "Live exchange rates between world currencies."),
)

@Composable
fun FormulasScreen(
    resetSignal: Int = 0,
    /** A tool to open straight away, e.g. "calculus" from the classic pad. */
    requestedTool: String = "",
    onToolRequestUsed: () -> Unit = {},
    onInnerChange: (Boolean) -> Unit = {},
) {
    val analyticsContext = androidx.compose.ui.platform.LocalContext.current
    fun analytics(key: String) = Analytics.event(analyticsContext, key)

    var selected by remember { mutableStateOf<Formula?>(null) }
    var tool by remember { mutableStateOf("") }

    // Pressing the Formulas tab again returns to this hub.
    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) { selected = null; tool = "" }
    }
    // A key on the classic pad can ask for a tool by name.
    LaunchedEffect(requestedTool) {
        if (requestedTool.isNotEmpty()) {
            selected = null
            tool = requestedTool
            onToolRequestUsed()
        }
    }
    // Tell the root whether an inner screen is open, so the back button knows
    // to leave it before it offers to exit. Leaving the tab clears the flag.
    LaunchedEffect(selected, tool) { onInnerChange(selected != null || tool.isNotEmpty()) }
    DisposableEffect(Unit) { onDispose { onInnerChange(false) } }

    val context = androidx.compose.ui.platform.LocalContext.current
    var custom by remember { mutableStateOf(CustomCalcStore.load(context)) }
    var editing by remember { mutableStateOf<CustomCalculator?>(null) }
    var creating by remember { mutableStateOf(false) }

    fun persist(next: List<CustomCalculator>) {
        custom = next
        CustomCalcStore.save(context, next)
    }

    when {
        creating || editing != null -> CustomCalcEditor(
            existing = editing,
            onSave = { saved ->
                val next = custom.filterNot { it.id == saved.id } + saved
                persist(next)
                creating = false
                editing = null
            },
            onDelete = { gone ->
                persist(custom.filterNot { it.id == gone.id })
                editing = null
            },
            onBack = { creating = false; editing = null },
        )
        selected != null -> FormulaDetailScreen(selected!!) { selected = null }
        tool == "reference" -> FormulaReferenceScreen { tool = "" }
        tool == "statistics" -> StatisticsScreen { tool = "" }
        tool == "distributions" -> DistributionsScreen { tool = "" }
        tool == "complex" -> ComplexScreen { tool = "" }
        tool == "rpn" -> RpnScreen { tool = "" }
        tool == "matrix" -> MatrixScreen { tool = "" }
        tool == "handwriting" -> HandwritingScreen { tool = "" }
        tool == "solver" -> EquationSolverScreen { tool = "" }
        tool == "table" -> TableScreen { tool = "" }
        tool == "symbolic" -> SymbolicScreen { tool = "" }
        tool == "limits" -> LimitsScreen { tool = "" }
        tool == "partial" -> PartialFractionsScreen { tool = "" }
        tool == "calculus" -> CalculusScreen { tool = "" }
        tool == "graph" -> GraphScreen { tool = "" }
        tool == "converter" -> ConverterScreen { tool = "" }
        tool == "date" -> DateScreen { tool = "" }
        tool == "programmer" -> ProgrammerScreen { tool = "" }
        tool == "currency" -> CurrencyScreen { tool = "" }
        else -> FormulaList(
            custom = custom,
            onCreate = { creating = true },
            onEdit = { editing = it },
            onOpen = {
                selected = it
                analytics(it.id)
            },
            onTool = {
                tool = it
                analytics("tool:$it")
            },
        )
    }
}

@Composable
private fun FormulaList(
    custom: List<CustomCalculator>,
    onCreate: () -> Unit,
    onEdit: (CustomCalculator) -> Unit,
    onOpen: (Formula) -> Unit,
    onTool: (String) -> Unit,
) {
    var category by remember { mutableStateOf("Tools") }
    var query by remember { mutableStateOf("") }
    val searching = query.isNotBlank()
    // The user's own calculators behave exactly like the built-in ones.
    val customFormulas = remember(custom) { custom.map { it.toFormula() } }
    val everything = FormulaLibrary.formulas + customFormulas
    val list = if (category == CustomCalculator.CATEGORY) customFormulas
    else FormulaLibrary.byCategory(category)
    val results = if (searching) {
        everything.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
        }
    } else emptyList()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.calculators), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(
                R.string.calculator_count,
                FormulaLibrary.formulas.size,
                custom.size,
                TOOLS.size,
            ),
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.search_hint)) },
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
                label = { Text(stringResource(R.string.tools)) },
            )
            FilterChip(
                selected = category == CustomCalculator.CATEGORY,
                onClick = { category = CustomCalculator.CATEGORY },
                label = { Text(stringResource(R.string.my_calculators)) },
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
                    itemsIndexed(TOOLS) { i, t ->
                        ToolCard(
                            title = t.title,
                            subtitle = t.subtitle,
                            accent = TOOL_COLORS[i % TOOL_COLORS.size],
                        ) { onTool(t.id) }
                    }
                }
                if (category == CustomCalculator.CATEGORY) {
                    item {
                        ToolCard(
                            title = stringResource(R.string.create_calculator),
                            subtitle = stringResource(R.string.create_calculator_blurb),
                            accent = TOOL_COLORS.first(),
                        ) { onCreate() }
                    }
                    if (custom.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_custom_yet),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                items(list) { f -> FormulaCard(f, onOpen) }
                if (category == CustomCalculator.CATEGORY) {
                    items(custom) { c ->
                        ToolCard(
                            title = c.name,
                            subtitle = c.expression,
                            accent = TOOL_COLORS.last(),
                        ) { onEdit(c) }
                    }
                }
            }
        }
    }
}

private val TOOL_COLORS = listOf(Cyan, Violet, Indigo, Emerald, Orange, Teal, Pink, Cyan)

@Composable
private fun FormulaCard(f: Formula, onOpen: (Formula) -> Unit) {
    val accent = CategoryColors.of(f.category)
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(f) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(6.dp).height(72.dp).background(accent))
            Column(modifier = Modifier.padding(14.dp)) {
                Text(f.title, style = MaterialTheme.typography.titleMedium)
                Text(f.description, style = MaterialTheme.typography.bodySmall)
                Text(
                    f.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun ToolCard(title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title. $subtitle"
                role = Role.Button
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(6.dp).height(72.dp).background(accent))
            Column(modifier = Modifier.padding(14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
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

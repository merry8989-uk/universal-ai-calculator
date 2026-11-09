package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.calc.CustomCalculator
import com.merry8989uk.universalcalculator.data.CalcPrefs
import com.merry8989uk.universalcalculator.data.CalcPrefsStore
import com.merry8989uk.universalcalculator.data.CustomCalcStore
import com.merry8989uk.universalcalculator.diagnostics.Analytics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Formula
import com.merry8989uk.universalcalculator.calc.FormulaHindi
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

/** The tools that need computer-algebra mode. */
private val CAS_TOOLS = setOf("symbolic", "limits", "partial", "solver")

/** The two lists that are not categories: what the reader starred, and used last. */
private const val FAVOURITES = "Favourites"
private const val RECENT = "Recent"

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
    Tool("allformulas", "Every formula", "Every calculator and every reference formula, in one page."),
    Tool("currency", "Currency converter", "Live exchange rates between world currencies."),
    Tool("interest", "Interest calculator", "Any rate period and any duration \u2014 daily, weekly, monthly, or two dates."),
)

@Composable
fun FormulasScreen(
    resetSignal: Int = 0,
    /** A tool to open straight away, e.g. "calculus" from the classic pad. */
    requestedTool: String = "",
    onToolRequestUsed: () -> Unit = {},
    onInnerChange: (Boolean) -> Unit = {},
    /** The settings the tools inside read: bit width, complex form, and so on. */
    settings: com.merry8989uk.universalcalculator.settings.SettingsState =
        com.merry8989uk.universalcalculator.settings.SettingsState(),
    /** The assistant, reached from the disc on the formula page. */
    onExplain: (String) -> Unit = {},
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

    // Computer-algebra mode puts the symbolic tools away when it is switched off.
    val shownTools = remember(settings.casMode) {
        if (settings.casMode) TOOLS else TOOLS.filterNot { it.id in CAS_TOOLS }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var custom by remember { mutableStateOf(CustomCalcStore.load(context)) }
    var prefs by remember { mutableStateOf(CalcPrefsStore.load(context)) }

    fun keepPrefs(next: CalcPrefs) {
        prefs = next
        CalcPrefsStore.save(context, next)
    }
    var editing by remember { mutableStateOf<CustomCalculator?>(null) }
    var creating by remember { mutableStateOf(false) }

    fun persist(next: List<CustomCalculator>) {
        custom = next
        CustomCalcStore.save(context, next)
    }

    // Every tool this tab opens is drawn in the studio's colours: they all
    // draw with MaterialTheme, and this hands them the studio's scheme.
    StudioHost {
      // The assistant wraps the whole tab rather than one screen inside it. It
      // was on the formula page only, so a formula opened from the list — or a
      // tool — had no way to reach it, which read as the disc appearing on some
      // formulas and not others.
      androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
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
            selected != null -> FormulaDetailScreen(
                formula = selected!!,
                favourite = selected!!.id in prefs.favourites,
                onToggleFavourite = { keepPrefs(CalcPrefsStore.toggleFavourite(prefs, selected!!.id)) },
                onBack = { selected = null },
            )
            // A formula the app can work out opens its calculator rather than
            // leaving the reader to find it in the tools by hand.
            tool == "reference" -> FormulaReferenceScreen(
                onBack = { tool = "" },
                onOpenTool = { id ->
                    val found = com.merry8989uk.universalcalculator.calc.FormulaLibrary.formulas
                        .firstOrNull { it.id == id }
                    if (found != null) {
                        tool = ""
                        selected = found
                    }
                },
            )
            tool == "statistics" -> StatisticsScreen(
                onBack = { tool = "" },
                frequencyColumn = settings.frequencyColumn,
            )
            tool == "distributions" -> DistributionsScreen { tool = "" }
            tool == "complex" -> ComplexScreen(
                onBack = { tool = "" },
                format = com.merry8989uk.universalcalculator.calc.ComplexFormat
                    .valueOf(settings.complexFormat),
            )
            tool == "rpn" -> RpnScreen { tool = "" }
            tool == "matrix" -> MatrixScreen { tool = "" }
            tool == "handwriting" -> HandwritingScreen { tool = "" }
            tool == "solver" -> EquationSolverScreen(
                onClose = { tool = "" },
                notation = com.merry8989uk.universalcalculator.calc.IntervalNotation
                    .valueOf(settings.intervalNotation),
            )
            tool == "table" -> TableScreen { tool = "" }
            // CAS mode off: the symbolic screens are unreachable, so send the user back
            !settings.casMode && tool in CAS_TOOLS -> {
                tool = ""
                FormulaList(
                    custom = custom,
                    prefs = prefs,
                    onPrefs = { keepPrefs(it) },
                    onCreate = { creating = true },
                    onEdit = { editing = it },
                    onOpen = { selected = it },
                    onTool = { tool = it },
                    onImport = { persist(custom + it) },
                    tools = shownTools,
                )
            }
            tool == "symbolic" -> SymbolicScreen { tool = "" }
            tool == "limits" -> LimitsScreen { tool = "" }
            tool == "partial" -> PartialFractionsScreen { tool = "" }
            tool == "calculus" -> CalculusScreen { tool = "" }
            tool == "graph" -> GraphScreen { tool = "" }
            tool == "converter" -> ConverterScreen { tool = "" }
            tool == "date" -> DateScreen { tool = "" }
            tool == "programmer" -> ProgrammerScreen(
                onBack = { tool = "" },
                width = com.merry8989uk.universalcalculator.calc.BitWidth
                    .valueOf(settings.bitWidth),
            )
            // The converter is the one tool that writes something the user never
            // typed, so it is told which way the master switch is set.
            tool == "currency" -> CurrencyScreen(
                onBack = { tool = "" },
                savingEnabled = settings.dataSavingEnabled,
            )
            tool == "interest" -> InterestScreen { tool = "" }
            tool == "allformulas" -> FormulaReferenceScreen(
                onBack = { tool = "" },
                onOpenTool = { id ->
                    val found = com.merry8989uk.universalcalculator.calc.FormulaLibrary.formulas
                        .firstOrNull { it.id == id }
                    if (found != null) { tool = ""; selected = found }
                },
            )
            else -> FormulaList(
                custom = custom,
                prefs = prefs,
                onPrefs = { keepPrefs(it) },
                onCreate = { creating = true },
                onEdit = { editing = it },
                onImport = { persist(custom + it) },
                tools = shownTools,
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

        ExplainBubble(
            onExplain = {
                // Whatever is open is what gets explained: the formula on screen,
                // the tool that is open, or the list itself.
                val what = when {
                    selected != null -> explainQuestion(
                        "formula",
                        "${selected!!.title} — ${selected!!.description}",
                    )
                    tool.isNotEmpty() -> explainQuestion("tool", tool)
                    else -> explainQuestion("formula library", "")
                }
                onExplain(what)
            },
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        )
      }
    }
}

@Composable
private fun FormulaList(
    custom: List<CustomCalculator>,
    prefs: CalcPrefs = CalcPrefs(),
    onPrefs: (CalcPrefs) -> Unit = {},
    onCreate: () -> Unit,
    onEdit: (CustomCalculator) -> Unit,
    onOpen: (Formula) -> Unit,
    onTool: (String) -> Unit,
    /** Calculators that arrived from another phone, to be added to the saved ones. */
    onImport: (List<CustomCalculator>) -> Unit = {},
    /** The tools on offer, after CAS mode has had its say. */
    tools: List<Tool> = TOOLS,
) {
    var category by remember { mutableStateOf("Tools") }
    var query by remember { mutableStateOf("") }
    var sharing by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    val searching = query.isNotBlank()
    val hindi = readingHindi()
    // The user's own calculators behave exactly like the built-in ones.
    val myRows = remember(custom) { customRows(custom) }
    val customFormulas = remember(myRows) { myRows.map { it.formula } }
    val everything = FormulaLibrary.formulas + customFormulas
    val byId = remember { FormulaLibrary.formulas.associateBy { it.id } }
    val list = when (category) {
        // Their own calculators are drawn from the rows below, once each, since
        // a row here has to run on a tap and edit on a hold and the shared row
        // rendering further down knows only about the tap.
        CustomCalculator.CATEGORY -> emptyList()
        FAVOURITES -> prefs.favourites.mapNotNull { byId[it] }
        RECENT -> prefs.recents.mapNotNull { byId[it] }
        else -> FormulaLibrary.byCategory(category)
    }

    // Opening a calculator is what puts it in the recents, so every way of
    // opening one goes through here.
    fun open(f: Formula) {
        onPrefs(CalcPrefsStore.recordRecent(prefs, f.id))
        onOpen(f)
    }
    val results = if (searching) {
        everything.filter {
            // a Hindi reader types in Hindi, and the row in front of them is in
            // Hindi, so the Hindi words have to match as well
            it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                // "finance", "interest rate", "radius": the words are on the row
                // or in its fields, so searching them has to find it
                it.category.contains(query, ignoreCase = true) ||
                it.inputs.any { spec -> spec.label.contains(query, ignoreCase = true) } ||
                (hindi && (
                    FormulaHindi.title(it.id)?.contains(query, ignoreCase = true) == true ||
                        FormulaHindi.description(it.id)?.contains(query, ignoreCase = true) == true
                    ))
        }
    } else emptyList()

    // The frame and the colours come from the tab that opened this screen, so
    // the hub and the tools behind it are drawn as one app.
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        StudioHeader(
            subtitle = stringResource(
                R.string.calculator_count,
                FormulaLibrary.formulas.size,
                custom.size,
                tools.size,
            ),
            pill = FormulaLibrary.formulas.size.toString(),
        )

        StudioPanel(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp)) {
            Text(
                stringResource(R.string.calculators),
                style = MaterialTheme.typography.titleSmall,
                color = Studio.Text,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!searching) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StudioPill(
                    text = stringResource(R.string.tools),
                    selected = category == "Tools",
                ) { category = "Tools" }
                StudioPill(
                    text = stringResource(R.string.my_calculators),
                    selected = category == CustomCalculator.CATEGORY,
                ) { category = CustomCalculator.CATEGORY }
                StudioPill(
                    text = if (hindi) "पसंदीदा" else "Favourites",
                    selected = category == FAVOURITES,
                ) { category = FAVOURITES }
                StudioPill(
                    text = if (hindi) "हाल के" else "Recent",
                    selected = category == RECENT,
                ) { category = RECENT }
                FormulaLibrary.categories.forEach { c ->
                    StudioPill(
                        text = if (hindi) FormulaHindi.category(c) ?: c else c,
                        selected = category == c,
                    ) { category = c }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (searching) {
                    if (results.isEmpty()) {
                        item {
                            Text(
                                "No calculator matches your search.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Studio.Muted,
                            )
                        }
                    }
                    items(results) { f ->
                        StudioItem(
                            title = calcTitle(hindi, f.id, f.title),
                            subtitle = calcBlurb(hindi, f.id, f.description),
                            accent = CategoryColors.of(f.category),
                            meta = if (hindi) FormulaHindi.category(f.category) ?: f.category else f.category,
                            onClick = { open(f) },
                        )
                    }
                } else {
                    if (category == "Tools") {
                        itemsIndexed(tools) { i, t ->
                            StudioItem(
                                title = if (hindi) FormulaHindi.tool(t.id) ?: t.title else t.title,
                                subtitle = if (hindi) {
                                    FormulaHindi.toolSubtitle(t.id) ?: t.subtitle
                                } else t.subtitle,
                                accent = TOOL_COLORS[i % TOOL_COLORS.size],
                                onClick = { onTool(t.id) },
                            )
                        }
                    }
                    if (category == CustomCalculator.CATEGORY) {
                        item {
                            StudioItem(
                                title = stringResource(R.string.create_calculator),
                                subtitle = stringResource(R.string.create_calculator_blurb),
                                accent = TOOL_COLORS.first(),
                                onClick = onCreate,
                            )
                        }
                        if (custom.isEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.no_custom_yet),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Studio.Muted,
                                )
                            }
                        }
                        // One row per saved calculator, and the row does both
                        // things the calculator has: a tap works it out on the
                        // same page the built-ins open, a hold changes it. The
                        // row says "Hold to edit" because a long press is the
                        // one gesture nothing else on screen advertises.
                        items(myRows) { row ->
                            StudioItem(
                                title = calcTitle(hindi, row.formula.id, row.title),
                                subtitle = calcBlurb(hindi, row.formula.id, row.subtitle),
                                accent = TOOL_COLORS.last(),
                                meta = if (hindi) "देर तक दबाएँ: बदलें" else "Hold to edit",
                                onClick = { open(row.formula) },
                                onLongClick = { onEdit(row.calculator) },
                            )
                        }
                        if (custom.isNotEmpty()) {
                            item {
                                StudioItem(
                                    title = if (hindi) "कैलकुलेटर भेजें" else "Send a calculator",
                                    subtitle = if (hindi) {
                                        "इस डिवाइस से एक कैलकुलेटर दूसरे फ़ोन पर भेजें।"
                                    } else {
                                        "Share one of these with another phone, as text."
                                    },
                                    accent = TOOL_COLORS[1],
                                    onClick = { sharing = true },
                                )
                            }
                        }
                        item {
                            StudioItem(
                                title = if (hindi) "कैलकुलेटर लें" else "Take a calculator",
                                subtitle = if (hindi) {
                                    "किसी के भेजे हुए कैलकुलेटर का टेक्स्ट चिपकाएँ।"
                                } else {
                                    "Paste one that somebody sent you, and it joins this list."
                                },
                                accent = TOOL_COLORS[2],
                                onClick = { importing = true },
                            )
                        }
                    }
                    if ((category == FAVOURITES || category == RECENT) && list.isEmpty()) {
                        item {
                            Text(
                                if (hindi) "अभी यहाँ कुछ नहीं है - कोई कैलकुलेटर खोलें या उसे स्टार करें।"
                                else "Nothing here yet - open a calculator, or star one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Studio.Muted,
                            )
                        }
                    }
                    items(list) { f ->
                        StudioItem(
                            title = calcTitle(hindi, f.id, f.title),
                            subtitle = calcBlurb(hindi, f.id, f.description),
                            accent = CategoryColors.of(f.category),
                            meta = if (hindi) FormulaHindi.category(f.category) ?: f.category else f.category,
                            onClick = { open(f) },
                        )
                    }
                }
            }
        }
    }

    if (sharing) {
        SendCalculatorDialog(
            calculators = custom,
            hindi = hindi,
            onDismiss = { sharing = false },
        )
    }
    if (importing) {
        TakeCalculatorDialog(
            hindi = hindi,
            existing = custom,
            onTake = {
                onImport(it)
                importing = false
            },
            onDismiss = { importing = false },
        )
    }
}

private val TOOL_COLORS = listOf(Cyan, Violet, Indigo, Emerald, Orange, Teal, Pink, Cyan)

/**
 * True when the app is being read in Hindi.
 *
 * The locale comes from the wrapped context, so this answers the same way the
 * strings around it do: chosen in Settings, or inherited from the phone.
 */
@Composable
private fun readingHindi(): Boolean =
    LocalConfiguration.current.locales[0].language == "hi"

/** A calculator's name, in the language the screen is in. */
private fun calcTitle(hindi: Boolean, id: String, english: String): String =
    if (hindi) FormulaHindi.title(id) ?: english else english

/** The line under it, likewise. */
private fun calcBlurb(hindi: Boolean, id: String, english: String): String =
    if (hindi) FormulaHindi.description(id) ?: english else english

@Composable
private fun FormulaDetailScreen(
    formula: Formula,
    favourite: Boolean = false,
    onToggleFavourite: () -> Unit = {},
    onBack: () -> Unit,
) {
    val hindi = readingHindi()
    val context = androidx.compose.ui.platform.LocalContext.current
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

    // A panel on the studio's backdrop: the inputs in the theme's own fields,
    // and each answer on its own result block.
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack) { Text("\u2190 Back") }
            TextButton(onClick = onToggleFavourite) {
                Text(if (favourite) "\u2605" else "\u2606")
            }
            // A picture of the answer, without the phone around it.
            TextButton(onClick = {
                ResultCard.share(
                    context,
                    ResultCard.content(
                        title = calcTitle(hindi, formula.id, formula.title),
                        category = if (hindi) {
                            FormulaHindi.category(formula.category) ?: formula.category
                        } else formula.category,
                        results = results.map { r ->
                            r.label to (Format.number(r.value) +
                                (if (r.unit.isNotEmpty()) " ${r.unit}" else ""))
                        },
                    ),
                )
            }) { Text(if (hindi) "\u0936\u0947\u092f\u0930" else "Share") }
            StudioLabel(
                if (hindi) FormulaHindi.category(formula.category) ?: formula.category
                else formula.category,
            )
        }

        Text(
            calcTitle(hindi, formula.id, formula.title),
            style = MaterialTheme.typography.titleLarge,
            color = Studio.Text,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            calcBlurb(hindi, formula.id, formula.description),
            style = MaterialTheme.typography.bodySmall,
            color = Studio.Muted,
        )

        StudioPanel(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp)) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
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
                    StudioResult(
                        label = r.label,
                        value = Format.number(r.value) +
                            (if (r.unit.isNotEmpty()) " ${r.unit}" else ""),
                        note = r.note,
                    )
                }
            }
        }
    }
}

/**
 * Picks which calculator to send, and hands its text to the share sheet.
 *
 * A dialog rather than an action on every row: a row's hold is already the
 * editor, and a second control per row is a second thing the reader has to be
 * taught. The text is the store's own format, so whoever receives it can paste
 * it back on the other side without this app inventing a second format.
 */
@Composable
private fun SendCalculatorDialog(
    calculators: List<CustomCalculator>,
    hindi: Boolean,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hindi) "कैलकुलेटर भेजें" else "Send a calculator") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                calculators.forEach { c ->
                    TextButton(
                        onClick = {
                            val text = shareText(c)
                            // No share sheet on the device is not a dead end:
                            // the text can still be copied by hand.
                            if (!shareText(context, c.name.ifBlank { "Calculator" }, text)) {
                                copyToClipboard(context, c.name.ifBlank { "Calculator" }, text)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(c.name.ifBlank { c.expression })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

/**
 * Takes a calculator somebody sent, from the clipboard or typed in.
 *
 * The field arrives filled in because the text on the clipboard is almost
 * always the text that was just sent, and asking the reader to paste it by hand
 * would be a step with no decision in it. Nothing is added until the text
 * actually reads back as a calculator, so a wrong paste cannot add a blank row.
 */
@Composable
private fun TakeCalculatorDialog(
    hindi: Boolean,
    existing: List<CustomCalculator>,
    onTake: (List<CustomCalculator>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var text by remember { mutableStateOf(clipboardText(context)) }
    val found = remember(text, existing) { importedCalculators(text, existing) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hindi) "कैलकुलेटर लें" else "Take a calculator") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = {
                        Text(if (hindi) "भेजा गया टेक्स्ट" else "The text you were sent")
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    when {
                        found.isEmpty() -> if (hindi) "अभी पढ़ने के लिए कुछ नहीं है।"
                        else "Nothing to read yet."
                        found.size == 1 -> if (hindi) "1 कैलकुलेटर मिला।" else "One calculator found."
                        else -> if (hindi) "${found.size} कैलकुलेटर मिले।"
                        else "${found.size} calculators found."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Studio.Muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onTake(found) },
                enabled = found.isNotEmpty(),
            ) {
                Text(if (hindi) "जोड़ें" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Whatever is on the clipboard, if it is text at all. */
private fun clipboardText(context: android.content.Context): String = runCatching {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
        as android.content.ClipboardManager
    clipboard.primaryClip?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
}.getOrDefault("")

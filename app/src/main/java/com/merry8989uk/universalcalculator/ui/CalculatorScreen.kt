package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.merry8989uk.universalcalculator.ai.AiClient
import com.merry8989uk.universalcalculator.ai.AnswerFormat
import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.data.HistoryViewModel
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import com.merry8989uk.universalcalculator.settings.SettingsState
import com.merry8989uk.universalcalculator.ui.theme.Amber
import com.merry8989uk.universalcalculator.ui.theme.Coral
import com.merry8989uk.universalcalculator.ui.theme.Cyan
import com.merry8989uk.universalcalculator.ui.theme.Emerald
import com.merry8989uk.universalcalculator.ui.theme.Indigo
import com.merry8989uk.universalcalculator.ui.theme.Orange
import com.merry8989uk.universalcalculator.ui.theme.Violet
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val PAD = listOf(
    listOf("C", "del", "%", "/"),
    listOf("7", "8", "9", "*"),
    listOf("4", "5", "6", "-"),
    listOf("1", "2", "3", "+"),
    listOf("neg", "0", ".", "="),
)

/** First scientific row — always visible. */
private val SCI_ROW_A = listOf("sin", "cos", "tan", "ln", "log", "sqrt", "x^2", "x^y", "pi", "e", "!")
/** Second scientific row — revealed by the one "more / less" button. */
private val SCI_ROW_B = listOf("asin", "acos", "atan", "sinh", "cosh", "tanh", "cbrt", "1/x", "abs", "nCr", "nPr", "round", "deg")

private val LABELS = mapOf(
    "del" to "\u232B", "/" to "\u00F7", "*" to "\u00D7", "-" to "\u2212",
    "neg" to "\u00B1", "x^2" to "x\u00B2", "x^y" to "x\u02B8", "pi" to "\u03C0",
    "sqrt" to "\u221A", "deg" to "\u00B0", "cbrt" to "\u221B",
)

private fun label(k: String) = LABELS[k] ?: k
private fun isOperator(k: String) = k in listOf("/", "*", "-", "+", "%")
private fun isClear(k: String) = k == "C" || k == "del"

private fun insertFor(key: String): String = when (key) {
    "sin", "cos", "tan", "asin", "acos", "atan" -> "$key("
    "sinh", "cosh", "tanh" -> "$key("
    "ln" -> "ln("
    "log" -> "log("
    "sqrt" -> "sqrt("
    "cbrt" -> "cbrt("
    "x^2" -> "^2"
    "x^y" -> "^"
    "pi" -> "pi"
    "e" -> "e"
    "!" -> "!"
    "(" -> "("
    ")" -> ")"
    "1/x" -> "1/("
    "abs" -> "abs("
    "nCr" -> "nCr("
    "nPr" -> "nPr("
    "round" -> "round("
    "deg" -> "\u00B0"
    else -> key
}

private val exprSaver = listSaver<TextFieldValue, Any>(
    save = { listOf(it.text, it.selection.start, it.selection.end) },
    restore = { TextFieldValue(it[0] as String, TextRange(it[1] as Int, it[2] as Int)) },
)

@Composable
private fun KeyButton(
    label: String,
    colors: List<Color>,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onHold: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val pressModifier = if (onHold == null) {
        Modifier.clickable(onClick = onPress)
    } else {
        Modifier.pointerInput(Unit) {
            detectTapGestures(onPress = {
                var fired = false
                val job = scope.launch {
                    delay(450)
                    fired = true
                    while (isActive) {
                        onHold()
                        delay(55)
                    }
                }
                tryAwaitRelease()
                job.cancel()
                if (!fired) onPress()
            })
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .then(pressModifier),
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
    var expr by rememberSaveable(stateSaver = exprSaver) { mutableStateOf(TextFieldValue("")) }
    var ans by rememberSaveable { mutableStateOf(0.0) }
    var degrees by rememberSaveable { mutableStateOf(settings.degreesMode) }
    var memory by rememberSaveable { mutableStateOf(0.0) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showMore by rememberSaveable { mutableStateOf(false) }

    val historyVm: HistoryViewModel = viewModel()
    val history by historyVm.entries.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var aiText by remember { mutableStateOf("") }
    var aiLoading by remember { mutableStateOf(false) }

    val text = expr.text

    val preview: String = if (text.isBlank()) "" else try {
        Format.number(Evaluator.evaluate(text, degrees, mapOf("ans" to ans)))
    } catch (e: Exception) {
        ""
    }

    fun insert(snippet: String) {
        val t = expr.text
        val start = expr.selection.min
        val end = expr.selection.max
        val next = t.substring(0, start) + snippet + t.substring(end)
        expr = TextFieldValue(next, TextRange(start + snippet.length))
    }

    fun backspace() {
        val t = expr.text
        val start = expr.selection.min
        val end = expr.selection.max
        expr = when {
            start != end -> TextFieldValue(t.substring(0, start) + t.substring(end), TextRange(start))
            start > 0 -> TextFieldValue(t.substring(0, start - 1) + t.substring(start), TextRange(start - 1))
            else -> expr
        }
    }

    fun currentValue(): Double = try {
        if (text.isBlank()) ans else Evaluator.evaluate(text, degrees, mapOf("ans" to ans))
    } catch (e: Exception) {
        ans
    }

    fun explainNow() {
        val q = if (text.isBlank()) {
            "What can this calculator do? Give three short examples."
        } else {
            "Explain this calculation step by step and give the final answer: $text" +
                (if (preview.isNotEmpty()) " = $preview" else "")
        }
        aiText = ""
        aiLoading = true
        scope.launch {
            aiText = AnswerFormat.clean(AiClient.ask(q))
            aiLoading = false
        }
        scope.launch { drawerState.open() }
    }

    if (showHistory) {
        HistoryScreen(
            history = history,
            onBack = { showHistory = false },
            onClear = { historyVm.clear(); showHistory = false },
            onPick = { entry ->
                // Load the expression (everything before " = ") back onto the board.
                val e = entry.substringBeforeLast(" = ").trim()
                expr = TextFieldValue(e, TextRange(e.length))
                showHistory = false
            },
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxHeight().width(320.dp)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\u2726", color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("AI explain", style = MaterialTheme.typography.titleLarge)
                    }
                    if (text.isNotBlank()) {
                        Text(text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp))
                        if (preview.isNotEmpty()) {
                            Text("= ${pretty(preview, settings.groupDigits)}",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    when {
                        aiLoading -> Row(
                            modifier = Modifier.padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Text("Thinking\u2026")
                        }
                        aiText.isNotBlank() -> Text(aiText, style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp))
                        else -> Text("Tap the magic button to get an explanation here.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 16.dp))
                    }
                    TextButton(onClick = { explainNow() }, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Explain again")
                    }
                }
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MagicButton(onClick = { explainNow() })
                    Spacer(Modifier.width(6.dp))
                    TextButton(onClick = { showHistory = true }) { Text("History") }
                    TextButton(onClick = { degrees = !degrees }) { Text(if (degrees) "DEG" else "RAD") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(onClick = { memory = 0.0 }, modifier = Modifier.weight(1f)) { Text("MC") }
                FilledTonalButton(onClick = { memory += currentValue() }, modifier = Modifier.weight(1f)) { Text("M+") }
                FilledTonalButton(onClick = { memory -= currentValue() }, modifier = Modifier.weight(1f)) { Text("M\u2212") }
                FilledTonalButton(onClick = { insert(Format.number(memory)) }, modifier = Modifier.weight(1f)) { Text("MR") }
            }
            if (memory != 0.0) {
                Text("M = ${Format.number(memory)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }

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
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }

                // The expression itself is editable: tap anywhere to move the
                // caret, then edit, select or delete just that part.
                BasicTextField(
                    value = expr,
                    onValueChange = { expr = it },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (text.isEmpty()) {
                                Text("0", style = MaterialTheme.typography.headlineMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        }
                    },
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
                        val q = if (text.isBlank()) "" else "Explain this calculation and the result: $text = $preview"
                        onAskAi(q)
                    },
                    enabled = text.isNotBlank(),
                ) { Text("Ask AI about this") }
            }

            if (functionKeys.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SCI_ROW_A.forEach { k ->
                        KeyButton(
                            label = label(k),
                            colors = listOf(Color(0xFFFFD54F), Amber),
                            contentColor = Color(0xFF3A2600),
                            modifier = Modifier.height(48.dp).defaultMinSize(minWidth = 64.dp),
                            onPress = { insert(insertFor(k)) },
                        )
                    }
                    // One button toggles the second row (more / less).
                    KeyButton(
                        label = if (showMore) "\u25B4" else "\u25BE",
                        colors = listOf(Violet, Indigo),
                        contentColor = Color.White,
                        modifier = Modifier.height(48.dp).defaultMinSize(minWidth = 56.dp),
                        onPress = { showMore = !showMore },
                    )
                }
                if (showMore) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SCI_ROW_B.forEach { k ->
                            KeyButton(
                                label = label(k),
                                colors = listOf(Color(0xFFFFD54F), Amber),
                                contentColor = Color(0xFF3A2600),
                                modifier = Modifier.height(48.dp).defaultMinSize(minWidth = 64.dp),
                                onPress = { insert(insertFor(k)) },
                            )
                        }
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
                            onPress = {
                                when (key) {
                                    "C" -> expr = TextFieldValue("")
                                    "del" -> backspace()
                                    "=" -> {
                                        val shown = try {
                                            val value = Evaluator.evaluate(text, degrees, mapOf("ans" to ans))
                                            ans = value
                                            Format.number(value)
                                        } catch (e: Exception) {
                                            "error"
                                        }
                                        DiagnosticsLogger.log("Calc", "${text.ifBlank { "0" }} = $shown")
                                        if (settings.historyEnabled) {
                                            historyVm.append("${text.ifBlank { "0" }} = $shown")
                                        }
                                        expr = TextFieldValue(shown, TextRange(shown.length))
                                    }
                                    "neg" -> {
                                        val t = expr.text
                                        expr = if (t.startsWith("-")) {
                                            TextFieldValue(t.drop(1), TextRange((t.length - 1).coerceAtLeast(0)))
                                        } else {
                                            TextFieldValue("-$t", TextRange(t.length + 1))
                                        }
                                    }
                                    else -> insert(key)
                                }
                            },
                            // Holding backspace deletes repeatedly.
                            onHold = if (key == "del") {
                                { backspace() }
                            } else {
                                null
                            },
                        )
                    }
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

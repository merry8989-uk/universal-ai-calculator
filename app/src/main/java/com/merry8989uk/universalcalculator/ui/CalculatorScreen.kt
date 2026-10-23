package com.merry8989uk.universalcalculator.ui

import android.content.Intent
import android.graphics.Bitmap
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.merry8989uk.universalcalculator.ai.AiClient
import com.merry8989uk.universalcalculator.ai.AnswerFormat
import androidx.compose.ui.res.stringResource
import com.merry8989uk.universalcalculator.sound.SoundPlayer
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.ExpressionEdit
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.Complex
import com.merry8989uk.universalcalculator.calc.ComplexEvaluator
import com.merry8989uk.universalcalculator.calc.Fractions
import com.merry8989uk.universalcalculator.calc.SpeechMath
import com.merry8989uk.universalcalculator.data.HistoryViewModel
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import com.merry8989uk.universalcalculator.settings.SettingsState
import com.merry8989uk.universalcalculator.settings.SettingsViewModel
import com.merry8989uk.universalcalculator.ui.theme.Amber
import com.merry8989uk.universalcalculator.ui.theme.Coral
import com.merry8989uk.universalcalculator.ui.theme.Cyan
import com.merry8989uk.universalcalculator.ui.theme.Emerald
import com.merry8989uk.universalcalculator.ui.theme.Indigo
import com.merry8989uk.universalcalculator.ui.theme.Orange
import com.merry8989uk.universalcalculator.ui.theme.Violet
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal val PAD = listOf(
    listOf("C", "del", "%", "/", "^"),
    listOf("7", "8", "9", "*", "("),
    listOf("4", "5", "6", "-", ")"),
    listOf("1", "2", "3", "+", "neg"),
    listOf("0", "00", ".", "=", "ans"),
)

/** First scientific row — always visible. */
internal val SCI_ROW_A = listOf("sin", "cos", "tan", "ln", "log", "sqrt", "x^2", "x^y", "pi", "e", "!")
/** Second scientific row — revealed by the one "more / less" button. */
internal val SCI_ROW_B = listOf(
    "asin", "acos", "atan", "sinh", "cosh", "tanh", "cbrt", "1/x", "abs",
    "nCr", "nPr", "round", "deg", "i", "x", "swap",
)

private val LABELS = mapOf(
    "del" to "\u232B", "/" to "\u00F7", "*" to "\u00D7", "-" to "\u2212",
    "neg" to "\u00B1", "x^2" to "x\u00B2", "x^y" to "x\u02B8", "pi" to "\u03C0",
    "sqrt" to "\u221A", "deg" to "\u00B0", "cbrt" to "\u221B",
    "swap" to "\u21C4", "x" to "x",
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
    "i" -> "i"
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
    classic: Boolean = false,
    haptic: Boolean = false,
    /** What a screen reader says. Falls back to the label. */
    spoken: String = "",
    onPress: () -> Unit,
    onHold: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val shape = if (classic) RoundedCornerShape(6.dp) else RoundedCornerShape(16.dp)

    // A key dips in while it is held, with a spring so it feels physical.
    val interaction = remember { MutableInteractionSource() }
    val pressedByClick by interaction.collectIsPressedAsState()
    var pressedByHold by remember { mutableStateOf(false) }
    val dip by animateFloatAsState(
        targetValue = if (pressedByClick || pressedByHold) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 1200f),
        label = "keypress",
    )

    fun buzz() {
        if (haptic) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        SoundPlayer.key()
    }
    val pressModifier = if (onHold == null) {
        Modifier.clickable(interactionSource = interaction, indication = null) {
            buzz()
            onPress()
        }
    } else {
        Modifier.pointerInput(Unit) {
            detectTapGestures(onPress = {
                pressedByHold = true
                var fired = false
                val job = scope.launch {
                    delay(450)
                    fired = true
                    while (isActive) {
                        buzz()
                        onHold()
                        delay(55)
                    }
                }
                tryAwaitRelease()
                pressedByHold = false
                job.cancel()
                if (!fired) {
                    buzz()
                    onPress()
                }
            })
        }
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = dip
                scaleY = dip
            }
            .clip(shape)
            .background(
                if (classic) SolidColor(colors.last()) else Brush.linearGradient(colors),
            )
            .border(if (classic) 0.dp else 1.dp, Color.White.copy(alpha = 0.14f), shape)
            .then(pressModifier)
            .semantics {
                contentDescription = spoken.ifBlank { label }
                role = Role.Button
            },
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
    onAskAiNow: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    var expr by rememberSaveable(stateSaver = exprSaver) { mutableStateOf(TextFieldValue("")) }
    var ans by rememberSaveable { mutableStateOf(0.0) }
    var degrees by rememberSaveable { mutableStateOf(settings.degreesMode) }
    var memory by rememberSaveable { mutableStateOf(0.0) }
    // The value the variable x takes when it appears in the expression.
    var xText by rememberSaveable { mutableStateOf("0") }
    var menuOpen by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var showAppInfo by remember { mutableStateOf(false) }
    var logLines by remember { mutableStateOf(listOf<String>()) }
    val xValue = xText.toDoubleOrNull() ?: 0.0
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showMore by rememberSaveable { mutableStateOf(false) }

    val historyVm: HistoryViewModel = viewModel()
    val history by historyVm.entries.collectAsState()
    val settingsVm: SettingsViewModel = viewModel()
    val savedExpression by settingsVm.savedExpression.collectAsState()
    var restored by remember { mutableStateOf(false) }

    // Bring the previous expression back, and keep the new one saved.
    LaunchedEffect(savedExpression, settings.saveState) {
        if (!restored && settings.saveState && savedExpression.isNotBlank() && expr.text.isEmpty()) {
            expr = TextFieldValue(savedExpression, TextRange(savedExpression.length))
            restored = true
        }
    }
    LaunchedEffect(expr.text, restored) {
        if (!settings.saveState) return@LaunchedEffect
        if (!restored && savedExpression.isNotBlank()) return@LaunchedEffect
        delay(700)
        settingsVm.setSavedExpression(expr.text)
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var aiText by remember { mutableStateOf("") }
    var aiLoading by remember { mutableStateOf(false) }

    val text = expr.text
    val classic = settings.layoutStyle == "Classic"

    val preview: String = if (text.isBlank()) "" else try {
        if (ComplexEvaluator.hasImaginary(text)) {
            val r = ComplexEvaluator.evaluate(
                                                text, degrees,
                                                mapOf("ans" to Complex(ans, 0.0), "x" to Complex(xValue, 0.0)),
                                            )
            if (r.isReal(1e-10)) Format.number(r.re) else Complex.text(r)
        } else {
            Format.number(Evaluator.evaluate(text, degrees, mapOf("ans" to ans)))
        }
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
        when {
            text.isBlank() -> ans
            ComplexEvaluator.hasImaginary(text) ->
                ComplexEvaluator.evaluate(
                    text, degrees,
                    mapOf("ans" to Complex(ans, 0.0), "x" to Complex(xValue, 0.0)),
                ).abs()
            else -> Evaluator.evaluate(text, degrees, mapOf("ans" to ans, "x" to xValue))
        }
    } catch (e: Exception) {
        ans
    }

    // ---- voice input ----
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
        if (spoken.isNotBlank()) {
            val exprText = SpeechMath.normalise(spoken)
            if (SpeechMath.looksUsable(exprText)) {
                expr = TextFieldValue(exprText, TextRange(exprText.length))
                // Hand the words and the expression to the assistant so it can
                // answer and explain, not just evaluate.
                if (settings.aiEnabled) {
                    // Send the spoken words and the expression to the assistant
                    // so it answers and explains, not just evaluates.
                    onAskAiNow(
                        "I asked by voice: \"$spoken\". That is the calculation $exprText. " +
                            "Work it out in a few lines and give the final answer.",
                    )
                }
            } else {
                aiText = "I heard \"$spoken\" but could not turn it into a calculation."
                scope.launch { drawerState.open() }
            }
        }
    }

    fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a calculation, e.g. twenty percent of fifty")
        }
        try {
            micLauncher.launch(intent)
        } catch (e: Exception) {
            aiText = "No speech service is available on this device."
            scope.launch { drawerState.open() }
        }
    }

    // ---- photo solve ----
    var photoBusy by remember { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bitmap: Bitmap? ->
        if (bitmap == null) return@rememberLauncherForActivityResult
        photoBusy = true
        scope.launch {
            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                out.toByteArray()
            }
            val r = AiClient.solveImage(bytes)
            photoBusy = false
            if (r.expression != null) {
                expr = TextFieldValue(r.expression, TextRange(r.expression.length))
                // Send the text read from the photo to the assistant too.
                if (settings.aiEnabled) {
                    onAskAiNow(
                        "I photographed a calculation and the text read from the photo is: " +
                            "${r.expression}" +
                            (if (r.lowConfidence) " (read with low confidence)" else "") +
                            ". Work it out in a few lines and give the final answer.",
                    )
                } else {
                    aiText = "Read from the photo: ${r.expression}"
                    drawerState.open()
                }
            } else {
                aiText = r.error ?: "Could not read the photo."
                drawerState.open()
            }
        }
    }

    fun startPhoto() {
        try {
            cameraLauncher.launch(null)
        } catch (e: Exception) {
            aiText = "No camera is available on this device."
            scope.launch { drawerState.open() }
        }
    }

    fun swapOperands() {
        val t = expr.text
        val swapped = ExpressionEdit.swapOperands(t)
        if (swapped != t) expr = TextFieldValue(swapped, TextRange(swapped.length))
    }

    /** Asks the assistant and shows the answer in the side drawer. */
    fun explainText(question: String) {
        aiText = ""
        aiLoading = true
        scope.launch {
            aiText = AnswerFormat.clean(AiClient.ask(question))
            aiLoading = false
        }
        scope.launch { drawerState.open() }
    }

    fun explainNow() {
        val q = if (text.isBlank()) {
            "What can this calculator do? Give three short examples."
        } else {
            "Explain this calculation step by step and give the final answer: $text" +
                (if (preview.isNotEmpty()) " = $preview" else "")
        }
        explainText(q)
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
                        Text(stringResource(R.string.ai_explain), style = MaterialTheme.typography.titleLarge)
                    }
                    if (text.isNotBlank()) {
                        Text(text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp))
                        if (preview.isNotEmpty()) {
                            Text("= ${pretty(preview, settings.numberFormat, settings.decimals)}",
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
                            Text(stringResource(R.string.thinking))
                        }
                        aiText.isNotBlank() -> {
                            val answerSpoken = stringResource(R.string.assistant_answer_a11y, aiText)
                            SelectionContainer {
                                Text(aiText, style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .semantics {
                                            liveRegion = LiveRegionMode.Polite
                                            contentDescription = answerSpoken
                                        })
                            }
                            AnswerActions(aiText, onClear = { aiText = "" })
                        }
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
                val historySpoken = stringResource(R.string.history_a11y)
                val menuSpoken = stringResource(R.string.more_options)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { showHistory = true },
                        modifier = Modifier.semantics { contentDescription = historySpoken },
                    ) { Text(stringResource(R.string.history)) }
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.semantics { contentDescription = menuSpoken },
                        ) {
                            Text("\u22EE", style = MaterialTheme.typography.headlineSmall)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_settings)) },
                                onClick = { menuOpen = false; onOpenSettings() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_diagnostics)) },
                                onClick = {
                                    menuOpen = false
                                    logLines = DiagnosticsLogger.recent().reversed()
                                    showDiagnostics = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_app_info)) },
                                onClick = { menuOpen = false; showAppInfo = true },
                            )
                            HorizontalDivider()
                            // The angle mode moved here from the top bar.
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (degrees) R.string.degrees_mode else R.string.radians_mode,
                                        ),
                                    )
                                },
                                onClick = { menuOpen = false; degrees = !degrees },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = { memory = 0.0 },
                    modifier = Modifier.weight(1f).semantics { contentDescription = "memory clear" },
                ) { Text("MC") }
                FilledTonalButton(
                    onClick = { memory += currentValue() },
                    modifier = Modifier.weight(1f).semantics { contentDescription = "memory add" },
                ) { Text("M+") }
                FilledTonalButton(
                    onClick = { memory -= currentValue() },
                    modifier = Modifier.weight(1f).semantics { contentDescription = "memory subtract" },
                ) { Text("M\u2212") }
                FilledTonalButton(
                    onClick = { insert(Format.number(memory)) },
                    modifier = Modifier.weight(1f).semantics { contentDescription = "memory recall" },
                ) { Text("MR") }
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

                val calcSpoken = stringResource(
                    R.string.calc_a11y,
                    text.ifBlank { stringResource(R.string.calc_empty) },
                )

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .semantics { contentDescription = calcSpoken },
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

                val resultSpoken = if (preview.isNotEmpty())
                    stringResource(
                        R.string.result_a11y,
                        pretty(preview, settings.numberFormat, settings.decimals),
                    )
                else ""

                if (settings.livePreview) {
                    val asFraction = if (settings.fractionsEnabled && preview.isNotEmpty()) {
                        preview.toDoubleOrNull()?.let { Fractions.mixedText(it) }
                    } else null
                    Text(
                        text = if (preview.isNotEmpty())
                            "= ${pretty(preview, settings.numberFormat, settings.decimals)}" +
                                (asFraction?.let { "   ($it)" } ?: "")
                        else "",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = resultSpoken
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val speakSpoken = stringResource(R.string.speak_a11y)
                    val photoSpoken = stringResource(R.string.photo_a11y)
                    TextButton(
                        onClick = { startVoice() },
                        modifier = Modifier.semantics { contentDescription = speakSpoken },
                    ) { Text("\uD83C\uDFA4 " + stringResource(R.string.speak)) }
                    TextButton(
                        onClick = { startPhoto() },
                        enabled = !photoBusy,
                        modifier = Modifier.semantics { contentDescription = photoSpoken },
                    ) {
                        Text(if (photoBusy) stringResource(R.string.reading) else "\uD83D\uDCF7 " + stringResource(R.string.photo))
                    }
                    TextButton(
                        onClick = {
                            val q = if (text.isBlank()) "" else "Explain this calculation and the result: $text = $preview"
                            onAskAi(q)
                        },
                        enabled = text.isNotBlank(),
                    ) { Text(stringResource(R.string.ask_ai_about_this)) }
                }
            }

            // When the expression uses x, a small field sets its value.
            if (ExpressionEdit.usesVariable(text)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.x_equals), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(
                        value = xText,
                        onValueChange = { xText = it },
                        singleLine = true,
                        modifier = Modifier.width(140.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.x_value_used),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // The magic button sits on the left, above the yellow rows and the
            // Clear key, so it is within reach of the thumb.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MagicButton(onClick = { explainNow() })
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.explain_with_ai),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                            classic = classic,
                            haptic = settings.hapticsEnabled,
                            spoken = Spoken.key(k),
                            onPress = { insert(insertFor(k)) },
                        )
                    }
                    // One button toggles the second row (more / less).
                    KeyButton(
                        label = if (showMore) "\u25B4" else "\u25BE",
                        colors = listOf(Violet, Indigo),
                        contentColor = Color.White,
                        modifier = Modifier.height(48.dp).defaultMinSize(minWidth = 56.dp),
                        classic = classic,
                        haptic = settings.hapticsEnabled,
                        spoken = stringResource(if (showMore) R.string.show_fewer_functions else R.string.show_more_functions),
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
                                classic = classic,
                                haptic = settings.hapticsEnabled,
                                spoken = Spoken.key(k),
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
                            classic = classic,
                            haptic = settings.hapticsEnabled,
                            spoken = Spoken.key(key),
                            onPress = {
                                when (key) {
                                    "C" -> expr = TextFieldValue("")
                                    "del" -> backspace()
                                    "=" -> {
                                        val shown = try {
                                            if (ComplexEvaluator.hasImaginary(text)) {
                                                val r = ComplexEvaluator.evaluate(
                                                text, degrees,
                                                mapOf("ans" to Complex(ans, 0.0), "x" to Complex(xValue, 0.0)),
                                            )
                                                if (r.isReal(1e-10)) {
                                                    ans = r.re
                                                    Format.number(r.re)
                                                } else {
                                                    Complex.text(r)
                                                }
                                            } else {
                                                val value = Evaluator.evaluate(text, degrees, mapOf("ans" to ans, "x" to xValue))
                                                ans = value
                                                Format.number(value)
                                            }
                                        } catch (e: Exception) {
                                            "error"
                                        }
                                        DiagnosticsLogger.log("Calc", "${text.ifBlank { "0" }} = $shown")
                                        if (settings.historyEnabled) {
                                            historyVm.append("${text.ifBlank { "0" }} = $shown")
                                        }
                                        expr = TextFieldValue(shown, TextRange(shown.length))
                                    }
                                    "00" -> insert("00")
                                    "ans" -> insert("ans")
                                    "x" -> insert("x")
                                    "swap" -> swapOperands()
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

    if (showDiagnostics) {
        DiagnosticsLogDialog(
            lines = logLines,
            onClear = { DiagnosticsLogger.clear(); logLines = emptyList() },
            onClose = { showDiagnostics = false },
        )
    }
    if (showAppInfo) {
        AppInfoDialog(onClose = { showAppInfo = false })
    }
}

/** Adds thousands separators to the integer part when the user wants them. */
/**
 * Formats a result for display: rounds to the chosen number of decimals, then
 * groups the digits the way the chosen locale does.
 *
 *   Indian        30,00,000.5
 *   International  3,000,000.5
 *   European       3.000.000,5
 *   None           3000000.5
 */
private fun pretty(text: String, format: String, decimals: Int): String =
    Format.grouped(text, format, decimals)


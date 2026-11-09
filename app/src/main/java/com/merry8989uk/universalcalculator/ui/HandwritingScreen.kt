package com.merry8989uk.universalcalculator.ui

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.ai.AiClient
import com.merry8989uk.universalcalculator.ai.UploadDecision
import com.merry8989uk.universalcalculator.ai.uploadDecision
import com.merry8989uk.universalcalculator.calc.ComplexEvaluator
import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Write a calculation with a finger, then read it. The strokes are drawn into
 * an image and read by the same vision model the photo solver uses, so the app
 * needs no handwriting library.
 */
@Composable
fun HandwritingScreen(
    onBack: () -> Unit,
    /**
     * The master data-saving switch, handed in rather than read here.
     *
     * The pad is the one place a drawing becomes a picture on the wire, so it
     * asks this before it renders anything. Required, not defaulted: a caller
     * that forgets to pass it is a caller that has not answered the question.
     */
    savingEnabled: Boolean,
    /**
     * Where an accepted reading goes.
     *
     * Defaulted so the screen compiles wherever it is opened from, and so a
     * caller that has somewhere to put the expression can say so. Until one
     * does, an accepted reading is copied to the clipboard, which is the one
     * route out of this screen the pad can take on its own.
     */
    onUseExpression: (String) -> Unit = {},
) {
    var strokes by remember { mutableStateOf(emptyList<List<Offset>>()) }
    var current by remember { mutableStateOf(emptyList<Offset>()) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    /**
     * Where the page is in its journey to the model.
     *
     * The drawing itself is held apart, in [strokes], because it is the user's
     * work and outlives every attempt to read it: a failure moves [state] on and
     * leaves the strokes exactly where they were, which is what makes the failure
     * a step rather than an ending. Only Clear drops the drawing.
     */
    var state by remember { mutableStateOf(SketchState()) }

    /** The rendered page, shown before it is sent and kept for the retry. */
    var page by remember { mutableStateOf<Bitmap?>(null) }

    /** What the model read, editable so a misreading costs a correction and not a redraw. */
    var read by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val ink = MaterialTheme.colorScheme.primary
    val paper = Color.White

    fun render(): Bitmap {
        val w = size.width.coerceAtLeast(1)
        val h = size.height.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 7f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        val all = if (current.isEmpty()) strokes else strokes + listOf(current)
        for (s in all) {
            if (s.size == 1) {
                canvas.drawCircle(s[0].x, s[0].y, 4f, paint)
            } else {
                for (i in 1 until s.size) canvas.drawLine(s[i - 1].x, s[i - 1].y, s[i].x, s[i].y, paint)
            }
        }
        return bmp
    }

    /**
     * Renders the page and shows it, without sending anything.
     *
     * The pad used to go from "tap Read it" to "the model has your handwriting"
     * in one step, so the user never saw the picture that left the phone. Here
     * the strokes become an image on screen first, and Send is a second, separate
     * tap. The switch is asked before the render, so a refusal costs no work and
     * opens no connection.
     */
    fun previewIt() {
        if (strokes.isEmpty() && current.isEmpty()) {
            message = "Write something first."
            return
        }
        val decision = uploadDecision(savingEnabled)
        if (decision is UploadDecision.Refused) {
            // Nothing was rendered and nothing was sent, but the drawing is
            // still on the pad, so the refusal is a stage with a way out rather
            // than a line of text under it.
            message = decision.message
            state = state.copy(stage = SketchStage.Failed(decision.message))
            return
        }
        message = ""
        read = ""
        scope.launch {
            val bmp = withContext(Dispatchers.Default) { render() }
            page = bmp
            state = state.copy(stage = SketchStage.Previewing, hasDrawing = true)
        }
    }

    /**
     * Sends the page that is on screen.
     *
     * Reached from Send in the preview and from Try again after a failed read —
     * never from the pad itself. The switch is asked again here because the
     * preview can sit on screen while it is turned off, and because this is the
     * moment the picture would leave the phone.
     */
    fun sendPage() {
        val bmp = page
        if (bmp == null) {
            // Nothing rendered: fall back to the pad, so the tap is never lost.
            previewIt()
            return
        }
        val decision = uploadDecision(savingEnabled)
        if (decision is UploadDecision.Refused) {
            message = decision.message
            state = state.copy(stage = SketchStage.Failed(decision.message))
            return
        }
        message = ""
        state = state.copy(stage = SketchStage.Sending)
        scope.launch {
            val bytes = withContext(Dispatchers.Default) {
                ByteArrayOutputStream().use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    out.toByteArray()
                }
            }
            val r = AiClient.solveImage(bytes)
            // The drawing is not touched here: sketchAfterRead decides what the
            // screen shows and carries the drawing through a failure, so one bad
            // read never costs the user their work.
            state = sketchAfterRead(state, r.expression, r.error)
            read = r.expression.orEmpty()
        }
    }

    fun clearAll() {
        strokes = emptyList()
        current = emptyList()
        read = ""
        message = ""
        page = null
        state = sketchCleared()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Handwriting", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("\u2190 Back") }
        }
        Text(
            "Write a calculation with your finger, then tap Read it.",
            style = MaterialTheme.typography.bodySmall,
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(paper)
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> current = listOf(offset) },
                        onDrag = { change, _ -> current = current + change.position },
                        onDragEnd = {
                            if (current.isNotEmpty()) strokes = strokes + listOf(current)
                            current = emptyList()
                            // The page can be read again, so a failure from here
                            // on has something to try again with.
                            state = state.copy(hasDrawing = strokes.isNotEmpty())
                        },
                        onDragCancel = { current = emptyList() },
                    )
                },
        ) {
            fun drawStroke(points: List<Offset>) {
                if (points.size == 1) {
                    drawCircle(ink, radius = 3f, center = points[0])
                } else {
                    for (i in 1 until points.size) {
                        drawLine(ink, points[i - 1], points[i], strokeWidth = 6f, cap = StrokeCap.Round)
                    }
                }
            }
            strokes.forEach { drawStroke(it) }
            if (current.isNotEmpty()) drawStroke(current)
        }

        // Said under the pad, where the drawing is made: Read it sends this page
        // to the assistant as a picture, and the user is told so before they tap
        // it rather than after.
        Text(
            stringResource(R.string.handwriting_sent_notice),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { clearAll() }) { Text("Clear") }
            Button(
                onClick = { previewIt() },
                enabled = state.stage !is SketchStage.Sending && state.stage !is SketchStage.Previewing,
            ) {
                Text(
                    if (state.stage is SketchStage.Sending) "Reading\u2026" else "Read it",
                )
            }
            if (state.stage is SketchStage.Sending) CircularProgressIndicator(modifier = Modifier.height(20.dp))
        }

        when (val s = state.stage) {
            SketchStage.Drawing -> {}

            // The page as it will be sent. Nothing has left the phone.
            SketchStage.Previewing -> SketchCard {
                Text(
                    stringResource(R.string.handwriting_preview_not_sent),
                    style = MaterialTheme.typography.bodyMedium,
                )
                page?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = stringResource(R.string.handwriting_preview_a11y),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { sendPage() }) { Text(stringResource(R.string.handwriting_send)) }
                    OutlinedButton(onClick = {
                        // Cancel keeps the drawing: it was the sending that was
                        // cancelled, not the work.
                        state = state.copy(stage = SketchStage.Drawing)
                    }) { Text(stringResource(R.string.handwriting_cancel)) }
                }
            }

            SketchStage.Sending -> {}

            // What the model read, offered and editable rather than announced.
            is SketchStage.Read -> SketchCard {
                Text(
                    stringResource(R.string.handwriting_read_as),
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = read,
                    onValueChange = { read = it },
                    label = { Text(stringResource(R.string.handwriting_edit_hint)) },
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                val value = read.trim()
                val result = remember(value) { evaluate(value) }
                if (result.isNotBlank()) {
                    Text(
                        "= $result",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            val text = read.trim()
                            if (text.isBlank()) return@Button
                            clipboard.setText(AnnotatedString(text))
                            onUseExpression(text)
                            message = stringResource(R.string.handwriting_copied)
                        },
                    ) { Text(stringResource(R.string.handwriting_use)) }
                    OutlinedButton(onClick = { sendPage() }) {
                        Text(stringResource(R.string.handwriting_try_again))
                    }
                }
            }

            // A failed read says what failed, and the drawing is still on the pad
            // above, so this is a step back rather than a dead end.
            is SketchStage.Failed -> SketchCard(error = true) {
                Text(
                    s.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                if (state.hasDrawing) {
                    Text(
                        stringResource(R.string.handwriting_failed_kept),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.canRetry) {
                        OutlinedButton(onClick = { sendPage() }) {
                            Text(stringResource(R.string.handwriting_try_again))
                        }
                    }
                }
            }
        }

        if (message.isNotBlank()) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.stage is SketchStage.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** One shape for the preview, the reading and the failure, so the pad reads as one flow. */
@Composable
private fun SketchCard(
    error: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        colors = if (error) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        },
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

/**
 * The value of what was read, or "" when it is not something that can be worked
 * out. A blank answer is quieter than "could not evaluate" here: the reading is
 * on screen and editable, so the user can see for themselves.
 */
private fun evaluate(expression: String): String {
    if (expression.isBlank()) return ""
    return try {
        if (ComplexEvaluator.hasImaginary(expression)) {
            val c = ComplexEvaluator.evaluate(expression)
            if (c.isReal(1e-10)) Format.number(c.re) else c.toString()
        } else {
            Format.number(Evaluator.evaluate(expression))
        }
    } catch (e: Exception) {
        ""
    }
}

/**
 * Where the sketch is on its way to the model.
 *
 * [Drawing] and [Previewing] are the two the pad did not have: the first is a
 * page that exists only on the screen, the second is the same page shown as the
 * picture it will be sent as. Separating them is what puts a Send between the
 * user's finger and the network.
 *
 * [Failed] is a stage and not an ending. The drawing lives outside this type, so
 * a failed read can only ever move the stage — there is no branch here that
 * could drop the user's work, which is what makes "the drawing is kept" a fact
 * about the type rather than a promise about the code.
 */
internal sealed interface SketchStage {

    /** The pad, with nothing sent and nothing read. */
    data object Drawing : SketchStage

    /** The rendered page is on screen, waiting for Send. */
    data object Previewing : SketchStage

    /** The page is with the model. */
    data object Sending : SketchStage

    /** The model read an expression. Held for the user to accept or correct. */
    data class Read(val expression: String) : SketchStage

    /** The read failed, and says why. The drawing is still on the pad. */
    data class Failed(val message: String) : SketchStage
}

/**
 * The pad's state: which stage it is in, and whether there is still a drawing.
 *
 * [hasDrawing] is carried rather than asked of the stroke list, so that "a
 * failure kept the drawing" is something a test can check without a screen.
 */
internal data class SketchState(
    val stage: SketchStage = SketchStage.Drawing,
    val hasDrawing: Boolean = false,
) {
    /** Try again is offered only when there is a page to send and nothing in flight. */
    val canRetry: Boolean get() = hasDrawing && stage !is SketchStage.Sending
}

/**
 * What a read did to the pad.
 *
 * An expression becomes text the user can accept; anything else becomes a
 * failure that names the reason and keeps [SketchState.hasDrawing] exactly as it
 * was. The drawing is never cleared here — only Clear does that — because a
 * model that could not read the page is not a reason to throw the page away.
 */
internal fun sketchAfterRead(state: SketchState, expression: String?, error: String?): SketchState {
    val text = expression?.trim().orEmpty()
    return if (text.isNotEmpty()) {
        state.copy(stage = SketchStage.Read(text))
    } else {
        state.copy(
            stage = SketchStage.Failed(
                error?.takeIf { it.isNotBlank() } ?: "Could not read that.",
            ),
        )
    }
}

/** An empty pad: the one transition that drops the drawing. */
internal fun sketchCleared(): SketchState = SketchState()

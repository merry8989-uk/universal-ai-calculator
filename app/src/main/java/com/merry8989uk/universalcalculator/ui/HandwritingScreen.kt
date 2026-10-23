package com.merry8989uk.universalcalculator.ui

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.ai.AiClient
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
fun HandwritingScreen(onBack: () -> Unit) {
    var strokes by remember { mutableStateOf(emptyList<List<Offset>>()) }
    var current by remember { mutableStateOf(emptyList<Offset>()) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var busy by remember { mutableStateOf(false) }
    var read by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
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

    fun readIt() {
        if (strokes.isEmpty() && current.isEmpty()) {
            message = "Write something first."
            return
        }
        busy = true
        message = ""
        scope.launch {
            val bytes = withContext(Dispatchers.Default) {
                ByteArrayOutputStream().use { out ->
                    render().compress(Bitmap.CompressFormat.JPEG, 85, out)
                    out.toByteArray()
                }
            }
            val r = AiClient.solveImage(bytes)
            busy = false
            if (r.expression != null) {
                read = r.expression
                result = try {
                    if (ComplexEvaluator.hasImaginary(r.expression)) {
                        val c = ComplexEvaluator.evaluate(r.expression)
                        if (c.isReal(1e-10)) Format.number(c.re) else c.toString()
                    } else {
                        Format.number(Evaluator.evaluate(r.expression))
                    }
                } catch (e: Exception) {
                    "could not evaluate"
                }
            } else {
                message = r.error ?: "Could not read that."
            }
        }
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { strokes = emptyList(); current = emptyList(); read = ""; result = ""; message = "" },
            ) { Text("Clear") }
            Button(onClick = { readIt() }, enabled = !busy) {
                Text(if (busy) "Reading\u2026" else "Read it")
            }
            if (busy) CircularProgressIndicator(modifier = Modifier.height(20.dp))
        }

        if (message.isNotBlank()) {
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        if (read.isNotBlank()) {
            Text("Read as: $read", style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp))
            if (result.isNotBlank()) {
                Text("= $result", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

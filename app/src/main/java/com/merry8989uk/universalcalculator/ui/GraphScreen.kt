package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Evaluator

@Composable
fun GraphScreen(onBack: () -> Unit) {
    var expr by remember { mutableStateOf("sin(x)") }
    var xminText by remember { mutableStateOf("-6.5") }
    var xmaxText by remember { mutableStateOf("6.5") }

    val lo = xminText.toDoubleOrNull() ?: -6.5
    val hi = xmaxText.toDoubleOrNull() ?: 6.5

    val samples = remember(expr, lo, hi) {
        val n = 400
        val list = ArrayList<Pair<Double, Double>>(n + 1)
        if (hi > lo) {
            for (i in 0..n) {
                val x = lo + (hi - lo) * i / n
                val y = try {
                    Evaluator.evaluate(expr, degrees = false, vars = mapOf("x" to x))
                } catch (e: Exception) {
                    Double.NaN
                }
                list.add(Pair(x, y))
            }
        }
        list
    }

    val finite = samples.filter { !it.second.isNaN() && !it.second.isInfinite() }
    var ymin = finite.minOfOrNull { it.second } ?: -1.0
    var ymax = finite.maxOfOrNull { it.second } ?: 1.0
    if (ymax - ymin < 1e-9) { ymin -= 1.0; ymax += 1.0 }
    val pad = (ymax - ymin) * 0.1
    ymin -= pad; ymax += pad

    val curveColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Graphing", style = MaterialTheme.typography.titleLarge)
        Text("Plot y = f(x).", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = expr, onValueChange = { expr = it }, label = { Text("f(x)") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = xminText, onValueChange = { xminText = it }, label = { Text("x min") },
                singleLine = true, modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = xmaxText, onValueChange = { xmaxText = it }, label = { Text("x max") },
                singleLine = true, modifier = Modifier.weight(1f),
            )
        }

        Canvas(
            modifier = Modifier.fillMaxWidth().height(320.dp).padding(top = 12.dp),
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f || hi <= lo) return@Canvas

            fun sx(x: Double): Float = ((x - lo) / (hi - lo) * w).toFloat()
            fun sy(y: Double): Float = (h - (y - ymin) / (ymax - ymin) * h).toFloat()

            // Axes
            if (0.0 in lo..hi) {
                val x0 = sx(0.0)
                drawLine(axisColor, Offset(x0, 0f), Offset(x0, h), strokeWidth = 2f)
            }
            if (0.0 in ymin..ymax) {
                val y0 = sy(0.0)
                drawLine(axisColor, Offset(0f, y0), Offset(w, y0), strokeWidth = 2f)
            }

            // Curve
            val path = Path()
            var started = false
            for (p in samples) {
                val y = p.second
                if (y.isNaN() || y.isInfinite()) { started = false; continue }
                val px = sx(p.first)
                val py = sy(y).coerceIn(-10f, h + 10f)
                if (!started) { path.moveTo(px, py); started = true } else path.lineTo(px, py)
            }
            drawPath(path, color = curveColor, style = Stroke(width = 4f))
        }

        Text(
            "y range shown: ${"%.3f".format(ymin)} to ${"%.3f".format(ymax)}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.merry8989uk.universalcalculator.calc.Plot3D

private val MODES = listOf("2-D", "Parametric", "3-D surface")

@Composable
fun GraphScreen(onBack: () -> Unit) {
    var mode by remember { mutableStateOf("2-D") }
    val curveColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Graphing", style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MODES.forEach { m ->
                FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m) })
            }
        }

        when (mode) {
            "Parametric" -> ParametricPlot(curveColor, axisColor)
            "3-D surface" -> SurfacePlot(curveColor, axisColor)
            else -> PlainPlot(curveColor, axisColor)
        }
    }
}

@Composable
private fun PlainPlot(curveColor: Color, axisColor: Color) {
    var expr by remember { mutableStateOf("sin(x)") }
    var xminText by remember { mutableStateOf("-6.5") }
    var xmaxText by remember { mutableStateOf("6.5") }
    val lo = xminText.toDoubleOrNull() ?: -6.5
    val hi = xmaxText.toDoubleOrNull() ?: 6.5

    val samples = remember(expr, lo, hi) {
        val n = 400
        val list = ArrayList<Pair<Double, Double>>(n + 1)
        if (hi > lo) for (i in 0..n) {
            val x = lo + (hi - lo) * i / n
            val y = try {
                Evaluator.evaluate(expr, degrees = false, vars = mapOf("x" to x))
            } catch (e: Exception) { Double.NaN }
            list.add(x to y)
        }
        list
    }

    val finite = samples.filter { !it.second.isNaN() && !it.second.isInfinite() }
    var ymin = finite.minOfOrNull { it.second } ?: -1.0
    var ymax = finite.maxOfOrNull { it.second } ?: 1.0
    if (ymax - ymin < 1e-9) { ymin -= 1.0; ymax += 1.0 }
    val pad = (ymax - ymin) * 0.1
    ymin -= pad; ymax += pad

    Text("Plot y = f(x).", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(
        value = expr, onValueChange = { expr = it }, label = { Text("f(x)") },
        singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        OutlinedTextField(value = xminText, onValueChange = { xminText = it },
            label = { Text("x min") }, singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = xmaxText, onValueChange = { xmaxText = it },
            label = { Text("x max") }, singleLine = true, modifier = Modifier.weight(1f))
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(300.dp).padding(top = 10.dp)) {
        val w = size.width; val h = size.height
        if (w <= 0f || h <= 0f || hi <= lo) return@Canvas
        fun sx(x: Double): Float = ((x - lo) / (hi - lo) * w).toFloat()
        fun sy(y: Double): Float = (h - (y - ymin) / (ymax - ymin) * h).toFloat()
        if (0.0 in lo..hi) { val x0 = sx(0.0); drawLine(axisColor, Offset(x0, 0f), Offset(x0, h), strokeWidth = 2f) }
        if (0.0 in ymin..ymax) { val y0 = sy(0.0); drawLine(axisColor, Offset(0f, y0), Offset(w, y0), strokeWidth = 2f) }
        val path = Path(); var started = false
        for (p in samples) {
            val y = p.second
            if (y.isNaN() || y.isInfinite()) { started = false; continue }
            val px = sx(p.first); val py = sy(y).coerceIn(-10f, h + 10f)
            if (!started) { path.moveTo(px, py); started = true } else path.lineTo(px, py)
        }
        drawPath(path, color = curveColor, style = Stroke(width = 4f))
    }
    Text("y range: ${"%.3f".format(ymin)} to ${"%.3f".format(ymax)}", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ParametricPlot(curveColor: Color, axisColor: Color) {
    var xExpr by remember { mutableStateOf("cos(t)") }
    var yExpr by remember { mutableStateOf("sin(t)") }
    var tMinText by remember { mutableStateOf("0") }
    var tMaxText by remember { mutableStateOf("6.283185307179586") }
    val t0 = tMinText.toDoubleOrNull() ?: 0.0
    val t1 = tMaxText.toDoubleOrNull() ?: 6.283185307179586

    val pts = remember(xExpr, yExpr, t0, t1) {
        Plot3D.sampleParametric(xExpr, yExpr, t0, t1, 800)
    }
    val fin = pts.filter { !it.first.isNaN() && !it.second.isNaN() }
    var xmin = fin.minOfOrNull { it.first } ?: -1.0
    var xmax = fin.maxOfOrNull { it.first } ?: 1.0
    var ymin = fin.minOfOrNull { it.second } ?: -1.0
    var ymax = fin.maxOfOrNull { it.second } ?: 1.0
    if (xmax - xmin < 1e-9) { xmin -= 1.0; xmax += 1.0 }
    if (ymax - ymin < 1e-9) { ymin -= 1.0; ymax += 1.0 }

    Text("Plot x(t), y(t) \u2014 try cos(t), sin(t).", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(value = xExpr, onValueChange = { xExpr = it }, label = { Text("x(t)") },
        singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    OutlinedTextField(value = yExpr, onValueChange = { yExpr = it }, label = { Text("y(t)") },
        singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        OutlinedTextField(value = tMinText, onValueChange = { tMinText = it },
            label = { Text("t min") }, singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = tMaxText, onValueChange = { tMaxText = it },
            label = { Text("t max") }, singleLine = true, modifier = Modifier.weight(1f))
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(300.dp).padding(top = 10.dp)) {
        val w = size.width; val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val pad = 0.08
        val xlo = xmin - (xmax - xmin) * pad; val xhi = xmax + (xmax - xmin) * pad
        val ylo = ymin - (ymax - ymin) * pad; val yhi = ymax + (ymax - ymin) * pad
        fun sx(x: Double): Float = ((x - xlo) / (xhi - xlo) * w).toFloat()
        fun sy(y: Double): Float = (h - (y - ylo) / (yhi - ylo) * h).toFloat()
        if (0.0 in xlo..xhi) { val x0 = sx(0.0); drawLine(axisColor, Offset(x0, 0f), Offset(x0, h), strokeWidth = 2f) }
        if (0.0 in ylo..yhi) { val y0 = sy(0.0); drawLine(axisColor, Offset(0f, y0), Offset(w, y0), strokeWidth = 2f) }
        val path = Path(); var started = false
        for (p in pts) {
            if (p.first.isNaN() || p.second.isNaN()) { started = false; continue }
            val px = sx(p.first); val py = sy(p.second)
            if (!started) { path.moveTo(px, py); started = true } else path.lineTo(px, py)
        }
        drawPath(path, color = curveColor, style = Stroke(width = 4f))
    }
}

@Composable
private fun SurfacePlot(curveColor: Color, axisColor: Color) {
    var expr by remember { mutableStateOf("sin(x)*cos(y)") }
    var xMinText by remember { mutableStateOf("-3") }
    var xMaxText by remember { mutableStateOf("3") }
    var yMinText by remember { mutableStateOf("-3") }
    var yMaxText by remember { mutableStateOf("3") }
    var steps by remember { mutableFloatStateOf(20f) }
    var yaw by remember { mutableFloatStateOf(35f) }
    var pitch by remember { mutableFloatStateOf(55f) }

    val x0 = xMinText.toDoubleOrNull() ?: -3.0
    val x1 = xMaxText.toDoubleOrNull() ?: 3.0
    val y0 = yMinText.toDoubleOrNull() ?: -3.0
    val y1 = yMaxText.toDoubleOrNull() ?: 3.0
    val n = steps.toInt()

    val grid = remember(expr, x0, x1, y0, y1, n) {
        Plot3D.sampleSurface(expr, x0, x1, y0, y1, n)
    }
    val (zlo, zhi) = remember(grid) { Plot3D.zRange(grid) }

    Text("Plot a surface z = f(x, y), then drag the sliders to turn it.",
        style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(value = expr, onValueChange = { expr = it }, label = { Text("f(x, y)") },
        singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        OutlinedTextField(value = xMinText, onValueChange = { xMinText = it },
            label = { Text("x min") }, singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = xMaxText, onValueChange = { xMaxText = it },
            label = { Text("x max") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        OutlinedTextField(value = yMinText, onValueChange = { yMinText = it },
            label = { Text("y min") }, singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = yMaxText, onValueChange = { yMaxText = it },
            label = { Text("y max") }, singleLine = true, modifier = Modifier.weight(1f))
    }

    Text("Detail (${n} \u00D7 $n)", style = MaterialTheme.typography.bodySmall)
    Slider(value = steps, onValueChange = { steps = it }, valueRange = 6f..40f)
    Text("Turn (${yaw.toInt()}\u00B0)", style = MaterialTheme.typography.bodySmall)
    Slider(value = yaw, onValueChange = { yaw = it }, valueRange = 0f..360f)
    Text("Tilt (${pitch.toInt()}\u00B0)", style = MaterialTheme.typography.bodySmall)
    Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0f..90f)

    Canvas(modifier = Modifier.fillMaxWidth().height(320.dp).padding(top = 8.dp)) {
        val w = size.width; val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Centre the surface in x and y so it stays on screen while turning.
        val cx = (x0 + x1) / 2.0
        val cy = (y0 + y1) / 2.0
        val cz = (zlo + zhi) / 2.0
        val span = maxOf(x1 - x0, y1 - y0, (zhi - zlo) * 2.0, 1e-6)

        val projected = Array(n + 1) { i ->
            Array(n + 1) { j ->
                val x = x0 + (x1 - x0) * i / n
                val y = y0 + (y1 - y0) * j / n
                val z = grid[i][j]
                if (z.isNaN() || z.isInfinite()) null
                else Plot3D.project(Plot3D.P3((x - cx) / span, (y - cy) / span, (z - cz) / span), yaw.toDouble(), pitch.toDouble())
            }
        }

        val scale = minOf(w, h) * 0.85
        fun px(p: Plot3D.P2): Float = (w / 2f + p.sx * scale).toFloat()
        fun py(p: Plot3D.P2): Float = (h / 2f + p.sy * scale).toFloat()

        // wireframe: lines along both grid directions
        val base = curveColor
        for (i in 0..n) {
            for (j in 0..n) {
                val a = projected[i][j] ?: continue
                if (j + 1 <= n) {
                    val b = projected[i][j + 1] ?: continue
                    val depth = ((a.depth + b.depth) / 2.0).coerceIn(-1.0, 1.0)
                    drawLine(
                        base.copy(alpha = (0.35 + 0.55 * (depth + 1) / 2).toFloat()),
                        Offset(px(a), py(a)), Offset(px(b), py(b)), strokeWidth = 2f,
                    )
                }
                if (i + 1 <= n) {
                    val c = projected[i + 1][j] ?: continue
                    val depth = ((a.depth + c.depth) / 2.0).coerceIn(-1.0, 1.0)
                    drawLine(
                        base.copy(alpha = (0.35 + 0.55 * (depth + 1) / 2).toFloat()),
                        Offset(px(a), py(a)), Offset(px(c), py(c)), strokeWidth = 2f,
                    )
                }
            }
        }
    }
    Text("z range: ${"%.3f".format(zlo)} to ${"%.3f".format(zhi)}", style = MaterialTheme.typography.bodySmall)
}

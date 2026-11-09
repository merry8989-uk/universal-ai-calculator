package com.merry8989uk.universalcalculator.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.merry8989uk.universalcalculator.calc.DepthFade
import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.NiceTicks
import com.merry8989uk.universalcalculator.calc.Plot3D
import com.merry8989uk.universalcalculator.calc.PlotWindow

private val MODES = listOf("2-D", "Parametric", "3-D surface")

/** The window a fresh plot opens on, and what "Reset view" goes back to. */
private val DEFAULT_WINDOW = PlotWindow.of(-6.5, 6.5, -1.0, 1.0)

/** How many shades the 3-D wireframe is drawn in, from the back to the front. */
private const val DEPTH_BUCKETS = 8

/** Samples taken across the visible width. The old fixed-range plot used 400. */
private const val SAMPLES = 400

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
    var window by remember { mutableStateOf(DEFAULT_WINDOW) }
    var fitToken by remember { mutableIntStateOf(0) }

    // The window is the only thing that says what is on screen. The samples are
    // taken over a slightly wider range than it, snapped to a coarse grid, so
    // that dragging re-evaluates the expression a handful of times on the way
    // across the screen instead of on every frame.
    val box = window.sampleRange()
    val samples = remember(expr, box) {
        PlotWindow.sampleCurve(box.first, box.second, (SAMPLES * 1.5).toInt()) { x ->
            try {
                Evaluator.evaluate(expr, degrees = false, vars = mapOf("x" to x))
            } catch (e: Exception) {
                Double.NaN
            }
        }
    }

    // The vertical frame follows the curve when the expression or the reset
    // changes, and is left alone otherwise: a pinch is the user saying what they
    // want to see, and refitting underneath the fingers would undo it a frame
    // later. This is why the fit is keyed on those two things and not on the
    // window, which every gesture moves.
    LaunchedEffect(expr, fitToken) {
        val onScreen = samples.filter { it.x >= window.xMin && it.x <= window.xMax }
        val (lo, hi) = PlotWindow.fitY(onScreen.map { it.y })
        window = window.withYRange(lo, hi)
    }

    val strokes = remember(samples, window) { PlotWindow.runs(samples, window) }

    val gridColor = axisColor.copy(alpha = 0.18f)
    val density = LocalDensity.current
    val labelPaint = remember(axisColor, density) {
        Paint().apply {
            isAntiAlias = true
            color = axisColor.toArgb()
            textSize = with(density) { 9.sp.toPx() }
        }
    }

    Text("Plot y = f(x). Drag to pan, pinch to zoom.", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(
        value = expr, onValueChange = { expr = it }, label = { Text("f(x)") },
        singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            "x \u2208 [${NiceTicks.short(window.xMin)}, ${NiceTicks.short(window.xMax)}]   " +
                "y \u2208 [${NiceTicks.short(window.yMin)}, ${NiceTicks.short(window.yMax)}]",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = {
            window = DEFAULT_WINDOW
            fitToken++
        }) { Text("Reset view") }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .padding(top = 6.dp)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    if (w <= 0f || h <= 0f) return@detectTransformGestures
                    // The pan is applied first and the pinch about the centroid
                    // second, so the fingers' centre lands where it started
                    // whether the gesture was a drag, a pinch or both at once.
                    var next = window.panByPixels(pan.x, pan.y, w, h)
                    if (zoom != 1f) {
                        next = next.zoomBy(zoom.toDouble(), (centroid.x / w).toDouble(), (centroid.y / h).toDouble())
                    }
                    window = next
                }
            },
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val xTicks = NiceTicks.choose(window.xMin, window.xMax, (w / 90f).toInt().coerceIn(2, 12))
        val yTicks = NiceTicks.choose(window.yMin, window.yMax, (h / 70f).toInt().coerceIn(2, 10))

        for (t in xTicks.values) {
            val px = window.screenX(t, w)
            drawLine(gridColor, Offset(px, 0f), Offset(px, h), strokeWidth = 1f)
        }
        for (t in yTicks.values) {
            val py = window.screenY(t, h)
            drawLine(gridColor, Offset(0f, py), Offset(w, py), strokeWidth = 1f)
        }

        // The zero lines are the two gridlines that matter, so they are drawn
        // heavier and last: they are where the eye looks first.
        if (0.0 in window.xMin..window.xMax) {
            val x0 = window.screenX(0.0, w)
            drawLine(axisColor, Offset(x0, 0f), Offset(x0, h), strokeWidth = 2f)
        }
        if (0.0 in window.yMin..window.yMax) {
            val y0 = window.screenY(0.0, h)
            drawLine(axisColor, Offset(0f, y0), Offset(w, y0), strokeWidth = 2f)
        }

        // One path per stroke. A stroke ends where the samples stop being the
        // same piece of curve, which is how a pole is left as a gap instead of
        // being joined up into a wall.
        for (stroke in strokes) {
            if (stroke.size < 2) continue
            val path = Path()
            val first = stroke.first()
            path.moveTo(window.screenX(first.x, w), window.screenY(first.y, h))
            for (index in 1 until stroke.size) {
                val p = stroke[index]
                path.lineTo(window.screenX(p.x, w), window.screenY(p.y, h))
            }
            drawPath(path, color = curveColor, style = Stroke(width = 4f))
        }

        // Labels go on last so the curve cannot run through a number.
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            for (t in xTicks.values) {
                val text = NiceTicks.format(t, xTicks.step)
                val half = labelPaint.measureText(text) / 2f
                val px = window.screenX(t, w)
                val x = (px - half).coerceIn(1f, (w - 2f * half).coerceAtLeast(1f))
                native.drawText(text, x, h - 5f, labelPaint)
            }
            for (t in yTicks.values) {
                val py = window.screenY(t, h)
                if (py < 12f || py > h - 4f) continue
                native.drawText(NiceTicks.format(t, yTicks.step), 3f, py - 5f, labelPaint)
            }
        }
    }
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

    // The turn, the projection and the fade are the same for every frame at a
    // given angle, so they are worked out once per angle instead of once per
    // frame. This used to happen inside the draw lambda, which rebuilt the whole
    // surface and made a fresh colour for each of the thousands of lines on every
    // single frame; the drawing now only scales what is already here.
    val wire = remember(grid, yaw, pitch) {
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
                else Plot3D.project(
                    Plot3D.P3((x - cx) / span, (y - cy) / span, (z - cz) / span),
                    yaw.toDouble(),
                    pitch.toDouble(),
                )
            }
        }
        // Segments are grouped into a handful of shades rather than given a
        // colour each, so the draw call can paint a whole depth band in one pass.
        val shades = Array(DEPTH_BUCKETS) { ArrayList<FloatArray>(64) }
        fun link(a: Plot3D.P2, b: Plot3D.P2) {
            val depth = (a.depth + b.depth) / 2.0
            shades[DepthFade.bucket(depth, DEPTH_BUCKETS)].add(
                floatArrayOf(a.sx.toFloat(), a.sy.toFloat(), b.sx.toFloat(), b.sy.toFloat()),
            )
        }
        for (i in 0..n) {
            for (j in 0..n) {
                val a = projected[i][j] ?: continue
                if (j + 1 <= n) projected[i][j + 1]?.let { link(a, it) }
                if (i + 1 <= n) projected[i + 1][j]?.let { link(a, it) }
            }
        }
        shades
    }

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

    Text("Detail ($n \u00D7 $n)", style = MaterialTheme.typography.bodySmall)
    Slider(value = steps, onValueChange = { steps = it }, valueRange = 6f..40f)
    Text("Turn (${yaw.toInt()}\u00B0)", style = MaterialTheme.typography.bodySmall)
    Slider(value = yaw, onValueChange = { yaw = it }, valueRange = 0f..360f)
    Text("Tilt (${pitch.toInt()}\u00B0)", style = MaterialTheme.typography.bodySmall)
    Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0f..90f)

    Canvas(modifier = Modifier.fillMaxWidth().height(320.dp).padding(top = 8.dp)) {
        val w = size.width; val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val scale = minOf(w, h) * 0.85f
        val halfW = w / 2f
        val halfH = h / 2f
        for (bucket in 0 until DEPTH_BUCKETS) {
            val segments = wire[bucket]
            if (segments.isEmpty()) continue
            val path = Path()
            for (s in segments) {
                path.moveTo(halfW + s[0] * scale, halfH + s[1] * scale)
                path.lineTo(halfW + s[2] * scale, halfH + s[3] * scale)
            }
            drawPath(
                path,
                color = curveColor.copy(alpha = DepthFade.alphaFor(bucket, DEPTH_BUCKETS)),
                style = Stroke(width = 2f),
            )
        }
    }
    Text("z range: ${"%.3f".format(zlo)} to ${"%.3f".format(zhi)}", style = MaterialTheme.typography.bodySmall)
}

package com.merry8989uk.universalcalculator.calc

import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * The frame a 2-D plot is drawn in, and every sum that moves it.
 *
 * The screen used to hold the window in two text boxes and hand the canvas two
 * bare ranges, so the only thing it could do with a window was draw it. Panning
 * and zooming need the window to be a value that can be moved, kept, and passed
 * back to the canvas, so it is a value here instead.
 *
 * Nothing in this file knows what a pixel is. Screen positions are asked for in
 * fractions of a canvas the caller measures, which is what lets the whole of it
 * be tested on a plain JVM with no device in sight.
 */
data class PlotWindow(
    val xMin: Double,
    val xMax: Double,
    val yMin: Double,
    val yMax: Double,
) {
    val xSpan: Double get() = xMax - xMin
    val ySpan: Double get() = yMax - yMin

    /**
     * Where [x] lands across a canvas [widthPx] wide. The caller guarantees a
     * canvas with a size; a zero width has no left or right to speak of.
     */
    fun screenX(x: Double, widthPx: Float): Float =
        ((x - xMin) / xSpan * widthPx).toFloat()

    /**
     * Where [y] lands down a canvas [heightPx] tall. Screen y grows downwards and
     * mathematical y grows upwards, so the subtraction is the whole conversion.
     */
    fun screenY(y: Double, heightPx: Float): Float =
        (heightPx - (y - yMin) / ySpan * heightPx).toFloat()

    /** The x value under a pixel, which is what a pinch has to keep still. */
    fun dataX(px: Float, widthPx: Float): Double =
        xMin + (px / widthPx).toDouble() * xSpan

    /** The y value under a pixel, measured up from the bottom of the canvas. */
    fun dataY(py: Float, heightPx: Float): Double =
        yMin + ((heightPx - py) / heightPx).toDouble() * ySpan

    /**
     * The window after a drag of ([dxPx], [dyPx]).
     *
     * The window moves against the finger, because a drag is a hand pushing the
     * paper: the paper slides right, so what the frame shows slides left. Without
     * the sign flip the plot runs away from the thumb.
     */
    fun panByPixels(dxPx: Float, dyPx: Float, widthPx: Float, heightPx: Float): PlotWindow {
        if (widthPx <= 0f || heightPx <= 0f) return this
        val dx = -(dxPx / widthPx) * xSpan
        val dy = (dyPx / heightPx) * ySpan
        return PlotWindow(xMin + dx, xMax + dx, yMin + dy, yMax + dy)
    }

    /**
     * The window after a pinch, keeping the point under the fingers where it is.
     *
     * [factor] above one is a zoom in, because zooming in means showing a
     * smaller stretch of the world. [focusX] and [focusY] are fractions of the
     * canvas measured from the top left, which is how the gesture reports its
     * centroid; the anchored point is fixed before the new span is centred on it,
     * so the fingers stay on the same feature of the curve.
     */
    fun zoomBy(factor: Double, focusX: Double = 0.5, focusY: Double = 0.5): PlotWindow {
        if (!factor.isFinite() || factor <= 0.0) return this
        val fx = focusX.coerceIn(0.0, 1.0)
        val fy = focusY.coerceIn(0.0, 1.0)
        val newX = (xSpan / factor).coerceIn(MIN_SPAN, MAX_SPAN)
        val newY = (ySpan / factor).coerceIn(MIN_SPAN, MAX_SPAN)
        val anchorX = xMin + fx * xSpan
        val anchorY = yMax - fy * ySpan
        val newMinX = anchorX - fx * newX
        val newMaxY = anchorY + fy * newY
        return PlotWindow(newMinX, newMinX + newX, newMaxY - newY, newMaxY)
    }

    /** The same window with a different vertical range, or this one if it is nonsense. */
    fun withYRange(lo: Double, hi: Double): PlotWindow {
        if (!lo.isFinite() || !hi.isFinite() || hi - lo < MIN_SPAN) return this
        return PlotWindow(xMin, xMax, lo, hi)
    }

    /**
     * The range to take samples over, which is wider than the window and
     * snapped to a coarse grid.
     *
     * A drag asks for a new window on every frame, and re-evaluating the
     * expression on every one of those frames is the difference between a
     * plot that follows the thumb and one that stutters behind it. Snapping
     * the range to a sixteenth of its own width means the samples only have
     * to be rebuilt when the window has moved a sixteenth of a screen, and
     * the padding either side means the samples already in hand still cover
     * the window that has just arrived. The points that end up outside the
     * window cost nothing to draw, because the canvas clips them.
     *
     * The width is snapped in eighth-octaves for the same reason, so a pinch
     * does not rebuild the curve sixty times a second either.
     */
    fun sampleRange(pad: Double = 0.25, cells: Int = 16, octaveSteps: Int = 8): Pair<Double, Double> {
        if (!(xSpan > 0.0) || !xSpan.isFinite()) return xMin to xMax
        val n = cells.coerceAtLeast(1)
        val steps = octaveSteps.coerceAtLeast(1)
        val width = 2.0.pow(round(log2(xSpan) * steps) / steps)
        if (!(width > 0.0) || !width.isFinite()) return xMin to xMax
        val cell = width / n
        val snappedLo = floor(xMin / cell) * cell
        val lo = snappedLo - pad * width
        val hi = snappedLo + width * (1.0 + pad + 1.0 / n)
        return lo to hi
    }

    /**
     * True when the step from [previous] to [y] is too tall to be a slope.
     *
     * A sampled pole does not arrive as a hole: the samples either side of it are
     * enormous and opposite in sign, so joining them draws a straight wall where
     * the function has nothing at all — the false vertical line through tan(x).
     * A jump of more than [factor] times the visible height cannot be part of a
     * curve the user can see, so the pen is lifted there instead.
     *
     * A sample that is not a number is also a place to lift the pen, which is
     * what the old NaN check did and what still has to happen for a domain hole.
     */
    fun isPoleJump(previous: Double, y: Double, factor: Double = POLE_JUMP_FACTOR): Boolean {
        if (!previous.isFinite() || !y.isFinite()) return true
        return abs(y - previous) > factor * ySpan
    }

    companion object {
        /**
         * How many window heights a step has to cover before it is called a pole
         * rather than a steep bit of curve. Two heights is far beyond anything
         * that fits on screen, so a legitimate steep slope never reaches it.
         */
        const val POLE_JUMP_FACTOR = 2.0

        /** A range narrower than this is a zoom that has run out of meaning. */
        private const val MIN_SPAN = 1e-12

        /** A range wider than this is a zoom that has lost the plot. */
        private const val MAX_SPAN = 1e12

        /** A range more than this many times its own middle is driven by outliers. */
        private const val OUTLIER_RATIO = 8.0

        /**
         * A window built from four numbers that may have come from a text field.
         *
         * An inverted or empty range is put back in order rather than rejected,
         * because the only caller is a screen that has to draw something.
         */
        fun of(xMin: Double, xMax: Double, yMin: Double, yMax: Double): PlotWindow {
            val (xl, xh) = ordered(xMin, xMax)
            val (yl, yh) = ordered(yMin, yMax)
            return PlotWindow(xl, xh, yl, yh)
        }

        private fun ordered(a: Double, b: Double): Pair<Double, Double> {
            var lo = if (a.isFinite()) a else -1.0
            var hi = if (b.isFinite()) b else 1.0
            if (hi < lo) { val t = lo; lo = hi; hi = t }
            if (hi - lo < MIN_SPAN) {
                val mid = (lo + hi) / 2.0
                lo = mid - 0.5
                hi = mid + 0.5
            }
            return lo to hi
        }

        /**
         * Samples f across [xMin]..[xMax] in [steps] equal steps.
         *
         * A value the expression refuses becomes NaN, which is a hole to be left
         * out of the drawing rather than a number to be plotted. The sampling is
         * even in x, so the samples nearest a pole get very close to it and the
         * pole rule has something to catch.
         */
        fun sampleCurve(
            xMin: Double,
            xMax: Double,
            steps: Int,
            f: (Double) -> Double,
        ): List<CurvePoint> {
            if (!(xMax > xMin) || steps < 1) return emptyList()
            val n = steps.coerceAtMost(20000)
            val out = ArrayList<CurvePoint>(n + 1)
            for (i in 0..n) {
                val x = xMin + (xMax - xMin) * i / n
                val y = try {
                    f(x)
                } catch (e: Exception) {
                    Double.NaN
                }
                out.add(CurvePoint(x, y))
            }
            return out
        }

        /**
         * Splits a sampled curve into the runs that can be joined without lifting
         * the pen.
         *
         * This is the rule the drawing follows: one path per run, and a run ends
         * wherever [PlotWindow.isPoleJump] says the next sample is not the same
         * piece of curve. Keeping it here rather than in the draw call is what
         * makes it possible to say, in a test, that tan(x) is two strokes and not
         * one wall.
         */
        fun runs(
            points: List<CurvePoint>,
            window: PlotWindow,
            factor: Double = POLE_JUMP_FACTOR,
        ): List<List<CurvePoint>> {
            val out = ArrayList<List<CurvePoint>>()
            var current = ArrayList<CurvePoint>()
            var previous: CurvePoint? = null
            for (p in points) {
                val prev = previous
                if (prev != null && window.isPoleJump(prev.y, p.y, factor)) {
                    if (current.isNotEmpty()) out.add(current)
                    current = ArrayList()
                }
                if (p.y.isFinite()) current.add(p) else { if (current.isNotEmpty()) out.add(current); current = ArrayList() }
                previous = p
            }
            if (current.isNotEmpty()) out.add(current)
            // A run of one point is a stroke with no length, so it is not drawn.
            // Around a pole the samples either side are several consecutive huge
            // jumps, and each of those leaves a single point behind; dropping
            // them is what makes tan(x) read as three strokes and not as seven.
            return out.filter { it.size >= 2 }
        }

        /**
         * The vertical frame that fits a set of samples, in the two passes a
         * plotted function needs.
         *
         * The obvious answer — the smallest and largest value — is the wrong one
         * for anything with a pole in it: tan(x) over a few periods reaches 1e15
         * either side of each asymptote, and a frame that contains 1e15 squashes
         * every visible part of the curve onto the axis. So when the outer 2% of
         * the samples stretch the range out by more than [OUTLIER_RATIO], the
         * middle of the data is used instead. A function without outliers keeps
         * its exact range, which is what makes the numbers on the axis meaningful.
         */
        fun fitY(values: Iterable<Double>, pad: Double = 0.1, tail: Double = 0.02): Pair<Double, Double> {
            val finite = values.filter { it.isFinite() }.sorted()
            if (finite.isEmpty()) return -1.0 to 1.0
            val rawLo = finite.first()
            val rawHi = finite.last()
            val t = tail.coerceIn(0.0, 0.25)
            val loIndex = (finite.size * t).toInt().coerceIn(0, finite.size - 1)
            val hiIndex = (finite.size * (1.0 - t)).toInt().coerceIn(0, finite.size - 1)
            val trimmedLo = finite[loIndex]
            val trimmedHi = finite[hiIndex]
            val rawSpan = rawHi - rawLo
            val trimmedSpan = trimmedHi - trimmedLo
            val useTrimmed = trimmedSpan > 0.0 && rawSpan > OUTLIER_RATIO * trimmedSpan
            val lo = if (useTrimmed) trimmedLo else rawLo
            val hi = if (useTrimmed) trimmedHi else rawHi
            if (hi - lo < MIN_SPAN) {
                val mid = (lo + hi) / 2.0
                return (mid - 1.0) to (mid + 1.0)
            }
            val p = (hi - lo) * pad.coerceIn(0.0, 1.0)
            return (lo - p) to (hi + p)
        }
    }
}

/** One sample of a plotted function: where it was asked and what came back. */
data class CurvePoint(val x: Double, val y: Double)

/**
 * The gridlines and the numbers beside them.
 *
 * A tick every 0.37 units is arithmetically correct and useless to read, so the
 * step is chosen from the 1, 2, 5, 10 family that people count in, and the label
 * is written to the precision that step actually carries. Both halves have to
 * agree — a step of 0.5 labelled "1" is worse than no label at all — which is why
 * they are worked out together here instead of at the call site.
 */
object NiceTicks {

    /** A step and the tick values that sit on it inside the range. */
    data class TickSet(val step: Double, val values: List<Double>) {
        val isEmpty: Boolean get() = values.isEmpty()
    }

    /**
     * The nicest step for a range, aimed at about [target] ticks.
     *
     * Rounds the ideal step up or down to the nearest of 1, 2 or 5 times a power
     * of ten, which is the family that stays readable however far the window is
     * zoomed.
     */
    fun step(span: Double, target: Int): Double {
        if (!span.isFinite() || span <= 0.0) return 1.0
        val want = target.coerceIn(1, 100)
        val raw = span / want
        if (!raw.isFinite() || raw <= 0.0) return 1.0
        val magnitude = 10.0.pow(floor(log10(raw)))
        val norm = raw / magnitude
        // The break points are the ones that leave the count nearest the target:
        // rounding 2.2 up to 5 throws away more than half the ticks asked for.
        val nice = when {
            norm < 1.5 -> 1.0
            norm < 3.0 -> 2.0
            norm < 7.0 -> 5.0
            else -> 10.0
        }
        return nice * magnitude
    }

    /**
     * The ticks inside [min]..[max] on the nice step nearest [target] of them.
     *
     * Multiples of the step are walked from the first one inside the range, so
     * the numbers on the axis are exact multiples of it rather than the
     * accumulated remains of repeated addition. An empty list is a real answer: a
     * range too narrow or too odd to hold a tick should be drawn without any.
     */
    fun choose(min: Double, max: Double, target: Int = 6): TickSet {
        if (!min.isFinite() || !max.isFinite() || max <= min) return TickSet(1.0, emptyList())
        val s = step(max - min, target)
        if (!s.isFinite() || s <= 0.0) return TickSet(1.0, emptyList())
        val first = ceil(min / s - 1e-9)
        val last = floor(max / s + 1e-9)
        if (!first.isFinite() || !last.isFinite() || last < first) return TickSet(s, emptyList())
        val count = (last - first).toInt()
        if (count < 0 || count > 500) return TickSet(s, emptyList())
        val values = (0..count).map { i ->
            val v = (first + i) * s
            if (v == 0.0) 0.0 else v
        }
        return TickSet(s, values)
    }

    /** Just the values, for callers that do not need the step. */
    fun ticks(min: Double, max: Double, target: Int = 6): List<Double> = choose(min, max, target).values

    /**
     * A tick value written the way its step can carry it.
     *
     * The decimal places come from the step, not from the value, so every label
     * on an axis has the same shape: 0.5, 1.0, 1.5 rather than 0.5, 1, 1.5.
     */
    fun format(value: Double, step: Double): String {
        if (!value.isFinite()) return ""
        if (value == 0.0) return "0"
        val magnitude = abs(value)
        if (magnitude >= 1e7 || magnitude < 1e-4) return String.format(Locale.ROOT, "%.1e", value)
        val places = when {
            !step.isFinite() || step <= 0.0 -> 2
            step >= 1.0 -> 0
            else -> ceil(-log10(step)).toInt().coerceIn(0, 6)
        }
        val text = String.format(Locale.ROOT, "%.${places}f", value)
        return if (text == "-0" || text == "-0.0") "0" else text
    }

    /** A value for a readout rather than an axis: four figures and no ceremony. */
    fun short(value: Double): String {
        if (!value.isFinite()) return "-"
        if (value == 0.0) return "0"
        return String.format(Locale.ROOT, "%.4g", value)
    }
}

/**
 * How much of the wireframe's colour a line at a given depth keeps.
 *
 * The surface used to make a fresh colour for every one of the thousands of
 * lines on every frame, which is a lot of objects to throw away to draw a shade.
 * The depths are the same from frame to frame for a given turn, so they are
 * turned into a handful of shades once per turn and the drawing picks one.
 */
object DepthFade {

    /** The faintest a line at the back of the surface gets. */
    const val FARTHEST = 0.35f

    /** The strongest a line at the front of the surface gets. */
    const val NEAREST = 0.90f

    /** The alpha for a depth in -1..1, where -1 is the back of the surface. */
    fun alpha(depth: Double): Float {
        val d = if (depth.isFinite()) depth.coerceIn(-1.0, 1.0) else 0.0
        return (FARTHEST + (NEAREST - FARTHEST) * ((d + 1.0) / 2.0)).toFloat()
    }

    /** The shade [depth] belongs to, out of [buckets] shades. */
    fun bucket(depth: Double, buckets: Int): Int {
        val n = buckets.coerceAtLeast(1)
        val d = if (depth.isFinite()) depth.coerceIn(-1.0, 1.0) else 0.0
        val t = (d + 1.0) / 2.0
        return (t * (n - 1)).roundToInt().coerceIn(0, n - 1)
    }

    /** The alpha for a shade, so the drawing and [alpha] never disagree. */
    fun alphaFor(bucket: Int, buckets: Int): Float {
        val n = buckets.coerceAtLeast(1)
        val t = if (n == 1) 0.5 else bucket.coerceIn(0, n - 1).toDouble() / (n - 1)
        return (FARTHEST + (NEAREST - FARTHEST) * t).toFloat()
    }
}

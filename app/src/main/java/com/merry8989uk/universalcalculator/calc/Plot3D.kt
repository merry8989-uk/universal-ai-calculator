package com.merry8989uk.universalcalculator.calc

import kotlin.math.cos
import kotlin.math.sin

/**
 * The maths behind the 3-D surface plot and the parametric plot. Kept pure so
 * the projection can be tested without a screen.
 */
object Plot3D {

    data class P3(val x: Double, val y: Double, val z: Double)

    /** A projected point: screen x, screen y (up is negative) and a depth. */
    data class P2(val sx: Double, val sy: Double, val depth: Double)

    /** Rotates by [yaw] about z, then by [pitch] about x, and projects. */
    fun project(p: P3, yawDeg: Double, pitchDeg: Double): P2 {
        val yaw = Math.toRadians(yawDeg)
        val pitch = Math.toRadians(pitchDeg)
        val x1 = p.x * cos(yaw) - p.y * sin(yaw)
        val y1 = p.x * sin(yaw) + p.y * cos(yaw)
        val z1 = p.z
        val y2 = y1 * cos(pitch) - z1 * sin(pitch)
        val z2 = y1 * sin(pitch) + z1 * cos(pitch)
        return P2(x1, -z2, y2)
    }

    /**
     * Samples z = f(x, y) on a (steps+1) x (steps+1) grid. A cell that cannot
     * be evaluated becomes NaN.
     */
    fun sampleSurface(
        expr: String,
        xMin: Double,
        xMax: Double,
        yMin: Double,
        yMax: Double,
        steps: Int,
        degrees: Boolean = false,
    ): Array<DoubleArray> {
        val n = steps.coerceIn(2, 80)
        val out = Array(n + 1) { DoubleArray(n + 1) }
        for (i in 0..n) {
            val x = xMin + (xMax - xMin) * i / n
            for (j in 0..n) {
                val y = yMin + (yMax - yMin) * j / n
                out[i][j] = try {
                    Evaluator.evaluate(expr, degrees, mapOf("x" to x, "y" to y))
                } catch (e: Exception) {
                    Double.NaN
                }
            }
        }
        return out
    }

    /** Samples a parametric curve (x(t), y(t)) over [tMin, tMax]. */
    fun sampleParametric(
        xExpr: String,
        yExpr: String,
        tMin: Double,
        tMax: Double,
        steps: Int,
        degrees: Boolean = false,
    ): List<Pair<Double, Double>> {
        val n = steps.coerceIn(2, 4000)
        val out = ArrayList<Pair<Double, Double>>(n + 1)
        for (i in 0..n) {
            val t = tMin + (tMax - tMin) * i / n
            val x = try { Evaluator.evaluate(xExpr, degrees, mapOf("t" to t)) } catch (e: Exception) { Double.NaN }
            val y = try { Evaluator.evaluate(yExpr, degrees, mapOf("t" to t)) } catch (e: Exception) { Double.NaN }
            out.add(x to y)
        }
        return out
    }

    /** The smallest and largest finite value in a surface grid. */
    fun zRange(grid: Array<DoubleArray>): Pair<Double, Double> {
        var lo = Double.MAX_VALUE
        var hi = -Double.MAX_VALUE
        for (row in grid) for (v in row) {
            if (v.isNaN() || v.isInfinite()) continue
            if (v < lo) lo = v
            if (v > hi) hi = v
        }
        if (lo > hi) return -1.0 to 1.0
        if (hi - lo < 1e-9) return (lo - 1.0) to (hi + 1.0)
        return lo to hi
    }
}

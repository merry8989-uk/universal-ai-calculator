package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs
import kotlin.math.pow

/**
 * Limits, worked out numerically.
 *
 * There is no symbolic engine behind this: the expression is sampled closer and
 * closer to the point and the results are extrapolated to zero step size
 * (Richardson extrapolation). For a smooth function the central sample is
 * accurate to even powers of the step, and a one-sided sample to odd powers, so
 * the right powers are removed in each case.
 *
 * A limit that runs away is reported as ±Infinity rather than a wrong number.
 */
object Limits {

    enum class Side { BOTH, LEFT, RIGHT }

    private const val STEPS = 8

    /** Null when the limit cannot be worked out. */
    fun limit(
        expr: String,
        at: Double,
        side: Side = Side.BOTH,
        degrees: Boolean = false,
    ): Double? {
        val samples = ArrayList<Double>(STEPS)
        for (k in 1..STEPS) {
            val h = 10.0.pow(-k.toDouble())
            val value = when (side) {
                Side.BOTH -> {
                    val ahead = evaluate(expr, "x", at + h, degrees) ?: return null
                    val behind = evaluate(expr, "x", at - h, degrees) ?: return null
                    (ahead + behind) / 2.0
                }
                Side.RIGHT -> evaluate(expr, "x", at + h, degrees) ?: return null
                Side.LEFT -> evaluate(expr, "x", at - h, degrees) ?: return null
            }
            if (value.isNaN() || value.isInfinite()) return null
            samples.add(value)
        }
        if (runsAway(samples)) {
            return if (samples.last() > 0) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY
        }
        val powers = if (side == Side.BOTH) doubleArrayOf(2.0, 4.0, 6.0)
        else doubleArrayOf(1.0, 2.0, 3.0)
        return extrapolate(samples, powers)
    }

    /**
     * The limit as x tends to ±Infinity, by substituting x = ±1/t and taking
     * t to zero from above.
     */
    fun limitAtInfinity(expr: String, negative: Boolean = false, degrees: Boolean = false): Double? {
        val replacement = if (negative) "(-1/t)" else "(1/t)"
        val substituted = substituteX(expr, replacement)
        val samples = ArrayList<Double>(STEPS)
        for (k in 1..STEPS) {
            val t = 10.0.pow(-k.toDouble())
            val value = evaluate(substituted, "t", t, degrees) ?: return null
            if (value.isNaN() || value.isInfinite()) return null
            samples.add(value)
        }
        if (runsAway(samples)) {
            return if (samples.last() > 0) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY
        }
        return extrapolate(samples, doubleArrayOf(1.0, 2.0, 3.0))
    }

    /**
     * Replaces a standalone `x` with [replacement], leaving names that merely
     * contain an x — `exp`, `max` — untouched.
     */
    internal fun substituteX(expr: String, replacement: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c.isLetter() || c == '_') {
                val start = i
                while (i < expr.length && (expr[i].isLetterOrDigit() || expr[i] == '_')) i++
                val name = expr.substring(start, i)
                out.append(if (name.equals("x", ignoreCase = true)) replacement else name)
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    /** Removes the leading error terms, leaving the value at step size zero. */
    private fun extrapolate(samples: List<Double>, powers: DoubleArray): Double {
        var values = samples
        for (p in powers) {
            if (values.size < 2) break
            val ratio = 10.0.pow(p)
            val next = ArrayList<Double>(values.size - 1)
            for (i in 0 until values.size - 1) {
                next.add((values[i + 1] * ratio - values[i]) / (ratio - 1))
            }
            values = next
        }
        return values.first()
    }

    /**
     * True when the samples keep growing as the step shrinks — the sign of a
     * limit that does not exist as a number.
     */
    private fun runsAway(samples: List<Double>): Boolean {
        if (samples.size < 3) return false
        val first = abs(samples.first())
        val last = abs(samples.last())
        return last > 1e6 && last > 10 * maxOf(first, 1e-30)
    }

    private fun evaluate(expr: String, varName: String, value: Double, degrees: Boolean): Double? =
        try {
            Evaluator.evaluate(expr, degrees, mapOf(varName to value))
        } catch (e: Exception) {
            null
        }
}

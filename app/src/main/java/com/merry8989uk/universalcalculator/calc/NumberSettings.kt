package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * How wide a number is when the programmer's keys work on it.
 *
 * A 32-bit machine wraps at two thousand million; a 64-bit one does not. The
 * difference is visible the moment you press NOT, so it is a setting.
 */
enum class BitWidth {
    BITS_32,
    BITS_64;

    val bits: Int get() = if (this == BITS_32) 32 else 64

    /**
     * Fits a value into this width, the way the machine would.
     *
     * For 32 bits that means wrapping through an `Int`, so `not(0)` is −1 and
     * `0xFFFFFFFF + 1` is 0.
     */
    fun fit(v: Long): Long = if (this == BITS_32) v.toInt().toLong() else v

    val label: String get() = "$bits bits"
}

/** How a complex number is written out. */
enum class ComplexFormat {
    /** a + bi */
    RECTANGULAR,
    /** r ∠ θ, the way an engineer writes it */
    POLAR;

    val label: String
        get() = when (this) {
            RECTANGULAR -> "Rectangular  a+bi"
            POLAR -> "Polar  r\u2220\u03B8"
        }

    val example: String
        get() = when (this) {
            RECTANGULAR -> "3+4i"
            POLAR -> "5\u222053.13\u00B0"
        }
}

/**
 * How an interval is written.
 *
 * All three say the same thing; they are the three conventions in print:
 *
 *   PARENTHESES   [0, 1) ∪ (2, +\u221E)
 *   PLAIN         [0; 1) \u222A (2; \u221E)
 *   BRACKETED     [0; 1] \u222A [2; +\u221E[
 */
enum class IntervalNotation {
    PARENTHESES,
    PLAIN,
    BRACKETED;

    val label: String
        get() = when (this) {
            PARENTHESES -> "[0, 1) \u222A (2, +\u221E)"
            PLAIN -> "[0; 1) \u222A (2; \u221E)"
            BRACKETED -> "[0; 1] \u222A [2; +\u221E["
        }
}

/**
 * Writing a complex number and an interval the way the settings ask.
 */
object NumberStyle {

    /** A complex number in the chosen form. */
    fun complex(
        re: Double,
        im: Double,
        format: ComplexFormat,
        degrees: Boolean = true,
    ): String = when (format) {
        ComplexFormat.RECTANGULAR -> Complex.text(Complex(re, im))
        ComplexFormat.POLAR -> {
            val r = hypot(re, im)
            val theta = atan2(im, re)
            val shown = if (degrees) Math.toDegrees(theta) else theta
            val unit = if (degrees) "\u00B0" else " rad"
            "${Format.number(r)}\u2220${Format.number(shown)}$unit"
        }
    }

    /**
     * An interval from [lo] to [hi], with each end either included or not.
     *
     * An infinity never gets a closed bracket, whatever the style, because there
     * is no end there to include.
     */
    fun interval(
        lo: Double,
        hi: Double,
        includeLo: Boolean,
        includeHi: Boolean,
        notation: IntervalNotation,
    ): String {
        // an end at infinity is written as the symbol, never as the word
        val loText = if (lo.isFinite()) Format.number(lo) else infinity(false, notation)
        val hiText = if (hi.isFinite()) Format.number(hi) else infinity(true, notation)
        val loClosed = includeLo && lo.isFinite()
        val hiClosed = includeHi && hi.isFinite()

        return when (notation) {
            IntervalNotation.PARENTHESES -> {
                val loSign = if (lo.isFinite()) (if (loClosed) "[" else "(") else "("
                val hiSign = if (hi.isFinite()) (if (hiClosed) "]" else ")") else ")"
                "$loSign$loText, $hiText$hiSign"
            }
            IntervalNotation.PLAIN -> {
                val loSign = if (lo.isFinite()) (if (loClosed) "[" else "(") else "("
                val hiSign = if (hi.isFinite()) (if (hiClosed) "]" else ")") else ")"
                "$loSign$loText; $hiText$hiSign"
            }
            IntervalNotation.BRACKETED -> {
                // the bracketed style uses a reversed bracket for an open end
                val loSign = if (lo.isFinite()) (if (loClosed) "[" else "]") else "]"
                val hiSign = if (hi.isFinite()) (if (hiClosed) "]" else "[") else "["
                "$loSign$loText; $hiText$hiSign"
            }
        }
    }

    /** The same interval, but with the two unbounded halves joined. */
    fun union(
        first: String,
        second: String,
    ): String = "$first \u222A $second"

    /**
     * The sign to print for an infinity, which differs by style.
     *
     * The plain style leaves off the plus, the way most books write it.
     */
    fun infinity(positive: Boolean, notation: IntervalNotation): String = when {
        !positive -> "-\u221E"
        notation == IntervalNotation.PLAIN -> "\u221E"
        else -> "+\u221E"
    }
}

/**
 * Statistics that can be told how often each value occurred.
 *
 * The frequency column is the `n` column of a statistical editor: a value of 7
 * seen four times counts four times, not once.
 */
object WeightedStatistics {

    /** The total count, which with weights is the sum of the weights. */
    fun count(xs: List<Double>, weights: List<Double>?): Double =
        if (weights == null) xs.size.toDouble() else weights.sum()

    fun mean(xs: List<Double>, weights: List<Double>? = null): Double {
        if (xs.isEmpty()) return Double.NaN
        if (weights == null) return xs.sum() / xs.size
        val total = weights.sum()
        if (abs(total) < 1e-15) return Double.NaN
        return xs.indices.sumOf { xs[it] * weights.getOrElse(it) { 0.0 } } / total
    }

    /** The sum of squared deviations, weighted or not. */
    private fun sumSquares(xs: List<Double>, weights: List<Double>?, m: Double): Double =
        if (weights == null) {
            xs.sumOf { (it - m) * (it - m) }
        } else {
            xs.indices.sumOf { weights.getOrElse(it) { 0.0 } * (xs[it] - m) * (xs[it] - m) }
        }

    /** Population variance: divide by the total count. */
    fun variance(xs: List<Double>, weights: List<Double>? = null): Double {
        if (xs.isEmpty()) return Double.NaN
        val n = count(xs, weights)
        if (n <= 0) return Double.NaN
        return sumSquares(xs, weights, mean(xs, weights)) / n
    }

    /** Sample variance: divide by one less than the total count. */
    fun sampleVariance(xs: List<Double>, weights: List<Double>? = null): Double {
        val n = count(xs, weights)
        if (n <= 1) return Double.NaN
        return sumSquares(xs, weights, mean(xs, weights)) / (n - 1)
    }

    fun sd(xs: List<Double>, weights: List<Double>? = null): Double =
        Math.sqrt(variance(xs, weights))

    fun sampleSd(xs: List<Double>, weights: List<Double>? = null): Double =
        Math.sqrt(sampleVariance(xs, weights))

    /**
     * The middle value once each value is repeated by its weight.
     *
     * With weights this is the value where the running total of weights first
     * reaches half of the whole.
     */
    fun median(xs: List<Double>, weights: List<Double>? = null): Double {
        if (xs.isEmpty()) return Double.NaN
        if (weights == null) {
            val sorted = xs.sorted()
            val n = sorted.size
            return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }
        val pairs = xs.indices
            .map { xs[it] to weights.getOrElse(it) { 0.0 } }
            .sortedBy { it.first }
        val total = pairs.sumOf { it.second }
        if (total <= 0) return Double.NaN
        var running = 0.0
        for ((value, w) in pairs) {
            running += w
            if (running >= total / 2.0) return value
        }
        return pairs.last().first
    }
}

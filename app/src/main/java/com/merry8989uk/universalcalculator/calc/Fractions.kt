package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs
import kotlin.math.floor

/**
 * Turns a decimal result into a simple fraction, so 0.75 can also be read as
 * 3/4. Uses the continued-fraction expansion, which gives the best rational
 * approximation for a given denominator limit.
 */
object Fractions {

    data class F(val numerator: Long, val denominator: Long)

    /**
     * Best rational approximation of [value] with denominator at most [maxDen].
     * Returns null when nothing is close enough (for example for π).
     */
    fun fromDecimal(value: Double, maxDen: Long = 10000, tolerance: Double = 1e-9): F? {
        if (!value.isFinite()) return null
        if (maxDen < 1) return null
        val sign = if (value < 0) -1L else 1L
        val x = abs(value)

        var h1 = 1L; var h0 = 0L
        var k1 = 0L; var k0 = 1L
        var b = x
        var guard = 0
        while (guard++ < 64) {
            val a = floor(b).toLong()
            val h2 = a * h1 + h0
            val k2 = a * k1 + k0
            if (k2 > maxDen || k2 <= 0L) break
            h0 = h1; h1 = h2
            k0 = k1; k1 = k2
            val frac = b - a
            if (frac < 1e-12) break
            b = 1.0 / frac
        }
        if (k1 == 0L) return null
        val approx = h1.toDouble() / k1.toDouble()
        if (abs(approx - x) > tolerance * maxOf(1.0, x)) return null
        return F(sign * h1, k1)
    }

    /** "3/4" — or null when the value is a whole number or has no neat fraction. */
    fun text(value: Double, maxDen: Long = 10000): String? {
        val f = fromDecimal(value, maxDen) ?: return null
        if (f.denominator == 1L) return null
        return "${f.numerator}/${f.denominator}"
    }

    /** "2 3/4" — a mixed number, or null when the value is whole or not a fraction. */
    fun mixedText(value: Double, maxDen: Long = 10000): String? {
        val f = fromDecimal(value, maxDen) ?: return null
        if (f.denominator == 1L) return null
        val whole = f.numerator / f.denominator
        val rest = abs(f.numerator % f.denominator)
        if (whole == 0L) return "${f.numerator}/${f.denominator}"
        return "$whole $rest/${f.denominator}"
    }
}

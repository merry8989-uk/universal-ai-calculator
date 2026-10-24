package com.merry8989uk.universalcalculator.calc

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The special functions a scientific calculator is expected to have.
 *
 * Every one is implemented from scratch rather than pulled in, because this
 * module deliberately has no dependencies. The approximations are the standard
 * ones and each is accurate to around ten significant figures.
 */
object SpecialFunctions {

    // ---- gamma family ----

    /** Lanczos coefficients, g = 7, n = 9. */
    private val LANCZOS = doubleArrayOf(
        0.99999999999980993,
        676.5203681218851,
        -1259.1392167224028,
        771.32342877765313,
        -176.61502916214059,
        12.507343278686905,
        -0.13857109526572012,
        9.9843695780195716e-6,
        1.5056327351493116e-7,
    )

    /** Γ(x). Reflection handles x < 0.5, where the series would lose accuracy. */
    fun gamma(x: Double): Double {
        if (x.isNaN()) return Double.NaN
        if (x <= 0.0 && x == Math.floor(x)) return Double.NaN // poles at 0, -1, -2, ...
        if (x < 0.5) return PI / (sin(PI * x) * gamma(1.0 - x))
        val z = x - 1.0
        var series = LANCZOS[0]
        val t = z + 7.5
        for (i in 1 until LANCZOS.size) series += LANCZOS[i] / (z + i)
        return sqrt(2.0 * PI) * t.pow(z + 0.5) * exp(-t) * series
    }

    /** ln|Γ(x)| — the form that does not overflow for large x. */
    fun logGamma(x: Double): Double {
        val g = gamma(x)
        return if (g.isFinite() && g != 0.0) ln(abs(g)) else Double.NaN
    }

    /** B(x, y) = Γ(x)Γ(y) / Γ(x + y). */
    fun beta(x: Double, y: Double): Double {
        val g = gamma(x) * gamma(y) / gamma(x + y)
        // go through logs when the pieces overflow
        return if (g.isFinite()) g
        else exp(logGamma(x) + logGamma(y) - logGamma(x + y))
    }

    /** ψ(x), the digamma function: the derivative of ln Γ. */
    fun digamma(x: Double): Double {
        if (x <= 0.0 && x == Math.floor(x)) return Double.NaN
        var value = x
        var shift = 0.0
        // recurrence up to 8, where the asymptotic series is accurate
        while (value < 8.0) {
            shift -= 1.0 / value
            value += 1.0
        }
        val inv = 1.0 / value
        val inv2 = inv * inv
        val series = inv2 * (
            1.0 / 12.0 - inv2 * (
                1.0 / 120.0 - inv2 * (
                    1.0 / 252.0 - inv2 * (
                        1.0 / 240.0 - inv2 / 132.0
                        )
                    )
                )
            )
        return shift + ln(value) - 0.5 * inv - series
    }

    // ---- error function ----

    /** erf(x), from the Maclaurin series. Clamped past |x| = 4, where it is 1 to 1e-8. */
    fun erf(x: Double): Double {
        if (x == 0.0) return 0.0
        if (abs(x) > 4.0) return if (x > 0) 1.0 else -1.0
        var sum = 0.0
        var term = x
        var n = 0
        while (n < 90) {
            sum += term
            n++
            term *= -(x * x) * (2 * n - 1) / (n * (2 * n + 1))
            if (abs(term) < 1e-18 * abs(sum)) break
        }
        return 2.0 / sqrt(PI) * sum
    }

    /** 1 − erf(x), kept separate so it stays accurate for large x. */
    fun erfc(x: Double): Double = 1.0 - erf(x)

    // ---- zeta ----

    /**
     * ζ(s) for s > 1, by Euler–Maclaurin. The tail beyond N is replaced by the
     * expansion, which is what makes this converge quickly.
     */
    fun zeta(s: Double, terms: Int = 24): Double {
        if (s <= 1.0) return if (s == 1.0) Double.POSITIVE_INFINITY else Double.NaN
        var total = 0.0
        for (n in 1 until terms) total += n.pow(-s)
        val n = terms.toDouble()
        total += n.pow(1.0 - s) / (s - 1.0)
        total += 0.5 * n.pow(-s)
        total += (s / 12.0) * n.pow(-s - 1.0)
        total -= (s * (s + 1.0) * (s + 2.0) / 720.0) * n.pow(-s - 3.0)
        return total
    }

    // ---- Lambert W ----

    /**
     * The principal branch of W, where w·eʷ = x. Solved by Newton from a
     * starting guess; the whole real branch is covered, down to −1/e.
     */
    fun lambertW(x: Double): Double {
        if (x.isNaN()) return Double.NaN
        val edge = -1.0 / Math.E
        if (x < edge) return Double.NaN
        if (x == 0.0) return 0.0
        var w = when {
            x < -0.2 -> ln(-x) - ln(-ln(-x)) // near the branch point
            x < 1.0 -> x
            x < 3.0 -> x / Math.E
            else -> ln(x) - ln(ln(x))
        }
        repeat(80) {
            val ew = exp(w)
            val f = w * ew - x
            val slope = ew * (w + 1.0)
            if (slope == 0.0) return w
            val next = w - f / slope
            if (abs(next - w) < 1e-16) return next
            w = next
        }
        return w
    }
}

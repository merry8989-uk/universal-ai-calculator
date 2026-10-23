package com.merry8989uk.universalcalculator.calc

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Probability distributions, t-tests and confidence intervals.
 * Everything is pure maths, so it is fully unit-tested.
 */
object Distributions {

    // ---------- helpers ----------

    /** Lanczos approximation of ln(Γ(x)). */
    fun logGamma(x: Double): Double {
        val g = 7.0
        val c = doubleArrayOf(
            0.99999999999980993, 676.5203681218851, -1259.1392167224028,
            771.32342877765313, -176.61502916214059, 12.507343278686905,
            -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7,
        )
        if (x < 0.5) return ln(PI / sin(PI * x)) - logGamma(1.0 - x)
        val z = x - 1.0
        var a = c[0]
        val t = z + g + 0.5
        for (i in 1..8) a += c[i] / (z + i)
        return 0.5 * ln(2.0 * PI) + (z + 0.5) * ln(t) - t + ln(a)
    }

    /** Error function (Abramowitz & Stegun 7.1.26). */
    fun erf(x: Double): Double {
        val sign = if (x < 0) -1.0 else 1.0
        val ax = abs(x)
        val t = 1.0 / (1.0 + 0.3275911 * ax)
        val y = 1.0 - ((((1.061405429 * t - 1.453152027) * t + 1.421413741) * t -
            0.284496736) * t + 0.254829592) * t * exp(-ax * ax)
        return sign * y
    }

    // ---------- normal ----------

    fun normalPdf(x: Double, mean: Double = 0.0, sd: Double = 1.0): Double {
        if (sd <= 0) throw Evaluator.EvalException("Standard deviation must be positive")
        val z = (x - mean) / sd
        return exp(-0.5 * z * z) / (sd * sqrt(2.0 * PI))
    }

    fun normalCdf(x: Double, mean: Double = 0.0, sd: Double = 1.0): Double {
        if (sd <= 0) throw Evaluator.EvalException("Standard deviation must be positive")
        return 0.5 * (1.0 + erf((x - mean) / (sd * sqrt(2.0))))
    }

    /** Inverse normal CDF, by bisection (robust and easy to trust). */
    fun normalInv(p: Double, mean: Double = 0.0, sd: Double = 1.0): Double {
        if (p <= 0.0 || p >= 1.0) throw Evaluator.EvalException("Probability must be between 0 and 1")
        var lo = mean - 40.0 * sd
        var hi = mean + 40.0 * sd
        repeat(200) {
            val mid = (lo + hi) / 2.0
            if (normalCdf(mid, mean, sd) < p) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }

    // ---------- t ----------

    fun tPdf(x: Double, df: Double): Double {
        if (df <= 0) throw Evaluator.EvalException("Degrees of freedom must be positive")
        val c = exp(logGamma((df + 1.0) / 2.0) - logGamma(df / 2.0)) / sqrt(df * PI)
        return c * (1.0 + x * x / df).pow(-(df + 1.0) / 2.0)
    }

    /** Student-t CDF, by Simpson integration of the density. */
    fun tCdf(t: Double, df: Double): Double {
        if (df <= 0) throw Evaluator.EvalException("Degrees of freedom must be positive")
        val lo = -80.0
        if (t <= lo) return 0.0
        val n = 20000
        val h = (t - lo) / n
        var sum = tPdf(lo, df) + tPdf(t, df)
        for (i in 1 until n) {
            sum += (if (i % 2 == 0) 2.0 else 4.0) * tPdf(lo + i * h, df)
        }
        return (sum * h / 3.0).coerceIn(0.0, 1.0)
    }

    /** Inverse t CDF (critical value), by bisection. */
    fun tInv(p: Double, df: Double): Double {
        if (p <= 0.0 || p >= 1.0) throw Evaluator.EvalException("Probability must be between 0 and 1")
        var lo = -200.0
        var hi = 200.0
        repeat(200) {
            val mid = (lo + hi) / 2.0
            if (tCdf(mid, df) < p) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }

    // ---------- discrete ----------

    fun binomialPmf(k: Int, n: Int, p: Double): Double {
        if (n < 0 || k < 0 || k > n) return 0.0
        if (p < 0.0 || p > 1.0) throw Evaluator.EvalException("p must be between 0 and 1")
        if (p == 0.0) return if (k == 0) 1.0 else 0.0
        if (p == 1.0) return if (k == n) 1.0 else 0.0
        val logC = logGamma(n + 1.0) - logGamma(k + 1.0) - logGamma(n - k + 1.0)
        return exp(logC + k * ln(p) + (n - k) * ln(1.0 - p))
    }

    fun binomialCdf(k: Int, n: Int, p: Double): Double {
        var s = 0.0
        for (i in 0..minOf(k, n)) s += binomialPmf(i, n, p)
        return s.coerceIn(0.0, 1.0)
    }

    fun poissonPmf(k: Int, lambda: Double): Double {
        if (k < 0) return 0.0
        if (lambda < 0) throw Evaluator.EvalException("Mean must not be negative")
        if (lambda == 0.0) return if (k == 0) 1.0 else 0.0
        return exp(-lambda + k * ln(lambda) - logGamma(k + 1.0))
    }

    fun poissonCdf(k: Int, lambda: Double): Double {
        var s = 0.0
        for (i in 0..k) s += poissonPmf(i, lambda)
        return s.coerceIn(0.0, 1.0)
    }

    // ---------- inference ----------

    /** One-sample t-test against a hypothesised mean. */
    fun tTestOneSample(xs: List<Double>, mu0: Double = 0.0): Map<String, Double> {
        if (xs.size < 2) throw Evaluator.EvalException("Need at least two values")
        val n = xs.size
        val mean = Statistics.mean(xs)
        val sd = Statistics.sd(xs, sample = true)
        val se = sd / sqrt(n.toDouble())
        if (se == 0.0) throw Evaluator.EvalException("All values are identical")
        val t = (mean - mu0) / se
        val df = (n - 1).toDouble()
        val p = 2.0 * (1.0 - tCdf(abs(t), df))
        return linkedMapOf(
            "t statistic" to t,
            "Degrees of freedom" to df,
            "p value (two-tailed)" to p.coerceIn(0.0, 1.0),
            "Sample mean" to mean,
            "Standard error" to se,
        )
    }

    /** Welch's two-sample t-test. */
    fun tTestTwoSample(a: List<Double>, b: List<Double>): Map<String, Double> {
        if (a.size < 2 || b.size < 2) throw Evaluator.EvalException("Need at least two values in each sample")
        val na = a.size.toDouble()
        val nb = b.size.toDouble()
        val va = Statistics.variance(a, sample = true)
        val vb = Statistics.variance(b, sample = true)
        val se = sqrt(va / na + vb / nb)
        if (se == 0.0) throw Evaluator.EvalException("Both samples have zero spread")
        val t = (Statistics.mean(a) - Statistics.mean(b)) / se
        val df = (va / na + vb / nb).pow(2.0) /
            ((va / na).pow(2.0) / (na - 1.0) + (vb / nb).pow(2.0) / (nb - 1.0))
        val p = 2.0 * (1.0 - tCdf(abs(t), df))
        return linkedMapOf(
            "t statistic" to t,
            "Degrees of freedom" to df,
            "p value (two-tailed)" to p.coerceIn(0.0, 1.0),
            "Mean of sample 1" to Statistics.mean(a),
            "Mean of sample 2" to Statistics.mean(b),
        )
    }

    /** Confidence interval for a mean (t-based). */
    fun confidenceIntervalMean(xs: List<Double>, level: Double = 0.95): Map<String, Double> {
        if (xs.size < 2) throw Evaluator.EvalException("Need at least two values")
        if (level <= 0.0 || level >= 1.0) throw Evaluator.EvalException("Confidence level must be between 0 and 1")
        val n = xs.size
        val mean = Statistics.mean(xs)
        val se = Statistics.sd(xs, sample = true) / sqrt(n.toDouble())
        val crit = tInv((1.0 + level) / 2.0, (n - 1).toDouble())
        val margin = crit * se
        return linkedMapOf(
            "Confidence level" to level * 100.0,
            "Sample mean" to mean,
            "Margin of error" to margin,
            "Lower bound" to mean - margin,
            "Upper bound" to mean + margin,
        )
    }

    /** Two-tailed p-value for a z score. */
    fun pValueFromZ(z: Double): Double = (2.0 * (1.0 - normalCdf(abs(z)))).coerceIn(0.0, 1.0)
}

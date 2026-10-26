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

    /**
     * Student-t CDF, in closed form.
     *
     * P(T <= t) = 1 - ½·I_x(df/2, ½) for t > 0, with x = df/(df + t²), and the
     * mirror image below zero. The old version integrated the density from a
     * hard-coded -80, which threw away the left tail entirely: for df = 1 that
     * tail is about 0.004, so every answer was low by a constant and tInv then
     * inverted the error into a critical value 19% too large.
     */
    fun tCdf(t: Double, df: Double): Double {
        if (df <= 0) throw Evaluator.EvalException("Degrees of freedom must be positive")
        if (t == 0.0) return 0.5
        val x = df / (df + t * t)
        val half = 0.5 * betai(df / 2.0, 0.5, x)
        return (if (t > 0) 1.0 - half else half).coerceIn(0.0, 1.0)
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
    // ---------- incomplete gamma and beta ----------

    /** Series expansion for the regularised lower incomplete gamma. */
    private fun gser(a: Double, x: Double): Double {
        val gln = logGamma(a)
        if (x <= 0) return 0.0
        var ap = a
        var sum = 1.0 / a
        var del = sum
        for (n in 1..500) {
            ap += 1.0
            del *= x / ap
            sum += del
            if (abs(del) < abs(sum) * 1e-12) break
        }
        return sum * exp(-x + a * ln(x) - gln)
    }

    /** Continued fraction for the upper incomplete gamma. */
    private fun gcf(a: Double, x: Double): Double {
        val fpmin = 1e-300
        val gln = logGamma(a)
        var b = x + 1.0 - a
        var c = 1.0 / fpmin
        var d = 1.0 / b
        var h = d
        for (i in 1..500) {
            val an = -i * (i - a)
            b += 2.0
            d = an * d + b
            if (abs(d) < fpmin) d = fpmin
            c = b + an / c
            if (abs(c) < fpmin) c = fpmin
            d = 1.0 / d
            val del = d * c
            h *= del
            if (abs(del - 1.0) < 1e-12) break
        }
        return exp(-x + a * ln(x) - gln) * h
    }

    /** Regularised lower incomplete gamma P(a, x). */
    fun gammp(a: Double, x: Double): Double {
        if (a <= 0.0 || x < 0.0) throw Evaluator.EvalException("Bad arguments for the incomplete gamma")
        return if (x < a + 1.0) gser(a, x) else 1.0 - gcf(a, x)
    }

    /**
     * The upper tail of the incomplete gamma, Q(a,x).
     *
     * Computed from the continued fraction directly when x is large. Doing it as
     * `1 - gammp` would subtract two numbers that agree to fifteen digits, and
     * the answer would come out as exactly zero — which reads as "impossible"
     * rather than "very unlikely".
     */
    fun gammq(a: Double, x: Double): Double {
        if (a <= 0.0 || x < 0.0) throw Evaluator.EvalException("Bad arguments for the incomplete gamma")
        return if (x < a + 1.0) 1.0 - gser(a, x) else gcf(a, x)
    }

    private fun betacf(a: Double, b: Double, x: Double): Double {
        val fpmin = 1e-300
        val qab = a + b
        val qap = a + 1.0
        val qam = a - 1.0
        var c = 1.0
        var d = 1.0 - qab * x / qap
        if (abs(d) < fpmin) d = fpmin
        d = 1.0 / d
        var h = d
        for (m in 1..500) {
            val m2 = 2 * m
            var aa = m * (b - m) * x / ((qam + m2) * (a + m2))
            d = 1.0 + aa * d
            if (abs(d) < fpmin) d = fpmin
            c = 1.0 + aa / c
            if (abs(c) < fpmin) c = fpmin
            d = 1.0 / d
            h *= d * c
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
            d = 1.0 + aa * d
            if (abs(d) < fpmin) d = fpmin
            c = 1.0 + aa / c
            if (abs(c) < fpmin) c = fpmin
            d = 1.0 / d
            val del = d * c
            h *= del
            if (abs(del - 1.0) < 1e-12) break
        }
        return h
    }

    /** Regularised incomplete beta I_x(a, b). */
    fun betai(a: Double, b: Double, x: Double): Double {
        if (x <= 0.0) return 0.0
        if (x >= 1.0) return 1.0
        val bt = exp(logGamma(a + b) - logGamma(a) - logGamma(b) + a * ln(x) + b * ln(1.0 - x))
        return if (x < (a + 1.0) / (a + b + 2.0)) bt * betacf(a, b, x) / a
        else 1.0 - bt * betacf(b, a, 1.0 - x) / b
    }

    // ---------- chi-square ----------

    fun chiSquarePdf(x: Double, df: Double): Double {
        if (df <= 0) throw Evaluator.EvalException("Degrees of freedom must be positive")
        if (x <= 0) return 0.0
        val k = df / 2.0
        return exp((k - 1.0) * ln(x) - x / 2.0 - k * ln(2.0) - logGamma(k))
    }

    fun chiSquareCdf(x: Double, df: Double): Double {
        if (df <= 0) throw Evaluator.EvalException("Degrees of freedom must be positive")
        if (x <= 0) return 0.0
        return gammp(df / 2.0, x / 2.0).coerceIn(0.0, 1.0)
    }

    /**
     * P(X² > x) — the p-value for a chi-square statistic.
     *
     * Straight from the upper incomplete gamma, not from `1 - CDF`: for a large
     * statistic the subtraction cancels to exactly 0.0.
     */
    fun chiSquareUpperTail(x: Double, df: Double): Double {
        if (df <= 0) throw Evaluator.EvalException("Degrees of freedom must be positive")
        if (x <= 0) return 1.0
        return gammq(df / 2.0, x / 2.0).coerceIn(0.0, 1.0)
    }

    // ---------- F ----------

    fun fCdf(f: Double, d1: Double, d2: Double): Double {
        if (d1 <= 0 || d2 <= 0) throw Evaluator.EvalException("Degrees of freedom must be positive")
        if (f <= 0) return 0.0
        return betai(d1 / 2.0, d2 / 2.0, d1 * f / (d1 * f + d2)).coerceIn(0.0, 1.0)
    }

    /**
     * P(F > f) — the p-value for an F statistic.
     *
     * The incomplete beta with the arguments swapped gives the tail directly, so
     * nothing is subtracted and a tiny p-value stays a tiny p-value.
     */
    fun fUpperTail(f: Double, d1: Double, d2: Double): Double {
        if (d1 <= 0 || d2 <= 0) throw Evaluator.EvalException("Degrees of freedom must be positive")
        if (f <= 0) return 1.0
        return betai(d2 / 2.0, d1 / 2.0, d2 / (d2 + d1 * f)).coerceIn(0.0, 1.0)
    }

    // ---------- tests ----------

    /** One-way analysis of variance. */
    fun oneWayAnova(groups: List<List<Double>>): Map<String, Double> {
        if (groups.size < 2) throw Evaluator.EvalException("Need at least two groups")
        groups.forEach { if (it.size < 2) throw Evaluator.EvalException("Each group needs at least two values") }
        val all = groups.flatten()
        val grand = Statistics.mean(all)
        val n = all.size
        var ssBetween = 0.0
        var ssWithin = 0.0
        for (g in groups) {
            val m = Statistics.mean(g)
            ssBetween += g.size * (m - grand) * (m - grand)
            for (x in g) ssWithin += (x - m) * (x - m)
        }
        val dfB = (groups.size - 1).toDouble()
        val dfW = (n - groups.size).toDouble()
        val msB = ssBetween / dfB
        val msW = if (dfW > 0) ssWithin / dfW else 0.0
        val f = if (msW == 0.0) 0.0 else msB / msW
        val p = if (f > 0.0 && dfW > 0) fUpperTail(f, dfB, dfW) else 1.0
        return linkedMapOf(
            "F statistic" to f,
            "df between" to dfB,
            "df within" to dfW,
            "p value" to p,
            "SS between" to ssBetween,
            "SS within" to ssWithin,
            "MS between" to msB,
            "MS within" to msW,
        )
    }

    /** Chi-square goodness of fit. */
    fun chiSquareGoodnessOfFit(observed: List<Double>, expected: List<Double>): Map<String, Double> {
        if (observed.size != expected.size) throw Evaluator.EvalException("Both lists must be the same length")
        if (observed.size < 2) throw Evaluator.EvalException("Need at least two categories")
        var chi = 0.0
        for (i in observed.indices) {
            val e = expected[i]
            if (e <= 0) throw Evaluator.EvalException("Expected counts must be positive")
            chi += (observed[i] - e) * (observed[i] - e) / e
        }
        val df = (observed.size - 1).toDouble()
        return linkedMapOf(
            "Chi-square" to chi,
            "Degrees of freedom" to df,
            "p value" to chiSquareUpperTail(chi, df),
        )
    }

    /** Chi-square test of independence on a contingency table. */
    fun chiSquareIndependence(table: List<List<Double>>): Map<String, Double> {
        if (table.size < 2) throw Evaluator.EvalException("Need at least two rows")
        val cols = table[0].size
        if (cols < 2) throw Evaluator.EvalException("Need at least two columns")
        if (table.any { it.size != cols }) throw Evaluator.EvalException("Every row must be the same length")
        val rowSums = table.map { it.sum() }
        val colSums = (0 until cols).map { j -> table.sumOf { it[j] } }
        val total = rowSums.sum()
        if (total <= 0) throw Evaluator.EvalException("The table must not be empty")
        var chi = 0.0
        for (i in table.indices) for (j in 0 until cols) {
            val e = rowSums[i] * colSums[j] / total
            if (e <= 0) continue
            chi += (table[i][j] - e) * (table[i][j] - e) / e
        }
        val df = ((table.size - 1) * (cols - 1)).toDouble()
        return linkedMapOf(
            "Chi-square" to chi,
            "Degrees of freedom" to df,
            "p value" to chiSquareUpperTail(chi, df),
        )
    }
}

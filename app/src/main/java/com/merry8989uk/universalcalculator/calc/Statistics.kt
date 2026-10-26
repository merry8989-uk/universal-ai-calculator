package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Descriptive statistics over a data set, plus simple linear regression.
 * Everything returns an ordered map so the UI can render it directly.
 */
object Statistics {

    /** Parses a free-text list: numbers separated by comma, space, semicolon or newline. */
    fun parse(text: String): List<Double> =
        text.split(',', ' ', ';', '\n', '\t')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toDoubleOrNull() }

    fun mean(xs: List<Double>): Double = xs.sum() / xs.size

    fun geometricMean(xs: List<Double>): Double {
        if (xs.isEmpty() || xs.any { it <= 0.0 }) return Double.NaN
        return kotlin.math.exp(xs.sumOf { kotlin.math.ln(it) } / xs.size)
    }

    fun harmonicMean(xs: List<Double>): Double {
        if (xs.isEmpty() || xs.any { it == 0.0 }) return Double.NaN
        return xs.size / xs.sumOf { 1.0 / it }
    }

    fun median(xs: List<Double>): Double {
        val s = xs.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2.0
    }

    fun mode(xs: List<Double>): List<Double> {
        val counts = xs.groupingBy { it }.eachCount()
        val max = counts.values.maxOrNull() ?: return emptyList()
        if (max <= 1) return emptyList()
        return counts.filterValues { it == max }.keys.sorted()
    }

    fun variance(xs: List<Double>, sample: Boolean = true): Double {
        if (xs.size < 2) return 0.0
        val m = mean(xs)
        val ss = xs.sumOf { (it - m).pow(2) }
        return ss / (if (sample) xs.size - 1 else xs.size).toDouble()
    }

    fun sd(xs: List<Double>, sample: Boolean = true): Double = sqrt(variance(xs, sample))

    /** Linear-interpolated quantile, q in 0..1. */
    fun quantile(xs: List<Double>, q: Double): Double {
        if (xs.isEmpty()) return Double.NaN
        val s = xs.sorted()
        if (s.size == 1) return s[0]
        val pos = q * (s.size - 1)
        val lo = pos.toInt()
        val hi = minOf(lo + 1, s.size - 1)
        val frac = pos - lo
        return s[lo] + frac * (s[hi] - s[lo])
    }

    fun skewness(xs: List<Double>): Double {
        val n = xs.size
        if (n < 3) return Double.NaN
        val m = mean(xs)
        // The n/((n-1)(n-2)) correction is the one that belongs with the SAMPLE
        // standard deviation. Pairing it with the population SD inflated the
        // answer by (n/(n-1))^1.5 — a flat set came out strongly skewed.
        val s = sd(xs, sample = true)
        if (s == 0.0) return 0.0
        return xs.sumOf { ((it - m) / s).pow(3) } * n / ((n - 1.0) * (n - 2.0))
    }

    fun kurtosis(xs: List<Double>): Double {
        val n = xs.size
        if (n < 4) return Double.NaN
        val m = mean(xs)
        // same pairing as skewness: the correction goes with the sample SD
        val s = sd(xs, sample = true)
        if (s == 0.0) return 0.0
        return xs.sumOf { ((it - m) / s).pow(4) } * n * (n + 1.0) /
            ((n - 1.0) * (n - 2.0) * (n - 3.0)) -
            3.0 * (n - 1.0).pow(2) / ((n - 2.0) * (n - 3.0))
    }

    /** z-score of a value against the set. */
    fun zScore(xs: List<Double>, value: Double): Double {
        val s = sd(xs)
        return if (s == 0.0) 0.0 else (value - mean(xs)) / s
    }

    /** Linear regression y = a + b x. Returns (slope, intercept, r2, correlation). */
    fun linearRegression(xs: List<Double>, ys: List<Double>): List<Double> {
        val n = minOf(xs.size, ys.size)
        if (n < 2) return listOf(Double.NaN, Double.NaN, Double.NaN, Double.NaN)
        val x = xs.take(n); val y = ys.take(n)
        val mx = mean(x); val my = mean(y)
        val sxx = x.sumOf { (it - mx).pow(2) }
        val sxy = (0 until n).sumOf { (x[it] - mx) * (y[it] - my) }
        val syy = y.sumOf { (it - my).pow(2) }
        if (sxx == 0.0) return listOf(Double.NaN, Double.NaN, Double.NaN, Double.NaN)
        val slope = sxy / sxx
        val intercept = my - slope * mx
        val r = if (sxx == 0.0 || syy == 0.0) 0.0 else sxy / sqrt(sxx * syy)
        return listOf(slope, intercept, r * r, r)
    }

    /** The full descriptive summary, in display order. */
    fun summary(xs: List<Double>): LinkedHashMap<String, Double> {
        val out = LinkedHashMap<String, Double>()
        if (xs.isEmpty()) return out
        out["Count"] = xs.size.toDouble()
        out["Sum"] = xs.sum()
        out["Mean"] = mean(xs)
        out["Median"] = median(xs)
        out["Mode"] = mode(xs).firstOrNull() ?: Double.NaN
        out["Minimum"] = xs.minOrNull() ?: 0.0
        out["Maximum"] = xs.maxOrNull() ?: 0.0
        out["Range"] = (xs.maxOrNull() ?: 0.0) - (xs.minOrNull() ?: 0.0)
        out["Variance (sample)"] = variance(xs, true)
        out["Variance (population)"] = variance(xs, false)
        out["Std dev (sample)"] = sd(xs, true)
        out["Std dev (population)"] = sd(xs, false)
        out["Q1"] = quantile(xs, 0.25)
        out["Q3"] = quantile(xs, 0.75)
        out["IQR"] = quantile(xs, 0.75) - quantile(xs, 0.25)
        out["Skewness"] = skewness(xs)
        out["Kurtosis"] = kurtosis(xs)
        return out
    }
}

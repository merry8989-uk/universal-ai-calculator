package com.merry8989uk.universalcalculator.calc

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

/** Number formatting used across every screen. */
object Format {

    /** A clean, human result string: trims noise, uses scientific for extremes. */
    fun number(v: Double): String {
        if (v.isNaN()) return "NaN"
        if (v.isInfinite()) return if (v > 0) "Infinity" else "-Infinity"
        if (v == 0.0) return "0"
        val a = abs(v)
        if (a >= 1e15 || a < 1e-9) {
            return String.format("%.6e", v).replace("e+", "e").replace("E", "e")
        }
        val rounded = BigDecimal(v.toString()).round(MathContext(12)).stripTrailingZeros()
        return rounded.toPlainString()
    }

    /**
     * Formats a result for display: rounds to [decimals] places (or -1 for
     * automatic) and groups the digits the way [format] asks.
     *
     *   "Indian"        30,00,000.5
     *   "International"  3,000,000.5
     *   "European"       3.000.000,5
     *   "None"           3000000.5
     */
    fun grouped(text: String, format: String = "Indian", decimals: Int = -1): String {
        var t = text
        if (decimals >= 0 && !t.contains('e') && !t.contains('E')) {
            t.toDoubleOrNull()?.let { t = String.format(java.util.Locale.US, "%.${decimals}f", it) }
        }
        if (format == "None") return t

        val neg = t.startsWith("-")
        val body = if (neg) t.substring(1) else t
        val dot = body.indexOf('.')
        val intPart = if (dot >= 0) body.substring(0, dot) else body
        val frac = if (dot >= 0) body.substring(dot + 1) else ""
        if (intPart.isEmpty() || intPart.any { !it.isDigit() }) return t

        val groupSep: Char
        val decSep: Char
        val indian: Boolean
        when (format) {
            "European" -> { groupSep = '.'; decSep = ','; indian = false }
            "Indian" -> { groupSep = ','; decSep = '.'; indian = true }
            else -> { groupSep = ','; decSep = '.'; indian = false }
        }

        val grouped = if (indian) {
            if (intPart.length <= 3) intPart
            else intPart.dropLast(3).reversed().chunked(2).joinToString(groupSep.toString()).reversed() +
                groupSep + intPart.takeLast(3)
        } else {
            intPart.reversed().chunked(3).joinToString(groupSep.toString()).reversed()
        }

        return (if (neg) "-" else "") + grouped + (if (frac.isNotEmpty()) "$decSep$frac" else "")
    }

    /** Fixed-decimal formatting for inputs/table cells. */
    fun decimal(v: Double, places: Int = 4): String =
        BigDecimal(v.toString()).setScale(places, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

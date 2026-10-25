package com.merry8989uk.universalcalculator.calc

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

/** Number formatting used across every screen. */
object Format {

    /**
     * A clean, human result string, written the way [mode] asks.
     *
     * [digits] is how many digits to keep after the point in Fixed, and how many
     * to show in the mantissa for Scientific and Engineering.
     */
    fun number(
        v: Double,
        mode: DecimalFormat = DecimalFormat.NORMAL,
        digits: Int = 10,
    ): String {
        if (v.isNaN()) return "NaN"
        if (v.isInfinite()) return if (v > 0) "Infinity" else "-Infinity"
        if (v == 0.0) return if (mode == DecimalFormat.FIXED) {
            String.format(java.util.Locale.US, "%.${digits.coerceIn(0, 16)}f", 0.0)
        } else {
            "0"
        }
        val d = digits.coerceIn(0, 16)
        return when (mode) {
            DecimalFormat.NORMAL -> plain(v)
            DecimalFormat.FIXED -> String.format(java.util.Locale.US, "%.${d}f", v)
            DecimalFormat.SCIENTIFIC -> mantissa(v, d, multipleOfThree = false)
            DecimalFormat.ENGINEERING -> mantissa(v, d, multipleOfThree = true)
            DecimalFormat.ENGINEERING_SI -> engineeringSi(v, d)
        }
    }

    /** The shortest exact form, trimming noise. */
    private fun plain(v: Double): String {
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
     * The mantissa and the power of ten, written as `1.2341×10³`.
     *
     * With [multipleOfThree] the exponent is pushed down to a multiple of three,
     * which is what engineering notation means: the point sits after thousands,
     * millions and so on.
     */
    private fun mantissa(v: Double, digits: Int, multipleOfThree: Boolean): String {
        val exponent = if (v == 0.0) 0 else Math.floor(Math.log10(abs(v))).toInt()
        val e = if (multipleOfThree) Math.floorDiv(exponent, 3) * 3 else exponent
        val m = v / Math.pow(10.0, e.toDouble())
        // rounding the mantissa can carry it to ten, e.g. 9.99 -> 10.0
        var mantissa = String.format(java.util.Locale.US, "%.${digits}f", m)
        var power = e
        if (mantissa.startsWith("10") && digits > 0) {
            mantissa = String.format(java.util.Locale.US, "%.${digits}f", 1.0)
            power = if (multipleOfThree) e + 3 else e + 1
        }
        val trimmed = if (digits > 0) mantissa.trimEnd('0').trimEnd('.') else mantissa
        return trimmed + "\u00D710" + superscript(power)
    }

    /** Engineering notation with the power of ten swapped for its SI prefix. */
    private fun engineeringSi(v: Double, digits: Int): String {
        val exponent = if (v == 0.0) 0 else Math.floor(Math.log10(abs(v))).toInt()
        val e = Math.floorDiv(exponent, 3) * 3
        val usable = e.coerceIn(-24, 24)
        val prefix = SI_PREFIXES.firstOrNull { it.first == usable }?.second
            ?: return mantissa(v, digits, multipleOfThree = true)
        val m = v / Math.pow(10.0, usable.toDouble())
        var text = String.format(java.util.Locale.US, "%.${digits}f", m)
        if (text.startsWith("1000") && digits > 0) {
            text = String.format(java.util.Locale.US, "%.${digits}f", 1.0)
            val up = usable + 3
            val upPrefix = SI_PREFIXES.firstOrNull { it.first == up }?.second ?: prefix
            val upM = v / Math.pow(10.0, up.toDouble())
            val upText = String.format(java.util.Locale.US, "%.${digits}f", upM)
            return trim(upText, digits) + upPrefix
        }
        return trim(text, digits) + prefix
    }

    private fun trim(text: String, digits: Int): String =
        if (digits > 0) text.trimEnd('0').trimEnd('.') else text

    /** An exponent as superscript digits, so it reads as a power rather than code. */
    private fun superscript(power: Int): String {
        val digits = "\u2070\u00B9\u00B2\u00B3\u2074\u2075\u2076\u2077\u2078\u2079"
        val sign = if (power < 0) "\u207B" else ""
        return sign + abs(power).toString().map { digits[it - '0'] }.joinToString("")
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
        // Scientific and SI forms are not plain digits; grouping them would break them.
        if (t.contains('\u00D7') || t.contains('e') || t.contains('E')) return t

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

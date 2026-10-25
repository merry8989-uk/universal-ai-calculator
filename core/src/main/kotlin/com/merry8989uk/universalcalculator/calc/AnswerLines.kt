package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10

/**
 * The one or two lines a result is shown as.
 *
 * A big answer is hard to read as a wall of digits, so the reference shows it
 * twice: the whole number, grouped, and beneath it the same number in scientific
 * form. `28 14 74 97 67 10 656` on top and `= 2.814 749 767 1×10¹⁴` below says
 * both "exactly this" and "about this much", which is what someone checking a
 * power actually wants.
 */
data class AnswerLines(
    /** The result as written, grouped the way the settings ask. */
    val full: String,
    /** The scientific form, with a leading "= ", or empty when not needed. */
    val scientific: String,
) {
    val isTwoLines: Boolean get() = scientific.isNotEmpty()
}

object AnswerStyle {

    /** Above this size a result gets the second line as well. */
    const val BIG = 1e6

    private val SUPERSCRIPT = mapOf(
        '-' to '\u207B', '0' to '\u2070', '1' to '\u00B9', '2' to '\u00B2', '3' to '\u00B3',
        '4' to '\u2074', '5' to '\u2075', '6' to '\u2076', '7' to '\u2077',
        '8' to '\u2078', '9' to '\u2079',
    )

    /** The power of ten written as a superscript. */
    fun superscript(exponent: Int): String =
        exponent.toString().map { SUPERSCRIPT[it] ?: it }.joinToString("")

    /**
     * Groups the digits of [text] in threes with spaces, after the point as
     * well as before it.
     *
     * `2.81474976710656` becomes `2.814 749 767 1`, which is how the reference
     * writes a mantissa — long digits are unreadable without the breaks.
     */
    fun groupThrees(text: String): String {
        // an exponent is not a run of digits to be broken up
        if (text.contains('e') || text.contains('E')) return text
        val negative = text.startsWith("-")
        val body = text.removePrefix("-")
        val dot = body.indexOf('.')
        val whole = if (dot < 0) body else body.substring(0, dot)
        val fraction = if (dot < 0) "" else body.substring(dot + 1)
        val groupedWhole = whole.reversed().chunked(3).joinToString(" ").reversed()
        val groupedFraction = if (fraction.isEmpty()) "" else
            "." + fraction.chunked(3).joinToString(" ")
        return (if (negative) "-" else "") + groupedWhole + groupedFraction
    }

    /**
     * The two lines for [value].
     *
     * [full] is handed in already grouped the way the user chose, because the
     * grouping of the whole number is a setting. The scientific line is grouped
     * in threes regardless, because that is a reading aid rather than a
     * preference.
     */
    fun lines(value: Double, full: String): AnswerLines {
        if (!value.isFinite() || value == 0.0) return AnswerLines(full, "")
        val size = abs(value)
        if (size < BIG && size >= 1e-4) return AnswerLines(full, "")
        val exponent = floor(log10(size)).toInt()
        val mantissa = value / Math.pow(10.0, exponent.toDouble())
        // a mantissa that rounded up to ten belongs one power higher
        val (m, e) = if (abs(mantissa) >= 10.0) mantissa / 10.0 to exponent + 1 else mantissa to exponent
        val written = groupThrees(Format.number(Math.round(m * 1e12) / 1e12))
        return AnswerLines(full, "= $written\u00D710" + superscript(e))
    }
}

package com.merry8989uk.universalcalculator.calc

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Small text/number/colour format converters (A18 of the plan). */
object Converters {

    private val ROMAN = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
    )

    fun toRoman(value: Int): String {
        if (value <= 0 || value > 3999) throw Evaluator.EvalException("Roman numerals cover 1 to 3999")
        var n = value
        val sb = StringBuilder()
        for ((v, s) in ROMAN) while (n >= v) { sb.append(s); n -= v }
        return sb.toString()
    }

    fun fromRoman(text: String): Int {
        val s = text.trim().uppercase()
        if (s.isEmpty()) throw Evaluator.EvalException("Empty Roman numeral")
        val map = mapOf(
            'I' to 1, 'V' to 5, 'X' to 10, 'L' to 50,
            'C' to 100, 'D' to 500, 'M' to 1000,
        )
        var total = 0
        var prev = 0
        for (ch in s.reversed()) {
            val v = map[ch] ?: throw Evaluator.EvalException("'$ch' is not a Roman numeral")
            if (v < prev) total -= v else { total += v; prev = v }
        }
        if (total <= 0 || total > 3999) {
            throw Evaluator.EvalException("Roman numerals must be between I and MMMCMXCIX")
        }
        // Only the canonical form is accepted, so "IIIIII" or "VV" are rejected.
        if (toRoman(total) != s) {
            throw Evaluator.EvalException("'$s' is not a valid Roman numeral")
        }
        return total
    }

    private val ONES = listOf(
        "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
        "seventeen", "eighteen", "nineteen",
    )
    private val TENS = listOf(
        "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
    )

    private fun below1000(n: Int): String {
        val sb = StringBuilder()
        if (n >= 100) {
            sb.append(ONES[n / 100]).append(" hundred")
            if (n % 100 != 0) sb.append(" ")
        }
        val r = n % 100
        when {
            r == 0 -> {}
            r < 20 -> sb.append(ONES[r])
            else -> {
                sb.append(TENS[r / 10])
                if (r % 10 != 0) sb.append("-").append(ONES[r % 10])
            }
        }
        return sb.toString()
    }

    /** Number to English words (supports Indian and international grouping). */
    fun numberToWords(value: Long, indian: Boolean = true): String {
        if (value == 0L) return "zero"
        val neg = value < 0
        var n = if (neg) -value else value
        val parts = mutableListOf<String>()
        if (indian) {
            val units = listOf(1_000_000_000L to "crore", 100_000L to "lakh", 1_000L to "thousand")
            for ((div, name) in units) {
                if (n >= div) { parts.add("${below1000((n / div).toInt())} $name"); n %= div }
            }
        } else {
            val units = listOf(
                1_000_000_000L to "billion", 1_000_000L to "million", 1_000L to "thousand",
            )
            for ((div, name) in units) {
                if (n >= div) { parts.add("${below1000((n / div).toInt())} $name"); n %= div }
            }
        }
        if (n > 0) parts.add(below1000(n.toInt()))
        return (if (neg) "minus " else "") + parts.joinToString(" ")
    }

    fun textToBinary(text: String): String =
        text.toByteArray(Charsets.UTF_8).joinToString(" ") { b ->
            Integer.toBinaryString(b.toInt() and 0xFF).padStart(8, '0')
        }

    fun binaryToText(text: String): String {
        val bytes = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map {
            Integer.parseInt(it, 2).toByte()
        }.toByteArray()
        return String(bytes, Charsets.UTF_8)
    }

    fun textToHex(text: String): String =
        text.toByteArray(Charsets.UTF_8).joinToString(" ") { b ->
            "%02X".format(b.toInt() and 0xFF)
        }

    fun hexToText(text: String): String {
        val cleaned = text.replace("0x", "", ignoreCase = true).replace(" ", "").replace(",", "")
        if (cleaned.length % 2 != 0) throw Evaluator.EvalException("Hex needs an even number of digits")
        val bytes = ByteArray(cleaned.length / 2) { i ->
            cleaned.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return String(bytes, Charsets.UTF_8)
    }

    fun textToAscii(text: String): String =
        text.toCharArray().joinToString(" ") { it.code.toString() }

    fun asciiToText(text: String): String =
        text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            .map { it.toInt().toChar() }.joinToString("")

    fun rgbToHex(r: Int, g: Int, b: Int): String =
        "#%02X%02X%02X".format(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))

    /** Returns (r, g, b) for a #RRGGBB hex string. */
    fun hexToRgb(hex: String): Triple<Int, Int, Int> {
        val h = hex.trim().removePrefix("#")
        if (h.length != 6) throw Evaluator.EvalException("Use a 6-digit hex like #1565C0")
        val r = h.substring(0, 2).toInt(16)
        val g = h.substring(2, 4).toInt(16)
        val b = h.substring(4, 6).toInt(16)
        return Triple(r, g, b)
    }

    fun timestampToDate(epochSeconds: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    fun dateToTimestamp(date: String): Long {
        val d = LocalDate.parse(date.trim())
        return d.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
    }
}

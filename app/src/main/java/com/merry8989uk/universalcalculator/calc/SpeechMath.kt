package com.merry8989uk.universalcalculator.calc

/**
 * Turns a spoken phrase into an expression the calculator understands:
 * "twenty percent of fifty" becomes "20%*50", "one hundred and five" becomes
 * "100+5".
 *
 * Pure text work, so it is fully unit-tested without a microphone.
 */
object SpeechMath {

    private val SMALL = mapOf(
        "zero" to 0L, "one" to 1L, "two" to 2L, "three" to 3L, "four" to 4L,
        "five" to 5L, "six" to 6L, "seven" to 7L, "eight" to 8L, "nine" to 9L,
        "ten" to 10L, "eleven" to 11L, "twelve" to 12L, "thirteen" to 13L,
        "fourteen" to 14L, "fifteen" to 15L, "sixteen" to 16L, "seventeen" to 17L,
        "eighteen" to 18L, "nineteen" to 19L, "twenty" to 20L, "thirty" to 30L,
        "forty" to 40L, "fifty" to 50L, "sixty" to 60L, "seventy" to 70L,
        "eighty" to 80L, "ninety" to 90L,
    )

    private val SCALE = mapOf(
        "hundred" to 100L, "thousand" to 1000L, "lakh" to 100000L,
        "million" to 1000000L, "crore" to 10000000L,
    )

    /** Longest phrases first, so "square root of" wins over "of". */
    private val PHRASES = listOf(
        "raised to the power of" to "^",
        "to the power of" to "^",
        "square root of" to "sqrt(",
        "square root" to "sqrt(",
        "cube root of" to "cbrt(",
        "cube root" to "cbrt(",
        "root of" to "sqrt(",
        "raised to" to "^",
        "power of" to "^",
        "to the power" to "^",
        "multiplied by" to "*",
        "multiply by" to "*",
        "divided by" to "/",
        "divide by" to "/",
        "percentage of" to "%*",
        "percent of" to "%*",
        "per cent of" to "%*",
        "open bracket" to "(",
        "close bracket" to ")",
        "open parenthesis" to "(",
        "close parenthesis" to ")",
        "how much is" to "",
        "equal to" to "",
        "equals" to "",
        "what is" to "",
        "what's" to "",
        "calculate" to "",
        "added to" to "+",
        "take away" to "-",
        "subtract" to "-",
        "plus" to "+",
        "minus" to "-",
        "times" to "*",
        "into" to "*",
        "over" to "/",
        "per cent" to "%",
        "percentage" to "%",
        "percent" to "%",
        "squared" to "^2",
        "cubed" to "^3",
        "factorial" to "!",
        "point" to ".",
        "dot" to ".",
        "add" to "+",
        "less" to "-",
        "and" to "+",
        "of" to "*",
    )

    /**
     * Normalises a spoken phrase. Number words are combined, unbalanced
     * brackets are closed, and anything that is not part of an expression is
     * dropped.
     */
    fun normalise(text: String): String {
        val words = text.lowercase().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

        // 1. combine runs of number words into digits
        val built = StringBuilder()
        var idx = 0
        while (idx < words.size) {
            val w = words[idx]
            if (w in SMALL || w in SCALE) {
                val run = ArrayList<String>()
                while (idx < words.size && (words[idx] in SMALL || words[idx] in SCALE)) {
                    run.add(words[idx]); idx++
                }
                built.append(' ').append(numberValue(run)).append(' ')
            } else {
                built.append(' ').append(w).append(' ')
                idx++
            }
        }
        // collapse the spaces left by the number pass, so multi-word phrases match
        var s = built.toString().replace(Regex("\\s+"), " ")

        // 2. word operators, longest phrase first
        for ((from, to) in PHRASES.sortedByDescending { it.first.length }) {
            s = s.replace(" $from ", " $to ")
        }

        // 3. keep only what an expression can contain
        val allowed = "0123456789.+-*/^()%!abcdefghijklmnopqrstuvwxyz "
        s = s.filter { it in allowed }

        s = s.replace(Regex("\\s+"), "").trim()
        s = s.replace(Regex("[+*/^]{2,}")) { m -> m.value.last().toString() }
        s = s.trimStart('*', '/', '+', '^')

        val open = s.count { it == '(' } - s.count { it == ')' }
        if (open > 0) s += ")".repeat(open)

        return s
    }

    /** "one hundred twenty five" -> 125, "two thousand" -> 2000. */
    private fun numberValue(run: List<String>): Long {
        var total = 0L
        var current = 0L
        for (w in run) {
            val small = SMALL[w]
            if (small != null) { current += small; continue }
            val scale = SCALE[w] ?: continue
            if (scale == 100L) {
                current = if (current == 0L) 100L else current * 100L
            } else {
                total += (if (current == 0L) 1L else current) * scale
                current = 0L
            }
        }
        return total + current
    }

    /** True when the phrase produced something worth evaluating. */
    fun looksUsable(expr: String): Boolean =
        expr.isNotBlank() && (expr.any { it.isDigit() } || expr.contains("pi") || expr.contains("sqrt"))
}

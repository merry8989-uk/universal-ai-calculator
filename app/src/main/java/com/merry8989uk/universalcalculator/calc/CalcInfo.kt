package com.merry8989uk.universalcalculator.calc

/**
 * The line under a result that explains how it was read.
 *
 * A calculator that quietly decides what `2π` or `100 + 20%` means should say
 * so, because the two settings that control them can be set either way. This
 * works out the note, or returns nothing when there is nothing worth saying.
 */
object CalcInfo {

    /**
     * A short explanation of [expression], or "" when it reads the obvious way.
     *
     * [percentage] is the setting, so the note can name what the percent was
     * taken of.
     */
    fun explain(
        expression: String,
        percentage: PercentageMode = PercentageMode.MULTIPLICATIVE,
        implied: ImpliedMultiplication = ImpliedMultiplication.SAME_PRECEDENCE,
    ): String {
        val notes = ArrayList<String>()

        implicitNote(expression, implied)?.let { notes.add(it) }
        percentageNote(expression, percentage)?.let { notes.add(it) }

        return notes.joinToString("  \u00B7  ")
    }

    /** `2π` and `3(4+5)` mean multiply, and the setting decides where that sits. */
    private fun implicitNote(expression: String, implied: ImpliedMultiplication): String? {
        val hasImplicit = Regex("""\d\s*[a-zA-Z(]|[a-zA-Z)]\s*[a-zA-Z(]""").containsMatchIn(expression)
        if (!hasImplicit) return null
        return when (implied) {
            ImpliedMultiplication.SAME_PRECEDENCE -> "a missing \u00D7 counts as \u00D7"
            ImpliedMultiplication.HIGHER_PRECEDENCE -> "a missing \u00D7 is worked out first"
        }
    }

    /**
     * `100 + 20%` has two readings, so say which one was used and what it came to.
     */
    private fun percentageNote(expression: String, percentage: PercentageMode): String? {
        // find the first "<something> + <n>%" or "- <n>%"
        val m = Regex("""([0-9.]+)\s*([+\-])\s*([0-9.]+)\s*%""").find(expression) ?: return null
        val base = m.groupValues[1].toDoubleOrNull() ?: return null
        val sign = m.groupValues[2]
        val pct = m.groupValues[3].toDoubleOrNull() ?: return null

        return when (percentage) {
            PercentageMode.MULTIPLICATIVE -> {
                val part = base * pct / 100.0
                val word = if (sign == "+") "of" else "off"
                "${Format.number(pct)}% $word ${Format.number(base)} = ${Format.number(part)}"
            }
            PercentageMode.ADDITIVE -> {
                val part = pct / 100.0
                val total = if (sign == "+") base + part else base - part
                "${Format.number(pct)}% is ${Format.number(part)}, so the answer is ${Format.number(total)}"
            }
        }
    }
}

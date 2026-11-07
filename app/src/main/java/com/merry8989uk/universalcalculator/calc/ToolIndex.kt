package com.merry8989uk.universalcalculator.calc

/**
 * Which reference formula has a working calculator behind it.
 *
 * The app has two formula surfaces and they did not know about each other: the
 * reference page, where a formula is written down, and the tools, where the same
 * formula is worked out. So somebody searching "determinant" on the reference
 * page was told it was something to read, while a determinant calculator sat one
 * tab away. That is not a gap in what the app can do — it is the app failing to
 * mention it.
 *
 * Matching is on the title, because that is the one thing both sides carry. The
 * two are written by different hands and the symbols differ — a reference says
 * 2×2 where a tool says 2x2 — so the title is folded down to its letters and
 * digits before comparing.
 */
object ToolIndex {

    /**
     * A title reduced to what two spellings have in common.
     *
     * Everything that is not a letter or a digit goes, and the rest is
     * lowercased: "Determinant of a 2×2 matrix" and "Determinant of a 2x2
     * matrix" both become "determinantofa2x2matrix".
     */
    fun key(title: String): String {
        val out = StringBuilder(title.length)
        for (c in title) {
            when {
                c.isLetterOrDigit() -> out.append(c.lowercaseChar())
                // the multiplication sign is the one symbol worth keeping: 2x2
                // and 2 by 2 are different matrices from 22
                c == '\u00D7' || c == '\u2715' -> out.append('x')
                else -> Unit
            }
        }
        return out.toString()
    }

    /**
     * The calculators by the folded title, ready to look up.
     *
     * Where two calculators share a title the first one wins, because the list is
     * in a deliberate order and the first is the one written for that name.
     */
    fun byTitle(tools: List<Formula>): Map<String, Formula> {
        val out = LinkedHashMap<String, Formula>(tools.size)
        for (tool in tools) {
            val k = key(tool.title)
            if (k.isNotEmpty() && !out.containsKey(k)) out[k] = tool
        }
        return out
    }

    /** The calculator for this reference title, or null when there is none. */
    fun forTitle(referenceTitle: String, index: Map<String, Formula>): Formula? =
        index[key(referenceTitle)]
}

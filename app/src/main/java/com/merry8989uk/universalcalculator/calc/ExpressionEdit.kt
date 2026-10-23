package com.merry8989uk.universalcalculator.calc

/**
 * Small edits to an expression that the ⇄ key needs.
 */
object ExpressionEdit {

    private const val OPS = "+-*/^"

    /**
     * Swaps the two sides of the last operator that binds loosest, so the value
     * does not change by accident:
     *
     *   "5+3"      -> "3+5"
     *   "5+3*2"    -> "3*2+5"     (still 11, not 13)
     *   "(1+2)*3"  -> "3*(1+2)"
     *
     * A side that starts with a minus sign is bracketed so the result still
     * evaluates: "-5+3" -> "3+(-5)".
     */
    fun swapOperands(text: String): String {
        if (text.isBlank()) return text

        // loosest binding first: + and -, then * and /, then ^
        for (group in listOf("+-", "*/", "^")) {
            var depth = 0
            var split = -1
            for (i in text.indices) {
                val c = text[i]
                when {
                    c == '(' -> depth++
                    c == ')' -> depth--
                    depth == 0 && c in group -> {
                        val prev = text.getOrNull(i - 1)
                        val unary = (c == '-' || c == '+') &&
                            (prev == null || prev in OPS || prev == '(')
                        if (!unary && i > 0 && i < text.length - 1) split = i
                    }
                }
            }
            if (split > 0) {
                val left = text.substring(0, split)
                val right = text.substring(split + 1)
                if (left.isNotBlank() && right.isNotBlank()) {
                    val l = if (left.startsWith("-")) "($left)" else left
                    val r = if (right.startsWith("-")) "($right)" else right
                    return r + text[split] + l
                }
            }
        }
        return text
    }

    /** True when the expression uses the variable x. */
    fun usesVariable(text: String): Boolean =
        Regex("(?<![A-Za-z])x(?![A-Za-z])").containsMatchIn(text)
}

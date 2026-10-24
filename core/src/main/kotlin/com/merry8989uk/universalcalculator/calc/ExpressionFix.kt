package com.merry8989uk.universalcalculator.calc

/**
 * Turns a half-typed expression into one the evaluator can actually run.
 *
 * A calculator should not scold you for pressing `=` a moment early. If a
 * bracket is still open it is closed, a trailing operator is dropped, and a
 * closing bracket that closes nothing is removed. What is left is evaluated, so
 * `sin(30` gives 0.5 rather than "Missing ')'".
 *
 * `%` and `!` are postfix, so they are left alone: `50%` is already complete.
 */
object ExpressionFix {

    fun balance(
        text: String,
        closeBrackets: Boolean = true,
        dropTrailingOperator: Boolean = true,
    ): String {
        var t = text.trim()
        if (t.isEmpty()) return ""

        // 1. drop a trailing operator, or an open bracket nobody has filled in
        while (t.isNotEmpty()) {
            when (t.last()) {
                '+', '-', '*', '/', '^', ',' -> t = t.dropLast(1).trimEnd()
                '(' -> {
                    t = t.dropLast(1).trimEnd()
                    // and the name that opened it, so "sin(" becomes nothing
                    while (t.isNotEmpty() && (t.last().isLetterOrDigit() || t.last() == '_')) {
                        t = t.dropLast(1)
                    }
                    t = t.trimEnd()
                }
                else -> break
            }
        }
        if (t.isEmpty()) return ""

        // 2. drop brackets that close nothing, then close what is still open
        val out = StringBuilder()
        var depth = 0
        for (c in t) {
            when (c) {
                '(' -> { depth++; out.append(c) }
                ')' -> if (depth > 0) { depth--; out.append(c) }
                else -> out.append(c)
            }
        }
        repeat(depth) { out.append(')') }
        return out.toString()
    }

    /**
     * The ± key: flips the sign of the operand the user last typed.
     *
     * With nothing to negate it opens a minus; otherwise the trailing operand is
     * wrapped, and pressing ± again unwraps it. Either way the result is still a
     * complete expression.
     */
    fun negate(text: String): String {
        if (text.isEmpty()) return "-"
        if (text.last() in "+-*/^%(") return "$text-"

        // walk back to the start of the trailing operand, skipping over brackets
        var depth = 0
        var i = text.length
        while (i > 0) {
            val c = text[i - 1]
            when {
                c == ')' -> depth++
                c == '(' -> { if (depth == 0) break else depth-- }
                depth == 0 && c in "+-*/^%," -> break
            }
            i--
        }
        val head = text.substring(0, i)
        val operand = text.substring(i)
        if (operand.isEmpty()) return "$text-"

        // pressing ± twice puts it back the way it was
        if (operand.startsWith("(-") && operand.endsWith(")")) {
            return head + operand.substring(2, operand.length - 1)
        }
        return "$head(-$operand)"
    }
}

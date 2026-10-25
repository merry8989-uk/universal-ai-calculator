package com.merry8989uk.universalcalculator.calc

/**
 * Reads an expression the way a person would and says what is wrong with it.
 *
 * The evaluator already refuses nonsense, but it refuses it with a parser
 * message — "Unexpected 's'" — which tells the user nothing. This says the
 * thing they can act on: *sin needs a value after it*, *two operators in a row*,
 * *that symbol is not one the calculator knows*.
 *
 * A warning is not a crash: the calculator shows it and waits, rather than
 * working out a wrong answer.
 */
object ExpressionGuard {

    /** The functions the calculator knows, longest first so `asinh` wins over `a`. */
    private val FUNCTIONS = listOf(
        "arcsinh", "arccosh", "arctanh", "arcsin", "arccos", "arctan",
        "asinh", "acosh", "atanh", "asin", "acos", "atan",
        "sinh", "cosh", "tanh", "coth", "sech", "csch",
        "sin", "cos", "tan", "cot", "sec", "csc",
        "log2", "log10", "log", "ln", "exp", "sqrt", "cbrt", "abs",
        "floor", "ceil", "round", "sign", "gamma", "lgamma", "erf", "erfc",
        "zeta", "lambertw", "digamma", "beta", "hypot", "nthroot", "mod",
        "gcd", "lcm", "ncr", "npr", "fib", "catalan", "totient", "isqrt",
        "isprime", "nextprime", "rand", "sinc", "mround", "sum", "prod",
        "mean", "median", "min", "max", "range", "geomean", "harmean",
    ).sortedByDescending { it.length }

    /** Whether [word] is a name the calculator knows: a function or a variable. */
    private fun isAName(word: String): Boolean =
        FUNCTIONS.any { it.equals(word, ignoreCase = true) } ||
            CONSTANTS.any { it.equals(word, ignoreCase = true) }

    /**
     * Splits a run of letters into the names it is made of, or null when it
     * is not made of names at all.
     *
     * `sinsinsin` is three `sin`. Anything with a leftover letter is not a
     * decomposition, which is how `sink` is told apart from `sin`.
     */
    private fun decompose(word: String): List<String>? {
        val parts = ArrayList<String>()
        var rest = word
        while (rest.isNotEmpty()) {
            val name = FUNCTIONS.firstOrNull { rest.startsWith(it, ignoreCase = true) } ?: return null
            parts.add(name)
            rest = rest.substring(name.length)
        }
        return if (parts.size >= 2) parts else null
    }

    /** The names that stand for a value rather than doing something to one. */
    private val CONSTANTS = setOf(
        "pi", "\u03C0", "tau", "\u03C4", "e", "i", "x", "ans",
        "inf", "infinity", "golden", "\u03C6",
    )

    /** The characters an expression may contain. */
    private val ALLOWED = "0123456789.+-*/^()!% \u03C0\u03C4\u221A\u00D7\u00F7\u2212\u00B0,{}[]_"

    private val OPERATORS = setOf('+', '-', '*', '/', '^')

    /**
     * What is wrong with [text], or null when there is nothing to say.
     *
     * The messages are short and say what to do, not what the parser saw.
     */
    fun check(text: String): String? {
        val t = text.trim()
        if (t.isEmpty()) return null

        // 1. a symbol the calculator has no use for
        val unknown = t.firstOrNull { it !in ALLOWED && !it.isLetter() }
        if (unknown != null) return "'$unknown' is not a symbol this calculator knows."

        // 2. a run of letters that is not a name it knows
        for (word in Regex("[A-Za-z]+").findAll(t).map { it.value }) {
            if (isAName(word)) continue
            // it may be several names stuck together, which is what sinsinsin is
            val parts = decompose(word)
            if (parts != null) {
                return parts.first() + " is repeated. A function needs a value after it, " +
                    "as in " + parts.first() + "(30)."
            }
            return "'$word' is not a function this calculator knows."
        }

        // 3. a function with nothing after it, or two functions in a row
        val names = Regex("[A-Za-z]+").findAll(t).map { it.range.first to it.value }.toList()
        for ((at, word) in names) {
            if (!FUNCTIONS.any { it.equals(word, ignoreCase = true) }) continue
            val after = t.substring(at + word.length).trimStart()
            if (after.isEmpty()) return "$word needs a value after it, as in ${word}(30)."
            if (after.first().isLetter()) {
                val next = Regex("[A-Za-z]+").find(after)?.value ?: ""
                return "$word cannot be followed by $next. Put a value between them."
            }
        }

        // 4. two operators in a row, unless the second is a minus after ( or ^,
        //    which is how a negative number is written
        var previous = ' '
        for ((index, c) in t.withIndex()) {
            if (c in OPERATORS) {
                if (previous in OPERATORS) {
                    val allowed = c == '-' && (previous == '^' || previous == '*' || previous == '/')
                    if (!allowed) return "Two operators in a row: '$previous$c'. One number is missing."
                }
            }
            if (c != ' ') previous = c
        }

        // 5. an operator with nothing before it
        val firstMeaningful = t.firstOrNull { it != ' ' }
        if (firstMeaningful != null && firstMeaningful in setOf('*', '/', '^', '%', '!')) {
            return "The expression starts with '$firstMeaningful', which needs a number before it."
        }

        // 6. an operator with nothing after it — but only complain once the user
        //    has stopped typing, which the caller decides
        return null
    }

    /**
     * Whether the expression ends mid-thought: an operator or an open bracket
     * with nothing after it.
     *
     * The calculator uses this to stay quiet while a key is being pressed, and
     * to say something when the user asks for an answer.
     */
    fun isUnfinished(text: String): Boolean {
        val t = text.trimEnd()
        if (t.isEmpty()) return false
        if (t.last() in OPERATORS) return true
        if (t.last() == '(' || t.last() == ',' || t.last() == '/') return true
        return t.count { it == '(' } > t.count { it == ')' }
    }

    /**
     * The same text with a negative number bracketed, so `5--8` reads as
     * `5-(-8)`.
     *
     * A double sign is legal arithmetic but it is not what the user meant to
     * type, and bracketing it makes the screen agree with the answer.
     */
    fun bracketNegativeNumbers(text: String): String {
        val out = StringBuilder(text.length + 4)
        for ((index, c) in text.withIndex()) {
            if (c == '-' && out.isNotEmpty()) {
                val previous = out.last()
                val after = text.getOrNull(index + 1)
                // a minus that follows another operator, and is followed by a
                // number, a name or an open bracket, is a sign, not a subtraction
                // only when it follows another operator: a minus already
                // sitting after an open bracket is fine as it is
                val isSign = previous in OPERATORS
                val startsAValue = after != null &&
                    (after.isDigit() || after == '.' || after.isLetter() || after == '(')
                if (isSign && startsAValue) {
                    out.append("(-")
                    continue
                }
            }
            out.append(c)
        }
        val opened = out.count { it == '(' }
        val closed = out.count { it == ')' }
        if (opened > closed) repeat(opened - closed) { out.append(')') }
        return out.toString()
    }
}

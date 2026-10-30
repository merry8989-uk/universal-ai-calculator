package com.merry8989uk.universalcalculator.formula

/**
 * Turns a formula as it is printed into an expression the evaluator can run.
 *
 * A book writes `c = √(a² + b²)`; the evaluator speaks `sqrt(a^2+b^2)`. The gap
 * between the two is where a formula library quietly stops working, so this is
 * deliberately a separate, tested step rather than something done inline: when a
 * formula will not compute, the normaliser is the first place to look.
 *
 * Nothing here changes the meaning of the formula. If it cannot be translated
 * faithfully the result is null and the formula stays a reference, which is the
 * honest outcome — a wrong answer is worse than no button.
 */
object FormulaNormaliser {

    /** Superscript digits and signs, and the letters that appear raised. */
    private val SUPERSCRIPTS = mapOf(
        '⁰' to "0", '¹' to "1", '²' to "2", '³' to "3", '⁴' to "4",
        '⁵' to "5", '⁶' to "6", '⁷' to "7", '⁸' to "8", '⁹' to "9",
        '⁻' to "-", '⁺' to "+", 'ⁿ' to "n", 'ˣ' to "x", 'ᵃ' to "a",
        'ᵇ' to "b", 'ᶜ' to "c", 'ᶠ' to "f", 'ᵏ' to "k", 'ᵐ' to "m",
        'ᵗ' to "t", 'ʸ' to "y", 'ᵖ' to "p", 'ʳ' to "r", 'ˢ' to "s",
        'ᵛ' to "v", 'ʷ' to "w", 'ᵘ' to "u", 'ᵈ' to "d", 'ᵍ' to "g",
        'ᵒ' to "o", 'ʰ' to "h", 'ʲ' to "j", 'ᵀ' to "T",
    )

    /** Subscripts, which are part of a name: a₁ is the variable a1. */
    private val SUBSCRIPTS = mapOf(
        '₀' to "0", '₁' to "1", '₂' to "2", '₃' to "3", '₄' to "4",
        '₅' to "5", '₆' to "6", '₇' to "7", '₈' to "8", '₉' to "9",
        'ₐ' to "a", 'ₑ' to "e", 'ₒ' to "o", 'ₓ' to "x", 'ₙ' to "n",
        'ₜ' to "t", 'ₛ' to "s", 'ₚ' to "p", 'ₘ' to "m", 'ₗ' to "l",
        'ᵢ' to "i", 'ⱼ' to "j", 'ₖ' to "k", 'ᵣ' to "r", 'ᵤ' to "u",
        'ᵥ' to "v", 'ᵦ' to "b", 'ᵧ' to "g", 'ₕ' to "h",
    )

    /**
     * Greek letters become plain identifiers.
     *
     * They are kept distinct from the latin ones — α is a1, not a — because a
     * formula containing both α and a is common and collapsing them would
     * silently change the answer.
     */
    private val GREEK = mapOf(
        'α' to "a1", 'β' to "b1", 'γ' to "g1", 'δ' to "d1", 'ε' to "e1",
        'ζ' to "z1", 'η' to "h1", 'θ' to "th", 'ι' to "i1", 'κ' to "k1",
        'λ' to "l1", 'μ' to "mu1", 'ν' to "n1", 'ξ' to "x1", 'ρ' to "r1",
        'σ' to "s1", 'τ' to "t1", 'φ' to "p1", 'χ' to "c1", 'ψ' to "ps",
        'ω' to "w1",
        // The capitals get a whole word, not a digit suffix. The evaluator
        // lower-cases every identifier, so "D1" would arrive as "d1" and collide
        // with δ — a formula holding both would quietly use one for the other.
        'Δ' to "delta", 'Σ' to "sigma", 'Φ' to "phi", 'Ω' to "omega",
        'Λ' to "lambda", 'Π' to "pi2", 'Γ' to "gamma", 'Θ' to "theta2",
    )

    /** Names the evaluator knows. Everything else followed by ( is a product. */
    private val FUNCTIONS = setOf(
        "sin", "cos", "tan", "csc", "sec", "cot", "asin", "acos", "atan",
        "acot", "asec", "acsc", "sinh", "cosh", "tanh", "coth", "sech", "csch",
        "asinh", "acosh", "atanh", "sqrt", "cbrt", "log", "ln", "abs", "exp",
        "floor", "ceil", "round", "max", "min", "pow", "sign", "sinc", "atan2",
    )

    /** Names that stay whole: functions, constants and the Greek stand-ins. */
    private val KNOWN_NAMES: Set<String> = FUNCTIONS + setOf(
        "pi", "e", "th", "mu1", "ps", "delta", "sigma", "phi", "omega",
        "lambda", "pi2", "gamma", "theta2",
    )


    /**
     * Anything that means the formula is not a plain calculation.
     *
     * A derivative, an integral, a limit, a set, a sum over a range, an
     * inequality, an ellipsis, a plus-or-minus: each of these needs something
     * other than "put numbers in, get a number out".
     */
    private val NOT_A_PLAIN_CALCULATION = listOf(
        "dy/dx", "d²y", "d/dx", "∂", "∫", "∬", "lim", "Σ", "∑", "→", "…",
        "±", "∞", "≠", "≤", "≥", "∈", "∉", "∪", "∩", "⊂", "⊆", "∅", "′",
        "∇", "≈",
    )

    fun containsStructure(printed: String): String? =
        NOT_A_PLAIN_CALCULATION.firstOrNull { printed.contains(it) }

    /** True when the evaluator has a function by this name. */
    fun isFunction(name: String): Boolean = name in FUNCTIONS

    /**
     * The expression, ready to evaluate, or null when it cannot be translated.
     *
     * Returns null rather than a best guess: a formula that cannot be normalised
     * must stay a reference, and a silently mangled one would give a confident
     * wrong answer.
     */
    fun normalise(printed: String): String? {
        val out = StringBuilder(printed.length)
        var i = 0
        while (i < printed.length) {
            val c = printed[i]
            val sup = SUPERSCRIPTS[c]
            if (sup != null) {
                // a run of raised characters becomes one power
                val run = StringBuilder()
                while (i < printed.length && SUPERSCRIPTS[printed[i]] != null) {
                    run.append(SUPERSCRIPTS.getValue(printed[i]))
                    i++
                }
                val power = run.toString()
                if (power.all { it.isDigit() }) {
                    out.append('^').append(power)
                } else {
                    out.append("^(").append(power).append(')')
                }
                continue
            }
            val sub = SUBSCRIPTS[c]
            if (sub != null) {
                out.append(sub); i++; continue
            }
            val greek = GREEK[c]
            if (greek != null) {
                out.append(greek); i++; continue
            }
            when (c) {
                '√' -> { out.append("sqrt"); i++; continue }
                '∛' -> { out.append("cbrt"); i++; continue }
                '×', '·', '⋅' -> out.append('*')
                '÷' -> out.append('/')
                '−', '–', '—' -> out.append('-')
                'π' -> out.append("pi")
                '½' -> out.append("(1/2)")
                '¼' -> out.append("(1/4)")
                '¾' -> out.append("(3/4)")
                '{', '[' -> out.append('(')
                '}', ']' -> out.append(')')
                ' ' -> Unit
                else -> out.append(c)
            }
            i++
        }

        var s = out.toString()
        if (s.isBlank()) return null

        s = bracketRoots(s)
        s = insertImplicitProducts(s)
        if (s.isBlank()) return null
        return s
    }

    /**
     * Gives every root its brackets.
     *
     * `√(a² + b²)` already has them and is left alone. `√a²` does not, and the
     * operand is everything up to the next operator at the same level, so
     * `√a² + b` becomes `sqrt(a^2)+b` and not `sqrt(a^2+b)`.
     */
    private fun bracketRoots(s: String): String {
        val out = StringBuilder(s.length + 8)
        var i = 0
        while (i < s.length) {
            val isRoot = s.startsWith("sqrt", i) || s.startsWith("cbrt", i)
            if (!isRoot) {
                out.append(s[i]); i++; continue
            }
            val name = if (s.startsWith("sqrt", i)) "sqrt" else "cbrt"
            out.append(name)
            i += name.length
            // already bracketed: copy the group as it is
            if (i < s.length && s[i] == '(') {
                var depth = 0
                while (i < s.length) {
                    val c = s[i]
                    if (c == '(') depth++
                    if (c == ')') depth--
                    out.append(c)
                    i++
                    if (depth == 0) break
                }
                continue
            }
            // not bracketed: gather the operand
            var depth = 0
            val operand = StringBuilder()
            while (i < s.length) {
                val c = s[i]
                if (c == '(') depth++
                if (c == ')') {
                    if (depth == 0) break
                    depth--
                }
                if (depth == 0 && (c == '+' || c == '-' || c == '*' || c == '/' || c == '=')) break
                operand.append(c)
                i++
            }
            out.append('(').append(operand).append(')')
        }
        return out.toString()
    }

    /**
     * Puts the multiplication signs back in.
     *
     * `(a-b)(a+b)` and `2ab` are how a book writes a product; the evaluator wants
     * `(a-b)*(a+b)` and `2*a*b`. The expression is first cut into tokens, because
     * `ab` is a times b while `sin` is one name — splitting by character would
     * turn every function into a product.
     */
    fun insertImplicitProducts(s: String): String {
        val tokens = ArrayList<String>(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isLetter() -> {
                    val rest = s.substring(i).lowercase()
                    val known = KNOWN_NAMES.filter { rest.startsWith(it) }.maxByOrNull { it.length }
                    if (known != null) {
                        tokens.add(s.substring(i, i + known.length))
                        i += known.length
                    } else {
                        tokens.add(c.toString())
                        i++
                    }
                }
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    tokens.add(s.substring(start, i))
                }
                else -> {
                    tokens.add(c.toString())
                    i++
                }
            }
        }
        val out = StringBuilder(s.length + 8)
        for (index in tokens.indices) {
            if (index > 0 && needsProduct(tokens[index - 1], tokens[index])) out.append('*')
            out.append(tokens[index])
        }
        return out.toString()
    }

    /** True when two tokens next to each other are a product rather than a call. */
    private fun needsProduct(previous: String, next: String): Boolean {
        val leftIsValue = previous.last().isLetterOrDigit() || previous == ")"
        val rightIsValue = next.first().isLetterOrDigit() || next == "("
        if (!leftIsValue || !rightIsValue) return false
        // a name followed by a bracket is a function call, not a product
        if (next == "(" && isFunction(previous)) return false
        return true
    }

    /** The variables an expression needs, ignoring function and constant names. */
    fun variables(expression: String): List<String> {
        val found = LinkedHashSet<String>()
        var i = 0
        while (i < expression.length) {
            val c = expression[i]
            if (c.isLetter()) {
                val start = i
                while (i < expression.length && expression[i].isLetterOrDigit()) i++
                val word = expression.substring(start, i)
                if (word !in FUNCTIONS && word != "pi" && word != "e") found.add(word)
            } else {
                i++
            }
        }
        return found.toList()
    }

    /** A variable name that a person can read: a1 becomes "a". */
    fun friendlyName(id: String): String = when {
        id == "th" -> "θ"
        id == "mu1" -> "μ"
        id == "delta" -> "Δ"
        id == "sigma" -> "Σ"
        id == "phi" -> "Φ"
        id == "omega" -> "Ω"
        id == "lambda" -> "Λ"
        id == "pi2" -> "Π"
        id == "gamma" -> "Γ"
        id == "theta2" -> "Θ"
        id.length == 2 && id[1] == '1' -> id[0].toString()
        else -> id
    }
}

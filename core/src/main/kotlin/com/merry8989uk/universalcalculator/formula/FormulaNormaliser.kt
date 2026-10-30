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
        // A subscript letter is part of the name it hangs from: lₐ is one
        // variable, and read as l times a it becomes a different formula. The
        // underscore marks that, and the reader keeps it whole.
        'ₐ' to "_a", 'ₑ' to "_e", 'ₒ' to "_o", 'ₓ' to "_x", 'ₙ' to "_n",
        'ₜ' to "_t", 'ₛ' to "_s", 'ₚ' to "_p", 'ₘ' to "_m", 'ₗ' to "_l",
        'ᵢ' to "_i", 'ⱼ' to "_j", 'ₖ' to "_k", 'ᵣ' to "_r", 'ᵤ' to "_u",
        'ᵥ' to "_v", 'ᵦ' to "_b", 'ᵧ' to "_g", 'ₕ' to "_h",
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
        // combinations and permutations: real evaluator functions
        "ncr", "npr",
    )

    /** Names that stay whole: functions, constants and the Greek stand-ins. */
    private val KNOWN_NAMES: Set<String> = FUNCTIONS + setOf(
        "pi", "e", "th", "mu1", "ps", "delta", "sigma", "phi", "omega",
        "lambda", "pi2", "gamma", "theta2",
        // The single-letter Greek stand-ins. Without these the tokeniser splits
        // d1 into d times 1, which silently turns one variable into two.
        "a1", "b1", "g1", "d1", "e1", "z1", "h1", "i1", "k1", "l1",
        "n1", "x1", "r1", "s1", "t1", "c1", "w1",
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
        // A transpose is not a power: (Aᵀ)ᵀ = A is true of matrices and false of
        // numbers, so computing it as a power gives a confident wrong answer. A
        // matrix inverse is deliberately not on this list — x⁻¹ is an ordinary
        // reciprocal and belongs in the calculator.
        "ᵀ",
    )

    fun containsStructure(printed: String): String? =
        NOT_A_PLAIN_CALCULATION.firstOrNull { printed.contains(it) }

    /**
     * Operations a book writes as a name but the evaluator does not have.
     *
     * They are still treated as calls rather than products, because det(AB) read
     * as d times e times t times AB is not a smaller answer — it is a confident
     * wrong one. Left as a call it fails to evaluate, which is the honest
     * outcome: the app says it cannot do this, rather than doing it badly.
     */
    private val SYMBOLIC_NAMES = setOf(
        "det", "rank", "nullity", "adj", "trace", "var", "cov", "corr",
        "arg", "mod", "deg", "lcm", "hcf", "gcd",
    )

    /** True when the evaluator has a function by this name. */
    fun isFunction(name: String): Boolean = name in FUNCTIONS

    /** True when a name is followed by a bracket, whether or not it runs. */
    fun takesBracket(name: String): Boolean =
        name in FUNCTIONS || name.lowercase() in SYMBOLIC_NAMES

    /**
     * The expression, ready to evaluate, or null when it cannot be translated.
     *
     * Returns null rather than a best guess: a formula that cannot be normalised
     * must stay a reference, and a silently mangled one would give a confident
     * wrong answer.
     */
    fun normalise(printed: String): String? {
        // A logarithm with its base written underneath is rewritten before
        // anything else, because the subscript is part of the function name and
        // would otherwise be read as a variable multiplied in.
        val source = rewriteCombinations(rewriteLogBases(printed))
        val out = StringBuilder(source.length)
        var i = 0
        while (i < source.length) {
            val c = source[i]
            val sup = SUPERSCRIPTS[c]
            if (sup != null) {
                // a run of raised characters becomes one power
                val run = StringBuilder()
                while (i < source.length && SUPERSCRIPTS[source[i]] != null) {
                    run.append(SUPERSCRIPTS.getValue(source[i]))
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
                // a bare degree sign is a number of degrees: 90° is 90 in radian
                // measure, and the evaluator works in radians
                '°' -> out.append("*pi/180")
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
        s = bracketAbsolute(s)
        s = insertImplicitProducts(s)
        if (s.isBlank()) return null
        return s
    }

    /** nCr and nPr, with one letter on each side. */
    private val COMBINATION = Regex("([A-Za-z0-9])C([A-Za-z0-9])")
    private val PERMUTATION = Regex("([A-Za-z0-9])P([A-Za-z0-9])")
    /** nC(r − 1): the second argument written in brackets. */
    private val COMBINATION_BRACKETED = Regex("([A-Za-z0-9])C\\(([^()]*)\\)")
    private val PERMUTATION_BRACKETED = Regex("([A-Za-z0-9])P\\(([^()]*)\\)")

    /**
     * Rewrites nCr as ncr(n,r).
     *
     * This has to happen before the reader runs, because nCr is three letters in
     * a row and would otherwise be taken as one name — which is at least not a
     * product, but is still not a combination.
     */
    fun rewriteCombinations(text: String): String {
        // the bracketed argument first, so nC(r − 1) is not mistaken for a
        // one-letter argument followed by a stray bracket
        var out = COMBINATION_BRACKETED.replace(text) { m ->
            "ncr(" + m.groupValues[1] + "," + m.groupValues[2] + ")"
        }
        out = PERMUTATION_BRACKETED.replace(out) { m ->
            "npr(" + m.groupValues[1] + "," + m.groupValues[2] + ")"
        }
        out = COMBINATION.replace(out) { m ->
            "ncr(" + m.groupValues[1] + "," + m.groupValues[2] + ")"
        }
        return PERMUTATION.replace(out) { m ->
            "npr(" + m.groupValues[1] + "," + m.groupValues[2] + ")"
        }
    }

    /** A logarithm with its base written as a subscript. */
    private val LOG_BASE = Regex("log([\u2080-\u2089\u2090-\u209C]+)")

    /**
     * Rewrites logₐx as ln(x)/ln(a).
     *
     * The evaluator's `log` is base ten and `ln` is natural, so a base written
     * underneath has to be turned into a ratio of natural logarithms. Without
     * this, logₐ(xy) is read as log(a) times xy, and every logarithm identity
     * comes out wrong rather than failing loudly.
     */
    fun rewriteLogBases(text: String): String {
        if (!LOG_BASE.containsMatchIn(text)) return text
        val out = StringBuilder(text.length + 16)
        var last = 0
        for (match in LOG_BASE.findAll(text)) {
            out.append(text, last, match.range.first)
            // the underscore marks a subscripted variable name; a base is a
            // plain name, so it is stripped here
            val base = match.groupValues[1]
                .mapNotNull { SUBSCRIPTS[it] }
                .joinToString("")
                .replace("_", "")
            var i = match.range.last + 1
            val argument: String
            if (i < text.length && text[i] == '(') {
                var depth = 0
                val start = i
                while (i < text.length) {
                    if (text[i] == '(') depth++
                    if (text[i] == ')') {
                        depth--
                        if (depth == 0) {
                            i++
                            break
                        }
                    }
                    i++
                }
                argument = text.substring(start, i)
            } else {
                val start = i
                while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '√')) i++
                argument = text.substring(start, i)
            }
            if (base.isEmpty() || argument.isEmpty()) {
                out.append(text, match.range.first, match.range.last + 1)
            } else {
                out.append("(ln").append(argument).append("/ln(").append(base).append("))")
            }
            last = i
        }
        out.append(text, last, text.length)
        return out.toString()
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
     * Turns |x| into abs(x).
     *
     * A pair of pipes is an absolute value; a single one is "divides" in number
     * theory, which is not something to guess at, so an odd count is left alone
     * and the formula stays a reference.
     */
    private fun bracketAbsolute(s: String): String {
        if (s.count { it == '|' } == 0) return s
        if (s.count { it == '|' } % 2 != 0) return s
        val out = StringBuilder(s.length + 8)
        var open = true
        for (c in s) {
            if (c == '|') {
                out.append(if (open) "abs(" else ")")
                open = !open
            } else {
                out.append(c)
            }
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
                    // A capital is a variable of its own, and a function name is
                    // never allowed to start at one. The books write
                    // "sin A cos B" for sin(A)·cos(B); without this the reader
                    // takes "Acos" out of the middle and every addition formula
                    // in trigonometry becomes nonsense.
                    if (c.isUpperCase()) {
                        val rest = s.substring(i)
                        val lower = rest.lowercase()
                        // ...unless the book always writes this name in capitals
                        val symbolic = SYMBOLIC_NAMES
                            .filter { lower.startsWith(it) && it.length > 1 }
                            .maxByOrNull { it.length }
                        if (symbolic != null) {
                            tokens.add(rest.substring(0, symbolic.length))
                            i += symbolic.length
                            continue
                        }
                        // a run of capitals is one name: PV, FV, EAR, AB
                        val start = i
                        while (i < s.length && s[i].isUpperCase()) i++
                        tokens.add(s.substring(start, i))
                        continue
                    }

                    val start = i
                    while (i < s.length && (s[i].isLetter() || s[i].isDigit() || s[i] == '_')) i++
                    val run = s.substring(start, i)
                    val lower = run.lowercase()

                    // a name the reader knows always wins, so sinth is sin(th)
                    val known = (KNOWN_NAMES + SYMBOLIC_NAMES)
                        .filter { lower.startsWith(it) }
                        .maxByOrNull { it.length }
                    if (known != null) {
                        tokens.add(run.substring(0, known.length))
                        i = start + known.length
                        continue
                    }

                    // Otherwise a long run is a name — det, rank, nullity — and a
                    // short one is a product of letters. A name carrying a
                    // subscript or a number is one variable: l_a, a1, a2.
                    val hasSubscript = run.contains('_')
                    val hasDigit = run.any { it.isDigit() }
                    if (run.length >= 3 || hasSubscript || hasDigit) {
                        tokens.add(run)
                        continue
                    }
                    for (ch in run) tokens.add(ch.toString())
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
        // A function written without brackets takes the next single token as its
        // argument: a book writes sinA cosB, meaning sin(A)·cos(B). Without this
        // the tokeniser reads "acos" out of "AcosB" and the whole identity is
        // nonsense.
        val fixed = ArrayList<String>(tokens.size + 4)
        var at = 0
        while (at < tokens.size) {
            val token = tokens[at]
            if (!isFunction(token) || at + 1 >= tokens.size || tokens[at + 1] == "(") {
                fixed.add(token)
                at++
                continue
            }

            // sin²θ — a power written on the function belongs to its result, so
            // it has to move behind the argument: sin(θ)^2, not sin^2*θ.
            if (at + 3 < tokens.size && tokens[at + 1] == "^" &&
                tokens[at + 2].first().isDigit() && tokens[at + 3].first().isLetterOrDigit()
            ) {
                fixed.add(token + "(")
                fixed.add(tokens[at + 3])
                fixed.add(")^" + tokens[at + 2])
                at += 4
                continue
            }

            // cos 2θ — a coefficient and its variable are one argument.
            if (at + 2 < tokens.size && tokens[at + 1].first().isDigit() &&
                tokens[at + 2].first().isLetter()
            ) {
                fixed.add(token + "(")
                fixed.add(tokens[at + 1] + "*" + tokens[at + 2])
                fixed.add(")")
                at += 3
                continue
            }

            // sinA — a single token is the argument.
            val argument = tokens[at + 1]
            if (argument.first().isLetterOrDigit() || argument == "(") {
                fixed.add(token + "(")
                fixed.add(argument)
                fixed.add(")")
                at += 2
                continue
            }

            fixed.add(token)
            at++
        }

        val out = StringBuilder(s.length + 8)
        for (index in fixed.indices) {
            if (index > 0 && needsProduct(fixed[index - 1], fixed[index])) out.append('*')
            out.append(fixed[index])
        }
        return out.toString()
    }

    /** True when two tokens next to each other are a product rather than a call. */
    private fun needsProduct(previous: String, next: String): Boolean {
        val leftIsValue = previous.last().isLetterOrDigit() || previous == ")"
        val rightIsValue = next.first().isLetterOrDigit() || next == "("
        if (!leftIsValue || !rightIsValue) return false
        // a name followed by a bracket is a function call, not a product
        if (next == "(" && takesBracket(previous)) return false
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
    /** The subscript form of a letter, for showing a name to a person. */
    private fun subscriptOf(c: Char): String = when (c) {
        'a' -> "ₐ"; 'e' -> "ₑ"; 'o' -> "ₒ"; 'x' -> "ₓ"; 'n' -> "ₙ"
        't' -> "ₜ"; 's' -> "ₛ"; 'p' -> "ₚ"; 'm' -> "ₘ"; 'l' -> "ₗ"
        'i' -> "ᵢ"; 'j' -> "ⱼ"; 'k' -> "ₖ"; 'r' -> "ᵣ"; 'u' -> "ᵤ"
        'v' -> "ᵥ"; 'b' -> "ᵦ"; 'g' -> "ᵧ"; 'h' -> "ₕ"
        else -> c.toString()
    }

    fun friendlyName(id: String): String = when {
        id.startsWith("l_") && id.length == 3 -> "l" + subscriptOf(id[2])
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

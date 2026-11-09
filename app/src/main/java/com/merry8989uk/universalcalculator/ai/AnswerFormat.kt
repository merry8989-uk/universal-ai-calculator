package com.merry8989uk.universalcalculator.ai

/**
 * Turns a model answer into clean plain text: strips Markdown emphasis,
 * heading marks, backticks and LaTeX, so the user never sees stray ** or $$.
 *
 * The LaTeX half is a rewrite, not a deletion. The version before this one
 * mapped the commands it knew and then deleted every remaining `\command`,
 * which is how `\sqrt{4}` arrived on the screen as `4`: not untidy, wrong, and
 * wrong in the one direction nobody catches, because a flattened formula still
 * looks like an answer. A command that is not in the table now keeps its name,
 * so `\sqrt{4}` reads `sqrt(4)` and `\foo{2}` reads `foo(2)` — both can be read
 * and re-typed, and neither can be mistaken for a number the model never gave.
 *
 * The arguments are read with a brace counter rather than a regular expression,
 * because maths nests. `\sqrt{\frac{1}{2}}` has to become `sqrt(1/2)`; a
 * `[^}]*` match stops at the first closing brace and turns that into nonsense.
 */
object AnswerFormat {

    private val D = "$"

    /**
     * The commands that stand for something shorter, and the layout commands
     * that stand for nothing at all.
     *
     * Kept as a table so the answer to "what does this command become?" is one
     * lookup, and so an unknown command is visibly absent from it rather than
     * hidden in the middle of a chain of replaces.
     */
    private val SYMBOLS: Map<String, String> = mapOf(
        // arithmetic and relations
        "times" to " x ",
        "cdot" to " x ",
        "ast" to " x ",
        "div" to " / ",
        "pm" to "+/-",
        "mp" to "-/+",
        "approx" to " ~ ",
        "neq" to " != ",
        "ne" to " != ",
        "leq" to " <= ",
        "le" to " <= ",
        "geq" to " >= ",
        "ge" to " >= ",
        "lt" to " < ",
        "gt" to " > ",
        "infty" to "infinity",
        "to" to " -> ",
        "rightarrow" to " -> ",
        "Rightarrow" to " => ",
        "implies" to " => ",
        "dots" to "...",
        "ldots" to "...",
        "cdots" to "...",
        "degree" to " deg ",
        "circ" to " deg ",
        "therefore" to "therefore",
        "because" to "because",
        // layout: how the maths is set, not what it says
        "left" to "",
        "right" to "",
        "displaystyle" to "",
        "textstyle" to "",
        "limits" to "",
        "nolimits" to "",
        "big" to "",
        "Big" to "",
        "bigg" to "",
        "Bigg" to "",
        "quad" to " ",
        "qquad" to " ",
    )

    /**
     * The commands that only dress their argument, so what they hold is the
     * maths and the command itself is not: `\text{Area}` is Area, and
     * `\overline{AB}` is the line AB.
     */
    private val WRAPPERS: Set<String> = setOf(
        "text", "textrm", "textbf", "textit", "textsf", "texttt",
        "mathrm", "mathbf", "mathit", "mathcal", "mathbb", "mathsf", "mathtt",
        "operatorname",
        "vec", "hat", "bar", "overline", "underline", "widehat", "widetilde",
        "overbrace", "underbrace", "boxed",
    )

    /** The commands that take a numerator and a denominator. */
    private val FRACTIONS: Set<String> = setOf("frac", "dfrac", "tfrac", "cfrac")

    fun clean(input: String): String {
        var s = input

        // Block and inline maths delimiters
        s = s.replace(D + D, "\n")
        s = s.replace(D, "")

        s = maths(s)

        // Markdown
        s = s.replace("**", "")          // bold
        s = s.replace("__", "")          // bold (underscores)
        s = s.replace("`", "")
        s = s.replace(Regex("(?m)^#{1,6}\\s*"), "")
        s = s.replace(Regex("(?m)^\\s*\\*\\s+"), "\u2022 ")
        s = s.replace(Regex("(?m)^\\s*-\\s+"), "\u2022 ")
        // Single-asterisk italics, e.g. *Note: ...*. Only when the run starts
        // with a letter, so "2 * 3 * 4" is left alone.
        s = s.replace(Regex("\\*([A-Za-z][^*\\n]*)\\*")) { it.groupValues[1] }
        s = s.replace(Regex("_([A-Za-z][^_\\n]*)_")) { it.groupValues[1] }
        s = s.replace("~~", "")          // strikethrough
        // Tidy the punctuation left behind
        s = s.replace(Regex(" +([,.;:!?])"), "$1")
        s = s.replace(Regex("\\n{2,}(?=\u2022)"), "\n")

        // Tidy whitespace
        s = s.replace(Regex("[ \\t]{2,}"), " ")
        s = s.replace(Regex("\\n{3,}"), "\n\n")

        return s.trim()
    }

    /**
     * Every LaTeX command in [input], written out in plain maths.
     *
     * One pass, character by character, because a command and its arguments
     * have to be read together: what `\sqrt` becomes depends on what it holds.
     */
    private fun maths(input: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c != '\\') {
                // Braces group whatever they hold, so a superscript still
                // groups: x^{2n} reads x^(2n) rather than x^2n.
                out.append(plain(c))
                i++
                continue
            }
            if (i + 1 >= input.length) {
                // A trailing backslash says nothing at all.
                i++
                continue
            }
            val next = input[i + 1]
            if (!next.isLetter()) {
                when (next) {
                    '\\' -> out.append('\n')                 // a row break in a matrix
                    '!', '-' -> {}                           // no width at all
                    ',', ';', ':', ' ' -> out.append(' ')    // thin space
                    else -> out.append(plain(next))          // \% \{ \} \& \$
                }
                i += 2
                continue
            }

            // The command's name: a run of letters after the backslash.
            var j = i + 1
            while (j < input.length && input[j].isLetter()) j++
            val name = input.substring(i + 1, j)
            var k = j

            // An optional argument in brackets, as in \sqrt[3]{8}.
            var option: String? = null
            if (k < input.length && input[k] == '[') {
                val close = input.indexOf(']', k)
                if (close > k) {
                    option = input.substring(k + 1, close)
                    k = close + 1
                }
            }

            // Its arguments in braces, as many as follow.
            val args = mutableListOf<String>()
            while (true) {
                val start = skipSpaces(input, k)
                if (start >= input.length || input[start] != '{') break
                val end = closingBrace(input, start)
                if (end < 0) break
                args += input.substring(start + 1, end)
                k = end + 1
            }

            out.append(write(name, option, args))
            i = k
        }
        return out.toString()
    }

    /** What one command, with what it was given, reads as. */
    private fun write(name: String, option: String?, args: List<String>): String {
        // The argument is maths too, and is rewritten the same way: this is what
        // makes \sqrt{\frac{1}{2}} come out as sqrt(1/2) rather than as a brace
        // soup nobody can read back.
        val inner = args.map { maths(it) }
        val grouped = inner.joinToString("") { "(" + it + ")" }
        return when {
            // An environment name is layout. Its entries are not: they are the
            // maths, and they are left standing between the markers.
            name == "begin" || name == "end" -> ""
            name in FRACTIONS ->
                if (inner.size >= 2) inner[0] + "/" + inner[1] else inner.firstOrNull().orEmpty()
            name == "sqrt" -> {
                val radicand = inner.firstOrNull()
                when {
                    radicand == null -> "sqrt"
                    // An index is a power: the cube root of 8 is 8 to the one
                    // third, which is something the calculator can work out.
                    option != null -> "(" + radicand + ")^(1/" + maths(option) + ")"
                    else -> "sqrt(" + radicand + ")"
                }
            }
            name in WRAPPERS -> inner.firstOrNull().orEmpty()
            SYMBOLS.containsKey(name) -> SYMBOLS.getValue(name) + grouped
            // Anything else keeps its name and its arguments. This is the line
            // that matters: an unknown command is a word the user can read and
            // type, never a hole where a number used to be.
            else -> name + grouped
        }
    }

    /** Braces group; every other character is itself. */
    private fun plain(c: Char): Char = when (c) {
        '{' -> '('
        '}' -> ')'
        else -> c
    }

    private fun skipSpaces(s: String, from: Int): Int {
        var i = from
        while (i < s.length && (s[i] == ' ' || s[i] == '\t')) i++
        return i
    }

    /**
     * The index of the brace that closes the one at [open], or -1 if the group
     * is never closed. Nested braces are counted, and an escaped brace is not
     * counted at all — `\sqrt{\{x\}}` has one group, not three.
     */
    private fun closingBrace(s: String, open: Int): Int {
        var depth = 0
        var i = open
        while (i < s.length) {
            when (s[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
                '\\' -> i++   // the escaped character is not structure
            }
            i++
        }
        return -1
    }
}

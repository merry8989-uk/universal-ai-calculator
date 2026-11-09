package com.merry8989uk.universalcalculator.calc

/**
 * One row of the hub's shelf: a screen, not a formula.
 *
 * The tools are not calculators — they have no inputs and no answer, they are
 * places to go. That is why they were kept in the screen rather than in the
 * library, and that is exactly why the search could not see them: it was handed
 * the formula list, and a tool was never in it. Typing "graph" was told there
 * was no such calculator while Graphing sat in the list above the field.
 */
data class HubTool(val id: String, val title: String, val subtitle: String)

/**
 * A row a search found, whichever shelf it came from.
 *
 * The hub draws both kinds of row the same way and the reader does not care
 * which list a name came out of — but a tap has to do different things: a tool
 * opens a screen, a calculator opens its inputs. Keeping the two apart in the
 * result is what lets the tap stay right without the row looking different.
 */
sealed class HubHit {

    abstract val id: String

    /** The English name, as written in the lists. The screen translates it. */
    abstract val title: String

    abstract val subtitle: String

    /** What a formula row is coloured by. A tool has no category, so it is empty. */
    open val category: String get() = ""

    /** The calculator behind this hit, or null when the hit is a tool. */
    open val formula: Formula? get() = null

    class ToolHit(val tool: HubTool) : HubHit() {
        override val id: String get() = tool.id
        override val title: String get() = tool.title
        override val subtitle: String get() = tool.subtitle
    }

    class CalcHit(override val formula: Formula) : HubHit() {
        override val id: String get() = formula.id
        override val title: String get() = formula.title
        override val subtitle: String get() = formula.description
        override val category: String get() = formula.category
    }
}

/** What one search turned up: the rows that matched, and what to offer when none did. */
data class HubMatches(
    val tools: List<HubTool> = emptyList(),
    val formulas: List<Formula> = emptyList(),
    /** Nearest names to offer when nothing matched. Empty when even they are no help. */
    val suggestions: List<HubHit> = emptyList(),
) {
    val isEmpty: Boolean get() = tools.isEmpty() && formulas.isEmpty()
}

/**
 * The hub's one search field.
 *
 * The hub is two lists that grew separately — twenty tools that open screens and
 * several hundred calculators that open inputs — and the field searched only the
 * second, so a reader who typed the name of a tool they could see on the same
 * page was told nothing matched. There is no way to make that read as anything
 * but a broken search.
 *
 * So the matching lives here, over both lists, as plain functions: the screen
 * draws what comes back and does nothing else, and a test can ask the same
 * questions the reader asks without a phone in the loop.
 *
 * A query is split into words and every word has to be found somewhere on the
 * row — in its name, the line under it, its category, or the labels of its
 * inputs. Every word rather than the whole phrase, because "graph plot" should
 * find Graphing and "gst add" should find the calculator called that, and a
 * reader types words in the order they think of them, not in the order a title
 * happens to be written.
 */
object HubSearch {

    /** The tools shelf, in the order the Tools category shows it. */
    val tools: List<HubTool> = listOf(
        HubTool("reference", "Formula reference", "Every formula the app knows \u2014 maths, physics and chemistry."),
        HubTool("statistics", "Statistics", "Mean, median, SD, quartiles, skewness and regression."),
        HubTool("distributions", "Distributions & tests", "Normal/t/binomial/Poisson, t-tests and confidence intervals."),
        HubTool("complex", "Complex numbers", "Add, subtract, multiply, divide, magnitude, angle and roots of i."),
        HubTool("rpn", "RPN (stack) calculator", "Reverse-Polish entry: push numbers, then apply operations."),
        HubTool("matrix", "Matrix calculator", "Add, subtract, multiply, transpose, determinant and inverse."),
        HubTool("handwriting", "Handwriting", "Write a calculation with a finger and let the app read it."),
        HubTool("solver", "Equation solver", "Linear to quintic, several equations in several unknowns, and inequalities."),
        HubTool("table", "Table of values", "What f(x) and g(x) do across a range, row by row."),
        HubTool("symbolic", "Symbolic algebra", "Simplify, differentiate, integrate, factor and solve polynomials in x."),
        HubTool("limits", "Limits", "The limit of an expression as x approaches a point, one side at a time, or at infinity."),
        HubTool("partial", "Partial fractions", "Split a rational function into simple fractions."),
        HubTool("calculus", "Calculus", "Numerical derivative, second derivative, integral and summation."),
        HubTool("graph", "Graphing", "Plot y = f(x) on a live canvas."),
        HubTool("converter", "Converters", "Roman numerals, words, binary/hex/ASCII text, colour codes, timestamps."),
        HubTool("date", "Date calculator", "Difference, age, working days, week number, add/subtract, timestamps."),
        HubTool("programmer", "Programmer (base & bitwise)", "Base conversion and bitwise operations."),
        HubTool("allformulas", "Every formula", "Every calculator and every reference formula, in one page."),
        HubTool("currency", "Currency converter", "Live exchange rates between world currencies."),
        HubTool("interest", "Interest calculator", "Any rate period and any duration \u2014 daily, weekly, monthly, or two dates."),
    )

    /** How many nearest names the empty state offers before it gives up. */
    const val MAX_SUGGESTIONS = 6

    /**
     * Everything the query finds.
     *
     * [formulas] is the calculators on offer, built-in and the reader's own. A
     * Hindi reader is given the Hindi names to search as well, because the row
     * in front of them is in Hindi: typing the word they can see has to work.
     */
    fun find(
        query: String,
        formulas: List<Formula>,
        hindi: Boolean = false,
        shelf: List<HubTool> = tools,
    ): HubMatches {
        val words = words(query)
        if (words.isEmpty()) return HubMatches()

        val foundTools = shelf.filter { row -> rowText(row, hindi).let { hit -> words.all { it in hit } } }
        val foundFormulas = formulas.filter { f -> formulaText(f, hindi).let { hit -> words.all { it in hit } } }

        // Nothing matched. Rather than end the search on a dead sentence, offer
        // the nearest names: a reader who typed "graf" or "gst add" is one or
        // two letters away, and being told so is the difference between the app
        // knowing its own library and not.
        val near = if (foundTools.isEmpty() && foundFormulas.isEmpty()) {
            nearest(query, formulas, hindi, shelf)
        } else {
            emptyList()
        }
        return HubMatches(foundTools, foundFormulas, near)
    }

    /**
     * The closest names to a query that found nothing.
     *
     * Ranked by how near the name is, not by where it sits in the list, so a
     * near-miss at the bottom of the library outranks a poor one at the top.
     * Only rows that are recognisably the same word are offered: a suggestion
     * that is not close is worse than none, because the reader takes it as the
     * app guessing.
     */
    fun nearest(
        query: String,
        formulas: List<Formula>,
        hindi: Boolean = false,
        shelf: List<HubTool> = tools,
        limit: Int = MAX_SUGGESTIONS,
    ): List<HubHit> {
        val words = words(query)
        if (words.isEmpty() || limit <= 0) return emptyList()

        val scored = ArrayList<Pair<Int, HubHit>>()
        shelf.forEach { t ->
            val score = closeness(words, names(t.title, if (hindi) FormulaHindi.tool(t.id) else null))
            if (score > 0) scored.add(score to HubHit.ToolHit(t))
        }
        formulas.forEach { f ->
            val score = closeness(words, names(f.title, if (hindi) FormulaHindi.title(f.id) else null))
            if (score > 0) scored.add(score to HubHit.CalcHit(f))
        }
        // A stable sort on the score keeps the shelf's own order inside a rank,
        // so the same query always offers the same six.
        return scored.sortedByDescending { it.first }.take(limit).map { it.second }
    }

    // ---- the words a query is made of ----

    /**
     * The query as the words a row has to contain.
     *
     * Punctuation stuck to the ends of a word is dropped — a reader types
     * "graph?" and "2x2," — but only ASCII punctuation: a Devanagari vowel sign
     * is not decoration, and trimming it would break the word it belongs to.
     */
    fun words(query: String): List<String> =
        query.lowercase()
            .split(' ', '\t', '\n')
            .map { word -> word.trim { !it.isLetterOrDigit() && it.code < 128 } }
            .filter { it.isNotEmpty() }

    // ---- what a row can be found by ----

    private fun rowText(tool: HubTool, hindi: Boolean): String {
        val parts = ArrayList<String>(6)
        parts.add(tool.title)
        parts.add(tool.subtitle)
        parts.add(tool.id)
        if (hindi) {
            FormulaHindi.tool(tool.id)?.let { parts.add(it) }
            FormulaHindi.toolSubtitle(tool.id)?.let { parts.add(it) }
        }
        return joined(parts)
    }

    private fun formulaText(f: Formula, hindi: Boolean): String {
        val parts = ArrayList<String>(8 + f.inputs.size)
        parts.add(f.title)
        parts.add(f.description)
        parts.add(f.category)
        // "finance", "interest rate", "radius": the words are on the row or in
        // its fields, so searching them has to find it
        f.inputs.forEach { parts.add(it.label) }
        if (hindi) {
            FormulaHindi.title(f.id)?.let { parts.add(it) }
            FormulaHindi.description(f.id)?.let { parts.add(it) }
            FormulaHindi.category(f.category)?.let { parts.add(it) }
        }
        return joined(parts)
    }

    /**
     * The fields of one row as one lowercased string.
     *
     * Separated by newlines so a word cannot be assembled out of the end of one
     * field and the start of the next: "at" is not on the row "Data" + "base".
     */
    private fun joined(parts: List<String>): String {
        val out = StringBuilder(160)
        for (p in parts) {
            if (p.isEmpty()) continue
            if (out.isNotEmpty()) out.append('\n')
            out.append(p.lowercase())
        }
        return out.toString()
    }

    // ---- how near two names are ----

    /** The spellings of one name worth comparing: English, and Hindi when it is read. */
    private fun names(english: String, hindi: String?): List<String> {
        val out = ArrayList<String>(2)
        out.add(english)
        if (!hindi.isNullOrBlank()) out.add(hindi)
        return out
    }

    /**
     * A name as the words it is written in.
     *
     * Compared word by word rather than as one string, because a name is a
     * phrase: "Programmer (base & bitwise)" has to be near "programmer", and
     * scoring the whole phrase against the query would call it far away. The
     * folding is the same one the reference page uses, so punctuation and case
     * cost nothing and a 2x2 still differs from a 22.
     */
    private fun nameWords(name: String): List<String> =
        name.split(' ', '\t', '\n', '/', '(', ')', '&', '-', '\u2014', ',', '.', ':', ';')
            .map { ToolIndex.key(it) }
            .filter { it.length >= 2 }

    /**
     * How close a query is to a name, as a small rank rather than a distance.
     *
     * The ranks are ordered so the obvious reading wins: the same word beats a
     * word that merely starts with what was typed, which beats one that
     * contains it, which beats one letter out, which beats a word whose first
     * letters are what was typed — "graf" is someone reaching for "graphing"
     * and half the library has no claim on the suggestion. Anything further
     * apart scores nothing at all, because a wrong suggestion reads as the app
     * guessing, which is worse than an honest "nothing".
     */
    fun closeness(words: List<String>, names: List<String>): Int {
        var best = 0
        for (name in names) {
            for (candidate in nameWords(name)) {
                for (word in words) {
                    val q = ToolIndex.key(word)
                    if (q.length < 2) continue
                    val score = rank(q, candidate)
                    if (score > best) best = score
                }
            }
        }
        return best
    }

    /** The rank of one folded query word against one folded word of a name. */
    private fun rank(q: String, candidate: String): Int = when {
        candidate == q -> 5
        candidate.startsWith(q) || q.startsWith(candidate) -> 4
        candidate.contains(q) || q.contains(candidate) -> 3
        else -> {
            val d = distance(q, candidate)
            when {
                d == 1 -> 3
                d == 2 -> 2
                // the same opening letters: "graf" for "graphing"
                commonPrefix(q, candidate) >= 3 && commonPrefix(q, candidate) >= q.length - 1 -> 2
                d * 2 <= maxOf(q.length, candidate.length) -> 1
                else -> 0
            }
        }
    }

    private fun commonPrefix(a: String, b: String): Int {
        val limit = minOf(a.length, b.length)
        var i = 0
        while (i < limit && a[i] == b[i]) i++
        return i
    }

    /**
     * The number of single-letter edits between two words.
     *
     * A plain two-row table, because the words are short and there are a few
     * hundred of them: anything cleverer would be more code than the work it
     * saves. Long words are cut to their first 64 characters so a pasted
     * paragraph cannot turn a keystroke into a second of work.
     */
    fun distance(a: String, b: String): Int {
        val s = a.take(64)
        val t = b.take(64)
        if (s.isEmpty()) return t.length
        if (t.isEmpty()) return s.length
        var previous = IntArray(t.length + 1) { it }
        var current = IntArray(t.length + 1)
        for (i in 1..s.length) {
            current[0] = i
            for (j in 1..t.length) {
                val swap = previous[j - 1] + if (s[i - 1] == t[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, swap)
            }
            val done = previous
            previous = current
            current = done
        }
        return previous[t.length]
    }
}

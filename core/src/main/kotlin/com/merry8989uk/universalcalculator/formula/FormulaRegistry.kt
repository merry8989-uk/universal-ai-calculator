package com.merry8989uk.universalcalculator.formula

/** One line of `formulas.json`, before anything has been decided about it. */
data class RawFormula(
    val category: String,
    val title: String,
    val printed: String,
)

/**
 * Every reference formula, understood well enough to act on.
 *
 * Built once from the bundled data and then read by id. Nothing here decides
 * anything at the moment of calculation — the classification is done up front,
 * so a screen can say "this one has a calculator" before the user taps it, and a
 * test can check every entry at once.
 */
object FormulaRegistry {

    @Volatile
    private var cache: List<FormulaDefinition>? = null

    /** Builds the registry, classifying each entry. Cheap enough to do once. */
    fun build(raw: List<RawFormula>): List<FormulaDefinition> {
        val out = ArrayList<FormulaDefinition>(raw.size)
        val seen = HashMap<String, Int>()
        for (entry in raw) {
            var definition = FormulaClassifier.classify(entry.category, entry.title, entry.printed)
            // Two formulas in different categories can share a title. An id has to
            // be unique, so a repeat gets a number rather than silently replacing
            // the first one.
            val count = seen.merge(definition.id, 1, Int::plus) ?: 1
            if (count > 1) {
                definition = definition.copy(id = definition.id + "_" + count)
            }
            out.add(definition)
        }
        return out
    }

    fun of(raw: List<RawFormula>): List<FormulaDefinition> {
        cache?.let { return it }
        val built = build(raw)
        cache = built
        return built
    }

    fun clearCache() {
        cache = null
    }

    fun byId(definitions: List<FormulaDefinition>, id: String): FormulaDefinition? =
        definitions.firstOrNull { it.id == id }

    /** How many of each kind. The number the audit is actually about. */
    fun counts(definitions: List<FormulaDefinition>): Map<FormulaType, Int> {
        val out = LinkedHashMap<FormulaType, Int>()
        for (type in FormulaType.entries) out[type] = 0
        for (d in definitions) out[d.type] = (out[d.type] ?: 0) + 1
        return out
    }

    /** The ones that can be worked out, for the calculator list. */
    fun calculators(definitions: List<FormulaDefinition>): List<FormulaDefinition> =
        definitions.filter { it.type == FormulaType.DIRECT_CALCULATOR }

    /** The ones that can be checked rather than calculated. */
    fun checkers(definitions: List<FormulaDefinition>): List<FormulaDefinition> =
        definitions.filter { it.type == FormulaType.CHECKER }

    fun inCategory(definitions: List<FormulaDefinition>, category: String): List<FormulaDefinition> =
        definitions.filter { it.category == category }

    /**
     * A short, countable version of a failure reason.
     *
     * The whole reason is read, not just the part before the colon: the useful
     * detail — whether an expression would not run, or ran and gave a different
     * number — is always after it. A histogram that lumps those together says
     * nothing about what to build next.
     */
    private fun reasonKey(reason: String): String = when {
        reason.contains("differ by more than a rounding error") ->
            "the two sides give different numbers (a wrong reading)"
        reason.contains("the left side did not evaluate") ->
            "the left side would not run (a notation the reader cannot handle)"
        reason.contains("the right side did not evaluate") ->
            "the right side would not run (a notation the reader cannot handle)"
        reason.contains("looks like a calculator but does not evaluate") ->
            "looks like a calculator but will not run"
        reason.contains("no equals sign") -> "no equals sign"
        reason.contains("needs a solver") -> "needs a solver"
        reason.contains("no variables") -> "nothing to put numbers into"
        reason.contains("could not be read") -> "the text could not be read"
        reason.contains("one side of the equals sign is empty") -> "one side is empty"
        else -> reason.substringBefore(':').trim().ifEmpty { "other" }
    }

    /** The audit, as text: how many of each kind, and why the rest are not calculators. */
    fun audit(definitions: List<FormulaDefinition>): String = buildString {
        append("Reference formulas: ").append(definitions.size).append('\n')
        val counts = counts(definitions)
        for (type in FormulaType.entries) {
            append("  ").append(type.name.padEnd(18)).append(counts[type] ?: 0).append('\n')
        }
        // The full matrix, not just the calculable count: an engine's state is how
        // many of its formulas are calculators, how many are checkers, and how many
        // are still waiting, and one number cannot say that.
        append('\n').append("By engine (category):\n")
        append("  ").append("engine".padEnd(28))
            .append("total".padStart(6))
            .append("calc".padStart(6))
            .append("check".padStart(7))
            .append("solve".padStart(7))
            .append("ref".padStart(5))
            .append("review".padStart(8))
            .append('\n')
        val byCategory = definitions.groupBy { it.category }
            .entries
            .sortedByDescending { it.value.size }
        for ((category, list) in byCategory) {
            fun n(type: FormulaType) = list.count { it.type == type }
            append("  ").append(category.padEnd(28))
                .append(list.size.toString().padStart(6))
                .append(n(FormulaType.DIRECT_CALCULATOR).toString().padStart(6))
                .append(n(FormulaType.CHECKER).toString().padStart(7))
                .append(n(FormulaType.SOLVER).toString().padStart(7))
                .append(n(FormulaType.REFERENCE_ONLY).toString().padStart(5))
                .append(n(FormulaType.NEEDS_REVIEW).toString().padStart(8))
                .append('\n')
        }
        val review = definitions.filter { it.type == FormulaType.NEEDS_REVIEW }
        if (review.isNotEmpty()) {
            // Why they failed, counted. Without this the next round of work is a
            // guess about which engine to build, and guessing is how a plan turns
            // into a list of things that never get done.
            append('\n').append("Why the ").append(review.size).append(" need review:\n")
            val histogram = review
                .groupingBy { reasonKey(it.reason) }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
            for ((reason, count) in histogram) {
                append("  ").append(count.toString().padStart(4)).append("  ").append(reason).append('\n')
            }
            // The wrong readings are listed by name and by what they were read
            // as, because a count of them is not something anyone can act on.
            // Every one, by position, title, text and reason. A sample is enough
            // to spot a pattern; it is not enough to repair anything, and the
            // repairs are done by hand from this list.
            append('\n').append("Every one that needs review:\n")
            val position = HashMap<String, Int>()
            definitions.forEachIndexed { index, d -> position[d.id] = index + 1 }
            for (d in review) {
                append("  [").append(position[d.id] ?: 0).append("] ")
                    .append(d.category).append(" / ").append(d.title).append('\n')
                    .append("       text: ").append(d.printed.take(160)).append('\n')
                    .append("       why: ").append(d.reason.take(200)).append('\n')
            }
            append('\n').append("A sample:\n")
            for (d in review.take(12)) {
                append("  ").append(d.title).append(" — ").append(d.reason).append('\n')
            }
        }
    }
}

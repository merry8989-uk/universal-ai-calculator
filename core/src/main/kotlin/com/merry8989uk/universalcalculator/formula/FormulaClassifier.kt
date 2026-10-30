package com.merry8989uk.universalcalculator.formula

import com.merry8989uk.universalcalculator.calc.Evaluator

/**
 * Decides what each reference formula can actually do, and proves it.
 *
 * The plan is explicit that a formula parsing is not the same as a formula
 * working, so the classification is not made from the shape of the text alone:
 * a formula is only called a calculator once its expression has been evaluated
 * here, with real numbers, and produced a real answer. Anything that fails that
 * test is downgraded and the reason is kept, which is what makes the audit worth
 * reading.
 */
object FormulaClassifier {

    /** Values used to prove an expression runs. Chosen to avoid 0 and 1. */
    private val PROBES = listOf(2.0, 3.5, 0.5)

    /** A plain identifier: one variable and nothing else. */
    private val SINGLE_VARIABLE = Regex("[A-Za-z][A-Za-z0-9]*")

    /**
     * Classifies an entry, which may hold more than one formula.
     *
     * 117 of the 1,413 entries put a whole topic on one line — "A = a², P = 4a,
     * diagonal = a√2" — and reading that as one formula makes every one of them
     * nonsense. Each part is classified on its own and the best one is used,
     * while the full line is still what the user reads.
     */
    fun classify(category: String, title: String, printed: String): FormulaDefinition {
        val parts = splitParts(printed)
        if (parts.size <= 1) return classifyOne(category, title, printed, null)

        val ranked = parts
            .map { classifyOne(category, title, it, printed) }
            .sortedByDescending { rank(it.type) }
        val best = ranked.first()
        return if (best.type == FormulaType.DIRECT_CALCULATOR || best.type == FormulaType.CHECKER) {
            best.copy(
                reason = "of " + parts.size + " formulas on this line, this one works: " + best.reason,
            )
        } else {
            classifyOne(category, title, printed, null)
        }
    }

    /** Which outcome is more useful to the reader. */
    private fun rank(type: FormulaType): Int = when (type) {
        FormulaType.DIRECT_CALCULATOR -> 5
        FormulaType.CHECKER -> 4
        FormulaType.SOLVER -> 3
        FormulaType.REFERENCE_ONLY -> 2
        FormulaType.CONVERTER -> 1
        FormulaType.NEEDS_REVIEW -> 0
    }

    /**
     * Splits a line into its formulas.
     *
     * Only commas and semicolons outside brackets count — a comma inside a pair
     * of brackets belongs to that expression, and splitting on it would cut a
     * formula in half.
     */
    fun splitParts(printed: String): List<String> {
        val parts = ArrayList<String>()
        val current = StringBuilder()
        var depth = 0
        for (c in printed) {
            when {
                c == '(' || c == '[' || c == '{' -> {
                    depth++
                    current.append(c)
                }
                c == ')' || c == ']' || c == '}' -> {
                    depth--
                    current.append(c)
                }
                (c == ',' || c == ';') && depth == 0 -> {
                    val piece = current.toString().trim()
                    if (piece.isNotEmpty()) parts.add(piece)
                    current.setLength(0)
                }
                else -> current.append(c)
            }
        }
        val piece = current.toString().trim()
        if (piece.isNotEmpty()) parts.add(piece)
        return parts
    }

    private fun classifyOne(
        category: String,
        title: String,
        printed: String,
        whole: String?,
    ): FormulaDefinition {
        val id = idFor(category, title)
        val output = OutputSpec(label = title, precision = 8)
        // When the entry holds several formulas, the one being computed is kept
        // so the working can show it and the checker can split it on the equals
        // sign without picking up the rest of the line.
        val part = if (whole != null && printed != whole) printed else null

        fun reference(reason: String) = FormulaDefinition(
            id = id, title = title, category = category, printed = printed,
            expression = null, target = null, type = FormulaType.REFERENCE_ONLY,
            inputs = emptyList(), output = output, reason = reason,
        )

        if (!printed.contains('=')) {
            return reference("no equals sign, so there is nothing to compute")
        }

        FormulaNormaliser.containsStructure(printed)?.let {
            return FormulaDefinition(
                id = id, title = title, category = category, printed = printed,
                expression = null, target = null, type = FormulaType.SOLVER,
                inputs = emptyList(), output = output,
                reason = "contains \"$it\", which needs a solver rather than a formula",
            )
        }

        val lhsPrinted = printed.substringBefore('=').trim()
        val rhsPrinted = printed.substringAfter('=').trim()
        if (lhsPrinted.isEmpty() || rhsPrinted.isEmpty()) {
            return reference("one side of the equals sign is empty")
        }

        val lhs = FormulaNormaliser.normalise(lhsPrinted)
            ?: return reference("the left side could not be read")
        val rhs = FormulaNormaliser.normalise(rhsPrinted)
            ?: return reference("the right side could not be read")

        val lhsVars = FormulaNormaliser.variables(lhs)
        val rhsVars = FormulaNormaliser.variables(rhs)

        // ---- one variable on the left: this is a calculator ----
        if (SINGLE_VARIABLE.matches(lhs) && lhs !in rhsVars && rhsVars.isNotEmpty()) {
            val problem = prove(rhs, rhsVars)
            if (problem == null) {
                return FormulaDefinition(
                    id = id, title = title, category = category,
                    printed = whole ?: printed,
                    expression = rhs, target = lhs, type = FormulaType.DIRECT_CALCULATOR,
                    inputs = rhsVars.mapIndexed { index, name -> inputFor(name, index) },
                    output = OutputSpec(label = FormulaNormaliser.friendlyName(lhs), precision = 8),
                    part = part,
                    reason = "solves for ${FormulaNormaliser.friendlyName(lhs)} and evaluates",
                )
            }
            return FormulaDefinition(
                id = id, title = title, category = category, printed = printed,
                expression = null, target = null, type = FormulaType.NEEDS_REVIEW,
                inputs = emptyList(), output = output,
                reason = "looks like a calculator but does not evaluate: $problem",
            )
        }

        // ---- variables on both sides: an identity to check ----
        if (lhsVars.isNotEmpty() && rhsVars.isNotEmpty()) {
            val all = (lhsVars + rhsVars).distinct()
            val problem = proveIdentity(lhs, rhs, all, lhsPrinted, rhsPrinted)
            if (problem == null) {
                return FormulaDefinition(
                    id = id, title = title, category = category,
                    printed = whole ?: printed,
                    expression = null, target = null, type = FormulaType.CHECKER,
                    inputs = all.mapIndexed { index, name -> inputFor(name, index) },
                    output = OutputSpec(label = "Both sides"),
                    part = part,
                    reason = "an identity: both sides agree for the values tried",
                )
            }
            return FormulaDefinition(
                id = id, title = title, category = category, printed = printed,
                expression = null, target = null, type = FormulaType.NEEDS_REVIEW,
                inputs = emptyList(), output = output,
                reason = "both sides hold variables but they do not agree: $problem",
            )
        }

        return reference("no variables to put numbers into")
    }

    /** Runs the expression with sample values. Returns null when it works. */
    private fun prove(expression: String, variables: List<String>): String? {
        for (probe in PROBES) {
            val vars = variables.associateWith { probe }
            val value = try {
                Evaluator.evaluate(expression, vars = vars)
            } catch (e: Exception) {
                return "it did not evaluate (${e.javaClass.simpleName})"
            }
            if (value.isNaN()) return "it gave no number"
            if (value.isInfinite()) return "it gave an infinite answer"
        }
        return null
    }

    /** Evaluates both sides and compares them. Returns null when they agree. */
    private fun proveIdentity(
        lhs: String,
        rhs: String,
        variables: List<String>,
        printedLhs: String,
        printedRhs: String,
    ): String? {
        for (probe in PROBES) {
            val vars = variables.associateWith { probe }
            val a = try {
                Evaluator.evaluate(lhs, vars = vars)
            } catch (e: Exception) {
                return "the left side did not evaluate"
            }
            val b = try {
                Evaluator.evaluate(rhs, vars = vars)
            } catch (e: Exception) {
                return "the right side did not evaluate"
            }
            if (a.isNaN() || b.isNaN()) return "one side gave no number"
            val scale = maxOf(1.0, kotlin.math.abs(a), kotlin.math.abs(b))
            if (kotlin.math.abs(a - b) / scale > 1e-6) {
                // The normalised forms are carried in the reason on purpose: a
                // wrong reading is the one failure that gives a confident wrong
                // answer, so it has to be findable by name rather than counted.
                return "the two sides differ by more than a rounding error: " +
                    "read \"$printedLhs\" as [$lhs] and \"$printedRhs\" as [$rhs]"
            }
        }
        return null
    }

    /**
     * A starting value for an input.
     *
     * Different for each variable on purpose. Filling every box with 1 makes a
     * whole class of formula collapse: (i-1)/(n-1) is 0/0 when both are 1, and
     * that says nothing about whether the formula works.
     */
    private fun inputFor(variable: String, index: Int) = InputSpec(
        id = variable,
        label = FormulaNormaliser.friendlyName(variable),
        sample = 2.0 + index,
    )

    /**
     * A stable id from the category and title.
     *
     * Stable matters: the manifest, the tests and any saved calculation all refer
     * to a formula by this, so it must not depend on position in the file.
     */
    fun idFor(category: String, title: String): String =
        slug(category) + "." + slug(title)

    fun slug(text: String): String = text
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifEmpty { "untitled" }
}

package com.merry8989uk.universalcalculator.formula

/**
 * What a reference formula can actually do.
 *
 * The distinction matters more than the count: a formula that can be computed
 * and a formula that can only be read are different products, and pretending
 * otherwise is how a formula library fills up with buttons that do nothing.
 */
enum class FormulaType {
    /** One variable on the left, an expression of the others on the right. */
    DIRECT_CALCULATOR,

    /** Needs an equation solved or a numerical method run. */
    SOLVER,

    /** An identity or a theorem: given values, both sides should agree. */
    CHECKER,

    /** Converts between representations. */
    CONVERTER,

    /** Nothing to compute: a definition, a table, a rule. */
    REFERENCE_ONLY,

    /** The expression is not understood well enough to be sure. */
    NEEDS_REVIEW,
}

/** One thing the user types in. */
data class InputSpec(
    val id: String,
    val label: String,
    /** A dimension such as "length" or "mass", or null when it is a pure number. */
    val unit: String? = null,
    val required: Boolean = true,
    /** What to put in the box to show the formula working. */
    val sample: Double = 1.0,
)

/** What comes out. */
data class OutputSpec(
    val label: String,
    val unit: String? = null,
    val precision: Int = 8,
)

/**
 * A reference formula, understood well enough to act on.
 *
 * [printed] is what the book says and is never altered — it is what the user
 * reads. [expression] is the same thing in the syntax the evaluator speaks, and
 * is null when the formula cannot be computed. Keeping both means the reference
 * screen can go on showing the real formula while the calculator works from the
 * normalised one.
 */
data class FormulaDefinition(
    val id: String,
    val title: String,
    val category: String,
    val printed: String,
    val expression: String?,
    /** The variable being solved for, for a direct calculator. */
    val target: String?,
    val type: FormulaType,
    val inputs: List<InputSpec>,
    val output: OutputSpec,
    val validation: List<String> = emptyList(),
    /** Why it was classified this way, in words, for the audit. */
    val reason: String = "",
)

/** A calculation to run. */
data class CalculationRequest(
    val formulaId: String,
    val inputs: Map<String, Double>,
)

/** One line of working. */
data class CalculationStep(
    val description: String,
    val expression: String,
    val result: String,
)

/**
 * What comes back.
 *
 * Deliberately not just a number: the exact form and the decimal are kept apart,
 * because "1/3" and "0.33333333" are different answers to a mathematician, and
 * the steps are the part a student learns from.
 */
data class CalculationResult(
    val formulaId: String,
    val success: Boolean,
    val exactValue: String? = null,
    val approximateValue: Double? = null,
    val outputUnit: String? = null,
    val steps: List<CalculationStep> = emptyList(),
    val warnings: List<String> = emptyList(),
    val error: String? = null,
) {
    companion object {
        fun failed(formulaId: String, error: String) =
            CalculationResult(formulaId = formulaId, success = false, error = error)
    }
}

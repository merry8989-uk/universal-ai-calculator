package com.merry8989uk.universalcalculator.calc

/** One input of a user-made calculator: a name the expression can use. */
data class CustomInput(
    val key: String,
    val label: String,
    val default: Double = 0.0,
)

/**
 * A calculator the user built themselves: a name, some inputs, and an
 * expression that ties them together.
 *
 * These live on the device only. The expression is evaluated by the same
 * [Evaluator] the rest of the app uses, so a custom calculator behaves exactly
 * like a built-in one.
 */
data class CustomCalculator(
    val id: String,
    val name: String,
    val description: String = "",
    val inputs: List<CustomInput> = emptyList(),
    val expression: String,
    val unit: String = "",
) {

    /** Where these appear in the Formulas hub. */
    val category: String get() = CATEGORY

    /**
     * Null when the calculator is usable, otherwise a message to show the user.
     * The expression is actually run, so a typo is caught here rather than when
     * the user taps Compute.
     */
    fun problem(): String? {
        if (name.isBlank()) return "Give the calculator a name."
        if (expression.isBlank()) return "The formula is empty."

        val bad = inputs.firstOrNull { !isValidKey(it.key) }
        if (bad != null) {
            return "\"${bad.key}\" is not a usable input name. Use letters, digits and " +
                "underscores, and do not start with a digit."
        }
        val duplicate = inputs.groupBy { it.key }.entries.firstOrNull { it.value.size > 1 }
        if (duplicate != null) return "The input \"${duplicate.key}\" is defined twice."
        val blankLabel = inputs.firstOrNull { it.label.isBlank() }
        if (blankLabel != null) return "Every input needs a label."

        return try {
            Evaluator.evaluate(expression, vars = defaults())
            null
        } catch (e: Exception) {
            "The formula could not be worked out: ${e.message}"
        }
    }

    /** The inputs at their default values. */
    fun defaults(): Map<String, Double> = inputs.associate { it.key to it.default }

    /** Works out the result for a set of input values. */
    fun compute(values: Map<String, Double>, degrees: Boolean = false): Double =
        Evaluator.evaluate(expression, degrees = degrees, vars = values)

    /** The same thing as a built-in, so one UI renders both. */
    fun toFormula(degrees: Boolean = false): Formula = Formula(
        id = "custom_" + id,
        title = name,
        category = category,
        description = description.ifBlank { expression },
        inputs = inputs.map { InputSpec(it.key, it.label, it.default) },
        compute = { m ->
            listOf(OutputSpec(name.ifBlank { "Result" }, compute(m, degrees), unit))
        },
    )

    companion object {
        const val CATEGORY = "My calculators"

        /** Identifiers an expression can reference. */
        fun isValidKey(key: String): Boolean =
            key.isNotBlank() &&
                !key.first().isDigit() &&
                key.all { it.isLetterOrDigit() || it == '_' } &&
                key !in RESERVED

        /** Names the evaluator already understands; they cannot be inputs. */
        private val RESERVED = setOf(
            "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh",
            "ln", "log", "sqrt", "cbrt", "abs", "round", "floor", "ceil", "exp",
            "pi", "e", "ans", "ncr", "npr", "gcd", "lcm", "min", "max", "mod",
        )
    }
}

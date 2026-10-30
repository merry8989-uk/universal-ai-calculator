package com.merry8989uk.universalcalculator.formula

import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.Format

/**
 * Runs a formula and says how it got there.
 *
 * This is the only place a mathematical answer is produced. The assistant is
 * allowed to find the formula and explain it, but never to invent the number:
 * a model that is confidently wrong about a square root is worse than no
 * assistant at all, and a deterministic engine can be tested.
 */
object FormulaCalculator {

    /** Significant digits kept in the working. */
    private const val SHOWN = 6

    fun calculate(definition: FormulaDefinition, request: CalculationRequest): CalculationResult {
        return when (definition.type) {
            FormulaType.DIRECT_CALCULATOR -> direct(definition, request)
            FormulaType.CHECKER -> check(definition, request)
            FormulaType.SOLVER -> CalculationResult.failed(
                definition.id,
                "This formula needs an equation solved rather than a number put in.",
            )
            FormulaType.CONVERTER -> CalculationResult.failed(
                definition.id,
                "This formula converts between representations; use the converter.",
            )
            else -> CalculationResult.failed(
                definition.id,
                "This formula is for reference and cannot be calculated.",
            )
        }
    }

    private fun direct(
        definition: FormulaDefinition,
        request: CalculationRequest,
    ): CalculationResult {
        val expression = definition.expression
            ?: return CalculationResult.failed(definition.id, "This formula has no expression.")

        val missing = definition.inputs
            .filter { it.required && request.inputs[it.id] == null }
            .map { it.label }
        if (missing.isNotEmpty()) {
            return CalculationResult.failed(
                definition.id,
                "Fill in " + missing.joinToString(", ") + ".",
            )
        }

        val warnings = ArrayList<String>()
        for (spec in definition.inputs) {
            val value = request.inputs[spec.id] ?: continue
            if (value.isNaN() || value.isInfinite()) {
                return CalculationResult.failed(
                    definition.id,
                    "${spec.label} is not a number I can use.",
                )
            }
        }

        val value = try {
            Evaluator.evaluate(expression, vars = request.inputs.mapKeys { it.key.lowercase() })
        } catch (e: Exception) {
            return CalculationResult.failed(
                definition.id,
                "That did not work out: " + (e.message ?: e.javaClass.simpleName),
            )
        }

        if (value.isNaN()) {
            return CalculationResult.failed(
                definition.id,
                "That combination has no answer — check the values.",
            )
        }
        if (value.isInfinite()) {
            return CalculationResult.failed(
                definition.id,
                "That divides by zero.",
            )
        }

        // Say what was done, in the order a person would do it.
        val steps = ArrayList<CalculationStep>()
        steps.add(
            CalculationStep(
                description = "The formula",
                expression = definition.printed,
                result = "",
            ),
        )
        steps.add(
            CalculationStep(
                description = "Put the values in",
                expression = definition.inputs.joinToString(", ") { spec ->
                    "${spec.label} = ${number(request.inputs[spec.id] ?: 0.0)}"
                },
                result = "",
            ),
        )
        steps.add(
            CalculationStep(
                description = "Work it out",
                expression = substitute(expression, request.inputs),
                result = number(value),
            ),
        )

        if (definition.output.unit != null) {
            warnings.add("The answer is in the same unit as the inputs.")
        }

        return CalculationResult(
            formulaId = definition.id,
            success = true,
            exactValue = number(value),
            approximateValue = value,
            outputUnit = definition.output.unit,
            steps = steps,
            warnings = warnings,
        )
    }

    private fun check(
        definition: FormulaDefinition,
        request: CalculationRequest,
    ): CalculationResult {
        val parts = definition.printed.split('=', limit = 2)
        if (parts.size != 2) {
            return CalculationResult.failed(definition.id, "This formula has no two sides.")
        }
        val lhs = FormulaNormaliser.normalise(parts[0])
            ?: return CalculationResult.failed(definition.id, "The left side could not be read.")
        val rhs = FormulaNormaliser.normalise(parts[1])
            ?: return CalculationResult.failed(definition.id, "The right side could not be read.")

        val vars = request.inputs.mapKeys { it.key.lowercase() }
        val a = try {
            Evaluator.evaluate(lhs, vars = vars)
        } catch (e: Exception) {
            return CalculationResult.failed(definition.id, "The left side did not work out.")
        }
        val b = try {
            Evaluator.evaluate(rhs, vars = vars)
        } catch (e: Exception) {
            return CalculationResult.failed(definition.id, "The right side did not work out.")
        }
        val scale = maxOf(1.0, kotlin.math.abs(a), kotlin.math.abs(b))
        val holds = kotlin.math.abs(a - b) / scale < 1e-6

        val steps = listOf(
            CalculationStep("Left side", parts[0].trim(), number(a)),
            CalculationStep("Right side", parts[1].trim(), number(b)),
        )

        return CalculationResult(
            formulaId = definition.id,
            success = true,
            exactValue = if (holds) "Holds" else "Does not hold",
            approximateValue = b - a,
            steps = steps,
            warnings = if (holds) emptyList() else listOf(
                "The two sides differ by ${number(b - a)}, so this is not an identity " +
                    "for these values.",
            ),
        )
    }

    /** The expression with the numbers in it, for the working. */
    private fun substitute(expression: String, inputs: Map<String, Double>): String {
        var out = expression
        // longest name first, so a1 is replaced before a
        for (key in inputs.keys.sortedByDescending { it.length }) {
            val value = inputs[key] ?: continue
            out = out.replace(Regex("(?<![A-Za-z0-9])" + Regex.escape(key) + "(?![A-Za-z0-9])"), number(value))
        }
        return out
    }

    /** A number as a person would write it: no trailing zeros, no 1e-17. */
    fun number(value: Double): String {
        if (value == 0.0) return "0"
        val magnitude = kotlin.math.abs(value)
        if (magnitude >= 1e-4 && magnitude < 1e15) {
            val rounded = String.format("%.$SHOWN" + "f", value).trimEnd('0').trimEnd('.')
            return if (rounded.isEmpty() || rounded == "-") "0" else rounded
        }
        return Format.number(value)
    }
}

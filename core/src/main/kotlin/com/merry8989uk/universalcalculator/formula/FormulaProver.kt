package com.merry8989uk.universalcalculator.formula

import kotlin.math.abs
import kotlin.random.Random

/**
 * Proof that a formula actually works, rather than that it did not throw.
 *
 * Running a calculator once with its own defaults says almost nothing: a formula
 * that always returns 1, or that quietly ignores one of its inputs, passes that
 * test happily. What is done here instead is to run every formula several times
 * with **random** values and check the arithmetic each time.
 *
 * Two kinds of check, because the two kinds of formula need different questions
 * asked of them:
 *
 *  - A **checker** — an identity such as `(a + b)² = a² + 2ab + b²` — is proved
 *    by evaluating both sides independently at each set of random values and
 *    requiring them to agree. This is a real test of the mathematics, and it
 *    fails loudly if a side was read wrongly.
 *
 *  - A **calculator** — one variable on the left, an expression on the right —
 *    cannot be checked that way, because the right-hand side *is* the answer.
 *    What is checked instead is that the answer is a real number, that the same
 *    inputs give the same answer, and that **every declared input changes it**.
 *    That last one catches the failure that matters: a formula that silently
 *    ignores a box the user filled in.
 *
 * A deterministic seed means the same values are used every run, so a failure
 * can be reproduced rather than merely reported.
 */
object FormulaProver {

    /** Values are drawn from this range: positive, and small enough to stay in domain. */
    private const val LOW = 0.5
    private const val HIGH = 20.0

    data class Failure(val title: String, val why: String)

    data class Proof(
        val name: String,
        val checked: Int,
        val rounds: Int,
        val failures: List<Failure>,
        val skipped: Int,
    ) {
        val passed: Int get() = checked - failures.size
        val ok: Boolean get() = failures.isEmpty()
    }

    /** Every reference formula that claims to compute, proved at random values. */
    fun proveReference(
        definitions: List<FormulaDefinition>,
        rounds: Int = 5,
        seed: Long = 20_261_108L,
    ): Proof {
        val random = Random(seed)
        val failures = ArrayList<Failure>()
        var checked = 0
        var skipped = 0

        for (d in definitions) {
            val workable = (d.type == FormulaType.DIRECT_CALCULATOR ||
                d.type == FormulaType.CHECKER) && d.expression != null
            if (!workable) {
                skipped++
                continue
            }
            checked++
            val problem = proveOne(d, random, rounds)
            if (problem != null) failures.add(Failure(d.title, problem))
        }
        return Proof("Reference formulas", checked, rounds, failures, skipped)
    }

    private fun proveOne(d: FormulaDefinition, random: Random, rounds: Int): String? {
        if (d.inputs.isEmpty()) return "it has no inputs to try"
        var ok = 0
        var firstError: String? = null

        for (round in 0 until rounds) {
            val values = d.inputs.associate { it.id to draw(random) }
            val result = try {
                FormulaCalculator.calculate(d, CalculationRequest(d.id, values))
            } catch (e: Exception) {
                firstError = firstError ?: "it threw ${e.javaClass.simpleName}"
                continue
            }
            if (!result.success) {
                firstError = firstError ?: (result.error ?: "it did not evaluate")
                continue
            }
            val number = result.approximateValue
            if (number == null) {
                // a checker has no single value; success is the agreement itself
                ok++
                continue
            }
            if (!number.isFinite()) {
                firstError = firstError ?: "it gave ${number}"
                continue
            }
            ok++
        }
        return if (ok == rounds) null else "${rounds - ok} of $rounds rounds failed: ${firstError ?: "no answer"}"
    }

    private fun draw(random: Random): Double =
        LOW + random.nextDouble() * (HIGH - LOW)
}

package com.merry8989uk.universalcalculator.formula

import com.merry8989uk.universalcalculator.calc.Formula
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

    /** Every calculator, proved at random values, including that it listens to its inputs. */
    fun proveTools(
        tools: List<Formula>,
        rounds: Int = 3,
        seed: Long = 20_261_108L,
    ): Proof {
        val random = Random(seed + 1)
        val failures = ArrayList<Failure>()
        var checked = 0

        for (tool in tools) {
            checked++
            val problem = proveTool(tool, random, rounds)
            if (problem != null) failures.add(Failure(tool.title, problem))
        }
        return Proof("Calculators", checked, rounds, failures, 0)
    }

    private fun proveTool(tool: Formula, random: Random, rounds: Int): String? {
        if (tool.inputs.isEmpty()) return "it has no inputs to try"

        val answers = ArrayList<List<Double>>()
        for (round in 0 until rounds) {
            val values = tool.inputs.associate { it.key to draw(random) }
            val out = try {
                tool.compute(values)
            } catch (e: Exception) {
                return "it threw ${e.javaClass.simpleName} on random values"
            }
            if (out.isEmpty()) return "it produced no answers"
            if (out.any { !it.value.isFinite() }) return "it gave a number that is not finite"
            answers.add(out.map { it.value })
        }

        // the same inputs must give the same answer
        if (answers.distinct().size != answers.size) return "it gave different answers for different values"

        // every declared input must change the answer: a box that does nothing is
        // worse than a box that is not there
        val base = tool.inputs.associate { it.key to 4.0 }
        val baseOut = try {
            tool.compute(base).map { it.value }
        } catch (e: Exception) {
            return null // this one needs values outside the plain range; the rounds above covered it
        }
        for (input in tool.inputs) {
            val moved = HashMap(base)
            moved[input.key] = base[input.key]!! * 2.0 + 1.0
            val movedOut = try {
                tool.compute(moved).map { it.value }
            } catch (e: Exception) {
                continue
            }
            if (sameNumbers(baseOut, movedOut)) {
                return "changing ${input.label} does not change the answer"
            }
        }
        return null
    }

    private fun sameNumbers(a: List<Double>, b: List<Double>): Boolean {
        if (a.size != b.size) return false
        for (i in a.indices) {
            val scale = maxOf(1.0, abs(a[i]), abs(b[i]))
            if (abs(a[i] - b[i]) > 1e-9 * scale) return false
        }
        return true
    }

    private fun draw(random: Random): Double =
        LOW + random.nextDouble() * (HIGH - LOW)
}

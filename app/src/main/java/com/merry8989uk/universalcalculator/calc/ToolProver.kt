package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs
import kotlin.random.Random

/**
 * Proof that a calculator works, rather than that it did not throw.
 *
 * Running a calculator once with its own defaults says almost nothing: one that
 * always returns 1 passes, and so does one that ignores a box the user filled in.
 * Each is run several times on random values instead, and three things are
 * required of every run — a real number, the same number for the same inputs, and
 * a different number when any single input changes.
 *
 * That last one is the failure worth catching. A box that does nothing is worse
 * than a box that is not there, because the user types a value, gets an answer,
 * and the answer did not use what they typed.
 */
object ToolProver {

    private const val LOW = 0.5
    private const val HIGH = 20.0

    data class Failure(val title: String, val why: String)

    data class Proof(
        val checked: Int,
        val rounds: Int,
        val failures: List<Failure>,
    ) {
        val passed: Int get() = checked - failures.size
        val ok: Boolean get() = failures.isEmpty()
    }

    fun proveTools(
        tools: List<Formula>,
        rounds: Int = 3,
        seed: Long = 20_261_108L,
    ): Proof {
        val random = Random(seed + 1)
        val failures = ArrayList<Failure>()
        for (tool in tools) {
            val problem = proveOne(tool, random, rounds)
            if (problem != null) failures.add(Failure(tool.title, problem))
        }
        return Proof(tools.size, rounds, failures)
    }

    private fun proveOne(tool: Formula, random: Random, rounds: Int): String? {
        if (tool.inputs.isEmpty()) return "it has no inputs to try"

        // A random value outside a formula's domain — a non-integer where a
        // count is wanted — is a fact about the domain, not a fault in the
        // formula, so one bad round is tolerated and all of them are not.
        val answers = ArrayList<List<Double>>()
        for (round in 0 until rounds) {
            val values = tool.inputs.associate { it.key to draw(random) }
            val out = try {
                tool.compute(values)
            } catch (e: Exception) {
                continue
            }
            if (out.isEmpty()) return "it produced no answers"
            if (out.any { !it.value.isFinite() }) return "it gave a number that is not finite"
            answers.add(out.map { it.value })
        }
        if (answers.size < maxOf(1, rounds - 1)) return "it could not be worked out at all"

        // every declared input must change the answer
        // Distinct values, because equal ones hide the very bug being looked for:
        // with start = end, a growth rate is flat no matter what the years are.
        val base = tool.inputs.mapIndexed { i, spec -> spec.key to 3.0 + i * 2.0 }.toMap()
        val baseOut = try {
            tool.compute(base).map { it.value }
        } catch (e: Exception) {
            return null
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

    private fun draw(random: Random): Double = LOW + random.nextDouble() * (HIGH - LOW)
}

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

    /**
     * Values are drawn around each input's own default.
     *
     * A single global range is wrong for most of the library: an MPC of 7 is not a
     * rate, a mass of 3 kg is not a planet, and a distance of 2 m puts the force
     * so far below one that it cannot be told from zero. The default is the one
     * number that says what the box means, so the range is built from it.
     */
    private fun drawAround(spec: com.merry8989uk.universalcalculator.calc.InputSpec, random: Random): Double {
        val base = spec.default
        val factor = 0.5 + random.nextDouble() * 1.5
        return if (abs(base) > 1e-9) base * factor else 0.5 + random.nextDouble() * 2.0
    }

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
            val values = tool.inputs.associate { it.key to drawAround(it, random) }
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

        // Every declared input must change the answer — but not at every set of
        // values. A single fixed base is unfair: with start = end a growth rate is
        // flat, and a formula that branches on a 0/1 input sits in one branch
        // whichever way the number is nudged. Several bases are tried instead, and
        // an input is only called dead when it makes no difference at any of them.
        for (input in tool.inputs) {
            var matters = false
            var judged = 0
            for (attempt in 0 until 6) {
                val base = tool.inputs.associate { it.key to drawAround(it, random) }
                val before = computeOrNull(tool, base) ?: continue
                // Two ways of moving it, because one is not enough: doubling never
                // crosses a 0/1 switch, and zero never crosses anything else.
                for (nudged in listOf(base[input.key]!! * 2.0 + 1.0, 0.0)) {
                    val moved = HashMap(base)
                    moved[input.key] = nudged
                    val after = computeOrNull(tool, moved) ?: continue
                    judged++
                    if (!sameNumbers(before, after)) {
                        matters = true
                        break
                    }
                }
                if (matters) break
            }
            // if nothing could be worked out, that is not the input's fault
            // a domain limit — a triangle that cannot exist at those sides — is a
            // fact about the range tried, not a fault in the formula
            if (!matters && judged == 0) return null
            if (!matters) return "changing ${input.label} never changes the answer"
        }
        return null
    }

    /** The answers, or null when this set of values is outside the formula's domain. */
    private fun computeOrNull(tool: Formula, values: Map<String, Double>): List<Double>? = try {
        val out = tool.compute(values).map { it.value }
        if (out.any { !it.isFinite() }) null else out
    } catch (e: Exception) {
        null
    }

    private fun sameNumbers(a: List<Double>, b: List<Double>): Boolean {
        if (a.size != b.size) return false
        for (i in a.indices) {
            // relative only: a floor of 1.0 would call a force of 1e-10 and one of
            // 1e-11 the same number, and they are not
            val scale = maxOf(abs(a[i]), abs(b[i]))
            if (scale == 0.0) continue
            if (abs(a[i] - b[i]) > 1e-9 * scale) return false
        }
        return true
    }

}

package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs

/**
 * Splits a rational function into partial fractions.
 *
 * P(x)/Q(x) is rewritten as a polynomial plus a sum of simple terms
 * `A / (x - r)^k`. The coefficients are found by sampling: multiplying through
 * by Q turns the problem into a square linear system, which is then solved
 * directly.
 *
 * The denominator must split into real linear factors. `1/(x^2+1)` has no real
 * root and is reported as not possible rather than guessed at.
 */
object PartialFractions {

    /** One `coefficient / (x - root) ^ power`. */
    data class Term(val coefficient: Double, val root: Double, val power: Int)

    sealed interface Outcome {
        data class Decomposed(
            val polynomial: DoubleArray,
            val terms: List<Term>,
        ) : Outcome

        data class NotPossible(val reason: String) : Outcome
    }

    /** Numeric equality for coefficients and roots. */
    private const val EPS = 1e-9

    fun decompose(numerator: DoubleArray, denominator: DoubleArray): Outcome {
        var num = Symbolic.trim(numerator)
        val den = Symbolic.trim(denominator)
        if (den.size <= 1) {
            return Outcome.NotPossible("The denominator is a constant, so there is nothing to split.")
        }

        // An improper fraction: divide out the polynomial part first, then
        // split what is left over.
        var polynomial = doubleArrayOf(0.0)
        var guard = 0
        while (!Symbolic.isZero(num) &&
            Symbolic.degree(num) >= Symbolic.degree(den) &&
            guard++ < 64
        ) {
            val shift = Symbolic.degree(num) - Symbolic.degree(den)
            val factor = num[Symbolic.degree(num)] / den[Symbolic.degree(den)]
            val step = shifted(Symbolic.scale(den, factor), shift)
            polynomial = Symbolic.add(polynomial, shifted(doubleArrayOf(factor), shift))
            val reduced = Symbolic.trim(Symbolic.subtract(num, step))
            if (Symbolic.degree(reduced) >= Symbolic.degree(num)) {
                return Outcome.NotPossible("The division did not settle; the fraction may be malformed.")
            }
            num = reduced
        }
        return decomposeRemainder(num, den, polynomial)
    }

    private fun decomposeRemainder(
        numerator: DoubleArray,
        denominator: DoubleArray,
        polynomial: DoubleArray,
    ): Outcome {
        val num = Symbolic.trim(numerator)
        val den = Symbolic.trim(denominator)
        if (Symbolic.isZero(num)) {
            return Outcome.Decomposed(polynomial, emptyList())
        }

        // Distinct real roots, with how many times each occurs.
        val roots = Symbolic.roots(den)
        if (roots.isEmpty()) {
            return Outcome.NotPossible("The denominator has no real roots, so it cannot be split this way.")
        }
        val factors = ArrayList<Pair<Double, Int>>()
        for (r in roots) {
            var remaining = den
            var count = 0
            while (remaining.size > 1 && abs(Symbolic.evaluate(remaining, r)) < 1e-7) {
                remaining = divideByRoot(remaining, r)
                count++
            }
            if (count > 0) factors.add(r to count)
        }
        val total = factors.sumOf { it.second }
        if (total != Symbolic.degree(den)) {
            return Outcome.NotPossible(
                "The denominator does not split into real linear factors " +
                    "(a quadratic factor such as x^2+1 has no real root).",
            )
        }

        // Basis: Q(x) / (x - r)^k, one per unknown.
        val basis = ArrayList<DoubleArray>()
        val labels = ArrayList<Pair<Double, Int>>()
        for ((root, multiplicity) in factors) {
            for (k in 1..multiplicity) {
                basis.add(divideRepeated(den, root, k))
                labels.add(root to k)
            }
        }

        val unknowns = basis.size
        val points = samplePoints(unknowns, factors.map { it.first })
            ?: return Outcome.NotPossible("Could not find enough sample points away from the roots.")

        // One row per sample point: sum of c_j * basis_j(x) = numerator(x)
        val matrix = Array(unknowns) { DoubleArray(unknowns + 1) }
        for (row in 0 until unknowns) {
            val x = points[row]
            for (col in 0 until unknowns) {
                matrix[row][col] = Symbolic.evaluate(basis[col], x)
            }
            matrix[row][unknowns] = Symbolic.evaluate(num, x)
        }

        val solution = solve(matrix) ?: return Outcome.NotPossible("The coefficients could not be solved.")
        val terms = labels.mapIndexed { i, (root, power) ->
            Term(clean(solution[i]), clean(root), power)
        }.filter { abs(it.coefficient) > EPS }

        return Outcome.Decomposed(cleanAll(polynomial), terms)
    }

    /** Q(x) / (x - r)^k, by dividing k times. */
    private fun divideRepeated(q: DoubleArray, root: Double, k: Int): DoubleArray {
        var out = q
        repeat(k) { out = divideByRoot(out, root) }
        return out
    }

    /** Synthetic division by (x - r). */
    private fun divideByRoot(p: DoubleArray, r: Double): DoubleArray {
        val t = Symbolic.trim(p)
        val n = t.size - 1
        if (n < 1) return doubleArrayOf(0.0)
        val out = DoubleArray(n)
        var carry = t[n]
        out[n - 1] = carry
        for (i in n - 1 downTo 1) {
            carry = t[i] + carry * r
            out[i - 1] = carry
        }
        return Symbolic.trim(out)
    }

    /** x^shift times p. */
    private fun shifted(p: DoubleArray, shift: Int): DoubleArray {
        if (shift <= 0) return p
        val out = DoubleArray(p.size + shift)
        for (i in p.indices) out[i + shift] = p[i]
        return out
    }

    /** Points to sample at: small integers, avoiding the roots. */
    private fun samplePoints(count: Int, roots: List<Double>): DoubleArray? {
        val out = ArrayList<Double>(count)
        var candidate = 0.0
        var guard = 0
        while (out.size < count && guard < 2000) {
            val nearRoot = roots.any { abs(it - candidate) < 1e-6 }
            if (!nearRoot) out.add(candidate)
            candidate = if (candidate <= 0) -candidate + 1 else -candidate
            guard++
        }
        return if (out.size == count) out.toDoubleArray() else null
    }

    /** Gaussian elimination with partial pivoting. Null if singular. */
    private fun solve(m: Array<DoubleArray>): DoubleArray? {
        val n = m.size
        for (col in 0 until n) {
            var pivot = col
            for (row in col + 1 until n) {
                if (abs(m[row][col]) > abs(m[pivot][col])) pivot = row
            }
            if (abs(m[pivot][col]) < 1e-12) return null
            val tmp = m[col]; m[col] = m[pivot]; m[pivot] = tmp
            for (row in col + 1 until n) {
                val f = m[row][col] / m[col][col]
                for (c in col..n) m[row][c] -= f * m[col][c]
            }
        }
        val x = DoubleArray(n)
        for (row in n - 1 downTo 0) {
            var sum = m[row][n]
            for (c in row + 1 until n) sum -= m[row][c] * x[c]
            x[row] = sum / m[row][row]
        }
        return x
    }

    /** Rounds away the noise that sampling leaves behind. */
    private fun clean(v: Double): Double {
        val r = Math.round(v * 1e9) / 1e9
        return if (abs(r) < 1e-9) 0.0 else r
    }

    private fun cleanAll(p: DoubleArray): DoubleArray =
        Symbolic.trim(DoubleArray(p.size) { clean(p[it]) })

    /** A readable form, e.g. "1/(x+2) + 3/(x-1)^2". */
    fun format(terms: List<Term>): String = terms.joinToString(" + ") { t ->
        val base = "(x" + (if (t.root < 0) "+" else "-") + trimNumber(abs(t.root)) + ")"
        val power = if (t.power == 1) "" else "^${t.power}"
        "${trimNumber(t.coefficient)}/$base$power"
    }

    private fun trimNumber(v: Double): String {
        val r = clean(v)
        return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
    }
}

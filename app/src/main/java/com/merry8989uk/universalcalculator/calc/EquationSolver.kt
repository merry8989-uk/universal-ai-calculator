package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs
import kotlin.math.sqrt

/** One root, real or complex. */
data class Root(val re: Double, val im: Double) {
    val isReal: Boolean get() = abs(im) < 1e-9

    /** The root as it should be written. */
    val text: String
        get() = if (isReal) Format.number(re) else Complex.text(Complex(re, im))

    /** True when this is a whole number, which is what the detail panel works on. */
    val isWholeNumber: Boolean
        get() = isReal && abs(re - Math.round(re)) < 1e-9
}

/** What a polynomial equation turned out to be. */
data class PolySolution(
    val degree: Int,
    val roots: List<Root>,
    /** A plain sentence describing the roots. */
    val kind: String,
    /** Vieta: the sum of the roots, −b/a for a quadratic. */
    val sumOfRoots: Double?,
    /** Vieta: the product of the roots. */
    val productOfRoots: Double?,
    /** True when every root is real. */
    val allReal: Boolean,
) {
    /** The distinct real roots, for anything that wants plain numbers. */
    val realRoots: List<Double> get() = roots.filter { it.isReal }.map { it.re }
}

/** What a set of simultaneous equations turned out to be. */
sealed interface LinearSolution {
    /** One answer each. */
    data class Unique(val values: DoubleArray) : LinearSolution {
        override fun equals(other: Any?) = other is Unique && values.contentEquals(other.values)
        override fun hashCode() = values.contentHashCode()
    }
    /** The equations contradict each other. */
    data object None : LinearSolution
    /** The equations say the same thing more than once, so there are endless answers. */
    data object Infinite : LinearSolution
}

/**
 * Solving equations, the way the classic calculators do it.
 *
 * Three kinds, because they are three different questions:
 *
 *   - a polynomial, one unknown, degree one to five
 *   - several equations in as many unknowns
 *   - an inequality, whose answer is a range rather than a number
 *
 * The polynomial and simultaneous work is handed to [Matrix], which already had
 * a root finder and a linear solver; this is the layer that names the answer.
 */
object EquationSolver {

    const val MAX_DEGREE = 5

    /** The words for each degree, for the heading. */
    fun degreeName(degree: Int): String = when (degree) {
        1 -> "Linear"
        2 -> "Quadratic"
        3 -> "Cubic"
        4 -> "Quartic"
        5 -> "Quintic"
        else -> "Degree $degree"
    }

    /**
     * All the roots of a polynomial, highest power first.
     *
     * `[1, 0, -4]` is x² − 4, whose roots are 2 and −2.
     */
    fun polynomial(coeffsHighFirst: DoubleArray): PolySolution? {
        val c = trimLeading(coeffsHighFirst)
        val degree = c.size - 1
        if (degree < 1) return null

        val roots: List<Root> = try {
            Matrix.polyRoots(c).map { Root(clean(it.re), clean(it.im)) }
        } catch (e: Exception) {
            return null
        }
        // A root that is not a finite number is not a root.
        val usable = roots.filter { it.re.isFinite() && it.im.isFinite() }
        if (usable.isEmpty()) return null

        // Highest power first is what the caller gave, so Vieta reads off directly.
        val a = c.first()
        val b = if (c.size > 1) c[1] else 0.0
        val constant = c.last()
        val sum = if (abs(a) > 1e-12) -b / a else null
        val product = if (abs(a) > 1e-12 && degree > 0) {
            // product of roots = (−1)^degree · constant / leading
            val sign = if (degree % 2 == 0) 1.0 else -1.0
            sign * constant / a
        } else {
            null
        }

        val reals = usable.filter { it.isReal }
        val complexes = usable.filterNot { it.isReal }
        val distinctReals = reals.distinctBy { Math.round(it.re * 1e9) }.size
        val kind = when {
            complexes.isNotEmpty() -> "${reals.size} real and ${complexes.size} complex roots"
            distinctReals < reals.size -> "a repeated root, and $distinctReals distinct"
            else -> "${reals.size} distinct real roots"
        }

        return PolySolution(
            degree = degree,
            roots = usable.sortedWith(compareBy({ it.re }, { it.im })),
            kind = kind,
            sumOfRoots = sum,
            productOfRoots = product,
            allReal = complexes.isEmpty(),
        )
    }

    /**
     * Several equations in as many unknowns.
     *
     * [a] is the coefficients, one row per equation; [b] is what each is equal to.
     */
    fun simultaneous(a: Array<DoubleArray>, b: DoubleArray): LinearSolution {
        if (a.isEmpty() || a.size != b.size) return LinearSolution.None
        val n = a.size
        if (a.any { it.size != n }) return LinearSolution.None

        // Gaussian elimination on the augmented matrix. The determinant alone
        // cannot tell "no answer" from "endless answers" — both are singular —
        // so the augmented row is what decides it.
        val m = Array(n) { r -> DoubleArray(n + 1) { c -> if (c < n) a[r][c] else b[r] } }
        var row = 0
        val pivotColumns = ArrayList<Int>(n)
        for (col in 0 until n) {
            var pivot = -1
            for (r in row until n) {
                if (abs(m[r][col]) > 1e-9) { pivot = r; break }
            }
            if (pivot < 0) continue
            val swap = m[row]; m[row] = m[pivot]; m[pivot] = swap

            val lead = m[row][col]
            for (c in col..n) m[row][c] /= lead
            for (r in 0 until n) {
                if (r == row) continue
                val factor = m[r][col]
                if (abs(factor) > 1e-12) {
                    for (c in col..n) m[r][c] -= factor * m[row][c]
                }
            }
            pivotColumns.add(col)
            row++
        }

        // A row that says 0 = something is a contradiction.
        for (r in row until n) {
            if (abs(m[r][n]) > 1e-9) return LinearSolution.None
        }
        // Fewer pivots than unknowns: the equations say the same thing twice.
        if (pivotColumns.size < n) return LinearSolution.Infinite

        val x = DoubleArray(n)
        pivotColumns.forEachIndexed { i, c -> x[c] = m[i][n] }
        return LinearSolution.Unique(x.map { clean(it) }.toDoubleArray())
    }

    /** The wording for a linear inequality `ax + b (op) 0`. */
    fun linearInequality(
        a: Double,
        b: Double,
        lessThan: Boolean,
        notation: IntervalNotation = IntervalNotation.PARENTHESES,
    ): String {
        if (abs(a) < 1e-12) {
            // b on its own decides it
            val holds = if (lessThan) b < 0 else b > 0
            return if (holds) "True for every x" else "No value of x works"
        }
        // ax + b < 0  =>  x < -b/a  (the sign flips when a is negative)
        val bound = -b / a
        val strictLess = if (a > 0) lessThan else !lessThan
        val symbol = if (strictLess) "<" else ">"
        val bracket = if (strictLess) {
            NumberStyle.interval(Double.NEGATIVE_INFINITY, bound, false, false, notation)
        } else {
            NumberStyle.interval(bound, Double.POSITIVE_INFINITY, false, false, notation)
        }
        return "x $symbol ${Format.number(bound)}\n$bracket"
    }

    /**
     * The wording for a quadratic inequality `ax² + bx + c (op) 0`.
     *
     * The answer depends on which way the parabola opens and whether it meets
     * the axis at all, so each case is named rather than guessed.
     */
    fun quadraticInequality(
        a: Double,
        b: Double,
        c: Double,
        lessThan: Boolean,
        notation: IntervalNotation = IntervalNotation.PARENTHESES,
    ): String {
        if (abs(a) < 1e-12) return linearInequality(b, c, lessThan, notation)
        val disc = b * b - 4 * a * c

        if (disc < -1e-12) {
            // never touches the axis: the sign is the sign of a everywhere
            val alwaysPositive = a > 0
            val holds = if (lessThan) !alwaysPositive else alwaysPositive
            return if (holds) {
                "True for every x"
            } else {
                "No value of x works"
            }
        }

        val root = sqrt(disc.coerceAtLeast(0.0))
        val r1 = clean((-b - root) / (2 * a))
        val r2 = clean((-b + root) / (2 * a))
        val lo = minOf(r1, r2)
        val hi = maxOf(r1, r2)
        val loText = Format.number(lo)
        val hiText = Format.number(hi)

        if (abs(disc) < 1e-12) {
            // a repeated root: the parabola only touches the axis
            val holds = if (lessThan) a < 0 else a > 0
            return if (holds) {
                "True for every x except x = $loText"
            } else {
                "Only x = $loText, and only if the inequality is not strict"
            }
        }

        // between the roots when the inequality agrees with the sign of a
        val between = if (a > 0) lessThan else !lessThan
        return if (between) {
            "$loText < x < $hiText\n" +
                NumberStyle.interval(lo, hi, false, false, notation)
        } else {
            val left = NumberStyle.interval(Double.NEGATIVE_INFINITY, lo, false, false, notation)
            val right = NumberStyle.interval(hi, Double.POSITIVE_INFINITY, false, false, notation)
            "x < $loText or x > $hiText\n" + NumberStyle.union(left, right)
        }
    }

    // ---------------- helpers ----------------

    /** Drops leading zeros, so the degree is the real one. */
    private fun trimLeading(c: DoubleArray): DoubleArray {
        var i = 0
        while (i < c.size && abs(c[i]) < 1e-12) i++
        return if (i == 0) c else c.copyOfRange(i, c.size)
    }

    /** Kills the −0 and the 1e-17 that root finders leave behind. */
    private fun clean(v: Double): Double {
        if (abs(v) < 1e-12) return 0.0
        val r = Math.round(v * 1e9) / 1e9
        return if (abs(r) < 1e-12) 0.0 else r
    }
}

/**
 * The extra facts about a number, shown under a result.
 *
 * These are the things a scientific calculator offers when you press the little
 * arrow beside an answer: is it prime, what divides it, and how does it look in
 * the other bases.
 */
object ResultDetail {

    /** Every positive divisor of a whole number, smallest first. */
    fun divisors(n: Long): List<Long> {
        val v = abs(n)
        if (v == 0L) return emptyList()
        if (v == 1L) return listOf(1L)
        val out = ArrayList<Long>()
        var i = 1L
        while (i * i <= v) {
            if (v % i == 0L) {
                out.add(i)
                if (i != v / i) out.add(v / i)
            }
            i++
        }
        return out.sorted()
    }

    /** The prime factorisation, written as `2^3 × 3 × 5`. */
    fun primeFactorisation(n: Long): String {
        val v = abs(n)
        if (v < 2L) return if (v == 1L) "1" else "0"
        val parts = ArrayList<String>()
        var left = v
        var p = 2L
        while (p * p <= left) {
            if (left % p == 0L) {
                var count = 0
                while (left % p == 0L) { left /= p; count++ }
                parts.add(if (count == 1) "$p" else "$p^$count")
            }
            p++
        }
        if (left > 1L) parts.add("$left")
        return parts.joinToString(" \u00D7 ")
    }

    fun isPrime(n: Long): Boolean {
        val v = abs(n)
        if (v < 2L) return false
        if (v % 2L == 0L) return v == 2L
        var i = 3L
        while (i * i <= v) {
            if (v % i == 0L) return false
            i += 2
        }
        return true
    }

    fun binary(n: Long): String = java.lang.Long.toBinaryString(n)
    fun octal(n: Long): String = java.lang.Long.toOctalString(n)
    fun hex(n: Long): String = java.lang.Long.toHexString(n).uppercase()

    /** The whole-number reading of a result, or null when it is not one. */
    fun asWhole(v: Double): Long? =
        if (v.isFinite() && abs(v - Math.round(v)) < 1e-9 && abs(v) < 9e15) Math.round(v) else null
}

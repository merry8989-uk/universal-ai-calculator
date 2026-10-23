package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs

/** Matrix maths for the Matrix tool. Matrices are rows of doubles. */
object Matrix {

    /** Parses rows separated by newline or ';' and entries by comma or space. */
    fun parse(text: String): Array<DoubleArray> {
        val rows = text.split('\n', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { row ->
                row.split(',', ' ', '\t')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { it.toDoubleOrNull() ?: throw Evaluator.EvalException("Bad number in matrix") }
                    .toDoubleArray()
            }
            .toTypedArray()
        if (rows.isEmpty()) throw Evaluator.EvalException("Matrix is empty")
        val cols = rows[0].size
        if (rows.any { it.size != cols }) throw Evaluator.EvalException("All rows must have the same length")
        return rows
    }

    fun rows(m: Array<DoubleArray>) = m.size
    fun cols(m: Array<DoubleArray>) = if (m.isEmpty()) 0 else m[0].size

    fun format(m: Array<DoubleArray>): String =
        m.joinToString("\n") { row -> row.joinToString("  ") { Format.number(it) } }

    fun format(v: DoubleArray): String = v.joinToString("  ") { Format.number(it) }

    fun add(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        requireShape(a, b)
        return Array(rows(a)) { i -> DoubleArray(cols(a)) { j -> a[i][j] + b[i][j] } }
    }

    fun sub(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        requireShape(a, b)
        return Array(rows(a)) { i -> DoubleArray(cols(a)) { j -> a[i][j] - b[i][j] } }
    }

    fun mul(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        if (cols(a) != rows(b)) throw Evaluator.EvalException("A cols must equal B rows for A x B")
        val out = Array(rows(a)) { DoubleArray(cols(b)) }
        for (i in 0 until rows(a)) for (j in 0 until cols(b)) {
            var s = 0.0
            for (k in 0 until cols(a)) s += a[i][k] * b[k][j]
            out[i][j] = s
        }
        return out
    }

    fun scalar(a: Array<DoubleArray>, k: Double): Array<DoubleArray> =
        Array(rows(a)) { i -> DoubleArray(cols(a)) { j -> a[i][j] * k } }

    fun transpose(a: Array<DoubleArray>): Array<DoubleArray> =
        Array(cols(a)) { j -> DoubleArray(rows(a)) { i -> a[i][j] } }

    fun identity(n: Int): Array<DoubleArray> =
        Array(n) { i -> DoubleArray(n) { j -> if (i == j) 1.0 else 0.0 } }

    fun trace(a: Array<DoubleArray>): Double {
        var s = 0.0
        for (i in 0 until minOf(rows(a), cols(a))) s += a[i][i]
        return s
    }

    /** Determinant by Gaussian elimination with partial pivoting. */
    fun determinant(a: Array<DoubleArray>): Double {
        if (rows(a) != cols(a)) throw Evaluator.EvalException("Determinant needs a square matrix")
        val n = rows(a)
        val m = Array(n) { a[it].copyOf() }
        var det = 1.0
        for (c in 0 until n) {
            var pivot = c
            for (r in c + 1 until n) if (abs(m[r][c]) > abs(m[pivot][c])) pivot = r
            if (abs(m[pivot][c]) < 1e-12) return 0.0
            if (pivot != c) { val t = m[pivot]; m[pivot] = m[c]; m[c] = t; det = -det }
            det *= m[c][c]
            for (r in c + 1 until n) {
                val f = m[r][c] / m[c][c]
                for (k in c until n) m[r][k] -= f * m[c][k]
            }
        }
        return det
    }

    /** Inverse by Gauss-Jordan with partial pivoting. Null when singular. */
    fun inverse(a: Array<DoubleArray>): Array<DoubleArray>? {
        if (rows(a) != cols(a)) throw Evaluator.EvalException("Inverse needs a square matrix")
        val n = rows(a)
        val m = Array(n) { DoubleArray(2 * n) }
        for (i in 0 until n) {
            for (j in 0 until n) m[i][j] = a[i][j]
            m[i][n + i] = 1.0
        }
        for (c in 0 until n) {
            var pivot = c
            for (r in c + 1 until n) if (abs(m[r][c]) > abs(m[pivot][c])) pivot = r
            if (abs(m[pivot][c]) < 1e-12) return null
            if (pivot != c) { val t = m[pivot]; m[pivot] = m[c]; m[c] = t }
            val div = m[c][c]
            for (k in 0 until 2 * n) m[c][k] /= div
            for (r in 0 until n) {
                if (r == c) continue
                val f = m[r][c]
                if (f != 0.0) for (k in 0 until 2 * n) m[r][k] -= f * m[c][k]
            }
        }
        return Array(n) { i -> DoubleArray(n) { j -> m[i][n + j] } }
    }

    /** Solves a square system A x = b. Null when singular. */
    fun solve(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val inv = inverse(a) ?: return null
        val n = rows(a)
        return DoubleArray(n) { i -> (0 until n).sumOf { j -> inv[i][j] * b[j] } }
    }

    /** Cramer's-rule 2x2 / 3x3 solver for a small system entered as coefficients. */
    fun solve2x2(a1: Double, b1: Double, c1: Double, a2: Double, b2: Double, c2: Double): Pair<Double, Double>? {
        val d = a1 * b2 - a2 * b1
        if (abs(d) < 1e-12) return null
        val dx = c1 * b2 - c2 * b1
        val dy = a1 * c2 - a2 * c1
        return Pair(dx / d, dy / d)
    }

    fun solve3x3(
        a1: Double, b1: Double, c1: Double, d1: Double,
        a2: Double, b2: Double, c2: Double, d2: Double,
        a3: Double, b3: Double, c3: Double, d3: Double,
    ): Triple<Double, Double, Double>? {
        val det = a1 * (b2 * c3 - b3 * c2) - b1 * (a2 * c3 - a3 * c2) + c1 * (a2 * b3 - a3 * b2)
        if (abs(det) < 1e-12) return null
        val dx = d1 * (b2 * c3 - b3 * c2) - b1 * (d2 * c3 - d3 * c2) + c1 * (d2 * b3 - d3 * b2)
        val dy = a1 * (d2 * c3 - d3 * c2) - d1 * (a2 * c3 - a3 * c2) + c1 * (a2 * d3 - a3 * d2)
        val dz = a1 * (b2 * d3 - b3 * d2) - b1 * (a2 * d3 - a3 * d2) + d1 * (a2 * b3 - a3 * b2)
        return Triple(dx / det, dy / det, dz / det)
    }

    private fun requireShape(a: Array<DoubleArray>, b: Array<DoubleArray>) {
        if (rows(a) != rows(b) || cols(a) != cols(b)) {
            throw Evaluator.EvalException("Matrices must be the same size")
        }
    }
    // ================= eigenvalues =================

    /** Eigenvalues of a square matrix (real ones exact, complex handled too). */
    fun eigenvalues(a: Array<DoubleArray>): List<Complex> {
        requireSquare(a)
        val n = rows(a)
        return when (n) {
            0 -> throw Evaluator.EvalException("Matrix is empty")
            1 -> listOf(Complex.of(a[0][0]))
            2 -> {
                val tr = a[0][0] + a[1][1]
                val det = a[0][0] * a[1][1] - a[0][1] * a[1][0]
                val disc = tr * tr - 4.0 * det
                if (disc >= 0) {
                    val r = kotlin.math.sqrt(disc)
                    listOf(Complex.of((tr + r) / 2.0), Complex.of((tr - r) / 2.0))
                } else {
                    val r = kotlin.math.sqrt(-disc)
                    listOf(Complex(tr / 2.0, r / 2.0), Complex(tr / 2.0, -r / 2.0))
                }
            }
            else -> polyRoots(characteristicPolynomial(a))
        }
    }

    /** Coefficients of lambda^n + c1 lambda^(n-1) + ... + cn (Leverrier-Faddeev). */
    fun characteristicPolynomial(a: Array<DoubleArray>): DoubleArray {
        requireSquare(a)
        val n = rows(a)
        var b = identity(n)
        val coeffs = DoubleArray(n + 1)
        coeffs[0] = 1.0
        for (k in 1..n) {
            val ab = mul(a, b)
            val ck = -trace(ab) / k
            coeffs[k] = ck
            b = add(ab, scalar(identity(n), ck))
        }
        return coeffs
    }

    /** Durand-Kerner roots of a polynomial given highest power first. */
    fun polyRoots(coeffs: DoubleArray): List<Complex> {
        val lead = coeffs.firstOrNull() ?: 0.0
        if (lead == 0.0) throw Evaluator.EvalException("Leading coefficient must not be zero")
        val c = coeffs.map { it / lead }
        val n = c.size - 1
        if (n < 1) return emptyList()
        fun evalAt(z: Complex): Complex {
            var acc = Complex.of(c[0])
            for (i in 1..n) acc = acc.times(z).plus(Complex.of(c[i]))
            return acc
        }
        val roots = MutableList(n) { Complex.of(0.4, 0.9).pow(it + 1) }
        repeat(2000) {
            var moved = 0.0
            for (i in 0 until n) {
                var denom = Complex.of(1.0)
                for (j in 0 until n) if (j != i) denom = denom.times(roots[i].minus(roots[j]))
                if (denom.abs() < 1e-300) continue
                val delta = evalAt(roots[i]).div(denom)
                roots[i] = roots[i].minus(delta)
                moved = maxOf(moved, delta.abs())
            }
            if (moved < 1e-13) return roots.sortedBy { it.re }
        }
        return roots.sortedBy { it.re }
    }

    // ================= LU =================

    /** L, U and P with P*A = L*U (Doolittle, partial pivoting). */
    fun lu(a: Array<DoubleArray>): Triple<Array<DoubleArray>, Array<DoubleArray>, Array<DoubleArray>> {
        requireSquare(a)
        val n = rows(a)
        val u = Array(n) { a[it].copyOf() }
        val l = identity(n)
        val p = identity(n)
        for (col in 0 until n) {
            var pivot = col
            for (r in col + 1 until n) if (kotlin.math.abs(u[r][col]) > kotlin.math.abs(u[pivot][col])) pivot = r
            if (kotlin.math.abs(u[pivot][col]) < 1e-14) throw Evaluator.EvalException("Matrix is singular")
            if (pivot != col) {
                val tmp = u[col]; u[col] = u[pivot]; u[pivot] = tmp
                val tmpP = p[col]; p[col] = p[pivot]; p[pivot] = tmpP
                for (k in 0 until col) { val t = l[col][k]; l[col][k] = l[pivot][k]; l[pivot][k] = t }
            }
            for (r in col + 1 until n) {
                val f = u[r][col] / u[col][col]
                l[r][col] = f
                for (k in col until n) u[r][k] -= f * u[col][k]
            }
        }
        return Triple(l, u, p)
    }

    private fun requireSquare(a: Array<DoubleArray>) {
        if (a.isEmpty()) throw Evaluator.EvalException("Matrix is empty")
        val n = a[0].size
        if (a.any { it.size != n }) throw Evaluator.EvalException("Matrix must be square")
    }

}

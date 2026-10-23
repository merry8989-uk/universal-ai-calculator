package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.PI

/** Eigenvalues beyond 3 x 3, via the characteristic polynomial. */
class MatrixLargeEigenTest {

    private fun m(vararg rows: DoubleArray) = rows.toList().toTypedArray()

    private fun reals(a: Array<DoubleArray>): List<Double> =
        Matrix.eigenvalues(a).map { it.re }.sorted()

    @Test fun diagonal4x4() {
        val e = reals(m(
            doubleArrayOf(1.0, 0.0, 0.0, 0.0),
            doubleArrayOf(0.0, 2.0, 0.0, 0.0),
            doubleArrayOf(0.0, 0.0, 3.0, 0.0),
            doubleArrayOf(0.0, 0.0, 0.0, 4.0),
        ))
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0), e.map { kotlin.math.round(it * 1e4) / 1e4 })
    }

    @Test fun tridiagonal4x4() {
        // eigenvalues are 2 + 2 cos(k pi / 5)
        val e = reals(m(
            doubleArrayOf(2.0, 1.0, 0.0, 0.0),
            doubleArrayOf(1.0, 2.0, 1.0, 0.0),
            doubleArrayOf(0.0, 1.0, 2.0, 1.0),
            doubleArrayOf(0.0, 0.0, 1.0, 2.0),
        ))
        val expected = (1..4).map { 2.0 + 2.0 * cos(it * PI / 5.0) }.sorted()
        for (i in expected.indices) {
            assertEquals("eigenvalue ${i + 1}", expected[i], e[i], 1e-3)
        }
    }

    @Test fun identity5x5() {
        val n = 5
        val a = Array(n) { i -> DoubleArray(n) { j -> if (i == j) 1.0 else 0.0 } }
        val e = reals(a)
        for (v in e) assertEquals(1.0, v, 1e-6)
    }

    @Test fun traceAndDeterminantMatchTheEigenvalues() {
        val a = m(
            doubleArrayOf(4.0, 1.0, 0.0, 0.0),
            doubleArrayOf(1.0, 3.0, 1.0, 0.0),
            doubleArrayOf(0.0, 1.0, 2.0, 1.0),
            doubleArrayOf(0.0, 0.0, 1.0, 5.0),
        )
        val e = Matrix.eigenvalues(a)
        val sum = e.sumOf { it.re }
        val product = e.fold(1.0) { acc, c -> acc * c.re }
        assertEquals(Matrix.trace(a), sum, 1e-6)
        assertEquals(Matrix.determinant(a), product, 1e-4)
    }
}

package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class MatrixAdvancedTest {

    private fun m(vararg rows: DoubleArray) = rows.toList().toTypedArray()

    @Test fun eigenvalues2x2() {
        val e = Matrix.eigenvalues(m(doubleArrayOf(2.0, 1.0), doubleArrayOf(1.0, 3.0)))
        assertEquals(3.618034, e[0].re, 1e-5)
        assertEquals(1.381966, e[1].re, 1e-5)
        assertEquals(0.0, e[0].im, 1e-9)

        val d = Matrix.eigenvalues(m(doubleArrayOf(2.0, 0.0), doubleArrayOf(0.0, 3.0)))
        assertEquals(3.0, d[0].re, 1e-9)
        assertEquals(2.0, d[1].re, 1e-9)
    }

    @Test fun eigenvaluesCanBeComplex() {
        val e = Matrix.eigenvalues(m(doubleArrayOf(0.0, -1.0), doubleArrayOf(1.0, 0.0)))
        assertEquals(0.0, e[0].re, 1e-9)
        assertEquals(1.0, abs(e[0].im), 1e-9)
        assertEquals(0.0, e[1].re, 1e-9)
        assertEquals(1.0, abs(e[1].im), 1e-9)
    }

    @Test fun characteristicPolynomial() {
        val c = Matrix.characteristicPolynomial(m(doubleArrayOf(2.0, 1.0), doubleArrayOf(1.0, 3.0)))
        assertEquals(1.0, c[0], 1e-9)
        assertEquals(-5.0, c[1], 1e-9)
        assertEquals(5.0, c[2], 1e-9)
    }

    @Test fun eigenvalues3x3Diagonal() {
        val e = Matrix.eigenvalues(m(
            doubleArrayOf(2.0, 0.0, 0.0),
            doubleArrayOf(0.0, 3.0, 0.0),
            doubleArrayOf(0.0, 0.0, 4.0),
        ))
        val reals = e.map { it.re }.sorted()
        assertEquals(2.0, reals[0], 1e-4)
        assertEquals(3.0, reals[1], 1e-4)
        assertEquals(4.0, reals[2], 1e-4)
    }

    @Test fun eigenvalues3x3Symmetric() {
        val e = Matrix.eigenvalues(m(
            doubleArrayOf(2.0, 1.0, 0.0),
            doubleArrayOf(1.0, 2.0, 1.0),
            doubleArrayOf(0.0, 1.0, 2.0),
        ))
        val reals = e.map { it.re }.sorted()
        // known eigenvalues: 2 - sqrt(2), 2, 2 + sqrt(2)
        assertEquals(2.0 - kotlin.math.sqrt(2.0), reals[0], 1e-3)
        assertEquals(2.0, reals[1], 1e-3)
        assertEquals(2.0 + kotlin.math.sqrt(2.0), reals[2], 1e-3)
    }

    @Test fun luReconstructsTheMatrix() {
        val a = m(doubleArrayOf(4.0, 3.0), doubleArrayOf(6.0, 3.0))
        val (l, u, p) = Matrix.lu(a)
        val lu = Matrix.mul(l, u)
        val pa = Matrix.mul(p, a)
        for (i in 0..1) for (j in 0..1) assertEquals(pa[i][j], lu[i][j], 1e-9)
    }

    @Test fun luReconstructsA3x3WithPivoting() {
        val a = m(
            doubleArrayOf(0.0, 5.0, 5.0),
            doubleArrayOf(2.0, 9.0, 0.0),
            doubleArrayOf(6.0, 8.0, 8.0),
        )
        val (l, u, p) = Matrix.lu(a)
        val lu = Matrix.mul(l, u)
        val pa = Matrix.mul(p, a)
        for (i in 0..2) for (j in 0..2) assertEquals(pa[i][j], lu[i][j], 1e-9)
        // U is upper triangular, L is unit lower triangular
        for (i in 0..2) for (j in 0 until i) assertEquals(0.0, u[i][j], 1e-9)
        for (i in 0..2) for (j in i + 1..2) assertEquals(0.0, l[i][j], 1e-9)
        for (i in 0..2) assertEquals(1.0, l[i][i], 1e-9)
    }

    @Test fun singularLuIsRejected() {
        try {
            Matrix.lu(m(doubleArrayOf(1.0, 2.0), doubleArrayOf(2.0, 4.0)))
            throw AssertionError("expected failure")
        } catch (e: Evaluator.EvalException) { /* expected */ }
    }
}

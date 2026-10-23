package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MatrixTest {

    private fun m(vararg rows: DoubleArray) = rows.toList().toTypedArray()

    @Test fun parseAndShape() {
        val a = Matrix.parse("2 1\n1 3")
        assertEquals(2, Matrix.rows(a)); assertEquals(2, Matrix.cols(a))
        val b = Matrix.parse("1,2,3;4,5,6")
        assertEquals(2, Matrix.rows(b)); assertEquals(3, Matrix.cols(b))
    }

    @Test fun determinant() {
        assertEquals(5.0, Matrix.determinant(m(doubleArrayOf(2.0, 1.0), doubleArrayOf(1.0, 3.0))), 1e-9)
        assertEquals(0.0, Matrix.determinant(m(doubleArrayOf(1.0, 2.0), doubleArrayOf(2.0, 4.0))), 1e-9)
        assertEquals(-306.0, Matrix.determinant(m(
            doubleArrayOf(6.0, 1.0, 1.0),
            doubleArrayOf(4.0, -2.0, 5.0),
            doubleArrayOf(2.0, 8.0, 7.0),
        )), 1e-6)
    }

    @Test fun traceTransposeMultiply() {
        assertEquals(5.0, Matrix.trace(m(doubleArrayOf(2.0, 1.0), doubleArrayOf(1.0, 3.0))), 1e-9)
        val t = Matrix.transpose(m(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
        assertEquals(1.0, t[0][0], 1e-9); assertEquals(3.0, t[0][1], 1e-9)
        val p = Matrix.mul(
            m(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)),
            m(doubleArrayOf(5.0, 6.0), doubleArrayOf(7.0, 8.0)),
        )
        assertEquals(19.0, p[0][0], 1e-9); assertEquals(22.0, p[0][1], 1e-9)
        assertEquals(43.0, p[1][0], 1e-9); assertEquals(50.0, p[1][1], 1e-9)
    }

    @Test fun inverseTimesOriginalIsIdentity() {
        val a = m(doubleArrayOf(4.0, 7.0), doubleArrayOf(2.0, 6.0))
        val inv = Matrix.inverse(a)!!
        val prod = Matrix.mul(a, inv)
        for (i in 0..1) for (j in 0..1) {
            assertEquals(if (i == j) 1.0 else 0.0, prod[i][j], 1e-9)
        }
    }

    @Test fun singularHasNoInverse() {
        assertNull(Matrix.inverse(m(doubleArrayOf(1.0, 2.0), doubleArrayOf(2.0, 4.0))))
    }

    @Test fun solvers() {
        val r2 = Matrix.solve2x2(2.0, 1.0, 5.0, 1.0, -1.0, 1.0)
        assertNotNull(r2)
        assertEquals(2.0, r2!!.first, 1e-9); assertEquals(1.0, r2.second, 1e-9)

        val r3 = Matrix.solve3x3(
            1.0, 1.0, 1.0, 6.0,
            2.0, -1.0, 1.0, 3.0,
            1.0, 2.0, -1.0, 2.0,
        )
        assertNotNull(r3)
        assertEquals(1.0, r3!!.first, 1e-9)
        assertEquals(2.0, r3.second, 1e-9)
        assertEquals(3.0, r3.third, 1e-9)
    }

    @Test fun badInputIsRejected() {
        for (bad in listOf("", "1 2\n3")) {
            try { Matrix.parse(bad); throw AssertionError("expected failure for '$bad'") }
            catch (e: Evaluator.EvalException) { /* expected */ }
        }
    }
}

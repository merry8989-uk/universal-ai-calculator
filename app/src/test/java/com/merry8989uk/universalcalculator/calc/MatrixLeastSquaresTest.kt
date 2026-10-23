package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatrixLeastSquaresTest {

    private fun m(vararg rows: DoubleArray) = rows.toList().toTypedArray()

    @Test fun fitsAPerfectLine() {
        // rows are [1, x], b is y: the fit gives [intercept, slope]
        val a = m(doubleArrayOf(1.0, 1.0), doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0, 3.0))
        val x = Matrix.leastSquares(a, doubleArrayOf(2.0, 4.0, 6.0))!!
        assertEquals(0.0, x[0], 1e-9)     // intercept
        assertEquals(2.0, x[1], 1e-9)     // slope
        assertEquals(0.0, Matrix.residualNorm(a, doubleArrayOf(2.0, 4.0, 6.0), x), 1e-9)
    }

    @Test fun fitsWithNoiseAndBeatsAPerturbation() {
        val a = m(doubleArrayOf(1.0, 1.0), doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0, 3.0), doubleArrayOf(1.0, 4.0))
        val b = doubleArrayOf(2.1, 3.9, 6.2, 7.8)
        val x = Matrix.leastSquares(a, b)!!
        val best = Matrix.residualNorm(a, b, x)
        // nudging the answer must make it worse
        for (delta in listOf(0.05, -0.05)) {
            val worse = doubleArrayOf(x[0] + delta, x[1])
            assertTrue("perturbation should be worse", Matrix.residualNorm(a, b, worse) > best)
        }
        // the slope is close to 2, the intercept close to 0
        assertEquals(2.0, x[1], 0.1)
        assertEquals(0.0, x[0], 0.2)
    }

    @Test fun solvesASquareSystemExactly() {
        val a = m(doubleArrayOf(2.0, 1.0), doubleArrayOf(1.0, 3.0))
        val x = Matrix.leastSquares(a, doubleArrayOf(5.0, 10.0))!!
        assertEquals(1.0, x[0], 1e-9)
        assertEquals(3.0, x[1], 1e-9)
    }

    @Test fun rankDeficientReturnsNull() {
        val a = m(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(2.0, 4.0),
            doubleArrayOf(3.0, 6.0),
        )
        assertNull(Matrix.leastSquares(a, doubleArrayOf(1.0, 2.0, 3.0)))
    }

    @Test fun badShapesAreRejected() {
        try {
            Matrix.leastSquares(m(doubleArrayOf(1.0, 2.0)), doubleArrayOf(1.0))
            throw AssertionError("expected failure for fewer rows than columns")
        } catch (e: Evaluator.EvalException) { /* expected */ }
        try {
            Matrix.leastSquares(m(doubleArrayOf(1.0), doubleArrayOf(2.0)), doubleArrayOf(1.0))
            throw AssertionError("expected failure for a mismatched b")
        } catch (e: Evaluator.EvalException) { /* expected */ }
    }
}

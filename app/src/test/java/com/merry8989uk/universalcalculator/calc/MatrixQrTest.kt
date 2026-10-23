package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixQrTest {

    private fun m(vararg rows: DoubleArray) = rows.toList().toTypedArray()

    private fun checkQr(a: Array<DoubleArray>) {
        val (q, r) = Matrix.qr(a)
        val mRows = Matrix.rows(a); val nCols = Matrix.cols(a)
        // A = Q·R
        val qr = Matrix.mul(q, r)
        for (i in 0 until mRows) for (j in 0 until nCols) {
            assertEquals("A[$i][$j]", a[i][j], qr[i][j], 1e-9)
        }
        // Qᵀ·Q = I
        val qtq = Matrix.mul(Matrix.transpose(q), q)
        for (i in 0 until mRows) for (j in 0 until mRows) {
            assertEquals("QᵀQ[$i][$j]", if (i == j) 1.0 else 0.0, qtq[i][j], 1e-9)
        }
        // R is upper triangular
        for (i in 0 until mRows) for (j in 0 until minOf(i, nCols)) {
            assertEquals("R[$i][$j]", 0.0, r[i][j], 1e-9)
        }
    }

    @Test fun squareMatrix() {
        checkQr(m(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
        checkQr(m(
            doubleArrayOf(4.0, -2.0, 1.0),
            doubleArrayOf(1.0, 3.0, -5.0),
            doubleArrayOf(2.0, 6.0, 7.0),
        ))
    }

    @Test fun tallMatrix() {
        checkQr(m(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0),
            doubleArrayOf(5.0, 6.0),
        ))
    }

    @Test fun wideMatrix() {
        checkQr(m(
            doubleArrayOf(1.0, 2.0, 3.0),
            doubleArrayOf(4.0, 5.0, 6.0),
        ))
    }

    @Test fun zeroColumnIsHandled() {
        checkQr(m(doubleArrayOf(0.0, 1.0), doubleArrayOf(0.0, 2.0)))
    }
}

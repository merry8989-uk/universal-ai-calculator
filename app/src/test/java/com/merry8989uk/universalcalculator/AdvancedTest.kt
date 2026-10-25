package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.AdvancedCalculus
import com.merry8989uk.universalcalculator.calc.AdvancedMatrix
import com.merry8989uk.universalcalculator.calc.AdvancedStatistics
import com.merry8989uk.universalcalculator.calc.AngleUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The advanced routines the master plan asked for: Cholesky, a matrix power and
 * the matrix exponential; a Taylor series, a derivative of any order and the
 * points where a curve turns; and regression with more than one predictor.
 *
 * Every expected value here was worked out by hand or by an independent method
 * first, because a numerical routine that is subtly wrong still returns
 * plausible numbers.
 */
class AdvancedTest {

    private fun m(vararg rows: DoubleArray) = rows.toList().toTypedArray()

    private fun close(expected: Double, actual: Double, tolerance: Double = 1e-9, what: String = "") {
        assertEquals(what, expected, actual, tolerance)
    }

    private fun sameMatrix(expected: Array<DoubleArray>, actual: Array<DoubleArray>?, tolerance: Double = 1e-8) {
        assertNotNull("expected a matrix, got null", actual)
        assertEquals("wrong number of rows", expected.size, actual!!.size)
        for (r in expected.indices) {
            assertEquals("row $r has the wrong width", expected[r].size, actual[r].size)
            for (c in expected[r].indices) {
                assertEquals(
                    "element [$r][$c]",
                    expected[r][c], actual[r][c], tolerance,
                )
            }
        }
    }

    // ---------------- Cholesky ----------------

    @Test fun choleskyOfAKnownMatrix() {
        // the textbook example: L is [[2,0,0],[6,1,0],[-8,5,3]]
        val a = m(
            doubleArrayOf(4.0, 12.0, -16.0),
            doubleArrayOf(12.0, 37.0, -43.0),
            doubleArrayOf(-16.0, -43.0, 98.0),
        )
        val l = AdvancedMatrix.cholesky(a)
        sameMatrix(
            m(
                doubleArrayOf(2.0, 0.0, 0.0),
                doubleArrayOf(6.0, 1.0, 0.0),
                doubleArrayOf(-8.0, 5.0, 3.0),
            ),
            l,
        )
    }

    @Test fun choleskyTimesItsTransposeGivesTheMatrixBack() {
        val a = m(
            doubleArrayOf(25.0, 15.0, -5.0),
            doubleArrayOf(15.0, 18.0, 0.0),
            doubleArrayOf(-5.0, 0.0, 11.0),
        )
        val l = AdvancedMatrix.cholesky(a)!!
        val t = Array(l.size) { r -> DoubleArray(l.size) { c -> l[c][r] } }
        sameMatrix(a, AdvancedMatrix.multiply(l, t))
    }

    @Test fun choleskyRefusesAMatrixThatIsNotPositiveDefinite() {
        // [[1,2],[2,1]] has an eigenvalue of -1, so no real factor exists
        assertNull(AdvancedMatrix.cholesky(m(doubleArrayOf(1.0, 2.0), doubleArrayOf(2.0, 1.0))))
    }

    @Test fun choleskyRefusesAMatrixThatIsNotSymmetric() {
        assertNull(AdvancedMatrix.cholesky(m(doubleArrayOf(4.0, 1.0), doubleArrayOf(2.0, 4.0))))
    }

    @Test fun choleskyOfTheIdentityIsTheIdentity() {
        val i = AdvancedMatrix.identity(3)
        sameMatrix(i, AdvancedMatrix.cholesky(i))
    }

    // ---------------- matrix power ----------------

    @Test fun aMatrixToTheThird() {
        val a = m(doubleArrayOf(1.0, 1.0), doubleArrayOf(0.0, 1.0))
        sameMatrix(m(doubleArrayOf(1.0, 3.0), doubleArrayOf(0.0, 1.0)), AdvancedMatrix.power(a, 3))
    }

    @Test fun aMatrixToTheZeroIsTheIdentity() {
        val a = m(doubleArrayOf(2.0, 3.0), doubleArrayOf(5.0, 7.0))
        sameMatrix(AdvancedMatrix.identity(2), AdvancedMatrix.power(a, 0))
    }

    @Test fun aMatrixToTheFirstIsItself() {
        val a = m(doubleArrayOf(2.0, 3.0), doubleArrayOf(5.0, 7.0))
        sameMatrix(a, AdvancedMatrix.power(a, 1))
    }

    @Test fun aNegativePowerIsTheInverseRaised() {
        // [[1,1],[0,1]]^-1 is [[1,-1],[0,1]]
        val a = m(doubleArrayOf(1.0, 1.0), doubleArrayOf(0.0, 1.0))
        sameMatrix(m(doubleArrayOf(1.0, -1.0), doubleArrayOf(0.0, 1.0)), AdvancedMatrix.power(a, -1))
    }

    @Test fun aPowerAndItsReverseCancel() {
        // A^4 times A^-4 is the identity. Note it is NOT power(power(A,4), -4):
        // that would be (A^4)^-4, which is A^-16.
        val a = m(doubleArrayOf(2.0, 1.0), doubleArrayOf(1.0, 3.0))
        val up = AdvancedMatrix.power(a, 4)!!
        val down = AdvancedMatrix.power(a, -4)!!
        sameMatrix(AdvancedMatrix.identity(2), AdvancedMatrix.multiply(up, down), 1e-9)
    }

    @Test fun aSingularMatrixHasNoNegativePower() {
        assertNull(AdvancedMatrix.power(m(doubleArrayOf(1.0, 2.0), doubleArrayOf(2.0, 4.0)), -1))
    }

    // ---------------- matrix exponential ----------------

    @Test fun theExponentialOfZeroIsTheIdentity() {
        sameMatrix(
            AdvancedMatrix.identity(2),
            AdvancedMatrix.expm(m(doubleArrayOf(0.0, 0.0), doubleArrayOf(0.0, 0.0))),
        )
    }

    @Test fun theExponentialOfANilpotentMatrixStopsAtTwoTerms() {
        // A^2 = 0, so e^A = I + A exactly
        val a = m(doubleArrayOf(0.0, 1.0), doubleArrayOf(0.0, 0.0))
        sameMatrix(m(doubleArrayOf(1.0, 1.0), doubleArrayOf(0.0, 1.0)), AdvancedMatrix.expm(a))
    }

    @Test fun theExponentialOfADiagonalMatrixIsTheExponentialOfItsEntries() {
        val a = m(doubleArrayOf(1.0, 0.0), doubleArrayOf(0.0, 1.0))
        val e = Math.E
        sameMatrix(m(doubleArrayOf(e, 0.0), doubleArrayOf(0.0, e)), AdvancedMatrix.expm(a), 1e-9)
    }

    @Test fun theExponentialOfARotationIsARotation() {
        // e^[[0,-1],[1,0]] is the rotation by one radian
        val a = m(doubleArrayOf(0.0, -1.0), doubleArrayOf(1.0, 0.0))
        sameMatrix(
            m(
                doubleArrayOf(Math.cos(1.0), -Math.sin(1.0)),
                doubleArrayOf(Math.sin(1.0), Math.cos(1.0)),
            ),
            AdvancedMatrix.expm(a),
            1e-9,
        )
    }

    @Test fun theExponentialIsAlwaysInvertible() {
        // e^A is never singular, however awkward A is
        val a = m(doubleArrayOf(-3.0, 7.0), doubleArrayOf(2.0, -5.0))
        val e = AdvancedMatrix.expm(a)!!
        assertNotNull("the exponential came out singular", com.merry8989uk.universalcalculator.calc.Matrix.inverse(e))
    }

    // ---------------- the n-th derivative ----------------

    @Test fun theDerivativeOfAPolynomialAtEveryOrder() {
        // x^4 at x = 3: 108, 108, 72, 24, 0
        assertEquals(108.0, AdvancedCalculus.nthDerivative("x^4", 3.0, 1), 1e-6)
        assertEquals(108.0, AdvancedCalculus.nthDerivative("x^4", 3.0, 2), 1e-6)
        assertEquals(72.0, AdvancedCalculus.nthDerivative("x^4", 3.0, 3), 1e-5)
        // a fourth difference divides by h^4, so it is the loosest of the four
        assertEquals(24.0, AdvancedCalculus.nthDerivative("x^4", 3.0, 4), 5e-3)
    }

    @Test fun theDerivativeOfExpStaysExp() {
        // e^x is its own derivative at every order
        for (order in 1..4) {
            assertEquals("order $order", Math.E, AdvancedCalculus.nthDerivative("exp(x)", 1.0, order), 1e-3)
        }
    }

    @Test fun theDerivativesOfSineGoRoundTheCircle() {
        assertEquals(1.0, AdvancedCalculus.nthDerivative("sin(x)", 0.0, 1), 1e-8)
        assertEquals(0.0, AdvancedCalculus.nthDerivative("sin(x)", 0.0, 2), 1e-8)
        assertEquals(-1.0, AdvancedCalculus.nthDerivative("sin(x)", 0.0, 3), 1e-6)
        assertEquals(0.0, AdvancedCalculus.nthDerivative("sin(x)", 0.0, 4), 1e-4)
    }

    @Test fun theZerothDerivativeIsTheFunctionItself() {
        assertEquals(9.0, AdvancedCalculus.nthDerivative("x^2", 3.0, 0), 1e-12)
    }

    @Test fun anOrderBeyondTheLimitGivesNoAnswerRatherThanNonsense() {
        assertTrue(AdvancedCalculus.nthDerivative("x^8", 1.0, 9).isNaN())
        assertTrue(AdvancedCalculus.nthDerivative("x^8", 1.0, -1).isNaN())
    }

    @Test fun theAngleUnitReachesTheDerivative() {
        // d/dx sin(x) at 90 degrees is 0, because in degrees the slope there is flat
        val degrees = AdvancedCalculus.nthDerivative("sin(x)", 90.0, 1, AngleUnit.DEGREE)
        assertTrue("expected a small slope in degrees, got $degrees", Math.abs(degrees) < 1e-3)
        val radians = AdvancedCalculus.nthDerivative("sin(x)", Math.PI / 2, 1, AngleUnit.RADIAN)
        assertTrue("expected a flat slope in radians, got $radians", Math.abs(radians) < 1e-6)
    }

    // ---------------- the Taylor series ----------------

    @Test fun theTaylorSeriesOfExpAboutZero() {
        val t = AdvancedCalculus.taylor("exp(x)", 0.0, 4, 0.5)!!
        // 1/0!, 1/1!, 1/2!, 1/3!, 1/4!
        val wanted = listOf(1.0, 1.0, 0.5, 1.0 / 6, 1.0 / 24)
        t.coefficients.forEachIndexed { i, c ->
            assertEquals("coefficient $i", wanted[i], c, 1e-6)
        }
    }

    @Test fun theSeriesAgreesWithTheFunctionWhereItWasTaken() {
        val t = AdvancedCalculus.taylor("exp(x)", 0.0, 5, 0.0)!!
        assertEquals("the series must give f at its own centre", t.valueAtAbout, t.checkActual, 1e-9)
    }

    @Test fun theSeriesGetsCloserAsTheOrderRises() {
        val coarse = AdvancedCalculus.taylor("exp(x)", 0.0, 2, 1.0)!!
        val fine = AdvancedCalculus.taylor("exp(x)", 0.0, 6, 1.0)!!
        assertTrue(
            "a higher order should be closer: ${coarse.error} then ${fine.error}",
            fine.error < coarse.error,
        )
    }

    @Test fun theSeriesOfAPolynomialIsThePolynomialItself() {
        // a degree-3 polynomial is exactly its own Taylor series to order 3
        val t = AdvancedCalculus.taylor("x^3 - 2x + 1", 0.0, 3, 2.0)!!
        assertEquals(0.0, t.error, 1e-5)
    }

    @Test fun theSeriesAboutAPointThatIsNotZero() {
        // e^x about 1 has coefficients e/k!
        val t = AdvancedCalculus.taylor("exp(x)", 1.0, 3, 1.5)!!
        val e = Math.E
        assertEquals(e, t.coefficients[0], 1e-5)
        assertEquals(e, t.coefficients[1], 1e-4)
        assertEquals(e / 2, t.coefficients[2], 1e-4)
    }

    @Test fun theSeriesIsWrittenOut() {
        val t = AdvancedCalculus.taylor("exp(x)", 0.0, 3, 0.5)!!
        assertTrue("the text is empty", t.text.isNotBlank())
        assertTrue("a term is missing: ${t.text}", t.text.contains("x"))
    }

    @Test fun anOrderBeyondTheLimitIsClampedRatherThanRefused() {
        // asking for more terms than the step can carry gives the most it can
        val t = AdvancedCalculus.taylor("exp(x)", 0.0, 99, 0.5)
        assertNotNull(t)
        assertEquals(AdvancedCalculus.MAX_ORDER + 1, t!!.coefficients.size)
    }

    // ---------------- turning points ----------------

    @Test fun theTurningPointOfAParabola() {
        // x^2 - 4x + 3 turns at x = 2, and the value there is -1
        val points = AdvancedCalculus.turningPoints("x^2 - 4x + 3", -5.0, 5.0)
        assertEquals(1, points.size)
        assertEquals(2.0, points[0].x, 1e-6)
        assertEquals(-1.0, points[0].y, 1e-6)
        assertEquals("minimum", points[0].kind)
    }

    @Test fun ACubicHasAMaximumThenAMinimum() {
        // x^3 - 3x turns at -1 (a maximum, y = 2) and 1 (a minimum, y = -2)
        val points = AdvancedCalculus.turningPoints("x^3 - 3x", -3.0, 3.0)
        assertEquals(2, points.size)
        assertEquals(-1.0, points[0].x, 1e-5)
        assertEquals("maximum", points[0].kind)
        assertEquals(2.0, points[0].y, 1e-5)
        assertEquals(1.0, points[1].x, 1e-5)
        assertEquals("minimum", points[1].kind)
        assertEquals(-2.0, points[1].y, 1e-5)
    }

    @Test fun aQuarticHasThreeTurningPoints() {
        // x^4 - 4x^2: minima at ±sqrt(2) and a maximum at 0
        val points = AdvancedCalculus.turningPoints("x^4 - 4x^2", -3.0, 3.0)
        assertEquals(3, points.size)
        assertEquals(-Math.sqrt(2.0), points[0].x, 1e-4)
        assertEquals("minimum", points[0].kind)
        assertEquals(0.0, points[1].x, 1e-4)
        assertEquals("maximum", points[1].kind)
        assertEquals(Math.sqrt(2.0), points[2].x, 1e-4)
        assertEquals("minimum", points[2].kind)
    }

    @Test fun aStraightLineNeverTurns() {
        assertTrue(AdvancedCalculus.turningPoints("x", -5.0, 5.0).isEmpty())
        assertTrue(AdvancedCalculus.turningPoints("3", -5.0, 5.0).isEmpty())
    }

    @Test fun sineTurnsFourTimesOverTwoPeriods() {
        val points = AdvancedCalculus.turningPoints("sin(x)", -7.0, 7.0)
        assertEquals(4, points.size)
        // maxima at -3pi/2 and pi/2, minima at -pi/2 and 3pi/2
        assertEquals("maximum", points[0].kind)
        assertEquals("minimum", points[1].kind)
        assertEquals("maximum", points[2].kind)
        assertEquals("minimum", points[3].kind)
        assertEquals(Math.PI / 2, points[2].x, 1e-4)
    }

    @Test fun aBackwardsOrEmptyRangeFindsNothing() {
        assertTrue(AdvancedCalculus.turningPoints("x^2", 5.0, -5.0).isEmpty())
        assertTrue(AdvancedCalculus.turningPoints("x^2", 1.0, 1.0).isEmpty())
    }

    // ---------------- multiple regression ----------------

    @Test fun aLineThroughPointsIsRecoveredExactly() {
        // y = 1 + 2x
        val x = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val y = x.map { 1.0 + 2.0 * it }
        val r = AdvancedStatistics.multipleRegression(listOf(x), y)!!
        assertEquals(1.0, r.coefficients[0], 1e-9)
        assertEquals(2.0, r.coefficients[1], 1e-9)
        assertEquals(1.0, r.rSquared, 1e-9)
    }

    @Test fun twoPredictorsAreRecoveredExactly() {
        // y = 1 + 2x1 + 3x2
        val x1 = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        val x2 = listOf(1.0, 1.0, 2.0, 3.0, 4.0, 5.0)
        val y = x1.indices.map { 1.0 + 2.0 * x1[it] + 3.0 * x2[it] }
        val r = AdvancedStatistics.multipleRegression(listOf(x1, x2), y)!!
        assertEquals(1.0, r.coefficients[0], 1e-8)
        assertEquals(2.0, r.coefficients[1], 1e-8)
        assertEquals(3.0, r.coefficients[2], 1e-8)
        assertEquals(1.0, r.rSquared, 1e-9)
        assertEquals(2, r.predictors)
        assertEquals(6, r.observations)
    }

    @Test fun aNoisyFitStillExplainsMostOfIt() {
        val x = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val y = listOf(2.1, 3.9, 6.2, 7.8, 10.1)
        val r = AdvancedStatistics.multipleRegression(listOf(x), y)!!
        assertTrue("R squared came out at ${r.rSquared}", r.rSquared > 0.99)
        assertTrue("the slope came out at ${r.coefficients[1]}", Math.abs(r.coefficients[1] - 2.0) < 0.05)
    }

    @Test fun theAdjustedRSquaredIsNoBetterThanThePlainOne() {
        // adjusted penalises predictors, so it can only be lower (or equal)
        val x1 = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        val x2 = listOf(1.0, 1.0, 2.0, 3.0, 4.0, 5.0)
        val y = listOf(3.0, 5.0, 9.0, 13.0, 17.0, 21.0)
        val r = AdvancedStatistics.multipleRegression(listOf(x1, x2), y)!!
        assertTrue(r.adjustedRSquared <= r.rSquared + 1e-12)
    }

    @Test fun anImpossibleRegressionIsRefusedRatherThanGuessed() {
        // one predictor and one observation cannot be fitted
        assertNull(AdvancedStatistics.multipleRegression(listOf(listOf(1.0)), listOf(2.0)))
        // no predictors at all
        assertNull(AdvancedStatistics.multipleRegression(emptyList(), listOf(1.0, 2.0)))
        // a predictor of the wrong length
        assertNull(AdvancedStatistics.multipleRegression(listOf(listOf(1.0, 2.0)), listOf(1.0, 2.0, 3.0)))
    }

    @Test fun theFittedLineIsWrittenOut() {
        val x = listOf(1.0, 2.0, 3.0, 4.0)
        val y = x.map { 1.0 + 2.0 * it }
        val r = AdvancedStatistics.multipleRegression(listOf(x), y)!!
        val text = r.text(listOf("x"))
        assertTrue("the line reads '$text'", text.contains("y =") && text.contains("x"))
    }
}

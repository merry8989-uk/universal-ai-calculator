package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Numeric limits. Every expected value here was worked out independently before
 * the assertion was written.
 */
class LimitsTest {

    private fun lim(expr: String, at: Double, side: Limits.Side = Limits.Side.BOTH): Double? =
        Limits.limit(expr, at, side)

    @Test fun theClassicSincLimit() {
        assertEquals(1.0, lim("sin(x)/x", 0.0)!!, 1e-6)
    }

    @Test fun aRemovableSingularity() {
        assertEquals(4.0, lim("(x^2-4)/(x-2)", 2.0)!!, 1e-6)
    }

    @Test fun anotherRemovableSingularity() {
        assertEquals(3.0, lim("(x^3-1)/(x-1)", 1.0)!!, 1e-6)
    }

    @Test fun theHalfAngleLimit() {
        assertEquals(0.5, lim("(1-cos(x))/x^2", 0.0)!!, 1e-5)
    }

    @Test fun theExponentialLimit() {
        assertEquals(1.0, lim("(exp(x)-1)/x", 0.0)!!, 1e-6)
    }

    @Test fun aScaledSine() {
        assertEquals(3.0, lim("sin(3*x)/x", 0.0)!!, 1e-6)
    }

    @Test fun aSquareRootLimit() {
        assertEquals(0.25, lim("(sqrt(x)-2)/(x-4)", 4.0)!!, 1e-6)
    }

    @Test fun aPlainPolynomialIsJustItsValue() {
        assertEquals(7.0, lim("x^2+3", 2.0)!!, 1e-6)
    }

    // ---- one sided ----

    @Test fun theRightHandLimitOfAbsOverX() {
        assertEquals(1.0, lim("abs(x)/x", 0.0, Limits.Side.RIGHT)!!, 1e-6)
    }

    @Test fun theLeftHandLimitOfAbsOverX() {
        assertEquals(-1.0, lim("abs(x)/x", 0.0, Limits.Side.LEFT)!!, 1e-6)
    }

    @Test fun aFunctionWithADifferentLimitFromEachSide() {
        // 1/x runs off in opposite directions, so the two-sided limit is not a number.
        assertTrue(lim("1/x", 0.0, Limits.Side.RIGHT)!!.isInfinite())
        assertTrue(lim("1/x", 0.0, Limits.Side.LEFT)!!.isInfinite())
        assertTrue(lim("1/x", 0.0, Limits.Side.RIGHT)!! > 0)
        assertTrue(lim("1/x", 0.0, Limits.Side.LEFT)!! < 0)
    }

    // ---- divergence ----

    @Test fun anEvenPoleDivergesToPositiveInfinity() {
        val v = lim("1/x^2", 0.0)
        assertTrue("expected divergence, got $v", v != null && v == Double.POSITIVE_INFINITY)
    }

    @Test fun aStraightLineDiverges() {
        val v = lim("x", 100.0)
        assertTrue(v != null && v == Double.POSITIVE_INFINITY)
    }

    // ---- at infinity ----

    @Test fun aRatioOfQuadratics() {
        assertEquals(3.0, Limits.limitAtInfinity("(3*x^2+2*x)/(x^2+1)")!!, 1e-5)
    }

    @Test fun oneOverXVanishes() {
        assertEquals(0.0, Limits.limitAtInfinity("1/x")!!, 1e-6)
    }

    @Test fun aRatioOfCubics() {
        assertEquals(2.5, Limits.limitAtInfinity("(5*x^3-x)/(2*x^3+1)")!!, 1e-5)
    }

    @Test fun aLinearOverLinear() {
        assertEquals(2.0, Limits.limitAtInfinity("(2*x+1)/(x-3)")!!, 1e-5)
    }

    @Test fun theLowerOrderTermWins() {
        assertEquals(0.0, Limits.limitAtInfinity("(x+1)/(x^2+1)")!!, 1e-6)
    }

    @Test fun xItselfDiverges() {
        assertTrue(Limits.limitAtInfinity("x") == Double.POSITIVE_INFINITY)
        assertTrue(Limits.limitAtInfinity("x", negative = true) == Double.NEGATIVE_INFINITY)
    }

    @Test fun oneOverXAtNegativeInfinityIsStillZero() {
        assertEquals(0.0, Limits.limitAtInfinity("1/x", negative = true)!!, 1e-6)
    }

    @Test fun anExpressionThatWillNotParseGivesNull() {
        assertNull(lim("x +", 1.0))
        assertNull(lim("nonsense(x)", 1.0))
    }
}

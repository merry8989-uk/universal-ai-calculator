package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The four trigonometric functions that have holes, and the holes themselves.
 *
 * tan and sec have no value where the cosine is zero — 90°, 270°, -90° — and
 * cot and csc have none where the sine is — 0°, 180°, 360°. Dividing does not
 * reveal this: 90° is not exactly π/2 as a double, so the cosine of it is 6e-17
 * rather than 0 and the division answers 1.6e16, a number no tangent ever
 * takes. The evaluator therefore reads the angle as it was written and refuses
 * at a pole with the word "undefined", which is a fact about the function and
 * not a fault in the app.
 *
 * These tests hold that line for all four functions in all three angle units,
 * and check that every value which is not a pole comes back exactly as it did
 * before the change.
 */
class TrigPolesTest {

    private fun ev(e: String, unit: AngleUnit = AngleUnit.DEGREE) =
        Evaluator.evaluate(e, unit = unit)

    /**
     * The message [e] is refused with, or "" when it produced a number.
     *
     * Only an [Evaluator.UndefinedValue] is caught, so an expression that still
     * overflows — the old answer — fails the test rather than quietly reading
     * as "not undefined".
     */
    private fun refusal(e: String, unit: AngleUnit = AngleUnit.DEGREE): String = try {
        Evaluator.evaluate(e, unit = unit)
        ""
    } catch (ex: Evaluator.UndefinedValue) {
        ex.message ?: ""
    }

    // ---- tan and sec: the zeros of the cosine ----

    @Test fun tanOfARightAngleInDegreesIsUndefined() {
        assertEquals("undefined", refusal("tan(90)"))
    }

    @Test fun tanIsUndefinedOnEveryQuarterTurnThatZeroesTheCosine() {
        for (angle in listOf("270", "-90", "450", "-270", "630")) {
            assertEquals("undefined", refusal("tan($angle)"))
        }
    }

    @Test fun secOfARightAngleIsUndefinedToo() {
        for (angle in listOf("90", "270", "-90")) {
            assertEquals("undefined", refusal("sec($angle)"))
        }
    }

    // ---- cot and csc: the zeros of the sine ----

    @Test fun cotOfZeroIsUndefinedRatherThanAnOverflow() {
        // The old answer was "Result is infinite (overflow)", which reads as a
        // fault. The test would fail on it, because that is an EvalException
        // and not an UndefinedValue.
        assertEquals("undefined", refusal("cot(0)"))
    }

    @Test fun cotIsUndefinedOnEveryHalfTurnThatZeroesTheSine() {
        for (angle in listOf("180", "360", "-180", "-360", "540")) {
            assertEquals("undefined", refusal("cot($angle)"))
        }
    }

    @Test fun cscOfZeroIsUndefinedToo() {
        for (angle in listOf("0", "180", "-180")) {
            assertEquals("undefined", refusal("csc($angle)"))
        }
    }

    // ---- one convention, not four ----

    @Test fun allFourSayTheSameWordAndItIsPlainProse() {
        val poles = listOf("tan(90)", "sec(90)", "cot(0)", "csc(0)")
        for (expression in poles) {
            val message = refusal(expression)
            assertEquals("$expression should say undefined", "undefined", message)
            // Nothing about it should read like a crash or a size problem.
            assertTrue(
                "$expression should not read like a fault: $message",
                !message.contains("overflow", ignoreCase = true) &&
                    !message.contains("infinite", ignoreCase = true) &&
                    !message.contains("error", ignoreCase = true),
            )
        }
    }

    @Test fun aRefusalIsStillAnEvalExceptionSoExistingCallersKeepWorking() {
        // Every screen already catches EvalException; the new type rides on it
        // rather than replacing it, so no caller has to learn a second catch.
        try {
            Evaluator.evaluate("tan(90)", unit = AngleUnit.DEGREE)
            fail("tan(90) should have been refused")
        } catch (ex: Evaluator.EvalException) {
            assertEquals("undefined", ex.message)
        }
    }

    @Test fun thePolesAreFoundInGradiansAndRadiansAsWell() {
        assertEquals("undefined", refusal("tan(100)", AngleUnit.GRADIAN))
        assertEquals("undefined", refusal("sec(300)", AngleUnit.GRADIAN))
        assertEquals("undefined", refusal("cot(200)", AngleUnit.GRADIAN))
        assertEquals("undefined", refusal("csc(0)", AngleUnit.GRADIAN))
        assertEquals("undefined", refusal("tan(pi/2)", AngleUnit.RADIAN))
        assertEquals("undefined", refusal("sec(3*pi/2)", AngleUnit.RADIAN))
        assertEquals("undefined", refusal("cot(pi)", AngleUnit.RADIAN))
        assertEquals("undefined", refusal("csc(0)", AngleUnit.RADIAN))
    }

    @Test fun anUndefinedPartMakesTheWholeExpressionUndefined() {
        // One convention, applied wherever the pole is reached: a sum that
        // contains tan(90) has no value either.
        for (expression in listOf("2*tan(90)", "tan(90)+1", "1+cot(0)", "sqrt(sec(90))")) {
            assertEquals("undefined", refusal(expression))
        }
    }

    // ---- everything that is not a pole is untouched ----

    @Test fun ordinaryTangentsAreUnchanged() {
        assertEquals(0.0, ev("tan(0)"), 1e-12)
        assertEquals(0.5773502691896257, ev("tan(30)"), 1e-12)
        assertEquals(1.0, ev("tan(45)"), 1e-12)
        assertEquals(1.7320508075688767, ev("tan(60)"), 1e-12)
        assertEquals(-1.0, ev("tan(135)"), 1e-12)
        assertEquals(1.0, ev("tan(pi/4)", AngleUnit.RADIAN), 1e-12)
        assertEquals(0.0, ev("tan(0)", AngleUnit.GRADIAN), 1e-12)
    }

    @Test fun ordinaryReciprocalsAreUnchanged() {
        assertEquals(1.0, ev("cot(45)"), 1e-12)
        assertEquals(2.0, ev("sec(60)"), 1e-12)
        assertEquals(2.0, ev("csc(30)"), 1e-12)
        assertEquals(1.0, ev("sec(0)"), 1e-12)
        assertEquals(1.0, ev("csc(90)"), 1e-12)
        // cot(90) is the cosine over the sine, which is zero and not a pole.
        assertEquals(0.0, ev("cot(90)"), 1e-12)
        assertEquals(-1.0, ev("sec(180)"), 1e-12)
        assertEquals(-1.0, ev("csc(-90)"), 1e-12)
    }

    @Test fun sineAndCosineAreUntouched() {
        // They have no poles, so nothing about them should have moved.
        assertEquals(1.0, ev("sin(90)"), 1e-12)
        assertEquals(0.0, ev("cos(90)"), 1e-12)
        assertEquals(0.0, ev("sin(0)"), 1e-12)
        assertEquals(1.0, ev("cos(0)"), 1e-12)
        assertEquals(0.5, ev("sin(30)"), 1e-12)
    }

    @Test fun anAngleCloseToAPoleIsStillAnOrdinaryValue() {
        // A tenth of a millionth of a degree short of 90° is not 90°: it has a
        // tangent, a large one, and the calculator should still give it.
        val near = ev("tan(89.9999999)")
        assertTrue("a near-pole must still be a number: $near", near.isFinite())
        assertTrue("a near-pole must still be large: $near", near > 1e8)
    }

    @Test fun anglesThatOnlyLookLikePolesAreNotTreatedAsOnes() {
        // 90.0000001° and 0.0000001° are ordinary angles, not holes.
        assertEquals(0.0, ev("cot(90)"), 1e-12)
        assertTrue(ev("cot(0.0000001)").isFinite())
        assertTrue(ev("csc(0.0000001)").isFinite())
        assertTrue(ev("tan(90.0000001)").isFinite())
        assertTrue(ev("sec(90.0000001)").isFinite())
    }
}

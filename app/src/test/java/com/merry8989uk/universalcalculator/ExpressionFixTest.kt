package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.ExpressionFix
import com.merry8989uk.universalcalculator.calc.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two things that used to make the calculator shout "error": a bracket left
 * open and an operator pressed with nothing after it. Both are now completed.
 */
class ExpressionFixTest {

    private fun value(text: String, degrees: Boolean = false): Double =
        Evaluator.evaluate(ExpressionFix.balance(text), degrees)

    // ---- completing a half-typed expression ----

    @Test fun anOpenBracketIsClosed() {
        assertEquals("sin(30)", ExpressionFix.balance("sin(30"))
        assertEquals("(5+3)", ExpressionFix.balance("(5+3"))
        assertEquals("((1+2))", ExpressionFix.balance("((1+2)"))
    }

    @Test fun aTrailingOperatorIsDropped() {
        assertEquals("5", ExpressionFix.balance("5+"))
        assertEquals("5", ExpressionFix.balance("5*"))
        assertEquals("2", ExpressionFix.balance("2^"))
        assertEquals("5+3", ExpressionFix.balance("5+3-"))
    }

    @Test fun aBracketNobodyFilledInIsRemoved() {
        assertEquals("", ExpressionFix.balance("sin("))
        assertEquals("5", ExpressionFix.balance("5+sin("))
        assertEquals("", ExpressionFix.balance("("))
    }

    @Test fun aBracketThatClosesNothingIsRemoved() {
        assertEquals("5+3", ExpressionFix.balance("5+3)"))
        assertEquals("(5+3)", ExpressionFix.balance("(5+3))"))
    }

    @Test fun postfixOperatorsAreLeftAlone() {
        // % and ! complete the number, so stripping them would change the answer
        assertEquals("50%", ExpressionFix.balance("50%"))
        assertEquals("5!", ExpressionFix.balance("5!"))
        assertEquals(0.5, value("50%"), 1e-12)
        assertEquals(120.0, value("5!"), 1e-12)
    }

    @Test fun aCompleteExpressionIsUntouched() {
        for (text in listOf("1+1", "sin(30)", "2*(3+4)", "-5", "10/4", "nCr(5,2)")) {
            assertEquals(text, ExpressionFix.balance(text))
        }
    }

    @Test fun anEmptyExpressionStaysEmpty() {
        assertEquals("", ExpressionFix.balance(""))
        assertEquals("", ExpressionFix.balance("   "))
        assertEquals("", ExpressionFix.balance("+"))
    }

    // ---- the failures this was written for ----

    @Test fun whatUsedToBeAnErrorNowGivesAnAnswer() {
        // each of these threw before: "Missing ')'" or "Unexpected end of expression"
        // trig reads degrees, which is how the keypad is set up
        assertEquals(0.5, value("sin(30", degrees = true), 1e-12)
        assertEquals(8.0, value("(5+3"), 1e-12)
        assertEquals(5.0, value("5+"), 1e-12)
        assertEquals(8.0, value("5+3)"), 1e-12)
    }

    @Test fun theDrawerKeysEvaluateOnceAnArgumentIsTyped() {
        // every function key opens a bracket; typing a number and pressing = works
        val cases = mapOf(
            "sin(30" to 0.5, "csc(30" to 2.0, "log2(8" to 3.0,
            "gamma(5" to 24.0, "gcd(12,18" to 6.0, "nthroot(27,3" to 3.0,
            "mean(1,2,3" to 2.0, "erf(1" to 0.8427007929497149,
        )
        cases.forEach { (text, expected) ->
            // degrees only changes the trig cases; the rest are unaffected
            assertEquals("$text should evaluate", expected, value(text, degrees = true), 1e-9)
        }
    }

    // ---- the ± key ----

    @Test fun negateWrapsTheTrailingOperand() {
        assertEquals("(-5)", ExpressionFix.negate("5"))
        assertEquals("2+(-3)", ExpressionFix.negate("2+3"))
        assertEquals("2*(-3)", ExpressionFix.negate("2*3"))
        assertEquals("(-5!)", ExpressionFix.negate("5!"))
    }

    @Test fun negateTwicePutsItBack() {
        for (text in listOf("5", "2+3", "2*3", "5!")) {
            assertEquals(text, ExpressionFix.negate(ExpressionFix.negate(text)))
        }
    }

    @Test fun negateOnAnEmptyOrOpenExpressionAddsAMinus() {
        assertEquals("-", ExpressionFix.negate(""))
        assertEquals("2*-", ExpressionFix.negate("2*"))
        assertEquals("5+-", ExpressionFix.negate("5+"))
    }

    @Test fun negatedExpressionsStillEvaluate() {
        assertEquals(-5.0, value(ExpressionFix.negate("5")), 1e-12)
        assertEquals(-1.0, value(ExpressionFix.negate("2+3")), 1e-12)
        assertEquals(-6.0, value(ExpressionFix.negate("2*3")), 1e-12)
        // the sign key then = on a half-typed function still works
        assertEquals(-0.5, value(ExpressionFix.negate("sin(30"), degrees = true), 1e-12)
    }

    // ---- the decimals setting ----

    @Test fun theChosenNumberOfDecimalsIsRespected() {
        val third = Format.number(1.0 / 3.0)
        assertEquals("0.33", Format.grouped(third, "None", 2))
        assertEquals("0.3333", Format.grouped(third, "None", 4))
        assertEquals("0", Format.grouped(third, "None", 0))
    }

    @Test fun groupingAndDecimalsWorkTogether() {
        assertEquals("1,23,456.79", Format.grouped("123456.789", "Indian", 2))
        assertEquals("123,456.79", Format.grouped("123456.789", "International", 2))
        assertEquals("123.456,79", Format.grouped("123456.789", "European", 2))
    }

    @Test fun aWholeNumberIsNotGivenPointlessZeros() {
        // 2 decimals on an exact answer reads as 12, not 12.00
        assertEquals("12.00", Format.grouped("12", "None", 2))
        assertTrue(Format.grouped("12", "None", -1) == "12")
    }
}

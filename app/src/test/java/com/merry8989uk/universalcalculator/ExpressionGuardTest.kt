package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.ExpressionGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The guard that reads an expression the way a person would and says what is
 * wrong with it.
 *
 * The point of these tests is the wording as much as the detection: a message
 * that says "Unexpected 's'" tells the user nothing, and the whole reason the
 * guard exists is that the parser's own message is useless.
 */
class ExpressionGuardTest {

    private fun ok(text: String) {
        assertNull("expected no complaint for '$text'", ExpressionGuard.check(text))
    }

    private fun warns(text: String): String {
        val w = ExpressionGuard.check(text)
        assertNotNull("expected a complaint for '$text'", w)
        assertTrue("the complaint is empty for '$text'", w!!.isNotBlank())
        return w
    }

    // ---------------- the case the user reported ----------------

    @Test fun aFunctionRepeatedIsRefused() {
        val w = warns("sinsinsin")
        assertTrue("the message does not name the problem: '$w'", w.contains("sin"))
        assertTrue("the message does not say what to do: '$w'", w.contains("value"))
    }

    @Test fun twoFunctionsInARowAreNamed() {
        assertTrue(warns("coscos").contains("cos"))
        assertTrue(warns("sinsin").contains("sin"))
        assertTrue(warns("lnln").contains("ln"))
    }

    @Test fun aFunctionWithNothingAfterItIsRefused() {
        assertTrue(warns("sin").contains("value"))
        assertTrue(warns("2+sin").contains("value"))
        // an open bracket is half-typed rather than wrong, which isUnfinished says
        assertTrue(ExpressionGuard.isUnfinished("cos("))
    }

    @Test fun aFunctionFollowedByANameIsRefused() {
        val w = warns("sin x")
        assertTrue("the message reads '$w'", w.contains("sin"))
    }

    @Test fun functionsNestedProperlyAreFine() {
        ok("sin(sin(1))")
        ok("sin(cos(1))")
        ok("ln(exp(2))")
        ok("sin(30)+cos(60)")
    }

    // ---------------- the symbols ----------------

    @Test fun aSymbolItDoesNotKnowIsNamed() {
        assertTrue(warns("2%#$").contains("#"))
        assertTrue(warns("5&3").contains("&"))
        assertTrue(warns("2@3").contains("@"))
        assertTrue(warns("2+3;4").contains(";"))
    }

    @Test fun aWordThatIsNotAFunctionIsNamed() {
        assertTrue(warns("sink").contains("sink"))
        assertTrue(warns("hello").contains("hello"))
    }

    @Test fun theNamesItDoesKnowAreAccepted() {
        for (name in listOf("sin", "cos", "tan", "ln", "log", "sqrt", "abs", "exp", "gamma", "erf")) {
            ok("$name(1)")
        }
    }

    // ---------------- the operators ----------------

    @Test fun twoOperatorsInARowAreRefused() {
        assertTrue(warns("+-+").contains("Two operators"))
        assertTrue(warns("2++3").contains("Two operators"))
        assertTrue(warns("5*/3").contains("Two operators"))
        assertTrue(warns("2^^3").contains("Two operators"))
    }

    @Test fun aSignAfterAnOperatorIsAllowed() {
        // 5*-3 and 2^-1 are how a negative number is written
        ok("5*-3")
        ok("2^-1")
        ok("10/-2")
    }

    @Test fun anExpressionCannotStartWithAnOperator() {
        assertTrue(warns("*5").contains("number before it"))
        assertTrue(warns("/5").contains("number before it"))
        assertTrue(warns("%5").contains("number before it"))
    }

    // ---------------- the things that are fine ----------------

    @Test fun ordinaryArithmeticIsFine() {
        for (e in listOf("2+3", "5-8", "2*3", "10/4", "2^10", "(2+3)*4", "2!", "50%", "1.5+2.5")) {
            ok(e)
        }
    }

    @Test fun theConstantsAndVariablesAreFine() {
        ok("2\u03C0")
        ok("2pi")
        ok("x^2+1")
        ok("ans+1")
        ok("ln(e)")
    }

    @Test fun anEmptyExpressionHasNothingToSay() {
        ok("")
        ok("   ")
    }

    // ---------------- the double sign ----------------

    @Test fun aDoubleSignIsBracketed() {
        assertEquals("5-(-8)", ExpressionGuard.bracketNegativeNumbers("5--8"))
        assertEquals("5+(-3)", ExpressionGuard.bracketNegativeNumbers("5+-3"))
        assertEquals("2*(-3)", ExpressionGuard.bracketNegativeNumbers("2*-3"))
        assertEquals("10/(-2)", ExpressionGuard.bracketNegativeNumbers("10/-2"))
        assertEquals("2^(-1)", ExpressionGuard.bracketNegativeNumbers("2^-1"))
    }

    @Test fun anOrdinarySubtractionIsLeftAlone() {
        assertEquals("5-8", ExpressionGuard.bracketNegativeNumbers("5-8"))
        assertEquals("2-3-4", ExpressionGuard.bracketNegativeNumbers("2-3-4"))
        assertEquals("100-50+10", ExpressionGuard.bracketNegativeNumbers("100-50+10"))
    }

    @Test fun anAlreadyBracketedSignIsNotBracketedAgain() {
        // the bug this test exists for: 3+(-2) becoming 3+((-2))
        assertEquals("3+(-2)", ExpressionGuard.bracketNegativeNumbers("3+(-2)"))
        assertEquals("sin(-30)", ExpressionGuard.bracketNegativeNumbers("sin(-30)"))
        assertEquals("5-(-8)", ExpressionGuard.bracketNegativeNumbers("5-(-8)"))
    }

    @Test fun anOpenBracketIsClosed() {
        assertEquals("5-(-8)", ExpressionGuard.bracketNegativeNumbers("5--8"))
        assertEquals("sin(30)", ExpressionGuard.bracketNegativeNumbers("sin(30"))
        assertEquals("((1+2))", ExpressionGuard.bracketNegativeNumbers("((1+2"))
    }

    @Test fun theBracketedFormStillWorksOut() {
        // the whole point: 5--8 must give 13
        val worked = ExpressionGuard.bracketNegativeNumbers("5--8")
        assertEquals(13.0, com.merry8989uk.universalcalculator.calc.Evaluator.evaluate(worked), 1e-12)
        val worked2 = ExpressionGuard.bracketNegativeNumbers("2*-3")
        assertEquals(-6.0, com.merry8989uk.universalcalculator.calc.Evaluator.evaluate(worked2), 1e-12)
    }

    // ---------------- unfinished ----------------

    @Test fun anUnfinishedExpressionIsRecognised() {
        assertTrue(ExpressionGuard.isUnfinished("2+"))
        assertTrue(ExpressionGuard.isUnfinished("sin("))
        assertTrue(ExpressionGuard.isUnfinished("(2+3"))
        assertFalse(ExpressionGuard.isUnfinished("2+3"))
        assertFalse(ExpressionGuard.isUnfinished("sin(30)"))
        assertFalse(ExpressionGuard.isUnfinished(""))
    }
}

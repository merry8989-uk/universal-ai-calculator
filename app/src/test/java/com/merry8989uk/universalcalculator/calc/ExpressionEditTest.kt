package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The ⇄ swap key and the x variable. */
class ExpressionEditTest {

    @Test fun swapsSimpleOperands() {
        assertEquals("3+5", ExpressionEdit.swapOperands("5+3"))
        assertEquals("3-5", ExpressionEdit.swapOperands("5-3"))
        assertEquals("2*10", ExpressionEdit.swapOperands("10*2"))
        assertEquals("4/20", ExpressionEdit.swapOperands("20/4"))
    }

    @Test fun keepsTheValueWhenPrecedenceDiffers() {
        // the loosest-binding operator is the one that moves, so 11 stays 11
        assertEquals("3*2+5", ExpressionEdit.swapOperands("5+3*2"))
        assertEquals("4*3+2", ExpressionEdit.swapOperands("2+3*4"))
        assertEquals(11.0, Evaluator.evaluate(ExpressionEdit.swapOperands("5+3*2")), 1e-9)
    }

    @Test fun respectsBrackets() {
        assertEquals("3*(1+2)", ExpressionEdit.swapOperands("(1+2)*3"))
        assertEquals("(3+4)+(1+2)", ExpressionEdit.swapOperands("(1+2)+(3+4)"))
    }

    @Test fun handlesUnaryMinus() {
        assertEquals("3+(-5)", ExpressionEdit.swapOperands("-5+3"))
        assertEquals(3.0 + -5.0, Evaluator.evaluate(ExpressionEdit.swapOperands("-5+3")), 1e-9)
    }

    @Test fun leavesUnswappableTextAlone() {
        assertEquals("", ExpressionEdit.swapOperands(""))
        assertEquals("42", ExpressionEdit.swapOperands("42"))
        assertEquals("-7", ExpressionEdit.swapOperands("-7"))
        assertEquals("5+", ExpressionEdit.swapOperands("5+"))
    }

    @Test fun swappedExpressionsStillEvaluate() {
        for (e in listOf("5+3", "5+3*2", "(1+2)*3", "-5+3", "10*2", "2^8")) {
            val swapped = ExpressionEdit.swapOperands(e)
            try {
                Evaluator.evaluate(swapped)
            } catch (ex: Exception) {
                throw AssertionError("'$e' swapped to '$swapped' which does not evaluate: ${ex.message}")
            }
        }
    }

    @Test fun detectsTheVariable() {
        assertTrue(ExpressionEdit.usesVariable("x^2+1"))
        assertTrue(ExpressionEdit.usesVariable("2*x"))
        assertTrue(ExpressionEdit.usesVariable("x"))
        assertFalse(ExpressionEdit.usesVariable("2+3"))
        assertFalse(ExpressionEdit.usesVariable("sin(2)"))
        assertFalse(ExpressionEdit.usesVariable("max(1,2)"))
    }
}

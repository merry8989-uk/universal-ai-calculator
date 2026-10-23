package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sinh

class ComplexEvaluatorTest {

    private fun ev(e: String, deg: Boolean = false) = ComplexEvaluator.evaluate(e, deg)

    private fun assertComplex(re: Double, im: Double, e: String, tol: Double = 1e-9) {
        val v = ev(e)
        assertEquals("re of $e", re, v.re, tol)
        assertEquals("im of $e", im, v.im, tol)
    }

    @Test fun detectingTheImaginaryUnit() {
        assertTrue(ComplexEvaluator.hasImaginary("2i"))
        assertTrue(ComplexEvaluator.hasImaginary("3+4i"))
        assertTrue(ComplexEvaluator.hasImaginary("i"))
        assertTrue(ComplexEvaluator.hasImaginary("sqrt(-4)+i"))
        assertFalse(ComplexEvaluator.hasImaginary("sin(0.5)"))
        assertFalse(ComplexEvaluator.hasImaginary("pi"))
        assertFalse(ComplexEvaluator.hasImaginary("2+3"))
        assertFalse(ComplexEvaluator.hasImaginary("log(100)"))
    }

    @Test fun arithmetic() {
        assertComplex(11.0, -2.0, "(3+4i)*(1-2i)")
        assertComplex(-1.0, 2.0, "(3+4i)/(1-2i)")
        assertComplex(3.0, 2.0, "(2+3i)+(1-i)")
        assertComplex(0.0, 2.0, "(1+i)^2")
        assertComplex(-1.0, 0.0, "i^2")
        assertComplex(0.0, -1.0, "1/i")
        assertComplex(-6.0, 0.0, "3i*2i")
        assertComplex(0.0, 2.0, "sqrt(-4)")
        assertComplex(0.0, 2.0, "2i")
        assertComplex(3.0, 2.0, "3+2i")
        assertComplex(6.0, 0.0, "2*3i/i")
    }

    @Test fun transcendentalFunctions() {
        assertComplex(0.0, PI, "ln(-1)", 1e-9)
        assertComplex(-1.0, 0.0, "exp(i*pi)", 1e-9)
        assertComplex(0.0, sinh(1.0), "sin(i)", 1e-9)
        assertComplex(5.0, 0.0, "abs(3+4i)")
        assertEquals(PI / 2.0, ev("arg(i)").re, 1e-9)
        assertComplex(3.0, -4.0, "conj(3+4i)")
    }

    @Test fun realExpressionsStillWork() {
        assertEquals(7.0, ev("3+4").re, 1e-12)
        assertEquals(0.0, ev("3+4").im, 1e-12)
        assertEquals(2.0, ev("sqrt(4)").re, 1e-12)
        assertEquals(1.0, ev("sin(90)", deg = true).re, 1e-9)
        assertEquals(0.5, ev("50%").re, 1e-12)
        assertEquals(120.0, ev("5!").re, 1e-9)
    }

    @Test fun constantsAndDegrees() {
        assertEquals(PI, ev("pi").re, 1e-12)
        assertEquals(0.0, ev("90\u00B0").im, 1e-12)
        assertEquals(PI / 2.0, ev("90\u00B0").re, 1e-9)      // RAD: the degree sign converts
        assertEquals(90.0, ev("90\u00B0", deg = true).re, 1e-9)  // DEG: left alone
    }

    @Test fun errorsAreReported() {
        for (bad in listOf("2+", "(1+2", "i!", "unknownname", "i^", "5i)(")) {
            try {
                ev(bad)
                throw AssertionError("expected an error for '$bad'")
            } catch (e: Evaluator.EvalException) { /* expected */ }
        }
    }

    @Test fun matchesTheDoubleEvaluatorOnRealInput() {
        val cases = listOf("2+3*4", "(2+3)*4", "2^3^2", "sqrt(2)", "ln(e)", "log(1000)", "5!/3!")
        for (c in cases) {
            assertEquals("mismatch on $c", Evaluator.evaluate(c), ev(c).re, 1e-9)
        }
    }
}

package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluatorTest {

    private fun ev(e: String, deg: Boolean = false) = Evaluator.evaluate(e, deg)

    @Test fun arithmetic() {
        assertEquals(7.0, ev("3+4"), 1e-9)
        assertEquals(-1.0, ev("3-4"), 1e-9)
        assertEquals(12.0, ev("3*4"), 1e-9)
        assertEquals(2.5, ev("10/4"), 1e-9)
        assertEquals(8.0, ev("2^3"), 1e-9)
    }

    @Test fun precedenceAndParens() {
        assertEquals(14.0, ev("2+3*4"), 1e-9)
        assertEquals(20.0, ev("(2+3)*4"), 1e-9)
        assertEquals(-9.0, ev("-3^2"), 1e-9)
        assertEquals(16.0, ev("2^2^2"), 1e-9)
    }

    @Test fun percentBehavesLikeACalculator() {
        assertEquals(220.0, ev("200+10%"), 1e-9)
        assertEquals(180.0, ev("200-10%"), 1e-9)
        assertEquals(20.0, ev("200*10%"), 1e-9)
        assertEquals(2000.0, ev("200/10%"), 1e-9)
        assertEquals(0.5, ev("50%"), 1e-9)
        assertEquals(360.0, ev("15%*2400"), 1e-9)
        assertEquals(1180.0, ev("1000+18%"), 1e-9)
        assertEquals(1.5, ev("(100+50)%"), 1e-9)
    }

    @Test fun functions() {
        assertEquals(3.0, ev("sqrt(9)"), 1e-9)
        assertEquals(2.0, ev("cbrt(8)"), 1e-9)
        assertEquals(120.0, ev("5!"), 1e-9)
        assertEquals(1.0, ev("sin(90)", deg = true), 1e-9)
        assertEquals(0.0, ev("cos(90)", deg = true), 1e-9)
        assertEquals(1.0, ev("tan(45)", deg = true), 1e-9)
        assertEquals(2.0, ev("log(100)"), 1e-9)
        assertEquals(0.0, ev("ln(1)"), 1e-9)
        assertEquals(3.0, ev("log(8, 2)"), 1e-9)
        assertEquals(10.0, ev("nCr(5, 2)"), 1e-9)
        assertEquals(20.0, ev("nPr(5, 2)"), 1e-9)
        assertEquals(6.0, ev("gcd(12, 18)"), 1e-9)
        assertEquals(36.0, ev("lcm(12, 18)"), 1e-9)
        assertEquals(1.0, ev("mod(10, 3)"), 1e-9)
        assertEquals(5.0, ev("abs(-5)"), 1e-9)
        assertEquals(5.0, ev("hypot(3, 4)"), 1e-9)
    }

    @Test fun constants() {
        assertEquals(Math.PI, ev("pi"), 1e-9)
        assertEquals(Math.E, ev("e"), 1e-9)
    }

    @Test fun ansVariable() {
        assertEquals(42.0, Evaluator.evaluate("ans", vars = mapOf("ans" to 42.0)), 1e-9)
    }

    @Test fun errorsAreReported() {
        for (bad in listOf("1/0", "2+", "(1+2", "sqrt(", "unknownname", "5!5")) {
            try {
                Evaluator.evaluate(bad)
                throw AssertionError("expected an error for: $bad")
            } catch (e: Evaluator.EvalException) {
                // expected
            }
        }
    }

    @Test fun divisionByZeroAndOverflowMessages() {
        assertTrue(catchMessage("1/0").contains("zero", ignoreCase = true))
    }

    private fun catchMessage(e: String): String = try {
        Evaluator.evaluate(e); ""
    } catch (ex: Exception) {
        ex.message ?: ""
    }
}

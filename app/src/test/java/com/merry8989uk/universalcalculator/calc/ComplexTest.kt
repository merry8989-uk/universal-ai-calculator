package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

class ComplexTest {

    private fun c(re: Double, im: Double) = Complex(re, im)

    @Test fun parsing() {
        assertEquals(c(3.0, 4.0), Complex.parse("3+4i"))
        assertEquals(c(3.0, 4.0), Complex.parse("3 + 4j"))
        assertEquals(c(0.0, 2.0), Complex.parse("2i"))
        assertEquals(c(-5.0, 0.0), Complex.parse("-5"))
        assertEquals(c(1.0, -2.0), Complex.parse("1 - 2i"))
        assertEquals(c(0.0, 1.0), Complex.parse("i"))
        assertEquals(c(0.0, -1.0), Complex.parse("-i"))
    }

    @Test fun parsingRejectsGarbage() {
        for (bad in listOf("", "abc", "3+xi")) {
            try { Complex.parse(bad); throw AssertionError("expected failure for '$bad'") }
            catch (e: Evaluator.EvalException) { /* expected */ }
        }
    }

    @Test fun arithmetic() {
        assertEquals(c(11.0, -2.0), c(3.0, 4.0).times(c(1.0, -2.0)))
        assertEquals(c(2.0, 0.0), c(1.0, 1.0).plus(c(1.0, -1.0)))
        assertEquals(c(-1.0, 2.0), c(3.0, 4.0).div(c(1.0, -2.0)))
        assertEquals(c(0.0, 2.0), c(1.0, 1.0).pow(2))
        assertEquals(c(0.0, 2.0), c(-4.0, 0.0).sqrt())
        val root2 = c(2.0, 0.0).sqrt()
        assertEquals(1.41421356, root2.re, 1e-8)
        assertEquals(0.0, root2.im, 1e-9)
    }

    @Test fun properties() {
        assertEquals(5.0, c(3.0, 4.0).abs(), 1e-9)
        assertEquals(c(3.0, -4.0), c(3.0, 4.0).conj())
        assertEquals(c(-3.0, -4.0), c(3.0, 4.0).neg())
        assertEquals(PI / 4.0, c(1.0, 1.0).arg(), 1e-9)
    }

    @Test fun rendering() {
        assertEquals("3 + 4i", Complex.text(c(3.0, 4.0)))
        assertEquals("3 - 4i", Complex.text(c(3.0, -4.0)))
        assertEquals("i", Complex.text(c(0.0, 1.0)))
        assertEquals("-i", Complex.text(c(0.0, -1.0)))
        assertEquals("5", Complex.text(c(5.0, 0.0)))
        assertEquals("2i", Complex.text(c(0.0, 2.0)))
    }

    @Test fun polarRoundTrip() {
        val (r, a) = Complex.polar(c(3.0, 4.0), degrees = true)
        assertEquals(5.0, r, 1e-9)
        assertEquals(53.1301, a, 1e-3)
        val back = Complex.fromPolar(r, a)
        assertEquals(3.0, back.re, 1e-9)
        assertEquals(4.0, back.im, 1e-9)
    }

    @Test fun divisionByZeroIsRejected() {
        try { c(1.0, 1.0).div(c(0.0, 0.0)); throw AssertionError("expected failure") }
        catch (e: Evaluator.EvalException) { /* expected */ }
    }
}

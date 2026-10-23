package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolicTest {

    private fun p(vararg c: Double) = c

    @Test fun parsing() {
        assertArrayEquals(p(1.0, -5.0, 0.0, 2.0), Symbolic.parse("2x^3 - 5x + 1"), 1e-12)
        assertArrayEquals(p(0.0, 0.0, 1.0), Symbolic.parse("x^2"), 1e-12)
        assertArrayEquals(p(0.0, -1.0), Symbolic.parse("-x"), 1e-12)
        assertArrayEquals(p(0.0, 3.0), Symbolic.parse("3x"), 1e-12)
        assertArrayEquals(p(7.0), Symbolic.parse("7"), 1e-12)
        assertArrayEquals(p(0.0, 1.0), Symbolic.parse("x"), 1e-12)
    }

    @Test fun badInputIsRejected() {
        for (bad in listOf("", "abc", "x^-1", "2x^", "3y")) {
            try { Symbolic.parse(bad); throw AssertionError("expected failure for '$bad'") }
            catch (e: Evaluator.EvalException) { /* expected */ }
        }
    }

    @Test fun calculus() {
        assertArrayEquals(p(-5.0, 0.0, 6.0), Symbolic.derivative(p(1.0, -5.0, 0.0, 2.0)), 1e-12)
        assertArrayEquals(p(0.0, 1.0, -2.5, 0.0, 0.5), Symbolic.integral(p(1.0, -5.0, 0.0, 2.0)), 1e-12)
        assertEquals(7.0, Symbolic.evaluate(p(1.0, -5.0, 0.0, 2.0), 2.0), 1e-12)
        assertEquals(1.0, Symbolic.evaluate(p(1.0, -5.0, 0.0, 2.0), 0.0), 1e-12)  // the constant term
    }

    @Test fun arithmetic() {
        // (1 + x)(1 - x) = 1 - x^2
        assertArrayEquals(p(1.0, 0.0, -1.0), Symbolic.multiply(p(1.0, 1.0), p(1.0, -1.0)), 1e-12)
        assertArrayEquals(p(2.0, 1.0), Symbolic.add(p(1.0, 1.0), p(1.0)), 1e-12)
        assertArrayEquals(p(0.0, 1.0), Symbolic.subtract(p(1.0, 1.0), p(1.0)), 1e-12)
        assertArrayEquals(p(2.0, 4.0), Symbolic.scale(p(1.0, 2.0), 2.0), 1e-12)
    }

    @Test fun rootsOfQuadratics() {
        assertEquals(listOf(1.0, 2.0), Symbolic.roots(p(2.0, -3.0, 1.0)))
        assertEquals(listOf(2.0), Symbolic.roots(p(-4.0, 2.0)))
        assertTrue(Symbolic.roots(p(1.0, 0.0, 1.0)).isEmpty())     // x^2 + 1
    }

    @Test fun rootsOfCubics() {
        assertEquals(listOf(1.0, 2.0, 3.0), Symbolic.roots(p(-6.0, 11.0, -6.0, 1.0)))
        assertEquals(listOf(0.0, 1.0, 2.0), Symbolic.roots(p(0.0, 2.0, -3.0, 1.0)))  // x(x-1)(x-2)
    }

    @Test fun factoring() {
        assertEquals("(x - 1)(x - 2)", Symbolic.factor(p(2.0, -3.0, 1.0)))
        assertEquals("(x^2 + 1)", Symbolic.factor(p(1.0, 0.0, 1.0)))
        assertEquals("(x + 2)(x - 2)", Symbolic.factor(p(-4.0, 0.0, 1.0)))
    }

    @Test fun formatting() {
        assertEquals("2x^3 - 5x + 1", Symbolic.format(p(1.0, -5.0, 0.0, 2.0)))
        assertEquals("x^2 + 1", Symbolic.format(p(1.0, 0.0, 1.0)))
        assertEquals("x - 3", Symbolic.format(p(-3.0, 1.0)))
        assertEquals("0", Symbolic.format(p(0.0)))
        assertEquals("-x", Symbolic.format(p(0.0, -1.0)))
    }

    @Test fun binomialExpansion() {
        assertArrayEquals(p(1.0, 3.0, 3.0, 1.0), Symbolic.expandBinomial(1.0, 1.0, 3), 1e-9)
        assertArrayEquals(p(1.0, 2.0, 1.0), Symbolic.expandBinomial(1.0, 1.0, 2), 1e-9)
    }

    @Test fun degreeAndTrim() {
        assertEquals(3, Symbolic.degree(p(1.0, -5.0, 0.0, 2.0)))
        assertEquals(0, Symbolic.degree(p(4.0, 0.0, 0.0)))
        assertTrue(Symbolic.isZero(p(0.0, 0.0)))
    }
}

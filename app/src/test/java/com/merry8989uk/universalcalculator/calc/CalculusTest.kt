package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculusTest {

    @Test fun derivatives() {
        assertEquals(6.0, Calculus.derivative("x^2", 3.0), 1e-4)
        assertEquals(12.0, Calculus.derivative("x^3", 2.0), 1e-3)
        assertEquals(0.0, Calculus.derivative("5", 2.0), 1e-6)
        assertEquals(12.0, Calculus.secondDerivative("x^3", 2.0), 1e-2)
    }

    @Test fun integrals() {
        assertEquals(1.0 / 3.0, Calculus.integrate("x^2", 0.0, 1.0), 1e-6)
        assertEquals(2.0, Calculus.integrate("x", 0.0, 2.0), 1e-6)
        assertEquals(2.0, Calculus.integrate("1", 0.0, 2.0), 1e-6)
    }

    @Test fun summation() {
        assertEquals(55.0, Calculus.summation("x", 1, 10), 1e-9)
        assertEquals(14.0, Calculus.summation("x^2", 1, 3), 1e-9)
    }
}

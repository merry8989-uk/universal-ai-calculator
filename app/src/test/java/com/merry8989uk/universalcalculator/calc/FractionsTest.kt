package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI

class FractionsTest {

    @Test fun simpleFractions() {
        assertEquals("1/2", Fractions.text(0.5))
        assertEquals("3/4", Fractions.text(0.75))
        assertEquals("1/3", Fractions.text(1.0 / 3.0))
        assertEquals("2/3", Fractions.text(2.0 / 3.0))
        assertEquals("1/8", Fractions.text(0.125))
        assertEquals("5/2", Fractions.text(2.5))
        assertEquals("1/7", Fractions.text(1.0 / 7.0))
        assertEquals("22/7", Fractions.text(22.0 / 7.0))
    }

    @Test fun wholeNumbersHaveNoFractionForm() {
        assertNull(Fractions.text(1.0))
        assertNull(Fractions.text(42.0))
        assertNull(Fractions.text(0.0))
        assertNull(Fractions.text(-3.0))
    }

    @Test fun irrationalsAreRejected() {
        assertNull(Fractions.text(PI))
        assertNull(Fractions.text(Math.E))
        assertNull(Fractions.text(kotlin.math.sqrt(2.0)))
    }

    @Test fun negatives() {
        assertEquals("-1/2", Fractions.text(-0.5))
        assertEquals("-3/4", Fractions.text(-0.75))
        assertEquals("-1/2", Fractions.mixedText(-0.5))
    }

    @Test fun mixedNumbers() {
        assertEquals("2 3/4", Fractions.mixedText(2.75))
        assertEquals("1 1/2", Fractions.mixedText(1.5))
        assertEquals("3/4", Fractions.mixedText(0.75))
        assertNull(Fractions.mixedText(4.0))
    }

    @Test fun denominatorLimitIsRespected() {
        // 1/7 needs a denominator of 7
        assertEquals("1/7", Fractions.text(1.0 / 7.0, maxDen = 7))
        // with a tiny limit there is no good approximation
        assertNull(Fractions.text(1.0 / 7.0, maxDen = 2))
    }

    @Test fun nonFiniteValuesAreSafe() {
        assertNull(Fractions.text(Double.NaN))
        assertNull(Fractions.text(Double.POSITIVE_INFINITY))
        assertNull(Fractions.text(Double.NEGATIVE_INFINITY))
    }

    @Test fun fromDecimalReturnsThePair() {
        val f = Fractions.fromDecimal(0.75)!!
        assertEquals(3L, f.numerator)
        assertEquals(4L, f.denominator)
    }
}

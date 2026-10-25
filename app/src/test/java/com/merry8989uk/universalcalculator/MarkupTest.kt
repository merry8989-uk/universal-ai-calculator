package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Markup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The mark-up key's arithmetic.
 *
 * The distinction being protected: a margin is a share of the *price*, so 25%
 * on a cost of 100 gives 133.33, not 125. Getting this backwards is the classic
 * way to sell at a loss while believing you made a quarter.
 */
class MarkupTest {

    @Test fun aQuarterMarginOnAHundred() {
        // 100 / (1 - 0.25) = 133.333...
        assertEquals(133.333333333, Markup.sellingPrice(100.0, 25.0)!!, 1e-6)
    }

    @Test fun noMarginMeansThePriceIsTheCost() {
        assertEquals(100.0, Markup.sellingPrice(100.0, 0.0)!!, 1e-12)
    }

    @Test fun aMarginOfAHundredIsImpossible() {
        // no price can be all profit
        assertNull(Markup.sellingPrice(100.0, 100.0))
        assertNull(Markup.sellingPrice(100.0, 150.0))
    }

    @Test fun theMarginComesBack() {
        val price = Markup.sellingPrice(100.0, 25.0)!!
        assertEquals(25.0, Markup.margin(100.0, price)!!, 1e-9)
    }

    @Test fun theMarkUpIsADifferentNumber() {
        // the same sale is a 25% margin but a 33.33% mark-up
        val price = Markup.sellingPrice(100.0, 25.0)!!
        assertEquals(33.333333333, Markup.markupPercent(100.0, price)!!, 1e-6)
    }

    @Test fun sellingAtCostIsNoMarginAndNoMarkUp() {
        assertEquals(0.0, Markup.margin(100.0, 100.0)!!, 1e-12)
        assertEquals(0.0, Markup.markupPercent(100.0, 100.0)!!, 1e-12)
    }

    @Test fun nonsenseIsRefusedRatherThanGuessed() {
        assertNull(Markup.sellingPrice(Double.NaN, 10.0))
        assertNull(Markup.sellingPrice(100.0, Double.POSITIVE_INFINITY))
        assertNull(Markup.margin(100.0, 0.0))
        assertNull(Markup.markupPercent(0.0, 100.0))
    }
}

package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.GrandTotal
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The grand total.
 *
 * The bug this exists for: the first version read the answer back out of the
 * text on screen, so `1 234` and `2.8x10^14` both failed to parse and the total
 * stayed silently at zero. The total is now added as a number, never parsed.
 */
class GrandTotalTest {

    @Test fun answersAddUp() {
        var t = 0.0
        t = GrandTotal.add(t, 250.0)
        t = GrandTotal.add(t, 180.0)
        t = GrandTotal.add(t, 180.0)
        assertEquals(610.0, t, 1e-12)
    }

    @Test fun negativeAnswersTakeAway() {
        var t = 100.0
        t = GrandTotal.add(t, -30.0)
        assertEquals(70.0, t, 1e-12)
    }

    @Test fun noAnswerLeavesTheTotalAlone() {
        assertEquals(50.0, GrandTotal.add(50.0, null), 1e-12)
        assertEquals(50.0, GrandTotal.add(50.0, Double.NaN), 1e-12)
        assertEquals(50.0, GrandTotal.add(50.0, Double.POSITIVE_INFINITY), 1e-12)
    }

    @Test fun anOverflowDoesNotWreckTheTotal() {
        assertEquals(10.0, GrandTotal.add(10.0, Double.MAX_VALUE), 1e-12)
    }

    @Test fun pressingAgainStartsFromZero() {
        assertEquals(0.0, GrandTotal.afterPress(610.0, justPressed = true), 1e-12)
        assertEquals(610.0, GrandTotal.afterPress(610.0, justPressed = false), 1e-12)
    }

    @Test fun aFractionalAnswerIsKept() {
        var t = 0.0
        t = GrandTotal.add(t, 0.1)
        t = GrandTotal.add(t, 0.2)
        assertEquals(0.3, t, 1e-12)
    }
}

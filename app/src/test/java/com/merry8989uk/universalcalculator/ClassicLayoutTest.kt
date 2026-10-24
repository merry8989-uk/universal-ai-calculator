package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.CLASSIC_ROWS
import com.merry8989uk.universalcalculator.ui.DISPLAY_WEIGHT_CLASSIC
import com.merry8989uk.universalcalculator.ui.PAD_WEIGHT_CLASSIC
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The classic pad is fifty-four keys on a phone screen. The display has to give
 * up its room for that to fit without the keys being squashed.
 */
class ClassicLayoutTest {

    @Test fun thePadGetsTheLargerShare() {
        assertTrue(
            "the pad must get more room than the display",
            PAD_WEIGHT_CLASSIC > DISPLAY_WEIGHT_CLASSIC,
        )
    }

    @Test fun theDisplayKeepsAFairShare() {
        // not so small that the expression and the answer are crowded
        val share = DISPLAY_WEIGHT_CLASSIC / (DISPLAY_WEIGHT_CLASSIC + PAD_WEIGHT_CLASSIC)
        assertTrue("the display keeps only ${(share * 100).toInt()}%", share > 0.15f)
        assertTrue("the display takes ${(share * 100).toInt()}%", share < 0.35f)
    }

    @Test fun thePadIsBigEnoughToBeWorthTheSpace() {
        assertEquals(54, CLASSIC_ROWS.sumOf { it.size })
    }
}

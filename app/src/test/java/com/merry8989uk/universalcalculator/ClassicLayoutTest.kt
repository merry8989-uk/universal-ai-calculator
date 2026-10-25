package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.CLASSIC_KEY_HEIGHT
import com.merry8989uk.universalcalculator.ui.CLASSIC_PAGE_ONE
import com.merry8989uk.universalcalculator.ui.CLASSIC_PAGE_TWO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The classic pad keeps its keys small on purpose. Fifty-four compact keys read
 * better than a handful of fat ones, and it is what a real scientific calculator
 * looks like — so the pad asks for a fixed row height rather than a share of the
 * screen, and the display takes whatever is left.
 */
class ClassicLayoutTest {

    @Test fun theKeysStayCompact() {
        assertTrue("a ${CLASSIC_KEY_HEIGHT.value}dp row is not compact", CLASSIC_KEY_HEIGHT.value <= 46f)
        assertTrue("a ${CLASSIC_KEY_HEIGHT.value}dp row is too small to tap", CLASSIC_KEY_HEIGHT.value >= 32f)
    }

    @Test fun theWholePadFitsWithRoomToSpare() {
        // nine rows plus the gaps between them
        val rows = CLASSIC_PAGE_ONE.size
        val padHeight = rows * CLASSIC_KEY_HEIGHT.value + (rows - 1) * 2f
        assertTrue("the pad asks for ${padHeight}dp, more than a phone can spare", padHeight <= 430f)
    }

    @Test fun thePadIsNotTheStandardCalculator() {
        // the standard pad uses 56dp keys; the classic one must be smaller
        assertTrue(
            "the classic keys must be smaller than the standard pad's",
            CLASSIC_KEY_HEIGHT.value < 56f,
        )
    }

    @Test fun bothPagesAreTheSameShape() {
        assertEquals(CLASSIC_PAGE_ONE.size, CLASSIC_PAGE_TWO.size)
        CLASSIC_PAGE_ONE.indices.forEach { i ->
            assertEquals(
                "page two row $i does not match page one",
                CLASSIC_PAGE_ONE[i].size, CLASSIC_PAGE_TWO[i].size,
            )
        }
    }
}

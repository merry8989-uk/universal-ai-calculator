package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.CLASSIC_KEY_HEIGHT
import com.merry8989uk.universalcalculator.ui.CLASSIC_PAGE_ONE
import com.merry8989uk.universalcalculator.ui.CLASSIC_PAGE_TWO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test fun everyRowOnEveryPageIsTheSameWidth() {
        for ((name, page) in listOf("one" to CLASSIC_PAGE_ONE, "two" to CLASSIC_PAGE_TWO)) {
            // a row is as wide as its spans add up to, not as many keys as it holds
            val width = page.first().sumOf { it.span }
            page.forEachIndexed { i, row ->
                assertEquals(
                    "page $name row $i is a different width",
                    width, row.sumOf { it.span },
                )
            }
        }
    }

    @Test fun pageTwoHasNoNumberPad() {
        // the digits and the four rules are page one's job. A second copy on
        // page two would waste the page, which is for the functions that do not
        // fit on page one.
        val digits = (0..9).map { it.toString() } + listOf(".", "00", "=", "+", "-", "\u2212")
        val onPageTwo = CLASSIC_PAGE_TWO.flatten()
            .flatMap { listOfNotNull(it.label, it.action) }
        for (d in digits) {
            assertFalse("page two still has '$d'", d in onPageTwo)
        }
    }

    @Test fun pageTwoIsFullOfFunctions() {
        // and it must actually be used, not left half empty
        val keys = CLASSIC_PAGE_TWO.flatten()
        assertTrue("page two has only ${keys.size} keys", keys.size >= 36)
        val tools = keys.count { it.tool != null }
        val typed = keys.count { it.tool == null && it.action.isNotBlank() }
        assertTrue("expected the tools to still be there, found $tools", tools >= 15)
        assertTrue("expected the rest to type something, found $typed", typed >= 15)
    }
}

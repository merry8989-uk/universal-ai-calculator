package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.aiIsVisible
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Turning Ask AI off must take the assistant off every screen — the button in
 * the tab bar, the magic button above the keypad, the voice and photo row, and
 * its own tab. Minimal mode does the same thing for a different reason, so both
 * go through one rule and the two can never disagree.
 */
class AssistantVisibilityTest {

    @Test fun onByDefault() {
        assertTrue(aiIsVisible(aiEnabled = true, minimal = false))
    }

    @Test fun switchingAskAiOffHidesIt() {
        assertFalse(aiIsVisible(aiEnabled = false, minimal = false))
    }

    @Test fun minimalModeHidesItEvenWhenAskAiIsOn() {
        assertFalse(aiIsVisible(aiEnabled = true, minimal = true))
    }

    @Test fun bothOffIsStillHidden() {
        assertFalse(aiIsVisible(aiEnabled = false, minimal = true))
    }

    @Test fun onlyOneCombinationShowsIt() {
        val shown = listOf(true to false, true to true, false to false, false to true)
            .filter { (enabled, minimal) -> aiIsVisible(enabled, minimal) }
        assertTrue("exactly one combination should show the assistant, got $shown", shown.size == 1)
    }
}

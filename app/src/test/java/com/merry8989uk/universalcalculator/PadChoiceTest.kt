package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.Features
import com.merry8989uk.universalcalculator.ui.modeTarget
import com.merry8989uk.universalcalculator.ui.usesClassicPad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The classic scientific pad is built and tested but switched off.
 *
 * The rules it used to follow are still in the code, behind the flag, so that
 * turning it back on restores them exactly. These tests pin the *off* state, so
 * nobody turns it on by accident — and the pad's own tests keep the code honest
 * while it waits.
 */
class PadChoiceTest {

    @Test fun theClassicPadIsSwitchedOff() {
        assertFalse(
            "the classic pad is meant to be off for now; turning it on should be a deliberate change",
            Features.SCI_CLASSIC,
        )
    }

    @Test fun nothingSwapsWhileItIsOff() {
        for (layout in listOf("Modern", "Classic", "Classic Scientific", "")) {
            for (scientific in listOf(true, false)) {
                for (minimal in listOf(true, false)) {
                    assertFalse(
                        "$layout / scientific=$scientific / minimal=$minimal should not swap",
                        usesClassicPad(layout, isScientificTab = scientific, minimal = minimal),
                    )
                }
            }
        }
    }

    @Test fun aClassicPickLandsOnTheScientificTab() {
        // the MODE list still offers "classic"; it must go somewhere sensible
        assertEquals("tab:1", modeTarget("classic", minimal = false))
        assertEquals("tab:1", modeTarget("classic", minimal = true))
    }

    @Test fun theOtherPicksAreUntouched() {
        assertEquals("tab:0", modeTarget("standard", minimal = false))
        assertEquals("tab:1", modeTarget("scientific", minimal = false))
        assertEquals("tool:matrix", modeTarget("matrix", minimal = false))
        assertEquals("tool:matrix", modeTarget("matrix", minimal = true))
    }
}

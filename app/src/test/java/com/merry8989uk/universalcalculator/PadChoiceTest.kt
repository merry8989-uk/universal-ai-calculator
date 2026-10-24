package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.usesClassicPad
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules the tab bar follows.
 *
 * Outside minimal mode the classic pad takes the Scientific tab's place, and it
 * never touches Standard. Inside minimal mode nothing is replaced — the classic
 * pad gets its own tab beside the modern one, so all the calculators are there.
 */
class PadChoiceTest {

    @Test fun theScientificTabSwapsWhenTheSettingIsOn() {
        assertTrue(usesClassicPad("Classic Scientific", isScientificTab = true, minimal = false))
    }

    @Test fun theStandardTabNeverSwaps() {
        assertFalse(usesClassicPad("Classic Scientific", isScientificTab = false, minimal = false))
    }

    @Test fun theOtherLayoutsLeaveBothTabsAlone() {
        for (layout in listOf("Modern", "Classic", "")) {
            assertFalse(layout, usesClassicPad(layout, isScientificTab = true, minimal = false))
            assertFalse(layout, usesClassicPad(layout, isScientificTab = false, minimal = false))
        }
    }

    @Test fun minimalModeNeverReplacesTheScientificTab() {
        // In minimal mode the scientific pad and the classic pad both have a tab,
        // so the setting must not make one of them disappear.
        assertFalse(usesClassicPad("Classic Scientific", isScientificTab = true, minimal = true))
        assertFalse(usesClassicPad("Classic Scientific", isScientificTab = false, minimal = true))
    }

    @Test fun minimalIsTheDefaultForNothing() {
        // the two-argument call still means "not minimal"
        assertTrue(usesClassicPad("Classic Scientific", isScientificTab = true))
    }
}

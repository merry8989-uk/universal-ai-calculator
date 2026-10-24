package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.usesClassicPad
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The classic pad replaces the scientific one. It must never appear on the
 * Standard tab, whatever the setting says.
 */
class PadChoiceTest {

    @Test fun theScientificTabSwapsWhenAsked() {
        assertTrue(usesClassicPad("Classic Scientific", isScientificTab = true))
    }

    @Test fun theStandardTabNeverSwaps() {
        assertFalse(usesClassicPad("Classic Scientific", isScientificTab = false))
    }

    @Test fun theOtherLayoutsLeaveBothTabsAlone() {
        for (layout in listOf("Modern", "Classic", "")) {
            assertFalse(layout, usesClassicPad(layout, isScientificTab = true))
            assertFalse(layout, usesClassicPad(layout, isScientificTab = false))
        }
    }
}

package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.PAD
import com.merry8989uk.universalcalculator.ui.SCIENTIFIC_KEYS
import com.merry8989uk.universalcalculator.ui.label
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The keypad, as a layout rather than as a screen.
 *
 * The order of the rows is the thing the user asked about: the clearing keys at
 * the top, then the memory keys, and only then the digits. These tests pin that
 * order so it cannot drift back.
 */
class KeypadLayoutTest {

    private fun everyKey(): List<String> = PAD.flatten() + SCIENTIFIC_KEYS

    @Test fun everyRowIsTheSameWidth() {
        val width = PAD.first().size
        PAD.forEachIndexed { i, row ->
            assertEquals("row $i is a different width", width, row.size)
        }
    }

    @Test fun theClearingRowIsFirst() {
        val first = PAD.first()
        assertTrue("C is not in the top row", "C" in first)
        assertTrue("AC is not in the top row", "AC" in first)
        assertTrue("CE is not in the top row", "CE" in first)
        assertTrue("delete is not in the top row", "del" in first)
    }

    @Test fun theMemoryKeysSitAboveTheDigits() {
        val rows = PAD.map { it.toList() }
        val memoryRow = rows.indexOfFirst { "MC" in it && "MR" in it }
        val digitRow = rows.indexOfFirst { "7" in it && "8" in it && "9" in it }
        assertTrue("no memory row", memoryRow >= 0)
        assertTrue("no digit row", digitRow >= 0)
        assertTrue(
            "the memory keys should be above the digits, found at rows $memoryRow and $digitRow",
            memoryRow < digitRow,
        )
    }

    @Test fun theMemoryRowIsDirectlyAboveTheDigits() {
        val rows = PAD.map { it.toList() }
        val memoryRow = rows.indexOfFirst { "MC" in it && "MR" in it }
        val digitRow = rows.indexOfFirst { "7" in it && "8" in it && "9" in it }
        assertEquals("nothing should sit between the memory keys and the digits", digitRow, memoryRow + 1)
    }

    @Test fun theClearingRowSitsAboveTheMemoryRow() {
        val rows = PAD.map { it.toList() }
        val clearRow = rows.indexOfFirst { "C" in it && "AC" in it }
        val memoryRow = rows.indexOfFirst { "MC" in it && "MR" in it }
        assertTrue("the clearing row should come first", clearRow < memoryRow)
    }

    @Test fun theThreeKeysTheUserAskedForAreThere() {
        val keys = everyKey()
        for (k in listOf("GT", "MU", "CE")) {
            assertTrue("$k is missing from the keypad", k in keys)
        }
    }

    @Test fun theMemoryKeysAreAllThere() {
        val keys = everyKey()
        for (k in listOf("MC", "M+", "M-", "MR")) {
            assertTrue("$k is missing", k in keys)
        }
    }

    @Test fun clearAndClearAllAreDifferentKeys() {
        val keys = everyKey()
        assertTrue("C is missing", "C" in keys)
        assertTrue("AC is missing", "AC" in keys)
        assertTrue("they must not be the same key", "C" != "AC")
    }

    @Test fun theKeysThatMovedOffThePadAreStillReachable() {
        // the memory row took their place on the number pad
        val sci = SCIENTIFIC_KEYS
        for (k in listOf("neg", "ans", "^")) {
            assertTrue("$k cannot be typed any more", k in sci)
        }
    }

    @Test fun noKeyIsOnTheNumberPadTwice() {
        // a bracket legitimately appears on both the number pad and the constants
        // row, so this only looks for a key repeated inside the number pad
        val keys = PAD.flatten()
        val repeated = keys.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        assertTrue("these keys appear more than once: $repeated", repeated.isEmpty())
    }

    @Test fun everyKeyHasSomethingPrintedOnIt() {
        for (k in everyKey()) {
            assertTrue("$k prints nothing", label(k).isNotBlank())
        }
    }

    @Test fun theNewKeysPrintThemselves() {
        assertEquals("GT", label("GT"))
        assertEquals("MU", label("MU"))
        assertEquals("CE", label("CE"))
        assertEquals("AC", label("AC"))
    }

    @Test fun theMemoryMinusPrintsAMinus() {
        assertEquals("M\u2212", label("M-"))
    }
}

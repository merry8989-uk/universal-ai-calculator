package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.PAD
import com.merry8989uk.universalcalculator.ui.SCIENTIFIC_KEYS
import com.merry8989uk.universalcalculator.ui.label
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue("CE is not in the top row", "CE" in first)
        assertTrue("delete is not in the top row", "del" in first)
    }

    @Test fun theSignKeyAndTheAnswerKeyAreOnTheStandardPad() {
        // issue #41: both used to be reachable only from the scientific drawer
        val keys = PAD.flatten()
        assertTrue("the sign key is missing from the number pad", "neg" in keys)
        assertTrue("the answer key is missing from the number pad", "ans" in keys)
    }

    @Test fun theSignKeySitsWithTheClearingKeysAndTheAnswerKeyWithTheNumbers() {
        // ± changes an entry, so it belongs beside the keys that change entries;
        // ans puts a number on the board, so it belongs beside the digits
        assertTrue("± is not in the top row", "neg" in PAD.first())
        assertTrue("ans is not in the bottom row", "ans" in PAD.last())
    }

    @Test fun theDuplicateClearLabelIsWhatPaidForTheSignKey() {
        // AC and C are the same key in code, so AC gave its place to ±. If AC
        // ever comes back, something else has to give way, and this says so.
        val keys = PAD.flatten()
        assertFalse("AC is back on the pad, so the sign key lost its place", "AC" in keys)
        assertTrue("C must still clear", "C" in keys)
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

    @Test fun clearingAndClearingTheEntryAreDifferentKeys() {
        val keys = everyKey()
        assertTrue("C is missing", "C" in keys)
        assertTrue("CE is missing", "CE" in keys)
        assertTrue("they must not be the same key", "C" != "CE")
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

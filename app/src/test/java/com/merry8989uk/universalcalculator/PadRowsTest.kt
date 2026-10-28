package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.PAD
import com.merry8989uk.universalcalculator.ui.PadRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one key that may come off the pad, and the one key that may grow.
 *
 * Everything else is pinned: a change here that moved any other key would be a
 * layout regression the user never asked for.
 */
class PadRowsTest {

    private val bottomRow = listOf("0", "00", ".", "GT", "=")

    @Test fun withGtOnTheRowIsUnchanged() {
        assertEquals(bottomRow, PadRows.keysFor(bottomRow, showGt = true))
    }

    @Test fun withGtOffOnlyThatKeyGoes() {
        assertEquals(listOf("0", "00", ".", "="), PadRows.keysFor(bottomRow, showGt = false))
    }

    @Test fun noOtherRowChangesWhenGtIsOff() {
        for (row in PAD) {
            if (PadRows.rowHasGt(row)) continue
            assertEquals(row, PadRows.keysFor(row, showGt = false))
        }
    }

    @Test fun theEqualsKeyStaysWhereItWas() {
        // it is the last key of the bottom row either way
        assertEquals("=", PadRows.keysFor(bottomRow, showGt = false).last())
        assertEquals("=", PadRows.keysFor(bottomRow, showGt = true).last())
    }

    @Test fun equalsIsTwiceAsWideWhenGtIsOff() {
        assertEquals(2f, PadRows.weightFor("=", showGt = false))
        assertEquals(1f, PadRows.weightFor("=", showGt = true))
    }

    @Test fun everyOtherKeyKeepsItsWidth() {
        for (key in listOf("0", "7", "+", "MU", "GT", "AC", "(")) {
            assertEquals("$key changed width", 1f, PadRows.weightFor(key, showGt = false))
            assertEquals("$key changed width", 1f, PadRows.weightFor(key, showGt = true))
        }
    }

    @Test fun thePadHasExactlyOneGtKey() {
        val count = PAD.sumOf { row -> row.count { it == "GT" } }
        assertEquals("the pad should carry one GT key", 1, count)
    }

    /**
     * The layout the user drew, pinned exactly.
     *
     * Column four is the four operators, top to bottom, the way a desk
     * calculator arranges them; everything else sits to its left.
     */
    @Test fun thePadMatchesTheLayoutThatWasDrawn() {
        assertEquals(
            listOf(
                listOf("C", "AC", "CE", "del", "%"),
                listOf("MC", "M+", "M-", "MR", "/"),
                listOf("7", "8", "9", "MU", "*"),
                listOf("4", "5", "6", ")", "-"),
                listOf("1", "2", "3", "(", "+"),
                listOf("0", "00", ".", "GT", "="),
            ),
            PAD,
        )
    }

    @Test fun theRightHandColumnIsTheFourOperators() {
        val right = PAD.map { it.last() }.drop(1).take(4)
        assertEquals(listOf("/", "*", "-", "+"), right)
    }

    @Test fun equalsIsTheLastKeyOfTheLastRow() {
        assertEquals("=", PAD.last().last())
    }

    @Test fun everyRowHasFiveKeys() {
        for ((i, row) in PAD.withIndex()) {
            assertEquals("row $i has ${row.size} keys", 5, row.size)
        }
    }

    @Test fun noKeyAppearsTwice() {
        val all = PAD.flatten()
        val repeated = all.groupingBy { it }.eachCount().filter { it.value > 1 }
        assertTrue("these keys appear more than once: $repeated", repeated.isEmpty())
    }

    @Test fun thePadStillHasEveryDigitAndTheFourOperators() {
        val all = PAD.flatten().toSet()
        for (key in listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")) {
            assertTrue("digit $key is missing", key in all)
        }
        for (op in listOf("+", "-", "*", "/")) {
            assertTrue("operator $op is missing", op in all)
        }
        assertTrue("equals is missing", "=" in all)
    }
}

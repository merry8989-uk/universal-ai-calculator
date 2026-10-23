package com.merry8989uk.universalcalculator.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTapeTest {

    @Test fun newestEntryComesFirst() {
        val stored = "1+1 = 2\n2+2 = 4\n3+3 = 6"
        assertEquals(listOf("3+3 = 6", "2+2 = 4", "1+1 = 2"), tapeEntries(stored))
    }

    @Test fun blankLinesAndTrailingNewlinesAreIgnored() {
        val stored = "\n1+1 = 2\n\n\n2+2 = 4\n"
        assertEquals(listOf("2+2 = 4", "1+1 = 2"), tapeEntries(stored))
    }

    @Test fun anEmptyHistoryGivesNoEntries() {
        assertTrue(tapeEntries("").isEmpty())
        assertTrue(tapeEntries("\n\n").isEmpty())
    }

    @Test fun anEntryIsSplitBackIntoItsExpression() {
        // The tape loads everything before the last " = " back onto the board.
        val entry = "1250+3750 = 5000"
        assertEquals("1250+3750", entry.substringBeforeLast(" = ").trim())
    }

    @Test fun anExpressionWithEqualsInsideItStillLoads() {
        // "a = b" typed by hand, then evaluated, must not lose its first part.
        val entry = "x = 5 = 5"
        assertEquals("x = 5", entry.substringBeforeLast(" = ").trim())
    }
}

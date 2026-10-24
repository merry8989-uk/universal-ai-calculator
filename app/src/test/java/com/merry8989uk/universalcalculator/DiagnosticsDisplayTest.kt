package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The live log shows a clock, not a stopwatch. The milliseconds are kept in the
 * stored line but must not reach the screen, where they read as a slow response.
 */
class DiagnosticsDisplayTest {

    @Test fun theMillisecondsAreDroppedOnScreen() {
        val stored = "2026-10-24 14:32:05.847 [Calc] 2+2 = 4"
        assertEquals("2026-10-24 14:32:05 [Calc] 2+2 = 4", DiagnosticsLogger.displayLine(stored))
    }

    @Test fun theRestOfTheLineIsUntouched() {
        val stored = "2026-10-24 09:00:01.001 [AI] Answer received (245 chars)"
        val shown = DiagnosticsLogger.displayLine(stored)
        assertTrue(shown.contains("[AI] Answer received (245 chars)"))
        assertTrue(shown.startsWith("2026-10-24 09:00:01 "))
    }

    @Test fun aLineWithNoTimestampIsLeftAlone() {
        assertEquals("plain message", DiagnosticsLogger.displayLine("plain message"))
    }

    @Test fun theStoredFormKeepsItsMilliseconds() {
        val stored = "2026-10-24 14:32:05.847 [Calc] 1+1 = 2"
        assertTrue(stored.contains(".847"))
        assertFalse(DiagnosticsLogger.displayLine(stored).contains(".847"))
    }

    @Test fun onlyTheFirstTimestampIsStripped() {
        val stored = "2026-10-24 14:32:05.847 [Calc] 1.500 * 2 = 3"
        assertEquals("2026-10-24 14:32:05 [Calc] 1.500 * 2 = 3", DiagnosticsLogger.displayLine(stored))
    }
}

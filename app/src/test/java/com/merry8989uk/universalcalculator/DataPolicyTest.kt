package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.data.DataPolicy
import com.merry8989uk.universalcalculator.data.HistoryRetention
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The master switch, and the three-day rule on the history.
 *
 * The point of the master switch is that a user who wants nothing kept should
 * not have to find four separate toggles. These tests hold that line: with it
 * off, every individual setting underneath is irrelevant.
 */
class DataPolicyTest {

    // ---- the master switch wins ----

    @Test fun withSavingOffNothingIsStoredWhateverTheOtherSwitchesSay() {
        assertFalse(DataPolicy.mayStore(false))
        assertFalse(DataPolicy.mayArchive(false))
        assertFalse(DataPolicy.mayKeepHistory(false, historyEnabled = true))
        assertFalse(DataPolicy.mayLog(false, diagnosticsEnabled = true))
    }

    @Test fun withSavingOnTheIndividualSwitchesDecide() {
        assertTrue(DataPolicy.mayStore(true))
        assertTrue(DataPolicy.mayArchive(true))
        assertTrue(DataPolicy.mayKeepHistory(true, historyEnabled = true))
        assertFalse(DataPolicy.mayKeepHistory(true, historyEnabled = false))
        assertTrue(DataPolicy.mayLog(true, diagnosticsEnabled = true))
        assertFalse(DataPolicy.mayLog(true, diagnosticsEnabled = false))
    }

    // ---- the history keeps three days ----

    private val now = 1_700_000_000_000L
    private val day = 24L * 60L * 60L * 1000L

    @Test fun anEntryFromTodayIsKept() {
        val kept = HistoryRetention.keep(listOf("$now|2+2 = 4"), now)
        assertEquals(1, kept.size)
        assertTrue(kept[0].endsWith("2+2 = 4"))
    }

    @Test fun anEntryFromTwoDaysAgoIsKept() {
        val old = now - 2 * day
        assertEquals(1, HistoryRetention.keep(listOf("$old|3+3 = 6"), now).size)
    }

    @Test fun anEntryOlderThanThreeDaysIsDropped() {
        val old = now - 4 * day
        assertTrue(HistoryRetention.keep(listOf("$old|4+4 = 8"), now).isEmpty())
    }

    @Test fun theBoundaryIsThreeDaysNotFour() {
        val justInside = now - 3 * day + 1000
        val justOutside = now - 3 * day - 1000
        assertEquals(1, HistoryRetention.keep(listOf("$justInside|a"), now).size)
        assertTrue(HistoryRetention.keep(listOf("$justOutside|a"), now).isEmpty())
    }

    @Test fun anUnstampedLineIsGivenTodaysDateRatherThanDropped() {
        // this is what an upgrade looks like: the old lines have no stamp, and
        // losing them all at once would be a nasty surprise
        val kept = HistoryRetention.keep(listOf("2+2 = 4"), now)
        assertEquals(1, kept.size)
        assertEquals("2+2 = 4", HistoryRetention.text(kept[0], now))
        assertTrue(kept[0].startsWith("$now|"))
    }

    @Test fun theOrderIsKept() {
        val kept = HistoryRetention.keep(
            listOf("${now - 2 * day}|first", "$now|second"),
            now,
        )
        assertEquals(2, kept.size)
        assertTrue(kept[0].endsWith("first"))
        assertTrue(kept[1].endsWith("second"))
    }

    @Test fun theListIsCapped() {
        val lines = (1..600).map { "$now|entry $it" }
        assertEquals(HistoryRetention.MAX_ENTRIES, HistoryRetention.keep(lines, now).size)
    }

    @Test fun theTextOfALineIsWhatTheUserWrote() {
        assertEquals("250*12 = 3,000", HistoryRetention.text("$now|250*12 = 3,000", now))
    }

    @Test fun aStoredLineCarriesItsMoment() {
        assertEquals("$now|2+2 = 4", HistoryRetention.stored("2+2 = 4", now))
    }

    @Test fun aNewlineInAnEntryCannotBreakTheFile() {
        assertEquals("$now|a b", HistoryRetention.stored("a\nb", now))
    }

    @Test fun anEmptyListStaysEmpty() {
        assertTrue(HistoryRetention.keep(emptyList(), now).isEmpty())
        assertTrue(HistoryRetention.keep(listOf("", "   "), now).isEmpty())
    }
}

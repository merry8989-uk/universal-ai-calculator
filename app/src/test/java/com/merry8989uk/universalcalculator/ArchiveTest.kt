package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.ArchiveEntry
import com.merry8989uk.universalcalculator.calc.ArchiveFormat
import com.merry8989uk.universalcalculator.calc.ArchiveSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The calculation archive: the record that survives a cleared history.
 *
 * The two things being protected here are the financial year boundary — a day
 * either side moves a whole year's takings — and the difference between counting
 * calculations and adding up their results.
 */
class ArchiveTest {

    private fun entry(
        date: String,
        time: String,
        expression: String,
        result: String,
        value: Double?,
    ) = ArchiveEntry(
        at = LocalDateTime.of(LocalDate.parse(date), java.time.LocalTime.parse(time)),
        kind = "Standard",
        expression = expression,
        result = result,
        value = value,
    )

    // ---------------- the financial year ----------------

    @Test fun theFinancialYearRunsFromAprilToMarch() {
        // the last day of one year, and the first of the next
        assertEquals(2025 to 2026, ArchiveFormat.financialYear(LocalDate.of(2026, 3, 31)))
        assertEquals(2026 to 2027, ArchiveFormat.financialYear(LocalDate.of(2026, 4, 1)))
    }

    @Test fun aDayInTheMiddleBelongsToItsYear() {
        assertEquals(2026 to 2027, ArchiveFormat.financialYear(LocalDate.of(2026, 10, 26)))
        assertEquals(2026 to 2027, ArchiveFormat.financialYear(LocalDate.of(2027, 3, 31)))
        assertEquals(2027 to 2028, ArchiveFormat.financialYear(LocalDate.of(2027, 4, 1)))
    }

    @Test fun theYearIsNamedTheWayTheUserDrewIt() {
        assertEquals("FY 2026-27", ArchiveFormat.financialYearName(LocalDate.of(2026, 10, 26)))
        assertEquals("FY 2025-26", ArchiveFormat.financialYearName(LocalDate.of(2026, 1, 15)))
    }

    // ---------------- the names ----------------

    @Test fun theDayFileIsNamedAfterTheDay() {
        assertEquals("01 October 2026.txt", ArchiveFormat.dayFile(LocalDate.of(2026, 10, 1)))
    }

    @Test fun theMonthSummaryIsNamedAfterTheMonth() {
        assertEquals(
            "Monthly summary - October 2026.txt",
            ArchiveFormat.monthFile(LocalDate.of(2026, 10, 15)),
        )
    }

    @Test fun theFolderNamesAreTheMonthsAndTheYears() {
        assertEquals("October", ArchiveFormat.monthName(LocalDate.of(2026, 10, 1)))
        assertEquals("2026", LocalDate.of(2026, 10, 1).year.toString())
    }

    // ---------------- the line format ----------------

    @Test fun aLineSurvivesTheRoundTrip() {
        val e = entry("2026-10-26", "09:08:50", "9500*2", "19,000", 19000.0)
        val back = ArchiveFormat.parse(LocalDate.of(2026, 10, 26), ArchiveFormat.line(e))
        assertEquals(1, back.size)
        assertEquals("9500*2", back[0].expression)
        assertEquals("19,000", back[0].result)
        assertEquals(19000.0, back[0].value!!, 1e-9)
        assertEquals(e.at, back[0].at)
    }

    @Test fun aLineWithoutANumberStillReads() {
        val e = entry("2026-10-26", "09:08:50", "1/0", "Cannot divide by zero", null)
        val back = ArchiveFormat.parse(LocalDate.of(2026, 10, 26), ArchiveFormat.line(e))
        assertEquals(1, back.size)
        assertNull(back[0].value)
        assertEquals("Cannot divide by zero", back[0].result)
    }

    @Test fun aHalfWrittenLineIsSkippedAndTheRestSurvives() {
        // this is what a crash mid-write leaves behind
        val good = ArchiveFormat.line(entry("2026-10-26", "09:00:00", "2+2", "4", 4.0))
        val half = "09:01:00|Standard|3+3"
        val text = good + "\n" + half
        val back = ArchiveFormat.parse(LocalDate.of(2026, 10, 26), text)
        assertEquals("the good line should still be there", 1, back.size)
        assertEquals("2+2", back[0].expression)
    }

    @Test fun aBarInTheExpressionCannotBreakTheFile() {
        val e = entry("2026-10-26", "09:00:00", "a|b", "1", 1.0)
        val back = ArchiveFormat.parse(LocalDate.of(2026, 10, 26), ArchiveFormat.line(e))
        assertEquals(1, back.size)
        assertEquals(1.0, back[0].value!!, 1e-9)
    }

    // ---------------- counting and adding are different ----------------

    @Test fun theCountAndTheSumAreNotTheSameThing() {
        val entries = listOf(
            entry("2026-10-01", "09:00:00", "2+2", "4", 4.0),
            entry("2026-10-01", "09:01:00", "3+3", "6", 6.0),
            entry("2026-10-01", "09:02:00", "1/0", "error", null),
        )
        assertEquals(3, ArchiveSummary.count(entries))
        assertEquals(10.0, ArchiveSummary.sum(entries), 1e-9)
        assertEquals(2, ArchiveSummary.howManyHaveANumber(entries))
    }

    @Test fun theDaysAreCountedSeparately() {
        val entries = listOf(
            entry("2026-10-01", "09:00:00", "1+1", "2", 2.0),
            entry("2026-10-01", "09:05:00", "2+2", "4", 4.0),
            entry("2026-10-02", "10:00:00", "3+3", "6", 6.0),
        )
        val days = ArchiveSummary.daysOf(entries)
        assertEquals(2, days.size)
        assertEquals(LocalDate.of(2026, 10, 1), days[0].date)
        assertEquals(2, days[0].calculations)
        assertEquals(1, days[1].calculations)
    }

    // ---------------- the summaries ----------------

    @Test fun theMonthSummarySaysWhatHappened() {
        val entries = listOf(
            entry("2026-10-01", "09:00:00", "2+2", "4", 4.0),
            entry("2026-10-02", "09:00:00", "3+3", "6", 6.0),
        )
        val text = ArchiveSummary.monthText(LocalDate.of(2026, 10, 1), entries)
        assertTrue(text.contains("Monthly summary - October 2026"))
        assertTrue(text.contains("Calculations: 2"))
        assertTrue(text.contains("Days with calculations: 2"))
        assertTrue(text.contains("01 October 2026"))
        assertTrue(text.contains("02 October 2026"))
    }

    @Test fun theMonthSummaryWarnsWhenItDidNotAddEverything() {
        val entries = listOf(
            entry("2026-10-01", "09:00:00", "2+2", "4", 4.0),
            entry("2026-10-01", "09:01:00", "1/0", "error", null),
        )
        val text = ArchiveSummary.monthText(LocalDate.of(2026, 10, 1), entries)
        assertTrue("the summary should say one was not added: $text", text.contains("not plain numbers"))
    }

    @Test fun theFinancialYearSummaryBreaksDownByMonth() {
        val entries = listOf(
            entry("2026-04-10", "09:00:00", "1+1", "2", 2.0),
            entry("2026-10-01", "09:00:00", "2+2", "4", 4.0),
        )
        val text = ArchiveSummary.financialYearText(2026, entries)
        assertTrue(text.contains("Financial year 2026-27"))
        assertTrue(text.contains("01 April 2026 to 31 March 2027"))
        assertTrue(text.contains("Calculations: 2"))
        assertTrue(text.contains("April 2026"))
        assertTrue(text.contains("October 2026"))
    }

    @Test fun anEmptyMonthSaysSoRatherThanCrashing() {
        val text = ArchiveSummary.monthText(LocalDate.of(2026, 10, 1), emptyList())
        assertTrue(text.contains("Calculations: 0"))
    }
}

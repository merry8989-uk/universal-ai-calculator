package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateCalcTest {

    private fun d(s: String) = LocalDate.parse(s)

    @Test fun dayCounts() {
        assertEquals(365L, DateCalc.daysBetween(d("2024-01-01"), d("2024-12-31")))
        assertEquals(366L, DateCalc.daysBetween(d("2024-01-01"), d("2025-01-01")))
        assertEquals(-365L, DateCalc.daysBetween(d("2024-12-31"), d("2024-01-01")))
        assertEquals(26L, DateCalc.yearsBetween(d("2000-01-01"), d("2026-01-01")))
    }

    @Test fun period() {
        val (y, m, dd) = DateCalc.period(d("2000-01-01"), d("2001-03-15"))
        assertEquals(1, y); assertEquals(2, m); assertEquals(14, dd)
    }

    @Test fun workingDays() {
        // Mon 1 Jan 2024 .. Mon 8 Jan 2024 -> Mon-Fri = 5
        assertEquals(5L, DateCalc.workingDays(d("2024-01-01"), d("2024-01-08")))
        // a full week has 7 calendar days but 5 working days
        assertEquals(5L, DateCalc.workingDays(d("2024-01-01"), d("2024-01-08")))
    }

    @Test fun calendarHelpers() {
        assertTrue(DateCalc.isLeapYear(2024))
        assertFalse(DateCalc.isLeapYear(2023))
        assertTrue(DateCalc.isLeapYear(2000))
        assertFalse(DateCalc.isLeapYear(1900))
        assertEquals(29, DateCalc.daysInMonth(d("2024-02-10")))
        assertEquals(28, DateCalc.daysInMonth(d("2023-02-10")))
        assertEquals(366, DateCalc.dayOfYear(d("2024-12-31")))
        assertEquals(1, DateCalc.isoWeek(d("2024-01-01")))
        assertEquals(4, DateCalc.quarter(d("2024-11-05")))
        assertEquals("Friday", DateCalc.dayName(d("2024-01-05")))
    }

    @Test fun addAndSubtract() {
        assertEquals(d("2024-03-01"), DateCalc.addDays(d("2024-02-28"), 2))
        assertEquals(d("2024-02-29"), DateCalc.addDays(d("2024-03-01"), -1))
        assertEquals(d("2025-02-28"), DateCalc.addMonths(d("2024-02-29"), 12))
        assertEquals(d("2030-02-28"), DateCalc.addYears(d("2025-02-28"), 5))
    }
}

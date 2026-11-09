package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.ArchiveEntry
import com.merry8989uk.universalcalculator.data.HistorySearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Searching the history and the archive.
 *
 * Both screens were a list you could only scroll. Three days of the tape, or a
 * month of the archive, is already more lines than a person will read looking
 * for one of them, so the line worth going back to was effectively lost — the
 * search box is what makes either list usable after the first screenful.
 *
 * What is pinned here is that a line is found by any part of it, sum or answer,
 * that an empty box hides nothing, and that the order the lines were already in
 * survives the filter: the newest line is still at the top after a search.
 */
class HistorySearchTest {

    private val lines = listOf(
        "9500*2 = 19,000",
        "1250+3750 = 5000",
        "rent/3 = 8,333.33",
    )

    private fun entry(expression: String, result: String) = ArchiveEntry(
        at = LocalDateTime.of(LocalDate.parse("2026-10-26"), LocalTime.parse("09:08:50")),
        kind = "Standard",
        expression = expression,
        result = result,
        value = result.replace(",", "").toDoubleOrNull(),
    )

    // ---------------- the history ----------------

    @Test fun aLineIsFoundByPartOfItsSum() {
        assertEquals(listOf("1250+3750 = 5000"), HistorySearch.lines(lines, "3750"))
    }

    @Test fun aLineIsFoundByTheAnswerItCameTo() {
        assertEquals(listOf("9500*2 = 19,000"), HistorySearch.lines(lines, "19,000"))
    }

    @Test fun oneQueryCanMatchMoreThanOneLine() {
        // "5" is in two of the three lines; both are worth showing
        assertEquals(
            listOf("9500*2 = 19,000", "1250+3750 = 5000"),
            HistorySearch.lines(lines, "5"),
        )
    }

    @Test fun caseAndSurroundingSpacesAreIgnored() {
        assertEquals(listOf("rent/3 = 8,333.33"), HistorySearch.lines(lines, "  RENT "))
    }

    @Test fun anEmptyBoxHidesNothing() {
        assertEquals(lines, HistorySearch.lines(lines, ""))
        assertEquals(lines, HistorySearch.lines(lines, "   "))
    }

    @Test fun aSearchKeepsTheOrderTheLinesWereAlreadyIn() {
        // the screen hands the lines over newest first, and a filter must not
        // rearrange them
        assertEquals(lines, HistorySearch.lines(lines, "="))
    }

    @Test fun aQueryThatMatchesNothingGivesAnEmptyList() {
        assertTrue(HistorySearch.lines(lines, "no such sum").isEmpty())
        assertTrue(HistorySearch.lines(emptyList(), "anything").isEmpty())
    }

    @Test fun aBlankQueryMatchesEveryLineAndNothingElseDoes() {
        assertTrue(HistorySearch.matches("1+1 = 2", ""))
        assertFalse(HistorySearch.matches("1+1 = 2", "3"))
    }

    // ---------------- the archive ----------------

    @Test fun anArchiveRowIsFoundByItsExpression() {
        val entries = listOf(entry("9500*2", "19,000"), entry("1250+3750", "5000"))
        assertEquals(listOf("9500*2"), HistorySearch.entries(entries, "9500").map { it.expression })
    }

    @Test fun anArchiveRowIsFoundByItsResult() {
        // the person looking for the answer should not have to know which half
        // of the row it is in
        val entries = listOf(entry("9500*2", "19,000"), entry("1250+3750", "5000"))
        assertEquals(listOf("1250+3750"), HistorySearch.entries(entries, "5000").map { it.expression })
    }

    @Test fun anEmptyBoxHidesNothingInTheArchiveEither() {
        val entries = listOf(entry("9500*2", "19,000"), entry("1250+3750", "5000"))
        assertEquals(entries, HistorySearch.entries(entries, ""))
    }

    @Test fun anArchiveSearchThatMatchesNothingGivesAnEmptyList() {
        val entries = listOf(entry("9500*2", "19,000"))
        assertTrue(HistorySearch.entries(entries, "1250").isEmpty())
    }
}

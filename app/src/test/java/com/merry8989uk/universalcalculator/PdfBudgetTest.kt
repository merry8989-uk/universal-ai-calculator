package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ai.PdfBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How much of a long PDF is worth reading.
 *
 * Every page is a picture and a call to the vision model, so a hundred-page book
 * is not a hundred pages of waiting. The old answer was a flat twenty, which was
 * wrong twice: a forty-page question paper got half its questions answered, and a
 * five-page handout waited for nothing.
 */
class PdfBudgetTest {

    private fun stop(pagesRead: Int, elapsedMs: Long = 0, chars: Int = 0, total: Int) =
        PdfBudget.stopReason(pagesRead, elapsedMs, chars, total)

    @Test fun aShortDocumentIsReadWhole() {
        assertNull("it stopped before the end", stop(0, 0, 0, 5))
        assertNull(stop(4, 30_000, 4_000, 5))
        assertEquals(PdfBudget.Stop.Finished, stop(5, 30_000, 4_000, 5))
    }

    @Test fun aLongDocumentKeepsGoingPastTwentyPages() {
        // the old limit; a forty-page question paper must not stop here
        assertNull("it stopped at twenty pages again", stop(20, 20_000, 8_000, 40))
        assertNull(stop(39, 60_000, 20_000, 40))
        assertEquals(PdfBudget.Stop.Finished, stop(40, 60_000, 20_000, 40))
    }

    @Test fun thePageCapStillExists() {
        assertEquals(PdfBudget.Stop.PageCap, stop(PdfBudget.MAX_PAGES, 0, 0, 500))
    }

    @Test fun aSlowPhoneStopsOnTimeNotOnPages() {
        // one page took so long that waiting for sixty would be unreasonable
        assertEquals(PdfBudget.Stop.Time, stop(3, PdfBudget.TIME_BUDGET_MS, 500, 200))
    }

    @Test fun aWordyDocumentStopsWhenTheTextFillsUp() {
        assertEquals(PdfBudget.Stop.Text, stop(12, 10_000, PdfBudget.TEXT_BUDGET_CHARS, 80))
    }

    @Test fun theCheapestStopWinsWhenTwoApply() {
        // all three budgets exhausted: the page cap is the honest description,
        // because that is the one that would have stopped it anyway
        assertEquals(PdfBudget.Stop.PageCap, stop(PdfBudget.MAX_PAGES, 999_999, 999_999, 500))
    }

    // ---- the note that goes to the model ----

    @Test fun aWholeDocumentSaysSo() {
        assertEquals("Read all 5 pages.", PdfBudget.note(PdfBudget.Stop.Finished, 5, 5))
        assertEquals("Read all 1 page.", PdfBudget.note(PdfBudget.Stop.Finished, 1, 1))
    }

    @Test fun aPartialReadSaysWhatWasMissed() {
        val note = PdfBudget.note(PdfBudget.Stop.PageCap, 60, 145)
        assertTrue("the counts are missing: $note", note.contains("60") && note.contains("145"))
        assertTrue("it does not warn about the rest: $note", note.contains("not read"))
    }

    @Test fun everyKindOfStopSaysSomethingDifferent() {
        val notes = listOf(
            PdfBudget.Stop.PageCap,
            PdfBudget.Stop.Time,
            PdfBudget.Stop.Text,
        ).map { PdfBudget.note(it, 10, 50) }
        assertEquals("two stops give the same sentence", 3, notes.toSet().size)
    }

    @Test fun aPartialReadIsMarkedPartial() {
        assertTrue(PdfBudget.partial(PdfBudget.Stop.PageCap))
        assertTrue(PdfBudget.partial(PdfBudget.Stop.Time))
        assertTrue(!PdfBudget.partial(PdfBudget.Stop.Finished))
    }
}

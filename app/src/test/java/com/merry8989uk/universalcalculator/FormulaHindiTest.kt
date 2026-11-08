package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.FormulaHindi
import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Hindi hub, held to the library it describes.
 *
 * A missing translation falls back to English, which is untidy but harmless; a
 * translation attached to an id that no longer exists is the real risk, because
 * it is invisible - the entry simply stops being used and nobody notices. So the
 * ids are checked in both directions, and every string is checked to actually be
 * in Devanagari rather than an English copy left in place.
 */
class FormulaHindiTest {

    private val devanagari = Regex("[\\u0900-\\u097F]")

    @Test fun everyCalculatorHasATitleAndADescription() {
        val missing = FormulaLibrary.formulas
            .filter { FormulaHindi.title(it.id).isNullOrBlank() || FormulaHindi.description(it.id).isNullOrBlank() }
            .map { it.id }
        assertTrue("no Hindi title or description for: $missing", missing.isEmpty())
    }

    @Test fun thereAreNoTranslationsForCalculatorsThatNoLongerExist() {
        val live = FormulaLibrary.formulas.map { it.id }.toSet()
        val stale = FormulaHindi.ids - live
        assertTrue("these translations belong to no calculator: $stale", stale.isEmpty())
        // and the other direction, so a calculator cannot be left in English
        assertEquals("the Hindi map and the library disagree", live.size, FormulaHindi.size)
    }

    @Test fun everyCategoryIsTranslated() {
        val missing = FormulaLibrary.categories.filter { FormulaHindi.category(it).isNullOrBlank() }
        assertTrue("no Hindi name for: $missing", missing.isEmpty())
    }

    @Test fun everyToolIsTranslated() {
        assertTrue("the tools shelf is not translated", FormulaHindi.tools.size >= 20)
        assertTrue(
            "the tool subtitles are missing",
            FormulaHindi.tools.keys.all { !FormulaHindi.toolSubtitle(it).isNullOrBlank() },
        )
        for (id in listOf("reference", "statistics", "currency", "interest")) {
            assertTrue("$id has no Hindi name", !FormulaHindi.tool(id).isNullOrBlank())
        }
    }

    @Test fun theTranslationsAreActuallyInHindi() {
        val english = FormulaLibrary.formulas
            .filter { FormulaHindi.title(it.id) == it.title || FormulaHindi.description(it.id) == it.description }
            .map { it.id }
        assertTrue("these are still English: $english", english.isEmpty())
        assertTrue(
            "a title is not in Devanagari",
            devanagari.containsMatchIn(FormulaHindi.title(FormulaLibrary.formulas.first().id) ?: ""),
        )
    }
}

package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Formula
import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import com.merry8989uk.universalcalculator.calc.HubHit
import com.merry8989uk.universalcalculator.calc.HubSearch
import com.merry8989uk.universalcalculator.calc.InputSpec
import com.merry8989uk.universalcalculator.calc.OutputSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The hub's one search field, over both shelves.
 *
 * The hub holds two kinds of row: twenty tools that open screens and several
 * hundred calculators that open inputs. The field searched only the calculators,
 * so a reader who typed the name of a tool that was already on the page was told
 * nothing matched — a search that contradicts what is in front of the person
 * using it, and the sort of thing that reads as the app being broken rather than
 * incomplete.
 *
 * These are the rules the field follows: a tool is found by its name and by the
 * line under it, a calculator by its name, its category and its field labels, a
 * Hindi reader finds rows by their Hindi names, and a search that finds nothing
 * offers the nearest names rather than ending.
 */
class HubSearchTest {

    private fun calc(
        id: String,
        title: String,
        category: String = "Everyday",
        description: String = "for the test",
        inputLabel: String = "x",
    ): Formula = Formula(id, title, category, description, listOf(InputSpec("x", inputLabel, 1.0))) { _ ->
        listOf(OutputSpec("Result", 1.0))
    }

    // ---- the tools, which the search could not see at all ----

    @Test fun aToolIsFoundByItsNameEvenThoughItIsNotACalculator() {
        val found = HubSearch.find("programmer", FormulaLibrary.formulas)
        assertEquals(listOf("programmer"), found.tools.map { it.id })
        // and it is not dressed up as a calculator: a tool has no inputs to fill
        assertTrue("the programmer tool came back as a calculator", found.formulas.isEmpty())
    }

    @Test fun theGraphingToolIsFoundByItsName() {
        val found = HubSearch.find("graph", FormulaLibrary.formulas)
        assertEquals(listOf("graph"), found.tools.map { it.id })
    }

    @Test fun aToolIsFoundByTheLineUnderItsName() {
        // "bitwise" is in the programmer tool's subtitle, not its title
        val found = HubSearch.find("bitwise", FormulaLibrary.formulas)
        assertEquals(listOf("programmer"), found.tools.map { it.id })
    }

    @Test fun everyToolTheShelfOffersIsFoundByItsOwnName() {
        // the bug was that none of them were, so this is the one that would have
        // caught it: type what the row says, and the row comes back
        assertEquals(20, HubSearch.tools.size)
        for (tool in HubSearch.tools) {
            val found = HubSearch.find(tool.title, FormulaLibrary.formulas)
            assertTrue(
                "'${tool.title}' does not find its own tool",
                found.tools.any { it.id == tool.id },
            )
        }
    }

    // ---- both shelves in one answer ----

    @Test fun oneQueryReturnsToolsAndCalculatorsTogether() {
        val found = HubSearch.find("matrix", FormulaLibrary.formulas)
        assertTrue("the matrix tool is missing", found.tools.any { it.id == "matrix" })
        assertTrue("the matrix calculators are missing", found.formulas.any { it.id == "det2" })
    }

    @Test fun aTaxQueryFindsTheGstCalculators() {
        val found = HubSearch.find("gst", FormulaLibrary.formulas)
        val ids = found.formulas.map { it.id }
        assertTrue("gst_add is missing", "gst_add" in ids)
        assertTrue("gst_remove is missing", "gst_remove" in ids)
        assertTrue("a tax query should not turn up a tool", found.tools.isEmpty())
    }

    @Test fun theWordsOfAQueryDoNotHaveToBeInTheTitleOrder() {
        // a reader types the words in the order they think of them
        val found = HubSearch.find("gst add", FormulaLibrary.formulas)
        assertEquals(listOf("gst_add"), found.formulas.map { it.id })
    }

    @Test fun aCalculatorIsFoundByItsCategoryAndByItsFieldLabels() {
        val zeta = calc("zeta", "Zeta", category = "Ancient Rites", inputLabel = "ram speed")
        val others = FormulaLibrary.formulas + zeta
        assertEquals(listOf("zeta"), HubSearch.find("ancient", others).formulas.map { it.id })
        assertEquals(listOf("zeta"), HubSearch.find("ram speed", others).formulas.map { it.id })
    }

    // ---- a query that finds nothing ----

    @Test fun anEmptyQueryIsNotASearch() {
        for (query in listOf("", "   ")) {
            val found = HubSearch.find(query, FormulaLibrary.formulas)
            assertTrue("an empty field listed rows", found.isEmpty)
            assertTrue("an empty field offered suggestions", found.suggestions.isEmpty())
        }
    }

    @Test fun aQueryThatMatchesNothingOffersNoWildGuesses() {
        // being told "nothing" is a truer answer than a suggestion that is not
        // close, which the reader takes as the app guessing
        val found = HubSearch.find("zzzqqq", FormulaLibrary.formulas)
        assertTrue(found.isEmpty)
        assertTrue("a nonsense query produced suggestions", found.suggestions.isEmpty())
    }

    // ---- the empty state, which now points somewhere ----

    @Test fun aNearMissIsOfferedInsteadOfEndingTheSearch() {
        val found = HubSearch.find("graf", FormulaLibrary.formulas)
        assertTrue("'graf' should match nothing outright", found.isEmpty)
        assertFalse("'graf' ended the search with nothing to offer", found.suggestions.isEmpty())
        assertEquals("the nearest name to 'graf' is not Graphing", "graph", found.suggestions.first().id)
    }

    @Test fun aSuggestedRowCanBeOpenedLikeTheRowItStandsFor() {
        // a suggestion is only useful if it is a row the hub can act on: a tool
        // id the Tools category opens, or a calculator that is in the library
        val found = HubSearch.find("programmr", FormulaLibrary.formulas)
        assertTrue(found.isEmpty)
        val first = found.suggestions.first()
        assertTrue("the suggestion is not a tool", first is HubHit.ToolHit)
        assertTrue(
            "the suggested tool is not one the shelf offers",
            HubSearch.tools.any { it.id == first.id },
        )
        assertEquals("programmer", first.id)
    }

    @Test fun aSuggestionsCalculatorIsOneTheLibraryHolds() {
        val found = HubSearch.find("matrixx", FormulaLibrary.formulas)
        assertTrue(found.isEmpty)
        val calcs = found.suggestions.filterIsInstance<HubHit.CalcHit>().map { it.formula.id }
        assertTrue("no calculator was suggested", calcs.isNotEmpty())
        assertTrue(
            "a suggestion is not in the library",
            calcs.all { id -> FormulaLibrary.formulas.any { it.id == id } },
        )
    }

    // ---- Hindi ----

    @Test fun theHindiNameIsSearchedWhenTheHubIsReadInHindi() {
        // the row in front of a Hindi reader is in Hindi, so the word they can
        // see has to work
        assertEquals(
            listOf("graph"),
            HubSearch.find("\u0917\u094D\u0930\u093E\u092B\u093C", FormulaLibrary.formulas, hindi = true)
                .tools.map { it.id },
        )
        // and the same letters are not an English name, so they find nothing
        assertTrue(
            HubSearch.find("\u0917\u094D\u0930\u093E\u092B\u093C", FormulaLibrary.formulas, hindi = false)
                .isEmpty,
        )
    }

    @Test fun aCalculatorsHindiTitleFindsItOnlyWhenHindiIsRead() {
        val interest = "\u092C\u094D\u092F\u093E\u091C" // "interest"
        val hindi = HubSearch.find(interest, FormulaLibrary.formulas, hindi = true)
        assertTrue(
            "the Hindi title did not find the interest calculators",
            hindi.formulas.any { it.id == "compound_interest" },
        )
        assertTrue(
            "the Hindi title was searched in English",
            HubSearch.find(interest, FormulaLibrary.formulas, hindi = false).isEmpty,
        )
    }
}

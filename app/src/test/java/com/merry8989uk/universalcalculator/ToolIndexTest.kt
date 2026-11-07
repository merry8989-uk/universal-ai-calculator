package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import com.merry8989uk.universalcalculator.calc.Formula
import com.merry8989uk.universalcalculator.calc.InputSpec
import com.merry8989uk.universalcalculator.calc.OutputSpec
import com.merry8989uk.universalcalculator.calc.ToolIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which reference formula has a working calculator behind it.
 *
 * The app had two formula surfaces that did not know about each other: the
 * reference page, where a formula is written down, and the tools, where the same
 * formula is worked out. Searching "determinant" on the reference page was told
 * it was something to read, while a determinant calculator sat one tab away.
 */
class ToolIndexTest {

    private fun tool(id: String, title: String): Formula =
        Formula(id, title, "Test", "for the test", listOf(InputSpec("x", "x", 1.0))) { _ ->
            listOf(OutputSpec("Result", 1.0))
        }

    // ---- the folding ----

    @Test fun twoSpellingsOfTheSameTitleFoldTogether() {
        // a reference writes 2×2 and a tool writes 2x2
        assertEquals(
            ToolIndex.key("Determinant of a 2\u00D72 matrix"),
            ToolIndex.key("Determinant of a 2x2 matrix"),
        )
    }

    @Test fun caseAndPunctuationDoNotMatter() {
        assertEquals(ToolIndex.key("Heron's formula"), ToolIndex.key("herons formula"))
        assertEquals(ToolIndex.key("Area (base \u00D7 height)"), ToolIndex.key("area base height"))
    }

    @Test fun theNumberInAMatrixSizeSurvives() {
        // 2x2 must not fold to 22, or a 2x2 and a 22 would be the same key
        assertTrue(ToolIndex.key("a 2x2 matrix") != ToolIndex.key("a 22 matrix"))
        assertTrue(ToolIndex.key("a 2\u00D72 matrix") != ToolIndex.key("a 22 matrix"))
    }

    // ---- the lookup ----

    @Test fun theCalculatorIsFoundByItsTitle() {
        val tools = listOf(tool("det2", "Determinant of a 2x2 matrix"))
        val index = ToolIndex.byTitle(tools)
        assertEquals("det2", ToolIndex.forTitle("Determinant of a 2\u00D72 matrix", index)?.id)
    }

    @Test fun aFormulaWithNoCalculatorGetsNothing() {
        val tools = listOf(tool("det2", "Determinant of a 2x2 matrix"))
        val index = ToolIndex.byTitle(tools)
        assertNull(ToolIndex.forTitle("Determinant of a product", index))
        assertNull(ToolIndex.forTitle("", index))
    }

    @Test fun theFirstOfTwoCalculatorsWithTheSameTitleWins() {
        // the list is in a deliberate order and the first is written for that name
        val tools = listOf(tool("first", "Area"), tool("second", "Area"))
        assertEquals("first", ToolIndex.forTitle("area", ToolIndex.byTitle(tools))?.id)
    }

    @Test fun anUntitledCalculatorIsNotIndexed() {
        // otherwise every formula with no title would point at it
        val index = ToolIndex.byTitle(listOf(tool("blank", "")))
        assertTrue(index.isEmpty())
    }

    // ---- the real library ----

    @Test fun theRealLibraryFindsTheMatrixCalculators() {
        val index = ToolIndex.byTitle(FormulaLibrary.formulas)
        for (pair in listOf(
            "Determinant of a 2\u00D72 matrix" to "det2",
            "Determinant of a 3\u00D73 matrix" to "det3",
            "Trace of a 3\u00D73 matrix" to "trace3",
        )) {
            val found = ToolIndex.forTitle(pair.first, index)
            assertNotNull("no calculator found for ${pair.first}", found)
            assertEquals(pair.first, pair.second, found!!.id)
        }
    }

    @Test fun theReferencePageShowsTheLinkOnlyWhereThereIsOne() {
        // the page draws the button from the tool field; a formula without one
        // must not get a button that goes nowhere
        val page = listOf(
            java.io.File("app/src/main/assets/formula-site.html"),
            java.io.File("src/main/assets/formula-site.html"),
        ).firstOrNull { it.exists() }?.readText() ?: return
        assertTrue("the page never asks for a tool", page.contains("f.tool"))
        assertTrue("the page has no way to open one", page.contains("Android.openTool"))
        // and it is drawn only when there is one
        assertTrue("the button is drawn unconditionally", page.contains("} else if (f.tool) {"))
    }
}

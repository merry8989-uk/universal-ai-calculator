package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * One page for everything.
 *
 * The app had two formula surfaces and neither knew about the other: a library of
 * calculators and a reference of formulas that are written down. The page now
 * carries both, so there is one place to look and one search to type into.
 */
class FormulaPageTest {

    private fun page(): String? = listOf(
        File("app/src/main/assets/formula-site.html"),
        File("src/main/assets/formula-site.html"),
    ).firstOrNull { it.exists() }?.readText()

    private fun bridge(): String? = listOf(
        File("app/src/main/java/com/merry8989uk/universalcalculator/ui/FormulaBridge.kt"),
        File("src/main/java/com/merry8989uk/universalcalculator/ui/FormulaBridge.kt"),
    ).firstOrNull { it.exists() }?.readText()

    @Test fun thePageAsksForBothKinds() {
        val text = page() ?: return
        assertTrue("the page never asks for the calculators", text.contains("Android.tools()"))
        assertTrue("the page never asks for the reference", text.contains("Android.library()"))
        assertTrue("the page never runs a calculator", text.contains("Android.calculateTool"))
        assertTrue("the page never runs a reference formula", text.contains("Android.calculate("))
    }

    @Test fun everyFormulaIsItsOwnPage() {
        val text = page() ?: return
        // a list that navigates, and a route for each kind
        assertTrue("there is no route for a formula", text.contains("#/f/"))
        assertTrue("there is no route for a calculator", text.contains("#/t/"))
        assertTrue("the route is not read", text.contains("hashchange"))
        assertTrue("there is no back from a page", text.contains("history.back()"))
        // and the phone's back button walks those pages rather than leaving
        val screen = listOf(
            File("app/src/main/java/com/merry8989uk/universalcalculator/ui/FormulaWebScreen.kt"),
            File("src/main/java/com/merry8989uk/universalcalculator/ui/FormulaWebScreen.kt"),
        ).firstOrNull { it.exists() }?.readText() ?: return
        assertTrue("the phone back button ignores the page history", screen.contains("canGoBack()"))
    }

    @Test fun thePageHasASwitchBetweenThem() {
        val text = page() ?: return
        assertTrue("no switch between calculators and reference", text.contains("Calculators "))
        assertTrue("no reference option", text.contains("Reference "))
        assertTrue("no everything option", text.contains("Everything "))
    }

    @Test fun theBridgePublishesTheCalculators() {
        val text = bridge() ?: return
        assertTrue("the calculators are not published", text.contains("fun toolsJson()"))
        assertTrue("a calculator cannot be run", text.contains("fun calculateToolJson("))
        // every calculator in the library has to reach the page
        assertTrue(
            "the bridge does not read the library",
            text.contains("FormulaLibrary.formulas"),
        )
    }

    @Test fun thePageIsReachableFromTheToolsList() {
        val screen = listOf(
            File("app/src/main/java/com/merry8989uk/universalcalculator/ui/FormulasScreen.kt"),
            File("src/main/java/com/merry8989uk/universalcalculator/ui/FormulasScreen.kt"),
        ).firstOrNull { it.exists() }?.readText() ?: return
        assertTrue("there is no entry for the whole page", screen.contains("\"allformulas\""))
        assertTrue("the entry does not open the page", screen.contains("tool == \"allformulas\""))
    }

    @Test fun theLibraryBehindThePageIsNotEmpty() {
        // the page is only worth having if there is something to put in it
        assertTrue("no calculators in the library", FormulaLibrary.formulas.size > 100)
        for (f in FormulaLibrary.formulas.take(20)) {
            assertNotNull("a calculator with no title", f.title.ifBlank { null })
            assertTrue("a calculator with no inputs: ${f.id}", f.inputs.isNotEmpty())
        }
    }

    @Test fun everyCalculatorRunsSoThePageCannotShowAnError() {
        // the page runs each card as it is drawn; one that throws would show a
        // red box on a card nobody asked a question about
        var broken = 0
        for (f in FormulaLibrary.formulas) {
            try {
                val values = f.inputs.associate { it.key to it.default }
                val out = f.compute(values)
                if (out.isEmpty() || out.any { it.value.isNaN() }) broken++
            } catch (e: Exception) {
                broken++
            }
        }
        assertEquals("calculators that do not run with their own defaults", 0, broken)
    }
}

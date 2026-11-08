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
        assertTrue("the page never asks for the list", text.contains("Android.index()"))
        // and one record at a time, not the whole library up front
        assertTrue("the page never asks for a formula", text.contains("Android.formula("))
        assertTrue("the page never asks for a calculator", text.contains("Android.tool("))
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

    @Test fun thePageIsLightAndSimple() {
        val text = page() ?: return
        // the design it was asked to match: light paper, one accent, no chrome
        assertTrue("no light background", text.contains("#f5f7fb"))
        assertTrue("no white cards", text.contains("--card:#ffffff"))
        assertTrue("the dark theme is still there", !text.contains("#0d1117"))
        // and the WebView behind it must not flash dark before it paints
        val screen = listOf(
            File("app/src/main/java/com/merry8989uk/universalcalculator/ui/FormulaWebScreen.kt"),
            File("src/main/java/com/merry8989uk/universalcalculator/ui/FormulaWebScreen.kt"),
        ).firstOrNull { it.exists() }?.readText() ?: return
        assertTrue("the view behind the page is still dark",
            screen.contains("0xFFF5F7FB"))
    }

    @Test fun everyFormulaPageCanBeWorkedOutByHand() {
        val text = page() ?: return
        // the box the reader types into, on every formula's page
        assertTrue("there is no expression box", text.contains("id=\"ownExpr\""))
        assertTrue("there are no values to fill in", text.contains("id=\"ownVars\""))
        assertTrue("the box is not run", text.contains("Android.evaluate("))
        // and it must reach the app's own evaluator, not a second one in JavaScript
        val bridge = bridge() ?: return
        assertTrue("the bridge has no evaluator", bridge.contains("fun evaluateJson("))
        assertTrue("the evaluator is not the app's", bridge.contains("Evaluator.evaluate("))
    }

    @Test fun thePageHasAnAToZIndex() {
        val text = page() ?: return
        assertTrue("there is no A-Z strip", text.contains("A–Z"))
        assertTrue("the letters are not built", text.contains("String.fromCharCode"))
        assertTrue("a letter does not filter", text.contains("firstLetter"))
    }

    @Test fun thePageHasASwitchBetweenThem() {
        val text = page() ?: return
        assertTrue("no switch between calculators and reference", text.contains("Calculators "))
        assertTrue("no reference option", text.contains("Reference "))
        assertTrue("no option showing both", text.contains("data-k=\"all\""))
    }

    @Test fun theBridgePublishesTheCalculators() {
        val text = bridge() ?: return
        assertTrue("there is no index", text.contains("fun indexJson()"))
        assertTrue("one formula cannot be fetched", text.contains("fun formulaJson("))
        assertTrue("one calculator cannot be fetched", text.contains("fun toolJson("))
        assertTrue("a calculator cannot be run", text.contains("fun calculateToolJson("))
        assertTrue("the bridge does not read the library", text.contains("FormulaLibrary.formulas"))
        // the old whole-library dump has to be gone, or the page is heavy again
        assertTrue("the whole library is still sent at start-up",
            !text.contains("fun libraryJson("))
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

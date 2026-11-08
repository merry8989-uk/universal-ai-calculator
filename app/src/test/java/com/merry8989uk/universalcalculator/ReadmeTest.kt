package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import com.merry8989uk.universalcalculator.calc.Units
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The README's numbers, held to the app.
 *
 * The README said 151 calculators when there were 347, 44 unit categories when
 * there were 43, and "333 tests" in one place and "114 tests" in another. A
 * number nobody can check is worse than no number, so the ones that can be
 * counted are counted here.
 */
class ReadmeTest {

    private fun readme(): String? = listOf(
        File("README.md"),
        File("../README.md"),
    ).firstOrNull { it.exists() }?.readText()

    @Test fun theCalculatorCountIsTheRealOne() {
        val text = readme() ?: return
        val real = FormulaLibrary.formulas.size
        assertTrue("the README does not say $real calculators", text.contains("$real built-in one-shot"))
        assertTrue("the README's coverage line is stale", text.contains("all $real calculators"))
    }

    @Test fun theCategoryAndUnitCountsAreTheRealOnes() {
        val text = readme() ?: return
        val categories = Units.categories.size
        val units = Units.categories.sumOf { it.units.size }
        assertTrue(
            "the README does not say $categories categories and $units units",
            text.contains("$categories categories and $units units"),
        )
    }

    @Test fun theFormulaCountIsTheRealOne() {
        val text = readme() ?: return
        val asset = listOf(
            File("src/main/assets/formulas.json"),
            File("app/src/main/assets/formulas.json"),
        ).firstOrNull { it.exists() } ?: return
        val count = Regex("\"t\"\\s*:").findAll(asset.readText()).count()
        val written = String.format("%,d", count)
        assertTrue("the README does not say $written formulas", text.contains("$written formulas"))
    }

    @Test fun theTestCountIsNotStatedAsAnExactNumber() {
        // a count of tests in prose goes stale the moment a test is added, and
        // nothing can check it; "over 1,200" is true and stays true
        val text = readme() ?: return
        assertTrue("the README no longer says how much is tested", text.contains("over 1,200 tests"))
        for (stale in listOf("333 tests", "114 tests", "343 formulas", "151 built-in")) {
            assertTrue("the README still says \"$stale\"", !text.contains(stale))
        }
    }
}

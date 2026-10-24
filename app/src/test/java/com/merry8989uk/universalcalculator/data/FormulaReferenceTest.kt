package com.merry8989uk.universalcalculator.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the bundled formula reference: it must be present, reasonably large,
 * complete, and free of duplicates.
 */
class FormulaReferenceTest {

    private val text: String by lazy {
        val candidates = listOf(
            File("src/main/assets/formulas.json"),
            File("app/src/main/assets/formulas.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw AssertionError("formulas.json not found in ${candidates.map { it.absolutePath }}")
        file.readText(Charsets.UTF_8)
    }

    private val entries: List<Triple<String, String, String>> by lazy {
        Regex("""\{[^{}]*?"c":\s*"(.*?)",\s*"t":\s*"(.*?)",\s*"f":\s*"(.*?)"\s*\}""", RegexOption.DOT_MATCHES_ALL)
            .findAll(text)
            .map { Triple(it.groupValues[1], it.groupValues[2], it.groupValues[3]) }
            .toList()
    }

    private fun norm(s: String) = s.lowercase()
        .replace(Regex("\\s+"), "")
        .replace("\u2212", "-").replace("\u00D7", "*").replace("\u00F7", "/").replace("\u00B7", "*")

    @Test fun referenceIsPresentAndSubstantial() {
        // the reference holds only what cannot be a calculator: rules, identities and tables
        assertTrue("expected 1000+ reference formulas, found ${entries.size}", entries.size >= 1000)
    }

    @Test fun noTwoEntriesInACategoryShareATitle() {
        // A repeated title makes the list look like it repeats itself, even when
        // the formulas differ. Each one now says what it is.
        val seen = HashMap<Pair<String, String>, Int>()
        val repeats = ArrayList<String>()
        entries.forEach { (cat, title, _) ->
            val key = cat to title
            val n = (seen[key] ?: 0) + 1
            seen[key] = n
            if (n == 2) repeats.add("$cat / $title")
        }
        assertTrue("these titles appear more than once: $repeats", repeats.isEmpty())
    }

    @Test fun noEntrySharesANameWithACalculator() {
        // A formula that can be computed lives in the library; the reference is
        // for the ones that cannot. Nothing may be in both.
        val norm = { s: String -> s.lowercase().replace(Regex("[^a-z0-9]"), "") }
        val calculatorNames = com.merry8989uk.universalcalculator.calc.FormulaLibrary.formulas
            .map { norm(it.title) }
            .toSet()
        val clash = entries.filter { norm(it.second) in calculatorNames }.map { it.second }
        assertTrue("these are in both the reference and the library: $clash", clash.isEmpty())
    }

    @Test fun everyEntryIsComplete() {
        val broken = entries.filter { it.first.isBlank() || it.second.isBlank() || it.third.isBlank() }
        assertTrue("incomplete entries: $broken", broken.isEmpty())
    }

    @Test fun noFormulaAppearsTwice() {
        val seen = HashMap<String, String>()
        val duplicates = ArrayList<String>()
        for ((cat, title, formula) in entries) {
            val k = norm(formula)
            val previous = seen.put(k, "$cat / $title")
            if (previous != null) duplicates.add("'$formula' appears in [$previous] and [$cat / $title]")
        }
        assertTrue("duplicate formulas:\n" + duplicates.joinToString("\n"), duplicates.isEmpty())
    }

    @Test fun coversEveryTopic() {
        val cats = entries.map { it.first }.toSet()
        for (expected in listOf(
            "Sets & Numbers", "Algebra", "Geometry", "Trigonometry",
            "Matrices & Determinants", "Vectors", "Analytic Geometry",
            "Differential Calculus", "Integral Calculus", "Differential Equations",
            "Series", "Probability & Combinatorics", "Finance & Economics",
            "Physics", "Chemistry",
        )) {
            assertTrue("missing category '$expected'", expected in cats)
        }
        assertEquals(15, cats.size)
    }

    @Test fun searchFindsThings() {
        val list = entries.map { RefFormula(it.first, it.second, it.third) }
        assertTrue("no match for 'cos'", FormulaReference.search(list, "cos", null).isNotEmpty())
        assertTrue("no match for 'Bayes'", FormulaReference.search(list, "Bayes", null).isNotEmpty())
        assertTrue("no match for 'integral'", FormulaReference.search(list, "integral", null).isNotEmpty())
        assertEquals(list.size, FormulaReference.search(list, "", null).size)
        assertTrue(FormulaReference.search(list, "", "Geometry").all { it.category == "Geometry" })
    }
}

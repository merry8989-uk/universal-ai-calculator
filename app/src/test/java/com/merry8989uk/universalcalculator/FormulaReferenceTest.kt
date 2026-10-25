package com.merry8989uk.universalcalculator

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The bundled reference, checked as data rather than through the app.
 *
 * The rule the user set is that no formula appears twice — a reference that
 * repeats itself is a reference you stop trusting, because you cannot tell
 * whether the second copy is the same formula or a different one that looks
 * alike.
 */
class FormulaReferenceTest {

    private val formulas: List<JSONObject> by lazy {
        val path = listOf(
            "src/main/assets/formulas.json",
            "app/src/main/assets/formulas.json",
        ).firstOrNull { File(it).exists() }
        assertTrue("formulas.json not found", path != null)
        val arr = JSONObject(File(path!!).readText()).getJSONArray("formulas")
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    private fun title(f: JSONObject) = f.getString("t").trim().lowercase()
    private fun text(f: JSONObject) = f.getString("f").trim().lowercase().replace(" ", "")
    private fun category(f: JSONObject) = f.getString("c").trim()

    @Test fun theReferenceIsNotEmpty() {
        assertTrue("only ${formulas.size} formulas", formulas.size > 1000)
    }

    @Test fun noTitleAppearsTwice() {
        val repeated = formulas.groupingBy { title(it) }.eachCount().filter { it.value > 1 }.keys
        assertTrue("these titles appear more than once: $repeated", repeated.isEmpty())
    }

    @Test fun noFormulaTextAppearsTwice() {
        val repeated = formulas.groupingBy { text(it) }.eachCount().filter { it.value > 1 }.keys
        assertTrue("these formulas appear more than once: $repeated", repeated.isEmpty())
    }

    @Test fun everyEntryIsComplete() {
        for (f in formulas) {
            assertTrue("an entry has no category: $f", category(f).isNotBlank())
            assertTrue("an entry has no title: $f", f.getString("t").isNotBlank())
            assertTrue("an entry has no formula: $f", f.getString("f").isNotBlank())
        }
    }

    @Test fun noEntryIsTheWreckageOfAnOcrPass() {
        // a page number glued to the end is the signature of a bad extraction
        val suspicious = formulas.filter { f ->
            val body = f.getString("f").trim()
            Regex("\\s\\d{2,3}$").containsMatchIn(body)
        }.map { it.getString("t") }
        assertTrue("these look like OCR leftovers: $suspicious", suspicious.isEmpty())
    }

    @Test fun theFinancialDerivativesAreThere() {
        // the derivatives sheet was the one real gap in the reference
        val cat = formulas.filter { category(it) == "Financial Derivatives" }
        assertTrue("the derivatives category has only ${cat.size} entries", cat.size >= 50)
        val titles = cat.map { title(it) }
        for (wanted in listOf(
            "put-call parity", "black-scholes call", "ito's lemma",
            "delta", "gamma", "theta", "vega", "rho",
        )) {
            assertTrue("$wanted is missing", wanted in titles)
        }
    }

    @Test fun everyCategoryHasSomethingInIt() {
        val byCat = formulas.groupingBy { category(it) }.eachCount()
        val empty = byCat.filter { it.value < 5 }.keys
        assertTrue("these categories are nearly empty: $empty", empty.isEmpty())
    }

    @Test fun theThreeSubjectsTheUserAskedForAreCovered() {
        val cats = formulas.map { category(it) }.toSet()
        // mathematics is the bulk; physics and chemistry have their own sections
        assertTrue("no Physics section", "Physics" in cats)
        assertTrue("no Chemistry section", "Chemistry" in cats)
        val maths = listOf(
            "Algebra", "Geometry", "Trigonometry", "Integral Calculus",
            "Differential Calculus", "Series", "Vectors",
        )
        for (m in maths) assertTrue("no $m section", m in cats)
    }
}

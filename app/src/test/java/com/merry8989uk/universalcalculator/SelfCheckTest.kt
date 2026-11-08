package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.diagnostics.SelfCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The app checking itself on the phone it is running on.
 *
 * Every check here is something a build server cannot answer: whether the files
 * made it into the package, whether the page loads from the assets, whether the
 * engines run on a device rather than on a desktop JVM. The checks take the
 * assets as a parameter so they can be run here — otherwise a failure on a phone
 * would be indistinguishable from a broken check.
 */
class SelfCheckTest {

    private fun assets(name: String): String? {
        val file = listOf(
            File("app/src/main/assets/$name"),
            File("src/main/assets/$name"),
        ).firstOrNull { it.exists() } ?: return null
        return file.readText()
    }

    private fun run() = SelfCheck.checks(assets = { assets(it) })

    @Test fun everyCheckPassesOnTheRealPackage() {
        val results = run()
        val failed = results.filter { !it.ok }
        assertTrue(
            "these checks failed: " + failed.joinToString("; ") { "${it.name} — ${it.detail}" },
            failed.isEmpty(),
        )
        // eight checks plus the one over every reference formula, which needs an
        // app context and so reports itself as unrun here
        assertEquals(9, results.size)
    }

    @Test fun theFormulaFileIsInThePackage() {
        val check = run().first { it.name == "Formula file" }
        assertTrue("the formula file was not found: ${check.detail}", check.ok)
        // and the count in the detail is a real one
        assertTrue("the count looks wrong: ${check.detail}", check.detail.contains("1396"))
    }

    @Test fun aMissingFormulaFileIsReportedNotIgnored() {
        // the whole point of a self-check is that it fails loudly
        val results = SelfCheck.checks(assets = { null })
        val file = results.first { it.name == "Formula file" }
        assertTrue("a missing file was called fine", !file.ok)
        assertTrue("the message does not say what is wrong: ${file.detail}",
            file.detail.contains("not in the package"))
        val page = results.first { it.name == "Formula page" }
        assertTrue("a missing page was called fine", !page.ok)
    }

    @Test fun aTruncatedFormulaFileIsCaught() {
        // half a file parses as JSON and would look fine to everything else
        val results = SelfCheck.checks(assets = { name ->
            if (name == "formulas.json") "{\"formulas\":[{\"t\":\"one\"}]}" else assets(name)
        })
        val file = results.first { it.name == "Formula file" }
        assertTrue("a truncated file passed", !file.ok)
    }

    @Test fun theEnginesAreCheckedWithARealAnswer() {
        val results = run()
        val interest = results.first { it.name == "Interest engine" }
        assertTrue("the interest engine is wrong: ${interest.detail}", interest.ok)
        val schedule = results.first { it.name == "Schedule" }
        assertTrue("the schedule is wrong: ${schedule.detail}", schedule.ok)
        val print = results.first { it.name == "Print document" }
        assertTrue("the print document is wrong: ${print.detail}", print.ok)
        val office = results.first { it.name == "Office files" }
        assertTrue("office reading is wrong: ${office.detail}", office.ok)
    }

    @Test fun everyReferenceFormulaIsCheckedNotJustTheCalculators() {
        val results = SelfCheck.checks(assets = { assets(it) })
        val check = results.firstOrNull { it.name == "Reference formulas" }
        assertNotNull("the 1396 are not checked at all", check)
        // without an app context it must say so rather than claim success
        assertTrue("it claimed a pass it did not run", !check!!.ok)
        assertTrue("it does not say why: ${check.detail}", check.detail.contains("context"))
    }

    @Test fun theReportSaysHowManyFailed() {
        val allGood = SelfCheck.report(
            listOf(
                com.merry8989uk.universalcalculator.diagnostics.CheckResult("a", true, "fine"),
                com.merry8989uk.universalcalculator.diagnostics.CheckResult("b", true, "fine"),
            ),
        )
        assertTrue(allGood.contains("All 2 checks passed"))

        val oneBad = SelfCheck.report(
            listOf(
                com.merry8989uk.universalcalculator.diagnostics.CheckResult("a", true, "fine"),
                com.merry8989uk.universalcalculator.diagnostics.CheckResult("b", false, "broken"),
            ),
        )
        assertTrue("a failure is not counted: $oneBad", oneBad.contains("1 of 2 checks failed"))
        assertTrue("the failing one is not marked: $oneBad", oneBad.contains("FAIL"))
    }
}

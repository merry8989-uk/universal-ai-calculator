package com.merry8989uk.universalcalculator.diagnostics

import android.content.Context
import com.merry8989uk.universalcalculator.ai.OfficeText
import com.merry8989uk.universalcalculator.calc.Compounding
import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import com.merry8989uk.universalcalculator.calc.Growth
import com.merry8989uk.universalcalculator.calc.RateBasis
import com.merry8989uk.universalcalculator.ui.ReportDocument
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** One thing that was checked, and what happened. */
data class CheckResult(
    val name: String,
    val ok: Boolean,
    val detail: String,
)

/**
 * The things the app can check about itself, on the phone it is running on.
 *
 * Every one of these is something a build server cannot answer. The tests prove
 * the arithmetic and the parsing; they cannot prove that the formula file made
 * it into the package, that the page loads from the assets, or that the print
 * document is assembled on a device rather than on a desktop JVM. When something
 * is wrong on a phone and the words for it are hard to find, this is what says
 * which part failed.
 *
 * It is deliberately quiet: no network, no AI call, nothing written. It reads
 * what is already there and reports.
 */
object SelfCheck {

    /** Reads a bundled asset by name, or null when it is not there. */
    fun interface Assets {
        fun read(name: String): String?
    }

    fun run(context: Context): List<CheckResult> = checks(
        assets = { name ->
            try {
                context.assets.open(name).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                null
            }
        },
        context = context,
    )

    /**
     * Runs everything and writes the result to the log.
     *
     * A self-check nobody can find afterwards is a self-check that did not
     * happen. Every result goes into the diagnostics log, so the answer is in the
     * export the user sends on, beside the crash counts and the ping.
     */
    fun runAndLog(context: Context): List<CheckResult> {
        val results = run(context)
        DiagnosticsLogger.log("selfcheck", "ran ${results.size} checks")
        for (r in results) {
            DiagnosticsLogger.log(
                "selfcheck",
                (if (r.ok) "ok   " else "FAIL ") + r.name + " — " + r.detail,
            )
        }
        val failed = results.count { !it.ok }
        DiagnosticsLogger.log(
            "selfcheck",
            if (failed == 0) "all ${results.size} checks passed"
            else "$failed of ${results.size} checks failed",
        )
        return results
    }

    /**
     * Every check, given a way to read the bundled files.
     *
     * The reader is a parameter so the checks can be run in a test without a
     * phone, which is the only way to know that a failure here means a real
     * failure and not a broken check.
     */
    fun checks(assets: Assets, context: Context? = null): List<CheckResult> = listOf(
        formulaFile(assets),
        referencePage(assets),
        formulaLibrary(),
        interestEngine(),
        growthSchedule(),
        printDocument(),
        officeReading(),
        currencyData(),
        referenceFormulas(context),
    )

    // ---- the bundled files ----

    private fun formulaFile(assets: Assets): CheckResult {
        val text = assets.read("formulas.json")
            ?: return CheckResult("Formula file", false, "formulas.json is not in the package")
        val count = Regex("\"t\"\\s*:").findAll(text).count()
        return if (count > 100) {
            CheckResult("Formula file", true, "$count formulas in the package")
        } else {
            CheckResult("Formula file", false, "only $count formulas found; the file looks truncated")
        }
    }

    private fun referencePage(assets: Assets): CheckResult {
        val text = assets.read("formula-studio.html")
            ?: return CheckResult("Formula page", false, "formula-studio.html is not in the package")
        // the page must not reach for anything: the app is used on trains, and a
        // second file is a second thing that can fail to load
        if (text.contains("cdnjs") || text.contains("src=\"http")) {
            return CheckResult("Formula page", false, "the page still loads something from the internet")
        }
        if (text.contains("src=\"math.min.js\"")) {
            return CheckResult("Formula page", false, "the maths is a separate file rather than part of the page")
        }
        val count = Regex("\\{\"?serial").findAll(text).count()
        val hasMaths = text.contains("math.evaluate")
        return if (count > 100 && hasMaths) {
            CheckResult("Formula page", true, "self-contained, $count formulas, its own maths")
        } else {
            CheckResult("Formula page", false, "the page has $count formulas and maths: $hasMaths")
        }
    }

    // ---- the engines ----

    /**
     * Every calculator, run with random values rather than its own defaults.
     *
     * The old version ran each one once with the numbers it ships with and asked
     * only whether it threw. A calculator that always returns 1 passed that. This
     * runs each several times on random values, requires a real number each time,
     * and requires every input to change the answer.
     */
    private fun formulaLibrary(): CheckResult {
        val tools = FormulaLibrary.formulas
        if (tools.isEmpty()) return CheckResult("Calculators", false, "no calculators in the library")
        val proof = com.merry8989uk.universalcalculator.formula.FormulaProver.proveTools(tools)
        return if (proof.ok) {
            CheckResult("Calculators", true, "${proof.checked} run at random values, all agree with themselves")
        } else {
            val named = proof.failures.take(5).joinToString("; ") { "${it.title}: ${it.why}" }
            CheckResult("Calculators", false, "${proof.failures.size} of ${proof.checked} failed — $named")
        }
    }

    private fun interestEngine(): CheckResult {
        // 8% effective on 50,000 for a year is 54,000, whatever the period
        val plan = Growth.plan(
            principal = 50_000.0,
            annualRatePercent = 8.0,
            years = 1.0,
            compounding = Compounding.WEEKLY,
            basis = RateBasis.EFFECTIVE,
        )
        val ok = Math.abs(plan.maturity - 54_000.0) < 0.01
        return CheckResult(
            "Interest engine",
            ok,
            if (ok) "8% effective on 50,000 gives 54,000" else "gave ${plan.maturity}, expected 54000",
        )
    }

    private fun growthSchedule(): CheckResult {
        val plan = Growth.plan(10_000.0, 8.0, 1.0, Compounding.MONTHLY)
        if (plan.rows.size != 12) {
            return CheckResult("Schedule", false, "${plan.rows.size} rows for a year of months")
        }
        val last = plan.rows.last()
        return if (Math.abs(last.closing - plan.maturity) < 0.01) {
            CheckResult("Schedule", true, "12 rows, and the last one matches the answer")
        } else {
            CheckResult("Schedule", false, "the last row and the answer disagree")
        }
    }

    private fun printDocument(): CheckResult {
        val plan = Growth.plan(10_000.0, 8.0, 1.0, Compounding.MONTHLY)
        val html = ReportDocument.growth(plan, "8", "nominal", "monthly", "1 year")
        val ok = html.contains("<table>") && html.contains("10,000")
        return CheckResult(
            "Print document",
            ok,
            if (ok) "the statement builds with its schedule" else "the statement came out without a table",
        )
    }

    private fun officeReading(): CheckResult {
        val text = OfficeText.extract(sampleDocx(), "check.docx")
        return if (text == "self check") {
            CheckResult("Office files", true, "a Word file reads back as its text")
        } else {
            CheckResult("Office files", false, "a Word file did not read back: ${text ?: "nothing"}")
        }
    }

    private fun currencyData(): CheckResult {
        val countries = com.merry8989uk.universalcalculator.calc.Countries.all.size
        val coins = com.merry8989uk.universalcalculator.calc.Crypto.all.size
        return if (countries > 50 && coins > 10) {
            CheckResult("Currency data", true, "$countries countries and $coins coins")
        } else {
            CheckResult("Currency data", false, "only $countries countries and $coins coins")
        }
    }

    /**
     * Every reference formula, run once.
     *
     * The library check covers the calculators — the formulas that were built to
     * be worked out. This one covers the other 1396: each is asked to produce an
     * answer with its sample values, and the ones that cannot are named. A
     * formula that silently stopped working is the failure nobody reports,
     * because there is no button to press that looks broken.
     */
    private fun referenceFormulas(context: Context?): CheckResult {
        if (context == null) {
            return CheckResult("Reference formulas", false, "no app context to read them from")
        }
        val definitions = try {
            com.merry8989uk.universalcalculator.data.FormulaReference.definitions(context)
        } catch (e: Exception) {
            return CheckResult("Reference formulas", false, "the reference would not load: ${e.message}")
        }
        if (definitions.isEmpty()) {
            return CheckResult("Reference formulas", false, "the reference is empty")
        }

        val proof = com.merry8989uk.universalcalculator.formula.FormulaProver
            .proveReference(definitions)
        val detail = if (proof.ok) {
            "${proof.passed} proved at random values, ${proof.skipped} are for reading"
        } else {
            val named = proof.failures.take(5).joinToString("; ") { "${it.title}: ${it.why}" }
            "${proof.failures.size} of ${proof.checked} failed — $named"
        }
        return CheckResult("Reference formulas", proof.ok, detail)
    }

    /** A one-paragraph Word file, built here so the check needs no file on disk. */
    private fun sampleDocx(): ByteArray {
        val body = "<w:p><w:r><w:t>self check</w:t></w:r></w:p>"
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { out ->
            out.putNextEntry(ZipEntry("word/document.xml"))
            out.write("<w:document><w:body>$body</w:body></w:document>".toByteArray())
            out.closeEntry()
        }
        return bytes.toByteArray()
    }

    /** The whole report, for the diagnostics export. */
    fun report(results: List<CheckResult>): String = buildString {
        append("Self-check\n")
        for (r in results) {
            append(if (r.ok) "  ok    " else "  FAIL  ").append(r.name).append(" — ").append(r.detail).append('\n')
        }
        val failed = results.count { !it.ok }
        append(
            if (failed == 0) "All ${results.size} checks passed.\n"
            else "$failed of ${results.size} checks failed.\n",
        )
    }
}

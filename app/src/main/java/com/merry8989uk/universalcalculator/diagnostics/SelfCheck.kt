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

    fun run(context: Context): List<CheckResult> = checks { name ->
        try {
            context.assets.open(name).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Every check, given a way to read the bundled files.
     *
     * The reader is a parameter so the checks can be run in a test without a
     * phone, which is the only way to know that a failure here means a real
     * failure and not a broken check.
     */
    fun checks(assets: Assets): List<CheckResult> = listOf(
        formulaFile(assets),
        referencePage(assets),
        formulaLibrary(),
        interestEngine(),
        growthSchedule(),
        printDocument(),
        officeReading(),
        currencyData(),
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
        val text = assets.read("formula-site.html")
            ?: return CheckResult("Formula page", false, "formula-site.html is not in the package")
        val asksTheApp = text.contains("Android.calculate")
        val drawsSteps = text.contains("steps")
        return if (asksTheApp && drawsSteps) {
            CheckResult("Formula page", true, "loads from the package and shows the working")
        } else {
            CheckResult(
                "Formula page",
                false,
                "the page is there but " +
                    (if (!asksTheApp) "does not ask the app for answers" else "does not show the working"),
            )
        }
    }

    // ---- the engines ----

    private fun formulaLibrary(): CheckResult {
        val formulas = FormulaLibrary.formulas
        if (formulas.isEmpty()) return CheckResult("Formula library", false, "no calculators in the library")
        var broken = 0
        for (f in formulas) {
            try {
                val values = f.inputs.associate { it.key to it.default }
                val out = f.compute(values)
                if (out.isEmpty() || out.any { it.value.isNaN() }) broken++
            } catch (e: Exception) {
                broken++
            }
        }
        return if (broken == 0) {
            CheckResult("Formula library", true, "${formulas.size} calculators all run")
        } else {
            CheckResult("Formula library", false, "$broken of ${formulas.size} calculators do not run")
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

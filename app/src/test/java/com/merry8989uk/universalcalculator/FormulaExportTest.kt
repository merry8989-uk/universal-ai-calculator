package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.formula.CalculationRequest
import com.merry8989uk.universalcalculator.formula.FormulaCalculator
import com.merry8989uk.universalcalculator.formula.FormulaRegistry
import com.merry8989uk.universalcalculator.formula.FormulaType
import com.merry8989uk.universalcalculator.formula.RawFormula
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Publishes the whole formula library as JSON for the website.
 *
 * The site runs the same formulas in JavaScript, and a website that quietly
 * computes something different from the app is worse than no website. So this
 * does not just dump the formulas — it also evaluates each one with the app's own
 * engine on its own sample inputs and writes the answer alongside. The site's
 * engine is then checked against those numbers in CI, which makes the app the
 * oracle rather than a second opinion.
 *
 * The file it writes is `docs/formulas.json`, which the Pages workflow deploys.
 */
class FormulaExportTest {

    private fun repoFile(relative: String): File {
        // unit tests run with the module as the working directory, but be
        // forgiving about it so this does not depend on which module runs it
        val candidates = listOf(
            File(relative),
            File("../$relative"),
            File("../../$relative"),
        )
        return candidates.firstOrNull { it.parentFile?.exists() == true } ?: File(relative)
    }

    @Test fun theWholeLibraryIsPublishedForTheSite() {
        val assetFile = listOf(
            File("src/main/assets/formulas.json"),
            File("app/src/main/assets/formulas.json"),
            File("../app/src/main/assets/formulas.json"),
        ).firstOrNull { it.exists() }
        assertTrue("the bundled formula asset was not found", assetFile != null)

        val raw = MiniJson.readFormulas(assetFile!!.readText())
        assertTrue("the formula asset is empty", raw.isNotEmpty())

        val definitions = FormulaRegistry.of(raw)
        assertTrue("the registry came back empty", definitions.isNotEmpty())

        val out = StringBuilder()
        out.append("{\"generated\":\"from the app's own registry\",\"formulas\":[")
        var first = true
        var computed = 0

        for (d in definitions) {
            val sample = d.inputs.associate { it.id to it.sample }

            // the app's own answer, used to check the site's engine
            var expected: Double? = null
            val offered = d.type == FormulaType.DIRECT_CALCULATOR || d.type == FormulaType.CHECKER
            // An expression carrying OCR damage — a subscript rendered as "ᴄ", a
            // stray CJK character — is not something the browser engine can read,
            // and offering it as a calculator would be offering a wrong number.
            // The site shows those as reference only; the app's own review list
            // already holds them.
            val expr = d.expression
            val readable = expr != null && expr.all { ch ->
                (ch.isLetterOrDigit() && ch.code < 128) || ch in " +-*/^().,%!\u00B0_"
            }
            if (offered && readable) {
                val result = FormulaCalculator.calculate(d, CalculationRequest(d.id, sample))
                if (result.success) {
                    expected = result.approximateValue
                    if (expected != null) computed++
                }
            }

            if (!first) out.append(',')
            first = false
            out.append('{')
            out.append("\"id\":").append(quote(d.id)).append(',')
            out.append("\"title\":").append(quote(d.title)).append(',')
            out.append("\"category\":").append(quote(d.category)).append(',')
            out.append("\"printed\":").append(quote(d.printed)).append(',')
            out.append("\"expression\":").append(d.expression?.let { quote(it) } ?: "null").append(',')
            out.append("\"type\":").append(quote(d.type.name)).append(',')
            out.append("\"output\":{")
            out.append("\"label\":").append(quote(d.output.label)).append(',')
            out.append("\"unit\":").append(d.output.unit?.let { quote(it) } ?: "null")
            out.append("},")
            out.append("\"inputs\":[")
            d.inputs.forEachIndexed { i, spec ->
                if (i > 0) out.append(',')
                out.append('{')
                out.append("\"id\":").append(quote(spec.id)).append(',')
                out.append("\"label\":").append(quote(spec.label)).append(',')
                out.append("\"sample\":").append(num(spec.sample))
                out.append('}')
            }
            out.append("],")
            out.append("\"expected\":").append(expected?.let { num(it) } ?: "null").append(',')
            // only a formula that HAS an expression we cannot read is "under
            // review"; one with no expression at all is simply not a calculator,
            // and labelling it damaged would be untrue
            out.append("\"review\":")
                .append(if (offered && expr != null && !readable) "true" else "false")
            out.append('}')
        }
        out.append("]}")

        val target = repoFile("docs/formulas.json")
        target.parentFile?.mkdirs()
        target.writeText(out.toString())

        // A second file, for reading rather than for the page: everything the
        // library could not turn into a calculator, with the reason the
        // classifier gave. Without this the "why is this one not working"
        // question can only be answered by guessing.
        val review = StringBuilder()
        val byType = definitions.groupingBy { it.type.name }.eachCount()
        review.append("# Formula library \u2014 what is not a calculator, and why\n\n")
        review.append("| Type | Count |\n| --- | --- |\n")
        for ((type, count) in byType.entries.sortedByDescending { it.value }) {
            review.append("| $type | $count |\n")
        }
        review.append("\nTotal: ${definitions.size}. Computed: $computed.\n")

        for (type in listOf(
            FormulaType.NEEDS_REVIEW,
            FormulaType.SOLVER,
            FormulaType.REFERENCE_ONLY,
            FormulaType.CONVERTER,
        )) {
            val group = definitions.filter { it.type == type }
            if (group.isEmpty()) continue
            review.append("\n## $type (${group.size})\n\n")
            for (d in group) {
                review.append("- **${d.title}** (${d.category}) \u2014 ${d.printed}\n")
                review.append("  - ${d.reason}\n")
            }
        }
        val reviewTarget = repoFile("docs/formula-review.md")
        reviewTarget.parentFile?.mkdirs()
        reviewTarget.writeText(review.toString())

        assertTrue("nothing in the library could be computed", computed > 0)

        // ---- coverage: which of them has a calculator, and which has none ----
        //
        // The question this answers is the one a person actually asks: "is there
        // a calculator for this formula?" A formula can be worked out on its own
        // page, or have a separate calculator built for it, or neither.
        val toolIndex = com.merry8989uk.universalcalculator.calc.ToolIndex
            .byTitle(com.merry8989uk.universalcalculator.calc.FormulaLibrary.formulas)
        val none = ArrayList<String>()
        var ownPage = 0
        var viaTool = 0
        var both = 0
        var serial = 0
        val coverage = StringBuilder()
        coverage.append("# Which formulas have a calculator\n\n")
        coverage.append("| | Count |\n| --- | --- |\n")
        for (d in definitions) {
            serial++
            val workable = (d.type == FormulaType.DIRECT_CALCULATOR ||
                d.type == FormulaType.CHECKER) && d.expression != null
            val tool = toolIndex[com.merry8989uk.universalcalculator.calc.ToolIndex.key(d.title)]
            if (workable && tool != null) both++
            else if (workable) ownPage++
            else if (tool != null) viaTool++
            else none.add("| $serial | ${d.title} | ${d.category} | ${d.type} |")
        }
        val total = definitions.size
        coverage.append("| Formulas in the reference | $total |\n")
        coverage.append("| Worked out on its own page | $ownPage |\n")
        coverage.append("| Have a separate calculator | $viaTool |\n")
        coverage.append("| Both | $both |\n")
        coverage.append("| **No calculator at all** | **${none.size}** |\n\n")
        coverage.append("## No calculator (${none.size})\n\n")
        coverage.append("| # | Title | Category | Type |\n| --- | --- | --- | --- |\n")
        coverage.append(none.joinToString("\n"))
        coverage.append("\n")
        val coverageFile = File("docs/formula-coverage.md")
        coverageFile.writeText(coverage.toString())
        println("COVERAGE total=$total own=$ownPage tool=$viaTool both=$both none=${none.size}")
        assertTrue("the export is suspiciously small", out.length > 100_000)
        assertTrue("the file was not written where the site expects it: ${target.absolutePath}", target.exists())
    }

    private fun quote(s: String): String {
        val b = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '"' -> b.append("\\\"")
                '\\' -> b.append("\\\\")
                '\n' -> b.append("\\n")
                '\r' -> b.append("\\r")
                '\t' -> b.append("\\t")
                else -> if (c < ' ') b.append(String.format("\\u%04x", c.code)) else b.append(c)
            }
        }
        return b.append('"').toString()
    }

    private fun num(v: Double): String =
        if (v.isNaN() || v.isInfinite()) "null" else v.toString()
}

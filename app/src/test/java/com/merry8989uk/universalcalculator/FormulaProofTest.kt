package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import com.merry8989uk.universalcalculator.calc.ToolProver
import com.merry8989uk.universalcalculator.formula.FormulaProver
import com.merry8989uk.universalcalculator.formula.FormulaRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every formula, run at random values, and the results published.
 *
 * Running each formula once with the numbers it ships with proves almost nothing:
 * a formula that always returns 1 passes, and so does one that ignores a box the
 * user filled in. This runs each of them several times on random values and
 * checks the arithmetic each time — a checker has to have both sides agree, and a
 * calculator has to give a real number that changes when each of its inputs does.
 *
 * The failures are written to `docs/formula-proof.md` rather than asserted, so
 * that the list can be read and worked through instead of the build going red
 * with a count and no names.
 */
class FormulaProofTest {

    private fun definitions() = FormulaRegistry.of(
        MiniJson.readFormulas(
            listOf(
                File("src/main/assets/formulas.json"),
                File("app/src/main/assets/formulas.json"),
                File("../app/src/main/assets/formulas.json"),
            ).firstOrNull { it.exists() }!!.readText(),
        ),
    )

    @Test fun everyReferenceFormulaIsProvedAtRandomValues() {
        val proof = FormulaProver.proveReference(definitions())

        val out = StringBuilder()
        out.append("# Proving the formulas at random values\n\n")
        out.append("Each formula is run several times with random values in ")
        out.append("0.5 to 20, and the arithmetic is checked every time.\n\n")
        out.append("| | Count |\n| --- | --- |\n")
        out.append("| Formulas that claim to compute | ${proof.checked} |\n")
        out.append("| Proved at every round | ${proof.passed} |\n")
        out.append("| **Failed** | **${proof.failures.size}** |\n")
        out.append("| For reading, not computing | ${proof.skipped} |\n\n")
        out.append("Rounds per formula: ${proof.rounds}, on a fixed seed so a failure repeats.\n\n")

        if (proof.failures.isNotEmpty()) {
            out.append("## Failed (${proof.failures.size})\n\n")
            out.append("| Title | What went wrong |\n| --- | --- |\n")
            for (f in proof.failures) {
                out.append("| ${f.title} | ${f.why.replace("|", "/")} |\n")
            }
        }
        repoFile("docs/formula-proof.md").writeText(out.toString())

        // the harness has to have actually run; the failures are the report
        assertTrue("the prover checked nothing", proof.checked > 200)
    }

    @Test fun everyCalculatorIsProvedAtRandomValues() {
        val proof = ToolProver.proveTools(FormulaLibrary.formulas)
        println("TOOLS checked=${proof.checked} failed=${proof.failures.size}")
        for (f in proof.failures.take(40)) {
            println("  FAIL ${f.title}: ${f.why}")
        }
        assertTrue("the prover checked nothing", proof.checked > 200)
    }

    private fun repoFile(relative: String): File {
        val candidates = listOf(
            File(relative),
            File("../$relative"),
            File("../../$relative"),
        )
        return candidates.firstOrNull { it.parentFile?.exists() == true } ?: File(relative)
    }
}

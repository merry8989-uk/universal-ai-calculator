package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The guard and the evaluator have to agree about what a function is.
 *
 * The guard runs before the evaluator: it reads what was typed and refuses it
 * in words the user can act on. That is only helpful while the two are talking
 * about the same calculator. When the guard kept a list of its own it drifted —
 * thirty-four functions the evaluator resolves were missing from it and
 * `harmmean` was spelled `harmean` — and the calculator then refused sums it
 * could have worked out, with no way for the user to know the difference.
 *
 * The guard now reads the evaluator's table, so these tests are here to keep
 * that true: they compare the two in both directions, over the whole table
 * rather than over a few names somebody remembered to add.
 */
class ExpressionGuardParityTest {

    /** Every name the evaluator resolves, as the guard has to see it. */
    private val table: Set<String> get() = Evaluator.FUNCTION_NAMES

    /** What the evaluator said, or null when it worked the expression out. */
    private fun complaintOf(expression: String): String? = try {
        Evaluator.evaluate(expression)
        null
    } catch (e: Exception) {
        e.message ?: e.toString()
    }

    // ---------------- the whole table, both ways ----------------

    @Test fun everyFunctionTheEvaluatorResolvesIsAcceptedByTheGuard() {
        val refused = table.filter { ExpressionGuard.check("$it(1)") != null }
            .map { "$it -> ${ExpressionGuard.check("$it(1)")}" }
        assertTrue("the guard refuses functions the evaluator resolves: $refused", refused.isEmpty())
    }

    @Test fun theGuardIsNotSimplyAcceptingEverything() {
        // A name nothing resolves has to still be refused, or the test above
        // would pass on a guard that had stopped checking names at all.
        val invented = listOf("frobnicate", "sinhcosh", "nonsense")
        for (name in invented) {
            assertNotNull(
                "the guard accepts '$name', which the evaluator cannot resolve",
                ExpressionGuard.check("$name(1)"),
            )
        }
    }

    @Test fun theGuardAcceptsEveryNameInTheCaseTheEvaluatorFoldsItTo() {
        // the parser lower-cases a name before looking it up, so SIN and sin
        // are the same function and the guard must not disagree
        val refused = table.filter { ExpressionGuard.check("${it.uppercase()}(1)") != null }
        assertTrue("the guard refuses upper-case function names: $refused", refused.isEmpty())
    }

    @Test fun everyNameInTheTableIsOneTheParserActuallyHandles() {
        // The table is the dispatch, so this is a check that nobody has put a
        // second copy of the function list back beside it: a name in the table
        // has to reach a handler, not fall through to "unknown function".
        val unresolved = table.filter { complaintOf("$it(1)")?.contains("Unknown function") == true }
        assertTrue("the table lists names the parser does not handle: $unresolved", unresolved.isEmpty())
    }

    @Test fun aFunctionTheTableDoesNotHoldIsRefusedByBothOfThem() {
        // the six arc names the old hand-kept list allowed are not functions
        // the evaluator resolves, so neither side may accept them
        val notInTheTable = listOf(
            "arcsin", "arccos", "arctan", "arcsinh", "arccosh", "arctanh",
        )
        for (name in notInTheTable) {
            assertFalse("$name is in the evaluator's table after all", name in table)
            assertNotNull(
                "the guard accepts '$name', which the evaluator cannot resolve",
                ExpressionGuard.check("$name(0.5)"),
            )
            assertTrue(
                "the evaluator resolves '$name' after all",
                complaintOf("$name(0.5)")?.contains("Unknown function") == true,
            )
        }
    }

    // ---------------- the case the tracker reported ----------------

    @Test fun theHarmonicMeanComputes() {
        // 3 / (1/1 + 1/2 + 1/3) = 18/11
        assertNull(
            "the guard refuses harmmean",
            ExpressionGuard.check("harmmean(1,2,3)"),
        )
        assertEquals(18.0 / 11.0, Evaluator.evaluate("harmmean(1,2,3)"), 1e-12)
    }

    @Test fun theMisspelledHarmeanIsRefusedRatherThanPassedOn() {
        // the guard used to let this through to the evaluator, which then said
        // "unknown function" — a name the calculator printed on a key
        assertNotNull("the guard accepts the misspelling", ExpressionGuard.check("harmean(1,2,3)"))
        assertTrue(
            "the evaluator resolves the misspelling after all",
            complaintOf("harmean(1,2,3)")?.contains("Unknown function") == true,
        )
    }

    @Test fun theFunctionsThatWereMissingAreAccepted() {
        // the names the tracker listed, as a reader of the report would type them
        val reported = listOf(
            "fact", "trunc", "atan2", "var", "sd", "harmmean", "mode", "count",
            "clamp", "square", "cube", "frac", "rint", "even", "odd", "pct",
        )
        for (name in reported) {
            assertNull("the guard still refuses '$name'", ExpressionGuard.check("$name(1,2)"))
        }
    }

    // ---------------- the constants, which drifted the same way ----------------

    @Test fun phiIsAValueTheGuardLetsThrough() {
        // the evaluator has read `phi` as the golden ratio all along; the guard
        // listed only the Greek letter and refused the word
        assertNull("the guard refuses phi", ExpressionGuard.check("phi"))
        assertEquals((1 + kotlin.math.sqrt(5.0)) / 2, Evaluator.evaluate("phi"), 1e-12)
    }

    @Test fun everyConstantTheEvaluatorReadsIsAcceptedByTheGuard() {
        val refused = Evaluator.CONSTANT_NAMES.filter { ExpressionGuard.check(it) != null }
        assertTrue("the guard refuses constants the evaluator reads: $refused", refused.isEmpty())
    }
}

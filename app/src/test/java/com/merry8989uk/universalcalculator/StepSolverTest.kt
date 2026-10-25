package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.StepSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The working, one move at a time.
 *
 * The thing being protected here is honesty. A worked solution that is subtly
 * wrong is worse than none: the reader has no way to tell, and learns the wrong
 * order of operations from it. So every test that shows steps also checks that
 * the steps arrive where the calculator arrived.
 */
class StepSolverTest {

    private fun values(expression: String, degrees: Boolean = false): List<String> =
        StepSolver.steps(expression, Evaluator.evaluate(expression, degrees = degrees), degrees)
            .map { it.value }

    private fun results(expression: String): List<String> =
        StepSolver.steps(expression, Evaluator.evaluate(expression)).map { it.result }

    // ---------------- the order of operations ----------------

    @Test fun multiplyingIsDoneBeforeAdding() {
        val steps = StepSolver.steps("2+3*4", Evaluator.evaluate("2+3*4"))
        assertEquals(2, steps.size)
        assertEquals("3*4", steps[0].piece)
        assertEquals("12", steps[0].value)
        assertEquals("2+12", steps[0].result)
        assertEquals("14", steps[1].value)
    }

    @Test fun bracketsGoFirst() {
        val steps = StepSolver.steps("(2+3)*4", Evaluator.evaluate("(2+3)*4"))
        assertEquals("2+3", steps[0].piece)
        assertEquals("5", steps[0].value)
        // the brackets then have no job left
        assertTrue("the bracket should be dropped: ${steps.map { it.result }}", "5*4" in results("(2+3)*4"))
    }

    @Test fun dividingIsDoneBeforeAdding() {
        assertEquals(listOf("2.5", "4.5"), values("10/4+2"))
        assertEquals(listOf("4", "96"), values("100-20/5"))
    }

    @Test fun aMixedSumWalksAllTheWayDown() {
        val steps = StepSolver.steps("2*(3+4)-5", Evaluator.evaluate("2*(3+4)-5"))
        assertEquals(4, steps.size)
        assertEquals("9", steps.last().value)
        assertEquals("9", steps.last().result)
    }

    // ---------------- functions ----------------

    @Test fun aFunctionIsWorkedOutAsItsOwnStep() {
        val steps = StepSolver.steps("sin(30)+2", Evaluator.evaluate("sin(30)+2", degrees = true), degrees = true)
        assertEquals("sin(30)", steps[0].piece)
        assertEquals("0.5", steps[0].value)
        assertEquals("2.5", steps.last().value)
    }

    @Test fun aFunctionsBracketsAreNotRemoved() {
        // the bug this test exists for: sin(30) being flattened to sin30
        for (step in StepSolver.steps("sin(30)+2", Evaluator.evaluate("sin(30)+2", degrees = true), degrees = true)) {
            assertTrue("a bracket was lost: '${step.result}'", !step.result.contains("sin30"))
        }
    }

    @Test fun theSquareRootIsItsOwnStep() {
        assertEquals(listOf("4", "12"), values("sqrt(16)*3"))
    }

    // ---------------- simple cases ----------------

    @Test fun aSingleOperationIsOneStep() {
        assertEquals(listOf("1024"), values("2^10"))
        assertEquals(listOf("-3"), values("5-8"))
        assertEquals(listOf("6"), values("2*3"))
    }

    @Test fun aLongerChainKeepsGoing() {
        assertEquals("0.5", values("1/3+1/6").last())
    }

    @Test fun theLastStepIsTheAnswer() {
        for (e in listOf("2+3*4", "(2+3)*4", "10/4+2", "2*(3+4)-5", "100-20/5", "2^10", "sqrt(16)*3")) {
            val answer = Evaluator.evaluate(e)
            val steps = StepSolver.steps(e, answer)
            if (steps.isEmpty()) continue
            val arrived = steps.last().result.toDoubleOrNull()
            assertEquals("$e did not arrive at its own answer", answer, arrived!!, 1e-9)
        }
    }

    // ---------------- what it refuses ----------------

    @Test fun anExpressionThatCannotBeBrokenDownGivesNothing() {
        assertTrue(StepSolver.steps("", 0.0).isEmpty())
        // a bare number has no steps to show
        assertTrue(StepSolver.steps("42", 42.0).isEmpty())
    }

    @Test fun stepsThatDisagreeWithTheAnswerAreThrownAway() {
        // the answer handed in is deliberately wrong; nothing should be shown
        assertTrue(
            "working that does not reach the given answer must not be shown",
            StepSolver.steps("2+3*4", 99.0).isEmpty(),
        )
    }

    @Test fun dividingByZeroGivesNothingRatherThanAnInfinity() {
        assertTrue(StepSolver.steps("1/0", null).isEmpty())
    }

    // ---------------- the reasons ----------------

    @Test fun everyStepSaysWhyItWentFirst() {
        val steps = StepSolver.steps("2+3*4", Evaluator.evaluate("2+3*4"))
        assertTrue(steps.isNotEmpty())
        for (step in steps) {
            assertTrue("a step has no reason: $step", step.why.isNotBlank())
        }
    }
}

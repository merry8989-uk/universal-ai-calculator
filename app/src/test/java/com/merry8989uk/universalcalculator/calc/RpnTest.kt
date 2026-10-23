package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class RpnTest {

    private fun run(vararg ops: String, start: List<Double> = emptyList()): List<Double> {
        var s = start
        for (op in ops) s = if (op.toDoubleOrNull() != null) Rpn.push(s, op.toDouble()) else Rpn.apply(s, op)
        return s
    }

    @Test fun pushesValues() {
        assertEquals(listOf(3.0, 4.0), run("3", "4"))
    }

    @Test fun binaryOperations() {
        assertEquals(listOf(7.0), run("3", "4", "+"))
        assertEquals(listOf(-1.0), run("3", "4", "-"))
        assertEquals(listOf(12.0), run("3", "4", "*"))
        assertEquals(listOf(0.75), run("3", "4", "/"))
        assertEquals(listOf(8.0), run("2", "3", "^"))
    }

    @Test fun chainedEvaluation() {
        // (2 + 3) * 4 = 20
        assertEquals(listOf(20.0), run("2", "3", "+", "4", "*"))
    }

    @Test fun unaryOperations() {
        assertEquals(listOf(3.0), run("9", "sqrt"))
        assertEquals(listOf(0.25), run("4", "inv"))
        assertEquals(listOf(-4.0), run("4", "neg"))
        assertEquals(listOf(4.0), run("-4", "abs"))
        assertEquals(listOf(0.0), run("1", "ln"))
        assertEquals(listOf(2.0), run("100", "log"))
        assertEquals(listOf(1.0), run("0", "exp"))
    }

    @Test fun stackOperations() {
        assertEquals(listOf(2.0, 1.0), run("1", "2", "swap"))
        assertEquals(listOf(1.0), run("1", "2", "drop"))
        assertEquals(listOf(3.0, 3.0), run("3", "dup"))
        assertEquals(listOf(1.0, 3.0, 2.0), run("1", "2", "3", "swap"))
    }

    @Test fun errorsAreReported() {
        for (bad in listOf(
            listOf("+"),
            listOf("1", "+"),
            listOf("swap"),
            listOf("drop"),
            listOf("dup"),
            listOf("0", "inv"),
            listOf("-1", "sqrt"),
            listOf("0", "ln"),
            listOf("1", "0", "/"),
            listOf("1", "nope"),
        )) {
            try {
                run(*bad.toTypedArray())
                throw AssertionError("expected failure for $bad")
            } catch (e: Evaluator.EvalException) { /* expected */ }
        }
    }

    @Test fun everyAdvertisedOperationIsHandled() {
        // each operation must either work or throw a clean EvalException, never a crash
        for (op in Rpn.operations) {
            try {
                Rpn.apply(listOf(2.0, 3.0), op)
            } catch (e: Evaluator.EvalException) {
                throw AssertionError("'$op' should work with [2, 3]: ${e.message}")
            }
        }
    }
}

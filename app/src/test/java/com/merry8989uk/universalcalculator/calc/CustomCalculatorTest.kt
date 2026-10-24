package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The user's own calculators: what counts as valid, and whether they compute. */
class CustomCalculatorTest {

    private fun emi() = CustomCalculator(
        id = "c1",
        name = "Loan EMI",
        description = "Monthly payment on a loan",
        inputs = listOf(
            CustomInput("P", "Principal", 100000.0),
            CustomInput("r", "Monthly rate", 0.01),
            CustomInput("n", "Months", 12.0),
        ),
        expression = "P*r*(1+r)^n/((1+r)^n-1)",
        unit = "\u20B9",
    )

    @Test fun aWellFormedCalculatorHasNoProblem() {
        assertNull(emi().problem())
    }

    @Test fun itComputesWithItsInputs() {
        // verified in Python: 8884.878867834166
        assertEquals(8884.878867834166, emi().compute(emi().defaults()), 1e-6)
    }

    @Test fun itComputesWithValuesSuppliedLater() {
        val c = emi()
        val over = mapOf("P" to 200000.0, "r" to 0.01, "n" to 12.0)
        // twice the principal means twice the payment
        assertEquals(2 * 8884.878867834166, c.compute(over), 1e-6)
    }

    @Test fun theDefaultsComeFromTheInputs() {
        assertEquals(mapOf("P" to 100000.0, "r" to 0.01, "n" to 12.0), emi().defaults())
    }

    @Test fun aBlankNameIsRejected() {
        assertNotNull(emi().copy(name = "  ").problem())
    }

    @Test fun aBlankFormulaIsRejected() {
        assertNotNull(emi().copy(expression = "").problem())
    }

    @Test fun aFormulaThatWillNotParseIsRejected() {
        val problem = emi().copy(expression = "P * (r + ").problem()
        assertNotNull("an unbalanced bracket must be caught before saving", problem)
    }

    @Test fun anInputNameStartingWithADigitIsRejected() {
        val bad = emi().copy(inputs = listOf(CustomInput("2x", "Bad", 1.0)))
        assertNotNull(bad.problem())
    }

    @Test fun anInputNameWithASpaceIsRejected() {
        val bad = emi().copy(inputs = listOf(CustomInput("a b", "Bad", 1.0)))
        assertNotNull(bad.problem())
    }

    @Test fun aNameTheEvaluatorOwnsIsRejected() {
        // "sin" is a function, so it cannot also be an input.
        val bad = emi().copy(
            inputs = listOf(CustomInput("sin", "Bad", 1.0)),
            expression = "sin",
        )
        assertNotNull(bad.problem())
    }

    @Test fun duplicateInputsAreRejected() {
        val bad = emi().copy(
            inputs = listOf(CustomInput("x", "One", 1.0), CustomInput("x", "Two", 2.0)),
            expression = "x",
        )
        assertNotNull(bad.problem())
    }

    @Test fun anInputWithNoLabelIsRejected() {
        val bad = emi().copy(inputs = listOf(CustomInput("x", "  ", 1.0)), expression = "x")
        assertNotNull(bad.problem())
    }

    @Test fun aCalculatorWithNoInputsIsFine() {
        val c = CustomCalculator(id = "c", name = "Two", expression = "1+1")
        assertNull(c.problem())
        assertEquals(2.0, c.compute(emptyMap()), 1e-9)
    }

    @Test fun underscoresAndDigitsAreAllowedAfterTheFirstCharacter() {
        assertTrue(CustomCalculator.isValidKey("x1"))
        assertTrue(CustomCalculator.isValidKey("rate_2"))
        assertTrue(CustomCalculator.isValidKey("P"))
    }

    @Test fun itBecomesAFormulaTheSharedUiCanRender() {
        val f = emi().toFormula()
        assertEquals("custom_c1", f.id)
        assertEquals("Loan EMI", f.title)
        assertEquals(CustomCalculator.CATEGORY, f.category)
        assertEquals(3, f.inputs.size)
        assertEquals("P", f.inputs[0].key)
        // and computing through the Formula path agrees
        val out = f.compute(emi().defaults())
        assertEquals(1, out.size)
        assertEquals(8884.878867834166, out[0].value, 1e-6)
        assertEquals("\u20B9", out[0].unit)
    }

    @Test fun anEmptyDescriptionFallsBackToTheFormula() {
        val f = emi().copy(description = "").toFormula()
        assertEquals("P*r*(1+r)^n/((1+r)^n-1)", f.description)
    }
}

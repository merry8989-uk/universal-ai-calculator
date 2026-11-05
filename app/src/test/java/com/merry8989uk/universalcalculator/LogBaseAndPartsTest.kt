package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.formula.FormulaClassifier
import com.merry8989uk.universalcalculator.formula.FormulaNormaliser
import com.merry8989uk.universalcalculator.formula.FormulaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A base written after an underscore, and a line holding two formulas.
 *
 * The first was read as "log" times a variable times the argument, which is a
 * different number. The second was re-read whole when no part of it could be
 * computed, so the reason given was about a sentence nobody had written.
 */
class LogBaseAndPartsTest {

    // ---- log_b a ----

    @Test fun aBaseAfterAnUnderscoreIsABase() {
        val out = FormulaNormaliser.rewriteLogBases("log_b a")
        assertEquals("(lna/ln(b))", out.replace(" ", ""))
    }

    @Test fun aRaisedBaseStillWorks() {
        // the form the books use, which already worked and must not regress
        val out = FormulaNormaliser.rewriteLogBases("log\u2090b")
        assertEquals("(lnb/ln(a))", out.replace(" ", ""))
    }

    @Test fun aLogarithmWithNoBaseIsLeftForTheBracketingRule() {
        // log a is the common logarithm; there is no base to move
        assertEquals("log a", FormulaNormaliser.rewriteLogBases("log a"))
        assertEquals("log(x)", FormulaNormaliser.rewriteLogBases("log(x)"))
    }

    @Test fun theReciprocalOfALogarithmBecomesACalculator() {
        // log_a b = 1 / log_b a, which was reading as 1/log_b*a
        val d = FormulaClassifier.classify(
            "Algebra",
            "Reciprocal of a logarithm",
            "log\u2090b = 1 / log_b a",
        )
        assertTrue(
            "the right side was still wrong: ${d.type}",
            d.type != FormulaType.NEEDS_REVIEW,
        )
    }

    // ---- a line holding two formulas ----

    @Test fun aLineOfTwoFormulasIsNotReadAsOne() {
        val d = FormulaClassifier.classify(
            "Algebra",
            "Sum and product of roots",
            "x\u2081 + x\u2082 = \u2212b / a, x\u2081x\u2082 = c / a",
        )
        // whatever it comes out as, the reason must not be about the two halves
        // concatenated into an expression that is not one
        assertTrue(
            "the reading was concatenated: ${d.reason}",
            !d.reason.contains("x1*x2=c/a") || d.reason.contains("of 2 formulas"),
        )
        // and the reader still sees both formulas
        assertTrue("the line was cut", d.printed.contains("x\u2081x\u2082"))
    }

    @Test fun aSingleFormulaLineIsUnaffected() {
        val d = FormulaClassifier.classify("Algebra", "Area", "A = l * b")
        assertEquals(FormulaType.DIRECT_CALCULATOR, d.type)
        assertTrue("the reason changed", !d.reason.contains("of 1 formulas"))
    }
}

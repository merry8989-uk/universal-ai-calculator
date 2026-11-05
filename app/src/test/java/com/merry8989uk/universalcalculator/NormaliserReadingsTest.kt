package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.formula.FormulaNormaliser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Two readings the review list named as wrong, and neither was a hard case.
 *
 * `sin⁻¹x` was read as sine raised to the power minus one, and `ln a` was read as
 * the single name "lna". Both produced expressions the evaluator then rejected,
 * so whole sections of the library were listed as needing review when the fault
 * was in the reading.
 */
class NormaliserReadingsTest {

    // ---- sin⁻¹ is arcsin ----

    @Test fun anInversePowerOnAFunctionIsTheInverseFunction() {
        assertEquals("asin(-x)", FormulaNormaliser.rewriteInversePowers("sin\u207B\u00B9(-x)"))
        assertEquals("acos(x)", FormulaNormaliser.rewriteInversePowers("cos\u207B\u00B9x"))
        assertEquals("atan(x)", FormulaNormaliser.rewriteInversePowers("tan\u207B\u00B9x"))
    }

    @Test fun theLongerHyperbolicNameWins() {
        // sinh⁻¹ is arsinh, not asin times h⁻¹
        assertEquals("asinh(x)", FormulaNormaliser.rewriteInversePowers("sinh\u207B\u00B9x"))
        assertEquals("atanh(x)", FormulaNormaliser.rewriteInversePowers("tanh\u207B\u00B9x"))
    }

    @Test fun anOrdinaryNegativePowerIsLeftAlone() {
        // x⁻¹ is a reciprocal and must stay one; only a function name takes the
        // inverse reading
        assertEquals("x\u207B\u00B9", FormulaNormaliser.rewriteInversePowers("x\u207B\u00B9"))
        assertEquals("a\u207B\u00B2", FormulaNormaliser.rewriteInversePowers("a\u207B\u00B2"))
    }

    // ---- ln a is ln(a) ----

    @Test fun aFunctionWithoutBracketsGetsThem() {
        assertEquals("ln(a)", FormulaNormaliser.rewriteBareApplications("ln a"))
        assertEquals("sin(x)", FormulaNormaliser.rewriteBareApplications("sin x"))
    }

    @Test fun aCoefficientAndItsVariableAreLeftToTheTokeniser() {
        // cos 2θ is cos(2θ); taking the 2 alone here would break it
        assertEquals("cos 2\u03B8", FormulaNormaliser.rewriteBareApplications("cos 2\u03B8"))
    }

    @Test fun aFactorInFrontIsMadeExplicit() {
        // x ln a must become x*ln(a), or the brackets re-glue to the x
        assertEquals("x*ln(a)", FormulaNormaliser.rewriteBareApplications("x ln a"))
    }

    @Test fun everyApplicationOnTheLineIsBracketed() {
        assertEquals(
            "sin(A) = b / sin(B)",
            FormulaNormaliser.rewriteBareApplications("sin A = b / sin B"),
        )
    }

    @Test fun anArcNameIsBracketedToo() {
        // arcsin is rewritten to asin first, so the bracketing rule has to run
        // after that or it never sees the name it is meant to bracket
        assertEquals("asin(x)", FormulaNormaliser.rewriteBareApplications("asin x"))
    }

    @Test fun aBracketedArgumentIsNotTouched() {
        // already bracketed, so there is nothing to do and nothing to break
        assertEquals("sin(x)", FormulaNormaliser.rewriteBareApplications("sin(x)"))
        assertEquals("ln(a+b)", FormulaNormaliser.rewriteBareApplications("ln(a+b)"))
    }

    @Test fun aProductOfLettersIsNotAFunctionCall() {
        // the whole risk of this rule: ab must stay a product
        assertEquals("ab + cd", FormulaNormaliser.rewriteBareApplications("ab + cd"))
        assertEquals("2 a b", FormulaNormaliser.rewriteBareApplications("2 a b"))
    }

    @Test fun theWholeReadingWorksEndToEnd() {
        // e^(x ln a) — the case that was listed as unreadable
        val read = FormulaNormaliser.normalise("e^(x ln a)")
        assertTrue("the reading was $read", read != null)
        assertTrue("ln a was still glued together: $read", read!!.contains("ln(a)"))
    }
}

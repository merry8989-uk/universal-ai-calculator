package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.formula.FormulaClassifier
import com.merry8989uk.universalcalculator.formula.FormulaNormaliser
import com.merry8989uk.universalcalculator.formula.FormulaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An acronym is one name, and a chain of equals is one identity.
 *
 * Both were listed on the review list and neither was a hard case: EAR was being
 * read as E times AR because "e" is Euler's number on its own, and a chain like
 * the sine rule was being cut at the first equals sign, leaving the rest of the
 * chain as an expression that is not one.
 */
class AcronymAndChainTest {

    // ---- an acronym is one name ----

    @Test fun anAcronymStaysOneName() {
        // E is Euler's number; EAR is not E times AR
        val out = FormulaNormaliser.normalise("EAR = (1 + r/n)^n - 1")!!
        assertTrue("the reading was $out", out.startsWith("EAR"))
        assertTrue("the acronym was split: $out", !out.contains("E*AR"))
    }

    @Test fun aSingleCapitalIsStillEulersNumber() {
        // the fix must not cost the constant
        assertEquals("e", FormulaNormaliser.normalise("e")!!.lowercase())
        val out = FormulaNormaliser.normalise("e^x")!!
        assertTrue("the reading was $out", out.startsWith("e"))
    }

    @Test fun aKnownAcronymOfTwoLettersIsLeftAlone() {
        // PV is one name in the physics section and must stay one
        val out = FormulaNormaliser.normalise("PV = nRT")!!
        assertTrue("the reading was $out", out.contains("PV"))
    }

    @Test fun theEffectiveAnnualRateBecomesACalculator() {
        val d = FormulaClassifier.classify(
            "Algebra",
            "Effective annual rate",
            "EAR = (1 + r/n)\u207F \u2212 1",
        )
        assertEquals(FormulaType.DIRECT_CALCULATOR, d.type)
        assertEquals("EAR", d.target)
    }

    // ---- a chain of equals is one identity ----

    @Test fun aChainIsAReferenceRatherThanAFalseChecker() {
        // The sine rule holds for the sides and opposite angles of one triangle,
        // not for six free numbers. Checking it with independent values would
        // call it false; checking it with equal values would call "a = b = c"
        // true. So it is read and explained, and not claimed as a calculator.
        val d = FormulaClassifier.classify(
            "Geometry",
            "Sine rule",
            "a / sin A = b / sin B = c / sin C",
        )
        assertEquals(FormulaType.REFERENCE_ONLY, d.type)
        assertTrue("the reason was ${d.reason}", d.reason.contains("chain"))
    }

    @Test fun aChainIsNeverCalledAnIdentity() {
        // the trap: with every variable set to the same probe, a = b = c looks
        // like it holds, and the app would be claiming something untrue
        val d = FormulaClassifier.classify("Algebra", "Not a chain", "a = b = c")
        assertTrue("a chain was accepted as an identity: ${d.type}", d.type != FormulaType.CHECKER)
    }

    @Test fun anOrdinaryTwoSidedFormulaIsNotAChain() {
        val d = FormulaClassifier.classify("Algebra", "Area", "A = l * b")
        assertEquals(FormulaType.DIRECT_CALCULATOR, d.type)
    }
}

package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.formula.FormulaClassifier
import com.merry8989uk.universalcalculator.formula.FormulaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Two reasons the review list was longer than it should have been.
 *
 * Neither was a bug in the maths. One was a sentence after a colon being parsed
 * as though it were part of the formula; the other was calling a matrix
 * operation a mistake when it is simply something a number calculator cannot do.
 */
class ClassifierHonestyTest {

    // ---- the sentence after a colon is for the reader ----

    @Test fun theProseAfterAColonIsNotPartOfTheFormula() {
        assertEquals(
            "D = b\u00B2 \u2212 4ac",
            FormulaClassifier.stripTrailingProse(
                "D = b\u00B2 \u2212 4ac: two real roots if D > 0, one if D = 0",
            ),
        )
    }

    @Test fun aRatioIsNotProse() {
        // a colon with no sentence after it belongs to the maths
        assertEquals("x : y", FormulaClassifier.stripTrailingProse("x : y"))
        assertEquals("a : b : c", FormulaClassifier.stripTrailingProse("a : b : c"))
    }

    @Test fun aColonInsideBracketsIsLeftAlone() {
        // set-builder notation, where the colon is part of the meaning
        val text = "{ x : x \u2208 A or x \u2208 B }"
        assertEquals(text, FormulaClassifier.stripTrailingProse(text))
    }

    @Test fun theDiscriminantBecomesACalculatorAgain() {
        val d = FormulaClassifier.classify(
            "Algebra",
            "Discriminant",
            "D = b\u00B2 \u2212 4ac: two real roots if D > 0, one if D = 0",
        )
        assertEquals(FormulaType.DIRECT_CALCULATOR, d.type)
        assertTrue("the reading was ${d.expression}", d.expression!!.contains("b^2-4"))
        // and the reader still sees the sentence the book wrote
        assertTrue("the prose was lost", d.printed.contains("two real roots"))
    }

    // ---- a matrix operation is not a mistake ----

    @Test fun aMatrixOperationIsAReferenceRatherThanAReview() {
        val d = FormulaClassifier.classify(
            "Matrices & Determinants",
            "Determinant of a product",
            "det(AB) = det A \u00B7 det B",
        )
        assertEquals(FormulaType.REFERENCE_ONLY, d.type)
        assertTrue("the reason did not name the operation: ${d.reason}", d.reason.contains("det"))
    }

    @Test fun everyMatrixNameIsRecognised() {
        for (name in listOf("det", "adj", "rank", "nullity", "trace", "transpose")) {
            assertEquals(
                "not recognised: $name",
                name,
                FormulaClassifier.matrixOperationIn("$name(A) = something"),
            )
        }
    }

    // ---- a statement is not a calculation ----

    @Test fun aStatementIsAReferenceRatherThanAReview() {
        val d = FormulaClassifier.classify(
            "Vectors",
            "Perpendicular vectors",
            "a \u00B7 b = 0 \u21D4 a \u22A5 b",
        )
        assertEquals(FormulaType.REFERENCE_ONLY, d.type)
        assertTrue("the reason did not name the symbol: ${d.reason}", d.reason.contains("\u21D4"))
    }

    @Test fun aSetRelationIsAStatement() {
        val d = FormulaClassifier.classify("Sets", "Union", "A \u222A B = { x : x \u2208 A }")
        assertEquals(FormulaType.REFERENCE_ONLY, d.type)
    }

    @Test fun anOrdinaryFormulaIsNotAStatement() {
        assertEquals(null, FormulaClassifier.statementSymbolIn("a + b = c"))
        assertEquals(null, FormulaClassifier.statementSymbolIn("x = (-b + sqrt(b^2-4ac))/(2a)"))
    }

    @Test fun anOrdinaryWordContainingAMatrixNameIsNotOne() {
        // "detail" starts with det and must not be taken for a determinant
        assertEquals(null, FormulaClassifier.matrixOperationIn("detail(x)"))
        assertEquals(null, FormulaClassifier.matrixOperationIn("ranked(x)"))
        // and a plain product is not a call
        assertEquals(null, FormulaClassifier.matrixOperationIn("det * A"))
    }
}

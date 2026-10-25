package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.NamedMatrix
import com.merry8989uk.universalcalculator.calc.NamedMatrixText
import com.merry8989uk.universalcalculator.calc.defaultNamedMatrices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Named matrices: the matrices a user keeps, and the text they are stored as.
 *
 * The parse is the part that matters. It reads a file that could have been
 * edited by hand or half-written by a crash, and it must never hand back a
 * matrix whose rows are the wrong length — that would fail much later, in the
 * middle of an inversion, with no clue where it came from.
 */
class NamedMatrixTest {

    private fun m(vararg rows: List<Double>) = NamedMatrix("T", rows.toList())

    // ---------------- the model ----------------

    @Test fun theShapeIsReported() {
        val a = m(listOf(1.0, 2.0, 3.0), listOf(4.0, 5.0, 6.0))
        assertEquals(2, a.height)
        assertEquals(3, a.width)
        assertEquals("2\u00D73", a.shape)
        assertFalse(a.isSquare)
    }

    @Test fun aSquareMatrixSaysSo() {
        val a = m(listOf(1.0, 2.0), listOf(3.0, 4.0))
        assertTrue(a.isSquare)
        assertEquals("2\u00D72", a.shape)
    }

    @Test fun anEmptyMatrixIsRecognised() {
        assertTrue(NamedMatrix("T", emptyList()).isEmpty)
        assertTrue(NamedMatrix("T", listOf(emptyList())).isEmpty)
        assertFalse(m(listOf(1.0)).isEmpty)
    }

    @Test fun itConvertsToTheArrayTheEngineWants() {
        val a = m(listOf(1.0, 2.0), listOf(3.0, 4.0))
        val array = a.toArray()
        assertEquals(2, array.size)
        assertEquals(2, array[0].size)
        assertEquals(3.0, array[1][0], 1e-12)
    }

    @Test fun theTextIsOneRowPerLine() {
        val a = m(listOf(1.0, 2.0), listOf(3.0, 4.0))
        assertEquals("1 2\n3 4", a.text())
    }

    @Test fun anAnswerIsNamedMatAns() {
        val a = NamedMatrix.answer(arrayOf(doubleArrayOf(1.0, 2.0)))
        assertEquals("MatAns", a.name)
        assertEquals("1\u00D72", a.shape)
    }

    // ---------------- the text ----------------

    @Test fun aMatrixSurvivesTheRoundTrip() {
        val original = listOf(
            NamedMatrix("MatA", listOf(listOf(1.0, 0.0), listOf(0.0, 1.0))),
            NamedMatrix("MatB", listOf(listOf(1.5, -2.25, 3.0))),
        )
        val back = NamedMatrixText.parse(NamedMatrixText.serialize(original))
        assertEquals(original.size, back.size)
        original.forEachIndexed { i, want ->
            assertEquals(want.name, back[i].name)
            assertEquals(want.rows, back[i].rows)
        }
    }

    @Test fun aFractionSurvivesToo() {
        val original = listOf(NamedMatrix("T", listOf(listOf(1.0 / 3.0))))
        val back = NamedMatrixText.parse(NamedMatrixText.serialize(original))
        assertEquals(1.0 / 3.0, back[0].rows[0][0], 1e-12)
    }

    @Test fun theDefaultsAreWhatTheReferenceOffers() {
        val names = defaultNamedMatrices().map { it.name }
        assertEquals(listOf("MatA", "MatB", "MatC", "MatD", "MatAns"), names)
        assertTrue(defaultNamedMatrices().all { it.shape == "3\u00D73" })
    }

    // ---------------- what it refuses ----------------

    @Test fun aFileWithoutTheHeaderIsIgnored() {
        assertTrue(NamedMatrixText.parse("something else\nMatA|1|1\n5").isEmpty())
        assertTrue(NamedMatrixText.parse("").isEmpty())
    }

    @Test fun aRowOfTheWrongLengthIsRefused() {
        // the header promises two cells, the row has three
        val text = "NEXUSMATRIX1\nT|2|2\n1 2 3\n4 5 6\n"
        assertTrue("a ragged matrix must not be handed back", NamedMatrixText.parse(text).isEmpty())
    }

    @Test fun aMissingRowIsRefused() {
        val text = "NEXUSMATRIX1\nT|3|2\n1 2\n3 4\n"
        assertTrue(NamedMatrixText.parse(text).isEmpty())
    }

    @Test fun aCellThatIsNotANumberIsRefused() {
        val text = "NEXUSMATRIX1\nT|2|2\n1 2\n3 x\n"
        assertTrue(NamedMatrixText.parse(text).isEmpty())
    }

    @Test fun anAbsurdSizeIsRefused() {
        assertTrue(NamedMatrixText.parse("NEXUSMATRIX1\nT|99999|2\n1 2\n").isEmpty())
        assertTrue(NamedMatrixText.parse("NEXUSMATRIX1\nT|0|0\n").isEmpty())
        assertTrue(NamedMatrixText.parse("NEXUSMATRIX1\nT|-3|2\n1 2\n").isEmpty())
    }

    @Test fun aBadHeadLineIsSkippedWithoutLosingTheRest() {
        // one unreadable entry must not take the others with it
        val text = "NEXUSMATRIX1\nnonsense\nT|1|2\n1 2\n"
        val back = NamedMatrixText.parse(text)
        assertEquals(1, back.size)
        assertEquals("T", back[0].name)
    }

    @Test fun aNameCanCarrySpaces() {
        val original = listOf(NamedMatrix("My Matrix", listOf(listOf(7.0))))
        val back = NamedMatrixText.parse(NamedMatrixText.serialize(original))
        assertEquals("My Matrix", back[0].name)
    }

    // ---------------- what it is for ----------------

    @Test fun aResultCanBeUsedAsAnInput() {
        // the whole point of MatAns: work something out, then use it
        val a = arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0))
        val transposed = com.merry8989uk.universalcalculator.calc.Matrix.transpose(a)
        val kept = NamedMatrix.answer(transposed)
        assertEquals("MatAns", kept.name)
        // and the engine can take it straight back
        assertEquals(
            com.merry8989uk.universalcalculator.calc.Matrix.determinant(a),
            com.merry8989uk.universalcalculator.calc.Matrix.determinant(kept.toArray()),
            1e-12,
        )
    }
}

package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Matrices and vectors, where the arithmetic is scalar.
 *
 * A determinant of a matrix is not something a number engine can do, and these
 * are not that: they take the entries as numbers and produce the one number the
 * formula is for. What cannot be done this way — the transpose of a product, an
 * inverse still written with det(A) inside it — stays a reference.
 */
class MatrixVectorTest {

    private fun run(id: String, values: Map<String, Double>): Map<String, Double> {
        val f = FormulaLibrary.formulas.firstOrNull { it.id == id }
        assertNotNull("there is no formula called $id", f)
        return f!!.compute(values).associate { it.label to it.value }
    }

    @Test fun aTwoByTwoDeterminant() {
        // [[1,2],[3,4]] has determinant 4 - 6 = -2
        val r = run("det2", mapOf("a" to 1.0, "b" to 2.0, "c" to 3.0, "d" to 4.0))
        assertEquals(-2.0, r["Determinant"]!!, 1e-9)
    }

    @Test fun aThreeByThreeDeterminant() {
        // expanded along the first row, and the middle term is subtracted
        val r = run(
            "det3",
            mapOf(
                "a" to 1.0, "b" to 2.0, "c" to 3.0,
                "d" to 4.0, "e" to 5.0, "f" to 6.0,
                "g" to 7.0, "h" to 8.0, "i" to 10.0,
            ),
        )
        assertEquals(-3.0, r["Determinant"]!!, 1e-9)
    }

    @Test fun theInverseOfATwoByTwo() {
        // [[4,7],[2,6]] has determinant 10, so the inverse is (1/10)[[6,-7],[-2,4]]
        val r = run("inv2", mapOf("a" to 4.0, "b" to 7.0, "c" to 2.0, "d" to 6.0))
        assertEquals(10.0, r["Determinant"]!!, 1e-9)
        assertEquals(0.6, r["Top left"]!!, 1e-9)
        assertEquals(-0.7, r["Top right"]!!, 1e-9)
        assertEquals(-0.2, r["Bottom left"]!!, 1e-9)
        assertEquals(0.4, r["Bottom right"]!!, 1e-9)
    }

    @Test fun aTraceIsTheDiagonal() {
        val r = run("trace3", mapOf("a" to 1.0, "e" to 5.0, "i" to 10.0))
        assertEquals(16.0, r["Trace"]!!, 1e-9)
    }

    @Test fun theLengthOfAVector() {
        val r = run("vec_magnitude", mapOf("x" to 1.0, "y" to 2.0, "z" to 3.0))
        assertEquals(Math.sqrt(14.0), r["Magnitude"]!!, 1e-9)
    }

    @Test fun aDotProductFromComponents() {
        val r = run(
            "dot_components",
            mapOf("a1" to 1.0, "a2" to 2.0, "a3" to 3.0, "b1" to 4.0, "b2" to 5.0, "b3" to 6.0),
        )
        assertEquals(32.0, r["Dot product"]!!, 1e-9)
    }

    @Test fun aDotProductFromMagnitudes() {
        // 3 x 4 x cos 60 = 6
        val r = run("dot_magnitudes", mapOf("a" to 3.0, "b" to 4.0, "th" to 60.0))
        assertEquals(6.0, r["Dot product"]!!, 1e-9)
    }

    @Test fun perpendicularVectorsHaveNoDotProduct() {
        // the check that the angle is being read in degrees and not radians
        val r = run("dot_magnitudes", mapOf("a" to 5.0, "b" to 7.0, "th" to 90.0))
        assertEquals(0.0, r["Dot product"]!!, 1e-9)
    }

    @Test fun theMagnitudeOfACrossProduct() {
        val r = run(
            "cross_magnitude",
            mapOf("a1" to 1.0, "a2" to 2.0, "a3" to 3.0, "b1" to 4.0, "b2" to 5.0, "b3" to 6.0),
        )
        assertEquals(Math.sqrt(54.0), r["Magnitude"]!!, 1e-9)
    }

    @Test fun aScalarTripleProductIsZeroWhenCoplanar() {
        // (1,2,3), (4,5,6), (7,8,9) lie in one plane, so the box has no volume
        val r = run(
            "scalar_triple",
            mapOf(
                "a1" to 1.0, "a2" to 2.0, "a3" to 3.0,
                "b1" to 4.0, "b2" to 5.0, "b3" to 6.0,
                "c1" to 7.0, "c2" to 8.0, "c3" to 9.0,
            ),
        )
        assertEquals(0.0, r["Triple product"]!!, 1e-9)
    }

    @Test fun theAngleBetweenTwoAxes() {
        val r = run(
            "angle_between_vectors",
            mapOf("a1" to 1.0, "a2" to 0.0, "a3" to 0.0, "b1" to 0.0, "b2" to 1.0, "b3" to 0.0),
        )
        assertEquals(90.0, r["Angle in degrees"]!!, 1e-9)
    }

    @Test fun theSameVectorWithItselfIsNoAngle() {
        val r = run(
            "angle_between_vectors",
            mapOf("a1" to 2.0, "a2" to 3.0, "a3" to 4.0, "b1" to 2.0, "b2" to 3.0, "b3" to 4.0),
        )
        assertEquals(0.0, r["Angle in degrees"]!!, 1e-9)
    }

    @Test fun everyNewEntryIsInTheLibraryAndInACategory() {
        for (id in listOf(
            "det2", "det3", "inv2", "trace3", "vec_magnitude", "dot_components",
            "dot_magnitudes", "cross_magnitude", "scalar_triple", "angle_between_vectors",
        )) {
            val f = FormulaLibrary.formulas.firstOrNull { it.id == id }
            assertNotNull("missing from the library: $id", f)
            assertTrue("no category on $id", f!!.category.isNotBlank())
            assertTrue("no inputs on $id", f.inputs.isNotEmpty())
        }
    }
}

package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The four matrix calculators a number engine really can do.
 *
 * A determinant is not something an engine of scalars can work on — these take
 * the entries as numbers and produce the one number the formula is for, which is
 * what a formula calculator is for.
 *
 * The vectors are not here because the library already had them: magnitude, dot,
 * cross magnitude, angle, projection and triple product were all built before
 * this. Writing them again under new names would have been six duplicates with
 * two sets of answers to keep in step.
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
                "d" to 4.0, "e2" to 5.0, "f" to 6.0,
                "g" to 7.0, "h" to 8.0, "i" to 10.0,
            ),
        )
        assertEquals(-3.0, r["Determinant"]!!, 1e-9)
    }

    @Test fun theMiddleEntryIsNotEulersNumber() {
        // the key is e2 for exactly this reason: "e" is a constant to the
        // evaluator, so a box labelled e would have been ignored
        val withFive = run(
            "trace3",
            mapOf("a" to 1.0, "e2" to 5.0, "i" to 10.0),
        )
        assertEquals(16.0, withFive["Trace"]!!, 1e-9)

        val withTwenty = run(
            "trace3",
            mapOf("a" to 1.0, "e2" to 20.0, "i" to 10.0),
        )
        assertEquals(31.0, withTwenty["Trace"]!!, 1e-9)
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

    @Test fun theInverseOfASingularMatrixIsNotANumber() {
        // [[1,2],[2,4]] has determinant zero; the app must not pretend otherwise
        val r = run("inv2", mapOf("a" to 1.0, "b" to 2.0, "c" to 2.0, "d" to 4.0))
        assertEquals(0.0, r["Determinant"]!!, 1e-9)
        val topLeft = r["Top left"]!!
        assertTrue(
            "a singular matrix produced a finite inverse: $topLeft",
            topLeft.isInfinite() || topLeft.isNaN(),
        )
    }

    @Test fun everyNewEntryIsInTheLibraryAndInADeclaredCategory() {
        for (id in listOf("det2", "det3", "inv2", "trace3")) {
            val f = FormulaLibrary.formulas.firstOrNull { it.id == id }
            assertNotNull("missing from the library: $id", f)
            assertTrue("'${f!!.category}' is not a declared category", f.category in FormulaLibrary.categories)
            assertTrue("no inputs on $id", f.inputs.isNotEmpty())
        }
    }

    @Test fun theVectorsWereAlreadyThere() {
        // written down so that nobody adds them a second time
        for (id in listOf(
            "vec_magnitude", "vec_dot", "vec_cross_magnitude", "vec_angle", "vec_triple",
        )) {
            val f = FormulaLibrary.formulas.firstOrNull { it.id == id }
            assertNotNull("the library lost its $id", f)
        }
        val magnitudes = FormulaLibrary.formulas.count { it.id == "vec_magnitude" }
        assertEquals("vec_magnitude was added twice", 1, magnitudes)
    }
}

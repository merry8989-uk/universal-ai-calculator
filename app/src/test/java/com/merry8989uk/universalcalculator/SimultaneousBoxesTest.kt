package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.EquationSolver
import com.merry8989uk.universalcalculator.calc.LinearSolution
import com.merry8989uk.universalcalculator.ui.boxIndex
import com.merry8989uk.universalcalculator.ui.readSystem
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The simultaneous screen's boxes, read back as a system.
 *
 * The solver underneath was already right, so [EquationSolverTest] covering it
 * could not have caught this: what was wrong was the walk from the boxes on
 * screen to the numbers handed over. A fixed stride of three put the answer of
 * a three-unknown row on top of its last coefficient, so a wrong answer came
 * back with nothing complaining, and a fourth unknown walked off the end of the
 * list. These tests therefore build the boxes exactly as the screen builds them
 * and read them exactly as the screen reads them; every answer below was worked
 * out by hand first.
 */
class SimultaneousBoxesTest {

    /** The boxes as the screen lays them out: row-major, one answer box per row. */
    private fun boxes(size: Int, vararg cells: String): List<String> {
        assertEquals("a $size-unknown grid holds ${size * (size + 1)} boxes", size * (size + 1), cells.size)
        return cells.toList()
    }

    /** What the Solve button does with these boxes. */
    private fun solve(size: Int, vararg cells: String): LinearSolution {
        val system = readSystem(boxes(size, *cells), size)
        assertNotNull("the screen should read a full grid as a system", system)
        return EquationSolver.simultaneous(system!!.first, system.second)
    }

    private fun valuesOf(solution: LinearSolution): DoubleArray {
        assertTrue("expected one answer each, got $solution", solution is LinearSolution.Unique)
        return (solution as LinearSolution.Unique).values
    }

    @Test fun aTwoUnknownSystemSolvesFromItsBoxes() {
        // x + y = 10 and x - y = 2, so x = 6 and y = 4
        val v = valuesOf(solve(2, "1", "1", "10", "1", "-1", "2"))
        assertEquals(6.0, v[0], 1e-9)
        assertEquals(4.0, v[1], 1e-9)
    }

    @Test fun aThreeUnknownSystemSolvesFromItsBoxes() {
        // x + y + z = 6
        // 2x - y + z = 3
        // x + 2y - z = 2   =>  x = 1, y = 2, z = 3
        val v = valuesOf(
            solve(
                3,
                "1", "1", "1", "6",
                "2", "-1", "1", "3",
                "1", "2", "-1", "2",
            ),
        )
        assertEquals(1.0, v[0], 1e-9)
        assertEquals(2.0, v[1], 1e-9)
        assertEquals(3.0, v[2], 1e-9)
    }

    @Test fun aFourUnknownSystemSolvesInsteadOfRunningOffTheEnd() {
        // x + y + z + w = 10
        // x - y + z + w = 6
        // x + y - z + w = 4
        // x + y + z - w = 2   =>  x = 1, y = 2, z = 3, w = 4
        val v = valuesOf(
            solve(
                4,
                "1", "1", "1", "1", "10",
                "1", "-1", "1", "1", "6",
                "1", "1", "-1", "1", "4",
                "1", "1", "1", "-1", "2",
            ),
        )
        assertEquals(1.0, v[0], 1e-9)
        assertEquals(2.0, v[1], 1e-9)
        assertEquals(3.0, v[2], 1e-9)
        assertEquals(4.0, v[3], 1e-9)
    }

    @Test fun theAnswerOfARowIsNotReadAsItsLastCoefficient() {
        // the same 3x3 system, read on its own: every answer box holds a number
        // that appears nowhere in its row's coefficients, so a reader that took
        // the fourth box as a coefficient would show 6, 3, 2 in the matrix and
        // the start of the next row as the right-hand side
        val system = readSystem(
            boxes(
                3,
                "1", "1", "1", "6",
                "2", "-1", "1", "3",
                "1", "2", "-1", "2",
            ),
            3,
        )
        assertNotNull("the screen should read a full grid as a system", system)
        val (a, b) = system!!
        assertArrayEquals(doubleArrayOf(1.0, 1.0, 1.0), a[0], 1e-12)
        assertArrayEquals(doubleArrayOf(2.0, -1.0, 1.0), a[1], 1e-12)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, -1.0), a[2], 1e-12)
        assertArrayEquals(doubleArrayOf(6.0, 3.0, 2.0), b, 1e-12)
    }

    @Test fun eachRowStartsAfterTheAnswerOfTheRowBefore() {
        // the answer box of the first row is the last box of that row, so the
        // next row begins one place further on; a stride of three would put
        // them on the same box at three unknowns
        assertEquals(3, boxIndex(3, 0, 3))
        assertEquals(4, boxIndex(3, 1, 0))
        assertEquals(4, boxIndex(4, 0, 4))
        assertEquals(5, boxIndex(4, 1, 0))
        assertEquals(3, boxIndex(2, 1, 0))
    }
}

package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.FunctionTableBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TABLE mode.
 *
 * The point of a table is that every cell is right, so each range here was
 * worked out by hand first. A cell the expression cannot fill must come back
 * empty rather than taking the rest of the table down with it.
 */
class FunctionTableTest {

    private fun build(
        f: String,
        g: String = "",
        start: Double,
        stop: Double,
        step: Double,
        max: Int = FunctionTableBuilder.DEFAULT_MAX_ENTRIES,
    ) = FunctionTableBuilder.build(f, g, start, stop, step, max)

    // ---------------- the ordinary case ----------------

    @Test fun aSquareAcrossASmallRange() {
        val t = build("x^2", start = -3.0, stop = 3.0, step = 1.0)
        assertEquals(7, t.rows.size)
        assertEquals(listOf(-3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0), t.rows.map { it.x })
        assertEquals(listOf(9.0, 4.0, 1.0, 0.0, 1.0, 4.0, 9.0), t.rows.map { it.f })
        assertFalse(t.truncated)
    }

    @Test fun aFractionalStepKeepsItsExactness() {
        // a step of 0.1 must not leave 0.30000000000000004 in the x column
        val t = build("x", start = 0.0, stop = 0.5, step = 0.1)
        assertEquals(listOf(0.0, 0.1, 0.2, 0.3, 0.4, 0.5), t.rows.map { it.x })
    }

    @Test fun theStepIsNotDroppedAtTheEnd() {
        // 0 to 1 by 0.25 must include both ends
        val t = build("x", start = 0.0, stop = 1.0, step = 0.25)
        assertEquals(listOf(0.0, 0.25, 0.5, 0.75, 1.0), t.rows.map { it.x })
    }

    @Test fun aSinglePointRange() {
        val t = build("x+1", start = 5.0, stop = 5.0, step = 1.0)
        assertEquals(1, t.rows.size)
        assertEquals(5.0, t.rows[0].x, 1e-12)
        assertEquals(6.0, t.rows[0].f!!, 1e-12)
    }

    // ---------------- the second function ----------------

    @Test fun theSecondFunctionRunsAlongside() {
        val t = build("x^2", "x+1", start = 0.0, stop = 3.0, step = 1.0)
        assertEquals(4, t.rows.size)
        assertEquals(listOf(0.0, 1.0, 4.0, 9.0), t.rows.map { it.f })
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0), t.rows.map { it.g })
    }

    @Test fun withoutASecondFunctionTheColumnIsEmpty() {
        val t = build("x^2", start = 0.0, stop = 2.0, step = 1.0)
        assertTrue(t.rows.all { it.g == null })
    }

    // ---------------- the cells that have no value ----------------

    @Test fun aCellWithNoValueIsLeftEmpty() {
        // 1/x at zero, and the square root of the negatives
        val t = build("1/x", start = -2.0, stop = 2.0, step = 1.0)
        assertEquals(5, t.rows.size)
        assertNull("1/0 must be empty", t.rows.first { it.x == 0.0 }.f)
        assertEquals(-0.5, t.rows.first { it.x == -2.0 }.f!!, 1e-12)
    }

    @Test fun oneBadCellDoesNotTakeTheTableDown() {
        val t = build("sqrt(x)", start = -2.0, stop = 2.0, step = 1.0)
        assertEquals(5, t.rows.size)
        assertNull(t.rows.first { it.x == -2.0 }.f)
        assertNull(t.rows.first { it.x == -1.0 }.f)
        assertEquals(0.0, t.rows.first { it.x == 0.0 }.f!!, 1e-12)
        assertEquals(2.0, t.rows.first { it.x == 2.0 }.f!!, 1e-12)
    }

    @Test fun anInfiniteValueIsLeftEmptyRatherThanShown() {
        val t = build("1/x", start = 0.0, stop = 1.0, step = 1.0)
        assertNull(t.rows.first { it.x == 0.0 }.f)
        assertEquals(1.0, t.rows.first { it.x == 1.0 }.f!!, 1e-12)
    }

    // ---------------- the range that goes backwards ----------------

    @Test fun aRangeThatCountsDownWorksToo() {
        val t = build("x^2", start = 3.0, stop = -3.0, step = 1.0)
        assertEquals(7, t.rows.size)
        assertEquals(listOf(3.0, 2.0, 1.0, 0.0, -1.0, -2.0, -3.0), t.rows.map { it.x })
        assertTrue("the step should have been turned round", t.step < 0)
    }

    @Test fun theDirectionComesFromTheTwoEndsNotTheSignOfTheStep() {
        // a negative step with the ends the other way round must still work
        val t = build("x", start = 0.0, stop = 2.0, step = -1.0)
        assertEquals(listOf(0.0, 1.0, 2.0), t.rows.map { it.x })
        assertTrue(t.step > 0)
    }

    // ---------------- the entry limit ----------------

    @Test fun theRangeIsCutShortAtTheLimit() {
        val t = build("x^2", start = -3.0, stop = 3.0, step = 1.0, max = 3)
        assertEquals(3, t.rows.size)
        assertTrue("the caller should be told the range was cut", t.truncated)
    }

    @Test fun aLimitThatExactlyFitsIsNotCalledTruncated() {
        val t = build("x^2", start = -3.0, stop = 3.0, step = 1.0, max = 7)
        assertEquals(7, t.rows.size)
        assertFalse("nothing was left out", t.truncated)
    }

    @Test fun aSillyLimitIsKeptInCheck() {
        val t = build("x", start = 0.0, stop = 1000000.0, step = 1.0, max = 999999)
        assertTrue("the hard ceiling must hold", t.rows.size <= FunctionTableBuilder.HARD_MAX_ENTRIES)
    }

    // ---------------- the inputs that make no sense ----------------

    @Test fun aStepOfZeroBuildsNothing() {
        assertTrue(build("x^2", start = 0.0, stop = 10.0, step = 0.0).isEmpty)
    }

    @Test fun anEmptyFunctionBuildsNothing() {
        assertTrue(build("", start = 0.0, stop = 10.0, step = 1.0).isEmpty)
        assertTrue(build("   ", start = 0.0, stop = 10.0, step = 1.0).isEmpty)
    }

    @Test fun aRangeThatNeverReachesItsEndIsStillBounded() {
        // start below stop with a positive step, but the two ends are equal-ish
        val t = build("x", start = 0.0, stop = 0.0, step = 1.0)
        assertEquals(1, t.rows.size)
    }

    @Test fun aFunctionThatCannotBeReadBuildsNoRows() {
        val t = build("nonsense(((", start = 0.0, stop = 2.0, step = 1.0)
        // the rows exist, but every cell is empty
        assertTrue(t.rows.isEmpty() || t.rows.all { it.f == null })
    }

    // ---------------- the row count the screen warns with ----------------

    @Test fun theRowCountMatchesWhatIsBuilt() {
        for ((start, stop, step) in listOf(
            Triple(-3.0, 3.0, 1.0),
            Triple(0.0, 1.0, 0.25),
            Triple(0.0, 10.0, 2.0),
            Triple(5.0, 5.0, 1.0),
        )) {
            val predicted = FunctionTableBuilder.countFor(start, stop, step)
            val built = build("x", start = start, stop = stop, step = step).rows.size
            assertEquals("countFor disagrees for $start..$stop by $step", built, predicted)
        }
    }

    @Test fun theRowCountOfAStepOfZeroIsZero() {
        assertEquals(0, FunctionTableBuilder.countFor(0.0, 10.0, 0.0))
    }

    // ---------------- the settings reach the cells ----------------

    @Test fun theAngleUnitReachesTheCells() {
        val degrees = FunctionTableBuilder.build(
            "sin(x)", "", 90.0, 90.0, 1.0,
            angle = com.merry8989uk.universalcalculator.calc.AngleUnit.DEGREE,
        )
        assertEquals(1.0, degrees.rows[0].f!!, 1e-9)

        val radians = FunctionTableBuilder.build(
            "sin(x)", "", Math.PI / 2, Math.PI / 2, 1.0,
            angle = com.merry8989uk.universalcalculator.calc.AngleUnit.RADIAN,
        )
        assertEquals(1.0, radians.rows[0].f!!, 1e-9)
    }

    @Test fun theXVariableIsTheOnlyThingTheFunctionSees() {
        // every row must get its own x, not the first one repeated
        val t = build("x*10", start = 1.0, stop = 4.0, step = 1.0)
        assertEquals(listOf(10.0, 20.0, 30.0, 40.0), t.rows.map { it.f })
    }

    @Test fun aRowAlwaysHasAnXEvenWhenTheCellIsEmpty() {
        val t = build("1/x", start = -1.0, stop = 1.0, step = 1.0)
        assertTrue("every row must carry its x", t.rows.all { it.x.isFinite() })
        assertNotNull(t.rows.first { it.x == 0.0 })
    }
}

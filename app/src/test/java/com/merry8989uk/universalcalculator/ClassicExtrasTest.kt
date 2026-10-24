package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.AngleUnit
import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.ExpressionFix
import com.merry8989uk.universalcalculator.ui.CLASSIC_ROWS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pieces that make the classic pad a classic pad: gradians, the two input
 * conveniences, and the promise that hiding the number pad loses no key.
 */
class ClassicExtrasTest {

    // ---- angle units ----

    @Test fun aFullTurnIsTheSameAngleInEveryUnit() {
        for (unit in AngleUnit.entries) {
            assertEquals(unit.name, 2 * Math.PI, unit.toRadians(unit.perTurn), 1e-9)
        }
    }

    @Test fun aRightAngleIsTheSameAngleInEveryUnit() {
        assertEquals(Math.PI / 2, AngleUnit.DEGREE.toRadians(90.0), 1e-12)
        assertEquals(Math.PI / 2, AngleUnit.RADIAN.toRadians(Math.PI / 2), 1e-12)
        assertEquals(Math.PI / 2, AngleUnit.GRADIAN.toRadians(100.0), 1e-12)
    }

    @Test fun gradiansReachTheEvaluator() {
        assertEquals(1.0, Evaluator.evaluate("sin(100)", unit = AngleUnit.GRADIAN), 1e-12)
        assertEquals(0.5, Evaluator.evaluate("sin(30)", unit = AngleUnit.DEGREE), 1e-12)
        assertEquals(1.0, Evaluator.evaluate("sin(pi/2)", unit = AngleUnit.RADIAN), 1e-12)
    }

    @Test fun theUnitIsReadTolerantly() {
        assertEquals(AngleUnit.GRADIAN, AngleUnit.of("grad"))
        assertEquals(AngleUnit.GRADIAN, AngleUnit.of("GRA"))
        assertEquals(AngleUnit.RADIAN, AngleUnit.of("rad"))
        assertEquals(AngleUnit.DEGREE, AngleUnit.of("nonsense"))
    }

    @Test fun degreesStillWorkTheOldWay() {
        // the boolean entry point must keep behaving as it always did
        assertEquals(0.5, Evaluator.evaluate("sin(30)", degrees = true), 1e-12)
    }

    // ---- the two input conveniences ----

    @Test fun bracketsCanBeLeftAlone() {
        assertEquals("sin(30)", ExpressionFix.balance("sin(30"))
        assertEquals("sin(30", ExpressionFix.balance("sin(30", closeBrackets = false))
    }

    @Test fun aTrailingOperatorCanBeKept() {
        assertEquals("5+", ExpressionFix.balance("5+"))
        assertEquals("5+", ExpressionFix.balance("5+", dropTrailingOperator = false))
    }

    // ---- nothing is lost when the number pad steps aside ----

    @Test fun theClassicPadCarriesEveryDigitAndOperator() {
        val actions = CLASSIC_ROWS.flatten().map { it.action }.toSet()
        for (d in "0123456789") {
            assertTrue("the classic pad has no '$d'", actions.contains(d.toString()))
        }
        for (key in listOf(".", "+", "-", "*", "/", "=", "C", "del", "(", ")")) {
            assertTrue("the classic pad has no '$key'", actions.contains(key))
        }
    }

    @Test fun theClassicPadHasNoEmptyKey() {
        val blank = CLASSIC_ROWS.flatten().filter { it.action.isBlank() && it.tool == null }
        assertTrue("keys that do nothing: $blank", blank.isEmpty())
    }

    @Test fun theNumberPadIsNotTheOnlyWayToAValue() {
        // every digit on the classic pad types something the evaluator can read
        val failures = CLASSIC_ROWS.flatten()
            .filter { it.action.length == 1 && it.action[0].isDigit() }
            .filter { Evaluator.evaluate(it.action) != it.action.toDouble() }
            .map { it.label }
        assertFalse("digits that do not evaluate: $failures", failures.isNotEmpty())
    }
}

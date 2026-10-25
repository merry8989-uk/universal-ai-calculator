package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.PercentageMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Shorthand, so the expectations below stay readable. */
private typealias PM = PercentageMode

/**
 * What a percent sign means after + or -.
 *
 * The two settings differ only there: multiplication and division always read a
 * percent as the number divided by a hundred, and that must not change.
 */
class PercentageModeTest {

    private fun of(expr: String, mode: PM) = Evaluator.evaluate(expr, percentage = mode)

    // ---- the difference, which is the whole point ----

    @Test fun addingAPercentOfTheTotal() {
        assertEquals(120.0, of("100 + 20%", PM.MULTIPLICATIVE), 1e-9)
    }

    @Test fun addingAPercentAsAPlainNumber() {
        assertEquals(100.2, of("100 + 20%", PM.ADDITIVE), 1e-9)
    }

    @Test fun subtractingAPercentOfTheTotal() {
        assertEquals(80.0, of("100 - 20%", PM.MULTIPLICATIVE), 1e-9)
    }

    @Test fun subtractingAPercentAsAPlainNumber() {
        assertEquals(99.8, of("100 - 20%", PM.ADDITIVE), 1e-9)
    }

    @Test fun theDefaultIsTheOnePeopleExpectFromAShop() {
        // no mode given means multiplicative
        assertEquals(220.0, Evaluator.evaluate("200 + 10%"), 1e-9)
    }

    // ---- what must not change ----

    @Test fun multiplicationIsTheSameEitherWay() {
        assertEquals(20.0, of("200 * 10%", PM.MULTIPLICATIVE), 1e-9)
        assertEquals(20.0, of("200 * 10%", PM.ADDITIVE), 1e-9)
    }

    @Test fun divisionIsTheSameEitherWay() {
        assertEquals(2000.0, of("200 / 10%", PM.MULTIPLICATIVE), 1e-9)
        assertEquals(2000.0, of("200 / 10%", PM.ADDITIVE), 1e-9)
    }

    @Test fun aPercentOnItsOwnIsTheSameEitherWay() {
        assertEquals(0.5, of("50%", PM.MULTIPLICATIVE), 1e-9)
        assertEquals(0.5, of("50%", PM.ADDITIVE), 1e-9)
    }

    @Test fun anExpressionWithoutAPercentIsUntouched() {
        for (expr in listOf("2+3", "10*4", "100/8", "2^10", "(1+2)*3")) {
            assertEquals(expr, of(expr, PM.MULTIPLICATIVE), of(expr, PM.ADDITIVE), 1e-9)
        }
    }

    // ---- the cases a real keypad produces ----

    @Test fun aChainOfPercentagesUsesTheRunningTotal() {
        // 100 + 10% = 110, then 110 + 10% = 121
        assertEquals(121.0, of("100 + 10% + 10%", PM.MULTIPLICATIVE), 1e-9)
        // 100 + 0.1 = 100.1, then 100.1 + 0.1 = 100.2
        assertEquals(100.2, of("100 + 10% + 10%", PM.ADDITIVE), 1e-9)
    }

    @Test fun aDiscountThenAVat() {
        // a 500 item at 20% off, then 5% tax on the discounted price
        assertEquals(420.0, of("500 - 20% + 5%", PM.MULTIPLICATIVE), 1e-9)
    }

    @Test fun aPercentOfAZeroTotalStaysZero() {
        assertEquals(0.0, of("0 + 20%", PM.MULTIPLICATIVE), 1e-9)
        assertEquals(0.2, of("0 + 20%", PM.ADDITIVE), 1e-9)
    }

    @Test fun aNegativeRunningTotalKeepsItsSign() {
        assertEquals(-120.0, of("-100 + 20%", PM.MULTIPLICATIVE), 1e-9)
        assertEquals(-99.8, of("-100 + 20%", PM.ADDITIVE), 1e-9)
    }

    @Test fun aFractionalPercentWorks() {
        assertEquals(101.5, of("100 + 1.5%", PM.MULTIPLICATIVE), 1e-9)
        assertEquals(100.015, of("100 + 1.5%", PM.ADDITIVE), 1e-9)
    }

    // ---- the settings round trip ----

    @Test fun everyModeHasALabelAnExampleAndADescription() {
        val thin = PM.all.filter { it.label.isBlank() || it.example.isBlank() || it.blurb.isBlank() }
        assertTrue("modes with nothing to show: $thin", thin.isEmpty())
        assertEquals(2, PM.all.size)
    }

    @Test fun theExampleMatchesWhatTheModeActuallyDoes() {
        // the example printed in settings must be the truth, not a wish
        for (mode in PM.all) {
            val expected = mode.example.substringAfter("= ").toDouble()
            assertEquals("${mode.label} prints '${mode.example}'", expected, of("100 + 20%", mode), 1e-9)
        }
    }

    @Test fun theStoredNameIsReadTolerantly() {
        assertEquals(PM.ADDITIVE, PM.of("additive"))
        assertEquals(PM.ADDITIVE, PM.of("ADDITIVE"))
        assertEquals(PM.ADDITIVE, PM.of("plain"))
        assertEquals(PM.MULTIPLICATIVE, PM.of("multiplicative"))
        assertEquals(PM.MULTIPLICATIVE, PM.of("nonsense"))
        assertEquals(PM.MULTIPLICATIVE, PM.of(""))
    }

    @Test fun everyModeSurvivesTheRoundTripThroughItsName() {
        for (mode in PM.all) assertEquals(mode, PM.of(mode.name))
    }
}

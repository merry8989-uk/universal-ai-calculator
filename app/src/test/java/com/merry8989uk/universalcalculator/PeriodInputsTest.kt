package com.merry8989uk.universalcalculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase D: the formulas that were still pinned to a month or a year.
 *
 * Each of these computed a period from a constant — 12 for "monthly", nothing
 * for "annual" — so a weekly arrangement got a monthly answer and a daily one
 * was not expressible at all. They now take the period as an input, and the
 * numbers below check that the default still gives exactly what it gave before.
 */
class PeriodInputsTest {

    private fun money(v: Double) = Math.round(v * 100.0) / 100.0

    private fun run(id: String, values: Map<String, Double>): Map<String, Double> {
        val formula = com.merry8989uk.universalcalculator.calc.FormulaLibrary.formulas
            .firstOrNull { it.id == id }
        assertNotNull("there is no formula called $id", formula)
        return formula!!.compute(values).associate { it.label to it.value }
    }

    @Test fun presentValueCompoundsAsOftenAsAsked() {
        // annual is the old behaviour and must be unchanged
        val annual = run("pv_fv", mapOf("amount" to 100_000.0, "rate" to 8.0, "years" to 5.0, "per" to 1.0))
        assertEquals(146932.81, money(annual["Future value"]!!), 0.01)

        // monthly compounds more often and is worth more, which the annual-only
        // formula could not express at all
        val monthly = run("pv_fv", mapOf("amount" to 100_000.0, "rate" to 8.0, "years" to 5.0, "per" to 12.0))
        assertEquals(148984.57, money(monthly["Future value"]!!), 0.01)
        assertEquals(60.0, monthly["Periods"]!!, 1e-9)
    }

    @Test fun inflationTakesTheSamePeriod() {
        val annual = run("inflation", mapOf("amount" to 100_000.0, "rate" to 6.0, "years" to 10.0, "per" to 1.0))
        assertEquals(179084.77, money(annual["Future cost of the same basket"]!!), 0.01)
        val monthly = run("inflation", mapOf("amount" to 100_000.0, "rate" to 6.0, "years" to 10.0, "per" to 12.0))
        assertEquals(181939.67, money(monthly["Future cost of the same basket"]!!), 0.01)
    }

    @Test fun aProvidentFundIsNotAlwaysMonthly() {
        val monthly = run("pf_maturity", mapOf("monthly" to 5_000.0, "rate" to 8.25, "years" to 20.0, "per" to 12.0))
        assertEquals(3059189.41, money(monthly["Maturity"]!!), 0.01)

        // the same money every week for the same years is a different answer
        val weekly = run("pf_maturity", mapOf("monthly" to 5_000.0, "rate" to 8.25, "years" to 20.0, "per" to 52.0))
        assertTrue("weekly should not equal monthly", money(weekly["Maturity"]!!) != money(monthly["Maturity"]!!))
        assertEquals("the contributions should be 52 x 20", 5_200_000.0, weekly["Invested"]!!, 0.01)
    }

    @Test fun aHomeLoanPaymentIsNotAlwaysMonthly() {
        val monthly = run(
            "mortgage",
            mapOf("price" to 5_000_000.0, "down" to 20.0, "rate" to 8.5, "years" to 20.0, "per" to 12.0),
        )
        val weekly = run(
            "mortgage",
            mapOf("price" to 5_000_000.0, "down" to 20.0, "rate" to 8.5, "years" to 20.0, "per" to 52.0),
        )
        assertEquals(1040.0, monthly["Payments"]!!, 1e-9)
        assertEquals(1040.0, weekly["Payments"]!!, 1e-9)
        // a weekly payment is smaller than a monthly one, and there are more of them
        assertTrue(
            "the weekly payment should be smaller",
            weekly["Payment"]!! < monthly["Payment"]!!,
        )
    }

    @Test fun theEffectiveRateShowsTheRatePerPeriodThatWasSet() {
        // 12% nominal compounded quarterly is 3% a period, not 1% a month
        val quarterly = run("effective_apr", mapOf("nominal" to 12.0, "n" to 4.0))
        assertEquals(3.0, quarterly["Rate per compounding period"]!!, 1e-9)
        val monthly = run("effective_apr", mapOf("nominal" to 12.0, "n" to 12.0))
        assertEquals(1.0, monthly["Rate per compounding period"]!!, 1e-9)
    }

    @Test fun aZeroPeriodFallsBackRatherThanDividingByIt() {
        // a cleared input must not crash the card
        val r = run("pv_fv", mapOf("amount" to 100_000.0, "rate" to 8.0, "years" to 5.0, "per" to 0.0))
        assertEquals(146932.81, money(r["Future value"]!!), 0.01)
    }
}

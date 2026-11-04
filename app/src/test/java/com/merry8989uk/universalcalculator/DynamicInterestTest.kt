package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.DateRange
import com.merry8989uk.universalcalculator.calc.DayCountBasis
import com.merry8989uk.universalcalculator.calc.DurationSpan
import com.merry8989uk.universalcalculator.calc.DynamicInterest
import com.merry8989uk.universalcalculator.calc.RateSpec
import com.merry8989uk.universalcalculator.calc.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Interest in whatever unit the money was actually lent in.
 *
 * The scenarios are the ones in the plan, because they are the cases the fixed
 * formula gets wrong: a daily rate over days, a monthly rate over days, and a
 * fortnightly rate compounded fortnightly over months.
 */
class DynamicInterestTest {

    private fun money(v: Double) = Math.round(v * 100.0) / 100.0

    @Test fun aDailyRateOverDays() {
        // Scenario 1: 0.1% a day for 18 days on 50,000 is 900
        val r = DynamicInterest.compute(
            principal = 50_000.0,
            rate = RateSpec(0.1, TimeUnit.DAY),
            duration = DurationSpan(18.0, TimeUnit.DAY),
        )
        assertEquals(900.0, money(r.totalInterest), 0.01)
        assertEquals(50_900.0, money(r.totalAmount), 0.01)
    }

    @Test fun aMonthlyRateOverDays() {
        // Scenario 2: 2% a month for 75 days. The month is not 30 days flat here —
        // it is 30.4375, the average — so this is not the plan's rounded figure.
        val r = DynamicInterest.compute(
            principal = 200_000.0,
            rate = RateSpec(2.0, TimeUnit.MONTH),
            duration = DurationSpan(75.0, TimeUnit.DAY),
        )
        // 200000 * (0.02 / 30.4375) * 75
        assertEquals(9856.26, money(r.totalInterest), 0.01)
        // and the same sum with the commercial month is the plan's round number
        val commercial = DynamicInterest.compute(
            principal = 200_000.0,
            rate = RateSpec(2.0, TimeUnit.MONTH),
            duration = DurationSpan(75.0, TimeUnit.DAY),
            dayCount = DayCountBasis.THIRTY_360,
        )
        assertEquals(10_000.0, money(commercial.totalInterest), 0.01)
    }

    @Test fun aFortnightlyRateCompoundedFortnightly() {
        // Scenario 3: 1.5% per fortnight, compounded each fortnight, over 180 days
        val r = DynamicInterest.compute(
            principal = 10_000.0,
            rate = RateSpec(1.5, TimeUnit.FORTNIGHT),
            duration = DurationSpan(180.0, TimeUnit.DAY),
            dayCount = DayCountBasis.THIRTY_360,
            compoundEvery = TimeUnit.FORTNIGHT,
        )
        // 12.857 periods of 1.5%
        assertEquals(12.857, r.compoundingPeriods!!, 0.001)
        val expected = 10_000.0 * Math.pow(1.015, 180.0 / 14.0) - 10_000.0
        assertEquals(money(expected), money(r.compoundInterest!!), 0.01)
    }

    @Test fun aWeekIsAWeekAndNotAFractionOfAYear() {
        // 1% a week for 3 weeks is 3% — no annualising anywhere
        val r = DynamicInterest.compute(
            principal = 1_000.0,
            rate = RateSpec(1.0, TimeUnit.WEEK),
            duration = DurationSpan(3.0, TimeUnit.WEEK),
        )
        assertEquals(30.0, money(r.totalInterest), 0.001)
    }

    @Test fun aDayIsADay() {
        // one day at 1% on 1000 is 10, which the year-based formula cannot express
        val r = DynamicInterest.compute(
            principal = 1_000.0,
            rate = RateSpec(1.0, TimeUnit.DAY),
            duration = DurationSpan(1.0, TimeUnit.DAY),
        )
        assertEquals(10.0, money(r.totalInterest), 0.001)
        assertEquals(10.0, money(r.dailyInterestAmount), 0.001)
    }

    @Test fun rateUnitAndTimeUnitCanDisagree() {
        // 2% a month for 2 years. Two years is 730 days, a month is 30.4375,
        // so this is 24.01 monthly charges rather than a flat 24.
        val r = DynamicInterest.compute(
            principal = 10_000.0,
            rate = RateSpec(2.0, TimeUnit.MONTH),
            duration = DurationSpan(2.0, TimeUnit.YEAR),
        )
        assertEquals(730.0, r.totalDays, 1e-9)
        assertEquals(4796.71, money(r.totalInterest), 0.01)
    }

    @Test fun aCustomRatePeriodIsHonoured() {
        // "1.2% every 10 days" is a real arrangement, and it is not 1.2% a month
        val r = DynamicInterest.compute(
            principal = 10_000.0,
            rate = RateSpec(1.2, TimeUnit.DAY, customEvery = 10.0),
            duration = DurationSpan(30.0, TimeUnit.DAY),
        )
        assertEquals(10.0, r.daysPerRateCharge, 1e-9)
        assertEquals(360.0, money(r.totalInterest), 0.001)
    }

    @Test fun theAnnualFigureIsAComparisonNotAConstant() {
        // 1% a day is 365% a year, which is the number a person needs to see to
        // understand what they have agreed to
        val r = DynamicInterest.compute(
            principal = 1_000.0,
            rate = RateSpec(1.0, TimeUnit.DAY),
            duration = DurationSpan(1.0, TimeUnit.DAY),
        )
        assertEquals(365.0, r.effectiveAnnualPercent, 1e-9)
        assertEquals(30.4375, r.effectiveMonthlyPercent, 1e-6)
    }

    @Test fun aDateRangeCountsTheDaysItSays() {
        val from = LocalDate.of(2026, 1, 1)
        val to = LocalDate.of(2026, 1, 31)
        assertEquals(30.0, DateRange(from, to).days(DayCountBasis.ACTUAL_365), 1e-9)
        assertEquals(31.0, DateRange(from, to, includeBothDays = true).days(DayCountBasis.ACTUAL_365), 1e-9)
        // the commercial convention calls that a month
        assertEquals(30.0, DateRange(from, to).days(DayCountBasis.THIRTY_360), 1e-9)
    }

    @Test fun aBackwardsDateRangeIsZeroRatherThanNegative() {
        val r = DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1))
        assertEquals(0.0, r.days(DayCountBasis.ACTUAL_365), 1e-9)
    }

    @Test fun theSameArrangementInTwoUnitsGivesTheSameMoney() {
        // 12% a year for 1 year, and 1% a month for 12 months, are close but not
        // identical — the month is not exactly one twelfth of a year. The point is
        // that each is internally consistent, not that they agree.
        val yearly = DynamicInterest.compute(
            principal = 100_000.0,
            rate = RateSpec(12.0, TimeUnit.YEAR),
            duration = DurationSpan(1.0, TimeUnit.YEAR),
        )
        assertEquals(12_000.0, money(yearly.totalInterest), 0.01)

        val monthly = DynamicInterest.compute(
            principal = 100_000.0,
            rate = RateSpec(1.0, TimeUnit.MONTH),
            duration = DurationSpan(12.0, TimeUnit.MONTH),
        )
        assertEquals(12_000.0, money(monthly.totalInterest), 0.01)
    }

    @Test fun feesAreAddedAfterTheInterest() {
        val r = DynamicInterest.compute(
            principal = 1_000.0,
            rate = RateSpec(1.0, TimeUnit.MONTH),
            duration = DurationSpan(1.0, TimeUnit.MONTH),
            fees = 50.0,
        )
        assertEquals(10.0, money(r.totalInterest), 0.001)
        assertEquals(1_060.0, money(r.totalAmount), 0.001)
    }

    @Test fun aZeroRateIsZeroInterestRatherThanADivision() {
        val r = DynamicInterest.compute(
            principal = 5_000.0,
            rate = RateSpec(0.0, TimeUnit.MONTH),
            duration = DurationSpan(6.0, TimeUnit.MONTH),
        )
        assertEquals(0.0, r.totalInterest, 1e-9)
        assertEquals(5_000.0, r.totalAmount, 1e-9)
    }

    @Test fun noFormulaHasAHardcodedYearInIt() {
        // the whole point: the denominators come from the chosen units
        val source = listOf(
            java.io.File("app/src/main/java/com/merry8989uk/universalcalculator/calc/DynamicInterest.kt"),
            java.io.File("src/main/java/com/merry8989uk/universalcalculator/calc/DynamicInterest.kt"),
        ).firstOrNull { it.exists() } ?: return
        val body = source.readText().substringAfter("object DynamicInterest {")
        assertTrue("the interest maths still divides by 12", !body.contains("/ 12"))
        assertTrue("the interest maths still divides by 365", !body.contains("/ 365.0"))
        assertTrue("the interest maths still divides by 100 flat", !body.contains("/ 100.0) *"))
    }
}

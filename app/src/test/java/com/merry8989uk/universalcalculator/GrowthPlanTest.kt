package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Compounding
import com.merry8989uk.universalcalculator.calc.Growth
import com.merry8989uk.universalcalculator.calc.InterestTreatment
import com.merry8989uk.universalcalculator.calc.RateBasis
import com.merry8989uk.universalcalculator.calc.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The growth plan: the same money, described the way the person actually
 * described it.
 *
 * Every number here was checked against the reference implementation the design
 * was drawn from, so a disagreement is a real bug rather than a rounding taste.
 */
class GrowthPlanTest {

    private fun money(v: Double) = Math.round(v * 100.0) / 100.0

    @Test fun anEffectiveRateDeliversExactlyTheYearItPromised() {
        // 8% effective on 50,000 for a year is 4,000 of interest, whatever the
        // period — that is what "effective" means
        val plan = Growth.plan(
            principal = 50_000.0,
            annualRatePercent = 8.0,
            years = 1.0,
            compounding = Compounding.WEEKLY,
            basis = RateBasis.EFFECTIVE,
        )
        assertEquals(52, plan.periods)
        assertEquals(4000.00, money(plan.totalInterest), 0.01)
        assertEquals(54_000.00, money(plan.maturity), 0.01)
        assertEquals(8.0, plan.apy * 100.0, 1e-6)
    }

    @Test fun aNominalRateDeliversMoreThanItSays() {
        // the same 8% read as nominal weekly is 8.32% of the opening balance over
        // the year — the difference between the two readings, on one screen
        val plan = Growth.plan(
            principal = 50_000.0,
            annualRatePercent = 8.0,
            years = 1.0,
            compounding = Compounding.WEEKLY,
            basis = RateBasis.NOMINAL,
        )
        assertEquals(4161.02, money(plan.totalInterest), 0.01)
        assertEquals(8.322, plan.apy * 100.0, 0.001)
    }

    @Test fun interestTakenOutDoesNotEarnInterest() {
        // 12% nominal monthly for five years, interest paid out: the principal
        // never grows, so this is 5 years x 12% of 50,000
        val plan = Growth.plan(
            principal = 50_000.0,
            annualRatePercent = 12.0,
            years = 5.0,
            compounding = Compounding.MONTHLY,
            basis = RateBasis.NOMINAL,
            treatment = InterestTreatment.PAID_OUT,
        )
        assertEquals(60, plan.periods)
        assertEquals(30_000.00, money(plan.totalInterest), 0.01)
        assertEquals(80_000.00, money(plan.maturity), 0.01)
        // and every period earns the same, because the balance never moves
        assertEquals(500.0, money(plan.rows[0].interest), 0.01)
        assertEquals(500.0, money(plan.rows[59].interest), 0.01)
    }

    @Test fun reinvestingTheSameInterestEarnsMoreThanPayingItOut() {
        val paid = Growth.plan(
            50_000.0, 12.0, 5.0, Compounding.MONTHLY, RateBasis.NOMINAL, InterestTreatment.PAID_OUT,
        )
        val kept = Growth.plan(
            50_000.0, 12.0, 5.0, Compounding.MONTHLY, RateBasis.NOMINAL, InterestTreatment.REINVESTED,
        )
        assertTrue(
            "reinvesting should beat paying out: ${kept.totalInterest} vs ${paid.totalInterest}",
            kept.totalInterest > paid.totalInterest,
        )
    }

    @Test fun moneyAddedEachPeriodIsCountedAsInvested() {
        // 25,000 at 10% quarterly for six months, with 1,000 added each quarter
        val plan = Growth.plan(
            principal = 25_000.0,
            annualRatePercent = 10.0,
            years = 6.0 / 12.0,
            compounding = Compounding.QUARTERLY,
            basis = RateBasis.NOMINAL,
            extraDeposit = 1_000.0,
        )
        assertEquals(2, plan.periods)
        assertEquals(2000.0, money(plan.totalDeposits), 0.01)
        assertEquals(27_000.00, money(plan.invested), 0.01)
        assertEquals(1290.62, money(plan.totalInterest), 0.01)
        assertEquals(28_290.62, money(plan.maturity), 0.01)
    }

    @Test fun simpleIsChargedOnTheOriginalPrincipalNotTheBalance() {
        // 8% simple for five years on 50,000 is 20,000 — the balance growing does
        // not make the interest grow
        val plan = Growth.plan(
            principal = 50_000.0,
            annualRatePercent = 8.0,
            years = 5.0,
            compounding = Compounding.SIMPLE,
        )
        assertEquals(5, plan.periods)
        assertEquals(20_000.00, money(plan.totalInterest), 0.01)
        assertEquals(70_000.00, money(plan.maturity), 0.01)
        // every period is charged the same, which is the whole of simple interest
        assertEquals(4000.0, money(plan.rows[0].interest), 0.01)
        assertEquals(4000.0, money(plan.rows[4].interest), 0.01)
        // and the plain benchmark is the same sum, because it is the same idea
        assertEquals(20_000.00, money(plan.simpleBenchmark), 0.01)
    }

    @Test fun thePlanRollsUpIntoYears() {
        val plan = Growth.plan(
            principal = 50_000.0,
            annualRatePercent = 8.0,
            years = 2.0,
            compounding = Compounding.MONTHLY,
        )
        assertEquals(24, plan.periods)
        val years = plan.byYear()
        assertEquals(2, years.size)
        // the closing balance of a year is the closing balance of its last month
        assertEquals(plan.rows[11].closing, years[0].closing, 1e-9)
        assertEquals(plan.rows[23].closing, years[1].closing, 1e-9)
        // and the interest adds up to the total
        assertEquals(plan.totalInterest, years.sumOf { it.interest }, 1e-6)
    }

    @Test fun weeksMonthsAndYearsAreTheSameLength() {
        // 52 weeks, 12 months and 1 year are all a year of the same plan
        val weeks = Growth.plan(10_000.0, 6.0, Growth.years(52.0, TimeUnit.WEEK), Compounding.YEARLY)
        val months = Growth.plan(10_000.0, 6.0, Growth.years(12.0, TimeUnit.MONTH), Compounding.YEARLY)
        val year = Growth.plan(10_000.0, 6.0, 1.0, Compounding.YEARLY)
        assertEquals(year.maturity, weeks.maturity, 1e-6)
        assertEquals(year.maturity, months.maturity, 1e-6)
        assertEquals(1.0, Growth.years(52.0, TimeUnit.WEEK), 1e-9)
        assertEquals(1.0, Growth.years(12.0, TimeUnit.MONTH), 1e-9)
    }

    @Test fun aZeroRateStillProducesAPlan() {
        val plan = Growth.plan(10_000.0, 0.0, 1.0, Compounding.MONTHLY)
        assertEquals(12, plan.periods)
        assertEquals(10_000.0, plan.maturity, 1e-9)
        assertEquals(0.0, plan.totalInterest, 1e-9)
        // and the APY of making nothing is nothing, not a division by zero
        assertEquals(0.0, plan.apy, 1e-12)
    }

    @Test fun aShorterTenureThanOnePeriodStillMakesOne() {
        // a fortnight at a monthly compounding has no whole periods; it must not
        // produce an empty plan
        val plan = Growth.plan(1_000.0, 12.0, 14.0 / 365.0, Compounding.MONTHLY)
        assertEquals(1, plan.periods)
        assertTrue(plan.rows.isNotEmpty())
    }

    @Test fun theRatePerPeriodFollowsTheBasisNotAConstant() {
        // 12% a year is 1% a month nominal, and 0.9489% effective
        val nominal = Growth.plan(1000.0, 12.0, 1.0, Compounding.MONTHLY, RateBasis.NOMINAL)
        val effective = Growth.plan(1000.0, 12.0, 1.0, Compounding.MONTHLY, RateBasis.EFFECTIVE)
        assertEquals(0.01, nominal.ratePerPeriod, 1e-12)
        assertEquals(0.0094888, effective.ratePerPeriod, 1e-6)
        // and the effective one lands on exactly 12% over the year
        assertEquals(1120.0, money(effective.maturity), 0.01)
    }
}

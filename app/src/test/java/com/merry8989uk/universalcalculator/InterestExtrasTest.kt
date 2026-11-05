package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.DayCountBasis
import com.merry8989uk.universalcalculator.calc.DurationSpan
import com.merry8989uk.universalcalculator.calc.DynamicInterest
import com.merry8989uk.universalcalculator.calc.FeeMode
import com.merry8989uk.universalcalculator.calc.Penalty
import com.merry8989uk.universalcalculator.calc.RateSpec
import com.merry8989uk.universalcalculator.calc.Tier
import com.merry8989uk.universalcalculator.calc.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The five dynamic variables from the PDF's expansion matrix.
 *
 * Each is a thing the textbook formula cannot express: a rate that changes part
 * way through, a late charge that is not interest, a fee taken out of the money
 * before it is handed over, tax withheld, and a repayment that is not one lump
 * at the end.
 */
class InterestExtrasTest {

    private fun money(v: Double) = Math.round(v * 100.0) / 100.0
    private val commercial = DayCountBasis.THIRTY_360

    @Test fun aTieredRateChargesEachBandOnItsOwnDays() {
        // 2% a month for the first 10 days, 3% after, on 100,000 for 30 days.
        // The first ten days are not re-charged at the higher rate.
        val r = DynamicInterest.compute(
            principal = 100_000.0,
            rate = RateSpec(2.0, TimeUnit.MONTH),
            duration = DurationSpan(30.0, TimeUnit.DAY),
            dayCount = commercial,
            tiers = listOf(
                Tier(10.0, RateSpec(2.0, TimeUnit.MONTH)),
                Tier(30.0, RateSpec(3.0, TimeUnit.MONTH)),
            ),
        )
        assertEquals(2666.67, money(r.totalInterest), 0.01)
        assertEquals("the bands are not reported separately", 2, r.tierInterest.size)
    }

    @Test fun oneBandIsTheOrdinaryCase() {
        // a single band must give exactly what the plain rate gives, or the
        // feature has quietly changed the answer for everyone who does not use it
        val plain = DynamicInterest.compute(
            principal = 50_000.0,
            rate = RateSpec(2.0, TimeUnit.MONTH),
            duration = DurationSpan(45.0, TimeUnit.DAY),
            dayCount = commercial,
        )
        val oneBand = DynamicInterest.compute(
            principal = 50_000.0,
            rate = RateSpec(2.0, TimeUnit.MONTH),
            duration = DurationSpan(45.0, TimeUnit.DAY),
            dayCount = commercial,
            tiers = listOf(Tier(1000.0, RateSpec(2.0, TimeUnit.MONTH))),
        )
        assertEquals(plain.totalInterest, oneBand.totalInterest, 1e-9)
    }

    @Test fun aLateChargeIsNotInterest() {
        // 0.1% a day for 5 days overdue on 100,000 is 500, and it is shown apart
        val r = DynamicInterest.compute(
            principal = 100_000.0,
            rate = RateSpec(2.0, TimeUnit.MONTH),
            duration = DurationSpan(30.0, TimeUnit.DAY),
            dayCount = commercial,
            penalty = Penalty(RateSpec(0.1, TimeUnit.DAY), overdueDays = 5.0),
        )
        assertEquals(500.0, money(r.penaltyAmount), 0.01)
        assertEquals("the late charge was folded into the interest", 2000.0, money(r.totalInterest), 0.01)
        assertEquals(102_500.0, money(r.totalAmount), 0.01)
    }

    @Test fun taxIsWithheldFromTheInterest() {
        // 10% TDS on 2,000 of interest leaves 1,800
        val r = DynamicInterest.compute(
            principal = 100_000.0,
            rate = RateSpec(2.0, TimeUnit.MONTH),
            duration = DurationSpan(30.0, TimeUnit.DAY),
            dayCount = commercial,
            tdsPercent = 10.0,
        )
        assertEquals(2000.0, money(r.totalInterest), 0.01)
        assertEquals(200.0, money(r.tdsAmount), 0.01)
        assertEquals(1800.0, money(r.netInterest), 0.01)
    }

    @Test fun aFeeCanBeTakenOutOfTheMoneyBeforeItIsHandedOver() {
        // the borrower asks for 100,000, the lender keeps 2,000 of it
        val deducted = DynamicInterest.compute(
            principal = 100_000.0,
            rate = RateSpec(2.0, TimeUnit.MONTH),
            duration = DurationSpan(30.0, TimeUnit.DAY),
            dayCount = commercial,
            fees = 2_000.0,
            feeMode = FeeMode.DEDUCTED_FROM_PRINCIPAL,
        )
        assertEquals("the fee was added as well as deducted", 102_000.0, money(deducted.totalAmount), 0.01)
        assertEquals(98_000.0, money(deducted.amountReceived), 0.01)
        assertEquals(2_000.0, money(deducted.upfrontDeducted), 0.01)

        val added = DynamicInterest.compute(
            principal = 100_000.0,
            rate = RateSpec(2.0, TimeUnit.MONTH),
            duration = DurationSpan(30.0, TimeUnit.DAY),
            dayCount = commercial,
            fees = 2_000.0,
            feeMode = FeeMode.ADDED_TO_TOTAL,
        )
        assertEquals(104_000.0, money(added.totalAmount), 0.01)
        assertEquals(100_000.0, money(added.amountReceived), 0.01)
    }

    @Test fun aLoanCanBeRepaidInInstalmentsRatherThanOneLump() {
        // 100,000 at 12% a year over a year, monthly: the ordinary EMI
        val r = DynamicInterest.compute(
            principal = 100_000.0,
            rate = RateSpec(12.0, TimeUnit.YEAR),
            duration = DurationSpan(1.0, TimeUnit.YEAR),
            dayCount = commercial,
            instalmentEvery = TimeUnit.MONTH,
        )
        assertEquals(8884.88, money(r.instalmentAmount), 0.01)
        assertEquals("the schedule does not have twelve rows", 12, r.schedule.size)
        // the last row clears the balance rather than leaving a penny behind
        assertEquals(0.0, money(r.schedule.last().closing), 0.01)
        val paid = r.schedule.sumOf { it.instalment }
        assertEquals(106_618.55, money(paid), 0.02)
    }

    @Test fun theSameLoanWeeklyIsNotTheSameAsMonthly() {
        // weekly instalments are smaller and the year costs less, because the
        // balance falls sooner — which is the whole reason the period is a choice
        val weekly = DynamicInterest.compute(
            principal = 100_000.0,
            rate = RateSpec(12.0, TimeUnit.YEAR),
            duration = DurationSpan(1.0, TimeUnit.YEAR),
            dayCount = commercial,
            instalmentEvery = TimeUnit.WEEK,
        )
        assertEquals("a 360-day year is not 52 whole weeks", 51, weekly.schedule.size)
        assertTrue("the weekly instalment should be smaller than the monthly one",
            weekly.instalmentAmount < 8884.88)
    }

    @Test fun aBulletLoanHasNoSchedule() {
        val r = DynamicInterest.compute(
            principal = 100_000.0,
            rate = RateSpec(12.0, TimeUnit.YEAR),
            duration = DurationSpan(1.0, TimeUnit.YEAR),
            dayCount = commercial,
        )
        assertTrue("a bullet loan should not produce a repayment schedule", r.schedule.isEmpty())
        assertEquals(0.0, r.instalmentAmount, 1e-9)
    }

    @Test fun aZeroInterestLoanStillProducesASchedule() {
        // dividing by a zero rate is the classic crash; it is a plain division here
        val r = DynamicInterest.compute(
            principal = 12_000.0,
            rate = RateSpec(0.0, TimeUnit.YEAR),
            duration = DurationSpan(1.0, TimeUnit.YEAR),
            dayCount = commercial,
            instalmentEvery = TimeUnit.MONTH,
        )
        assertEquals(12, r.schedule.size)
        assertEquals(1000.0, money(r.instalmentAmount), 0.01)
        assertEquals(0.0, money(r.schedule.sumOf { it.interest }), 0.01)
    }
}

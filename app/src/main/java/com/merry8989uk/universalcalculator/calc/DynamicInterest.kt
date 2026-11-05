package com.merry8989uk.universalcalculator.calc

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow

/**
 * A length of time, and how many days it is worth.
 *
 * Every financial formula in a textbook quietly assumes two things: the rate is
 * per year and the time is in years. Neither is true of the money people
 * actually lend — 0.1% a day for 18 days, 2% a month for 75 days, 1.5% a
 * fortnight compounded fortnightly. This table is what lets the engine work in
 * any of them: everything is converted to days once, and the formula never
 * mentions 12 or 365 again.
 */
enum class TimeUnit(val label: String, val days: Double, val commercialDays: Double) {
    SECOND("Second", 1.0 / 86400.0, 1.0 / 86400.0),
    MINUTE("Minute", 1.0 / 1440.0, 1.0 / 1440.0),
    HOUR("Hour", 1.0 / 24.0, 1.0 / 24.0),
    DAY("Day", 1.0, 1.0),
    WEEK("Week", 7.0, 7.0),
    FORTNIGHT("Fortnight (14 days)", 14.0, 14.0),
    MONTH("Month", 30.4375, 30.0),
    QUARTER("Quarter (3 months)", 91.3125, 90.0),
    HALF_YEAR("Half year (6 months)", 182.625, 180.0),
    YEAR("Year", 365.0, 360.0),
}

/**
 * How to count the days between two dates.
 *
 * Banks disagree about this, and the disagreement is worth real money on a large
 * principal, so it is a choice rather than a constant.
 */
enum class DayCountBasis(val label: String, val note: String) {
    ACTUAL_365("Actual/365", "The usual bank default. A year is 365 days."),
    ACTUAL_360("Actual/360", "Commercial paper. The same days, divided by 360 — about 1.4% more interest."),
    THIRTY_360("30/360", "Every month counts as 30 days and every year as 360, whatever the calendar says."),
    ACTUAL_ACTUAL("Actual/Actual", "Counts the leap day when the period crosses 29 February."),
}

/** Where an upfront charge goes. */
enum class FeeMode(val label: String, val note: String) {
    ADDED_TO_TOTAL("Added to the total", "The borrower pays the fee on top of the interest."),
    DEDUCTED_FROM_PRINCIPAL(
        "Deducted from the principal",
        "The lender takes the fee out of the money handed over, so the borrower receives less " +
            "but owes the full amount. This raises the real rate and is the one worth checking.",
    ),
}

/** A rate such as "2.5% per month", or "1.2% every 10 days". */
data class RateSpec(val value: Double, val per: TimeUnit, val customEvery: Double = 1.0) {
    /** How many days one charge covers. */
    fun daysPerCharge(dayCount: DayCountBasis): Double {
        val unitDays = if (dayCount == DayCountBasis.THIRTY_360) per.commercialDays else per.days
        return unitDays * customEvery
    }

    /** The rate as a fraction per day. */
    fun dailyFraction(dayCount: DayCountBasis): Double {
        val days = daysPerCharge(dayCount)
        return if (days > 0.0) (value / 100.0) / days else 0.0
    }
}

/**
 * One band of a tiered rate: the first [upToDays] days are charged at [rate].
 *
 * "2% a month for the first 10 days, 3% after" is a real arrangement and it is
 * not expressible as a single rate. Each band is charged on its own slice of
 * days and the slices add up.
 */
data class Tier(val upToDays: Double, val rate: RateSpec)

/** A late-payment charge: an extra rate on the amount outstanding for [overdueDays]. */
data class Penalty(val rate: RateSpec, val overdueDays: Double)

/** A duration such as "45 days", "3 months", or two calendar dates. */
sealed interface Duration {
    /** Total days, under [dayCount]. */
    fun days(dayCount: DayCountBasis): Double
}

/** A number of units — 45 days, 3 months, or "every 2 weeks" repeated N times. */
data class DurationSpan(
    val value: Double,
    val unit: TimeUnit,
    val customEvery: Double = 1.0,
) : Duration {
    override fun days(dayCount: DayCountBasis): Double {
        val unitDays = if (dayCount == DayCountBasis.THIRTY_360) unit.commercialDays else unit.days
        return value * unitDays * customEvery
    }
}

data class DateRange(
    val from: LocalDate,
    val to: LocalDate,
    val includeBothDays: Boolean = false,
) : Duration {
    override fun days(dayCount: DayCountBasis): Double {
        if (to.isBefore(from)) return 0.0
        val raw = ChronoUnit.DAYS.between(from, to).toDouble()
        val counted = if (includeBothDays) raw + 1.0 else raw
        return when (dayCount) {
            DayCountBasis.ACTUAL_365,
            DayCountBasis.ACTUAL_360,
            DayCountBasis.ACTUAL_ACTUAL,
            -> counted
            DayCountBasis.THIRTY_360 -> {
                // the commercial convention: every month is 30 days
                val months = (to.year - from.year) * 12 + (to.monthValue - from.monthValue)
                val dayBits = to.dayOfMonth - from.dayOfMonth
                (months * 30 + dayBits + if (includeBothDays) 1 else 0).toDouble()
            }
        }
    }
}

/** One line of a repayment schedule. */
data class ScheduleRow(
    val period: Int,
    val opening: Double,
    val interest: Double,
    val principal: Double,
    val instalment: Double,
    val closing: Double,
)

/** What came out, with every intermediate step, so the working can be checked. */
data class InterestBreakdown(
    val principal: Double,
    val fees: Double,
    val dailyRatePercent: Double,
    val daysPerRateCharge: Double,
    val totalDays: Double,
    val simpleInterest: Double,
    val compoundInterest: Double?,
    val compoundingPeriods: Double?,
    val totalInterest: Double,
    val totalAmount: Double,
    val dailyInterestAmount: Double,
    val effectiveAnnualPercent: Double,
    val effectiveMonthlyPercent: Double,
    // ---- the variables the PDF's expansion matrix adds ----
    /** Interest split by band, when a tiered rate was used. Empty otherwise. */
    val tierInterest: List<Pair<String, Double>> = emptyList(),
    /** The late charge, shown apart from interest because it is not interest. */
    val penaltyAmount: Double = 0.0,
    /** Withheld from the interest. */
    val tdsAmount: Double = 0.0,
    /** What the lender actually keeps after TDS. */
    val netInterest: Double = 0.0,
    /** Taken out of the money handed over, under the deducted-from-principal mode. */
    val upfrontDeducted: Double = 0.0,
    /** What the borrower actually receives. */
    val amountReceived: Double = 0.0,
    /** Equal instalments, when a repayment frequency was chosen. Empty for a bullet loan. */
    val schedule: List<ScheduleRow> = emptyList(),
    /** The size of one instalment. */
    val instalmentAmount: Double = 0.0,
)

/**
 * Interest that does not care what unit you think in.
 *
 * There is no 12 and no 365 anywhere below. Both come from the units the user
 * chose: a rate per month is divided by the days in a month, a duration in weeks
 * is multiplied by seven, and the two meet on the same footing — one day. That
 * is the whole of the design; every other formula here is the ordinary one.
 */
object DynamicInterest {

    fun compute(
        principal: Double,
        rate: RateSpec,
        duration: Duration,
        fees: Double = 0.0,
        dayCount: DayCountBasis = DayCountBasis.ACTUAL_365,
        compoundEvery: TimeUnit? = null,
        tiers: List<Tier> = emptyList(),
        feeMode: FeeMode = FeeMode.ADDED_TO_TOTAL,
        penalty: Penalty? = null,
        tdsPercent: Double = 0.0,
        instalmentEvery: TimeUnit? = null,
    ): InterestBreakdown {
        val totalDays = duration.days(dayCount)
        val yearDays = if (dayCount == DayCountBasis.THIRTY_360) 360.0 else 365.0

        // ---- interest, in bands or as one rate ----
        val bands: List<Pair<String, Double>>
        val simple: Double
        if (tiers.isEmpty()) {
            val daily = rate.dailyFraction(dayCount)
            simple = principal * daily * totalDays
            bands = emptyList()
        } else {
            // each band is charged on its own slice of days, and the slices add up
            var charged = 0.0
            val acc = ArrayList<Pair<String, Double>>()
            for (tier in tiers.sortedBy { it.upToDays }) {
                if (charged >= totalDays) break
                val sliceEnd = minOf(tier.upToDays, totalDays)
                val slice = (sliceEnd - charged).coerceAtLeast(0.0)
                if (slice <= 0.0) continue
                val amount = principal * tier.rate.dailyFraction(dayCount) * slice
                acc.add(
                    "${fmt(slice)} days at ${fmt(tier.rate.value)}% per " +
                        tier.rate.per.label.lowercase() to amount,
                )
                charged = sliceEnd
            }
            // anything past the last band is charged at the last band's rate
            if (charged < totalDays && tiers.isNotEmpty()) {
                val last = tiers.sortedBy { it.upToDays }.last()
                val slice = totalDays - charged
                val amount = principal * last.rate.dailyFraction(dayCount) * slice
                acc.add("${fmt(slice)} days at ${fmt(last.rate.value)}% per ${last.rate.per.label.lowercase()} (onward)" to amount)
            }
            bands = acc
            simple = acc.sumOf { it.second }
        }

        val dailyRatePercent = rate.dailyFraction(dayCount) * 100.0

        // ---- compounding, if asked for ----
        var compound: Double? = null
        var periods: Double? = null
        if (compoundEvery != null) {
            val periodDays = if (dayCount == DayCountBasis.THIRTY_360) {
                compoundEvery.commercialDays
            } else {
                compoundEvery.days
            }
            if (periodDays > 0.0) {
                val n = totalDays / periodDays
                periods = n
                val ratePerPeriod = rate.dailyFraction(dayCount) * periodDays
                compound = principal * (1.0 + ratePerPeriod).pow(n) - principal
            }
        }

        val interest = compound ?: simple

        // ---- the late charge, kept apart from the interest ----
        val penaltyAmount = if (penalty != null && penalty.overdueDays > 0.0) {
            principal * penalty.rate.dailyFraction(dayCount) * penalty.overdueDays
        } else {
            0.0
        }

        // ---- tax withheld from the interest ----
        val tds = if (tdsPercent > 0.0) interest * (tdsPercent / 100.0) else 0.0
        val netInterest = interest - tds

        // ---- where the fee goes ----
        val upfront = if (feeMode == FeeMode.DEDUCTED_FROM_PRINCIPAL) fees else 0.0
        val addedFee = if (feeMode == FeeMode.ADDED_TO_TOTAL) fees else 0.0
        val received = principal - upfront
        val totalAmount = principal + interest + penaltyAmount + addedFee

        // ---- the schedule, if the money is repaid in instalments ----
        val schedule = if (instalmentEvery != null) {
            buildSchedule(principal, rate, totalDays, dayCount, instalmentEvery)
        } else {
            emptyList()
        }

        return InterestBreakdown(
            principal = principal,
            fees = fees,
            dailyRatePercent = dailyRatePercent,
            daysPerRateCharge = rate.daysPerCharge(dayCount),
            totalDays = totalDays,
            simpleInterest = simple,
            compoundInterest = compound,
            compoundingPeriods = periods,
            totalInterest = interest,
            totalAmount = totalAmount,
            dailyInterestAmount = principal * rate.dailyFraction(dayCount),
            effectiveAnnualPercent = rate.dailyFraction(dayCount) * yearDays * 100.0,
            effectiveMonthlyPercent = rate.dailyFraction(dayCount) *
                (if (dayCount == DayCountBasis.THIRTY_360) 30.0 else 30.4375) * 100.0,
            tierInterest = bands,
            penaltyAmount = penaltyAmount,
            tdsAmount = tds,
            netInterest = netInterest,
            upfrontDeducted = upfront,
            amountReceived = received,
            schedule = schedule,
            instalmentAmount = schedule.firstOrNull()?.instalment ?: 0.0,
        )
    }

    /**
     * Equal instalments at [every], with the interest for each period worked out
     * on what is still owed.
     *
     * The instalment size comes from the ordinary annuity formula, and the rate
     * per period comes from the same daily figure as everything else — so a
     * weekly schedule on a monthly rate works without a special case.
     */
    private fun buildSchedule(
        principal: Double,
        rate: RateSpec,
        totalDays: Double,
        dayCount: DayCountBasis,
        every: TimeUnit,
    ): List<ScheduleRow> {
        val periodDays = if (dayCount == DayCountBasis.THIRTY_360) every.commercialDays else every.days
        if (periodDays <= 0.0 || totalDays <= 0.0) return emptyList()
        val n = (totalDays / periodDays).coerceAtLeast(1.0)
        val i = rate.dailyFraction(dayCount) * periodDays
        val whole = Math.floor(n).toInt().coerceAtLeast(1)
        val instalment = if (i == 0.0) {
            principal / n
        } else {
            principal * i * (1.0 + i).pow(n) / ((1.0 + i).pow(n) - 1.0)
        }

        val rows = ArrayList<ScheduleRow>(whole)
        var balance = principal
        for (p in 1..whole) {
            val interest = balance * i
            var principalPart = instalment - interest
            // the last row clears whatever is left, so rounding does not leave a penny
            if (p == whole || principalPart > balance) principalPart = balance
            val closing = (balance - principalPart).coerceAtLeast(0.0)
            rows.add(
                ScheduleRow(
                    period = p,
                    opening = balance,
                    interest = interest,
                    principal = principalPart,
                    instalment = principalPart + interest,
                    closing = closing,
                ),
            )
            balance = closing
            if (balance <= 0.0) break
        }
        return rows
    }

    private fun fmt(v: Double): String =
        if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString()
        else String.format(java.util.Locale.US, "%.2f", v)
}

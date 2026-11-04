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
 * fortnight compounded fortnightly for six months. This table is what lets the
 * engine work in any of them: everything is converted to days once, and the
 * formula never mentions 12 or 365 again.
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

/** A rate such as "2.5% per month", or "1.2% every 10 days". */
data class RateSpec(val value: Double, val per: TimeUnit, val customEvery: Double = 1.0) {
    /** How many days one charge covers. */
    fun daysPerCharge(dayCount: DayCountBasis): Double {
        val unitDays = if (dayCount == DayCountBasis.THIRTY_360) per.commercialDays else per.days
        return unitDays * customEvery
    }
}

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
            DayCountBasis.ACTUAL_365, DayCountBasis.ACTUAL_360, DayCountBasis.ACTUAL_ACTUAL -> counted
            DayCountBasis.THIRTY_360 -> {
                // the commercial convention: every month is 30 days
                val months = (to.year - from.year) * 12 + (to.monthValue - from.monthValue)
                val dayBits = to.dayOfMonth - from.dayOfMonth
                (months * 30 + dayBits + if (includeBothDays) 1 else 0).toDouble()
            }
        }
    }
}

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
    ): InterestBreakdown {
        val daysPerCharge = rate.daysPerCharge(dayCount)
        val totalDays = duration.days(dayCount)

        // A rate per month of 2% is 2/30.4375 percent per day. Nothing is annualised.
        val dailyRatePercent = if (daysPerCharge > 0.0) rate.value / daysPerCharge else 0.0
        val dailyRate = dailyRatePercent / 100.0

        val simple = principal * dailyRate * totalDays

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
                val ratePerPeriod = dailyRate * periodDays
                compound = principal * (1.0 + ratePerPeriod).pow(n) - principal
            }
        }

        val interest = compound ?: simple
        // The annual figure is a comparison, not a constant: it is whatever this
        // rate works out to over a year of the same convention.
        val yearDays = if (dayCount == DayCountBasis.THIRTY_360) 360.0 else 365.0
        val annualPercent = dailyRate * yearDays * 100.0
        val monthlyPercent = dailyRate * (if (dayCount == DayCountBasis.THIRTY_360) 30.0 else 30.4375) * 100.0

        return InterestBreakdown(
            principal = principal,
            fees = fees,
            dailyRatePercent = dailyRatePercent,
            daysPerRateCharge = daysPerCharge,
            totalDays = totalDays,
            simpleInterest = simple,
            compoundInterest = compound,
            compoundingPeriods = periods,
            totalInterest = interest,
            totalAmount = principal + interest + fees,
            dailyInterestAmount = principal * dailyRate,
            effectiveAnnualPercent = annualPercent,
            effectiveMonthlyPercent = monthlyPercent,
        )
    }
}

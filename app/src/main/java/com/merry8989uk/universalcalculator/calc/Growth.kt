package com.merry8989uk.universalcalculator.calc

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * How often interest is worked out, as a preset a person recognises.
 *
 * The order matters: it is the order they appear in the picker, from the one
 * that never compounds to the one that compounds once a year.
 */
enum class Compounding(val label: String, val periodsPerYear: Int) {
    SIMPLE("Simple", 1),
    DAILY("Daily", 365),
    WEEKLY("Weekly", 52),
    MONTHLY("Monthly", 12),
    QUARTERLY("Quarterly", 4),
    HALF_YEARLY("Half-yearly", 2),
    YEARLY("Yearly", 1),
}

/**
 * What "12% a year" actually means.
 *
 * This is the distinction most calculators hide, and it is worth real money.
 * A nominal 12% compounded monthly is 12.68% of the opening balance over the
 * year. An effective 12% is 12% — the monthly rate is derived so that the year
 * comes out at exactly 12%. Same words, different money.
 */
enum class RateBasis(val label: String, val note: String) {
    NOMINAL(
        "Nominal — split per period",
        "12% a year compounded monthly is 1% a month, which is 12.68% over the year.",
    ),
    EFFECTIVE(
        "Effective — the yearly total",
        "12% a year means 12% over the year, so each month is a little under 1%.",
    ),
}

/** What happens to the interest each period. */
enum class InterestTreatment(val label: String, val note: String) {
    REINVESTED("Reinvested (compounds)", "The interest stays in and earns interest next period."),
    PAID_OUT("Paid out to me", "The interest is taken out, so only the principal keeps earning."),
}

/** One period of a growth plan. */
data class GrowthRow(
    val period: Int,
    val year: Int,
    val opening: Double,
    val interest: Double,
    val deposit: Double,
    val closing: Double,
    val cumulativeInterest: Double,
)

/** A growth plan, or the same plan rolled up by year. */
data class GrowthPlan(
    val rows: List<GrowthRow>,
    val periods: Int,
    val periodsPerYear: Int,
    val years: Double,
    val ratePerPeriod: Double,
    val principal: Double,
    val totalDeposits: Double,
    val invested: Double,
    val maturity: Double,
    val totalInterest: Double,
    val firstPeriodInterest: Double,
    /** The compound annual growth actually achieved, as a fraction. */
    val apy: Double,
    /** What plain simple interest on the original principal would have paid. */
    val simpleBenchmark: Double,
) {
    /** The rows rolled up by year, for a plan with many periods. */
    fun byYear(): List<GrowthRow> {
        val out = ArrayList<GrowthRow>()
        var current: GrowthRow? = null
        for (r in rows) {
            val c = current
            if (c == null || c.year != r.year) {
                current = r.copy(period = r.year, interest = r.interest, deposit = r.deposit)
                out.add(current)
            } else {
                val merged = c.copy(
                    interest = c.interest + r.interest,
                    deposit = c.deposit + r.deposit,
                    closing = r.closing,
                    cumulativeInterest = r.cumulativeInterest,
                )
                out[out.size - 1] = merged
                current = merged
            }
        }
        return out
    }
}

/**
 * What a lump sum does over time, in the terms a person chose.
 *
 * This is the growth side of the same engine: the rate is annual, the period is
 * a preset, and the two are reconciled the way the basis says they should be.
 * Nothing here divides by 12 or 365 — the period count is the preset's own, so
 * a weekly plan is 52 periods a year because it says so, not because a constant
 * was typed somewhere.
 */
object Growth {

    fun plan(
        principal: Double,
        annualRatePercent: Double,
        years: Double,
        compounding: Compounding = Compounding.MONTHLY,
        basis: RateBasis = RateBasis.NOMINAL,
        treatment: InterestTreatment = InterestTreatment.REINVESTED,
        extraDeposit: Double = 0.0,
    ): GrowthPlan {
        val ppy = compounding.periodsPerYear.coerceAtLeast(1)
        val safeYears = years.coerceAtLeast(0.0)
        val n = (safeYears * ppy).roundToInt().coerceAtLeast(1)
        val r = annualRatePercent / 100.0

        // the two readings of the same sentence
        val ratePerPeriod = when {
            compounding == Compounding.SIMPLE -> r
            basis == RateBasis.EFFECTIVE -> (1.0 + r).pow(1.0 / ppy) - 1.0
            else -> r / ppy
        }

        val rows = ArrayList<GrowthRow>(n)
        var balance = principal
        var cumulative = 0.0
        var deposits = 0.0
        val isSimple = compounding == Compounding.SIMPLE

        for (p in 1..n) {
            val opening = balance
            // simple interest is always on the original principal; compound is on
            // what is actually there
            val interest = if (isSimple) principal * ratePerPeriod else opening * ratePerPeriod
            cumulative += interest
            deposits += extraDeposit
            val closing = if (treatment == InterestTreatment.PAID_OUT) {
                // the interest leaves, so only the principal and new money remain
                opening + extraDeposit
            } else {
                opening + interest + extraDeposit
            }
            rows.add(
                GrowthRow(
                    period = p,
                    year = (p - 1) / ppy + 1,
                    opening = opening,
                    interest = interest,
                    deposit = extraDeposit,
                    closing = closing,
                    cumulativeInterest = cumulative,
                ),
            )
            balance = closing
        }

        val invested = principal + deposits
        val maturity = if (treatment == InterestTreatment.PAID_OUT) {
            principal + deposits + cumulative
        } else {
            balance
        }
        val apy = if (invested > 0.0 && safeYears > 0.0) {
            (maturity / invested).pow(1.0 / safeYears) - 1.0
        } else {
            0.0
        }

        return GrowthPlan(
            rows = rows,
            periods = n,
            periodsPerYear = ppy,
            years = n.toDouble() / ppy,
            ratePerPeriod = ratePerPeriod,
            principal = principal,
            totalDeposits = deposits,
            invested = invested,
            maturity = maturity,
            totalInterest = cumulative,
            firstPeriodInterest = rows.firstOrNull()?.interest ?: 0.0,
            apy = apy,
            simpleBenchmark = principal * r * safeYears,
        )
    }

    /** Weeks, months or years turned into years. */
    fun years(value: Double, unit: TimeUnit): Double = when (unit) {
        TimeUnit.WEEK -> value / 52.0
        TimeUnit.FORTNIGHT -> value / 26.0
        TimeUnit.MONTH -> value / 12.0
        TimeUnit.QUARTER -> value / 4.0
        TimeUnit.HALF_YEAR -> value / 2.0
        TimeUnit.DAY -> value / 365.0
        TimeUnit.HOUR -> value / 8760.0
        TimeUnit.MINUTE -> value / 525600.0
        TimeUnit.SECOND -> value / 31536000.0
        TimeUnit.YEAR -> value
    }
}

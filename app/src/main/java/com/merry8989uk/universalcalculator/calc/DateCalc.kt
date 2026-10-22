package com.merry8989uk.universalcalculator.calc

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** Date arithmetic used by the Date tools screen. */
object DateCalc {

    fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

    fun weeksBetween(from: LocalDate, to: LocalDate): Long = daysBetween(from, to) / 7

    fun monthsBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.MONTHS.between(from, to)

    fun yearsBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.YEARS.between(from, to)

    /** Full age / span as years, months and days. */
    fun period(from: LocalDate, to: LocalDate): Triple<Int, Int, Int> {
        val p = Period.between(from, to)
        return Triple(p.years, p.months, p.days)
    }

    /** Working days between two dates, excluding Saturdays and Sundays. */
    fun workingDays(from: LocalDate, to: LocalDate): Long {
        var start = from
        var end = to
        val forward = !end.isBefore(start)
        if (!forward) { val t = start; start = end; end = t }
        var count = 0L
        var d = start
        while (d.isBefore(end)) {
            val dow = d.dayOfWeek.value // 1 = Monday ... 7 = Sunday
            if (dow <= 5) count++
            d = d.plusDays(1)
        }
        return if (forward) count else -count
    }

    fun addDays(date: LocalDate, days: Long): LocalDate = date.plusDays(days)

    fun isLeapYear(year: Int): Boolean = java.time.Year.isLeap(year.toLong())
}

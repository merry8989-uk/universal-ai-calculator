package com.merry8989uk.universalcalculator.calc

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.time.Period

/** Date and time arithmetic used by the Date tools screen. */
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
            if (d.dayOfWeek.value <= 5) count++
            d = d.plusDays(1)
        }
        return if (forward) count else -count
    }

    fun addDays(date: LocalDate, days: Long): LocalDate = date.plusDays(days)
    fun addMonths(date: LocalDate, months: Long): LocalDate = date.plusMonths(months)
    fun addYears(date: LocalDate, years: Long): LocalDate = date.plusYears(years)

    fun isLeapYear(year: Int): Boolean = java.time.Year.isLeap(year.toLong())

    fun daysInMonth(date: LocalDate): Int = date.lengthOfMonth()

    fun dayOfYear(date: LocalDate): Int = date.dayOfYear

    fun isoWeek(date: LocalDate): Int =
        date.get(WeekFields.ISO.weekOfWeekBasedYear())

    fun quarter(date: LocalDate): Int = (date.monthValue - 1) / 3 + 1

    fun dayName(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)

    /** Whole days from today to [date] (negative when in the past). */
    fun daysFromToday(date: LocalDate): Long = ChronoUnit.DAYS.between(LocalDate.now(), date)

    fun toEpochSeconds(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()

    fun fromEpochSeconds(seconds: Long): String =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(seconds), ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    /** Time arithmetic: returns "HH:mm" of start + minutes. */
    fun addMinutes(startHHmm: String, minutes: Long): String {
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        val t = java.time.LocalTime.parse(startHHmm, fmt)
        return t.plusMinutes(minutes).format(fmt)
    }
}

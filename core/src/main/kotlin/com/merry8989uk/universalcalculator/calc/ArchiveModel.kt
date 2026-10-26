package com.merry8989uk.universalcalculator.calc

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One calculation, kept forever.
 *
 * This is not the history. The history is a working list the user can wipe at
 * any moment; the archive is the record, and clearing the history must never
 * touch it. That is why it lives in its own folder and its own type.
 */
data class ArchiveEntry(
    /** When it was worked out, to the second. */
    val at: LocalDateTime,
    /** Which calculator did it, so a scientific answer can be told from a shop one. */
    val kind: String,
    /** What was typed. */
    val expression: String,
    /** What it came to, as it appeared on screen. */
    val result: String,
    /** The number behind the result, when there was one. Null for a list or an error. */
    val value: Double?,
)

/**
 * Writing and reading the archive.
 *
 * The format is one line per calculation, with fields separated by a bar. A line
 * is short, it can be read by eye in a file manager, and — the reason for
 * choosing it — a half-written line at the end of a crash is simply skipped
 * rather than corrupting the file.
 */
object ArchiveFormat {

    const val FIELD = '|'
    const val HEADER = "NEXUSARCHIVE1"

    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.UK)
    private val MONTH_LONG: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.UK)
    private val DAY_LONG: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.UK)

    fun time(at: LocalDateTime): String = at.format(TIME)
    fun monthName(date: LocalDate): String = date.format(MONTH_LONG)
    fun dayName(date: LocalDate): String = date.format(DAY_LONG)

    /** `01 October 2026`, which is also the file name. */
    fun dayFile(date: LocalDate): String = dayName(date) + ".txt"

    /** `Monthly summary - October 2026`. */
    fun monthFile(date: LocalDate): String = "Monthly summary - " + monthName(date) + " " + date.year + ".txt"

    /**
     * The Indian financial year a date belongs to: 1 April to 31 March.
     *
     * 31 March 2027 is the last day of FY 2026-27, and 1 April 2027 starts the
     * next one. Getting this a day wrong moves a whole year's takings.
     */
    fun financialYear(date: LocalDate): Pair<Int, Int> {
        val start = if (date.monthValue >= 4) date.year else date.year - 1
        return start to start + 1
    }

    /** `FY 2026-27`. */
    fun financialYearName(date: LocalDate): String {
        val (a, b) = financialYear(date)
        return "FY $a-" + (b % 100).toString().padStart(2, '0')
    }

    fun financialYearFile(date: LocalDate): String = financialYearName(date) + ".txt"

    /** One line of the day file. */
    fun line(entry: ArchiveEntry): String = buildString {
        append(entry.at.format(TIME))
        append(FIELD).append(entry.kind.replace(FIELD, ' '))
        append(FIELD).append(entry.expression.replace(FIELD, ' '))
        append(FIELD).append(entry.result.replace(FIELD, ' '))
        append(FIELD).append(entry.value?.let { Format.number(it) } ?: "")
    }

    /**
     * Reads a day file back.
     *
     * [date] supplies the day, because the line only carries the time. A line
     * that does not have five fields is skipped — that is the half-written one
     * at the end of a crash, and the rest of the file is still good.
     */
    fun parse(date: LocalDate, text: String): List<ArchiveEntry> {
        val out = ArrayList<ArchiveEntry>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line == HEADER) continue
            val parts = line.split(FIELD)
            if (parts.size < 4) continue
            val time = runCatching { java.time.LocalTime.parse(parts[0], TIME) }.getOrNull() ?: continue
            out.add(
                ArchiveEntry(
                    at = LocalDateTime.of(date, time),
                    kind = parts[1].trim(),
                    expression = parts[2].trim(),
                    result = parts[3].trim(),
                    value = parts.getOrNull(4)?.trim()?.toDoubleOrNull(),
                ),
            )
        }
        return out
    }
}

/** How much happened on one day. */
data class DayCount(val date: LocalDate, val calculations: Int)

/**
 * The summaries.
 *
 * The user was right to draw a line between the number of calculations and the
 * sum of their results: adding a length to a currency is meaningless. So the
 * count is always shown, and the sum is shown separately and labelled as the sum
 * of the numbers only.
 */
object ArchiveSummary {

    fun daysOf(entries: List<ArchiveEntry>): List<DayCount> =
        entries.groupBy { it.at.toLocalDate() }
            .map { (date, list) -> DayCount(date, list.size) }
            .sortedBy { it.date }

    fun count(entries: List<ArchiveEntry>): Int = entries.size

    /** The sum of the numbers, which is not the same thing as the count. */
    fun sum(entries: List<ArchiveEntry>): Double = entries.mapNotNull { it.value }.sum()

    fun howManyHaveANumber(entries: List<ArchiveEntry>): Int = entries.count { it.value != null }

    /** The month summary, as it is written to its own file. */
    fun monthText(month: LocalDate, entries: List<ArchiveEntry>): String = buildString {
        val name = ArchiveFormat.monthName(month) + " " + month.year
        appendLine("Monthly summary - $name")
        appendLine()
        val days = daysOf(entries)
        appendLine("Days with calculations: ${days.size}")
        appendLine("Calculations: ${count(entries)}")
        if (entries.isNotEmpty()) {
            appendLine("First: " + entries.minOf { it.at })
            appendLine("Last: " + entries.maxOf { it.at })
        }
        appendLine()
        appendLine("Per day:")
        for (d in days) appendLine("  " + ArchiveFormat.dayName(d.date) + "   " + d.calculations)
        appendLine()
        val withNumbers = howManyHaveANumber(entries)
        if (withNumbers > 0) {
            appendLine("Sum of the $withNumbers results that are plain numbers: " + Format.number(sum(entries)))
            if (withNumbers < entries.size) {
                appendLine(
                    "(" + (entries.size - withNumbers) +
                        " results were not plain numbers and are counted but not added.)",
                )
            }
        }
    }

    /** The financial-year summary, month by month. */
    fun financialYearText(startYear: Int, entries: List<ArchiveEntry>): String = buildString {
        val endYear = startYear + 1
        appendLine("Financial year $startYear-" + (endYear % 100).toString().padStart(2, '0'))
        appendLine("01 April $startYear to 31 March $endYear")
        appendLine()
        appendLine("Calculations: ${count(entries)}")
        appendLine()
        appendLine("Per month:")
        val byMonth = entries.groupBy { it.at.toLocalDate().withDayOfMonth(1) }.toSortedMap()
        for ((month, list) in byMonth) {
            appendLine("  " + ArchiveFormat.monthName(month) + " " + month.year + "   " + list.size)
        }
        appendLine()
        val withNumbers = howManyHaveANumber(entries)
        if (withNumbers > 0) {
            appendLine("Sum of the $withNumbers results that are plain numbers: " + Format.number(sum(entries)))
        }
    }
}

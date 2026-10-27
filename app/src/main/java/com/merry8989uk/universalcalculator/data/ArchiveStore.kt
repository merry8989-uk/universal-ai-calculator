package com.merry8989uk.universalcalculator.data

import android.content.Context
import com.merry8989uk.universalcalculator.calc.ArchiveEntry
import com.merry8989uk.universalcalculator.calc.ArchiveFormat
import com.merry8989uk.universalcalculator.calc.ArchiveSummary
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The calculation archive, on the device.
 *
 * It lives in the app's own external folder —
 * `Android/data/<package>/files/Nexus Calculator/` — because that is the one
 * place on Android that is both **visible in a file manager** and needs **no
 * permission**. A folder in shared storage would need storage permission and is
 * fragile; the app's private folder is invisible to the user. This is the middle
 * road, and it is the one that matches what was asked for.
 *
 * Nothing here is ever called when the history is cleared. The two are separate
 * on purpose: the history is a working list, the archive is the record.
 */
object ArchiveStore {

    /** The folder's name, as it appears in a file manager. */
    const val FOLDER = "Nexus Calculator"

    private const val FY_FOLDER = "Financial Years"

    /**
     * Where the archive lives.
     *
     * The external app folder when it is available, the internal one otherwise —
     * a phone with no external storage still keeps its record, it is just not
     * visible from a file manager.
     */
    fun root(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, FOLDER)
    }

    private fun yearDir(context: Context, year: Int) = File(root(context), year.toString())

    private fun monthDir(context: Context, date: LocalDate) =
        File(yearDir(context, date.year), ArchiveFormat.monthName(date))

    private fun dayFile(context: Context, date: LocalDate) =
        File(monthDir(context, date), ArchiveFormat.dayFile(date))

    /**
     * Adds one calculation to the record.
     *
     * Appending is all that happens here, so it is quick and it cannot lose the
     * rest of the file: a crash mid-write leaves a short last line, which
     * [ArchiveFormat.parse] skips.
     *
     * Returns false only when the folder could not be written at all — the app
     * carries on either way, because a calculator that refuses to calculate
     * because its diary is full is a worse calculator.
     */
    fun append(context: Context, entry: ArchiveEntry): Boolean = runCatching {
        val date = entry.at.toLocalDate()
        val file = dayFile(context, date)
        file.parentFile?.mkdirs()
        val fresh = !file.exists()
        file.appendText(
            buildString {
                if (fresh) appendLine(ArchiveFormat.HEADER)
                appendLine(ArchiveFormat.line(entry))
            },
        )
        true
    }.getOrDefault(false)

    /** Everything recorded on one day, oldest first. */
    fun entries(context: Context, date: LocalDate): List<ArchiveEntry> = runCatching {
        val file = dayFile(context, date)
        if (!file.exists()) emptyList() else ArchiveFormat.parse(date, file.readText())
    }.getOrDefault(emptyList())

    /** Everything recorded in one month. */
    fun entries(context: Context, year: Int, month: Int): List<ArchiveEntry> = runCatching {
        val dir = File(yearDir(context, year), monthNameFor(year, month))
        if (!dir.isDirectory) return emptyList()
        (dir.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name != summaryName(year, month) && it.name.endsWith(".txt") }
            .sortedBy { it.name }
            .flatMap { file ->
                val date = dateOfFile(file.name, year) ?: return@flatMap emptyList()
                ArchiveFormat.parse(date, file.readText())
            }
            .sortedBy { it.at }
    }.getOrDefault(emptyList())

    /** Everything recorded in a financial year, 1 April to 31 March. */
    fun entriesForFinancialYear(context: Context, startYear: Int): List<ArchiveEntry> = runCatching {
        val from = LocalDate.of(startYear, 4, 1)
        val to = LocalDate.of(startYear + 1, 3, 31)
        val out = ArrayList<ArchiveEntry>()
        var cursor = from.withDayOfMonth(1)
        while (!cursor.isAfter(to)) {
            out.addAll(entries(context, cursor.year, cursor.monthValue))
            cursor = cursor.plusMonths(1)
        }
        out.sortedBy { it.at }
    }.getOrDefault(emptyList())

    /** The years that have anything in them, newest first. */
    fun years(context: Context): List<Int> = runCatching {
        (root(context).listFiles() ?: emptyArray())
            .filter { it.isDirectory && it.name.toIntOrNull() != null }
            .mapNotNull { it.name.toIntOrNull() }
            .sortedDescending()
    }.getOrDefault(emptyList())

    /** The months of a year that have anything in them, in calendar order. */
    fun months(context: Context, year: Int): List<Int> = runCatching {
        val dir = yearDir(context, year)
        if (!dir.isDirectory) return emptyList()
        (1..12).filter { month ->
            val m = File(dir, monthNameFor(year, month))
            m.isDirectory && (m.listFiles() ?: emptyArray()).any { it.name.endsWith(".txt") }
        }
    }.getOrDefault(emptyList())

    /** The days of a month that have anything in them. */
    fun days(context: Context, year: Int, month: Int): List<LocalDate> = runCatching {
        val dir = File(yearDir(context, year), monthNameFor(year, month))
        if (!dir.isDirectory) return emptyList()
        (dir.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.endsWith(".txt") && it.name != summaryName(year, month) }
            .mapNotNull { dateOfFile(it.name, year) }
            .sorted()
    }.getOrDefault(emptyList())

    /**
     * Rebuilds the month and financial-year summaries.
     *
     * Kept out of [append] on purpose: appending has to be instant, and a month
     * summary needs every day file in the month read. This runs when the archive
     * screen is opened, which is often enough.
     */
    fun refreshSummaries(context: Context, date: LocalDate): Boolean = runCatching {
        val monthEntries = entries(context, date.year, date.monthValue)
        val monthSummary = File(monthDir(context, date), ArchiveFormat.monthFile(date))
        monthSummary.parentFile?.mkdirs()
        monthSummary.writeText(ArchiveSummary.monthText(date.withDayOfMonth(1), monthEntries))

        val startYear = ArchiveFormat.financialYear(date).first
        val fyEntries = entriesForFinancialYear(context, startYear)
        val fyDir = File(root(context), FY_FOLDER)
        fyDir.mkdirs()
        File(fyDir, ArchiveFormat.financialYearFile(date))
            .writeText(ArchiveSummary.financialYearText(startYear, fyEntries))
        true
    }.getOrDefault(false)

    /** The whole archive as one text file, for sharing or keeping elsewhere. */
    fun exportText(context: Context): String = buildString {
        for (year in years(context)) {
            for (month in months(context, year)) {
                for (date in days(context, year, month)) {
                    for (e in entries(context, date)) {
                        appendLine(
                            ArchiveFormat.dayName(date) + " " + ArchiveFormat.time(e.at) +
                                "  [" + e.kind + "]  " + e.expression + " = " + e.result,
                        )
                    }
                }
            }
        }
    }

    /**
     * The whole archive as one string, for a backup.
     *
     * Same line shape as the day files, with the date put in front so it can be
     * rebuilt anywhere: `2026-10-26|09:08:50|Standard|9500*2|19,000|19000`.
     * The day files keep their own layout; this is only what travels.
     */
    fun backupText(context: Context): String = buildString {
        appendLine(ArchiveFormat.HEADER)
        for (year in years(context)) {
            for (month in months(context, year)) {
                for (date in days(context, year, month)) {
                    for (e in entries(context, date)) {
                        appendLine(date.toString() + "|" + ArchiveFormat.line(e))
                    }
                }
            }
        }
    }

    /**
     * Puts a backup back into the day files.
     *
     * Existing entries on the same day are kept, and one that is already there is
     * not written twice, so restoring the same backup a second time changes
     * nothing. Returns how many entries were actually added.
     */
    fun restoreBackup(context: Context, text: String): Int {
        var added = 0
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line == ArchiveFormat.HEADER) continue
            val bar = line.indexOf('|')
            if (bar <= 0) continue
            val date = runCatching { LocalDate.parse(line.substring(0, bar)) }.getOrNull() ?: continue
            val rest = line.substring(bar + 1)
            val entry = ArchiveFormat.parse(date, rest).firstOrNull() ?: continue

            val already = entries(context, date).any {
                it.at == entry.at && it.expression == entry.expression && it.result == entry.result
            }
            if (already) continue
            if (append(context, entry)) added++
        }
        return added
    }

    /** How many calculations the archive holds in total. */
    fun totalEntries(context: Context): Int {
        var total = 0
        for (year in years(context)) {
            for (month in months(context, year)) total += entries(context, year, month).size
        }
        return total
    }

    /** Where the archive is, for showing the user. */
    fun location(context: Context): String = root(context).absolutePath

    // ---- names ----

    private fun monthNameFor(year: Int, month: Int): String =
        ArchiveFormat.monthName(LocalDate.of(year, month, 1))

    private fun summaryName(year: Int, month: Int): String =
        ArchiveFormat.monthFile(LocalDate.of(year, month, 1))

    /** `01 October 2026.txt` back to a date. */
    private fun dateOfFile(name: String, year: Int): LocalDate? = runCatching {
        val body = name.removeSuffix(".txt").trim()
        val parts = body.split(" ")
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = (1..12).firstOrNull {
            ArchiveFormat.monthName(LocalDate.of(year, it, 1)).equals(parts[1], ignoreCase = true)
        } ?: return null
        LocalDate.of(year, month, day)
    }.getOrNull()

    /** The current moment, in the form an entry wants. */
    fun now(): LocalDateTime = LocalDateTime.now()
}

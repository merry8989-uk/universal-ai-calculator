package com.merry8989uk.universalcalculator.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.merry8989uk.universalcalculator.calc.InterestBreakdown
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes a calculation and its schedule as a CSV and hands it to the share sheet.
 *
 * A schedule of thirty instalments is not something anyone reads off a phone
 * screen — it is something they put in a spreadsheet next to the rest of their
 * books. The PDF's last section asks for an export for exactly this reason.
 */
object CsvExport {

    fun write(context: Context, breakdown: InterestBreakdown, name: String = "interest"): File {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val out = File(dir, "$name-$stamp.csv")
        val sb = StringBuilder()

        fun row(vararg cells: String) {
            sb.append(cells.joinToString(",") { esc(it) }).append('\n')
        }

        row("Field", "Value")
        row("Principal", num(breakdown.principal))
        row("Rate per charge, days", num(breakdown.daysPerRateCharge))
        row("Daily rate, percent", num(breakdown.dailyRatePercent))
        row("Duration, days", num(breakdown.totalDays))
        row("Interest", num(breakdown.totalInterest))
        row("Interest per day", num(breakdown.dailyInterestAmount))
        row("Late charge", num(breakdown.penaltyAmount))
        row("Tax withheld", num(breakdown.tdsAmount))
        row("Net interest", num(breakdown.netInterest))
        row("Fee", num(breakdown.fees))
        row("Received", num(breakdown.amountReceived))
        row("Total payable", num(breakdown.totalAmount))
        row("Effective annual, percent", num(breakdown.effectiveAnnualPercent))
        row("Effective monthly, percent", num(breakdown.effectiveMonthlyPercent))

        if (breakdown.tierInterest.isNotEmpty()) {
            sb.append('\n')
            row("Band", "Interest")
            for ((label, amount) in breakdown.tierInterest) row(label, num(amount))
        }

        if (breakdown.schedule.isNotEmpty()) {
            sb.append('\n')
            row("Period", "Opening", "Interest", "Principal", "Instalment", "Closing")
            for (r in breakdown.schedule) {
                row(
                    r.period.toString(),
                    num(r.opening),
                    num(r.interest),
                    num(r.principal),
                    num(r.instalment),
                    num(r.closing),
                )
            }
        }

        out.writeText(sb.toString())
        return out
    }

    fun share(context: Context, file: File): String? {
        if (!file.exists() || file.length() == 0L) return "The CSV was not written."
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // the caller is the application, so the chooser needs its own task
            context.startActivity(
                Intent.createChooser(send, "Share the schedule").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            null
        } catch (e: Exception) {
            "Could not share the file (${e.javaClass.simpleName}). It is saved as ${file.name}."
        }
    }

    private fun esc(s: String): String =
        if (s.contains(',') || s.contains('"') || s.contains('\n')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else {
            s
        }

    private fun num(v: Double): String =
        if (v.isNaN() || v.isInfinite()) "" else String.format(Locale.US, "%.2f", v)
}

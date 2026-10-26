package com.merry8989uk.universalcalculator.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.calc.ArchiveEntry
import com.merry8989uk.universalcalculator.calc.ArchiveFormat
import com.merry8989uk.universalcalculator.calc.ArchiveSummary
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.data.ArchiveStore
import java.time.LocalDate

/**
 * The archive, as the user drew it: a year, a month, then day by day.
 *
 * The calculation is on the left and the date on the right, so a column of
 * figures can be read down without the dates getting in the way.
 *
 * This is the one screen that reads the archive. Nothing here can delete it —
 * there is no clear button on purpose, because the whole point of the archive is
 * that it survives a clear.
 */
@Composable
fun ArchiveScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }

    // rebuild the month and year summaries whenever this screen is opened
    remember(today) { ArchiveStore.refreshSummaries(context, today) }

    val years = remember(today) { ArchiveStore.years(context).ifEmpty { listOf(today.year) } }
    var year by remember(today) { mutableStateOf(years.firstOrNull() ?: today.year) }
    val months = remember(year) { ArchiveStore.months(context, year).ifEmpty { listOf(today.monthValue) } }
    var month by remember(year) { mutableStateOf(months.firstOrNull() ?: today.monthValue) }

    val entries = remember(year, month) { ArchiveStore.entries(context, year, month) }
    val byDay = remember(entries) { entries.groupBy { it.at.toLocalDate() }.toSortedMap() }
    val withNumbers = ArchiveSummary.howManyHaveANumber(entries)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 " + stringResource(R.string.back)) }
        Text(stringResource(R.string.archive_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.archive_where) + " " + ArchiveStore.location(context),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.archive_kept),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (years.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (y in years) {
                    FilterChip(selected = y == year, onClick = { year = y }, label = { Text(y.toString()) })
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (m in months) {
                val name = ArchiveFormat.monthName(LocalDate.of(year, m, 1))
                FilterChip(selected = m == month, onClick = { month = m }, label = { Text(name) })
            }
        }

        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.archive_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            Text(
                ArchiveFormat.monthName(LocalDate.of(year, month, 1)) + " " + year,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            for ((date, list) in byDay) {
                // the day's entries, with the date on the right of the last one
                Text(
                    ArchiveFormat.dayName(date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                )
                for (e in list) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            e.expression + " = " + e.result,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            ArchiveFormat.time(e.at).take(5),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                stringResource(R.string.archive_month_total, ArchiveSummary.count(entries)),
                style = MaterialTheme.typography.titleSmall,
            )
            if (withNumbers > 0) {
                Text(
                    stringResource(R.string.archive_month_sum, withNumbers, Format.number(ArchiveSummary.sum(entries))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (withNumbers < entries.size) {
                Text(
                    stringResource(R.string.archive_sum_note, entries.size - withNumbers),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // the year's own total, so the bigger picture is one tap away
        val fyStart = ArchiveFormat.financialYear(LocalDate.of(year, month, 1)).first
        val fyEntries = remember(fyStart) { ArchiveStore.entriesForFinancialYear(context, fyStart) }
        if (fyEntries.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                ArchiveFormat.financialYearName(LocalDate.of(year, month, 1)),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(R.string.archive_year_total, ArchiveSummary.count(fyEntries)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = {
                val text = ArchiveStore.exportText(context)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(send, null))
            }) { Text(stringResource(R.string.archive_export)) }
            Button(onClick = { ArchiveStore.refreshSummaries(context, today) }) {
                Text(stringResource(R.string.archive_refresh))
            }
        }
    }
}

/** One entry, for anything that wants it outside the screen. */
internal fun ArchiveEntry.text(): String = expression + " = " + result

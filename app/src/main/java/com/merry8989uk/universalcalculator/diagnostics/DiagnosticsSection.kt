package com.merry8989uk.universalcalculator.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.merry8989uk.universalcalculator.BuildConfig
import com.merry8989uk.universalcalculator.R

/**
 * The Diagnostics block that sits inside the Settings screen (Rule 7):
 * collection toggle (on by default), export format picker, export & share, and
 * clear.
 */
@Composable
fun DiagnosticsSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    vm: DiagnosticsViewModel = viewModel(),
) {
    val appName = stringResource(R.string.app_name)
    var format by remember { mutableStateOf(LogExporter.Format.TXT) }
    var live by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(enabled) { vm.refresh() }

    // Poll the in-memory log ring once a second so the box updates in real time.
    LaunchedEffect(Unit) {
        while (true) {
            live = DiagnosticsLogger.recent()
            delay(1000)
        }
    }

    val context = LocalContext.current
    var groups by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var top by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    // The round trip to the app's own server, measured only when asked.
    var pingBusy by remember { mutableStateOf(false) }
    var pingReport by remember { mutableStateOf<ConnectionCheck.Report?>(null) }
    val pingScope = rememberCoroutineScope()
    var crashCount by remember { mutableIntStateOf(0) }
    var anrCount by remember { mutableIntStateOf(0) }
    var showCrashes by remember { mutableStateOf(false) }

    LaunchedEffect(enabled, live.size) {
        groups = Analytics.groupTotals(context)
        top = Analytics.counters(context).take(10)
        crashCount = DiagnosticsLogger.crashCount()
        anrCount = DiagnosticsLogger.anrCount()
    }

    Text(stringResource(R.string.diag_title), style = MaterialTheme.typography.titleMedium)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.diag_collect))
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }

    Text(
        stringResource(R.string.diag_collect_help, humanSize(vm.logBytes)),
        style = MaterialTheme.typography.bodySmall,
    )

    // ---- connection ----
    Text(stringResource(R.string.diag_connection), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.diag_connection_help), style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = {
                if (pingBusy) return@Button
                pingBusy = true
                pingScope.launch {
                    pingReport = ConnectionCheck.run()
                    pingBusy = false
                }
            },
            enabled = !pingBusy,
        ) {
            Text(stringResource(if (pingBusy) R.string.diag_pinging else R.string.diag_ping))
        }
        pingReport?.let { report ->
            Text(
                if (report.reachable) {
                    stringResource(R.string.diag_ping_result, report.best, report.average, report.worst)
                } else {
                    stringResource(R.string.diag_ping_failed)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
    pingReport?.let { report ->
        Text(report.verdict, style = MaterialTheme.typography.bodySmall)
    }

    Text(stringResource(R.string.diag_export_format))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LogExporter.Format.entries.forEach { f ->
            FilterChip(
                selected = format == f,
                onClick = { format = f },
                label = { Text(".${f.ext}") },
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { vm.exportAndShare(format, appName, BuildConfig.VERSION_NAME) },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.diag_export))
        }
        OutlinedButton(
            onClick = { vm.clear() },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.diag_clear))
        }
    }

    // ---- usage counters and crashes ----
    Text(stringResource(R.string.diag_usage), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.diag_usage_help), style = MaterialTheme.typography.bodySmall)
    if (groups.isEmpty()) {
        Text(stringResource(R.string.diag_usage_empty), style = MaterialTheme.typography.bodySmall)
    } else {
        groups.forEach { (label, count) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text("$count", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (top.isNotEmpty()) {
            Text(stringResource(R.string.diag_most_used), style = MaterialTheme.typography.bodyMedium)
            top.forEach { (key, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(key, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    Text("$count", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    Text(
        if (crashCount == 0) stringResource(R.string.diag_crashes_none)
        else stringResource(R.string.diag_crashes, crashCount),
        style = MaterialTheme.typography.bodyMedium,
    )
    if (anrCount > 0) {
        Text(
            stringResource(R.string.diag_anr, anrCount),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (crashCount > 0) {
            OutlinedButton(onClick = { showCrashes = !showCrashes }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.diag_view_crashes))
            }
        }
        OutlinedButton(
            onClick = {
                DiagnosticsLogger.clearEverything(context)
                vm.refresh()
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.diag_reset_counters))
        }
    }
    if (showCrashes && crashCount > 0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A0F14))
                .padding(10.dp),
        ) {
            Text(
                DiagnosticsLogger.crashReport(),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE7B9C4),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
    }

    Text(stringResource(R.string.diag_live))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D0E15))
            .padding(10.dp),
    ) {
        if (live.isEmpty()) {
            Text(
                stringResource(R.string.diag_live_empty),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8A93A6),
                fontFamily = FontFamily.Monospace,
            )
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                live.takeLast(120).forEach { raw ->
                    // The file keeps milliseconds; showing them here made the
                    // timestamp look like a slow response time.
                    val line = DiagnosticsLogger.displayLine(raw)
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7CE38B),
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
    Text(
        "Stored on this device only \u2014 nothing is uploaded anywhere.",
        style = MaterialTheme.typography.bodySmall,
    )

    vm.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}

private fun humanSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

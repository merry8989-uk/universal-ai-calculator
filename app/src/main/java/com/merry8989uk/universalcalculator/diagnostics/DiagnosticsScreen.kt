package com.merry8989uk.universalcalculator.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.merry8989uk.universalcalculator.BuildConfig
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.ui.copyToClipboard
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The four things a diagnostics screen is for. */
private val TABS = listOf("Logs", "Usage", "Connection", "Export")

/**
 * Diagnostics, in one place.
 *
 * Everything used to be one long column inside Settings — the live log, the
 * counters, the crash report, the ping, the export buttons, the event list — so
 * the log was nowhere near the export button that sends it, and a settings
 * screen had become a debugging screen. It is its own screen now, and the four
 * things it does are four tabs.
 */
@Composable
fun DiagnosticsScreen(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    vm: DiagnosticsViewModel = viewModel(),
) {
    var tab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Diagnostics", style = MaterialTheme.typography.titleLarge)
        Text(
            "What this app records about itself, kept on this device unless you export it.",
            style = MaterialTheme.typography.bodySmall,
        )

        // ---- the one switch that governs all of it ----
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Collect diagnostics", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Logs, counters and crash reports stay on this phone.",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = onEnabledChange)
                }
                Text(
                    stringResource(R.string.diag_collect_help, humanSize(vm.logBytes)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (!enabled) {
                    Text(
                        "Collection is off. Nothing new is written; what is already here can still be exported or cleared.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        // ---- the tabs ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TABS.forEachIndexed { index, name ->
                FilterChip(
                    selected = tab == index,
                    onClick = { tab = index },
                    label = { Text(name) },
                )
            }
        }

        when (tab) {
            0 -> LogsTab(enabled)
            1 -> UsageTab(context, vm)
            2 -> ConnectionTab()
            else -> ExportTab(appName, vm)
        }
    }
}

// ---------------------------------------------------------------- the log

@Composable
private fun LogsTab(enabled: Boolean) {
    var live by remember { mutableStateOf<List<String>>(emptyList()) }
    var showEvents by remember { mutableStateOf(false) }

    // the ring is polled once a second so the box updates as things happen
    LaunchedEffect(Unit) {
        while (true) {
            live = DiagnosticsLogger.recent()
            delay(1000)
        }
    }

    Text(
        "Live log",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 14.dp),
    )
    Text(
        stringResource(R.string.diag_live),
        style = MaterialTheme.typography.bodySmall,
    )
    LogBox(
        lines = live.map { DiagnosticsLogger.displayLine(it) },
        empty = stringResource(R.string.diag_live_empty),
        colour = Color(0xFF7CE38B),
        height = 240.dp,
    )

    val eventCount = EventLog.totalCount()
    val waiting = EventUploader.pendingCount(LocalContext.current)
    Text(
        "Structured events",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
    Text(
        "$eventCount events  \u00B7  ${EventLog.sizeBytes() / 1024} KB  \u00B7  " +
            if (EventLog.enabled) "collecting" else "off",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        if (waiting == 0) "All events delivered to the warehouse"
        else "$waiting events waiting to be sent" +
            (EventUploader.lastError().takeIf { it.isNotBlank() }?.let { "  (last try: $it)" } ?: ""),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp),
    )
    if (eventCount > 0) {
        val byCategory = EventLog.counts().groupBy { name -> name.first.substringBefore('.') }
        Text(
            byCategory.entries.joinToString("  \u00B7  ") { (k, v) -> "$k ${v.sumOf { it.second }}" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
        OutlinedButton(
            onClick = { showEvents = !showEvents },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) { Text(if (showEvents) "Hide recent events" else "Show recent events") }
        if (showEvents) {
            LogBox(
                lines = EventLog.recent().takeLast(80),
                empty = "No events yet.",
                colour = Color(0xFFB7C4FF),
                height = 240.dp,
            )
        }
    }
    Text(
        "Stored on this device only \u2014 nothing is uploaded anywhere unless you export it.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp),
    )
}

// -------------------------------------------------------------- the usage

@Composable
private fun UsageTab(context: android.content.Context, vm: DiagnosticsViewModel) {
    var groups by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var top by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var crashCount by remember { mutableIntStateOf(0) }
    var anrCount by remember { mutableIntStateOf(0) }
    var showCrashes by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        groups = Analytics.groupTotals(context)
        top = Analytics.counters(context).take(12)
        crashCount = DiagnosticsLogger.crashCount()
        anrCount = DiagnosticsLogger.anrCount()
    }

    Text(
        stringResource(R.string.diag_usage),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 14.dp),
    )
    Text(stringResource(R.string.diag_usage_help), style = MaterialTheme.typography.bodySmall)
    if (groups.isEmpty()) {
        Text(stringResource(R.string.diag_usage_empty), style = MaterialTheme.typography.bodySmall)
    } else {
        groups.forEach { (label, count) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text("$count", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (top.isNotEmpty()) {
            Text(
                stringResource(R.string.diag_most_used),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp),
            )
            top.forEach { (key, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        key,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("$count", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    Text(
        "Crashes and freezes",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
    Text(
        if (crashCount == 0) stringResource(R.string.diag_crashes_none)
        else stringResource(R.string.diag_crashes, crashCount),
        style = MaterialTheme.typography.bodyMedium,
    )
    if (anrCount > 0) {
        Text(stringResource(R.string.diag_anr, anrCount), style = MaterialTheme.typography.bodyMedium)
    }
    if (crashCount > 0) {
        OutlinedButton(
            onClick = { showCrashes = !showCrashes },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) { Text(if (showCrashes) "Hide crash report" else stringResource(R.string.diag_view_crashes)) }
        if (showCrashes) {
            LogBox(
                lines = DiagnosticsLogger.crashReport().lines(),
                empty = "",
                colour = Color(0xFFE7B9C4),
                height = 220.dp,
            )
        }
    }

    // What a build server cannot check: whether the files made it into the
    // package and whether the engines run on this phone rather than on a
    // desktop. It is the answer to "something is wrong and I cannot say what".
    var checkResults by remember { mutableStateOf<List<CheckResult>?>(null) }
    Text(
        "Self-check",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
    Text(
        "Reads what is in the app and runs the engines once. No network, nothing written.",
        style = MaterialTheme.typography.bodySmall,
    )
    Button(
        onClick = { checkResults = SelfCheck.runAndLog(context) },
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) { Text("Run the self-check") }
    checkResults?.let { results ->
        Column(modifier = Modifier.padding(top = 6.dp)) {
            for (r in results) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        if (r.ok) "ok" else "FAIL",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (r.ok) Color(0xFF3FB950) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Column {
                        Text(r.name, style = MaterialTheme.typography.bodySmall)
                        Text(
                            r.detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TextButton(onClick = {
                copyToClipboard(context, "self-check", SelfCheck.report(results))
            }) { Text("Copy the report") }
        }
    }

    OutlinedButton(
        onClick = {
            DiagnosticsLogger.clearEverything(context)
            vm.refresh()
            groups = emptyList()
            top = emptyList()
            crashCount = 0
            anrCount = 0
        },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) { Text(stringResource(R.string.diag_reset_counters)) }
}

// --------------------------------------------------------- the connection

@Composable
private fun ConnectionTab() {
    var busy by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<ConnectionCheck.Report?>(null) }
    val scope = rememberCoroutineScope()

    Text(
        stringResource(R.string.diag_connection),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 14.dp),
    )
    Text(stringResource(R.string.diag_connection_help), style = MaterialTheme.typography.bodySmall)
    Button(
        onClick = {
            if (busy) return@Button
            busy = true
            scope.launch {
                report = ConnectionCheck.run()
                busy = false
            }
        },
        enabled = !busy,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(stringResource(if (busy) R.string.diag_pinging else R.string.diag_ping))
    }
    report?.let { r ->
        Text(
            if (r.reachable) {
                stringResource(R.string.diag_ping_result, r.best, r.average, r.worst)
            } else {
                stringResource(R.string.diag_ping_failed)
            },
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(r.verdict, style = MaterialTheme.typography.bodySmall)
    }
}

// ------------------------------------------------------------- the export

@Composable
private fun ExportTab(appName: String, vm: DiagnosticsViewModel) {
    var format by remember { mutableStateOf(LogExporter.Format.TXT) }
    val context = LocalContext.current

    Text(
        "Export",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 14.dp),
    )
    Text(
        "A file with everything on this screen: the log, the events, the counters and any crash reports.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(stringResource(R.string.diag_export_format), modifier = Modifier.padding(top = 8.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LogExporter.Format.entries.forEach { f ->
            FilterChip(
                selected = format == f,
                onClick = { format = f },
                label = { Text(".${f.ext}") },
            )
        }
    }
    Text(
        when (format) {
            LogExporter.Format.TXT -> "Plain text. Opens anywhere."
            LogExporter.Format.MD -> "Markdown. Reads well in a chat or a document."
            LogExporter.Format.JSON -> "One entry per line, for a script to read."
            LogExporter.Format.ZIP -> "A zip holding app.log, events.jsonl and meta.json."
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Button(
        onClick = { vm.exportAndShare(format, appName, BuildConfig.VERSION_NAME) },
        enabled = !vm.exporting,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    ) {
        Text(if (vm.exporting) "Exporting\u2026" else stringResource(R.string.diag_export))
    }

    // The result of the last export, shown here rather than being thrown away:
    // a button that appears to do nothing is indistinguishable from one that is
    // broken, which is exactly how the export was reported.
    vm.message?.let { line ->
        Card(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(line, style = MaterialTheme.typography.bodyMedium)
                vm.lastExportPath?.let { path ->
                    Text(
                        path,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                TextButton(onClick = { vm.dismissMessage() }) { Text("Dismiss") }
            }
        }
    }

    OutlinedButton(
        onClick = { vm.clear() },
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    ) { Text(stringResource(R.string.diag_clear)) }
    Text(
        "Clearing removes the log and the events from this phone. Counters are cleared on the Usage tab.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ----------------------------------------------------------------- shared

/** "0 B", "12 KB", "1.4 MB" — a size a person can read. */
internal fun humanSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
}

@Composable
private fun LogBox(lines: List<String>, empty: String, colour: Color, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D0E15))
            .padding(10.dp),
    ) {
        if (lines.isEmpty()) {
            Text(
                empty,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8A93A6),
                fontFamily = FontFamily.Monospace,
            )
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                lines.forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        color = colour,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

/**
 * The row in Settings that opens the screen above.
 *
 * Settings keeps the switch — turning collection off is a privacy decision and
 * belongs with the other privacy switches — and hands everything else over.
 */
@Composable
fun DiagnosticsSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
    /** Whether what is recorded here may also be sent to the developer. */
    shareEnabled: Boolean = false,
    onShareChange: (Boolean) -> Unit = {},
) {
    var live by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            live = EventLog.totalCount()
            delay(2000)
        }
    }
    Text(
        stringResource(R.string.diag_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.diag_collect))
            Text(
                if (enabled) "$live events recorded on this device"
                else "Off \u2014 nothing new is recorded",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
    // The second promise, next to the first one and off by default. Keeping a
    // log on this phone and sending it somewhere are different agreements.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Send diagnostics to the developer")
            Text(
                if (shareEnabled) {
                    "Event names, device model and app version go to this app's own warehouse"
                } else {
                    "Off \u2014 nothing leaves this phone"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = shareEnabled,
            onCheckedChange = onShareChange,
            enabled = enabled,
        )
    }
    OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Text("Open diagnostics \u2014 logs, usage, connection, export")
    }
}

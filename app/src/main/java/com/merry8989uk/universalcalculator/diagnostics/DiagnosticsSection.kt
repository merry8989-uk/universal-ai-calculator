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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
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

    Text("Diagnostics", style = MaterialTheme.typography.titleMedium)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Collect logs")
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }

    Text(
        "Logs are collected by default so problems can be diagnosed. " +
            "${humanSize(vm.logBytes)} stored.",
        style = MaterialTheme.typography.bodySmall,
    )

    Text("Export format")
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
            Text("Export & share")
        }
        OutlinedButton(
            onClick = { vm.clear() },
            modifier = Modifier.weight(1f),
        ) {
            Text("Clear logs")
        }
    }

    Text("Live log (updates in real time)")
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
                "Nothing logged yet. Logs are written on this device only.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8A93A6),
                fontFamily = FontFamily.Monospace,
            )
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                live.takeLast(120).forEach { line ->
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

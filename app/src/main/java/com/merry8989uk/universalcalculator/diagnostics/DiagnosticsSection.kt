package com.merry8989uk.universalcalculator.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(enabled) { vm.refresh() }

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

    vm.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}

private fun humanSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

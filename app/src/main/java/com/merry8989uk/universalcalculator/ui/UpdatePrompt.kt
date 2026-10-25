package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.updater.UpdateUiState

/**
 * The update, shown the moment the app finds one.
 *
 * It says what version is waiting and then, once the download starts, how far
 * along it is — a real bar with a real percentage, taken from the bytes that
 * have actually arrived, not a spinner that means nothing.
 */
@Composable
internal fun UpdatePrompt(
    state: UpdateUiState,
    onInstall: () -> Unit,
    onLater: () -> Unit,
    onDismissFailure: () -> Unit,
) {
    when (state) {
        is UpdateUiState.Available -> AlertDialog(
            onDismissRequest = onLater,
            title = { Text("Update to ${state.info.latestVersion}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A newer version of Nexus Calc is ready. It installs over this one — your history and settings are kept.")
                    if (state.info.notes.isNotBlank()) {
                        Text(
                            state.info.notes.trim().lines().take(6).joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState()),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onInstall) { Text("Download and install") }
            },
            dismissButton = {
                TextButton(onClick = onLater) { Text("Later") }
            },
        )

        is UpdateUiState.Downloading -> {
            val p = state.progress
            val spoken = p.percent?.let { "Downloading update, $it percent" } ?: "Downloading update"
            AlertDialog(
                // not dismissable: the download is already running
                onDismissRequest = {},
                title = { Text("Downloading the update") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = spoken
                        },
                    ) {
                        if (p.fraction != null) {
                            LinearProgressIndicator(
                                progress = { p.fraction },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            // the server did not say how big the file is
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                p.percent?.let { "$it%" } ?: "Downloading…",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                if (p.total > 0L) {
                                    "${mb(p.received)} of ${mb(p.total)} MB"
                                } else {
                                    "${mb(p.received)} MB"
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            "The installer opens by itself when the file is complete.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                confirmButton = {},
            )
        }

        is UpdateUiState.Failed -> AlertDialog(
            onDismissRequest = onDismissFailure,
            title = { Text("The update did not finish") },
            text = {
                Text(
                    "${state.message}\n\nYour app is untouched and still on the version you had. " +
                        "You can try again from Settings.",
                )
            },
            confirmButton = { TextButton(onClick = onDismissFailure) { Text("Close") } },
        )

        else -> Unit
    }
}

/** Bytes as megabytes, to one decimal place. */
private fun mb(bytes: Long): String =
    String.format(java.util.Locale.US, "%.1f", bytes / 1024.0 / 1024.0)

/** The progress bar's own description, for the tests and screen readers. */
internal fun progressDescription(percent: Int?): String =
    percent?.let { "Downloading update, $it percent" } ?: "Downloading update"

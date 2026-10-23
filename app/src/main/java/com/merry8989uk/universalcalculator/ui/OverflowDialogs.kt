package com.merry8989uk.universalcalculator.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.merry8989uk.universalcalculator.BuildConfig
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import com.merry8989uk.universalcalculator.settings.Contact
import com.merry8989uk.universalcalculator.updater.UpdateUiState
import com.merry8989uk.universalcalculator.updater.UpdateViewModel

/** The live diagnostics log, with a way to clear or share it. */
@Composable
fun DiagnosticsLogDialog(
    lines: List<String>,
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.diagnostics_title)) },
        text = {
            if (lines.isEmpty()) {
                Text(stringResource(R.string.diagnostics_empty))
            } else {
                Column(
                    modifier = Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
                ) {
                    lines.forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.clear_log)) }
                TextButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, DiagnosticsLogger.readAll())
                    }
                    context.startActivity(Intent.createChooser(share, null))
                }) { Text(stringResource(R.string.share_log)) }
            }
        },
    )
}

/**
 * Version, update check, the Instagram handle to reach us on, and the privacy
 * policy.
 */
@Composable
fun AppInfoDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val updater: UpdateViewModel = viewModel()
    val state = updater.state

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.app_info_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.titleMedium,
                )

                when (state) {
                    UpdateUiState.Checking -> Text(stringResource(R.string.checking_updates))
                    UpdateUiState.UpToDate -> Text(stringResource(R.string.up_to_date))
                    UpdateUiState.Downloading -> Text(stringResource(R.string.checking_updates))
                    is UpdateUiState.Failed -> Text(stringResource(R.string.update_failed))
                    is UpdateUiState.Available -> Button(onClick = { updater.install(state.info) }) {
                        Text(stringResource(R.string.update_available, state.info.version))
                    }
                    UpdateUiState.Idle -> Unit
                }

                OutlinedButton(onClick = { updater.check(BuildConfig.VERSION_NAME) }) {
                    Text(stringResource(R.string.check_for_updates))
                }
                OutlinedButton(onClick = { Contact.openInstagram(context) }) {
                    Text(
                        stringResource(R.string.contact_on_instagram) + "  " +
                            stringResource(R.string.instagram_handle, Contact.INSTAGRAM_HANDLE),
                    )
                }
                OutlinedButton(onClick = { Contact.openUrl(context, Contact.PRIVACY_URL) }) {
                    Text(stringResource(R.string.privacy_policy))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
        },
    )
}

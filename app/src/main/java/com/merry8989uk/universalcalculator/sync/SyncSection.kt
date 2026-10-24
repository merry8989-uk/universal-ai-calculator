package com.merry8989uk.universalcalculator.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The sync settings: a code the user chooses, a switch for whether history
 * travels, and the two actions.
 */
@Composable
fun SyncSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf(SyncStore.code(context)) }
    var includeHistory by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var lastUpload by remember { mutableStateOf(SyncStore.lastUpload(context)) }

    fun run(block: suspend () -> SyncService.Outcome) {
        if (busy) return
        busy = true
        status = ""
        scope.launch {
            val outcome = block()
            status = when (outcome) {
                is SyncService.Outcome.Ok -> outcome.message
                is SyncService.Outcome.Failed -> outcome.message
            }
            lastUpload = SyncStore.lastUpload(context)
            busy = false
        }
    }

    Text(stringResource(R.string.sync_title), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.sync_help), style = MaterialTheme.typography.bodySmall)

    OutlinedTextField(
        value = code,
        onValueChange = { code = it },
        label = { Text(stringResource(R.string.sync_code)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        stringResource(R.string.sync_code_help, SyncStore.MIN_CODE_LENGTH),
        style = MaterialTheme.typography.bodySmall,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.sync_include_history))
        Switch(checked = includeHistory, onCheckedChange = { includeHistory = it })
    }

    OutlinedButton(
        onClick = {
            SyncStore.setCode(context, code.trim())
            status = context.getString(R.string.sync_code_saved)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.sync_save_code))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { run { SyncService.upload(context, includeHistory) } },
            enabled = !busy,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.sync_upload))
        }
        OutlinedButton(
            onClick = { run { SyncService.restore(context, includeHistory) } },
            enabled = !busy,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.sync_restore))
        }
    }

    if (status.isNotBlank()) {
        Text(status, style = MaterialTheme.typography.bodyMedium)
    }
    if (lastUpload > 0L) {
        val when_ = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(lastUpload))
        Text(
            stringResource(R.string.sync_last_uploaded, when_),
            style = MaterialTheme.typography.bodySmall,
        )
    }

    Text(stringResource(R.string.sync_warning), style = MaterialTheme.typography.bodySmall)

    TextButton(onClick = { run { SyncService.erase(context) } }, enabled = !busy) {
        Text(stringResource(R.string.sync_delete))
    }
}

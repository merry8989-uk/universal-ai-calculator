package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.data.HistorySearch

/**
 * Full calculation history, newest first.
 *
 * Clearing asks first here too: this screen is where a user sees the whole tape
 * at once, and the Clear beside it is the one most likely to be reached for by
 * somebody who has just realised they are about to lose it.
 *
 * The search box is what makes this a history rather than a receipt. Three days
 * of work is more lines than can be read by eye, so the line worth going back to
 * is found by typing part of it — the sum, or the answer it came to — instead of
 * by scrolling and hoping. Clear still acts on the whole tape, not on what the
 * search happens to be showing: it says "Clear" and it means all of it.
 */
@Composable
fun HistoryScreen(
    history: String,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onPick: (String) -> Unit = {},
    purged: Int = 0,
    onUndoPurge: () -> Unit = {},
    onDismissPurge: () -> Unit = {},
) {
    val entries = history.split("\n").filter { it.isNotBlank() }.reversed()
    var confirming by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val shown = HistorySearch.lines(entries, query)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) { Text("\u2190 Back") }
            if (entries.isNotEmpty()) TextButton(onClick = { confirming = true }) { Text("Clear") }
        }
        Text("History", style = MaterialTheme.typography.titleLarge)
        Text(
            "Tap any calculation to put it back on the board.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp),
        )

        if (purged > 0) PurgeStrip(purged, onUndoPurge, onDismissPurge)

        if (entries.isNotEmpty()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.history_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }

        if (entries.isEmpty()) {
            Text(
                "No items yet. Your calculations will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else if (shown.isEmpty()) {
            Text(
                stringResource(R.string.search_no_match),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                shown.forEach { e ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onPick(e) }) {
                        Text(
                            e,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.history_clear_confirm_title)) },
            text = { Text(stringResource(R.string.history_clear_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onClear()
                    },
                ) { Text(stringResource(R.string.history_clear_confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

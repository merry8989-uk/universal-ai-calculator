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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Full calculation history, newest first. */
@Composable
fun HistoryScreen(
    history: String,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onPick: (String) -> Unit = {},
) {
    val entries = history.split("\n").filter { it.isNotBlank() }.reversed()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) { Text("\u2190 Back") }
            if (entries.isNotEmpty()) TextButton(onClick = onClear) { Text("Clear") }
        }
        Text("History", style = MaterialTheme.typography.titleLarge)
        Text(
            "Tap any calculation to put it back on the board.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp),
        )

        if (entries.isEmpty()) {
            Text(
                "No items yet. Your calculations will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                entries.forEach { e ->
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
}

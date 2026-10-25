package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.Markup

/**
 * The mark-up key.
 *
 * On a shop calculator MU is the "what do I sell this for" key: give it what
 * something cost and the margin you want, and it tells you the price. Doing it
 * as a small tool rather than a keystroke means it cannot be mistyped, and the
 * working is visible.
 */
@Composable
internal fun MarkupDialog(onClose: () -> Unit) {
    var cost by remember { mutableStateOf("100") }
    var margin by remember { mutableStateOf("25") }

    val c = cost.trim().toDoubleOrNull()
    val m = margin.trim().toDoubleOrNull()
    val result = if (c != null && m != null) Markup.sellingPrice(c, m) else null

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.markup_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.markup_help), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cost, onValueChange = { cost = it },
                        label = { Text(stringResource(R.string.markup_cost)) },
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = margin, onValueChange = { margin = it },
                        label = { Text(stringResource(R.string.markup_margin)) },
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                if (result == null) {
                    Text(
                        stringResource(R.string.markup_impossible),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        stringResource(R.string.markup_price) + " " + Format.number(result),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.markup_profit) + " " +
                            Format.number(result - c!!),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
        },
    )
}

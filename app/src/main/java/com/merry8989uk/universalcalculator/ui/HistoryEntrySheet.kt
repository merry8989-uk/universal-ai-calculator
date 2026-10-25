package com.merry8989uk.universalcalculator.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.StepSolver

/**
 * What can be done with one line of history.
 *
 * The clear button wipes everything, which is right for a fresh start and
 * useless for one wrong line. Holding an entry opens this instead: the working,
 * the entry itself, and the things a person actually wants to do with it.
 */
@Composable
internal fun HistoryEntrySheet(
    entry: String,
    onSteps: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onAskAi: () -> Unit,
    onUse: () -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(entry, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Option(stringResource(R.string.history_steps), onSteps)
                Option(stringResource(R.string.history_use), onUse)
                Option(stringResource(R.string.history_copy)) {
                    clipboard.setText(AnnotatedString(entry))
                    onCopy()
                }
                Option(stringResource(R.string.history_share)) {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, entry)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                    onShare()
                }
                Option(stringResource(R.string.history_ask_ai), onAskAi)
                Option(stringResource(R.string.history_delete), danger = true, onPress = onDelete)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun Option(label: String, danger: Boolean = false, onPress: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPress)
            .padding(vertical = 12.dp),
    )
}

/**
 * The working, one move at a time.
 *
 * When [StepSolver] cannot show the steps honestly it says so rather than
 * inventing them: a wrong line of working is worse than no working at all.
 */
@Composable
internal fun StepsDialog(
    expression: String,
    answer: Double?,
    degrees: Boolean,
    onClose: () -> Unit,
) {
    val steps = StepSolver.steps(expression, answer, degrees)

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.steps_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    expression,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (steps.isEmpty()) {
                    Text(
                        stringResource(R.string.steps_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    steps.forEachIndexed { i, step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                "${i + 1}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "${step.piece}  \u2192  ${step.value}",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    step.result,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    step.why,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                if (answer != null) {
                    Text(
                        stringResource(R.string.steps_result) + " " + Format.number(answer),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
        },
    )
}

package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.DecimalFormat
import com.merry8989uk.universalcalculator.calc.Format

/**
 * Picking how numbers are written out.
 *
 * Every mode is shown with the same number worked out in it, so the choice is
 * made by looking at the result rather than by reading the name.
 */
@Composable
internal fun DecimalFormatDialog(
    current: DecimalFormat,
    digits: Int,
    onPick: (DecimalFormat) -> Unit,
    onDigits: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // a value that shows the difference between the modes clearly
    val sample = 12345678.912345
    val needsDigits = current != DecimalFormat.NORMAL

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Decimal number format") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DecimalFormat.all.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPick(mode) }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(selected = mode == current, onClick = { onPick(mode) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                Format.number(sample, mode, digits),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                            )
                        }
                    }
                }

                if (needsDigits) {
                    Text(
                        "Digits after the point: $digits",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Slider(
                        value = digits.toFloat(),
                        onValueChange = { onDigits(it.toInt()) },
                        valueRange = 0f..16f,
                        steps = 15,
                    )
                    Text(
                        Format.number(sample, current, digits),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

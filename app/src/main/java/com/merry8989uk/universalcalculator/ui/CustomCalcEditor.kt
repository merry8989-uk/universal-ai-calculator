package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.calc.CustomCalculator
import com.merry8989uk.universalcalculator.calc.CustomInput
import com.merry8989uk.universalcalculator.calc.Format

/**
 * Builds or changes one custom calculator. The formula is run against the
 * defaults as you type, so a mistake is visible before saving.
 */
@Composable
fun CustomCalcEditor(
    existing: CustomCalculator?,
    onSave: (CustomCalculator) -> Unit,
    onDelete: (CustomCalculator) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var expression by remember { mutableStateOf(existing?.expression.orEmpty()) }
    var unit by remember { mutableStateOf(existing?.unit.orEmpty()) }
    val inputs = remember {
        mutableStateListOf<CustomInput>().apply {
            existing?.inputs?.forEach { add(it) }
            if (isEmpty()) add(CustomInput("x", "Value", 1.0))
        }
    }

    // Live preview: recompute on every keystroke.
    val draft = CustomCalculator(
        id = existing?.id ?: "new",
        name = name,
        description = description,
        inputs = inputs.toList(),
        expression = expression,
        unit = unit,
    )
    val problem = draft.problem()
    val preview = if (problem == null) {
        runCatching { draft.compute(draft.defaults()) }.getOrNull()
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(if (existing == null) R.string.create_calculator else R.string.edit_calculator),
            style = MaterialTheme.typography.titleLarge,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.calc_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.calc_description)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.calc_inputs), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.calc_inputs_help),
            style = MaterialTheme.typography.bodySmall,
        )
        inputs.forEachIndexed { index, input ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input.key,
                    onValueChange = { inputs[index] = input.copy(key = it.trim()) },
                    label = { Text(stringResource(R.string.calc_input_name)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = input.label,
                    onValueChange = { inputs[index] = input.copy(label = it) },
                    label = { Text(stringResource(R.string.calc_input_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                )
                OutlinedTextField(
                    value = Format.number(input.default),
                    onValueChange = { text ->
                        text.toDoubleOrNull()?.let { inputs[index] = input.copy(default = it) }
                    },
                    label = { Text(stringResource(R.string.calc_default)) },
                    singleLine = true,
                    modifier = Modifier.weight(0.9f),
                )
                val removeSpoken = stringResource(R.string.calc_remove_input)
                IconButton(
                    onClick = { if (inputs.size > 1) inputs.removeAt(index) },
                    modifier = Modifier.semantics { contentDescription = removeSpoken },
                ) {
                    Text("\u2715", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        OutlinedButton(onClick = { inputs.add(CustomInput("", "", 0.0)) }) {
            Text(stringResource(R.string.calc_add_input))
        }

        OutlinedTextField(
            value = expression,
            onValueChange = { expression = it },
            label = { Text(stringResource(R.string.calc_formula)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = unit,
            onValueChange = { unit = it },
            label = { Text(stringResource(R.string.calc_unit)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // The preview, or the reason it will not work.
        if (problem != null) {
            Text(
                problem,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (preview != null) {
            Text(
                stringResource(R.string.calc_preview) + ": " +
                    Format.number(preview) + if (unit.isBlank()) "" else " $unit",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSave(draft.copy(id = existing?.id ?: newId())) },
                enabled = problem == null,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.calc_save))
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.back))
            }
        }
        if (existing != null) {
            TextButton(onClick = { onDelete(existing) }) {
                Text(stringResource(R.string.calc_delete))
            }
        }
    }
}

/** Good enough for a local id: nothing here is shared or synced. */
internal fun newId(): String = "c" + System.currentTimeMillis().toString(36)

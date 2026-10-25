package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.FunctionTable
import com.merry8989uk.universalcalculator.calc.FunctionTableBuilder

/**
 * TABLE mode: what a function does across a range.
 *
 * Give it `f(x)`, where to start, where to stop and how big a step to take, and
 * it writes the values out. A second function `g(x)` can go alongside for
 * comparing two. Cells the expression cannot fill are left blank rather than
 * failing the table, because one bad point should not hide the other ninety-nine.
 */
@Composable
internal fun TableScreen(onClose: () -> Unit) {
    var f by remember { mutableStateOf("x^2") }
    var g by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("-3") }
    var stop by remember { mutableStateOf("3") }
    var step by remember { mutableStateOf("1") }
    var maxEntries by remember { mutableStateOf("200") }
    var withG by remember { mutableStateOf(false) }
    var table by remember { mutableStateOf<FunctionTable?>(null) }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Table", style = MaterialTheme.typography.titleMedium)
            FilterChip(withG, { withG = it; if (!it) g = "" }, { Text("with g(x)") })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = f, onValueChange = { f = it }, label = { Text("f(x)") },
                singleLine = true, modifier = Modifier.weight(1f),
            )
            if (withG) {
                OutlinedTextField(
                    value = g, onValueChange = { g = it }, label = { Text("g(x)") },
                    singleLine = true, modifier = Modifier.weight(1f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = start, onValueChange = { start = it }, label = { Text("start") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = stop, onValueChange = { stop = it }, label = { Text("stop") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = step, onValueChange = { step = it }, label = { Text("step") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = maxEntries, onValueChange = { maxEntries = it },
            label = { Text("most rows") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                error = ""
                val a = start.toDoubleOrNull()
                val b = stop.toDoubleOrNull()
                val h = step.toDoubleOrNull()
                val limit = maxEntries.toIntOrNull() ?: FunctionTableBuilder.DEFAULT_MAX_ENTRIES
                when {
                    f.isBlank() -> { error = "f(x) needs an expression."; table = null }
                    a == null || b == null || h == null -> {
                        error = "Start, stop and step all need a number."
                        table = null
                    }
                    h == 0.0 -> { error = "A step of zero would never finish."; table = null }
                    else -> {
                        table = FunctionTableBuilder.build(f, if (withG) g else "", a, b, h, limit)
                        if (table?.isEmpty == true) error = "That range produced no rows."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Build the table") }

        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        table?.let { t ->
            if (t.rows.isNotEmpty()) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 6.dp),
                ) {
                    HeaderCell("x", 1f)
                    HeaderCell("f(x)", 1f)
                    if (withG) HeaderCell("g(x)", 1f)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 460.dp),
                ) {
                    items(t.rows) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ValueCell(Format.number(row.x), 1f)
                            ValueCell(row.f?.let { Format.number(it) } ?: "\u2014", 1f)
                            if (withG) ValueCell(row.g?.let { Format.number(it) } ?: "\u2014", 1f)
                        }
                    }
                }
                if (t.truncated) {
                    Text(
                        "Stopped at ${t.rows.size} rows. Raise the row limit or widen the step " +
                            "to see the rest of the range.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        "${t.rows.size} rows, stepping by ${Format.number(t.step)}.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Back to the tools") }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.weight(weight).padding(horizontal = 8.dp),
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ValueCell(text: String, weight: Float) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        modifier = Modifier.weight(weight).padding(horizontal = 8.dp),
        maxLines = 1,
    )
}

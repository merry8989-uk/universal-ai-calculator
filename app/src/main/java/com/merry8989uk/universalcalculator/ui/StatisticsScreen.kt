package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.Statistics

@Composable
fun StatisticsScreen(onBack: () -> Unit) {
    var data by remember { mutableStateOf("12, 15, 11, 18, 22, 15, 9, 25, 15, 30") }
    var xSeries by remember { mutableStateOf("1, 2, 3, 4, 5") }
    var ySeries by remember { mutableStateOf("2, 4, 5, 4, 5") }

    val values = Statistics.parse(data)
    val summary = Statistics.summary(values)
    val reg = Statistics.linearRegression(Statistics.parse(xSeries), Statistics.parse(ySeries))

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Statistics", style = MaterialTheme.typography.titleLarge)
        Text("Enter numbers separated by commas, spaces or new lines.",
            style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = data,
            onValueChange = { data = it },
            label = { Text("Data set") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().height(110.dp).padding(top = 8.dp),
        )

        Text("${values.size} values", style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp))

        summary.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (value.isNaN()) "\u2014" else Format.number(value),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        Text("Linear regression (y = a + bx)", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = xSeries, onValueChange = { xSeries = it }, label = { Text("x values") },
                singleLine = true, modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = ySeries, onValueChange = { ySeries = it }, label = { Text("y values") },
                singleLine = true, modifier = Modifier.weight(1f),
            )
        }
        StatLine("Slope (b)", reg[0])
        StatLine("Intercept (a)", reg[1])
        StatLine("R squared", reg[2])
        StatLine("Correlation (r)", reg[3])
    }
}

@Composable
private fun StatLine(label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            if (value.isNaN()) "\u2014" else Format.number(value),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

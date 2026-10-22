package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.DateCalc
import com.merry8989uk.universalcalculator.calc.Programmer
import java.time.LocalDate

@Composable
fun ProgrammerScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("255") }
    var base by remember { mutableIntStateOf(10) }

    var a by remember { mutableStateOf("12") }
    var b by remember { mutableStateOf("10") }
    var op by remember { mutableStateOf("AND") }

    val value: Long? = remember(input, base) {
        try {
            Programmer.fromBase(input, base)
        } catch (e: Exception) {
            null
        }
    }

    val result: String = remember(a, b, op) {
        val x = a.toLongOrNull()
        val y = b.toLongOrNull()
        if (x == null || y == null) "\u2014"
        else when (op) {
            "AND" -> Programmer.and(x, y).toString()
            "OR" -> Programmer.or(x, y).toString()
            "XOR" -> Programmer.xor(x, y).toString()
            "NOT a" -> Programmer.not(x).toString()
            "a << b" -> Programmer.shl(x, y).toString()
            "a >> b" -> Programmer.shr(x, y).toString()
            else -> "\u2014"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Programmer", style = MaterialTheme.typography.titleLarge)

        Text("Base converter", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Value") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(2, 8, 10, 16).forEach { bs ->
                FilterChip(selected = base == bs, onClick = { base = bs }, label = { Text("Base $bs") })
            }
        }
        if (value == null) {
            Text("Enter a valid base-$base number", color = MaterialTheme.colorScheme.error)
        } else {
            listOf(2, 8, 10, 16).forEach { bs ->
                Text(
                    "${baseName(bs)}: ${Programmer.toBase(value, bs)}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text("Bitwise operations", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = a, onValueChange = { a = it }, label = { Text("a") },
                singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = b, onValueChange = { b = it }, label = { Text("b") },
                singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("AND", "OR", "XOR", "NOT a", "a << b", "a >> b").forEach { o ->
                FilterChip(selected = op == o, onClick = { op = o }, label = { Text(o) })
            }
        }
        Text("Result: $result", fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.titleLarge)
    }
}

private fun baseName(b: Int) = when (b) {
    2 -> "Binary"
    8 -> "Octal"
    10 -> "Decimal"
    else -> "Hex"
}

@Composable
fun DateScreen(onBack: () -> Unit) {
    val today = remember { LocalDate.now().toString() }
    var fromText by remember { mutableStateOf("2000-01-01") }
    var toText by remember { mutableStateOf(today) }

    val from = remember(fromText) { runCatching { LocalDate.parse(fromText) }.getOrNull() }
    val to = remember(toText) { runCatching { LocalDate.parse(toText) }.getOrNull() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Date calculator", style = MaterialTheme.typography.titleLarge)
        Text("Dates in yyyy-mm-dd format", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = fromText, onValueChange = { fromText = it },
            label = { Text("From") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = toText, onValueChange = { toText = it },
            label = { Text("To") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        if (from == null || to == null) {
            Text("Enter both dates as yyyy-mm-dd", color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp))
        } else {
            val (y, m, d) = DateCalc.period(from, to)
            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ResultLine("Days", DateCalc.daysBetween(from, to).toString())
                ResultLine("Weeks", DateCalc.weeksBetween(from, to).toString())
                ResultLine("Months", DateCalc.monthsBetween(from, to).toString())
                ResultLine("Years", DateCalc.yearsBetween(from, to).toString())
                ResultLine("Working days", DateCalc.workingDays(from, to).toString())
                ResultLine("Age / span", "$y years, $m months, $d days")
            }
        }
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
    }
}

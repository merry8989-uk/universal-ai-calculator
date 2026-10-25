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
import androidx.compose.foundation.verticalScroll
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
fun ProgrammerScreen(
    onBack: () -> Unit,
    /** 32 or 64 bits: the width the keys wrap at. */
    width: com.merry8989uk.universalcalculator.calc.BitWidth =
        com.merry8989uk.universalcalculator.calc.BitWidth.BITS_64,
) {
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

    val result: String = remember(a, b, op, width) {
        val x = a.toLongOrNull()
        val y = b.toLongOrNull()
        if (x == null || y == null) "\u2014"
        else when (op) {
            // every result is fitted to the chosen width, so 32-bit NOT is -1
            // and 32-bit 0xFFFFFFFF + 1 wraps to 0
            "AND" -> width.fit(Programmer.and(x, y)).toString()
            "OR" -> width.fit(Programmer.or(x, y)).toString()
            "XOR" -> width.fit(Programmer.xor(x, y)).toString()
            "NOT a" -> width.fit(Programmer.not(x)).toString()
            "a << b" -> width.fit(Programmer.shl(x, y)).toString()
            "a >> b" -> width.fit(Programmer.shr(x, y)).toString()
            "a >>> b" -> width.fit(Programmer.ushr(x, y)).toString()
            "popcount a" -> Programmer.popcount(x).toString()
            else -> "\u2014"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Programmer", style = MaterialTheme.typography.titleLarge)

        Text("Base converter", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = input, onValueChange = { input = it }, label = { Text("Value") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(2, 8, 10, 16, 36).forEach { bs ->
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
            listOf("AND", "OR", "XOR", "NOT a", "a << b", "a >> b", "a >>> b", "popcount a").forEach { o ->
                FilterChip(selected = op == o, onClick = { op = o }, label = { Text(o) })
            }
        }
        Text(
            "Result: $result",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "Working in ${width.bits} bits.",
            style = MaterialTheme.typography.bodySmall,
        )
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
    var offsetText by remember { mutableStateOf("30") }
    var epochText by remember { mutableStateOf("1700000000") }
    var startTime by remember { mutableStateOf("09:30") }
    var endTime by remember { mutableStateOf("18:15") }

    val from = remember(fromText) { runCatching { LocalDate.parse(fromText) }.getOrNull() }
    val to = remember(toText) { runCatching { LocalDate.parse(toText) }.getOrNull() }
    val epoch = epochText.trim().toLongOrNull()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Date calculator", style = MaterialTheme.typography.titleLarge)
        Text("Dates in yyyy-mm-dd format", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = fromText, onValueChange = { fromText = it }, label = { Text("From") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = toText, onValueChange = { toText = it }, label = { Text("To") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Facts about the \"from\" date", style = MaterialTheme.typography.titleMedium)
        from?.let { dt ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 6.dp)) {
                ResultLine("Day of week", DateCalc.dayName(dt))
                ResultLine("Day of year", DateCalc.dayOfYear(dt).toString())
                ResultLine("ISO week", DateCalc.isoWeek(dt).toString())
                ResultLine("Quarter", "Q${DateCalc.quarter(dt)}")
                ResultLine("Days in month", DateCalc.daysInMonth(dt).toString())
                ResultLine("Leap year", if (DateCalc.isLeapYear(dt.year)) "Yes" else "No")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Add / subtract days", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = offsetText, onValueChange = { offsetText = it },
            label = { Text("Days (+ to add, - to subtract)") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        val offset = offsetText.trim().toLongOrNull()
        if (from != null && offset != null) {
            ResultLine("Result date", DateCalc.addDays(from, offset).toString())
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Unix timestamp", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = epochText, onValueChange = { epochText = it },
            label = { Text("Epoch seconds") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        if (epoch != null) ResultLine("Local date", DateCalc.fromEpochSeconds(epoch))
        from?.let { ResultLine("From date as epoch", DateCalc.toEpochSeconds(it).toString()) }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Time interval", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            OutlinedTextField(
                value = startTime, onValueChange = { startTime = it }, label = { Text("Start (HH:mm)") },
                singleLine = true, modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = endTime, onValueChange = { endTime = it }, label = { Text("End (HH:mm)") },
                singleLine = true, modifier = Modifier.weight(1f),
            )
        }
        val mins = runCatching {
            val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            val s0 = java.time.LocalTime.parse(startTime.trim(), fmt)
            val e0 = java.time.LocalTime.parse(endTime.trim(), fmt)
            val m = java.time.Duration.between(s0, e0).toMinutes()
            if (m < 0) m + 1440 else m
        }.getOrNull()
        if (mins != null) {
            ResultLine("Minutes", mins.toString())
            ResultLine("Hours", String.format("%.2f", mins / 60.0))
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

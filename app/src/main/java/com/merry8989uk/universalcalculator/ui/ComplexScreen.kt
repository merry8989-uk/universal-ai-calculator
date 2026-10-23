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
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Complex
import com.merry8989uk.universalcalculator.calc.Format

/** Complex-number arithmetic: combine two numbers, or inspect one. */
@Composable
fun ComplexScreen(onBack: () -> Unit) {
    var z1 by remember { mutableStateOf("3+4i") }
    var z2 by remember { mutableStateOf("1-2i") }
    var result by remember { mutableStateOf("") }

    fun run(block: () -> String) {
        result = try {
            block()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Complex numbers", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("\u2190 Back") }
        }
        Text(
            "Write them like 3+4i, -2i or 5.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(value = z1, onValueChange = { z1 = it }, label = { Text("z1") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = z2, onValueChange = { z2 = it }, label = { Text("z2") }, modifier = Modifier.fillMaxWidth())

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { run { Complex.text(Complex.parse(z1) + Complex.parse(z2)) } }) { Text("z1 + z2") }
            Button(onClick = { run { Complex.text(Complex.parse(z1) - Complex.parse(z2)) } }) { Text("z1 \u2212 z2") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { run { Complex.text(Complex.parse(z1) * Complex.parse(z2)) } }) { Text("z1 \u00D7 z2") }
            Button(onClick = { run { Complex.text(Complex.parse(z1) / Complex.parse(z2)) } }) { Text("z1 \u00F7 z2") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                run {
                    val z = Complex.parse(z1)
                    val (r, a) = Complex.polar(z, degrees = true)
                    "|z1| = ${Format.number(r)}\narg = ${Format.number(a)}\u00B0"
                }
            }) { Text("Magnitude / angle") }
            Button(onClick = { run { Complex.text(Complex.parse(z1).conj()) } }) { Text("Conjugate") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { run { Complex.text(Complex.parse(z1).pow(2)) } }) { Text("z1\u00B2") }
            Button(onClick = { run { Complex.text(Complex.parse(z1).sqrt()) } }) { Text("\u221Az1") }
        }

        Text(
            result.ifEmpty { "Result appears here" },
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

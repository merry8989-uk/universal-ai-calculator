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
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.Symbolic

/**
 * Symbolic algebra for polynomials: simplify, differentiate, integrate,
 * evaluate, factor and find roots.
 */
@Composable
fun SymbolicScreen(onBack: () -> Unit) {
    var poly by remember { mutableStateOf("x^3 - 6x^2 + 11x - 6") }
    var at by remember { mutableStateOf("2") }
    var result by remember { mutableStateOf("") }

    fun run(block: (DoubleArray) -> String) {
        result = try {
            block(Symbolic.parse(poly))
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Symbolic algebra", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("\u2190 Back") }
        }
        Text(
            "Write a polynomial in x, like x^3 - 6x^2 + 11x - 6 or 2x^2 + 3x - 5.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = poly,
            onValueChange = { poly = it },
            label = { Text("Polynomial in x") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = at,
            onValueChange = { at = it },
            label = { Text("Evaluate at x =") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { run { Symbolic.format(it) } }) { Text("Simplify") }
            Button(onClick = {
                run { "p($at) = " + Format.number(Symbolic.evaluate(it, at.toDoubleOrNull() ?: 0.0)) }
            }) { Text("Evaluate") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { run { Symbolic.format(Symbolic.derivative(it)) } }) { Text("d/dx") }
            Button(onClick = { run { Symbolic.formatWithConstant(Symbolic.integral(it)) } }) { Text("\u222B dx") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                run {
                    val r = Symbolic.roots(it)
                    if (r.isEmpty()) "No real roots"
                    else "Roots: " + r.joinToString(", ") { x -> Format.number(x) }
                }
            }) { Text("Roots") }
            Button(onClick = { run { Symbolic.factor(it) } }) { Text("Factor") }
        }
        Button(
            onClick = {
                run {
                    "degree = " + Symbolic.degree(it) + "\n" +
                        "simplified: " + Symbolic.format(it) + "\n" +
                        "derivative: " + Symbolic.format(Symbolic.derivative(it)) + "\n" +
                        "integral: " + Symbolic.formatWithConstant(Symbolic.integral(it)) + "\n" +
                        "factored: " + Symbolic.factor(it) + "\n" +
                        "roots: " + Symbolic.roots(it).joinToString(", ") { x -> Format.number(x) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show everything") }

        Text(
            result.ifEmpty { "Result appears here" },
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

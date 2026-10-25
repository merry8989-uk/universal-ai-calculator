package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
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
import com.merry8989uk.universalcalculator.calc.Calculus
import com.merry8989uk.universalcalculator.calc.Format

@Composable
fun CalculusScreen(onBack: () -> Unit) {
    var expr by remember { mutableStateOf("x^2") }
    var mode by remember { mutableStateOf("Derivative at x") }
    var a by remember { mutableStateOf("2") }
    var b by remember { mutableStateOf("5") }
    var order by remember { mutableStateOf("3") }

    val result: String = remember(expr, mode, a, b) {
        try {
            when (mode) {
                "Derivative at x" -> Format.number(Calculus.derivative(expr, a.toDoubleOrNull() ?: 0.0))
                "Second derivative at x" -> Format.number(Calculus.secondDerivative(expr, a.toDoubleOrNull() ?: 0.0))
                "Definite integral" -> Format.number(
                    Calculus.integrate(expr, a.toDoubleOrNull() ?: 0.0, b.toDoubleOrNull() ?: 1.0)
                )
                "Summation" -> Format.number(
                    Calculus.summation(expr, (a.toDoubleOrNull() ?: 0.0).toInt(), (b.toDoubleOrNull() ?: 0.0).toInt())
                )
                "Derivative of order n" -> {
                    val n = order.trim().toIntOrNull() ?: 0
                    Format.number(
                        com.merry8989uk.universalcalculator.calc.AdvancedCalculus
                            .nthDerivative(expr, a.toDoubleOrNull() ?: 0.0, n),
                    )
                }
                "Taylor series" -> {
                    val n = order.trim().toIntOrNull() ?: 3
                    val t = com.merry8989uk.universalcalculator.calc.AdvancedCalculus
                        .taylor(expr, a.toDoubleOrNull() ?: 0.0, n, b.toDoubleOrNull() ?: 1.0)
                    if (t == null) {
                        "That series could not be worked out."
                    } else {
                        "f(x) \u2248 " + t.text +
                            "\nAt x = " + Format.number(t.checkAt) +
                            ", f gives " + Format.number(t.checkActual) +
                            "  (off by " + Format.number(t.error) + ")"
                    }
                }
                "Turning points" -> {
                    val points = com.merry8989uk.universalcalculator.calc.AdvancedCalculus
                        .turningPoints(expr, a.toDoubleOrNull() ?: -5.0, b.toDoubleOrNull() ?: 5.0)
                    if (points.isEmpty()) {
                        "No turning points between those two values."
                    } else {
                        points.joinToString("\n") {
                            "${it.kind} at x = " + Format.number(it.x) +
                                ", y = " + Format.number(it.y)
                        }
                    }
                }
                else -> ""
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Calculus", style = MaterialTheme.typography.titleLarge)
        Text("Numerical calculus on a function of x.", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = expr, onValueChange = { expr = it }, label = { Text("f(x)") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        if (mode == "Derivative of order n" || mode == "Taylor series") {
            OutlinedTextField(
                value = order, onValueChange = { order = it },
                label = { Text("Order n") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "Derivative at x", "Second derivative at x", "Derivative of order n",
                "Definite integral", "Summation", "Taylor series", "Turning points",
            ).forEach { m ->
                FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m) })
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = a, onValueChange = { a = it },
                label = { Text(if (mode == "Definite integral" || mode.startsWith("Sum")) "from / at" else "at x") },
                singleLine = true, modifier = Modifier.weight(1f),
            )
            if (mode == "Definite integral" || mode.startsWith("Sum")) {
                OutlinedTextField(
                    value = b, onValueChange = { b = it }, label = { Text("to") },
                    singleLine = true, modifier = Modifier.weight(1f),
                )
            }
        }

        Text("Result", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
        Text(result, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
    }
}

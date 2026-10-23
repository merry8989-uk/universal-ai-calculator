package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.merry8989uk.universalcalculator.calc.Matrix

@Composable
fun MatrixScreen(onBack: () -> Unit) {
    var aText by remember { mutableStateOf("2 1\n1 3") }
    var bText by remember { mutableStateOf("1 0\n0 1") }
    var result by remember { mutableStateOf("") }

    fun run(block: () -> String) {
        result = try { block() } catch (e: Exception) { "Error: ${e.message}" }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Matrix calculator", style = MaterialTheme.typography.titleLarge)
        Text("Rows on separate lines, entries separated by spaces or commas.",
            style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = aText, onValueChange = { aText = it }, label = { Text("Matrix A") },
            modifier = Modifier.fillMaxWidth().height(100.dp).padding(top = 8.dp),
        )
        OutlinedTextField(
            value = bText, onValueChange = { bText = it }, label = { Text("Matrix B") },
            modifier = Modifier.fillMaxWidth().height(100.dp).padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { run { Matrix.format(Matrix.add(Matrix.parse(aText), Matrix.parse(bText))) } }) { Text("A + B") }
            Button(onClick = { run { Matrix.format(Matrix.sub(Matrix.parse(aText), Matrix.parse(bText))) } }) { Text("A \u2212 B") }
            Button(onClick = { run { Matrix.format(Matrix.mul(Matrix.parse(aText), Matrix.parse(bText))) } }) { Text("A \u00D7 B") }
            Button(onClick = { run { Matrix.format(Matrix.transpose(Matrix.parse(aText))) } }) { Text("A\u1D40") }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = {
                run {
                    val m = Matrix.parse(aText)
                    "det(A) = " + com.merry8989uk.universalcalculator.calc.Format.number(Matrix.determinant(m)) +
                        "\ntrace(A) = " + com.merry8989uk.universalcalculator.calc.Format.number(Matrix.trace(m))
                }
            }) { Text("det / trace") }
            Button(onClick = {
                run {
                    val inv = Matrix.inverse(Matrix.parse(aText))
                        ?: return@run "A is singular (no inverse)"
                    "A\u207B\u00B9 =\n" + Matrix.format(inv)
                }
            }) { Text("Inverse") }
            Button(onClick = { run { "A\u1D40 =\n" + Matrix.format(Matrix.transpose(Matrix.parse(aText))) } }) { Text("Transpose") }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = {
                run {
                    val e = Matrix.eigenvalues(Matrix.parse(aText))
                    "Eigenvalues:\n" + e.joinToString("\n") { "\u03BB = " + Complex.text(it) }
                }
            }) { Text("Eigenvalues") }
            Button(onClick = {
                run {
                    val (l, u, _) = Matrix.lu(Matrix.parse(aText))
                    "L =\n" + Matrix.format(l) + "\nU =\n" + Matrix.format(u)
                }
            }) { Text("LU") }
            Button(onClick = {
                run {
                    val c = Matrix.characteristicPolynomial(Matrix.parse(aText))
                    "char poly: " + c.mapIndexed { i, v ->
                        val p = c.size - 1 - i
                        val sign = if (v < 0) " - " else if (i == 0) "" else " + "
                        val mag = Format.number(kotlin.math.abs(v))
                        sign + (if (p == 0) mag else if (p == 1) mag + "\u03BB" else mag + "\u03BB^" + p)
                    }.joinToString("")
                }
            }) { Text("Char poly") }
            Button(onClick = {
                run {
                    val (q, r) = Matrix.qr(Matrix.parse(aText))
                    "Q =\n" + Matrix.format(q) + "\nR =\n" + Matrix.format(r)
                }
            }) { Text("QR") }
        }

        Text(
            result.ifEmpty { "Result appears here" },
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}

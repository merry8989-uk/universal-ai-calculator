package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.merry8989uk.universalcalculator.calc.Distributions
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.Statistics

private val DISTS = listOf("Normal", "Student t", "Binomial", "Poisson")

/**
 * Probability distributions, t-tests and confidence intervals.
 */
@Composable
fun DistributionsScreen(onBack: () -> Unit) {
    var dist by remember { mutableStateOf("Normal") }
    var x by remember { mutableStateOf("1.96") }
    var p1 by remember { mutableStateOf("0") }
    var p2 by remember { mutableStateOf("1") }

    var groups by remember { mutableStateOf("1, 2, 3; 4, 5, 6; 7, 8, 9") }
    var observed by remember { mutableStateOf("10, 20, 30, 40") }
    var expected by remember { mutableStateOf("25, 25, 25, 25") }
    var table by remember { mutableStateOf("10, 20; 30, 40") }
    var data by remember { mutableStateOf("1, 2, 3, 4, 5") }
    var data2 by remember { mutableStateOf("2, 3, 4, 5, 6") }
    var mu0 by remember { mutableStateOf("3") }
    var level by remember { mutableStateOf("95") }

    var result by remember { mutableStateOf("") }

    fun run(block: () -> String) {
        result = try {
            block()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun nums(text: String) = Statistics.parse(text)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Distributions & tests", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("\u2190 Back") }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DISTS.forEach { d ->
                FilterChip(selected = dist == d, onClick = { dist = d }, label = { Text(d) })
            }
        }

        OutlinedTextField(value = x, onValueChange = { x = it }, label = { Text("x (value)") }, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = p1, onValueChange = { p1 = it },
                label = { Text(if (dist == "Binomial") "n" else "mean / df / lambda") },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = p2, onValueChange = { p2 = it },
                label = { Text(if (dist == "Binomial") "p" else "std dev") },
                modifier = Modifier.weight(1f),
            )
        }

        Button(onClick = {
            run {
                val xv = x.toDoubleOrNull() ?: throw IllegalArgumentException("x must be a number")
                val a = p1.toDoubleOrNull() ?: 0.0
                val b = p2.toDoubleOrNull() ?: 0.0
                when (dist) {
                    "Normal" -> "pdf = ${Format.number(Distributions.normalPdf(xv, a, b))}\n" +
                        "cdf = ${Format.number(Distributions.normalCdf(xv, a, b))}"
                    "Student t" -> "pdf = ${Format.number(Distributions.tPdf(xv, a))}\n" +
                        "cdf = ${Format.number(Distributions.tCdf(xv, a))}"
                    "Binomial" -> "P(X = k) = ${Format.number(Distributions.binomialPmf(xv.toInt(), a.toInt(), b))}\n" +
                        "P(X \u2264 k) = ${Format.number(Distributions.binomialCdf(xv.toInt(), a.toInt(), b))}"
                    else -> "P(X = k) = ${Format.number(Distributions.poissonPmf(xv.toInt(), a))}\n" +
                        "P(X \u2264 k) = ${Format.number(Distributions.poissonCdf(xv.toInt(), a))}"
                }
            }
        }) { Text("Compute") }

        Text("Data set (for tests)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
        OutlinedTextField(value = data, onValueChange = { data = it }, label = { Text("Sample 1") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = data2, onValueChange = { data2 = it }, label = { Text("Sample 2") }, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = mu0, onValueChange = { mu0 = it }, label = { Text("H\u2080 mean") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = level, onValueChange = { level = it }, label = { Text("Confidence %") }, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                run {
                    val r = Distributions.tTestOneSample(nums(data), mu0.toDoubleOrNull() ?: 0.0)
                    r.entries.joinToString("\n") { "${it.key}: ${Format.number(it.value)}" }
                }
            }) { Text("1-sample t") }
            Button(onClick = {
                run {
                    val r = Distributions.tTestTwoSample(nums(data), nums(data2))
                    r.entries.joinToString("\n") { "${it.key}: ${Format.number(it.value)}" }
                }
            }) { Text("2-sample t") }
            Button(onClick = {
                run {
                    val r = Distributions.confidenceIntervalMean(nums(data), (level.toDoubleOrNull() ?: 95.0) / 100.0)
                    r.entries.joinToString("\n") { "${it.key}: ${Format.number(it.value)}" }
                }
            }) { Text("Confidence interval") }
        }

        Text("More tests", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(value = groups, onValueChange = { groups = it },
            label = { Text("ANOVA groups (separate groups with ;)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = observed, onValueChange = { observed = it },
            label = { Text("Observed counts") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = expected, onValueChange = { expected = it },
            label = { Text("Expected counts") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = table, onValueChange = { table = it },
            label = { Text("Contingency table (rows with ;, columns with ,)") }, modifier = Modifier.fillMaxWidth())

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                run {
                    val gs = groups.split(';').map { Statistics.parse(it) }.filter { it.isNotEmpty() }
                    val r = Distributions.oneWayAnova(gs)
                    r.entries.joinToString("\n") { "${it.key}: ${Format.number(it.value)}" }
                }
            }) { Text("ANOVA") }
            Button(onClick = {
                run {
                    val r = Distributions.chiSquareGoodnessOfFit(nums(observed), nums(expected))
                    r.entries.joinToString("\n") { "${it.key}: ${Format.number(it.value)}" }
                }
            }) { Text("Goodness of fit") }
            Button(onClick = {
                run {
                    val rows = table.split(';').map { Statistics.parse(it) }.filter { it.isNotEmpty() }
                    val r = Distributions.chiSquareIndependence(rows)
                    r.entries.joinToString("\n") { "${it.key}: ${Format.number(it.value)}" }
                }
            }) { Text("Independence") }
        }

        Text(
            result.ifEmpty { "Result appears here" },
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

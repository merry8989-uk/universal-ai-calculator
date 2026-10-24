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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.Limits
import com.merry8989uk.universalcalculator.calc.PartialFractions
import com.merry8989uk.universalcalculator.calc.Symbolic

/** Limits, worked out numerically. */
@Composable
fun LimitsScreen(onBack: () -> Unit) {
    var expression by remember { mutableStateOf("sin(x)/x") }
    var point by remember { mutableStateOf("0") }
    var side by remember { mutableStateOf(Limits.Side.BOTH) }
    var negativeInfinity by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }

    // Read once here: a plain function inside a composable cannot call
    // stringResource itself.
    val badPoint = stringResource(R.string.limits_bad_point)
    val noLimit = stringResource(R.string.limits_none)
    val divergesUp = stringResource(R.string.limits_diverge_pos)
    val divergesDown = stringResource(R.string.limits_diverge_neg)
    val equalsPrefix = stringResource(R.string.limits_equals)

    fun describe(value: Double?): String = when {
        value == null -> noLimit
        value == Double.POSITIVE_INFINITY -> divergesUp
        value == Double.NEGATIVE_INFINITY -> divergesDown
        else -> equalsPrefix + Format.number(value)
    }

    fun work() {
        val asText = point.trim()
        result = if (negativeInfinity || asText.equals("inf", ignoreCase = true) ||
            asText.equals("infinity", ignoreCase = true)
        ) {
            describe(Limits.limitAtInfinity(expression, negative = negativeInfinity))
        } else {
            val at = asText.toDoubleOrNull()
            if (at == null) badPoint else describe(Limits.limit(expression, at, side))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.limits_title), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("\u2190 " + stringResource(R.string.back)) }
        }
        Text(stringResource(R.string.limits_help), style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = expression,
            onValueChange = { expression = it },
            label = { Text(stringResource(R.string.limits_expression)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = point,
            onValueChange = { point = it; negativeInfinity = false },
            label = { Text(stringResource(R.string.limits_point)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.limits_side), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = side == Limits.Side.BOTH,
                onClick = { side = Limits.Side.BOTH; negativeInfinity = false },
                label = { Text(stringResource(R.string.limits_both)) },
            )
            FilterChip(
                selected = side == Limits.Side.LEFT,
                onClick = { side = Limits.Side.LEFT; negativeInfinity = false },
                label = { Text(stringResource(R.string.limits_left)) },
            )
            FilterChip(
                selected = side == Limits.Side.RIGHT,
                onClick = { side = Limits.Side.RIGHT; negativeInfinity = false },
                label = { Text(stringResource(R.string.limits_right)) },
            )
        }
        FilterChip(
            selected = negativeInfinity,
            onClick = { negativeInfinity = !negativeInfinity },
            label = { Text(stringResource(R.string.limits_at_infinity)) },
        )

        Button(onClick = { work() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.limits_work_out))
        }

        if (result.isNotBlank()) {
            Text(
                result,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}



/** Splitting a rational function into partial fractions. */
@Composable
fun PartialFractionsScreen(onBack: () -> Unit) {
    var numerator by remember { mutableStateOf("x + 5") }
    var denominator by remember { mutableStateOf("x^2 + 3x + 2") }
    var result by remember { mutableStateOf("") }

    fun work() {
        result = try {
            val n = Symbolic.parse(numerator)
            val d = Symbolic.parse(denominator)
            when (val outcome = PartialFractions.decompose(n, d)) {
                is PartialFractions.Outcome.NotPossible -> outcome.reason
                is PartialFractions.Outcome.Decomposed -> {
                    val polynomialPart = if (Symbolic.isZero(outcome.polynomial)) {
                        ""
                    } else {
                        Symbolic.format(outcome.polynomial) +
                            if (outcome.terms.isEmpty()) "" else " + "
                    }
                    val termsPart = PartialFractions.format(outcome.terms)
                    "= " + polynomialPart + termsPart
                }
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.partial_title), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("\u2190 " + stringResource(R.string.back)) }
        }
        Text(stringResource(R.string.partial_help), style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = numerator,
            onValueChange = { numerator = it },
            label = { Text(stringResource(R.string.partial_numerator)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = denominator,
            onValueChange = { denominator = it },
            label = { Text(stringResource(R.string.partial_denominator)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(onClick = { work() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.partial_split))
        }

        if (result.isNotBlank()) {
            Text(
                result,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

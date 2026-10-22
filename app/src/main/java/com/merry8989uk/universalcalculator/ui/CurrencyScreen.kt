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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.CurrencyRates
import com.merry8989uk.universalcalculator.calc.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SHOWN = listOf(
    "INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CNY", "AED", "SGD",
    "CHF", "HKD", "MYR", "THB", "ZAR", "SAR", "NZD", "KRW", "BRL", "RUB",
)

@Composable
fun CurrencyScreen(onBack: () -> Unit) {
    var base by remember { mutableStateOf("USD") }
    var amountText by remember { mutableStateOf("1") }
    var rates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var status by remember { mutableStateOf("Tap Refresh to fetch live rates.") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val amount = amountText.toDoubleOrNull() ?: 1.0

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Currency converter", style = MaterialTheme.typography.titleLarge)
        Text("Live rates from open.er-api.com", style = MaterialTheme.typography.bodySmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = base, onValueChange = { base = it.trim().uppercase() },
                label = { Text("Base currency") }, singleLine = true, modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = amountText, onValueChange = { amountText = it },
                label = { Text("Amount") }, singleLine = true, modifier = Modifier.weight(1f),
            )
        }

        Button(
            onClick = {
                loading = true
                status = "Loading\u2026"
                scope.launch {
                    val r = withContext(Dispatchers.IO) { CurrencyRates.fetch(base) }
                    if (r == null) {
                        status = "Could not reach the rate service. Check your connection."
                        rates = emptyMap()
                    } else {
                        rates = r
                        status = "Rates for 1 $base"
                    }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.padding(top = 8.dp),
        ) { Text(if (loading) "Loading\u2026" else "Refresh rates") }

        Text(status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))

        SHOWN.filter { it != base }.forEach { code ->
            val rate = rates[code]
            if (rate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("$code", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        Format.number(amount * rate),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

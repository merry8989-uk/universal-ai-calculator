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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.CacheAge
import com.merry8989uk.universalcalculator.calc.CurrencyRates
import com.merry8989uk.universalcalculator.data.CurrencyCache
import com.merry8989uk.universalcalculator.calc.CurrencyMath
import com.merry8989uk.universalcalculator.calc.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The ones a person is most likely to want, in this order.
 *
 * They are not the whole list any more — the rate service returns about 160 and
 * the converter used to show only these twenty, so a user looking for rupees
 * from a neighbouring country found nothing and had no way to tell whether the
 * currency was missing or the app was broken. This is the top of the list now,
 * not the list.
 */
private val COMMON = listOf(
    "INR", "USD", "EUR", "GBP", "AED", "SGD", "JPY", "CNY",
    "AUD", "CAD", "CHF", "HKD", "MYR", "THB", "SAR", "ZAR",
    "NZD", "KRW", "BRL", "RUB",
)

/** Every code in the table, the common ones first and the rest alphabetically. */
internal fun orderedCodes(rates: Map<String, Double>): List<String> {
    val common = COMMON.filter { rates.containsKey(it) }
    val rest = rates.keys.filter { it !in COMMON }.sorted()
    return common + rest
}

@Composable
fun CurrencyScreen(onBack: () -> Unit) {
    var base by remember { mutableStateOf("USD") }
    var amountText by remember { mutableStateOf("1") }
    var rates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var status by remember { mutableStateOf("Loading the saved rates\u2026") }
    var loading by remember { mutableStateOf(false) }
    var cachedAt by remember { mutableStateOf(0L) }
    var filter by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val cache = remember { CurrencyCache(context) }

    // Show the last saved table straight away, so the converter works offline.
    LaunchedEffect(Unit) {
        val saved = cache.snapshot()
        if (saved != null && rates.isEmpty()) {
            base = saved.base
            rates = saved.rates
            cachedAt = saved.timestampMillis
            val age = CacheAge.describe(saved.timestampMillis, System.currentTimeMillis())
            status = "Saved rates for 1 ${saved.base} \u00B7 $age" +
                if (CacheAge.isStale(saved.timestampMillis, System.currentTimeMillis()))
                    " (tap Refresh for today's rates)" else ""
        } else if (rates.isEmpty()) {
            status = "No saved rates yet. Tap Refresh to fetch live rates."
        }
    }

    val amount = amountText.toDoubleOrNull() ?: 1.0
    val codes = orderedCodes(rates)
    val shown = if (filter.isBlank()) {
        codes
    } else {
        val q = filter.trim().uppercase()
        codes.filter { it.contains(q) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Currency converter", style = MaterialTheme.typography.titleLarge)
        Text(
            "Live rates from open.er-api.com, saved on your device so the converter still works offline.",
            style = MaterialTheme.typography.bodySmall,
        )

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
                        if (rates.isEmpty()) {
                            // A code the service does not know fails exactly like
                            // a dead connection, so say both rather than blaming
                            // the network for a typo.
                            status = "Could not get rates for \"$base\". " +
                                "Check the currency code, and your connection."
                        } else {
                            val age = CacheAge.describe(cachedAt, System.currentTimeMillis())
                            status = "Offline \u2014 showing the saved rates for 1 $base ($age)."
                        }
                    } else {
                        rates = r
                        val now = System.currentTimeMillis()
                        cachedAt = now
                        cache.save(base, r, now)
                        status = "Live rates for 1 $base \u00B7 ${r.size} currencies"
                    }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.padding(top = 8.dp),
        ) { Text(if (loading) "Loading\u2026" else "Refresh rates") }

        Text(status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))

        if (rates.isNotEmpty()) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text("Find a currency, e.g. INR, PKR, NPR") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text(
                "${shown.count { it != base }} of ${codes.count { it != base }} currencies",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        shown.filter { it != base }.forEach { code ->
            // the conversion goes through the tested helper rather than being
            // done inline here, so a wrong rate has one place to be wrong in
            val converted = CurrencyMath.convert(amount, code, rates)
            if (converted != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("$code", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        Format.number(converted),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        if (rates.isNotEmpty() && shown.count { it != base } == 0 && filter.isNotBlank()) {
            Text(
                "No currency matches \"$filter\".",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

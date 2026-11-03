package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.window.Dialog
import com.merry8989uk.universalcalculator.calc.CacheAge
import com.merry8989uk.universalcalculator.calc.Countries
import com.merry8989uk.universalcalculator.calc.Country
import com.merry8989uk.universalcalculator.calc.CurrencyMath
import com.merry8989uk.universalcalculator.calc.CurrencyRates
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.data.CurrencyCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Every currency the rate table holds, the common ones first and the rest
 * alphabetical.
 */
internal fun orderedCodes(rates: Map<String, Double>): List<String> {
    val common = COMMON_CURRENCIES.filter { rates.containsKey(it) }
    val rest = rates.keys.filter { it !in COMMON_CURRENCIES }.sorted()
    return common + rest
}

private val COMMON_CURRENCIES = listOf(
    "INR", "USD", "EUR", "GBP", "AED", "SGD", "JPY", "CNY",
    "AUD", "CAD", "CHF", "HKD", "MYR", "THB", "SAR", "ZAR",
    "NZD", "KRW", "BRL", "RUB",
)

/**
 * The currency converter.
 *
 * The question a person actually has is "what is my money worth over there",
 * and that is a question about two countries, not two codes. So it is asked with
 * two pickers — the country you are in and the country you are looking at — and
 * the answer is the one rate between them, large. The whole table stays below,
 * because sometimes the question is "and what about everywhere else".
 */
@Composable
fun CurrencyScreen(onBack: () -> Unit) {
    var from by remember { mutableStateOf(Countries.all.first { it.code == "INR" }) }
    var to by remember { mutableStateOf(Countries.all.first { it.code == "NPR" }) }
    var amountText by remember { mutableStateOf("100") }
    var rates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var status by remember { mutableStateOf("Loading the saved rates\u2026") }
    var loading by remember { mutableStateOf(false) }
    var cachedAt by remember { mutableStateOf(0L) }
    var filter by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val cache = remember { CurrencyCache(context) }

    // Whatever was saved is shown at once, so the converter is usable offline.
    LaunchedEffect(Unit) {
        val saved = cache.snapshot()
        if (saved != null && rates.isEmpty()) {
            rates = saved.rates
            cachedAt = saved.timestampMillis
            val age = CacheAge.describe(saved.timestampMillis, System.currentTimeMillis())
            status = "Saved rates for 1 ${saved.base} \u00B7 $age" +
                if (CacheAge.isStale(saved.timestampMillis, System.currentTimeMillis()))
                    " (tap Refresh for today's rates)" else ""
        }
    }

    // Fetch for the currency you are converting from, and again whenever it
    // changes — the table is always "one unit of this currency".
    LaunchedEffect(from.code) {
        loading = true
        status = "Getting rates for 1 ${from.code}\u2026"
        val fresh = withContext(Dispatchers.IO) { CurrencyRates.fetch(from.code) }
        if (fresh == null) {
            val saved = cache.snapshot()
            status = if (saved != null && saved.base == from.code) {
                val age = CacheAge.describe(saved.timestampMillis, System.currentTimeMillis())
                "Offline \u2014 saved rates for 1 ${from.code} ($age)."
            } else {
                "Could not get rates for ${from.code}. Check the connection and tap Refresh."
            }
        } else {
            rates = fresh
            val now = System.currentTimeMillis()
            cachedAt = now
            cache.save(from.code, fresh, now)
            status = "Live rates for 1 ${from.code} \u00B7 ${fresh.size} currencies"
        }
        loading = false
    }

    val amount = amountText.toDoubleOrNull() ?: 1.0
    val pairRate = CurrencyMath.crossRate(from.code, to.code, rates)
    val converted = CurrencyMath.convertBetween(amount, from.code, to.code, rates)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Currency converter", style = MaterialTheme.typography.titleLarge)
        Text(
            "Live rates from open.er-api.com, saved on your device so the converter still works offline.",
            style = MaterialTheme.typography.bodySmall,
        )

        // ---- the two countries ----
        Text(
            "From",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        CountryButton(from, onClick = { picking = "from" })

        Text(
            "To",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        CountryButton(to, onClick = { picking = "to" })

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            OutlinedTextField(
                value = amountText, onValueChange = { amountText = it },
                label = { Text("Amount in ${from.code}") },
                singleLine = true, modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { val t = from; from = to; to = t },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Swap") }
        }

        // ---- the answer, in one place ----
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (converted != null && pairRate != null) {
                    Text(
                        "${Format.number(converted)} ${to.code}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${Format.number(amount)} ${from.code} = " +
                            "${Format.number(converted)} ${to.code}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "1 ${from.code} = ${Format.number(pairRate)} ${to.code}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        "1 ${to.code} = ${Format.number(1.0 / pairRate)} ${from.code}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        if (rates.isEmpty()) "No rates yet — tap Refresh."
                        else "No rate for ${to.code} in this table.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    status = "Loading\u2026"
                    val fresh = withContext(Dispatchers.IO) { CurrencyRates.fetch(from.code) }
                    if (fresh == null) {
                        status = "Could not get rates for ${from.code}. " +
                            "Check the connection and the currency code."
                    } else {
                        rates = fresh
                        val now = System.currentTimeMillis()
                        cachedAt = now
                        cache.save(from.code, fresh, now)
                        status = "Live rates for 1 ${from.code} \u00B7 ${fresh.size} currencies"
                    }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.padding(top = 12.dp),
        ) { Text(if (loading) "Loading\u2026" else "Refresh rates") }

        Text(status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))

        // ---- and the whole table ----
        if (rates.isNotEmpty()) {
            val codes = orderedCodes(rates)
            val shown = if (filter.isBlank()) {
                codes
            } else {
                val q = filter.trim().uppercase()
                codes.filter { it.contains(q) }
            }
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text("Find a currency, e.g. INR, PKR, NPR") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(
                "${shown.count { it != from.code }} of ${codes.count { it != from.code }} currencies",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            shown.filter { it != from.code }.forEach { code ->
                // through the tested helper, so a wrong rate has one place to be
                // wrong in rather than two
                val value = CurrencyMath.convert(amount, code, rates) ?: return@forEach
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(code, style = MaterialTheme.typography.bodyMedium)
                        val where = Countries.byCode(code)
                        Text(
                            if (where.isEmpty()) Countries.currencyName(code)
                            else where.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Text(
                        Format.number(value),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            if (shown.count { it != from.code } == 0 && filter.isNotBlank()) {
                Text(
                    "No currency matches \"$filter\".",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    if (picking.isNotEmpty()) {
        CountryPickerDialog(
            title = if (picking == "from") "The country you are in" else "The country to compare with",
            onPick = { picked ->
                if (picking == "from") from = picked else to = picked
                picking = ""
            },
            onDismiss = { picking = "" },
        )
    }
}

@Composable
private fun CountryButton(country: Country, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(country.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${country.code} \u00B7 ${Countries.currencyName(country.code)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * Picks a country, with a search box.
 *
 * A dropdown of two hundred entries is not a list anyone scrolls, so the search
 * is the way in: typing "nep" or "NPR" both find Nepal.
 */
@Composable
private fun CountryPickerDialog(
    title: String,
    onPick: (Country) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val shown = remember(query) { Countries.search(query) }
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search a country or currency") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(
                    "${shown.size} of ${Countries.all.size} countries",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp).padding(top = 8.dp)) {
                    items(shown) { c ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(c) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(c.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${c.code} \u00B7 ${Countries.currencyName(c.code)}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Cancel")
                }
            }
        }
    }
}

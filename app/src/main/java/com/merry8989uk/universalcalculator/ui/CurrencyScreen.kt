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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.merry8989uk.universalcalculator.calc.CacheAge
import com.merry8989uk.universalcalculator.calc.CachedCrypto
import com.merry8989uk.universalcalculator.calc.Countries
import com.merry8989uk.universalcalculator.calc.Crypto
import com.merry8989uk.universalcalculator.calc.CryptoRates
import com.merry8989uk.universalcalculator.calc.CurrencyRates
import com.merry8989uk.universalcalculator.calc.ExchangeMath
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.data.CachedRates
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

/** One row of the picker: a country's money, or a coin. */
private data class PickEntry(
    val code: String,
    val title: String,
    val subtitle: String,
    val isCrypto: Boolean,
)

private fun entries(query: String): List<PickEntry> {
    val countries = Countries.search(query).map {
        PickEntry(it.code, it.name, "${it.code} \u00B7 ${Countries.currencyName(it.code)}", false)
    }
    val coins = Crypto.search(query).map {
        PickEntry(it.symbol, it.name, "${it.symbol} \u00B7 crypto", true)
    }
    return countries + coins
}

/** What to call a code, whichever kind it is. */
private fun titleOf(code: String): String =
    if (Crypto.isCrypto(code)) Crypto.name(code)
    else Countries.byCode(code).firstOrNull()?.name ?: code

private fun subtitleOf(code: String): String =
    if (Crypto.isCrypto(code)) "${code.uppercase()} \u00B7 crypto"
    else {
        val where = Countries.byCode(code)
        val name = Countries.currencyName(code)
        if (where.isEmpty()) "${code.uppercase()} \u00B7 $name"
        else "${code.uppercase()} \u00B7 $name \u00B7 ${where.joinToString(", ") { it.name }}"
    }

/** What the screen can start from, and what to call it, before the network answers. */
internal data class SavedStart(
    val fiat: Map<String, Double>,
    val coins: Map<String, Double>,
    val status: String?,
)

/**
 * Reads whatever is on the device into the screen's starting state.
 *
 * Reading is not writing, so this runs whatever the master switch says: a table
 * saved before the user turned saving off is still the best answer available
 * without a connection, and throwing it away would make the switch look like it
 * had broken the converter. When there is nothing saved — a fresh install, or a
 * phone that has never been online — both maps are empty and [SavedStart.status]
 * is null, so the screen waits for the fetch rather than inventing a rate.
 */
internal fun savedStart(
    savedFiat: CachedRates?,
    savedCoins: CachedCrypto?,
    nowMillis: Long,
): SavedStart {
    if (savedFiat == null && savedCoins == null) {
        return SavedStart(emptyMap(), emptyMap(), null)
    }
    val age = CacheAge.describe(savedFiat?.timestampMillis ?: 0L, nowMillis)
    return SavedStart(
        fiat = savedFiat?.rates ?: emptyMap(),
        coins = savedCoins?.prices ?: emptyMap(),
        status = "Saved rates \u00B7 $age (tap Refresh for today's)",
    )
}

/**
 * The sentence to show when nothing may be kept, or null when rates are saved as usual.
 *
 * It is a sentence and not an error because nothing has failed: the user asked
 * for this, and the only thing they cannot see from here is why the saved rates
 * they may have seen before are not there. It is about the writing and not about
 * the fetching, because the fetch still happens and the converter still works
 * for as long as the screen is open.
 */
internal fun liveOnlyNotice(savingEnabled: Boolean): String? =
    if (savingEnabled) null
    else "Data saving is off, so these rates are live only \u2014 nothing is written to " +
        "this device, and today's rates are fetched again next time."

/**
 * The converter.
 *
 * The question a person actually has is "what is my money worth over there", and
 * that is a question about two places — or about two coins, which have no place
 * at all. So it is asked with two pickers that hold both: a country you can find
 * by its name, and a coin you can find by its symbol or its name. The answer is
 * the one rate between them, large. The whole table stays below.
 */
@Composable
fun CurrencyScreen(onBack: () -> Unit, savingEnabled: Boolean) {
    var fromCode by remember { mutableStateOf("INR") }
    var toCode by remember { mutableStateOf("NPR") }
    var amountText by remember { mutableStateOf("100") }
    var fiat by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var coins by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var status by remember { mutableStateOf("Loading the saved rates\u2026") }
    var loading by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val cache = remember { CurrencyCache(context) }

    // Whatever was saved is shown at once, so the converter is usable offline.
    LaunchedEffect(Unit) {
        val start = savedStart(cache.snapshot(), cache.cryptoSnapshot(), System.currentTimeMillis())
        if (start.fiat.isNotEmpty() && fiat.isEmpty()) fiat = start.fiat
        if (start.coins.isNotEmpty() && coins.isEmpty()) coins = start.coins
        start.status?.let { status = it }
    }

    // The currency table is always fetched against the dollar, because every
    // conversion goes through it: a coin has a dollar price, a currency has a
    // dollar price, and a pair is the ratio.
    LaunchedEffect(Unit) {
        loading = true
        status = "Getting today's rates\u2026"
        val freshFiat = withContext(Dispatchers.IO) { CurrencyRates.fetch("USD") }
        val freshCoins = withContext(Dispatchers.IO) { CryptoRates.fetch() }
        val now = System.currentTimeMillis()
        // Shown either way; written only when the master switch allows it.
        if (freshFiat != null) {
            fiat = freshFiat
            cache.save("USD", freshFiat, now, savingEnabled)
        }
        if (freshCoins != null) {
            coins = freshCoins
            cache.saveCrypto(freshCoins, now, savingEnabled)
        }
        status = when {
            freshFiat != null && freshCoins != null ->
                "Live \u00B7 ${freshFiat.size} currencies and ${freshCoins.size} coins"
            freshFiat != null -> "Live currencies \u00B7 coins could not be reached, showing the saved ones"
            freshCoins != null -> "Live coins \u00B7 currencies could not be reached, showing the saved ones"
            else -> "Offline \u2014 showing the saved rates."
        }
        loading = false
    }

    val amount = amountText.toDoubleOrNull() ?: 1.0
    val pairRate = ExchangeMath.rate(fromCode, toCode, fiat, coins)
    val converted = ExchangeMath.convert(amount, fromCode, toCode, fiat, coins)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Currency and crypto", style = MaterialTheme.typography.titleLarge)
        Text(
            if (savingEnabled) {
                "Live rates from open.er-api.com and CoinGecko, saved on your device so the converter still works offline."
            } else {
                "Live rates from open.er-api.com and CoinGecko, fetched fresh each time you open this screen."
            },
            style = MaterialTheme.typography.bodySmall,
        )
        liveOnlyNotice(savingEnabled)?.let { notice ->
            Text(
                notice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Text("From", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
        AssetButton(fromCode, onClick = { picking = "from" })

        Text("To", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
        AssetButton(toCode, onClick = { picking = "to" })

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            OutlinedTextField(
                value = amountText, onValueChange = { amountText = it },
                label = { Text("Amount in ${fromCode.uppercase()}") },
                singleLine = true,
                // Decimal, not the default text keyboard: an amount has paise,
                // and the keyboard it was getting had no decimal point at all.
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { val t = fromCode; fromCode = toCode; toCode = t },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Swap") }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (converted != null && pairRate != null && pairRate > 0.0) {
                    Copyable(
                        text = "${Format.number(converted)} ${toCode.uppercase()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${Format.number(amount)} ${fromCode.uppercase()} = " +
                            "${Format.number(converted)} ${toCode.uppercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "1 ${fromCode.uppercase()} = ${Format.number(pairRate)} ${toCode.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        "1 ${toCode.uppercase()} = ${Format.number(1.0 / pairRate)} ${fromCode.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    CopyShareRow(
                        text = "${Format.number(amount)} ${fromCode.uppercase()} = " +
                            "${Format.number(converted)} ${toCode.uppercase()}",
                        subject = "Currency conversion",
                    )
                } else {
                    Text(
                        if (fiat.isEmpty() && coins.isEmpty()) "No rates yet — tap Refresh."
                        else "No rate for ${toCode.uppercase()} yet.",
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
                    val freshFiat = withContext(Dispatchers.IO) { CurrencyRates.fetch("USD") }
                    val freshCoins = withContext(Dispatchers.IO) { CryptoRates.fetch() }
                    val now = System.currentTimeMillis()
                    if (freshFiat != null) {
                        fiat = freshFiat
                        cache.save("USD", freshFiat, now, savingEnabled)
                    }
                    if (freshCoins != null) {
                        coins = freshCoins
                        cache.saveCrypto(freshCoins, now, savingEnabled)
                    }
                    status = when {
                        freshFiat != null && freshCoins != null ->
                            "Live \u00B7 ${freshFiat.size} currencies and ${freshCoins.size} coins"
                        freshFiat != null -> "Live currencies \u00B7 coins unavailable"
                        freshCoins != null -> "Live coins \u00B7 currencies unavailable"
                        else -> "Could not reach the rate services. Check the connection."
                    }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.padding(top = 12.dp),
        ) { Text(if (loading) "Loading\u2026" else "Refresh rates") }

        Text(status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))

        // ---- the whole table, currencies and coins ----
        if (fiat.isNotEmpty() || coins.isNotEmpty()) {
            val codes = orderedCodes(fiat) + coins.keys.sorted()
            val shown = if (filter.isBlank()) {
                codes
            } else {
                val q = filter.trim().uppercase()
                codes.filter { it.contains(q) || titleOf(it).uppercase().contains(q) }
            }
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text("Find a currency or coin, e.g. INR, Nepal, BTC") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(
                "${shown.count { it != fromCode }} of ${codes.count { it != fromCode }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            shown.filter { it != fromCode }.forEach { code ->
                // through the tested helper, so a wrong rate has one place to be
                // wrong in rather than two
                val value = ExchangeMath.convert(amount, fromCode, code, fiat, coins) ?: return@forEach
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            "${code.uppercase()} \u00B7 ${titleOf(code)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            subtitleOf(code),
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
            if (shown.count { it != fromCode } == 0 && filter.isNotBlank()) {
                Text(
                    "Nothing matches \"$filter\".",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    if (picking.isNotEmpty()) {
        AssetPickerDialog(
            title = if (picking == "from") "Convert from" else "Convert to",
            onPick = { code ->
                if (picking == "from") fromCode = code else toCode = code
                picking = ""
            },
            onDismiss = { picking = "" },
        )
    }
}

@Composable
private fun AssetButton(code: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(titleOf(code), style = MaterialTheme.typography.titleMedium)
            Text(subtitleOf(code), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Picks a currency or a coin, with a search box.
 *
 * Two hundred countries and seventy coins is not a list anyone scrolls, so the
 * search is the way in: "nep", "NPR" and "nepalese" all reach Nepal, and "bit",
 * "BTC" and "bitcoin" all reach Bitcoin. The coins are marked so the list does
 * not read as one long run of places and things.
 */
@Composable
private fun AssetPickerDialog(
    title: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val shown = remember(query) { entries(query) }
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Country, currency, or coin") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(
                    "${shown.size} of ${Countries.all.size + Crypto.all.size}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp).padding(top = 8.dp)) {
                    items(shown) { e ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(e.code) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(e.title, style = MaterialTheme.typography.bodyLarge)
                            Text(e.subtitle, style = MaterialTheme.typography.labelSmall)
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

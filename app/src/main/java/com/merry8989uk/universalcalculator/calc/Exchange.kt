package com.merry8989uk.universalcalculator.calc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Prices for the coins, in US dollars.
 *
 * Kept in dollars rather than in rupees because a dollar price is the same fact
 * for every user, and every conversion goes through it: a coin is worth so many
 * dollars, a currency is worth so many dollars, and any pair is the ratio of the
 * two. That also means crypto to crypto needs no special case — BTC to ETH is
 * just two dollar prices divided.
 */
object CryptoRates {

    private const val ENDPOINT =
        "https://api.coingecko.com/api/v3/coins/markets" +
            "?vs_currency=usd&order=market_cap_desc&per_page=250&page=1&sparkline=false"

    /** Symbol (upper case) to dollars per coin, or null when the call failed. */
    suspend fun fetch(): Map<String, Double>? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", "universal-calculator")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parse(body)
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /** One row of the market list, after the JSON has been read. */
    data class Row(val symbol: String, val name: String, val priceUsd: Double)

    /**
     * Turns market rows into symbol to price.
     *
     * Kept apart from the JSON reading so it can be tested: org.json is an
     * Android stub and is not available in a unit test, so anything that goes
     * through it cannot be checked here. The rule that matters — a coin with no
     * price is skipped rather than stored as zero, because zero would make it
     * infinitely valuable in the other direction — lives in this function and is
     * tested directly.
     */
    fun toPriceMap(rows: List<Row>): Map<String, Double>? {
        val out = HashMap<String, Double>()
        for (row in rows) {
            val symbol = row.symbol.trim().uppercase()
            if (symbol.isEmpty()) continue
            if (row.priceUsd.isNaN() || row.priceUsd <= 0.0) continue
            // the first entry for a symbol wins: the list is ordered by market
            // cap, so the top one is the coin people mean
            if (!out.containsKey(symbol)) out[symbol] = row.priceUsd
        }
        return if (out.isEmpty()) null else out
    }

    /**
     * Reads the market list into symbol to price.
     *
     * Written against optJSONArray and optDouble so a shape change in the
     * response degrades to "no prices" rather than to a crash on the converter
     * screen.
     */
    fun parse(body: String): Map<String, Double>? = try {
        val array = org.json.JSONArray(body)
        val rows = ArrayList<Row>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            rows.add(
                Row(
                    symbol = o.optString("symbol", ""),
                    name = o.optString("name", ""),
                    priceUsd = o.optDouble("current_price", Double.NaN),
                ),
            )
        }
        toPriceMap(rows)
    } catch (e: Exception) {
        null
    }
}

/**
 * Converting between anything, through the dollar.
 *
 * Two tables are enough for every pair the screen offers: what one unit of each
 * currency is worth in dollars, and what one coin is worth in dollars. A pair is
 * then one division, which is why BTC to ETH and INR to NPR are the same code
 * path and the same tests.
 */
object ExchangeMath {

    /**
     * Dollars per one unit of [code].
     *
     * [fiatRates] is the table the rate service returns, always based on the
     * dollar: "1 USD = rates[code] of that currency". So one unit of a currency
     * is 1 / rates[code] dollars. A coin's price is already in dollars.
     */
    fun usdPerUnit(
        code: String,
        fiatRates: Map<String, Double>,
        cryptoPrices: Map<String, Double>,
    ): Double? {
        val upper = code.uppercase()
        val coin = cryptoPrices[upper]
        if (coin != null) return if (coin > 0.0) coin else null
        val perUsd = fiatRates[upper] ?: return null
        if (perUsd <= 0.0) return null
        return 1.0 / perUsd
    }

    /** [amount] of [from] expressed in [to], or null if either has no price. */
    fun convert(
        amount: Double,
        from: String,
        to: String,
        fiatRates: Map<String, Double>,
        cryptoPrices: Map<String, Double>,
    ): Double? {
        if (from.equals(to, ignoreCase = true)) return amount
        val a = usdPerUnit(from, fiatRates, cryptoPrices) ?: return null
        val b = usdPerUnit(to, fiatRates, cryptoPrices) ?: return null
        if (b <= 0.0) return null
        return amount * a / b
    }

    /** The rate from one unit of [from] into [to]. */
    fun rate(
        from: String,
        to: String,
        fiatRates: Map<String, Double>,
        cryptoPrices: Map<String, Double>,
    ): Double? = convert(1.0, from, to, fiatRates, cryptoPrices)
}

/** A crypto price table as the cache stores it. */
data class CachedCrypto(val prices: Map<String, Double>, val timestampMillis: Long)

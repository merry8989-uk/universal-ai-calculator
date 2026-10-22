package com.merry8989uk.universalcalculator.calc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Fetches live exchange rates from a free, key-less service. */
object CurrencyRates {

    private const val ENDPOINT = "https://open.er-api.com/v6/latest/"

    /** Returns rates keyed by currency code for 1 unit of [base], or null on failure. */
    suspend fun fetch(base: String): Map<String, Double>? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(ENDPOINT + base).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", "universal-calculator")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val rates = json.optJSONObject("rates") ?: return@withContext null
            val out = HashMap<String, Double>()
            val keys = rates.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = rates.optDouble(k, Double.NaN)
                if (!v.isNaN()) out[k] = v
            }
            out
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}

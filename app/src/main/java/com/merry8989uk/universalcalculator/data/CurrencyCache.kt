package com.merry8989uk.universalcalculator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.currencyStore: DataStore<Preferences> by preferencesDataStore(name = "currency_cache")

/** The last rate table we saw, kept on the device so the converter works offline. */
data class CachedRates(
    val base: String,
    val rates: Map<String, Double>,
    val timestampMillis: Long,
)

class CurrencyCache(private val context: Context) {

    private object Keys {
        val BASE = stringPreferencesKey("base")
        val RATES = stringPreferencesKey("rates_json")
        val TIME = longPreferencesKey("timestamp")
    }

    val flow: Flow<CachedRates?> = context.currencyStore.data.map { p ->
        val base = p[Keys.BASE] ?: return@map null
        val json = p[Keys.RATES] ?: return@map null
        val time = p[Keys.TIME] ?: 0L
        val rates = decode(json)
        if (rates.isEmpty()) null else CachedRates(base, rates, time)
    }

    suspend fun snapshot(): CachedRates? = flow.first()

    suspend fun save(base: String, rates: Map<String, Double>, timestampMillis: Long) {
        if (rates.isEmpty()) return
        context.currencyStore.edit {
            it[Keys.BASE] = base
            it[Keys.RATES] = encode(rates)
            it[Keys.TIME] = timestampMillis
        }
    }

    suspend fun clear() = context.currencyStore.edit { it.clear() }

    private fun encode(rates: Map<String, Double>): String {
        val o = JSONObject()
        rates.forEach { (k, v) -> o.put(k, v) }
        return o.toString()
    }

    private fun decode(json: String): Map<String, Double> = try {
        val o = JSONObject(json)
        val out = HashMap<String, Double>()
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = o.optDouble(k, Double.NaN)
            if (!v.isNaN()) out[k] = v
        }
        out
    } catch (e: Exception) {
        emptyMap()
    }
}

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

/**
 * Whether a fetched rate table may be written to the device, and the write itself.
 *
 * The rate cache is the one thing the app stores that the user never typed: a
 * table of prices fetched from someone else's server. That makes it the one
 * place a user who asked for nothing to be kept would not think to look, and it
 * was exactly the place that went on writing after the master switch was turned
 * off. The rule lives here rather than in the screen so that the decision is one
 * function, and so it can be checked without a phone: what is being tested is
 * whether the writer ran, not whether a DataStore happened to accept a write.
 */
object RateCacheRule {

    /** True when a fetched table may be written. The master switch decides, alone. */
    fun mayWrite(savingEnabled: Boolean): Boolean = DataPolicy.mayStore(savingEnabled)

    /**
     * Runs [write] only if storing is allowed, and says whether it ran.
     *
     * The return value is not decoration: the screen shows the user a different
     * sentence depending on it, and a silent false would leave it claiming a
     * table was saved that is not on the disk.
     */
    suspend fun write(savingEnabled: Boolean, write: suspend () -> Unit): Boolean {
        if (!mayWrite(savingEnabled)) return false
        write()
        return true
    }
}

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
        // the coin prices live in the same store but under their own keys: they
        // come from a different service and go stale at a different speed, so
        // they must not be able to overwrite the currency table
        val CRYPTO = stringPreferencesKey("crypto_json")
        val CRYPTO_TIME = longPreferencesKey("crypto_timestamp")
    }

    val flow: Flow<CachedRates?> = context.currencyStore.data.map { p ->
        val base = p[Keys.BASE] ?: return@map null
        val json = p[Keys.RATES] ?: return@map null
        val time = p[Keys.TIME] ?: 0L
        val rates = decode(json)
        if (rates.isEmpty()) null else CachedRates(base, rates, time)
    }

    suspend fun snapshot(): CachedRates? = flow.first()

    /**
     * Saves the table if the master switch allows storing, and says whether it did.
     *
     * The fetch itself is never refused — a converter with no rates is useless —
     * so what the switch governs here is only whether the answer outlives the
     * screen. With saving off the table is still shown, and forgotten when the
     * user leaves.
     */
    suspend fun save(
        base: String,
        rates: Map<String, Double>,
        timestampMillis: Long,
        savingEnabled: Boolean,
    ): Boolean {
        if (rates.isEmpty()) return false
        return RateCacheRule.write(savingEnabled) {
            context.currencyStore.edit {
                it[Keys.BASE] = base
                it[Keys.RATES] = encode(rates)
                it[Keys.TIME] = timestampMillis
            }
        }
    }

    /** The last coin prices seen, or null when there are none saved. */
    suspend fun cryptoSnapshot(): com.merry8989uk.universalcalculator.calc.CachedCrypto? {
        val p = context.currencyStore.data.first()
        val json = p[Keys.CRYPTO] ?: return null
        val prices = decode(json)
        if (prices.isEmpty()) return null
        return com.merry8989uk.universalcalculator.calc.CachedCrypto(
            prices, p[Keys.CRYPTO_TIME] ?: 0L,
        )
    }

    /**
     * The same rule for the coin prices.
     *
     * They live under their own keys but they are still a fetched table written
     * to the same file, so a guard on one and not the other would leave half the
     * leak in place.
     */
    suspend fun saveCrypto(
        prices: Map<String, Double>,
        timestampMillis: Long,
        savingEnabled: Boolean,
    ): Boolean {
        if (prices.isEmpty()) return false
        return RateCacheRule.write(savingEnabled) {
            context.currencyStore.edit {
                it[Keys.CRYPTO] = encode(prices)
                it[Keys.CRYPTO_TIME] = timestampMillis
            }
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

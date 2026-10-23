package com.merry8989uk.universalcalculator.calc

/**
 * Pure currency maths, so conversions can be tested without a network.
 * A rate table is always "how many units of X for one unit of the base".
 */
object CurrencyMath {

    /** Converts [amount] of the base currency into [code]. */
    fun convert(amount: Double, code: String, rates: Map<String, Double>): Double? {
        val rate = rates[code] ?: return null
        if (rate <= 0.0) return null
        return amount * rate
    }

    /** The rate from [from] to [to], using a table based on any common base. */
    fun crossRate(from: String, to: String, rates: Map<String, Double>): Double? {
        if (from == to) return 1.0
        val f = rates[from] ?: return null
        val t = rates[to] ?: return null
        if (f <= 0.0) return null
        return t / f
    }

    /** Converts [amount] of [from] into [to]. */
    fun convertBetween(amount: Double, from: String, to: String, rates: Map<String, Double>): Double? {
        val r = crossRate(from, to, rates) ?: return null
        return amount * r
    }
}

/** Human-readable age of a cached rate table. */
object CacheAge {

    const val STALE_AFTER_MILLIS = 24L * 60L * 60L * 1000L

    fun isStale(timestampMillis: Long, nowMillis: Long, maxAgeMillis: Long = STALE_AFTER_MILLIS): Boolean {
        if (timestampMillis <= 0L) return true
        return nowMillis - timestampMillis > maxAgeMillis
    }

    /** "just now", "5 minutes ago", "3 hours ago", "2 days ago". */
    fun describe(timestampMillis: Long, nowMillis: Long): String {
        if (timestampMillis <= 0L) return "never"
        val seconds = (nowMillis - timestampMillis) / 1000L
        if (seconds < 0) return "just now"
        return when {
            seconds < 60 -> "just now"
            seconds < 3600 -> "${seconds / 60} minute" + (if (seconds / 60 == 1L) " ago" else "s ago")
            seconds < 86400 -> "${seconds / 3600} hour" + (if (seconds / 3600 == 1L) " ago" else "s ago")
            else -> "${seconds / 86400} day" + (if (seconds / 86400 == 1L) " ago" else "s ago")
        }
    }
}

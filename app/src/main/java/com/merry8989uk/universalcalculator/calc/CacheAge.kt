package com.merry8989uk.universalcalculator.calc

/**
 * Human-readable age of a cached rate table.
 *
 * The conversion maths that used to sit above this moved to ExchangeMath when the
 * converter started working in dollars, which is the only way a coin and a
 * currency can share one code path. This stayed because the age of the saved
 * rates is a separate question from the arithmetic.
 */
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

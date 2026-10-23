package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyMathTest {

    // a table based on USD
    private val rates = mapOf(
        "USD" to 1.0,
        "INR" to 83.0,
        "EUR" to 0.92,
        "JPY" to 150.0,
    )

    @Test fun convertFromBase() {
        assertEquals(830.0, CurrencyMath.convert(10.0, "INR", rates)!!, 1e-9)
        assertEquals(0.92, CurrencyMath.convert(1.0, "EUR", rates)!!, 1e-9)
        assertNull(CurrencyMath.convert(1.0, "GBP", rates))
        assertNull(CurrencyMath.convert(1.0, "INR", mapOf("INR" to 0.0)))
    }

    @Test fun crossRates() {
        assertEquals(1.0, CurrencyMath.crossRate("USD", "USD", rates)!!, 1e-12)
        assertEquals(83.0, CurrencyMath.crossRate("USD", "INR", rates)!!, 1e-9)
        assertEquals(1.0 / 83.0, CurrencyMath.crossRate("INR", "USD", rates)!!, 1e-12)
        // 1 INR in EUR = 0.92 / 83
        assertEquals(0.92 / 83.0, CurrencyMath.crossRate("INR", "EUR", rates)!!, 1e-12)
        assertNull(CurrencyMath.crossRate("INR", "GBP", rates))
    }

    @Test fun convertBetween() {
        assertEquals(830.0, CurrencyMath.convertBetween(10.0, "USD", "INR", rates)!!, 1e-9)
        // 1000 INR in USD
        assertEquals(1000.0 / 83.0, CurrencyMath.convertBetween(1000.0, "INR", "USD", rates)!!, 1e-9)
        assertEquals(150.0 / 83.0, CurrencyMath.convertBetween(1.0, "INR", "JPY", rates)!!, 1e-9)
    }

    @Test fun cacheAge() {
        val now = 1_700_000_000_000L
        assertEquals("never", CacheAge.describe(0L, now))
        assertEquals("just now", CacheAge.describe(now - 30_000L, now))
        assertEquals("5 minutes ago", CacheAge.describe(now - 5 * 60_000L, now))
        assertEquals("1 minute ago", CacheAge.describe(now - 60_000L, now))
        assertEquals("3 hours ago", CacheAge.describe(now - 3 * 3_600_000L, now))
        assertEquals("1 hour ago", CacheAge.describe(now - 3_600_000L, now))
        assertEquals("2 days ago", CacheAge.describe(now - 2 * 86_400_000L, now))
        assertEquals("just now", CacheAge.describe(now + 5_000L, now))
    }

    @Test fun staleness() {
        val now = 1_700_000_000_000L
        assertTrue(CacheAge.isStale(0L, now))
        assertTrue(CacheAge.isStale(now - 25 * 3_600_000L, now))
        assertFalse(CacheAge.isStale(now - 3 * 3_600_000L, now))
    }
}

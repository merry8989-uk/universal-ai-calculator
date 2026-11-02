package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.CacheAge
import com.merry8989uk.universalcalculator.calc.CurrencyMath
import com.merry8989uk.universalcalculator.ui.orderedCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The currency converter, checked end to end apart from the network.
 *
 * The screen itself cannot be run here, but everything it depends on can: the
 * maths, the cache-age wording, and the list of currencies it offers. Those are
 * the parts that were wrong.
 */
class CurrencyAuditTest {

    private val rates = mapOf(
        "USD" to 1.0, "INR" to 83.0, "EUR" to 0.92, "GBP" to 0.79,
        "PKR" to 278.0, "NPR" to 133.0, "BDT" to 110.0, "LKR" to 300.0,
        "JPY" to 150.0, "ZAR" to 18.0,
    )

    @Test fun everyCurrencyInTheTableIsOffered() {
        // The converter used to show a fixed list of twenty and hide the rest,
        // so a currency that was in the table could not be found.
        val offered = orderedCodes(rates)
        assertEquals("a currency in the table is not offered", rates.size, offered.size)
        for (code in rates.keys) {
            assertTrue("$code is missing from the list", code in offered)
        }
    }

    @Test fun theCommonOnesComeFirst() {
        val offered = orderedCodes(rates)
        assertEquals("INR", offered.first())
        // and the rest are alphabetical after them
        val tail = offered.drop(4)
        assertEquals("the remainder is not alphabetical", tail.sorted(), tail)
    }

    @Test fun aCurrencyWithNoRateIsNotSilentlyDropped() {
        // convert returns null for an unknown code, and the screen used to skip
        // the row, so a missing currency looked like a missing app feature.
        assertNull(CurrencyMath.convert(100.0, "XYZ", rates))
        assertNotNull(CurrencyMath.convert(100.0, "INR", rates))
    }

    @Test fun theRegionalCurrenciesWork() {
        // the ones a user in India is most likely to look for
        assertEquals(100.0 * 278.0, CurrencyMath.convert(100.0, "PKR", rates)!!, 1e-9)
        assertEquals(100.0 * 133.0, CurrencyMath.convert(100.0, "NPR", rates)!!, 1e-9)
        assertEquals(100.0 * 110.0, CurrencyMath.convert(100.0, "BDT", rates)!!, 1e-9)
        assertEquals(100.0 * 300.0, CurrencyMath.convert(100.0, "LKR", rates)!!, 1e-9)
    }

    @Test fun aCrossRateIsTheRatioOfTheTwoRates() {
        // 1 INR in PKR is the PKR rate over the INR rate
        assertEquals(278.0 / 83.0, CurrencyMath.crossRate("INR", "PKR", rates)!!, 1e-12)
        assertEquals(1.0, CurrencyMath.crossRate("INR", "INR", rates)!!, 1e-12)
        assertNull("a code with no rate must not produce a number",
            CurrencyMath.crossRate("INR", "XYZ", rates))
    }

    @Test fun aZeroRateIsRefusedRatherThanDividedBy() {
        val broken = mapOf("USD" to 1.0, "INR" to 0.0)
        assertNull(CurrencyMath.convert(10.0, "INR", broken))
        assertNull(CurrencyMath.crossRate("INR", "USD", broken))
    }

    @Test fun theAgeOfTheSavedRatesIsReadable() {
        val now = 1_700_000_000_000L
        assertEquals("never", CacheAge.describe(0L, now))
        assertEquals("just now", CacheAge.describe(now - 10_000L, now))
        assertEquals("2 hours ago", CacheAge.describe(now - 2 * 3_600_000L, now))
        assertTrue("a day-old table should be called stale",
            CacheAge.isStale(now - 25 * 3_600_000L, now))
    }

    @Test fun theConverterIsReachableFromTheToolsList() {
        // the screen existing is not the same as the user being able to open it
        val source = listOf(
            java.io.File("app/src/main/java/com/merry8989uk/universalcalculator/ui/FormulasScreen.kt"),
            java.io.File("src/main/java/com/merry8989uk/universalcalculator/ui/FormulasScreen.kt"),
        ).firstOrNull { it.exists() } ?: return
        val text = source.readText()
        assertTrue("there is no currency entry in the tools list",
            text.contains("Tool(\"currency\""))
        assertTrue("the currency tool is not routed to its screen",
            text.contains("CurrencyScreen"))
    }
}

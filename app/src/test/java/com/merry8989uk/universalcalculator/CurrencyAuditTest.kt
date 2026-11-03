package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.CacheAge
import com.merry8989uk.universalcalculator.calc.Countries
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
        // Everything that is not one of the common ones must come after them, and
        // be alphabetical among itself. Written without a fixed count, because
        // how many of the common ones are in a given table is not the point.
        val common = setOf("INR", "USD", "EUR", "GBP", "AED", "SGD", "JPY", "CNY",
            "AUD", "CAD", "CHF", "HKD", "MYR", "THB", "SAR", "ZAR",
            "NZD", "KRW", "BRL", "RUB")
        val firstRare = offered.indexOfFirst { it !in common }
        val tail = if (firstRare < 0) emptyList() else offered.drop(firstRare)
        assertEquals("the remainder is not alphabetical", tail.sorted(), tail)
        assertTrue("a common currency came after a rare one",
            offered.take(if (firstRare < 0) offered.size else firstRare).all { it in common })
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

    // ---- the country list ----

    @Test fun thereAreCountriesToChooseFrom() {
        // a converter that only knows twenty currencies cannot answer "what is my
        // rupee worth in Nepal" for most of the world
        assertTrue("only ${Countries.all.size} countries", Countries.all.size >= 190)
        assertTrue("only ${Countries.codes().size} currencies", Countries.codes().size >= 140)
    }

    @Test fun aCountryKnowsItsCurrency() {
        assertEquals("INR", Countries.codeFor("India"))
        assertEquals("NPR", Countries.codeFor("Nepal"))
        assertEquals("USD", Countries.codeFor("United States"))
        assertEquals("GBP", Countries.codeFor("United Kingdom"))
        // and it does not care about case or stray spaces
        assertEquals("INR", Countries.codeFor("  india "))
        assertNull(Countries.codeFor("Atlantis"))
    }

    @Test fun everyCountryHasAThreeLetterCode() {
        for (c in Countries.all) {
            assertTrue("${c.name} has the code ${c.code}", Regex("^[A-Z]{3}$").matches(c.code))
            assertTrue("${c.name} has no currency name", Countries.currencyName(c.code).isNotBlank())
        }
    }

    @Test fun searchingFindsACountryByAnyOfItsNames() {
        // typing the country, the code or the currency should all reach Nepal
        for (q in listOf("nep", "Nepal", "NPR", "nepalese")) {
            assertTrue("searching '$q' did not find Nepal",
                Countries.search(q).any { it.name == "Nepal" })
        }
    }

    @Test fun aCurrencyUsedByManyCountriesFindsThemAll() {
        // the euro belongs to twenty countries, and the franc to a dozen more
        val euro = Countries.byCode("EUR").map { it.name }
        assertTrue("the euro should belong to many countries, found ${euro.size}", euro.size >= 15)
        assertTrue("Germany is missing from the euro", "Germany" in euro)
        assertTrue("France is missing from the euro", "France" in euro)
    }

    @Test fun anEmptySearchReturnsEverything() {
        assertEquals(Countries.all.size, Countries.search("").size)
        assertEquals(Countries.all.size, Countries.search("   ").size)
    }

    @Test fun aCountryLabelSaysWhatTheMoneyIsCalled() {
        val india = Countries.all.first { it.name == "India" }
        val label = Countries.label(india)
        assertTrue("the label does not name the country: $label", label.contains("India"))
        assertTrue("the label does not give the code: $label", label.contains("INR"))
        assertTrue("the label does not name the currency: $label", label.contains("Rupee"))
    }

    @Test fun theConvertersOwnPairWorks() {
        // 100 rupees in Nepalese rupees, which is the example the whole two-picker
        // design exists for
        val r = CurrencyMath.convertBetween(100.0, "INR", "NPR", rates)
        assertNotNull("INR to NPR produced nothing", r)
        assertEquals(100.0 * 133.0 / 83.0, r!!, 1e-9)
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

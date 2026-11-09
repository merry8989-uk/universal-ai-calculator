package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.CacheAge
import com.merry8989uk.universalcalculator.calc.Countries
import com.merry8989uk.universalcalculator.calc.Crypto
import com.merry8989uk.universalcalculator.calc.CryptoRates
import com.merry8989uk.universalcalculator.calc.ExchangeMath
import com.merry8989uk.universalcalculator.ui.orderedCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    /** The rate table as the screen fetches it: always against the dollar. */
    private val usdFiat = mapOf("USD" to 1.0, "INR" to 83.0, "NPR" to 133.0, "EUR" to 0.92)
    private val usdCoins = mapOf("BTC" to 67000.0, "ETH" to 3300.0)

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
        assertNull(ExchangeMath.convert(100.0, "USD", "XYZ", rates, emptyMap()))
        assertNotNull(ExchangeMath.convert(100.0, "USD", "INR", rates, emptyMap()))
    }

    @Test fun theRegionalCurrenciesWork() {
        // the ones a user in India is most likely to look for
        assertEquals(100.0 * 278.0, ExchangeMath.convert(100.0, "USD", "PKR", rates, emptyMap())!!, 1e-9)
        assertEquals(100.0 * 133.0, ExchangeMath.convert(100.0, "USD", "NPR", rates, emptyMap())!!, 1e-9)
        assertEquals(100.0 * 110.0, ExchangeMath.convert(100.0, "USD", "BDT", rates, emptyMap())!!, 1e-9)
        assertEquals(100.0 * 300.0, ExchangeMath.convert(100.0, "USD", "LKR", rates, emptyMap())!!, 1e-9)
    }

    @Test fun aCrossRateIsTheRatioOfTheTwoRates() {
        // 1 INR in PKR is the PKR rate over the INR rate
        assertEquals(278.0 / 83.0, ExchangeMath.rate("INR", "PKR", rates, emptyMap())!!, 1e-12)
        assertEquals(1.0, ExchangeMath.rate("INR", "INR", rates, emptyMap())!!, 1e-12)
        assertNull("a code with no rate must not produce a number",
            ExchangeMath.rate("INR", "XYZ", rates, emptyMap()))
    }

    @Test fun aZeroRateIsRefusedRatherThanDividedBy() {
        val broken = mapOf("USD" to 1.0, "INR" to 0.0)
        assertNull(ExchangeMath.convert(10.0, "USD", "INR", broken, emptyMap()))
        assertNull(ExchangeMath.usdPerUnit("INR", broken, emptyMap()))
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
        val r = ExchangeMath.convert(100.0, "INR", "NPR", rates, emptyMap())
        assertNotNull("INR to NPR produced nothing", r)
        assertEquals(100.0 * 133.0 / 83.0, r!!, 1e-9)
    }

    // ---- crypto ----

    @Test fun thereAreCoinsToChooseFrom() {
        assertTrue("only ${Crypto.all.size} coins", Crypto.all.size >= 60)
        assertEquals("Bitcoin", Crypto.name("BTC"))
        assertEquals("Ethereum", Crypto.name("ETH"))
        assertTrue(Crypto.isCrypto("btc"))
        assertFalse(Crypto.isCrypto("INR"))
    }

    @Test fun aCoinIsFoundByItsSymbolOrItsName() {
        // a trader knows the symbol and a beginner knows the name; both must work
        for (q in listOf("BTC", "btc", "Bitcoin", "bitco", "BITCOIN")) {
            assertTrue("searching '$q' did not find Bitcoin",
                Crypto.search(q).any { it.symbol == "BTC" })
        }
        assertTrue("searching 'ethereum' did not find ETH",
            Crypto.search("ethereum").any { it.symbol == "ETH" })
    }

    @Test fun everyCoinHasASymbolAndAName() {
        for (c in Crypto.all) {
            assertTrue("${c.symbol} has no name", c.name.isNotBlank())
            assertTrue("${c.symbol} is not a symbol", Regex("^[A-Z0-9]{2,10}$").matches(c.symbol))
        }
        assertEquals("a symbol appears twice", Crypto.all.size, Crypto.all.map { it.symbol }.distinct().size)
    }

    @Test fun aCoinIsConvertedThroughTheDollar() {
        // the whole point of the dollar price: every pair is one division
        assertEquals(67000.0 * 83.0,
            ExchangeMath.convert(1.0, "BTC", "INR", usdFiat, usdCoins)!!, 1e-6)
        assertEquals(100.0 / 83.0 / 67000.0,
            ExchangeMath.convert(100.0, "INR", "BTC", usdFiat, usdCoins)!!, 1e-15)
    }

    @Test fun oneCoinIsWorthAnother() {
        // BTC to ETH, which is the question crypto has and currencies do not
        assertEquals(67000.0 / 3300.0,
            ExchangeMath.convert(1.0, "BTC", "ETH", usdFiat, usdCoins)!!, 1e-9)
        assertEquals(3300.0 / 67000.0,
            ExchangeMath.convert(1.0, "ETH", "BTC", usdFiat, usdCoins)!!, 1e-12)
        // and it works from the other direction too
        assertEquals(2.0 * 3300.0 / 67000.0,
            ExchangeMath.convert(2.0, "ETH", "BTC", usdFiat, usdCoins)!!, 1e-12)
    }

    @Test fun aCurrencyIsWorthADollarDividedByItsRate() {
        // the table is "1 USD = rate of that currency"
        assertEquals(1.0 / 83.0, ExchangeMath.usdPerUnit("INR", usdFiat, usdCoins)!!, 1e-15)
        assertEquals(1.0, ExchangeMath.usdPerUnit("USD", usdFiat, usdCoins)!!, 1e-15)
        assertEquals(67000.0, ExchangeMath.usdPerUnit("BTC", usdFiat, usdCoins)!!, 1e-9)
        assertNull(ExchangeMath.usdPerUnit("XYZ", usdFiat, usdCoins))
    }

    @Test fun convertingACodeToItselfChangesNothing() {
        assertEquals(5.0, ExchangeMath.convert(5.0, "BTC", "BTC", usdFiat, usdCoins)!!, 1e-12)
        assertEquals(5.0, ExchangeMath.convert(5.0, "INR", "inr", usdFiat, usdCoins)!!, 1e-12)
    }

    @Test fun aCoinWithNoPriceDoesNotBecomeFree() {
        // a zero price would make the coin infinitely valuable the other way round
        val noBtc = mapOf("ETH" to 3300.0)
        assertNull(ExchangeMath.convert(1.0, "BTC", "INR", usdFiat, noBtc))
        val zeroBtc = mapOf("BTC" to 0.0)
        assertNull(ExchangeMath.usdPerUnit("BTC", usdFiat, zeroBtc))
    }

    @Test fun aCoinWithNoPriceIsSkippedRatherThanStoredAsZero() {
        // The rule, tested without the JSON layer: org.json is an Android stub and
        // is not available in a unit test, so the reading is kept separate from it.
        val rows = listOf(
            CryptoRates.Row("btc", "Bitcoin", 67000.5),
            CryptoRates.Row("eth", "Ethereum", 3300.25),
            CryptoRates.Row("bad", "No Price", Double.NaN),
            CryptoRates.Row("zero", "Zero", 0.0),
            CryptoRates.Row("btc", "Bitcoin again", 1.0),
        )
        val prices = CryptoRates.toPriceMap(rows)!!
        assertEquals(67000.5, prices["BTC"]!!, 1e-9)
        assertEquals(3300.25, prices["ETH"]!!, 1e-9)
        assertFalse("a coin with no price was stored", prices.containsKey("BAD"))
        assertFalse("a zero price was stored", prices.containsKey("ZERO"))
        // the first entry wins, so a duplicate does not overwrite the real one
        assertEquals(67000.5, prices["BTC"]!!, 1e-9)
    }

    @Test fun anEmptyFeedGivesNothingRatherThanAnEmptyTable() {
        // an empty map would read as "no coin has a price", which is a different
        // thing from "the call failed", and the screen says them differently
        assertNull(CryptoRates.toPriceMap(emptyList()))
        assertNull(CryptoRates.toPriceMap(listOf(CryptoRates.Row("x", "X", 0.0))))
    }

    @Test fun theConverterIsReachableFromTheToolsList() {
        // the screen existing is not the same as the user being able to open it
        // The tools live beside the search rule now, so both files are read: the
        // question is whether the converter is reachable, not which file names it.
        val source = listOf(
            java.io.File("app/src/main/java/com/merry8989uk/universalcalculator/ui/FormulasScreen.kt"),
            java.io.File("src/main/java/com/merry8989uk/universalcalculator/ui/FormulasScreen.kt"),
        ).firstOrNull { it.exists() } ?: return
        val hub = listOf(
            java.io.File("app/src/main/java/com/merry8989uk/universalcalculator/calc/HubSearch.kt"),
            java.io.File("src/main/java/com/merry8989uk/universalcalculator/calc/HubSearch.kt"),
        ).firstOrNull { it.exists() }
        val text = source.readText() + (hub?.readText() ?: "")
        assertTrue("there is no currency entry in the tools list",
            text.contains("Tool(\"currency\""))
        assertTrue("the currency tool is not routed to its screen",
            text.contains("CurrencyScreen"))
    }
}

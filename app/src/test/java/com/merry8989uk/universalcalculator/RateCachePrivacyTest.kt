package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.CachedCrypto
import com.merry8989uk.universalcalculator.calc.ExchangeMath
import com.merry8989uk.universalcalculator.data.CachedRates
import com.merry8989uk.universalcalculator.data.DataPolicy
import com.merry8989uk.universalcalculator.data.RateCacheRule
import com.merry8989uk.universalcalculator.ui.liveOnlyNotice
import com.merry8989uk.universalcalculator.ui.savedStart
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A fetched rate table is not written to the device while saving is off.
 *
 * The master switch was honoured by the archive, the history and the diagnostics
 * log, and not by the rate cache: the converter went on writing every table it
 * fetched, so a user who had asked for nothing to be kept still had prices on
 * their disk. What is tested here is that the writer does not run at all, not
 * that a DataStore accepted a write and then had it undone — and that the screen
 * says so in a sentence rather than an error, because the user asked for this
 * and nothing has gone wrong.
 */
class RateCachePrivacyTest {

    // ---- the rule ----

    @Test fun withDataSavingOffNothingReachesTheDisk() {
        var writes = 0
        val written = runBlocking {
            RateCacheRule.write(savingEnabled = false) { writes++ }
        }
        assertFalse("the cache was written with the master switch off", written)
        assertEquals("a fetched rate was persisted with saving off", 0, writes)
    }

    @Test fun withDataSavingOnTheFetchedRateIsWritten() {
        var written: Map<String, Double>? = null
        val ok = runBlocking {
            RateCacheRule.write(savingEnabled = true) { written = mapOf("INR" to 83.0) }
        }
        assertTrue("the cache was not written with the master switch on", ok)
        assertEquals(mapOf("INR" to 83.0), written)
    }

    @Test fun theRuleIsTheMasterSwitchAndNotAFourthSettingOfItsOwn() {
        // the point of one switch is that a user who wants nothing kept does not
        // have to find every store; a cache with its own idea of "allowed" would
        // be the same bug again with a different name
        assertEquals(DataPolicy.mayStore(false), RateCacheRule.mayWrite(false))
        assertEquals(DataPolicy.mayStore(true), RateCacheRule.mayWrite(true))
        assertFalse(RateCacheRule.mayWrite(savingEnabled = false))
        assertTrue(RateCacheRule.mayWrite(savingEnabled = true))
    }

    // ---- the write sites ----

    private fun source(path: String): String? =
        listOf(File("app/src/main/java/com/merry8989uk/universalcalculator/$path"),
               File("src/main/java/com/merry8989uk/universalcalculator/$path"))
            .firstOrNull { it.exists() }?.readText()

    @Test fun everyWriteToTheRateCacheCarriesTheMasterSwitch() {
        val text = source("ui/CurrencyScreen.kt") ?: return
        val writes = Regex("""cache\.(save|saveCrypto)\([^)]*\)""").findAll(text).toList()
        assertTrue("the cache is not written from the screen at all", writes.size >= 2)
        for (w in writes) {
            assertTrue(
                "a fetched rate is written without asking the master switch: ${w.value}",
                w.value.contains("savingEnabled"),
            )
        }
    }

    @Test fun theCacheItselfAsksThePolicyRatherThanTrustingItsCaller() {
        val text = source("data/CurrencyCache.kt") ?: return
        assertTrue(
            "the cache does not consult the master switch",
            text.contains("DataPolicy.mayStore(savingEnabled)"),
        )
        assertTrue(
            "the write runs whatever the switch says",
            text.contains("if (!mayWrite(savingEnabled)) return false"),
        )
    }

    // ---- what the screen reads when there is nothing saved ----

    private val now = 1_700_000_000_000L
    private val hour = 60L * 60L * 1000L

    @Test fun withNothingSavedTheScreenStartsEmptyAndSaysNothing() {
        val start = savedStart(savedFiat = null, savedCoins = null, nowMillis = now)
        assertTrue("a rate appeared from an empty cache", start.fiat.isEmpty())
        assertTrue("a coin price appeared from an empty cache", start.coins.isEmpty())
        // no "Saved rates" line, so the screen is free to say it is fetching:
        // an empty cache is a phone that has never been online, or one whose
        // user has just turned saving off, and neither is an error
        assertNull("an empty cache is described as saved rates", start.status)
    }

    @Test fun aTableSavedBeforeTheSwitchWasTurnedOffIsStillReadable() {
        // reading is not writing: turning saving off must not make the converter
        // unusable offline with what it already has
        val start = savedStart(
            savedFiat = CachedRates("USD", mapOf("USD" to 1.0, "INR" to 83.0), now - 2 * hour),
            savedCoins = null,
            nowMillis = now,
        )
        assertEquals(83.0, start.fiat["INR"])
        assertTrue(start.coins.isEmpty())
        assertNotNull("the age of the saved table is not shown", start.status)
        assertTrue("the saved line does not say it is saved", start.status!!.contains("Saved rates"))
        assertTrue("the age is wrong", start.status!!.contains("2 hours ago"))
    }

    @Test fun theCoinsCanBeSavedWithoutTheCurrencies() {
        val start = savedStart(
            savedFiat = null,
            savedCoins = CachedCrypto(mapOf("BTC" to 67_000.0), now - hour),
            nowMillis = now,
        )
        assertTrue(start.fiat.isEmpty())
        assertEquals(67_000.0, start.coins["BTC"])
        assertNotNull("a saved coin price is not described", start.status)
    }

    @Test fun theConverterStillWorksWhileOnlineWithNoSavedTable() {
        // the change is about what is written, not about refusing to fetch: with
        // saving off and nothing on disk, a live table converts as it always did
        val live = mapOf("USD" to 1.0, "INR" to 83.0, "NPR" to 133.0)
        val nothingSaved = savedStart(null, null, now)
        assertTrue(nothingSaved.fiat.isEmpty())
        val converted = ExchangeMath.convert(100.0, "INR", "NPR", live, emptyMap())
        assertNotNull("the converter refused a live rate because nothing was cached", converted)
        assertEquals(100.0 * (1.0 / 83.0) / (1.0 / 133.0), converted!!, 1e-9)
    }

    // ---- what the screen says ----

    @Test fun theLiveOnlySentenceIsShownOnlyWhileSavingIsOff() {
        assertNull("the sentence is shown while rates are being saved", liveOnlyNotice(true))
        val notice = liveOnlyNotice(savingEnabled = false)
        assertNotNull("nothing tells the user the rates are live only", notice)
        assertTrue("the sentence does not say the rates are live only",
            notice!!.contains("live only"))
        assertTrue("the sentence does not say nothing is written",
            notice.contains("nothing is written"))
    }

    @Test fun theSentenceIsNotAnError() {
        // nothing failed here: the user asked for it, so the line must not read
        // like a fault ("could not", "failed", "error") or blame the network
        val notice = liveOnlyNotice(savingEnabled = false)!!
        for (word in listOf("error", "failed", "could not", "unable", "offline")) {
            assertFalse("the sentence reads as a fault: \"$word\"", notice.contains(word))
        }
    }
}

package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.data.CalcPrefs
import com.merry8989uk.universalcalculator.data.CalcPrefsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two personal lists in the hub.
 *
 * Three hundred and fifty calculators is a library; the six somebody uses every
 * day are the ones they should not have to hunt for. These are the rules that
 * make the shortcuts behave: starring twice un-stars, opening twice does not
 * duplicate, and the list stays short.
 */
class CalcPrefsTest {

    @Test fun aRoundTripKeepsBothListsInOrder() {
        val prefs = CalcPrefs(
            favourites = listOf("emi", "bmi", "cagr"),
            recents = listOf("bmi", "emi"),
        )
        val back = CalcPrefsStore.parse(CalcPrefsStore.serialize(prefs))
        assertEquals(prefs.favourites, back.favourites)
        assertEquals(prefs.recents, back.recents)
    }

    @Test fun starringTwiceTakesTheStarOff() {
        val one = CalcPrefsStore.toggleFavourite(CalcPrefs(), "emi")
        assertEquals(listOf("emi"), one.favourites)
        val two = CalcPrefsStore.toggleFavourite(one, "emi")
        assertTrue("the star did not come off", two.favourites.isEmpty())
    }

    @Test fun openingACalculatorPutsItAtTheFrontOnce() {
        var prefs = CalcPrefsStore.recordRecent(CalcPrefs(), "emi")
        prefs = CalcPrefsStore.recordRecent(prefs, "bmi")
        assertEquals(listOf("bmi", "emi"), prefs.recents)

        // opening the same one again moves it, it does not add a second copy
        prefs = CalcPrefsStore.recordRecent(prefs, "emi")
        assertEquals(listOf("emi", "bmi"), prefs.recents)
    }

    @Test fun theRecentListStaysShort() {
        var prefs = CalcPrefs()
        repeat(CalcPrefsStore.MAX_RECENTS + 5) { i ->
            prefs = CalcPrefsStore.recordRecent(prefs, "calc$i")
        }
        assertEquals(CalcPrefsStore.MAX_RECENTS, prefs.recents.size)
        // the newest is at the front and the oldest ones fell off the end
        assertEquals("calc16", prefs.recents.first())
        assertTrue("the oldest entry is still there", "calc0" !in prefs.recents)
    }

    @Test fun anUnreadableFileIsEmptyRatherThanACrash() {
        assertEquals(CalcPrefs(), CalcPrefsStore.parse(""))
        assertEquals(CalcPrefs(), CalcPrefsStore.parse("not our file\nfavourite|emi"))
        assertEquals(CalcPrefs(), CalcPrefsStore.parse("NEXUSPREFS1\nrubbish\nfavourite|\n|emi"))
    }

    @Test fun theTwoListsDoNotMix() {
        val prefs = CalcPrefsStore.parse("NEXUSPREFS1\nrecent|emi\nfavourite|bmi\n")
        assertEquals(listOf("bmi"), prefs.favourites)
        assertEquals(listOf("emi"), prefs.recents)
    }
}

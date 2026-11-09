package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.data.HistoryRetention
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Using a past result again.
 *
 * "Use this again" put the line back on the board and lost the answer on the way,
 * because it read the number back out of the text the answer had been displayed
 * as. That text is written the way the user asked to see it — `19,000` under
 * Indian grouping, `19.000` under European — and neither is something a number
 * parser will accept, so the exact answer arrived as nothing. The sum then had to
 * be worked out again, which is precisely what the button exists to avoid.
 *
 * The fix carries the number beside the line instead of reading it back out of
 * the text. What is pinned here is that the number survives the trip: whatever
 * the number format made the text look like, the same number comes back out — and
 * that a line whose answer was not a plain number comes back with nothing rather
 * than with a guess.
 */
class HistoryReuseTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60L * 60L * 1000L

    /** A stored line: "9500*2 = 19,000" as displayed, with 19000 carried behind it. */
    private fun stored(format: String, at: Long = now): String =
        HistoryRetention.stored("9500*2 = " + Format.grouped("19000", format, -1), at, 19000.0)

    // ---------------- the number the button needs ----------------

    @Test fun aGroupedResultComesBackAsItsNumberUnderEveryFormat() {
        // the three ways the app can group a number: 19,000 / 19,000 / 19.000
        for (format in listOf("Indian", "International", "European")) {
            assertEquals(format, 19000.0, HistoryRetention.valueOf(stored(format), now)!!, 0.0)
        }
    }

    @Test fun theTextIsStillTheAnswerTheUserRead() {
        // carrying the number must not change what the line looks like
        assertEquals("9500*2 = 19,000", HistoryRetention.text(stored("Indian"), now))
        assertEquals("9500*2 = 19.000", HistoryRetention.text(stored("European"), now))
    }

    @Test fun anUngroupedNumberComesBackUnchanged() {
        // with no separators at all the text is already a number, and it must
        // still come back as the one that was carried
        val line = stored("None")
        assertEquals("9500*2 = 19000", HistoryRetention.text(line, now))
        assertEquals(19000.0, HistoryRetention.valueOf(line, now)!!, 0.0)
    }

    @Test fun aNumberWithPaiseComesBackToThePaise() {
        val line = HistoryRetention.stored("rent/3 = 8,333.33", now, 25000.0 / 3.0)
        assertEquals(25000.0 / 3.0, HistoryRetention.valueOf(line, now)!!, 0.0)
    }

    @Test fun aResultThatIsNotANumberIsRefusedRatherThanGuessed() {
        // a unit, a complex value, an infinity: none of them has an exact number
        // to carry, and inventing one would put a wrong answer on the board
        assertNull(HistoryRetention.valueOf(HistoryRetention.stored("5 km + 3 km = 8 km", now), now))
        assertNull(HistoryRetention.valueOf(HistoryRetention.stored("3+4i = 7+4i", now), now))
        assertNull(
            HistoryRetention.valueOf(
                HistoryRetention.stored("1/0 = Infinity", now, Double.POSITIVE_INFINITY),
                now,
            ),
        )
    }

    @Test fun aLineWorkedOutBeforeTheNumberWasCarriedHasNone() {
        // lines already on the phone were written without a number. They read
        // back as no number rather than as a misread one, and their text is
        // untouched; the three-day rule clears them out on its own.
        val old = "$now|9500*2 = 19,000"
        assertNull(HistoryRetention.valueOf(old, now))
        assertEquals("9500*2 = 19,000", HistoryRetention.text(old, now))
    }

    @Test fun aDamagedNumberCostsTheNumberAndNotTheLine() {
        val damaged = "$now#not-a-number|9500*2 = 19,000"
        assertNull(HistoryRetention.valueOf(damaged, now))
        assertEquals("9500*2 = 19,000", HistoryRetention.text(damaged, now))
    }

    // ---------------- and it survives the rules from #31 ----------------

    @Test fun keepingALineDoesNotLoseTheNumber() {
        val kept = HistoryRetention.withPin(stored("Indian"), now)
        assertEquals(19000.0, HistoryRetention.valueOf(kept, now)!!, 0.0)
        assertEquals(now, HistoryRetention.parse(kept, now).at)
    }

    @Test fun lettingALineAgeAgainDoesNotLoseItEither() {
        val pinned = HistoryRetention.withPin(stored("European"), now)
        val released = HistoryRetention.withPin(pinned, now, pinned = false)
        assertEquals(19000.0, HistoryRetention.valueOf(released, now)!!, 0.0)
    }

    @Test fun theNumberSurvivesTheThreeDayRule() {
        // a pass re-stamps every line it keeps, and must carry the number with it
        val kept = HistoryRetention.keep(listOf(stored("Indian", now - 1 * day)), now)
        assertEquals(1, kept.size)
        assertEquals(19000.0, HistoryRetention.valueOf(kept[0], now)!!, 0.0)
    }

    @Test fun undoingAPurgeBringsTheNumberBackWithTheLine() {
        // Undo restores what the pass took, so the number has to be in the line
        // the pass handed back or the restored line is half a line
        val taken = HistoryRetention.pass(listOf(stored("Indian", now - 10 * day)), now).removed
        assertEquals(1, taken.size)
        val restored = taken.map { HistoryRetention.withPin(it, now) }
        assertEquals(19000.0, HistoryRetention.valueOf(restored[0], now)!!, 0.0)
        assertEquals("9500*2 = 19,000", HistoryRetention.text(restored[0], now))
    }
}

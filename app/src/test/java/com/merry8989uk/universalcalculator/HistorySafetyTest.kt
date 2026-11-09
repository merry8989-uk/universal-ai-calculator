package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.data.ArchiveWrite
import com.merry8989uk.universalcalculator.data.HistoryRetention
import com.merry8989uk.universalcalculator.data.attemptArchiveWrite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The tape must not be able to lose a line without saying so.
 *
 * Three things could take one before this: the AC key, the three-day rule on
 * every read, and an archive write that failed and was thrown away. Each of them
 * was silent, and each of them is pinned here — the first two as rules that can
 * be run, the last two as the source text of the screen, because a key handler
 * and a swallowed exception are not things a unit test can press.
 *
 * The two source checks are blunt on purpose. They are the same kind of check
 * [AuditTest] already makes, and they are the only way to hold a Compose key
 * branch still without a device.
 */
class HistorySafetyTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60L * 60L * 1000L
    private val old = now - 10 * day

    private fun main(path: String): String {
        val file = listOf(File("src/main/$path"), File("app/src/main/$path"))
            .firstOrNull { it.exists() }
            ?: error("could not find $path from ${File(".").absolutePath}")
        return file.readText()
    }

    // ---------------- the three-day rule, and the lines it may not take ----------------

    @Test fun aPinnedLineOlderThanThreeDaysSurvives() {
        val kept = HistoryRetention.keep(listOf(HistoryRetention.withPin("$old|9500*2 = 19,000", now)), now)
        assertEquals(1, kept.size)
        assertEquals("9500*2 = 19,000", HistoryRetention.text(kept[0], now))
    }

    @Test fun anUnpinnedLineOlderThanThreeDaysIsRemovedAndCounted() {
        val pass = HistoryRetention.pass(listOf("$old|4+4 = 8"), now)
        assertTrue(pass.kept.isEmpty())
        assertEquals(1, pass.removed.size)
    }

    @Test fun aPurgeReportsTheLinesItTookAndNotTheOnesItLeft() {
        val pass = HistoryRetention.pass(
            listOf("$old|gone", "$now|here", HistoryRetention.withPin("$old|kept", now)),
            now,
        )
        assertEquals(2, pass.kept.size)
        assertEquals(1, pass.removed.size)
        assertEquals("gone", HistoryRetention.text(pass.removed[0], now))
    }

    @Test fun aPassThatTakesNothingIsNotWorthAnnouncing() {
        // the tape only speaks up when something actually went
        assertTrue(HistoryRetention.pass(listOf("$now|1+1 = 2"), now).removed.isEmpty())
        assertTrue(HistoryRetention.pass(emptyList(), now).removed.isEmpty())
    }

    @Test fun undoingAPurgeKeepsTheLineForGood() {
        // Undo puts the line back pinned, because it is already past its third
        // day: restored as it was, the next pass would take it again and the
        // button would have been a lie.
        val taken = HistoryRetention.pass(listOf("$old|7*7 = 49"), now).removed
        val restored = taken.map { HistoryRetention.withPin(it, now) }
        val later = HistoryRetention.pass(restored, now + 30 * day)
        assertEquals(1, later.kept.size)
        assertTrue(later.removed.isEmpty())
        assertEquals("7*7 = 49", HistoryRetention.text(later.kept[0], now))
    }

    @Test fun theMomentALineWasWorkedOutSurvivesPinning() {
        // the pin rides in the stamp, so the line still knows when it happened
        val pinned = HistoryRetention.withPin("$old|2+2 = 4", now)
        assertEquals(old, HistoryRetention.parse(pinned, now).at)
        assertTrue(HistoryRetention.isPinned(pinned, now))
    }

    @Test fun pinningLeavesTheTextExactlyAsTheUserWroteIt() {
        val pinned = HistoryRetention.withPin("$now|250*12 = 3,000", now)
        assertEquals("250*12 = 3,000", HistoryRetention.text(pinned, now))
    }

    @Test fun unpinningLetsTheLineAgeAgain() {
        val released = HistoryRetention.withPin(HistoryRetention.withPin("$old|1+1 = 2", now), now, pinned = false)
        assertFalse(HistoryRetention.isPinned(released, now))
        assertTrue(HistoryRetention.pass(listOf(released), now).removed.isNotEmpty())
    }

    @Test fun aLineWrittenBeforePinsExistedStillReads() {
        // no migration: an old file has no pin mark, and no pin mark means not
        // pinned, which is what every line meant when it was written
        val oldStyle = "$old|9*9 = 81"
        assertFalse(HistoryRetention.isPinned(oldStyle, now))
        assertEquals("9*9 = 81", HistoryRetention.text(oldStyle, now))
        assertEquals(old, HistoryRetention.parse(oldStyle, now).at)
    }

    @Test fun aPinnedLineIsNeverDroppedToMakeRoom() {
        // the cap is a guard against a runaway file, not a reason to lose the one
        // line the user asked to keep
        val pinned = HistoryRetention.withPin("$now|keep me", now)
        val lines = listOf(pinned) + (1..HistoryRetention.MAX_ENTRIES).map { "$now|entry $it" }
        val kept = HistoryRetention.keep(lines, now)
        assertEquals(HistoryRetention.MAX_ENTRIES, kept.size)
        assertTrue(kept.any { HistoryRetention.text(it, now) == "keep me" })
    }

    // ---------------- the AC key ----------------

    @Test fun theAllClearKeyLeavesTheTapeAlone() {
        val screen = main("java/com/merry8989uk/universalcalculator/ui/CalculatorScreen.kt")
        val ac = screen.substringAfter("\"AC\" ->").substringBefore("\"del\" ->")

        assertTrue("AC still clears the entry it sits beside", ac.contains("expr = TextFieldValue(\"\")"))
        assertFalse("AC must never touch the history", ac.contains("historyVm.clear"))
    }

    @Test fun clearingTheTapeIsBehindAConfirmation() {
        val tape = main("java/com/merry8989uk/universalcalculator/ui/HistoryTape.kt")
        assertTrue("the tape asks before clearing", tape.contains("history_clear_confirm_title"))
        assertTrue("the Clear button opens the question", tape.contains("onClick = { confirming = true }"))
        assertFalse("Clear must not wipe straight away", tape.contains("TextButton(onClick = onClear)"))
    }

    // ---------------- the archive write ----------------

    @Test fun aFailedArchiveWriteIsReportedRatherThanSwallowed() {
        val outcome = attemptArchiveWrite { false }
        assertTrue("a write that did not happen must not look like one", outcome is ArchiveWrite.Failed)
    }

    @Test fun anArchiveWriteThatThrowsIsReportedToo() {
        val outcome = attemptArchiveWrite { throw java.io.IOException("no space left") }
        assertTrue(outcome is ArchiveWrite.Failed)
        assertEquals("no space left", (outcome as ArchiveWrite.Failed).reason)
    }

    @Test fun aWriteThatHappensIsNotCalledAFailure() {
        assertTrue(attemptArchiveWrite { true } is ArchiveWrite.Written)
    }

    @Test fun theCalculatorScreenSaysSoWhenTheArchiveRefusesALine() {
        val screen = main("java/com/merry8989uk/universalcalculator/ui/CalculatorScreen.kt")
        assertTrue("the screen checks what the archive said", screen.contains("is ArchiveWrite.Failed"))
        assertTrue("and shows the user a sentence", screen.contains("R.string.archive_write_failed"))
    }
}

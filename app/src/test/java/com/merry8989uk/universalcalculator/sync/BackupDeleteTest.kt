package com.merry8989uk.universalcalculator.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two things this change had to get right, both of them testable without a
 * socket: the body the delete builds, and what an upload actually carries.
 *
 * The delete is checked by reading the JSON back. That is worth doing because
 * the server's only guard is a boolean inside this body — a delete that arrives
 * without `confirm` is refused, and one that arrives with the string "true"
 * instead of the value is refused as well — so the body *is* the guard rather
 * than a note about one, and it should be inspected as such.
 *
 * The upload is checked through [BackupContents], which is the one place the
 * decision lives. The bug being fixed was a sentence, not a condition: the
 * switch said "history" while the archive travelled whatever it said. A test
 * that only read the label would have passed before this change and would pass
 * after it, so these tests read the rule and the words together and hold each
 * of them to the other.
 */
class BackupDeleteTest {

    // ---- the request the delete builds ----

    @Test fun theDeleteNamesTheActionAndTheAccountItIsFor() {
        val body = SyncRequest.delete("amit.kumar", "ab".repeat(32))
        assertEquals("delete", body.getString("sync"))
        // the id is sent as the account store holds it, already folded at sign-in
        assertEquals("amit.kumar", body.getString("id"))
        assertEquals("ab".repeat(32), body.getString("verifier"))
    }

    @Test fun theDeleteCarriesTheConfirmationAsAValue() {
        // the worker compares with `body?.confirm !== true`, so the string "true"
        // would be refused: this has to be a JSON boolean, not a word
        val body = SyncRequest.delete("amit", "ab".repeat(32))
        assertTrue("confirm should be a boolean", body.get("confirm") is Boolean)
        assertEquals(true, body.get("confirm"))
    }

    @Test fun theDeleteSendsNothingTheServerWouldHaveToTrust() {
        // exactly four fields, no slot and no version: the slot is derived from
        // the id on the server, so a client cannot aim a delete at somebody else
        val body = SyncRequest.delete("amit", "ab".repeat(32))
        assertEquals(setOf("sync", "id", "verifier", "confirm"), body.keySet())
        assertFalse(body.has("slot"))
        assertFalse(body.has("version"))
        assertFalse(body.has("blob"))
    }

    @Test fun theDeleteSendsTheVerifierAndNeverThePassword() {
        // the verifier is what the server already holds; the password stays on the
        // phone, and nothing about it may appear in a body that leaves
        val body = SyncRequest.delete("amit", "ab".repeat(32)).toString()
        assertFalse(body.contains("correct-horse"))
        assertTrue(body.contains("\"confirm\":true"))
    }

    // ---- what an upload contains ----

    @Test fun withTheSwitchOnTheArchiveAndTheHistoryBothTravel() {
        assertTrue(BackupContents.archiveIncluded(includeCalculations = true, archiveKeptOnPhone = true))
        assertTrue(BackupContents.historyIncluded(includeCalculations = true))
    }

    @Test fun withTheSwitchOffTheArchiveDoesNotTravel() {
        // this is the bug in one line: the archive used to go out whatever the
        // switch said, and the archive is the largest thing in a backup
        assertFalse(BackupContents.archiveIncluded(includeCalculations = false, archiveKeptOnPhone = true))
        assertFalse(BackupContents.historyIncluded(includeCalculations = false))
    }

    @Test fun aPhoneThatKeepsNoArchiveSendsNoneEvenWithTheSwitchOn() {
        // the master saving switch wins: there is nothing to send, and an empty
        // archive would only replace a good copy on the server with nothing
        assertFalse(BackupContents.archiveIncluded(includeCalculations = true, archiveKeptOnPhone = false))
    }

    @Test fun theSwitchSpeaksForEverythingThatTravels() {
        val title = BackupContents.SWITCH_TITLE.lowercase()
        val detail = BackupContents.SWITCH_DETAIL.lowercase()
        // one switch for the user's own working, so the words can be true of it
        assertTrue(title.contains("calculation"))
        assertTrue(detail.contains("archive"))
        assertTrue(detail.contains("history"))
    }

    @Test fun theSwitchNoLongerNamesOnePartWhileTheOtherGoesAnyway() {
        // the old label said "history" and left the archive unmentioned and going
        val title = BackupContents.SWITCH_TITLE.lowercase()
        assertFalse(title.contains("history"))
        assertFalse(title == "include the history")
    }

    @Test fun theSwitchSaysWhatIsSentWhenItIsOff() {
        // off is a real choice, so the label has to say what a backup is then:
        // settings and the user's own calculators, and nothing of their working
        val detail = BackupContents.SWITCH_DETAIL.lowercase()
        assertTrue(detail.contains("settings"))
        assertTrue(detail.contains("calculators"))
        assertTrue(detail.contains("off"))
    }
}

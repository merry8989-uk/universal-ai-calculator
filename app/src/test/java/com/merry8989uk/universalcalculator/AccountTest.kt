package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.sync.Account
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules an account follows, and the two secrets derived from it.
 *
 * The important property is that the verifier the server holds and the key the
 * data is encrypted with are not the same thing: a copy of the server's table
 * must not open a single calculation.
 */
class AccountTest {

    // ---- the id ----

    @Test fun aReasonableIdIsAccepted() {
        for (id in listOf("amit", "amit.kumar", "amit_89", "a-b-c", "user1234")) {
            assertNull("$id should be allowed", Account.problemWithId(id))
        }
    }

    @Test fun anIdIsFoldedToLowerCase() {
        assertEquals("amit", Account.normalise("  Amit "))
        // so these are the same account, not two
        assertEquals(Account.normalise("AMIT"), Account.normalise("amit"))
    }

    @Test fun aShortIdIsRefused() {
        assertTrue(Account.problemWithId("ab")!!.contains("3"))
    }

    @Test fun aLongIdIsRefused() {
        assertTrue(Account.problemWithId("a".repeat(25))!!.contains("24"))
    }

    @Test fun anIdWithStrangeCharactersIsRefused() {
        for (bad in listOf("amit kumar", "amit@home", "amit/1", "अमित", "amit#")) {
            assertTrue("$bad should be refused", Account.problemWithId(bad) != null)
        }
    }

    @Test fun anIdCannotStartWithPunctuation() {
        assertTrue(Account.problemWithId(".amit") != null)
        assertTrue(Account.problemWithId("-amit") != null)
    }

    @Test fun anEmptyIdIsRefused() {
        assertTrue(Account.problemWithId("") != null)
        assertTrue(Account.problemWithId("   ") != null)
    }

    // ---- the password ----

    @Test fun aShortPasswordIsRefused() {
        assertTrue(Account.problemWithPassword("short")!!.contains("8"))
        assertNull(Account.problemWithPassword("longenough"))
    }

    @Test fun theConfirmationHasToMatch() {
        assertNull(Account.problemWithConfirmation("correct-horse", "correct-horse"))
        assertTrue(Account.problemWithConfirmation("correct-horse", "correct-horsE") != null)
    }

    // ---- the two secrets ----

    @Test fun theVerifierIsStableForTheSameIdAndPassword() {
        val a = Account.verifier("amit", "correct-horse")
        val b = Account.verifier("amit", "correct-horse")
        assertEquals(a, b)
        assertEquals(64, a.length)
        assertTrue("expected hex", a.all { it in "0123456789abcdef" })
    }

    @Test fun theVerifierIsNotThePassword() {
        val v = Account.verifier("amit", "correct-horse")
        assertTrue(!v.contains("correct"))
        assertTrue(!v.contains("horse"))
    }

    @Test fun aDifferentPasswordGivesADifferentVerifier() {
        assertNotEquals(
            Account.verifier("amit", "correct-horse"),
            Account.verifier("amit", "correct-horsE"),
        )
    }

    @Test fun theSamePasswordUnderTwoIdsGivesTwoVerifiers() {
        // so a stolen verifier tells you nothing about another account
        assertNotEquals(
            Account.verifier("amit", "same-password"),
            Account.verifier("ravi", "same-password"),
        )
    }

    @Test fun theVerifierIsNotTheEncryptionKey() {
        // the whole design rests on this: the server holds the verifier, and the
        // data is encrypted with something else entirely
        val verifier = Account.verifier("amit", "correct-horse")
        val passphrase = Account.encryptionPassphrase("amit", "correct-horse")
        assertNotEquals(verifier, passphrase)
        assertTrue(!passphrase.contains(verifier))
        assertTrue(!verifier.contains(passphrase))
    }

    @Test fun theEncryptionPassphraseFollowsTheIdAndPassword() {
        assertEquals(
            Account.encryptionPassphrase("Amit", "correct-horse"),
            Account.encryptionPassphrase("amit", "correct-horse"),
        )
        assertNotEquals(
            Account.encryptionPassphrase("amit", "correct-horse"),
            Account.encryptionPassphrase("amit", "correct-horsE"),
        )
    }
}

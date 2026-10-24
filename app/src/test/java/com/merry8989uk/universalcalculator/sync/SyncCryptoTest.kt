package com.merry8989uk.universalcalculator.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The end-to-end encryption that keeps sync private. */
class SyncCryptoTest {

    private val code = "correct horse battery staple"

    @Test fun aRoundTripGivesBackExactlyWhatWentIn() {
        val plain = "NEXUSSYNC1\nsome payload\nwith several lines"
        val packed = SyncCrypto.encrypt(plain, code)
        assertEquals(plain, SyncCrypto.decrypt(packed, code))
    }

    @Test fun hindiSurvivesTheRoundTrip() {
        val plain = "मेरे कैलकुलेटर, गुणा, वर्गमूल"
        assertEquals(plain, SyncCrypto.decrypt(SyncCrypto.encrypt(plain, code), code))
    }

    @Test fun anEmptyPayloadSurvives() {
        assertEquals("", SyncCrypto.decrypt(SyncCrypto.encrypt("", code), code))
    }

    @Test fun aWrongCodeOpensNothing() {
        val packed = SyncCrypto.encrypt("secret", code)
        // The authentication tag fails, so this is null rather than rubbish.
        assertNull(SyncCrypto.decrypt(packed, "not the right code"))
    }

    @Test fun aTamperedBlobOpensNothing() {
        val packed = SyncCrypto.encrypt("secret", code)
        val raw = java.util.Base64.getDecoder().decode(packed)
        raw[raw.size - 1] = (raw[raw.size - 1] + 1).toByte()
        val damaged = java.util.Base64.getEncoder().encodeToString(raw)
        assertNull(SyncCrypto.decrypt(damaged, code))
    }

    @Test fun rubbishOpensNothing() {
        assertNull(SyncCrypto.decrypt("not base64 at all!!", code))
        assertNull(SyncCrypto.decrypt("", code))
    }

    @Test fun theSameCodeEncryptsTwiceDifferently() {
        // A fresh IV and salt each time, so identical payloads do not look alike.
        val a = SyncCrypto.encrypt("same text", code)
        val b = SyncCrypto.encrypt("same text", code)
        assertNotEquals(a, b)
        // and both still open
        assertEquals("same text", SyncCrypto.decrypt(a, code))
        assertEquals("same text", SyncCrypto.decrypt(b, code))
    }

    @Test fun theSaltTravelsWithTheBlob() {
        // Two devices with the same code must both be able to open it, which
        // only works because the salt is inside the packed bytes.
        val packed = SyncCrypto.encrypt("payload", code, SyncCrypto.newSalt())
        assertEquals("payload", SyncCrypto.decrypt(packed, code))
    }

    @Test fun theSlotIdIsStableAndHex() {
        val id = SyncCrypto.slotId(code)
        assertEquals(id, SyncCrypto.slotId(code))
        assertEquals(64, id.length)
        assertTrue("expected hex, got $id", id.all { it in "0123456789abcdef" })
    }

    @Test fun differentCodesGetDifferentSlots() {
        assertNotEquals(SyncCrypto.slotId("code one"), SyncCrypto.slotId("code two"))
    }

    @Test fun theSlotIdDoesNotGiveTheCodeAway() {
        val id = SyncCrypto.slotId(code)
        assertFalse(id.contains("horse"))
        assertNotEquals(code, id)
    }

    @Test fun aTinyChangeOfCodeChangesTheSlot() {
        assertNotEquals(SyncCrypto.slotId("abcdefghijkl"), SyncCrypto.slotId("abcdefghijkm"))
    }
}

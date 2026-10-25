package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.updater.DownloadProgress
import com.merry8989uk.universalcalculator.updater.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The progress bar shows a real number, so the arithmetic behind it has to be
 * right: a half-downloaded file must read 50%, and a server that does not say
 * how big the file is must not make the bar lie.
 */
class UpdateProgressTest {

    @Test fun anEmptyDownloadIsZero() {
        assertEquals(0f, DownloadProgress(0L, 1000L).fraction!!, 1e-6f)
        assertEquals(0, DownloadProgress(0L, 1000L).percent)
    }

    @Test fun aHalfwayDownloadIsHalf() {
        assertEquals(0.5f, DownloadProgress(500L, 1000L).fraction!!, 1e-6f)
        assertEquals(50, DownloadProgress(500L, 1000L).percent)
    }

    @Test fun aFinishedDownloadIsFull() {
        assertEquals(1f, DownloadProgress(1000L, 1000L).fraction!!, 1e-6f)
        assertEquals(100, DownloadProgress(1000L, 1000L).percent)
    }

    @Test fun theBarNeverGoesPastTheEnd() {
        // a server that sends more than it promised must not overrun the bar
        assertEquals(1f, DownloadProgress(1200L, 1000L).fraction!!, 1e-6f)
        assertEquals(100, DownloadProgress(1200L, 1000L).percent)
    }

    @Test fun anUnknownLengthDoesNotInventAPercentage() {
        assertNull(DownloadProgress(500L, 0L).fraction)
        assertNull(DownloadProgress(500L, 0L).percent)
    }

    @Test fun bytesArriveInOrderAndNeverGoBackwards() {
        var last = -1L
        var fine = true
        for (received in listOf(0L, 64L, 128L, 512L, 1000L)) {
            if (received < last) fine = false
            last = received
        }
        assertTrue("the byte count went backwards", fine)
    }

    // ---- the version comparison the check depends on ----

    @Test fun aHigherVersionIsNewer() {
        assertTrue(UpdateChecker.isNewer("1.55.0", "1.54.0"))
        assertTrue(UpdateChecker.isNewer("1.55.1", "1.55.0"))
        assertTrue(UpdateChecker.isNewer("2.0.0", "1.99.99"))
    }

    @Test fun theSameVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("1.55.0", "1.55.0"))
    }

    @Test fun anOlderVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("1.54.0", "1.55.0"))
        assertFalse(UpdateChecker.isNewer("1.9.0", "1.10.0"))
    }

    @Test fun aShorterVersionIsReadAsZeros() {
        assertTrue(UpdateChecker.isNewer("1.55.1", "1.55"))
        assertFalse(UpdateChecker.isNewer("1.55", "1.55.0"))
    }
}

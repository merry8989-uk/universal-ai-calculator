package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.updater.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which file a phone is offered.
 *
 * A release carries two APKs - the phone one and the watch one - and the checker
 * used to take the first one it found. That is a coin toss that installs a watch
 * app over a phone, so the pick is a function of the names, and this holds it.
 */
class UpdateCheckerTest {

    @Test fun thePhoneBuildIsChosenEvenWhenTheWatchBuildComesFirst() {
        val names = listOf(
            "nexus-calc-wear-2.53.0.apk",
            "nexus-calc-phone-2.53.0.apk",
            "nexus-calc-phone-2.53.0.aab",
        )
        assertEquals("nexus-calc-phone-2.53.0.apk", UpdateChecker.pickApk(names))
    }

    @Test fun theWatchBuildIsNeverChosen() {
        assertEquals(null, UpdateChecker.pickApk(listOf("nexus-calc-wear-2.53.0.apk")))
        assertNull(UpdateChecker.pickApk(emptyList()))
        assertNull(UpdateChecker.pickApk(listOf("notes.txt", "checksums.txt")))
    }

    @Test fun anOlderReleaseWithoutADeviceNameStillWorks() {
        // releases published before the names changed carry app-release.apk and
        // wear-release.apk; the phone one has to stay installable
        assertEquals("app-release.apk", UpdateChecker.pickApk(listOf("app-release.aab", "app-release.apk")))
        assertEquals(
            "app-release.apk",
            UpdateChecker.pickApk(listOf("wear-release.apk", "app-release.apk")),
        )
    }

    @Test fun aVersionIsOnlyAnUpdateWhenItIsHigher() {
        assertTrue(UpdateChecker.isNewer("2.53.1", "2.53.0"))
        assertTrue(UpdateChecker.isNewer("2.54", "2.53.9"))
        assertFalse(UpdateChecker.isNewer("2.53.0", "2.53.0"))
        assertFalse(UpdateChecker.isNewer("2.52.9", "2.53.0"))
    }
}

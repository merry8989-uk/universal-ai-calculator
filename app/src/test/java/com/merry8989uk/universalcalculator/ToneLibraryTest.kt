package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.sound.ToneLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tones on offer, and the rule for a file the user brings.
 *
 * The rule matters because a key tone is played on every press: a three-minute
 * song loaded into the sound pool would make the calculator stutter, and the
 * user would have no idea why.
 */
class ToneLibraryTest {

    // ---- what is offered ----

    @Test fun thereAreNineBuiltInTones() {
        assertEquals(9, ToneLibrary.BUILT_IN.size)
    }

    @Test fun offComesFirstAndTheCustomOneLast() {
        assertEquals(ToneLibrary.OFF, ToneLibrary.CHOICES.first())
        assertEquals(ToneLibrary.CUSTOM, ToneLibrary.CHOICES.last())
    }

    @Test fun noToneIsOfferedTwice() {
        assertEquals(ToneLibrary.CHOICES.size, ToneLibrary.CHOICES.toSet().size)
    }

    @Test fun theOriginalThreeAreStillThere() {
        for (tone in listOf("Click", "Bubble", "Pop")) {
            assertTrue("$tone was removed", tone in ToneLibrary.BUILT_IN)
        }
    }

    @Test fun onlyTheCustomChoiceNeedsAFile() {
        assertTrue(ToneLibrary.isCustom(ToneLibrary.CUSTOM))
        for (tone in ToneLibrary.BUILT_IN) {
            assertTrue("$tone should not need a file", !ToneLibrary.isCustom(tone))
        }
        assertTrue(!ToneLibrary.isCustom(ToneLibrary.OFF))
    }

    // ---- what a file has to be ----

    @Test fun aShortWavIsFine() {
        assertNull(ToneLibrary.problemWith("click.wav", 40_000, "wav"))
    }

    @Test fun everyCommonAudioTypeIsAccepted() {
        for (ext in listOf("wav", "mp3", "ogg", "m4a", "aac", "opus", "flac")) {
            assertNull("$ext should be allowed", ToneLibrary.problemWith("tone.$ext", 50_000, ext))
        }
    }

    @Test fun aPictureIsRefused() {
        val problem = ToneLibrary.problemWith("photo.jpg", 50_000, "jpg")
        assertTrue("a jpg should be refused: $problem", problem != null)
    }

    @Test fun aDocumentIsRefused() {
        assertTrue(ToneLibrary.problemWith("notes.pdf", 50_000, "pdf") != null)
    }

    @Test fun anEmptyFileIsRefused() {
        assertTrue(ToneLibrary.problemWith("tone.wav", 0, "wav") != null)
    }

    @Test fun aWholeSongIsRefused() {
        // three minutes of stereo mp3 is a few megabytes, not a key tone
        val problem = ToneLibrary.problemWith("song.mp3", 6_000_000, "mp3")
        assertTrue("a large file should be refused: $problem", problem != null)
    }

    @Test fun theSizeLimitIsGenerousEnoughForARealTone() {
        assertNull(ToneLibrary.problemWith("tone.wav", 1_900_000, "wav"))
        assertTrue(ToneLibrary.problemWith("tone.wav", 2_100_000, "wav") != null)
    }

    @Test fun theExtensionIsTakenFromTheNameWhenItIsNotGiven() {
        assertNull(ToneLibrary.problemWith("my-tone.ogg", 10_000, null))
        assertTrue(ToneLibrary.problemWith("my-tone.txt", 10_000, null) != null)
    }

    @Test fun aNamelessFileIsRefused() {
        assertTrue(ToneLibrary.problemWith(null, 10_000, "wav") != null)
        assertTrue(ToneLibrary.problemWith("   ", 10_000, "wav") != null)
    }

    @Test fun theExtensionIsNotCaseSensitive() {
        assertNull(ToneLibrary.problemWith("TONE.WAV", 10_000, "WAV"))
    }
}

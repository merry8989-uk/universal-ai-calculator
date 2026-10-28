package com.merry8989uk.universalcalculator

import androidx.compose.ui.graphics.Color
import com.merry8989uk.universalcalculator.ui.theme.CustomTheme
import com.merry8989uk.universalcalculator.ui.theme.Palette
import com.merry8989uk.universalcalculator.ui.theme.ThemeCodec
import com.merry8989uk.universalcalculator.ui.theme.ThemeFromImage
import com.merry8989uk.universalcalculator.ui.theme.ThemeStore
import com.merry8989uk.universalcalculator.ui.theme.blend
import com.merry8989uk.universalcalculator.ui.theme.readableOn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Themes the user builds: keeping them, reading colours out of a photo, and the
 * contrast rule that keeps the app readable whatever they pick.
 */
class CustomThemeTest {

    private fun sample(id: String = "t1", name: String = "Ocean") = CustomTheme(
        id = id,
        name = name,
        background = Color(0xFF0E1018),
        surface = Color(0xFF1A1D2B),
        primary = Color(0xFF4EA8FF),
        secondary = Color(0xFFFF7AB6),
    )

    // ---- keeping them ----

    @Test fun aThemeSurvivesTheRoundTrip() {
        val text = ThemeCodec.encode(listOf(sample()))
        val back = ThemeCodec.decode(text)
        assertEquals(1, back.size)
        assertEquals("Ocean", back[0].name)
        assertEquals(Color(0xFF4EA8FF), back[0].primary)
        assertEquals(Color(0xFF1A1D2B), back[0].surface)
        assertEquals(Color(0xFFFF7AB6), back[0].secondary)
    }

    /**
     * Every colour comes back exactly as it went in.
     *
     * This is the test that caught a real bug: `shl` and `or` are both infix and
     * share a precedence, so `a shl 24 or r shl 16 or g shl 8 or b` groups left
     * to right and rotates the bytes. Blue became alpha, and every saved theme
     * was quietly the wrong colour.
     */
    @Test fun coloursSurviveTheRoundTripExactly() {
        val spread = listOf(
            Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFF4EA8FF),
            Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF),
            Color(0x80FF8800), Color(0x00123456), Color(0xFF123456),
            Color(0xFF0E1018), Color(0xFF1A1D2B), Color(0xFFF3F7FA),
        )
        for (colour in spread) {
            val theme = sample().copy(primary = colour, background = colour)
            val back = ThemeCodec.decode(ThemeCodec.encode(listOf(theme))).single()
            assertEquals("$colour changed on the way through", colour, back.primary)
            assertEquals("$colour changed on the way through", colour, back.background)
        }
    }

    @Test fun theAlphaIsKept() {
        val translucent = Color(0x40FF8800)
        val back = ThemeCodec.decode(
            ThemeCodec.encode(listOf(sample().copy(primary = translucent))),
        ).single()
        assertEquals(translucent.alpha, back.primary.alpha, 1e-3f)
    }

    @Test fun everySwatchSurvivesTheRoundTrip() {
        for (colour in Palette.ALL) {
            val back = ThemeCodec.decode(
                ThemeCodec.encode(listOf(sample().copy(primary = colour))),
            ).single()
            assertEquals("$colour changed on the way through", colour, back.primary)
        }
    }

    @Test fun severalThemesSurvive() {
        val many = (1..5).map { sample("t$it", "Theme $it") }
        assertEquals(5, ThemeCodec.decode(ThemeCodec.encode(many)).size)
    }

    @Test fun aBarInTheNameCannotBreakTheFile() {
        // the fields are bar-separated, so a name with a bar has to be escaped
        val awkward = sample(name = "red | blue")
        val back = ThemeCodec.decode(ThemeCodec.encode(listOf(awkward)))
        assertEquals(1, back.size)
        assertEquals("red | blue", back[0].name)
    }

    @Test fun aBarInTheImagePathCannotBreakTheFile() {
        val withImage = sample().copy(imagePath = "/data/a|b/theme.jpg")
        val back = ThemeCodec.decode(ThemeCodec.encode(listOf(withImage)))
        assertEquals("/data/a|b/theme.jpg", back[0].imagePath)
    }

    @Test fun aHalfWrittenLineIsSkippedAndTheRestSurvives() {
        val good = ThemeCodec.encode(listOf(sample()))
        val broken = good + "t9|Half a theme\n"
        val back = ThemeCodec.decode(broken)
        assertEquals(1, back.size)
        assertEquals("Ocean", back[0].name)
    }

    @Test fun nonsenseIsSkippedRatherThanCrashing() {
        assertEquals(0, ThemeCodec.decode("NEXUSTHEME1\nnot|enough\n").size)
        assertEquals(0, ThemeCodec.decode("").size)
        assertEquals(0, ThemeCodec.decode("NEXUSTHEME1").size)
    }

    @Test fun aThemeWithNoIdIsNotLoaded() {
        assertEquals(0, ThemeCodec.decode("NEXUSTHEME1\n|Name|1|2|3|4|\n").size)
    }

    // ---- reading a photo ----

    @Test fun theAccentIsTheVividColourNotTheCommonOne() {
        // a red flower on a grey wall: mostly grey, but the theme should be red
        val pixels = IntArray(200) { 0xFF808080.toInt() } + IntArray(20) { 0xFFE01B1B.toInt() }
        val (accent, _) = ThemeFromImage.pick(pixels)
        assertTrue("the accent should be the red, not the grey", accent.red > accent.green + 0.3f)
    }

    @Test fun theSecondAccentIsADifferentHue() {
        val pixels = IntArray(50) { 0xFFE01B1B.toInt() } + IntArray(50) { 0xFF1B6BE0.toInt() }
        val (first, second) = ThemeFromImage.pick(pixels)
        assertNotEquals("the two accents should not be the same", first, second)
    }

    @Test fun aGreyPictureStillProducesATheme() {
        val (accent, second) = ThemeFromImage.pick(IntArray(100) { 0xFF777777.toInt() })
        assertTrue("an accent is still produced", accent.alpha > 0f)
        assertTrue("a second accent is still produced", second.alpha > 0f)
    }

    @Test fun anEmptyPictureFallsBackToTheStartingTheme() {
        val (accent, _) = ThemeFromImage.pick(IntArray(0))
        assertEquals(CustomTheme.START.primary, accent)
    }

    @Test fun theCanvasIsDarkWhateverTheAccent() {
        // the answer has to stay the brightest thing on the screen
        for (accent in Palette.ACCENTS) {
            val canvas = ThemeFromImage.canvasFor(accent)
            val luma = 0.2126f * canvas.red + 0.7152f * canvas.green + 0.0722f * canvas.blue
            assertTrue("the canvas for $accent is too bright ($luma)", luma < 0.2f)
        }
    }

    // ---- contrast ----

    @Test fun textOnADarkColourIsLight() {
        val ink = readableOn(Color(0xFF101018))
        assertTrue("dark background needs light text", ink.red > 0.8f)
    }

    @Test fun textOnALightColourIsDark() {
        val ink = readableOn(Color(0xFFF4F6FA))
        assertTrue("light background needs dark text", ink.red < 0.3f)
    }

    @Test fun everySwatchGetsReadableText() {
        for (colour in Palette.ALL) {
            val ink = readableOn(colour)
            val bgLuma = 0.2126f * colour.red + 0.7152f * colour.green + 0.0722f * colour.blue
            val inkLuma = 0.2126f * ink.red + 0.7152f * ink.green + 0.0722f * ink.blue
            assertTrue(
                "text on $colour would be hard to read",
                kotlin.math.abs(bgLuma - inkLuma) > 0.4f,
            )
        }
    }

    // ---- mixing ----

    @Test fun blendingAllTheWayGivesTheOtherColour() {
        val a = Color(0xFF000000)
        val b = Color(0xFFFFFFFF)
        assertEquals(1f, blend(a, b, 1f).red, 1e-4f)
        assertEquals(0f, blend(a, b, 0f).red, 1e-4f)
        assertEquals(0.5f, blend(a, b, 0.5f).red, 1e-2f)
    }

    @Test fun blendingIsClamped() {
        val a = Color(0xFF000000)
        val b = Color(0xFFFFFFFF)
        assertEquals(0f, blend(a, b, -5f).red, 1e-4f)
        assertEquals(1f, blend(a, b, 5f).red, 1e-4f)
    }

    // ---- pictures are shrunk before they are kept ----

    @Test fun aSmallPictureIsNotShrunk() {
        assertEquals(1, ThemeStore.sampleSizeFor(600, 400))
    }

    @Test fun aLargePictureIsShrunkToSomethingReasonable() {
        val sample = ThemeStore.sampleSizeFor(4000, 3000)
        assertTrue("a 4000px photo should be shrunk", sample > 1)
        assertTrue("but not into nothing", 4000 / sample >= 300)
    }

    @Test fun theShrinkIsAPowerOfTwo() {
        // BitmapFactory only honours powers of two
        for ((w, h) in listOf(1000 to 800, 2400 to 1800, 8000 to 6000)) {
            val s = ThemeStore.sampleSizeFor(w, h)
            assertTrue("$s is not a power of two", s > 0 && (s and (s - 1)) == 0)
        }
    }
}

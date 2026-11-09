package com.merry8989uk.universalcalculator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * What the Settings screen is allowed to show.
 *
 * These checks read the screen's own source. That is a blunt way to test a
 * screen, but it is the only way to catch the thing that actually goes wrong
 * here: a row quietly coming back, or the same control appearing twice.
 */
class SettingsLayoutTest {

    private fun source(path: String): String? {
        val f = listOf(File("src/main/$path"), File("app/src/main/$path"))
            .firstOrNull { it.exists() }
        return f?.readText()
    }

    private val settings: String by lazy {
        source("java/com/merry8989uk/universalcalculator/settings/SettingsScreen.kt") ?: ""
    }

    private val formulas: String by lazy {
        source("java/com/merry8989uk/universalcalculator/ui/FormulasScreen.kt") ?: ""
    }

    // ---- the settings that went with the classic pad ----

    @Test fun theClassicPadSettingsAreGone() {
        if (settings.isBlank()) return
        for (gone in listOf(
            "R.string.measuring_units",
            "R.string.division_sign",
            "R.string.multiplication_sign",
            "R.string.key_text_size",
            "R.string.bit_width",
            "R.string.complex_format",
            "R.string.interval_notation",
        )) {
            assertFalse("$gone is still offered in Settings", gone in settings)
        }
    }

    @Test fun theWholeKeyboardSectionIsGone() {
        if (settings.isBlank()) return
        assertFalse("the keyboard section is still there", "R.string.section_keyboard" in settings)
    }

    // ---- one text size, not two ----

    @Test fun textSizeAppearsOnlyOnce() {
        if (settings.isBlank()) return
        // the label moved into strings.xml when Settings was translated, so the
        // control is found by its string resource now, not by its English text
        val top = settings.indexOf("R.string.settings_text_size")
        val duplicate = settings.indexOf("R.string.display_text_size")
        assertTrue("the one text size control is missing", top > 0)
        assertEquals(
            "text size is offered twice",
            -1, duplicate,
        )
        assertEquals(
            "text size is drawn more than once",
            1, Regex("R\\.string\\.settings_text_size\\b").findAll(settings).count(),
        )
    }

    private fun assertEquals(message: String, expected: Int, actual: Int) {
        org.junit.Assert.assertEquals(message, expected, actual)
    }

    @Test fun theTextAndDensityControlsAreStillThere() {
        if (settings.isBlank()) return
        assertTrue("the text size control is missing", "R.string.settings_text_size" in settings)
        assertTrue("the density control is missing", "R.string.settings_density" in settings)
        assertTrue("the text size slider is missing", "vm::setTextScale" in settings)
        assertTrue("the density slider is missing", "vm::setDensity" in settings)
    }

    // ---- the archive belongs with the settings ----

    @Test fun theArchiveIsOpenedFromSettings() {
        if (settings.isBlank()) return
        assertTrue("Settings does not open the archive", "ArchiveScreen(" in settings)
        assertTrue("Settings has no archive row", "R.string.archive_title" in settings)
    }

    @Test fun theArchiveIsNotInTheFormulasHub() {
        if (formulas.isBlank()) return
        assertFalse("the archive is still a tool in the formulas hub", "\"archive\"" in formulas)
    }

    // ---- the settings that were kept ----

    @Test fun theSettingsThatWereNotAskedAboutAreStillThere() {
        if (settings.isBlank()) return
        for (kept in listOf(
            "R.string.cas_mode",
            "R.string.blink_cursor",
            "R.string.clear_on_exit",
            "R.string.calculation_history",
            "R.string.language",
        )) {
            assertTrue("$kept was removed by mistake", kept in settings)
        }
    }

    // ---- the screen is translated, not just labelled ----

    @Test fun noEnglishIsLeftInTheScreenItself() {
        // A literal in a Text() is a string no translation can reach: the app in
        // Hindi showed an English Settings screen, which is the one place a user
        // goes to change the language. Everything a user reads now comes from
        // strings.xml. What may stay literal is a stored value — "Modern",
        // "system", "en" — which is a key in DataStore, not prose.
        if (settings.isBlank()) return
        val storedValues = setOf(
            "system", "en", "hi", "Classic", "Classic Scientific", "Modern",
            "Indian", "International", "European", "None", "-1", "0", "2", "3", "4", "6",
            "Click", "Bubble", "Pop",
        )
        val prose = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"")
            .findAll(settings)
            .map { it.groupValues[1] }
            .filter { it.any { c -> c.isLetter() } }
            .filter { it !in storedValues }
            .filterNot { it.startsWith("com.merry8989uk") || it.startsWith("java/") || it.startsWith("audio/") }
            // a format pattern, an escaped glyph (a chevron is not a word) and an
            // interpolated identifier are not English either
            .filterNot { it.startsWith("%.2f") || it.contains("\\u") || it.contains("$") }
            .toList()
        assertTrue("these English strings are written into the screen instead of strings.xml: $prose", prose.isEmpty())
    }
}

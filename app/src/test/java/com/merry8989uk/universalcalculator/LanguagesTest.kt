package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.locale.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The languages the app offers, and the promise that each one does something.
 *
 * A language in the picker with no `values-<tag>` folder is a lie: the user
 * chooses it and nothing changes. This test is what stops that.
 */
class LanguagesTest {

    private val res: File? = listOf(File("app/src/main/res"), File("src/main/res"))
        .firstOrNull { it.exists() }

    @Test fun thereAreTwentyTwoIndianLanguages() {
        assertEquals(22, Languages.INDIAN.size)
    }

    @Test fun hindiComesFirst() {
        assertEquals("hi", Languages.INDIAN.first().tag)
    }

    @Test fun theTwentyTwoAreTheScheduledLanguages() {
        val expected = setOf(
            "hi", "bn", "mr", "te", "ta", "gu", "kn", "ml", "or", "pa", "as",
            "ur", "ne", "sa", "mai", "kok", "ks", "sd", "doi", "mni", "brx", "sat",
        )
        assertEquals(expected, Languages.INDIAN.map { it.tag }.toSet())
    }

    @Test fun everyLanguageInThePickerHasAFolderOfTranslations() {
        val base = res ?: return
        val missing = Languages.ALL
            .filter { !File(base, "values-${it.tag}/strings.xml").exists() }
            .map { it.tag }
        assertTrue("these are offered but translate nothing: $missing", missing.isEmpty())
    }

    @Test fun everyTranslationFolderBelongsToALanguageInThePicker() {
        val base = res ?: return
        val offered = Languages.ALL.map { it.tag }.toSet() + setOf("hi")
        val stray = (base.listFiles() ?: emptyArray())
            .filter { it.isDirectory && it.name.startsWith("values-") }
            .map { it.name.removePrefix("values-") }
            .filter { it.length <= 3 && it !in offered }
        assertTrue("these folders are not in the picker: $stray", stray.isEmpty())
    }

    @Test fun aLanguageIsFoundByItsTag() {
        assertNotNull(Languages.byTag("ta"))
        assertEquals("தமிழ்", Languages.byTag("ta")!!.native)
        assertNull(Languages.byTag("xx"))
    }

    @Test fun theRightToLeftLanguagesAreMarked() {
        for (tag in listOf("ur", "sd", "ks")) {
            assertTrue("$tag should be marked right to left", Languages.byTag(tag)!!.rtl)
        }
        for (tag in listOf("hi", "ta", "bn")) {
            assertTrue("$tag should not be marked right to left", !Languages.byTag(tag)!!.rtl)
        }
    }

    @Test fun everyLanguageHasANameInItsOwnScript() {
        for (entry in Languages.ALL) {
            assertTrue("${entry.tag} has no native name", entry.native.isNotBlank())
            assertTrue("${entry.tag} has no English name", entry.english.isNotBlank())
            assertTrue("${entry.tag} writes its own name in Latin letters", entry.native != entry.english)
        }
    }

    @Test fun worldLanguagesAreNotOfferedYet() {
        // they have no translations, so offering them would change nothing
        assertTrue(
            "a world language is offered without translations",
            Languages.ALL.none { it.tag in Languages.WORLD_PENDING.map { w -> w.tag } },
        )
    }
}

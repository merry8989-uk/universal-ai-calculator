package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.locale.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every translation must say the same things as English, and must keep the
 * placeholders it was given.
 *
 * A missing string shows the English fallback, which is untidy but harmless. A
 * missing `%3$d` is not harmless: the screen that formats it throws when it runs,
 * and it throws in a language the developer probably cannot read. That is what
 * this guards.
 *
 * The other half of this file is honesty. A language folder with fifteen strings
 * in it used to pass everything here, because the bar was seven tab keys — so
 * thirteen languages that translate 3.9% of the app were offered in the picker
 * as though they were finished. The bar is now two rules, below, and a language
 * that clears neither cannot be called translated.
 */
class TranslationTest {

    private val root: File = run {
        val candidates = listOf(
            File("src/main/res"),
            File("app/src/main/res"),
        )
        candidates.firstOrNull { it.isDirectory }
            ?: error("could not find the res directory from ${File(".").absolutePath}")
    }

    /** name -> text, for one strings.xml. */
    private fun stringsOf(dir: String): Map<String, String> {
        val file = File(root, "$dir/strings.xml")
        assertTrue("missing $file", file.exists())
        val text = file.readText()
        return Regex("<string name=\"([^\"]+)\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
            .findAll(text)
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    /** Every values-* directory that holds a strings.xml. */
    private fun locales(): List<String> =
        root.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("values") }
            ?.filter { File(it, "strings.xml").exists() }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()

    private fun placeholders(text: String): Set<String> =
        Regex("""%[0-9]*\$?[sdf]""").findAll(text).map { it.value }.toSet()

    @Test fun thereIsMoreThanOneLanguage() {
        assertTrue("expected English and at least one translation, found ${locales()}", locales().size >= 2)
    }

    /**
     * A translation may be partial, and Android is built for that: a string
     * missing from `values-ta` falls back to the English in `values/`, per
     * string, with no crash and no blank. Demanding a complete file would mean
     * a language cannot ship until all 488 strings are done, which is worse for
     * the user than half a translation that reads properly.
     *
     * What is NOT allowed is a key that does not exist in English at all — that
     * is a typo, and it would never be used.
     */
    @Test fun noLanguageHasAStringEnglishDoesNot() {
        val english = stringsOf("values")
        assertTrue("English looks empty", english.size > 100)
        for (locale in locales()) {
            if (locale == "values") continue
            val extra = stringsOf(locale).keys - english.keys
            assertEquals("$locale has strings English does not: ${extra.take(8)}", emptySet<String>(), extra)
        }
    }

    // ---- the two rules a language must clear to be called translated ----

    /**
     * The chrome: every string the tab bar and the Settings screen show.
     *
     * This is the set the picker's promise is measured against, because it is
     * the set a user can see without going looking: the six tabs, and every
     * label, heading, note and button on the screen where they change the
     * language. A language that translates the whole of this can honestly be
     * offered as a translation of the app's frame, even while deeper screens
     * still fall back.
     *
     * It is written out rather than counted, on purpose. A count would go stale
     * in both directions: adding an English string would take languages away for
     * a PR that translated nothing, and a language could pad its count with
     * strings nobody reads. Names cannot pad.
     *
     * The strings added to this list when the Settings literals moved into
     * strings.xml are NOT here yet: no locale has them, so requiring them today
     * would fail Hindi as well as Assamese, and this PR cannot write them. They
     * are added to this list as each language is re-translated — the floor only
     * goes up.
     */
    private val chrome: List<String> = listOf(
        // the tab bar and the words around it
        "tab_standard", "tab_scientific", "tab_convert", "tab_formulas",
        "tab_ask_ai", "tab_settings", "tab_sci_classic", "history",
        // the Settings frame
        "settings_title", "back", "close",
        "section_appearance", "section_general", "section_display", "section_tools",
        "section_ai", "section_updates", "section_reset", "section_feedback",
        // the rows
        "language", "notifications", "live_preview", "degrees_default",
        "number_format", "decimal_places", "calculation_history", "haptic_feedback",
        "fractions_in_results", "keep_screen_on", "save_state", "keypad_layout",
        "theme_label", "home_screen_widget", "lang_system", "key_sounds",
        "minimal_mode", "minimal_mode_help", "enable_ask_ai", "enable_ask_ai_help",
        "cas_mode", "cas_mode_help", "blink_cursor", "clear_on_exit",
        "clear_on_exit_help", "show_calc_info", "show_calc_info_help",
        "archive_title", "archive_kept",
        // the theme and tone names, and the update lines
        "theme_cyberpunk", "theme_emerald", "theme_sunset", "theme_midnight",
        "theme_dynamic", "theme_dynamic_note", "sound_off", "sound_click",
        "sound_bubble", "sound_pop", "check_for_updates", "up_to_date",
        "update_available",
    )

    /**
     * The floor: how many strings a language must carry to be called translated.
     *
     * 300 is three quarters of the 399 strings English had when this floor was
     * set. The eight languages that translate the app sit at 379 and 385; Urdu
     * sits at 181 and the thirteen thin ones at 15. So the floor is not a guess
     * at what is achievable — it is just below what the good files already
     * carry, and far above what a tab bar and a widget description add up to.
     *
     * It is a count, not a percentage, because English keeps growing: a
     * percentage would quietly take a language away every time an English string
     * was added, and take it away for a change that translated nothing. A count
     * only moves when somebody moves it, deliberately, in a commit.
     */
    private val floor: Int = 300

    /**
     * A language the picker offers as complete must translate the whole chrome.
     *
     * The picker's first group is Languages.COMPLETE, and a language in it is
     * presented without any warning label. That is a promise that this screen —
     * the one the user changes the language on — will read in their language,
     * and this test is what makes the promise true.
     */
    @Test fun everyLanguageOfferedAsCompleteTranslatesTheWholeChrome() {
        assertTrue("the chrome list has lost its strings", chrome.size >= 50)
        for (entry in Languages.COMPLETE) {
            val other = stringsOf("values-${entry.tag}")
            val missing = chrome.filter { it !in other.keys }
            assertTrue(
                "values-${entry.tag} is offered as complete but misses ${missing.size} of the " +
                    "${chrome.size} chrome strings: ${missing.take(10)}",
                missing.isEmpty(),
            )
        }
    }

    /** A language offered as complete must also carry most of the rest of the app. */
    @Test fun everyLanguageOfferedAsCompleteCarriesMostOfTheApp() {
        val english = stringsOf("values").size
        for (entry in Languages.COMPLETE) {
            val carried = stringsOf("values-${entry.tag}").size
            assertTrue(
                "values-${entry.tag} is offered as complete with only $carried of $english " +
                    "strings; the floor is $floor",
                carried >= floor,
            )
        }
    }

    /**
     * A thin language must not be offered as complete, and a complete one must
     * not be marked thin.
     *
     * This is the rule the thirteen folders fail: fifteen strings cannot clear
     * the floor, so they may only be offered under the "partly translated"
     * label. It reads the folders, not the list, so a new folder cannot arrive
     * unnoticed and a language cannot be promoted without the strings to back
     * it. The other direction is checked too, so a language cannot be demoted to
     * escape the chrome rule it would otherwise have to meet.
     */
    @Test fun aLanguageIsOnlyCalledCompleteWhenItIsComplete() {
        val offered = Languages.COMPLETE.map { it.tag }.toSet()
        val markedThin = Languages.INCOMPLETE.map { it.tag }.toSet()
        for (locale in locales()) {
            if (locale == "values") continue
            val code = locale.removePrefix("values-")
            if (code.length > 3) continue  // values-night, values-v31 and friends
            val strings = stringsOf(locale)
            val missingChrome = chrome.count { it !in strings.keys }
            val isThin = strings.size < floor || missingChrome > 0
            if (isThin) {
                assertTrue(
                    "$locale translates ${strings.size} strings and misses $missingChrome chrome " +
                        "strings, but the picker offers '$code' as complete",
                    code !in offered,
                )
            } else {
                assertTrue(
                    "$locale translates ${strings.size} strings and the whole chrome, but the " +
                        "picker calls '$code' thin — drop the label or the translation",
                    code !in markedThin,
                )
            }
        }
    }

    @Test fun everyLanguageKeepsItsPlaceholders() {
        val english = stringsOf("values")
        val problems = ArrayList<String>()
        for (locale in locales()) {
            if (locale == "values") continue
            val other = stringsOf(locale)
            for ((name, en) in english) {
                val wanted = placeholders(en)
                if (wanted.isEmpty()) continue
                // absent is fine — it falls back to English, which has them
                val got = placeholders(other[name] ?: continue)
                val lost = wanted - got
                if (lost.isNotEmpty()) problems.add("$locale/$name lost $lost")
            }
        }
        assertTrue("these would crash the screen that formats them: $problems", problems.isEmpty())
    }

    @Test fun noTranslationInventsAPlaceholder() {
        val english = stringsOf("values")
        val problems = ArrayList<String>()
        for (locale in locales()) {
            if (locale == "values") continue
            val other = stringsOf(locale)
            for ((name, text) in other) {
                val invented = placeholders(text) - placeholders(english[name] ?: "")
                if (invented.isNotEmpty()) problems.add("$locale/$name has $invented")
            }
        }
        assertTrue("a placeholder with no argument to fill it: $problems", problems.isEmpty())
    }

    @Test fun noTranslationIsLeftAsTheEnglishText() {
        // a handful of strings are the same in every language on purpose (product
        // names, "Ask AI", the theme names). A whole file left in English is not.
        val english = stringsOf("values")
        for (locale in locales()) {
            if (locale == "values") continue
            val other = stringsOf(locale)
            val identical = english.count { (name, text) -> other[name] == text }
            val share = identical.toDouble() / english.size
            assertTrue(
                "$locale is $((share * 100).toInt())% identical to English — is it translated at all?",
                share < 0.5,
            )
        }
    }

    @Test fun noTranslationHasAnUnescapedApostrophe() {
        // an apostrophe inside an unquoted Android string resource breaks the build
        for (locale in locales()) {
            val file = File(root, "$locale/strings.xml")
            val text = file.readText()
            val offenders = Regex("<string name=\"([^\"]+)\">([^<]*)</string>")
                .findAll(text)
                .filter { it.groupValues[2].contains("'") && !it.groupValues[2].contains("\\'") }
                .map { it.groupValues[1] }
                .toList()
            assertTrue("$locale has unescaped apostrophes in: $offenders", offenders.isEmpty())
        }
    }

    @Test fun everyLanguageFolderIsOfferedInThePicker() {
        // the picker is built from Languages.COMPLETE and Languages.INCOMPLETE, so
        // a folder in neither is a language nobody can choose — and a language in
        // either with no folder changes nothing. Both are checked in LanguagesTest;
        // here the folder side is checked against the same list.
        val offered = Languages.ALL.map { it.tag }.toSet()
        for (locale in locales()) {
            if (locale == "values") continue
            val code = locale.removePrefix("values-")
            if (code.length > 3) continue  // values-night, values-v31 and friends
            assertTrue("$locale exists but '$code' is not offered in the picker", code in offered)
        }
    }
}

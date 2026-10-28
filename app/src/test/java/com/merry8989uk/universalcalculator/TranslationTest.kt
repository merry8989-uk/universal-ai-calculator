package com.merry8989uk.universalcalculator

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
     * a language cannot ship until all 387 strings are done, which is worse for
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

    @Test fun everyLanguageTranslatesTheCoreChrome() {
        // the tabs and the main actions are the least a language should carry
        val mustHave = listOf(
            "tab_standard", "tab_scientific", "tab_convert", "tab_formulas",
            "tab_ask_ai", "tab_settings", "history", "settings_title", "back", "clear",
        )
        for (locale in locales()) {
            if (locale == "values") continue
            val other = stringsOf(locale)
            val missing = mustHave.filter { it !in other.keys }
            assertTrue("$locale does not translate $missing", missing.isEmpty())
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
        // the picker is built from Languages.ALL, so a folder not in that list
        // is a language nobody can choose — and a language in the list with no
        // folder changes nothing. Both are checked in LanguagesTest; here the
        // folder side is checked against the same list.
        val offered = com.merry8989uk.universalcalculator.locale.Languages.ALL.map { it.tag }.toSet()
        for (locale in locales()) {
            if (locale == "values") continue
            val code = locale.removePrefix("values-")
            if (code.length > 3) continue  // values-night, values-v31 and friends
            assertTrue("$locale exists but '$code' is not offered in the picker", code in offered)
        }
    }
}

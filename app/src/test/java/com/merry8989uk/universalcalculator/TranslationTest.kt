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

    @Test fun everyLanguageHasTheSameStringsAsEnglish() {
        val english = stringsOf("values")
        assertTrue("English looks empty", english.size > 100)
        for (locale in locales()) {
            if (locale == "values") continue
            val other = stringsOf(locale)
            val missing = english.keys - other.keys
            val extra = other.keys - english.keys
            assertEquals("$locale is missing these strings: ${missing.take(8)}", emptySet<String>(), missing)
            assertEquals("$locale has strings English does not: ${extra.take(8)}", emptySet<String>(), extra)
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

    @Test fun everyLanguageIsOfferedInTheSettingsList() {
        // the language codes the screen offers must match the folders that exist
        val source = File(root, "../java/com/merry8989uk/universalcalculator/settings/SettingsScreen.kt")
        val file = if (source.exists()) source else File(
            "app/src/main/java/com/merry8989uk/universalcalculator/settings/SettingsScreen.kt",
        )
        if (!file.exists()) return  // the test source set runs from a different root here
        val text = file.readText()
        val listed = Regex("LANGUAGES = listOf\\(([^)]*)\\)")
            .find(text)?.groupValues?.get(1)
            ?.split(",")?.map { it.trim().trim('"') }?.toSet()
            ?: emptySet()
        for (locale in locales()) {
            if (locale == "values") continue
            val code = locale.removePrefix("values-")
            assertTrue("$locale exists but '$code' is not offered in Settings", code in listed)
        }
    }
}

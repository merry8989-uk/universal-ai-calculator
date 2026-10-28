package com.merry8989uk.universalcalculator.locale

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies the language the user picked.
 *
 * The choice is mirrored into SharedPreferences as well as DataStore, because
 * the activity needs it synchronously in attachBaseContext, before any
 * coroutine can run.
 */
object LocaleHelper {

    private const val PREFS = "locale"
    private const val KEY = "language"

    const val SYSTEM = "system"

    /** Languages the app ships translations for. */
    val SUPPORTED: List<String> = listOf(SYSTEM, "en") + Languages.ALL.map { it.tag }

    fun store(context: Context, language: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, language).apply()
    }

    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, SYSTEM) ?: SYSTEM

    /** The same context, with the chosen language applied to its resources. */
    fun wrap(context: Context): Context {
        val language = current(context)
        if (language == SYSTEM || language.isBlank()) return context
        val locale = Locale.forLanguageTag(language)
        if (locale.language.isEmpty()) return context
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        // Urdu, Sindhi and Kashmiri read right to left. Telling the
        // configuration means Compose lays the whole app out mirrored rather
        // than leaving the keypad in the wrong direction.
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}

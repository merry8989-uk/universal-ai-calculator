package com.merry8989uk.universalcalculator.locale

import java.util.Locale

/**
 * The locale to apply for a chosen [language], keeping [phone]'s country when the
 * choice names none.
 *
 * The picker offers "English", not "English (India)": what it stores is `en`,
 * with no country in it, and applying that unchanged takes the country away from
 * the whole app. A phone in India that chose English would then start writing
 * 3,000,000.5 instead of 30,00,000.5, because the number format follows the
 * locale and the locale had quietly moved to nowhere.
 *
 * So a language changes the words and leaves the country alone. A choice that
 * names its own country — `de-DE`, say — is taken exactly as it is, and a tag
 * that is not a language at all leaves the phone as it was.
 */
fun localeFor(language: String, phone: Locale): Locale {
    val chosen = Locale.forLanguageTag(language)
    if (chosen.language.isEmpty()) return phone
    if (chosen.country.isNotEmpty()) return chosen
    if (phone.country.isEmpty()) return chosen
    return Locale.Builder().setLocale(chosen).setRegion(phone.country).build()
}

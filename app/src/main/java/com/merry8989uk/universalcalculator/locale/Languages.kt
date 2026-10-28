package com.merry8989uk.universalcalculator.locale

/**
 * Every language the app offers, and how to show it.
 *
 * Two lists, because they answer two different questions.
 *
 * [INDIAN] is the twenty-two languages in the Eighth Schedule of the Indian
 * Constitution — the ones a user in India is most likely to want, in the order
 * the Schedule lists them with Hindi first because it is the most spoken.
 *
 * [WORLD] is the ten most spoken languages in the world that the app does not
 * already cover through the Indian list. English is the base language, and
 * Hindi, Bengali and Urdu appear in both lists, so they are not repeated.
 *
 * A name is written in its own script. "தமிழ்" is what a Tamil speaker looks
 * for; "Tamil" is what everybody else reads. Both are shown.
 */
object Languages {

    data class Entry(
        /** The BCP-47 tag, which is also the `values-<tag>` folder suffix. */
        val tag: String,
        /** The name in its own script. */
        val native: String,
        /** The name in English, for everyone else. */
        val english: String,
        /** True when the script runs right to left. */
        val rtl: Boolean = false,
    )

    /** The twenty-two scheduled languages of India. */
    val INDIAN: List<Entry> = listOf(
        Entry("hi", "हिन्दी", "Hindi"),
        Entry("bn", "বাংলা", "Bengali"),
        Entry("mr", "मराठी", "Marathi"),
        Entry("te", "తెలుగు", "Telugu"),
        Entry("ta", "தமிழ்", "Tamil"),
        Entry("gu", "ગુજરાતી", "Gujarati"),
        Entry("kn", "ಕನ್ನಡ", "Kannada"),
        Entry("ml", "മലയാളം", "Malayalam"),
        Entry("or", "ଓଡ଼ିଆ", "Odia"),
        Entry("pa", "ਪੰਜਾਬੀ", "Punjabi"),
        Entry("as", "অসমীয়া", "Assamese"),
        Entry("ur", "اردو", "Urdu", rtl = true),
        Entry("ne", "नेपाली", "Nepali"),
        Entry("sa", "संस्कृतम्", "Sanskrit"),
        Entry("mai", "मैथिली", "Maithili"),
        Entry("kok", "कोंकणी", "Konkani"),
        Entry("ks", "کٲشُر", "Kashmiri", rtl = true),
        Entry("sd", "سنڌي", "Sindhi", rtl = true),
        Entry("doi", "डोगरी", "Dogri"),
        Entry("mni", "ꯃꯤꯇꯩꯂꯣꯟ", "Manipuri"),
        Entry("brx", "बड़ो", "Bodo"),
        Entry("sat", "ᱥᱟᱱᱛᱟᱲᱤ", "Santali"),
    )

    /**
     * The ten most spoken languages in the world, minus the ones above.
     *
     * These are listed but NOT offered in the picker yet, because the app has no
     * translation for them. Offering a language that changes nothing is worse
     * than not offering it: the user picks Español and the app stays English.
     *
     * The list is kept so that adding a `values-es` folder is the only step
     * needed when the translations exist.
     */
    val WORLD_PENDING: List<Entry> = listOf(
        Entry("zh", "中文", "Chinese"),
        Entry("es", "Español", "Spanish"),
        Entry("fr", "Français", "French"),
        Entry("ar", "العربية", "Arabic", rtl = true),
        Entry("pt", "Português", "Portuguese"),
        Entry("ru", "Русский", "Russian"),
        Entry("de", "Deutsch", "German"),
        Entry("ja", "日本語", "Japanese"),
        Entry("id", "Bahasa Indonesia", "Indonesian"),
    )

    /** Everything the picker offers. Only languages that actually translate. */
    val ALL: List<Entry> = INDIAN

    /** The tag for a language, or null when it is not offered. */
    fun byTag(tag: String): Entry? = ALL.firstOrNull { it.tag == tag }

    /** True when the tag is one the app ships a translation folder for. */
    fun isSupported(tag: String): Boolean = tag == LocaleHelper.SYSTEM || tag == "en" || byTag(tag) != null
}

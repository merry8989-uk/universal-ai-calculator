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
 *
 * Being in [INDIAN] is not the same as being translated. Thirteen of the
 * twenty-two carry fifteen of the app's strings — the tab bar and nothing else —
 * and Urdu carries 181 of them. That is why each entry says whether it is
 * [Entry.complete], and why the picker is built from [COMPLETE] and
 * [INCOMPLETE] rather than from the whole list: a language offered as though it
 * were finished, and then leaving nine tenths of the app in English, is the app
 * lying about itself. The folders stay, because a translated tab bar is worth
 * having; only the claim goes.
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
        /**
         * True when this language translates the whole of the screens a user
         * can see, false when it is still thin.
         *
         * Set by hand, because a resource folder cannot be counted at runtime,
         * and checked by `TranslationTest`, which reads the folders and refuses
         * to let this list and the files disagree in either direction.
         */
        val complete: Boolean = true,
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
        Entry("or", "ଓଡ଼ିଆ", "Odia", complete = false),
        Entry("pa", "ਪੰਜਾਬੀ", "Punjabi", complete = false),
        Entry("as", "অসমীয়া", "Assamese", complete = false),
        Entry("ur", "اردو", "Urdu", rtl = true, complete = false),
        Entry("ne", "नेपाली", "Nepali", complete = false),
        Entry("sa", "संस्कृतम्", "Sanskrit", complete = false),
        Entry("mai", "मैथिली", "Maithili", complete = false),
        Entry("kok", "कोंकणी", "Konkani", complete = false),
        Entry("ks", "کٲشُر", "Kashmiri", rtl = true, complete = false),
        Entry("sd", "سنڌي", "Sindhi", rtl = true, complete = false),
        Entry("doi", "डोगरी", "Dogri", complete = false),
        Entry("mni", "ꯃꯤꯇꯩꯂꯣꯟ", "Manipuri", complete = false),
        Entry("brx", "बड़ो", "Bodo", complete = false),
        Entry("sat", "ᱥᱟᱱᱛᱟᱲᱤ", "Santali", complete = false),
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

    /**
     * Every language that ships a `values-<tag>` folder.
     *
     * This is the answer to "does choosing this tag change anything at all", and
     * it is what [isSupported] needs. It is NOT what the picker shows: half of
     * these folders hold fifteen strings, so the picker offers [COMPLETE] first
     * and [INCOMPLETE] with a label that says what the user is getting.
     */
    val ALL: List<Entry> = INDIAN

    /** The languages that translate the whole app, in the Schedule's order. */
    val COMPLETE: List<Entry> = INDIAN.filter { it.complete }

    /** The languages whose translation is still thin, labelled as such. */
    val INCOMPLETE: List<Entry> = INDIAN.filterNot { it.complete }

    /** The tag for a language, or null when it is not offered. */
    fun byTag(tag: String): Entry? = ALL.firstOrNull { it.tag == tag }

    /** True when the tag is one the app ships a translation folder for. */
    fun isSupported(tag: String): Boolean = tag == LocaleHelper.SYSTEM || tag == "en" || byTag(tag) != null
}

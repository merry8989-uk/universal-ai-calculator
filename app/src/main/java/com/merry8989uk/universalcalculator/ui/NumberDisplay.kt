package com.merry8989uk.universalcalculator.ui

import com.merry8989uk.universalcalculator.calc.DecimalFormat
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.settings.SettingsState
import java.util.Locale

/**
 * How a result is written out, in one place for every screen.
 *
 * The calculator, the converter and anything else that shows a number all come
 * through here, because the alternative is what the app used to do: the pad wrote
 * 3.000.000,5 while the converter, three taps away, wrote 3000000.5 for the same
 * value. A number that changes its clothes when you change screen reads as a
 * different number.
 *
 * The work is [Format]'s; this only hands it the settings that shape the answer,
 * so a screen never has to remember which three of them the formatter wants.
 *
 * [locale] is a parameter rather than read inside the formatter so the rule can
 * be tested against a phone in another country, which is the only way to check a
 * rule about locales at all.
 */
fun SettingsState.displayNumber(text: String, locale: Locale = Locale.getDefault()): String {
    // Normal keeps the number the calculator worked out; the other four rewrite
    // it first (fixed, scientific, engineering, SI) and only then group it.
    val mode = DecimalFormat.of(decimalFormat)
    if (mode == DecimalFormat.NORMAL) return Format.grouped(text, numberFormat, decimals, locale)
    val value = text.toDoubleOrNull() ?: return text
    return Format.grouped(Format.number(value, mode, decimalDigits), numberFormat, -1, locale)
}

/**
 * A value the user did not type — a conversion, say — written the way the
 * calculator writes its own answers.
 *
 * Going through [Format.number] first means the answer is rounded and trimmed
 * before it is grouped, so the converter cannot drift into a display of its own.
 */
fun SettingsState.displayValue(value: Double, locale: Locale = Locale.getDefault()): String =
    displayNumber(Format.number(value), locale)

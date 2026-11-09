package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.settings.SettingsState
import com.merry8989uk.universalcalculator.ui.displayNumber
import com.merry8989uk.universalcalculator.ui.displayValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The converter writes a number the way the calculator writes one.
 *
 * The two screens used to disagree about the same value: the pad showed
 * 3.000.000,5 and the converter, one tab away, showed 3000000.5. Neither was
 * wrong on its own — the converter simply never asked the formatter, so a user
 * reading both had to decide which number to believe. That is a formatting bug
 * with a formatting fix: one function, fed by the settings, called from both.
 *
 * What is checked here is sameness, not any particular separator. The separator
 * is [NumberFormatFollowsLocaleTest]'s business; this test only insists that
 * whatever it decides, both screens say it.
 */
class ConverterMatchesCalculatorTest {

    private val india = Locale.forLanguageTag("en-IN")
    private val america = Locale.forLanguageTag("en-US")
    private val germany = Locale.forLanguageTag("de-DE")

    private val formats = listOf(
        Format.FOLLOW_LOCALE,
        "Indian",
        "International",
        "European",
        "None",
    )

    @Test fun bothScreensWriteTheSameStringForTheSameValueAndSettings() {
        for (format in formats) {
            for (decimals in listOf(-1, 0, 2)) {
                val settings = SettingsState(numberFormat = format, decimals = decimals)
                for (locale in listOf(india, america, germany)) {
                    assertEquals(
                        "format=$format decimals=$decimals locale=$locale",
                        settings.displayNumber(Format.number(3000000.5), locale),
                        settings.displayValue(3000000.5, locale),
                    )
                }
            }
        }
    }

    @Test fun theReportedBugIsGone() {
        // the value in the ticket, on the phone the ticket was written on
        val settings = SettingsState(numberFormat = "European")
        assertEquals("3.000.000,5", settings.displayNumber("3000000.5", germany))
        assertEquals("3.000.000,5", settings.displayValue(3000000.5, germany))
    }

    @Test fun theConverterFollowsThePhoneWhenNothingIsChosen() {
        val settings = SettingsState()
        assertEquals("30,00,000.5", settings.displayValue(3000000.5, india))
        assertEquals("3,000,000.5", settings.displayValue(3000000.5, america))
        assertEquals("3.000.000,5", settings.displayValue(3000000.5, germany))
    }

    @Test fun theDefaultSettingIsThePhoneAndNotACountry() {
        // if this ever goes back to a name, the format silently stops following
        // the language, which is the whole bug this change is about
        assertEquals(Format.FOLLOW_LOCALE, SettingsState().numberFormat)
    }

    @Test fun aStoredOverrideIsNotQuietlyRewrittenByThePhone() {
        // someone who chose Indian keeps Indian on a German phone
        val settings = SettingsState(numberFormat = "Indian")
        assertEquals("30,00,000.5", settings.displayValue(3000000.5, germany))
        assertEquals("30,00,000.5", settings.displayNumber("3000000.5", germany))
    }

    @Test fun theDecimalsSettingReachesTheConverterToo() {
        val settings = SettingsState(numberFormat = "None", decimals = 2)
        assertEquals("0.33", settings.displayValue(1.0 / 3.0, america))
        assertEquals(settings.displayNumber(Format.number(1.0 / 3.0), america), settings.displayValue(1.0 / 3.0, america))
    }

    @Test fun theScientificFormatIsWrittenTheSameOnBothScreens() {
        val settings = SettingsState(decimalFormat = "SCIENTIFIC", decimalDigits = 4)
        val calculator = settings.displayNumber(Format.number(12345678.9), america)
        assertEquals(calculator, settings.displayValue(12345678.9, america))
        assertTrue("scientific lost its power of ten: $calculator", calculator.contains("\u00D710"))
    }

    @Test fun aValueTheConverterCannotReadComesThroughUnchanged() {
        // the answer field holds a dash when the input is not a number, and that
        // dash must not become an empty string on its way through the formatter
        assertEquals("error", SettingsState().displayNumber("error", america))
        assertEquals("NaN", SettingsState().displayNumber("NaN", germany))
    }
}

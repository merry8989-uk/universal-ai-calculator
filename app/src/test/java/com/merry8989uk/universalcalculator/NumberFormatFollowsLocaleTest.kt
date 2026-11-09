package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.locale.localeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The number format follows the phone until someone says otherwise.
 *
 * A German phone that has never opened Settings used to write 3,000,000.5: the
 * separators were decided by a stored string that began life as "Indian", so the
 * app had an opinion about a question the phone had already answered. Nobody
 * picks a country by installing a calculator, and the ones who do care already
 * have four names to choose from.
 *
 * So the rule is: the four stored names are overrides and always win, and
 * anything else means nobody chose, and the locale decides. These tests hand the
 * locale in rather than reading the machine's own, because a rule about locales
 * is untestable on a machine that only has one.
 */
class NumberFormatFollowsLocaleTest {

    private val india = Locale.forLanguageTag("en-IN")
    private val america = Locale.forLanguageTag("en-US")
    private val germany = Locale.forLanguageTag("de-DE")

    @Test fun anIndianPhoneCountsInLakhsWithoutBeingToldTo() {
        assertEquals("30,00,000.5", Format.grouped("3000000.5", Format.FOLLOW_LOCALE, -1, india))
    }

    @Test fun anAmericanPhoneCountsInThousands() {
        assertEquals("3,000,000.5", Format.grouped("3000000.5", Format.FOLLOW_LOCALE, -1, america))
    }

    @Test fun aGermanPhonePutsTheCommaWhereThePointWas() {
        assertEquals("3.000.000,5", Format.grouped("3000000.5", Format.FOLLOW_LOCALE, -1, germany))
    }

    @Test fun followingThePhoneIsWhatTheFormatterDoesWhenNothingIsSaid() {
        assertEquals(
            Format.grouped("3000000.5", Format.FOLLOW_LOCALE, -1, america),
            Format.grouped("3000000.5", locale = america),
        )
        assertEquals(
            Format.grouped("3000000.5", Format.FOLLOW_LOCALE, -1, germany),
            Format.grouped("3000000.5", locale = germany),
        )
    }

    @Test fun aChosenFormatOutranksThePhone() {
        // the whole point of an override is that it survives a phone that disagrees
        assertEquals("30,00,000.5", Format.grouped("3000000.5", "Indian", -1, america))
        assertEquals("3,000,000.5", Format.grouped("3000000.5", "International", -1, germany))
        assertEquals("3.000.000,5", Format.grouped("3000000.5", "European", -1, india))
    }

    @Test fun noneMeansNoSeparatorsOnAnyPhone() {
        for (locale in listOf(india, america, germany)) {
            assertEquals("3000000.5", Format.grouped("3000000.5", "None", -1, locale))
        }
    }

    @Test fun aStoredValueThisVersionDoesNotKnowFallsBackToThePhone() {
        // a name from an older or newer build must not leave the display unformatted
        assertEquals("3,000,000.5", Format.grouped("3000000.5", "", -1, america))
        assertEquals("3,000,000.5", Format.grouped("3000000.5", "indian", -1, america))
        assertEquals("3.000.000,5", Format.grouped("3000000.5", "Klingon", -1, germany))
    }

    @Test fun theChosenDecimalsAreStillAppliedWhenThePhoneDecides() {
        assertEquals("30,00,000.50", Format.grouped("3000000.5", Format.FOLLOW_LOCALE, 2, india))
        assertEquals("3.000.000,50", Format.grouped("3000000.5", Format.FOLLOW_LOCALE, 2, germany))
        assertEquals("1,234", Format.grouped("1234.4", Format.FOLLOW_LOCALE, 0, america))
        assertEquals("1.234", Format.grouped("1234.4", Format.FOLLOW_LOCALE, 0, germany))
    }

    @Test fun frenchKeepsItsCommaForTheDecimalPoint() {
        // the grouping separator is a narrow space in French, which the platform
        // decides; the comma is the part that must not drift
        val french = Locale.forLanguageTag("fr-FR")
        val written = Format.grouped("3000000.5", Format.FOLLOW_LOCALE, -1, french)
        assertTrue("French wrote $written", written.endsWith(",5"))
        assertTrue("French wrote $written", written.startsWith("3"))
    }

    @Test fun aLanguageWithNoCountryStillCountsInLakhs() {
        // some phones report hi rather than hi-IN, and lakhs are still right
        assertEquals("30,00,000.5", Format.grouped("3000000.5", Format.FOLLOW_LOCALE, -1, Locale("hi")))
        assertEquals("30,00,000.5", Format.grouped("3000000.5", Format.FOLLOW_LOCALE, -1, Locale("mr")))
    }

    @Test fun aLanguageThatIsNotIndianDoesNotCountInLakhs() {
        // Spanish with no region is grouped like Spanish, not like Hindi: lakhs are
        // India's convention, and naming a language is not the same as naming India
        val spanish = Format.grouped("3000000.5", Format.FOLLOW_LOCALE, -1, Locale("es"))
        assertEquals(
            Format.grouped("3000000.5", Format.FOLLOW_LOCALE, -1, Locale.forLanguageTag("es-ES")),
            spanish,
        )
        assertNotEquals(
            Format.grouped("3000000.5", Format.FOLLOW_LOCALE, -1, Locale("hi")),
            spanish,
        )
    }

    @Test fun aNegativeNumberKeepsItsSignAndItsGrouping() {
        assertEquals("-30,00,000.5", Format.grouped("-3000000.5", Format.FOLLOW_LOCALE, -1, india))
        assertEquals("-3.000.000,5", Format.grouped("-3000000.5", Format.FOLLOW_LOCALE, -1, germany))
    }

    @Test fun choosingALanguageKeepsThePhonesCountry() {
        // the picker stores "en", not "en-IN"; an Indian phone must stay Indian,
        // or picking English would move the numbers to the American convention
        val kept = localeFor("en", india)
        assertEquals("en-IN", kept.toLanguageTag())
        assertEquals("30,00,000.5", Format.grouped("3000000.5", Format.FOLLOW_LOCALE, -1, kept))
    }

    @Test fun aLanguageThatNamesItsOwnCountryIsTakenAsItIs() {
        assertEquals("de-DE", localeFor("de-DE", india).toLanguageTag())
    }

    @Test fun aTagThatIsNotALanguageLeavesThePhoneAlone() {
        // nothing to apply, so nothing changes
        assertEquals(india, localeFor("", india))
        assertEquals(india, localeFor("!!!", india))
    }

    @Test fun whatIsNotAPlainNumberIsLeftAlone() {
        // an error, a NaN or a mantissa is not a number to group, on any phone
        assertEquals("error", Format.grouped("error", Format.FOLLOW_LOCALE, -1, germany))
        assertEquals("NaN", Format.grouped("NaN", Format.FOLLOW_LOCALE, -1, india))
        assertEquals("2+3", Format.grouped("2+3", Format.FOLLOW_LOCALE, -1, america))
        val scientific = Format.number(12345678.9, com.merry8989uk.universalcalculator.calc.DecimalFormat.SCIENTIFIC, 4)
        assertEquals(scientific, Format.grouped(scientific, Format.FOLLOW_LOCALE, -1, germany))
    }
}

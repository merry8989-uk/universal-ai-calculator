package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.UnitHit
import com.merry8989uk.universalcalculator.calc.UnitSearch
import com.merry8989uk.universalcalculator.calc.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The unit search, checked against the real table of 43 categories.
 *
 * The screen cannot be run here, so the rule the screen leans on is a pure
 * function of the query and the categories: which units match, and in what
 * order. That is the part that was missing — before it, the only way to a unit
 * was forty swipes of chips and a dropdown — so it is the part worth pinning
 * down. The expectations below use the real categories rather than a fixture,
 * because a search that works on invented units proves nothing about finding
 * "Concentration (molar)".
 */
class UnitSearchTest {

    /** Convenience: the units a query lands on, as "Category · unit" lines. */
    private fun found(query: String): List<String> =
        UnitSearch.hits(query).map { "${it.category.name} \u00B7 ${it.unit.name}" }

    private fun firstHit(query: String): UnitHit =
        UnitSearch.hits(query).firstOrNull()
            ?: throw AssertionError("nothing found for '$query'")

    // ---- Symbols ----

    @Test fun aSymbolFindsTheUnitItStandsFor() {
        // "m" is the metre and "kg" the kilogram: the two symbols a person is
        // most likely to type without knowing which category they sit under.
        assertEquals("Length \u00B7 Metre", found("m").first())
        assertEquals("Mass \u00B7 Kilogram", found("kg").first())
    }

    @Test fun anExactSymbolBeatsTheLongerOnesThatMerelyStartWithIt() {
        // "m" starts the symbols of millimetre, micrometre and mile. If those
        // came first, the field would be useless for the commonest unit there is.
        val hits = UnitSearch.hits("m")
        assertEquals("Metre", hits.first().unit.name)
        assertTrue("millimetre should still be on the list", hits.any { it.unit.name == "Millimetre" })
        assertTrue(
            "an exact symbol must come before every longer one",
            hits.indexOfFirst { it.unit.name == "Millimetre" } > 0,
        )
    }

    @Test fun theDegreeSignIsOptionalForCelsius() {
        // The symbol in the table is a bare "C", but nobody types it that way.
        // All three spellings have to reach the same unit.
        for (query in listOf("\u00B0C", "\u00B0c", "c")) {
            assertTrue(
                "'$query' should find Celsius, found ${found(query).take(5)}",
                found(query).contains("Temperature \u00B7 Celsius"),
            )
        }
    }

    @Test fun everyUnitInTheTableCanBeFoundByItsOwnSymbol() {
        // The search is only as good as its worst miss: a unit that cannot be
        // found by typing what it is called is a unit that still costs a swipe.
        val missing = mutableListOf<String>()
        Units.categories.forEachIndexed { ci, category ->
            category.units.forEachIndexed { ui, unit ->
                val hit = UnitHit(ci, ui)
                if (hit !in UnitSearch.hits(unit.symbol)) {
                    missing.add("${category.name} \u00B7 ${unit.name} (${unit.symbol})")
                }
            }
        }
        assertTrue("these units cannot be found by their own symbol: $missing", missing.isEmpty())
    }

    @Test fun noUnitIsOfferedTwice() {
        // A unit can match on its symbol, its name and its category at once;
        // it must still be listed once, or the list becomes noise.
        val hits = UnitSearch.hits("concentration")
        assertEquals("the same unit is listed twice", hits.size, hits.toSet().size)
    }

    // ---- Names and categories ----

    @Test fun aNameFindsItsUnit() {
        // "molar" is the word in the issue: the category the reporter could not
        // reach. It is matched as part of the category name, brackets and all.
        val hits = found("molar")
        assertTrue("'molar' should find Mole/litre, found $hits", hits.contains("Concentration (molar) \u00B7 Mole/litre"))
        assertTrue("'molar' should find Millimole/litre", hits.contains("Concentration (molar) \u00B7 Millimole/litre"))
    }

    @Test fun aCategoryNameListsTheWholeCategory() {
        val hits = found("concentration")
        val expected = Units.categories
            .filter { it.name.startsWith("Concentration") }
            .sumOf { it.units.size }
        assertEquals("a category search should list every unit in it", expected, hits.size)
        assertTrue("nothing outside a concentration category belongs", hits.all { it.startsWith("Concentration") })
    }

    @Test fun aNameSearchIgnoresCaseAndTheBracketsInTheCategory() {
        // Typed the way a person types it, not the way the table writes it.
        assertEquals(found("concentration molar"), found("Concentration (Molar)"))
        assertEquals(found("molar"), found("MOLAR"))
    }

    @Test fun aNameAndASymbolCanBeSearchedWithTheSpaceLeftIn() {
        // "light year" is one name with a space in it and "Light year" is how it
        // is stored; neither spelling should be the only one that works.
        assertTrue(found("light year").contains("Length \u00B7 Light year"))
        assertTrue(found("lightyear").contains("Length \u00B7 Light year"))
    }

    // ---- Nothing, and the edges ----

    @Test fun aQueryThatMatchesNothingFindsNothing() {
        // The screen shows "no unit matches" off the empty list, so it must be
        // empty rather than quietly falling back to the whole table.
        assertTrue(UnitSearch.hits("zzzqqq").isEmpty())
        assertTrue(UnitSearch.hits("concentraton").isEmpty())
    }

    @Test fun aBlankQueryMatchesNothingRatherThanEverything() {
        // An empty field means the user is browsing the chips; returning every
        // unit would put a 500-row list under a field nobody has typed in.
        for (blank in listOf("", " ", "   ")) {
            assertTrue("'$blank' should match nothing", UnitSearch.hits(blank).isEmpty())
        }
    }

    @Test fun aCategoryMatchComesAfterAnExactSymbol() {
        // "Pressure" the category and the units inside it are a worse answer
        // than a unit whose symbol is exactly what was typed, so a query that
        // does both must put the unit first.
        val hits = UnitSearch.hits("bar")
        assertFalse(hits.isEmpty())
        assertEquals("Bar", hits.first().unit.name)
    }

    @Test fun theLimitStopsTheListWhereItIsAsked() {
        // The screen asks for a bounded list so the results panel stays short;
        // the cut must come off the end, never off the best answers.
        val all = UnitSearch.hits("concentration")
        val few = UnitSearch.hits("concentration", limit = 3)
        assertEquals(3, few.size)
        assertEquals(all.take(3), few)
        assertTrue(UnitSearch.hits("concentration", limit = 0).isEmpty())
    }
}

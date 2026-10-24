package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.ui.CALC_MODES
import com.merry8989uk.universalcalculator.ui.CLASSIC_ALPHA
import com.merry8989uk.universalcalculator.ui.CLASSIC_PAGE
import com.merry8989uk.universalcalculator.ui.CLASSIC_PAGE_ONE
import com.merry8989uk.universalcalculator.ui.CLASSIC_PAGE_TWO
import com.merry8989uk.universalcalculator.ui.CLASSIC_SHIFT
import com.merry8989uk.universalcalculator.ui.ClassicKey
import com.merry8989uk.universalcalculator.ui.FUNCTION_KEYS
import com.merry8989uk.universalcalculator.ui.PAD
import com.merry8989uk.universalcalculator.ui.SCIENTIFIC_KEYS
import com.merry8989uk.universalcalculator.ui.Spoken
import com.merry8989uk.universalcalculator.ui.insertFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The classic pad. The tests that matter are the last few: every key on both
 * pages must produce something the evaluator can work out, so no key can answer
 * "error", and every key must carry the printed labels that give the pad its look.
 */
class ClassicPadTest {

    private val special = setOf(
        "C", "del", "=", "00", "ans", "x", "swap", "neg", "MR", "M+", "MS",
        CLASSIC_SHIFT, CLASSIC_ALPHA, CLASSIC_PAGE, "@mode", "@deg", "@left", "@right",
    )

    /** How many arguments a function takes, when it is not the usual one. */
    private val ARITY = mapOf(
        "nthroot" to 2, "hypot" to 2, "mod" to 2, "nCr" to 2, "nPr" to 2,
        "gcd" to 2, "lcm" to 2, "beta" to 2, "atan2" to 2, "pow" to 2,
        "sum" to 2, "prod" to 2, "min" to 2, "max" to 2, "range" to 2, "mround" to 2,
        "geomean" to 2, "harmmean" to 2, "mean" to 2, "median" to 2, "mode" to 2,
        "var" to 2, "sd" to 2,
        "rand" to 0,
    )

    private fun everyKey(): List<ClassicKey> = CLASSIC_PAGE_ONE.flatten() + CLASSIC_PAGE_TWO.flatten()

    /** A key that opens a screen rather than typing anything. */
    private fun isToolOnly(key: ClassicKey) =
        key.tool != null && (key.action.isEmpty() || key.action.startsWith("@"))

    private fun isPadLocal(action: String) = action.startsWith("@")

    // ---- shape ----

    @Test fun eachPageIsNineRowsOfSix() {
        for ((name, page) in listOf("one" to CLASSIC_PAGE_ONE, "two" to CLASSIC_PAGE_TWO)) {
            assertEquals("page $name has the wrong number of rows", 9, page.size)
            page.forEachIndexed { i, row ->
                assertEquals("page $name row $i does not have six keys", 6, row.size)
            }
        }
    }

    @Test fun everyKeyHasALabel() {
        val blank = everyKey().filter { it.label.isBlank() }
        assertTrue("keys with no label: $blank", blank.isEmpty())
    }

    @Test fun noKeyOnAPageIsDrawnTwice() {
        for ((name, page) in listOf("one" to CLASSIC_PAGE_ONE, "two" to CLASSIC_PAGE_TWO)) {
            val labels = page.flatten().map { it.label }
            assertEquals("page $name repeats a label", labels.size, labels.distinct().size)
        }
    }

    // ---- the printed labels ----

    @Test fun everyFunctionKeyCarriesItsPrintedLabels() {
        // the yellow label above and the blue label below are the whole look
        val missing = CLASSIC_PAGE_ONE.flatten()
            .filter { it.action in FUNCTION_KEYS }
            .filter { it.shift == null }
            .map { it.label }
        assertTrue("these function keys print no second label: $missing", missing.isEmpty())
    }

    @Test fun everyKeyWithAPrintedLabelCanActOnIt() {
        val broken = CLASSIC_PAGE_ONE.flatten().filter { key ->
            (key.shift != null && key.shiftAction == null && key.shiftTool == null) ||
                (key.alpha != null && key.alphaAction == null && key.alphaTool == null)
        }.map { it.label }
        assertTrue("these keys show a label they cannot act on: $broken", broken.isEmpty())
    }

    @Test fun theModifiersAreThereAndDistinct() {
        val actions = CLASSIC_PAGE_ONE.first().map { it.action }
        assertTrue("no SHIFT key", CLASSIC_SHIFT in actions)
        assertTrue("no ALPHA key", CLASSIC_ALPHA in actions)
        assertTrue("no page key", CLASSIC_PAGE in actions)
        assertTrue("SHIFT and ALPHA must be different keys", CLASSIC_SHIFT != CLASSIC_ALPHA)
    }

    @Test fun theSecondPageIsTurnedToByItsKey() {
        // the page key says where it goes: 2nd on page one, 1st on page two
        val one = CLASSIC_PAGE_ONE.first().first { it.action == CLASSIC_PAGE }
        val two = CLASSIC_PAGE_TWO.first().first { it.action == CLASSIC_PAGE }
        assertEquals("2nd", one.label)
        assertEquals("1st", two.label)
    }

    @Test fun theSecondPageHoldsTheAppsTools() {
        val tools = CLASSIC_PAGE_TWO.flatten().mapNotNull { it.tool }.toSet()
        for (expected in listOf("matrix", "statistics", "complex", "graph", "date", "currency")) {
            assertTrue("page two has no '$expected' tool", expected in tools)
        }
    }

    // ---- everything works ----

    @Test fun everyActionIsOneTheCalculatorKnows() {
        val plain = setOf(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "e",
            "+", "-", "*", "/", "%", "^", "(", ")",
        )
        val known = plain + special + FUNCTION_KEYS + SCIENTIFIC_KEYS.toSet() + "rand"
        val unknown = ArrayList<String>()
        everyKey().forEach { key ->
            val actions = listOfNotNull(
                key.action, key.shiftAction, key.alphaAction,
                key.tool, key.shiftTool, key.alphaTool,
            )
            for (action in actions) {
                if (action.isEmpty() || isPadLocal(action)) continue
                if (action !in known && key.tool == null) unknown.add("${key.label} -> $action")
            }
        }
        assertTrue("these keys do nothing: $unknown", unknown.isEmpty())
    }

    @Test fun everyTypedKeyProducesSomethingTheEvaluatorCanRun() {
        val failures = ArrayList<String>()
        everyKey().forEach { key ->
            for (action in listOfNotNull(key.action, key.shiftAction, key.alphaAction)) {
                if (action.isEmpty() || isPadLocal(action)) continue
                if (action in special) continue
                if (isToolOnly(key)) continue
                val snippet = insertFor(action)
                if (snippet.isBlank()) {
                    failures.add("${key.label} -> '$action' inserts nothing"); continue
                }
                if (action in FUNCTION_KEYS) {
                    val args = when (val n = ARITY[action] ?: 1) {
                        0 -> ""
                        else -> (1..n).joinToString(",") { "1" }
                    }
                    val probe = snippet + args + ")".repeat(snippet.count { it == '(' })
                    val value = runCatching { Evaluator.evaluate(probe, degrees = true) }.getOrNull()
                    if (value == null || !value.isFinite()) {
                        failures.add("${key.label} -> '$probe' does not evaluate")
                    }
                }
            }
        }
        assertTrue("these keys would show an error: $failures", failures.isEmpty())
    }

    @Test fun everyKeyHasASpokenLabel() {
        val missing = ArrayList<String>()
        everyKey().forEach { key ->
            for (action in listOfNotNull(key.action, key.shiftAction, key.alphaAction)) {
                if (action.isEmpty()) continue
                val spoken = if (isPadLocal(action)) Spoken.key(key.label) else Spoken.key(action)
                val isGlyph = spoken.length == 1 && !spoken[0].isLetterOrDigit()
                if (spoken.isBlank() || isGlyph) missing.add("${key.label} ($action) -> '$spoken'")
            }
        }
        assertTrue("keys a screen reader could not name: $missing", missing.isEmpty())
    }

    @Test fun theModernPadAndTheClassicPadShareTheirActions() {
        val modern = (PAD.flatten() + SCIENTIFIC_KEYS).toSet()
        val classicTyped = everyKey()
            .filterNot { isToolOnly(it) }
            .flatMap { listOfNotNull(it.action, it.shiftAction, it.alphaAction) }
            .filter { !isPadLocal(it) && it !in special && it !in setOf("e", "rand") }
            .toSet()
        val onlyClassic = classicTyped - modern
        assertTrue("the classic pad offers keys the modern pad does not: $onlyClassic", onlyClassic.isEmpty())
    }

    // ---- the MODE list ----

    @Test fun theModeListOffersEveryKindOfCalculation() {
        assertTrue("the MODE list is empty", CALC_MODES.size >= 15)
        val ids = CALC_MODES.map { it.id }
        assertEquals("the MODE list repeats an id", ids.size, ids.distinct().size)
        for (expected in listOf("standard", "scientific", "classic", "matrix", "statistics", "currency")) {
            assertTrue("the MODE list has no '$expected'", expected in ids)
        }
    }

    @Test fun everyModeHasANameAndADescription() {
        val thin = CALC_MODES.filter { it.name.isBlank() || it.blurb.isBlank() }
        assertTrue("modes with nothing to show: $thin", thin.isEmpty())
    }
}

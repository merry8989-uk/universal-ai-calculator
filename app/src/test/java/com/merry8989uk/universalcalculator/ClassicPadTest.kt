package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.ui.CALC_MODES
import com.merry8989uk.universalcalculator.ui.CLASSIC_ALPHA
import com.merry8989uk.universalcalculator.ui.CLASSIC_DPAD
import com.merry8989uk.universalcalculator.ui.CLASSIC_MODE
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

    /**
     * A probe value for the functions that dislike 1.
     *
     * zeta has a pole there, atanh and asech need a value inside (-1, 1), and
     * acosh needs one at or above 1.
     */
    private val PROBE = mapOf(
        "zeta" to 2.0,
        "atanh" to 0.5,
        "asech" to 0.5,
        "acosh" to 2.0,
        "acoth" to 2.0,
    )

    /** Functions the engine understands that the modern keypad has no key for. */
    /**
     * Real engine functions that simply have no key on the modern pad.
     *
     * Page two of the classic pad reaches them, which is the point of page two.
     */
    private val engineOnly = setOf(
        "rand", "sinc", "nCr", "nPr", "gcd", "lcm", "mod", "abs", "sign", "hypot",
        "cbrt", "nthroot", "sqrt", "floor", "ceil", "round", "mround",
        "gamma", "lgamma", "beta", "erf", "erfc", "zeta", "lambertw", "digamma",
        "fib", "catalan", "golden", "totient", "isqrt", "isprime", "nextprime",
        "sum", "prod", "mean", "median", "mode", "sd", "variance", "range",
        "min", "max", "geomean", "harmean", "atan2", "log2", "log10", "log1p", "expm1",
        "tau", "pi",
    )

    private fun everyKey(): List<ClassicKey> = CLASSIC_PAGE_ONE.flatten() + CLASSIC_PAGE_TWO.flatten()

    /** A key that opens a screen rather than typing anything. */
    private fun isToolOnly(key: ClassicKey) =
        key.tool != null && (key.action.isEmpty() || key.action.startsWith("@"))

    /**
     * Whether the key is the pad's own business rather than the engine's.
     *
     * A shape key like x/y is neither: it types nothing the evaluator knows, it
     * puts down a template that [com.merry8989uk.universalcalculator.calc.BoxExpression]
     * turns into a real expression.
     */
    private fun isPadLocal(action: String) =
        action.startsWith("@") || action in com.merry8989uk.universalcalculator.calc.BoxExpression.TEMPLATES

    // ---- shape ----

    @Test fun everyPageIsRowsOfSix() {
        // page one carries the digits and the rules; page two is the functions
        // that do not fit, so the two are not the same height and need not be
        for ((name, page) in listOf("one" to CLASSIC_PAGE_ONE, "two" to CLASSIC_PAGE_TWO)) {
            // six columns, counted in spans, because the direction pad is two
            assertEquals("page $name is not six across", 6, page.first().sumOf { it.span })
            page.forEach { row ->
                assertEquals(
                    "a row on page $name is a different width",
                    6, row.sumOf { it.span },
                )
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
        assertTrue("no MODE key", CLASSIC_MODE in actions)
        assertTrue("SHIFT and ALPHA must be different keys", CLASSIC_SHIFT != CLASSIC_ALPHA)
    }

    @Test fun theClearingKeysAreOnThePad() {
        // they are in the last row, as they are in the reference
        val actions = CLASSIC_PAGE_ONE.flatten().map { it.action }
        assertTrue("no delete key", "del" in actions)
        assertTrue("no clear key", "C" in actions)
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
        val known = plain + special + FUNCTION_KEYS + SCIENTIFIC_KEYS.toSet() + engineOnly
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
                    val n = ARITY[action] ?: 1
                    val probeValue = PROBE[action] ?: 1.0
                    val args = when (n) {
                        0 -> ""
                        else -> (1..n).joinToString(",") { probeValue.toString() }
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
            .filter { !isPadLocal(it) && it !in special && it !in engineOnly && it != "e" }
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

    // ---- the direction pad ----

    @Test fun theDirectionPadIsInTheModifierRow() {
        for ((name, page) in listOf("one" to CLASSIC_PAGE_ONE, "two" to CLASSIC_PAGE_TWO)) {
            val actions = page.first().map { it.action }
            assertTrue("page $name has no direction pad", CLASSIC_DPAD in actions)
        }
    }

    @Test fun theDirectionPadTakesTwoColumns() {
        val pad = CLASSIC_PAGE_ONE.first().first { it.action == CLASSIC_DPAD }
        assertEquals("the direction pad should be two columns wide", 2, pad.span)
    }

    @Test fun onlyTheDirectionPadSpansMoreThanOneColumn() {
        // every other key is one column, or the rows stop lining up
        val wide = everyKey().filter { it.span != 1 }
        assertEquals("these keys are wider than one column: $wide", 2, wide.size)
        assertTrue(
            "only the direction pad may be wider than one column",
            wide.all { it.action == CLASSIC_DPAD },
        )
    }

    @Test fun theModifierRowStillAddsUpToSixColumns() {
        // the six-across grid has to survive a key that takes two
        for ((name, page) in listOf("one" to CLASSIC_PAGE_ONE, "two" to CLASSIC_PAGE_TWO)) {
            val span = page.first().sumOf { it.span }
            assertEquals("page $name's first row is $span columns wide, not six", 6, span)
        }
    }
}

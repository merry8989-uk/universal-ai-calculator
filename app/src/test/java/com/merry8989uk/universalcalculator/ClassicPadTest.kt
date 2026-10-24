package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.ui.CLASSIC_ROWS
import com.merry8989uk.universalcalculator.ui.FUNCTION_KEYS
import com.merry8989uk.universalcalculator.ui.PAD
import com.merry8989uk.universalcalculator.ui.SCIENTIFIC_KEYS
import com.merry8989uk.universalcalculator.ui.Spoken
import com.merry8989uk.universalcalculator.ui.insertFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The classic scientific pad. The important test is the last one: every key it
 * offers must produce something the evaluator can actually work out, so no key
 * can ever answer "error".
 */
class ClassicPadTest {

    private val special = setOf("C", "del", "=", "00", "ans", "x", "swap", "neg", "MR", "M+", "MS")
    private val padLocal = setOf("@shift", "@left", "@right", "@menu", "@deg")

    private fun everyKey() = CLASSIC_ROWS.flatten()

    @Test fun thePadIsEightRowsOfSix() {
        assertEquals(8, CLASSIC_ROWS.size)
        CLASSIC_ROWS.forEachIndexed { i, row ->
            assertEquals("row $i does not have six keys", 6, row.size)
        }
    }

    @Test fun everyKeyHasALabel() {
        val blank = everyKey().filter { it.label.isBlank() }
        assertTrue("keys with no label: $blank", blank.isEmpty())
    }

    @Test fun noKeyIsDrawnTwice() {
        val labels = everyKey().map { it.label }
        assertEquals("a key label is used twice", labels.size, labels.distinct().size)
    }

    @Test fun everyKeyHasASpokenLabel() {
        val missing = everyKey().mapNotNull { key ->
            val action = key.action
            val spoken = if (action.startsWith("@")) key.label else Spoken.key(action)
            if (spoken.isBlank() || spoken == action && action.length == 1 && !action[0].isLetterOrDigit()) {
                "$action -> $spoken"
            } else null
        }
        assertTrue("keys a screen reader could not name: $missing", missing.isEmpty())
    }

    @Test fun everyActionIsOneTheCalculatorKnows() {
        // digits, the point, the operators and the brackets go through as themselves
        val plain = setOf(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "e",
            "+", "-", "*", "/", "%", "^", "(", ")",
        )
        val known = plain + special + FUNCTION_KEYS + SCIENTIFIC_KEYS.toSet()
        val unknown = ArrayList<String>()
        everyKey().forEach { key ->
            for (action in listOfNotNull(key.action, key.shiftedAction)) {
                if (action.isEmpty() || action.startsWith("@")) continue
                if (action !in known) unknown.add("${key.label} -> $action")
            }
        }
        assertTrue("these keys do nothing: $unknown", unknown.isEmpty())
    }

    @Test fun everyTypedKeyProducesSomethingTheEvaluatorCanRun() {
        // "Typed" means it inserts text rather than opening a screen.
        val failures = ArrayList<String>()
        everyKey().forEach { key ->
            for (action in listOfNotNull(key.action, key.shiftedAction)) {
                if (action.isEmpty() || action.startsWith("@")) continue
                if (action in special) continue          // C, del, =, ans … are actions, not text
                if (key.tool != null && action == key.action) continue
                val snippet = insertFor(action)
                if (snippet.isBlank()) { failures.add("${key.label} -> '$action' inserts nothing"); continue }
                // a function key must open a bracket that balances once a number is in it
                if (action in FUNCTION_KEYS) {
                    val probe = snippet + "1" + ")".repeat(snippet.count { it == '(' })
                    val value = runCatching { Evaluator.evaluate(probe, degrees = true) }.getOrNull()
                    if (value == null || !value.isFinite()) {
                        failures.add("${key.label} -> '$probe' does not evaluate")
                    }
                }
            }
        }
        assertTrue("these keys would show an error: $failures", failures.isEmpty())
    }

    @Test fun shiftGivesTheFunctionKeysASecondFunction() {
        // The point of SHIFT: the keys that insert a function get a second one.
        val withoutSecond = everyKey()
            .filter { it.action in FUNCTION_KEYS }
            .filter { it.shiftedLabel == null }
            .map { it.label }
        assertTrue("these function keys have no SHIFT layer: $withoutSecond", withoutSecond.isEmpty())
    }

    @Test fun theModernPadAndTheClassicPadShareTheirActions() {
        // Anything the classic pad types must also be reachable from the modern
        // pad, so the two are not offering different arithmetic.
        val modern = (PAD.flatten() + SCIENTIFIC_KEYS).toSet()
        val classicTyped = everyKey()
            .flatMap { listOfNotNull(it.action, it.shiftedAction) }
            .filter { !it.startsWith("@") && it !in special && it !in setOf("e") }
            .toSet()
        val onlyClassic = classicTyped - modern
        assertTrue("the classic pad offers keys the modern pad does not: $onlyClassic", onlyClassic.isEmpty())
    }
}

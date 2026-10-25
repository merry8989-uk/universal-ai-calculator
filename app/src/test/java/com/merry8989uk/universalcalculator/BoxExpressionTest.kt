package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.BoxExpression
import com.merry8989uk.universalcalculator.calc.ExpressionGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fill-in shapes: press x^y and get `□^□`, type into the holes, and press
 * backspace to empty a hole again rather than to destroy the shape.
 */
class BoxExpressionTest {

    private val box = BoxExpression.BOX.toString()

    private fun start(key: String): Pair<BoxExpression, Int> {
        val template = BoxExpression.templateFor(key)
        assertNotNull("$key is not a shape key", template)
        return BoxExpression().template(0, template!!)
    }

    // ---------------- the shapes ----------------

    @Test fun aPowerKeyPutsDownTwoHoles() {
        val (e, cursor) = start("x^y")
        assertEquals("$box^$box", e.text)
        assertEquals(0, cursor)
        assertEquals(listOf(0, 2), e.slots)
    }

    @Test fun aFractionKeyPutsDownTwoHoles() {
        val (e, cursor) = start("x/y")
        assertEquals("$box/$box", e.text)
        assertEquals(0, cursor)
        assertEquals(listOf(0, 2), e.slots)
    }

    @Test fun aReciprocalKeyPutsDownOne() {
        val (e, cursor) = start("1/x")
        assertEquals("1/$box", e.text)
        assertEquals(2, cursor)
    }

    @Test fun theShapesAreTheOnesTheUserAskedFor() {
        assertTrue("x^y" in BoxExpression.TEMPLATES)
        assertTrue("x/y" in BoxExpression.TEMPLATES)
        assertTrue("1/x" in BoxExpression.TEMPLATES)
    }

    // ---------------- filling them ----------------

    @Test fun typingFillsTheFirstHoleThenMovesOn() {
        var (e, cursor) = start("x^y")
        val (afterFirst, cursorAfterFirst) = e.type(cursor, "2")
        assertEquals("2^$box", afterFirst.text)
        assertEquals("the cursor should move to the next hole", 2, cursorAfterFirst)
        val (afterSecond, cursorAfterSecond) = afterFirst.type(cursorAfterFirst, "8")
        assertEquals("2^8", afterSecond.text)
        assertEquals(3, cursorAfterSecond)
    }

    @Test fun aFractionCanBeFilledTheSameWay() {
        var (e, cursor) = start("x/y")
        val (a, c1) = e.type(cursor, "3")
        val (b, _) = a.type(c1, "4")
        assertEquals("3/4", b.text)
        assertFalse(b.hasEmptySlot())
    }

    @Test fun aMultiCharacterPasteGoesIntoOneHoleAtATime() {
        // a hole holds one character, so only the first is used
        var (e, cursor) = start("x^y")
        val (after, _) = e.type(cursor, "25")
        assertEquals("2^$box", after.text)
    }

    // ---------------- backspace ----------------

    @Test fun backspaceEmptiesTheLastHole() {
        var (e, cursor) = start("x^y")
        val (a, c1) = e.type(cursor, "2")
        val (b, c2) = a.type(c1, "8")
        assertEquals("2^8", b.text)
        val (c, c3) = b.backspace(c2)
        assertEquals("the 8 should be a hole again", "2^$box", c.text)
        assertEquals(2, c3)
    }

    @Test fun backspaceThenEmptiesTheFirstHole() {
        // the case the user described: take the shape apart without losing it
        var (e, cursor) = start("x^y")
        val (a, c1) = e.type(cursor, "2")
        val (b, c2) = a.type(c1, "8")
        val (c, c3) = b.backspace(c2)
        val (d, c4) = c.backspace(c3)
        assertEquals("$box^$box", d.text)
        assertEquals(0, c4)
    }

    @Test fun backspaceOnAnEmptyShapeDoesNothing() {
        val (e, cursor) = start("x^y")
        val (after, at) = e.backspace(cursor)
        assertEquals(e.text, after.text)
        assertEquals(0, at)
    }

    @Test fun backspaceOnFreeTextStillDeletes() {
        val e = BoxExpression("2+3")
        val (after, cursor) = e.backspace(3)
        assertEquals("2+", after.text)
        assertEquals(2, cursor)
    }

    @Test fun aFilledShapeHasNothingLeftToEmpty() {
        var (e, cursor) = start("x/y")
        val (a, c1) = e.type(cursor, "3")
        val (b, _) = a.type(c1, "4")
        assertFalse(b.hasEmptySlot())
        // so backspace deletes as usual
        val (c, _) = b.backspace(3)
        assertEquals("3/", c.text)
    }

    // ---------------- with the rest of the expression ----------------

    @Test fun aShapeCanSitInsideABiggerSum() {
        val (e, cursor) = BoxExpression("1+").template(2, BoxExpression.templateFor("x^y")!!)
        assertEquals("1+$box^$box", e.text)
        assertEquals(listOf(2, 4), e.slots)
        assertEquals(2, cursor)
    }

    @Test fun typingBeforeAShapeMovesItsHoles() {
        val (e, _) = start("x^y")
        val (after, _) = e.type(0, "+")
        // the plus went into the first hole, so the shape is intact
        assertEquals("+^$box", after.text)
    }

    @Test fun aWholeNumberTypedLaterKeepsTheHolesStraight() {
        val (e, cursor) = BoxExpression("9").template(1, BoxExpression.templateFor("x/y")!!)
        assertEquals("9$box/$box", e.text)
        assertEquals(listOf(1, 3), e.slots)
        assertEquals(1, cursor)
    }

    // ---------------- the guard ----------------

    @Test fun anEmptyBoxIsNotAnAnswer() {
        val warning = ExpressionGuard.check("2^$box")
        assertNotNull("an empty box should be refused", warning)
        assertTrue("the message reads '$warning'", warning!!.contains("empty box"))
    }

    @Test fun aFilledShapeIsAccepted() {
        assertNull(ExpressionGuard.check("2^8"))
        assertNull(ExpressionGuard.check("3/4"))
    }
}

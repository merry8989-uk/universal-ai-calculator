package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.CurvePoint
import com.merry8989uk.universalcalculator.calc.DepthFade
import com.merry8989uk.universalcalculator.calc.NiceTicks
import com.merry8989uk.universalcalculator.calc.PlotWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.tan

/**
 * The arithmetic behind the graph screen: the window, the axis numbers, and where
 * a curve is allowed to be a curve.
 *
 * All of this used to live inside a Canvas draw lambda, which meant the only way
 * to find out whether a pinch anchored in the right place, or whether tan(x) drew
 * a wall through its asymptote, was to open the app and look. The sums are pure
 * now, so they are checked here with real numbers instead: a drag is checked
 * against the pixel it came from, a tick step against the family it must belong
 * to, and a pole against the samples either side of it.
 *
 * What these tests cannot say anything about is how the result looks. The layout,
 * the colours and the gesture feel are a device check; this file only pins the
 * numbers that the drawing is allowed to trust.
 */
class PlotWindowTest {

    private val unit = PlotWindow.of(-1.0, 1.0, -1.0, 1.0)

    private fun close(expected: Double, actual: Double, tolerance: Double = 1e-9) {
        assertTrue("expected $expected but was $actual", abs(expected - actual) <= tolerance)
    }

    // ---- 1. pan and zoom ----

    @Test fun aDragMovesTheWindowAgainstTheFinger() {
        // a quarter of the canvas dragged to the right shows a quarter of a
        // window further left, because the paper moves with the hand
        val moved = unit.panByPixels(100f, 0f, 400f, 400f)
        close(-1.5, moved.xMin)
        close(0.5, moved.xMax)
        close(-1.0, moved.yMin)
    }

    @Test fun aDragDownwardsCarriesTheCurveDownWithTheFinger() {
        // the whole of the plot has to move by the distance the finger moved, or
        // the curve slides out from under the thumb
        val moved = unit.panByPixels(0f, 100f, 400f, 400f)
        close(unit.screenY(0.0, 400f) + 100.0, moved.screenY(0.0, 400f).toDouble(), 1e-3)
        close(unit.ySpan, moved.ySpan)
    }

    @Test fun aDragLeavesTheSpanAlone() {
        val moved = unit.panByPixels(37f, -91f, 400f, 250f)
        close(unit.xSpan, moved.xSpan)
        close(unit.ySpan, moved.ySpan)
    }

    @Test fun aPinchKeepsThePointUnderTheFingersStill() {
        val focusX = 0.25
        val focusY = 0.75
        val anchoredX = unit.dataX((focusX * 400f).toFloat(), 400f)
        val anchoredY = unit.dataY((focusY * 400f).toFloat(), 400f)
        val zoomed = unit.zoomBy(2.0, focusX, focusY)
        close(anchoredX, zoomed.dataX((focusX * 400f).toFloat(), 400f))
        close(anchoredY, zoomed.dataY((focusY * 400f).toFloat(), 400f))
        close(1.0, zoomed.xSpan)
    }

    @Test fun zoomingInShrinksTheWindowAndZoomingOutGrowsIt() {
        val inward = unit.zoomBy(4.0)
        close(0.5, inward.xSpan)
        close(0.5, inward.ySpan)
        val outward = unit.zoomBy(0.25)
        close(8.0, outward.xSpan)
        close(8.0, outward.ySpan)
    }

    @Test fun zoomingInThenOutRestoresTheSpanItStartedWith() {
        val there = unit.zoomBy(3.0, 0.3, 0.8)
        val back = there.zoomBy(1.0 / 3.0, 0.3, 0.8)
        close(unit.xSpan, back.xSpan)
        close(unit.ySpan, back.ySpan)
        close(unit.xMin, back.xMin, 1e-6)
    }

    @Test fun aZoomOfNothingIsRefusedRatherThanCollapsingTheWindow() {
        assertEquals(unit, unit.zoomBy(0.0))
        assertEquals(unit, unit.zoomBy(Double.NaN))
        assertEquals(unit, unit.zoomBy(-2.0))
    }

    @Test fun aWindowIsAlwaysWideEnoughToDraw() {
        val flat = PlotWindow.of(3.0, 3.0, 5.0, 5.0)
        assertTrue("a single point became a window", flat.xSpan > 0.0 && flat.ySpan > 0.0)
        val inverted = PlotWindow.of(5.0, -5.0, 4.0, -4.0)
        assertTrue(inverted.xMin < inverted.xMax && inverted.yMin < inverted.yMax)
        close(-5.0, inverted.xMin)
    }

    @Test fun aCanvasWithNoSizeLeavesTheWindowAlone() {
        assertEquals(unit, unit.panByPixels(50f, 50f, 0f, 0f))
    }

    @Test fun theSamplesCoverTheWindowWithRoomToSpare() {
        val box = unit.sampleRange()
        assertTrue("the samples start inside the window", box.first < unit.xMin)
        assertTrue("the samples stop inside the window", box.second > unit.xMax)
    }

    @Test fun aDragAcrossTheScreenAsksForNewSamplesAFewTimesAndNotEveryFrame() {
        // this is what keeps a drag smooth: the sample box has to stay put while
        // the finger moves, or every one of the sixty frames of the drag pays for
        // a fresh set of evaluations
        var box = unit.sampleRange()
        var rebuilds = 0
        for (frame in 1..60) {
            val moved = unit.panByPixels(frame * (400f / 60f), 0f, 400f, 400f)
            val next = moved.sampleRange()
            if (next != box) {
                rebuilds++
                box = next
            }
        }
        assertTrue("the curve was rebuilt $rebuilds times during one drag", rebuilds in 1..20)
    }

    @Test fun aDragAcrossTheScreenEventuallyAsksForNewSamples() {
        val box = unit.sampleRange()
        val far = unit.panByPixels(400f, 0f, 400f, 400f).sampleRange()
        assertTrue("the samples never moved with the window", far.first < box.first)
        assertTrue("the new samples do not cover the window", far.second > far.first)
    }

    @Test fun aSmallPinchDoesNotAskForTheCurveToBeWorkedOutAgain() {
        val box = unit.sampleRange()
        val pinched = unit.zoomBy(1.01).sampleRange()
        assertEquals(box, pinched)
    }

    // ---- 2. nice numbers ----

    @Test fun theTickStepIsAlwaysOneTwoOrFiveTimesAPowerOfTen() {
        var span = 1e-6
        while (span < 1e7) {
            val s = NiceTicks.step(span, 6)
            val magnitude = 10.0.pow(floor(log10(s)))
            val norm = s / magnitude
            val family = listOf(1.0, 2.0, 5.0, 10.0).any { abs(it - norm) < 1e-9 }
            assertTrue("step $s for span $span is not a nice number", family)
            span *= 3.0
        }
    }

    @Test fun everyTickIsAnExactMultipleOfTheStepAndInsideTheRange() {
        val cases = listOf(
            Triple(-6.5, 6.5, 6),
            Triple(0.0, 1.0, 4),
            Triple(-0.003, 0.003, 5),
            Triple(1000.0, 1250.0, 6),
        )
        for ((lo, hi, target) in cases) {
            val set = NiceTicks.choose(lo, hi, target)
            assertFalse("no ticks for $lo..$hi", set.isEmpty)
            for (v in set.values) {
                assertTrue("tick $v outside $lo..$hi", v >= lo - 1e-9 && v <= hi + 1e-9)
                val multiple = v / set.step
                close(multiple, Math.rint(multiple), 1e-6)
            }
            assertTrue("too many ticks for $lo..$hi: ${set.values.size}", set.values.size <= 40)
        }
    }

    @Test fun theTicksAreEvenlySpacedAndAimedAtTheCountAskedFor() {
        val set = NiceTicks.choose(-6.5, 6.5, 6)
        val gaps = set.values.zipWithNext { a, b -> b - a }
        for (g in gaps) close(set.step, g, 1e-9)
        assertTrue("six ticks were asked for, ${set.values.size} came back", set.values.size in 4..12)
    }

    @Test fun aRangeTooOddToHoldATickIsDrawnWithoutAny() {
        assertTrue(NiceTicks.choose(2.0, 2.0).isEmpty)
        assertTrue(NiceTicks.choose(3.0, 1.0).isEmpty)
        assertTrue(NiceTicks.choose(Double.NaN, 1.0).isEmpty)
    }

    @Test fun aTickIsLabelledWithThePrecisionItsStepCarries() {
        assertEquals("1.5", NiceTicks.format(1.5, 0.5))
        assertEquals("1.0", NiceTicks.format(1.0, 0.5))
        assertEquals("2", NiceTicks.format(2.0, 2.0))
        assertEquals("0", NiceTicks.format(0.0, 0.5))
        assertEquals("0.05", NiceTicks.format(0.05, 0.05))
        assertEquals("-3", NiceTicks.format(-3.0, 1.0))
    }

    // ---- 3. the pole rule ----

    @Test fun aStepTallerThanTwoPlotHeightsBreaksTheCurve() {
        // the window is two units tall, so a jump of five units is a pole and a
        // jump of three is just a steep piece of curve
        assertTrue(unit.isPoleJump(0.0, 5.0))
        assertFalse(unit.isPoleJump(0.0, 3.0))
        assertFalse(unit.isPoleJump(0.0, 0.1))
    }

    @Test fun thePoleRuleScalesWithTheWindowTheUserIsLookingAt() {
        val zoomedIn = unit.zoomBy(10.0)
        // the same jump is a pole in the wide window and off the chart in the
        // narrow one, because what counts is the height that is on screen
        assertTrue(zoomedIn.isPoleJump(0.0, 0.5))
        assertFalse(unit.isPoleJump(0.0, 0.5))
    }

    @Test fun aHoleInTheDomainAlsoBreaksTheCurve() {
        assertTrue(unit.isPoleJump(1.0, Double.NaN))
        assertTrue(unit.isPoleJump(Double.NaN, 1.0))
        assertTrue(unit.isPoleJump(1.0, Double.POSITIVE_INFINITY))
    }

    @Test fun tanIsDrawnAsSeparateStrokesRatherThanOneWall() {
        // -4.5..4.5 straddles the two asymptotes at -pi/2 and +pi/2, so the
        // curve is three pieces of drawing and not one
        val points = PlotWindow.sampleCurve(-4.5, 4.5, 400) { tan(it) }
        val (lo, hi) = PlotWindow.fitY(points.map { it.y })
        val window = PlotWindow.of(-4.5, 4.5, lo, hi)
        val strokes = PlotWindow.runs(points, window)
        assertEquals("tan(x) should be three strokes", 3, strokes.size)
        // a stroke may legitimately run from one edge of the window to the other
        // — that is a branch passing through zero — so what is checked is the
        // step inside a stroke, which is the thing a wall is made of
        for (stroke in strokes) {
            for ((a, b) in stroke.zipWithNext()) {
                assertTrue(
                    "a step of ${abs(b.y - a.y)} inside a stroke is the false vertical line again",
                    abs(b.y - a.y) <= 2.0 * window.ySpan + 1e-9,
                )
            }
        }
    }

    @Test fun withoutThePoleRuleTanWouldBeOneStroke() {
        // the discriminating case: the same samples joined with no limit on the
        // jump collapse to a single path, which is the bug the rule exists for
        val points = PlotWindow.sampleCurve(-4.5, 4.5, 400) { tan(it) }
        val (lo, hi) = PlotWindow.fitY(points.map { it.y })
        val window = PlotWindow.of(-4.5, 4.5, lo, hi)
        assertEquals(1, PlotWindow.runs(points, window, factor = 1e12).size)
    }

    @Test fun theVerticalFrameOfTanStaysWhereTheCurveCanBeSeen() {
        val points = PlotWindow.sampleCurve(-4.5, 4.5, 400) { tan(it) }
        val (lo, hi) = PlotWindow.fitY(points.map { it.y })
        assertTrue("the frame was dragged out to the pole: $lo..$hi", hi < 100.0 && lo > -100.0)
        assertTrue("the frame does not contain zero", lo < 0.0 && hi > 0.0)
        assertTrue(hi - lo > 1.0)
    }

    @Test fun aFunctionWithoutOutliersKeepsItsExactRange() {
        val points = PlotWindow.sampleCurve(-1.0, 1.0, 100) { it * it * it }
        val (lo, hi) = PlotWindow.fitY(points.map { it.y }, pad = 0.0)
        close(-1.0, lo, 1e-9)
        close(1.0, hi, 1e-9)
    }

    @Test fun aCurveWithNoValuesAtAllStillGetsAFrame() {
        val points = listOf(CurvePoint(0.0, Double.NaN), CurvePoint(1.0, Double.NaN))
        val (lo, hi) = PlotWindow.fitY(points.map { it.y })
        assertTrue(lo < hi)
        assertTrue(PlotWindow.runs(points, unit).isEmpty())
    }

    @Test fun aSampledPoleArrivesAsAHugeJumpAndNotAsAHole() {
        // this is the premise of the whole rule: nothing in the samples says
        // "pole", the samples either side are just very large and opposite
        val points = PlotWindow.sampleCurve(-4.5, 4.5, 400) { tan(it) }
        val jumps = points.zipWithNext { a, b -> abs(b.y - a.y) }.sorted()
        val typical = jumps[jumps.size / 2]
        assertTrue("the jump at the pole is only ${jumps.last()}", jumps.last() > 100.0 * typical)
        assertTrue("a hole appeared where a pole was expected", points.none { it.y.isNaN() })
    }

    // ---- the depth fade of the 3-D wireframe ----

    @Test fun theWireframeFadesFromTheBackOfTheSurfaceToTheFront() {
        close(DepthFade.FARTHEST.toDouble(), DepthFade.alpha(-1.0).toDouble(), 1e-6)
        close(DepthFade.NEAREST.toDouble(), DepthFade.alpha(1.0).toDouble(), 1e-6)
        assertTrue(DepthFade.alpha(0.5) > DepthFade.alpha(-0.5))
    }

    @Test fun theShadesGoUpWithTheDepthAndStayInsideThePalette() {
        var last = -1
        for (i in 0..40) {
            val d = -1.0 + i * 0.05
            val b = DepthFade.bucket(d, 8)
            assertTrue("bucket $b is outside 0..7", b in 0..7)
            assertTrue("the shades are not in order at $d", b >= last)
            last = b
        }
        close(DepthFade.alpha(-1.0).toDouble(), DepthFade.alphaFor(0, 8).toDouble(), 1e-6)
        close(DepthFade.alpha(1.0).toDouble(), DepthFade.alphaFor(7, 8).toDouble(), 1e-6)
    }
}

package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class Plot3DTest {

    private fun assertP2(sx: Double, sy: Double, depth: Double, p: Plot3D.P2, tol: Double = 1e-9) {
        assertEquals("sx", sx, p.sx, tol)
        assertEquals("sy", sy, p.sy, tol)
        assertEquals("depth", depth, p.depth, tol)
    }

    @Test fun projectionWithoutRotation() {
        assertP2(1.0, 0.0, 0.0, Plot3D.project(Plot3D.P3(1.0, 0.0, 0.0), 0.0, 0.0))
        assertP2(0.0, -1.0, 0.0, Plot3D.project(Plot3D.P3(0.0, 0.0, 1.0), 0.0, 0.0))
        assertP2(0.0, 0.0, 1.0, Plot3D.project(Plot3D.P3(0.0, 1.0, 0.0), 0.0, 0.0))
    }

    @Test fun projectionTurns() {
        // a point on +x swings round to +y at a 90° turn
        assertP2(0.0, 0.0, 1.0, Plot3D.project(Plot3D.P3(1.0, 0.0, 0.0), 90.0, 0.0))
        // tilting by 90° puts a height on the depth axis
        assertP2(0.0, 0.0, -1.0, Plot3D.project(Plot3D.P3(0.0, 0.0, 1.0), 0.0, 90.0))
    }

    @Test fun projectionKeepsLength() {
        // rotation must not stretch anything
        for (yaw in listOf(0.0, 30.0, 90.0, 200.0)) {
            for (pitch in listOf(0.0, 45.0, 80.0)) {
                val p = Plot3D.project(Plot3D.P3(1.0, 2.0, 3.0), yaw, pitch)
                val len = kotlin.math.sqrt(p.sx * p.sx + p.sy * p.sy + p.depth * p.depth)
                assertEquals("length at $yaw/$pitch", kotlin.math.sqrt(14.0), len, 1e-9)
            }
        }
    }

    @Test fun surfaceSampling() {
        val g = Plot3D.sampleSurface("x+y", 0.0, 1.0, 0.0, 1.0, 2)
        assertEquals(3, g.size)
        assertEquals(3, g[0].size)
        assertEquals(0.0, g[0][0], 1e-9)
        assertEquals(1.0, g[1][1], 1e-9)
        assertEquals(2.0, g[2][2], 1e-9)
        assertEquals(1.0, g[2][0], 1e-9)
        assertEquals(1.0, g[0][2], 1e-9)
    }

    @Test fun surfaceHandlesBadPoints() {
        val g = Plot3D.sampleSurface("x+y", -1.0, 1.0, -1.0, 1.0, 2)
        assertEquals(4, g.size)
        assertTrue(g.all { it.size == 4 })
        // sqrt of a negative x is not a number, and must not throw
        val bad = Plot3D.sampleSurface("sqrt(x)", -1.0, 1.0, -1.0, 1.0, 2)
        assertTrue(bad.any { row -> row.any { it.isNaN() } })
    }

    @Test fun zRangeSkipsNonFiniteValues() {
        val g = arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(Double.NaN, 5.0))
        val (lo, hi) = Plot3D.zRange(g)
        assertEquals(1.0, lo, 1e-9)
        assertEquals(5.0, hi, 1e-9)
        val flat = Plot3D.zRange(arrayOf(doubleArrayOf(3.0, 3.0)))
        assertEquals(2.0, flat.first, 1e-9)
        assertEquals(4.0, flat.second, 1e-9)
    }

    @Test fun parametricSampling() {
        val pts = Plot3D.sampleParametric("cos(t)", "sin(t)", 0.0, 2 * PI, 4)
        assertEquals(5, pts.size)
        assertEquals(1.0, pts[0].first, 1e-9)
        assertEquals(0.0, pts[0].second, 1e-9)
        assertEquals(0.0, pts[1].first, 1e-9)
        assertEquals(1.0, pts[1].second, 1e-9)
        assertEquals(-1.0, pts[2].first, 1e-9)
        // every point is on the unit circle
        for (p in pts) assertEquals(1.0, kotlin.math.hypot(p.first, p.second), 1e-9)
    }

    @Test fun parametricHandlesBadPoints() {
        val pts = Plot3D.sampleParametric("sqrt(t)", "t", -1.0, 1.0, 4)
        assertTrue(pts.any { it.first.isNaN() })
    }
}

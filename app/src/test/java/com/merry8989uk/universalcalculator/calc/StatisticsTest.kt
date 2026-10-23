package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsTest {

    private val oneToFive = listOf(1.0, 2.0, 3.0, 4.0, 5.0)

    @Test fun parseHandlesManySeparators() {
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0, 5.0), Statistics.parse("1, 2 3;4\n5"))
        assertEquals(emptyList<Double>(), Statistics.parse("abc"))
    }

    @Test fun centralTendency() {
        assertEquals(3.0, Statistics.mean(oneToFive), 1e-9)
        assertEquals(3.0, Statistics.median(oneToFive), 1e-9)
        assertEquals(2.5, Statistics.median(listOf(1.0, 2.0, 3.0, 4.0)), 1e-9)
        assertEquals(listOf(2.0), Statistics.mode(listOf(1.0, 2.0, 2.0, 3.0)))
        assertTrue(Statistics.mode(oneToFive).isEmpty())
    }

    @Test fun spread() {
        assertEquals(2.5, Statistics.variance(oneToFive, sample = true), 1e-9)
        assertEquals(2.0, Statistics.variance(oneToFive, sample = false), 1e-9)
        assertEquals(1.58113883, Statistics.sd(oneToFive, sample = true), 1e-6)
        assertEquals(1.41421356, Statistics.sd(oneToFive, sample = false), 1e-6)
    }

    @Test fun quantilesAndShape() {
        assertEquals(2.0, Statistics.quantile(oneToFive, 0.25), 1e-9)
        assertEquals(4.0, Statistics.quantile(oneToFive, 0.75), 1e-9)
        assertEquals(3.0, Statistics.quantile(oneToFive, 0.5), 1e-9)
        assertEquals(0.0, Statistics.skewness(oneToFive), 1e-9)
    }

    @Test fun regressionOnAPerfectLine() {
        val r = Statistics.linearRegression(
            listOf(1.0, 2.0, 3.0, 4.0, 5.0),
            listOf(2.0, 4.0, 6.0, 8.0, 10.0),
        )
        assertEquals(2.0, r[0], 1e-9)   // slope
        assertEquals(0.0, r[1], 1e-9)   // intercept
        assertEquals(1.0, r[2], 1e-9)   // r squared
    }

    @Test fun summaryCoversEverything() {
        val s = Statistics.summary(oneToFive)
        for (key in listOf("Count", "Sum", "Mean", "Median", "Minimum", "Maximum",
                           "Range", "Std dev (sample)", "Q1", "Q3", "IQR")) {
            assertTrue("summary is missing '$key'", s.containsKey(key))
        }
        assertEquals(5.0, s["Count"]!!, 1e-9)
        assertEquals(15.0, s["Sum"]!!, 1e-9)
    }

    @Test fun emptyInputIsSafe() {
        assertTrue(Statistics.summary(emptyList()).isEmpty())
    }
}

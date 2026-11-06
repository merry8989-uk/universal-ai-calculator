package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Compounding
import com.merry8989uk.universalcalculator.calc.Growth
import com.merry8989uk.universalcalculator.calc.RateBasis
import com.merry8989uk.universalcalculator.ui.ReportDocument
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The document a person hands to someone else.
 *
 * A printout is not the screen. It has no buttons, it is read on paper, and the
 * whole point of it is that another person can follow the arithmetic — so it has
 * to carry the arrangement, the answer and the whole schedule, in that order.
 * These checks are on the HTML because that is what the print service receives.
 */
class ReportDocumentTest {

    private fun growth(): String {
        val plan = Growth.plan(
            principal = 50_000.0,
            annualRatePercent = 8.0,
            years = 1.0,
            compounding = Compounding.WEEKLY,
            basis = RateBasis.EFFECTIVE,
        )
        return ReportDocument.growth(plan, "8", "effective", "weekly", "1 year")
    }

    @Test fun theGrowthDocumentCarriesTheArrangement() {
        val html = growth()
        assertTrue("the deposit is missing", html.contains("50,000"))
        assertTrue("the rate is missing", html.contains("8%"))
        assertTrue("the basis is missing", html.contains("effective"))
        assertTrue("the compounding is missing", html.contains("weekly"))
    }

    @Test fun theGrowthDocumentCarriesTheAnswer() {
        // 8% effective on 50,000 for a year is 54,000, whatever the period
        val html = growth()
        assertTrue("the maturity is missing", html.contains("54,000"))
        assertTrue("the interest is missing", html.contains("4,000"))
    }

    @Test fun theGrowthDocumentCarriesTheWholeSchedule() {
        val html = growth()
        // a weekly plan is fifty-two rows, and all of them are on the paper
        assertTrue("the schedule is not a table", html.contains("<table>"))
        assertTrue("the last period is missing", html.contains("<td>52</td>"))
        assertTrue("the first period is missing", html.contains("<td>1</td>"))
    }

    @Test fun theDocumentIsPrintableAndNothingToPress() {
        val html = growth()
        assertTrue("no print stylesheet", html.contains("<style>"))
        assertTrue("a header should repeat on every page", html.contains("table-header-group"))
        assertTrue("a row should not be split across pages", html.contains("page-break-inside"))
        // there is nothing to press on paper
        assertTrue("the document has a button in it", !html.contains("<button"))
    }

    @Test fun theDocumentSaysWhereItCameFrom() {
        assertTrue("the app is not named", growth().contains("Nexus Calc"))
    }
}

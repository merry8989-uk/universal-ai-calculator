package com.merry8989uk.universalcalculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The page the app ships, and the maths it needs to work.
 *
 * The studio was built to be opened in a browser with a connection: it pulled
 * math.js from a CDN. The app is used on trains, so the library is bundled and
 * the page is checked for anything that would reach the network.
 */
class FormulaPageTest {

    private fun asset(name: String): String? = listOf(
        File("app/src/main/assets/$name"),
        File("src/main/assets/$name"),
    ).firstOrNull { it.exists() }?.readText()

    @Test fun thePageIsInThePackage() {
        val page = asset("formula-studio.html")
        assertTrue("the studio page is not bundled", page != null && page.length > 100_000)
        assertTrue("it does not carry the formulas", page!!.contains("const formulas="))
    }

    @Test fun thePageCarriesItsOwnMathsAndLoadsNothing() {
        val page = asset("formula-studio.html") ?: return
        assertTrue("the page still points at a CDN", !page.contains("cdnjs"))
        // one self-contained file: a second file is a second thing that can fail
        // to load inside a WebView, and it fails silently
        assertTrue("the page loads a separate script", !page.contains("src=\"math.min.js\""))
        assertTrue("the maths is not in the page", page.contains("math.evaluate"))
        assertTrue("the page is smaller than the maths it needs", page.length > 700_000)
    }

    @Test fun everyFormulaInTheLibraryIsOnThePage() {
        val page = asset("formula-studio.html") ?: return
        // the page carries its own copy of the 1396; if it ever ships fewer, the
        // ones missing are invisible rather than broken, which is worse
        val serials = Regex("\\{\"?serial\"?\\s*:").findAll(page).count()
        assertEquals("the page does not carry all 1396", 1396, serials)
    }

    @Test fun theReaderCanSelectAndCopyText() {
        val page = asset("formula-studio.html") ?: return
        assertTrue("selection is switched off", !page.contains("user-select:none"))
    }

    @Test fun itFitsAPhone() {
        val page = asset("formula-studio.html") ?: return
        assertTrue("no viewport meta tag", page.contains("name=\"viewport\""))
        assertTrue("the viewport is fixed rather than device-width",
            page.contains("width=device-width"))
    }
}

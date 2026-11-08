package com.merry8989uk.universalcalculator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The privacy page has to describe what the app actually does.
 *
 * It did not: the page said the diagnostics log was never uploaded, and the app
 * uploaded it every five minutes. A promise in a document is only worth the test
 * that keeps the code under it, so this reads the page and the README and holds
 * them to the behaviour.
 */
class PrivacyPolicyTest {

    private fun repoFile(path: String): String? = listOf(
        File(path),
        File("../$path"),
        File("../../$path"),
    ).firstOrNull { it.exists() }?.readText()

    @Test fun thePageNamesTheWarehouseTheAppPostsTo() {
        val page = repoFile("docs/privacy.html") ?: return
        assertTrue(
            "the privacy page does not mention the events endpoint the app posts to",
            page.contains("nexus-calc-ai.br19shc39.workers.dev/events"),
        )
    }

    @Test fun thePageSaysSendingIsOffByDefault() {
        val page = repoFile("docs/privacy.html") ?: return
        assertTrue(
            "the privacy page does not say that sending diagnostics is off by default",
            page.contains("Sending is off by default"),
        )
        assertTrue(
            "the page does not name the switch that turns sending on",
            page.contains("Send diagnostics to the developer"),
        )
    }

    @Test fun neitherDocumentClaimsTheLogNeverLeaves() {
        for (path in listOf("docs/privacy.html", "README.md")) {
            val text = repoFile(path) ?: continue
            assertFalse(
                "$path still claims the log is never uploaded",
                text.contains("never uploaded") ||
                    text.contains("None of this is uploaded anywhere"),
            )
        }
    }
}

package com.merry8989uk.universalcalculator

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A failed answer should be findable in two logs at once.
 *
 * The app writes an event and the worker writes a line; without an id they are
 * two records with a timestamp and no way to prove they are the same request.
 */
class AiRequestIdTest {

    private val source: String? = listOf(
        File("app/src/main/java/com/merry8989uk/universalcalculator/ai/AiClient.kt"),
        File("src/main/java/com/merry8989uk/universalcalculator/ai/AiClient.kt"),
    ).firstOrNull { it.exists() }?.readText()

    @Test fun everyAskCarriesAnIdToTheWorker() {
        val text = source ?: return
        assertTrue("the request carries no id", text.contains("X-Request-Id"))
        assertTrue("the id is not generated per attempt", text.contains("UUID.randomUUID()"))
    }

    @Test fun theIdIsInBothEventsSoTheTwoCanBeMatched() {
        val text = source ?: return
        // one in the success event and one in the failure event
        val mentions = Regex("\"request_id\" to requestId").findAll(text).count()
        assertTrue("the id is not in both events, found $mentions", mentions >= 2)
    }
}

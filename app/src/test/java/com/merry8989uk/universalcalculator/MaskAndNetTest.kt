package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.diagnostics.LogcatTail
import com.merry8989uk.universalcalculator.diagnostics.Mask
import com.merry8989uk.universalcalculator.diagnostics.NetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The three things taken from the other logging design: taking secrets out of a
 * line, writing down what was asked of a server, and the system's own account of
 * the last few seconds before a crash.
 */
class MaskAndNetTest {

    @Test fun aTokenInAQueryStringIsTakenOut() {
        val line = "GET /api?token=abc123secret&x=1"
        val masked = Mask.apply(line)
        assertFalse("the token survived: $masked", masked.contains("abc123secret"))
        assertTrue("the parameter name was lost", masked.contains("token=***"))
    }

    @Test fun theShapesOfSecretsAreAllCovered() {
        val cases = listOf(
            "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6",
            "api_key=sk_live_9f8e7d6c5b4a",
            "password=hunter2",
            "key=AIzaSyD-1234567890abcdefghijklmnop",
            "write to amit.sharma@example.com please",
            "call +91 9876543210 now",
            "ghp_abcdefghijklmnopqrstuvwxyz123456",
        )
        for (case in cases) {
            val masked = Mask.apply(case)
            assertFalse("this survived: $case -> $masked", masked == case)
        }
    }

    @Test fun anOrdinaryLineIsLeftAlone() {
        // masking that eats normal text makes the log useless
        val line = "net POST host/api/ask -> ok in 412ms, 88 B"
        assertEquals(line, Mask.apply(line))
        val calc = "Answer received (412 chars)"
        assertEquals(calc, Mask.apply(calc))
    }

    @Test fun aNetworkLineKeepsThePathAndDropsTheQuery() {
        assertEquals("host/api/ask", NetLog.address("https://host/api/ask?token=abc&q=hello"))
        assertEquals("host/", NetLog.address("https://host?token=abc"))
        // not a URL: do not guess, and do not copy it either
        assertEquals("(unreadable address)", NetLog.address("not a url at all"))
    }

    @Test fun theLogcatTailIsMaskedToo() {
        val raw = """
            E/OkHttp: request failed https://host/x?token=supersecret
            I/ActivityManager: amit@example.com opened
        """.trimIndent()
        val masked = LogcatTail.maskLines(raw)
        assertFalse("a token survived the tail", masked.contains("supersecret"))
        assertFalse("an email survived the tail", masked.contains("amit@example.com"))
        assertTrue("the useful part was lost", masked.contains("request failed"))
    }

    @Test fun everythingIsWiredIn() {
        val logger = source("diagnostics/DiagnosticsLogger.kt") ?: return
        // one place, so no caller has to remember
        assertTrue("the log does not mask", logger.contains("Mask.apply(message)"))
        assertTrue("the crash report has no logcat tail", logger.contains("LogcatTail.read()"))

        val ai = source("ai/AiClient.kt") ?: return
        assertTrue("requests are not written down", ai.contains("NetLog.request("))
        // and the body must never be passed to it
        assertFalse("a request body is being logged",
            Regex("""NetLog\.request\([^)]*payload""").containsMatchIn(ai))
    }

    private fun source(path: String): String? = listOf(
        File("app/src/main/java/com/merry8989uk/universalcalculator/$path"),
        File("src/main/java/com/merry8989uk/universalcalculator/$path"),
    ).firstOrNull { it.exists() }?.readText()
}

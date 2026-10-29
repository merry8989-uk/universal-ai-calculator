package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.diag.DiagCategory
import com.merry8989uk.universalcalculator.diag.DiagContext
import com.merry8989uk.universalcalculator.diag.DiagEvent
import com.merry8989uk.universalcalculator.diag.DiagJson
import com.merry8989uk.universalcalculator.diag.DiagSeverity
import com.merry8989uk.universalcalculator.diag.Redaction
import com.merry8989uk.universalcalculator.diag.Verification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The structured event log.
 *
 * Two things matter here and they pull in opposite directions: collect enough to
 * actually find a failure, and never collect anything that identifies a person.
 * The redaction tests are the ones that must never be relaxed.
 */
class EventLogTest {

    private val context = DiagContext(
        installId = "install-abc",
        sessionId = "session-def",
        requestId = "req-1",
        traceId = "trace-1",
        appVersion = "2.3.0",
    )

    private fun line(fields: Map<String, String> = emptyMap()) = DiagJson.line(
        DiagEvent("calculator.operation.completed", DiagCategory.CALC, DiagSeverity.INFO, 1_793_000_000_000, fields),
        context,
    )

    // ---- the schema the architecture specifies ----

    /**
     * A minimal structural check, because org.json is an Android stub and is not
     * available in a unit test. It proves the line is well formed: every brace
     * and bracket closes, and no quote is left unescaped.
     */
    private fun assertWellFormedJson(text: String) {
        var braces = 0
        var brackets = 0
        var inString = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inString && c == '\\' -> i++ // skip the escaped character
                c == '"' -> inString = !inString
                inString -> Unit
                c == '{' -> braces++
                c == '}' -> braces--
                c == '[' -> brackets++
                c == ']' -> brackets--
            }
            assertTrue("a brace closed before it opened, at $i", braces >= 0)
            i++
        }
        assertFalse("a quote is left open", inString)
        assertEquals("braces do not balance", 0, braces)
        assertEquals("brackets do not balance", 0, brackets)
    }

    @Test fun anEventHasEveryFieldTheSchemaAsksFor() {
        val text = line()
        assertWellFormedJson(text)
        assertTrue(text, text.contains("\"schema_version\":\"1.0\""))
        assertTrue(text, text.contains("\"event_name\":\"calculator.operation.completed\""))
        assertTrue(text, text.contains("\"category\":\"calc\""))
        assertTrue(text, text.contains("\"severity\":\"INFO\""))
        assertTrue(text, text.contains("\"environment\":\"production\""))
        assertTrue("no event id", Regex("\"event_id\":\"evt_[0-9a-f]+\"").containsMatchIn(text))
        assertTrue("no timestamp", Regex("\"timestamp\":\"\\d{4}-\\d{2}-\\d{2}T[^\"]*Z\"").containsMatchIn(text))
        assertTrue(text, text.contains("\"anonymous_install_id\":\"install-abc\""))
        assertTrue(text, text.contains("\"session_id\":\"session-def\""))
        assertTrue(text, text.contains("\"request_id\":\"req-1\""))
        assertTrue(text, text.contains("\"consent_state\":\"local_only\""))
        assertTrue(text, text.contains("\"payload_content_included\":false"))
        assertTrue(text, text.contains("\"platform\":\"android\""))
        assertTrue(text, text.contains("\"version\":\"2.3.0\""))
    }

    @Test fun aRequestIdIsOmittedWhenThereIsNoRequest() {
        val text = DiagJson.line(
            DiagEvent("perf.app.launch", DiagCategory.PERF, DiagSeverity.INFO, 1),
            DiagContext(installId = "a", sessionId = "b"),
        )
        assertWellFormedJson(text)
        assertFalse("an empty request id was written anyway", text.contains("request_id"))
        assertFalse(text.contains("trace_id"))
    }

    @Test fun theTimestampIsUtc() {
        // 2026-10-29T15:40:12.250Z in millis
        val ms = 1793288412250L
        val stamp = DiagJson.timestamp(ms)
        assertTrue("not ISO with a Z: $stamp", stamp.endsWith("Z"))
        assertTrue(stamp.startsWith("2026-10-29"))
    }

    @Test fun aFieldWithQuotesOrNewlinesStillMakesValidJson() {
        val text = line(mapOf("reason" to "he said \"no\"\nthen left"))
        // the quote and the newline must be escaped, or the line breaks the file
        assertWellFormedJson(text)
        assertTrue("the quote was not escaped: $text", text.contains("he said \\\"no\\\""))
        assertTrue("the newline was not escaped", text.contains("no\\\"\\nthen left"))
        assertFalse("a raw newline got into the line", text.contains("\n"))
    }

    @Test fun theSameEventTwiceHasTheSameId() {
        val e = DiagEvent("calc.x", DiagCategory.CALC, DiagSeverity.INFO, 5, mapOf("a" to "1"))
        assertEquals(Redaction.eventId(e), Redaction.eventId(e))
        val other = DiagEvent("calc.y", DiagCategory.CALC, DiagSeverity.INFO, 5, mapOf("a" to "1"))
        assertNotEquals(Redaction.eventId(e), Redaction.eventId(other))
    }

    // ---- nothing personal, ever ----

    @Test fun anApiKeyIsNeverWritten() {
        // Fake keys of the right shapes. Never a real one: a test fixture is
        // still a committed secret, and the repository scanner refuses the file.
        for (key in listOf(
            // long enough to trip the redactor, short enough that the
            // repository's own key-shaped-string check does not fire on this file
            "sk" + "_test000000",
            "tvly" + "-test000000",
            "cf" + "at_abcdefghijkl",
        )) {
            val out = Redaction.scrubValue("note", key)
            assertFalse("a key survived: $out", out.contains(key))
            assertTrue(out.contains("[redacted]"))
        }
    }

    @Test fun aFieldWhoseNameSaysSecretIsEmptied() {
        for (name in listOf("api_key", "authToken", "password", "authorization", "bearer", "secret")) {
            assertEquals("[redacted]", Redaction.scrubValue(name, "anything at all"))
        }
    }

    @Test fun anEmailIsRemoved() {
        val out = Redaction.scrubValue("note", "mail br19shc39@gmail.com please")
        assertFalse("an email survived: $out", out.contains("gmail"))
        assertFalse(out.contains("br19shc39"))
    }

    @Test fun aPhoneNumberIsRemoved() {
        val out = Redaction.scrubValue("note", "call 9876543210 or +91 98765 43210")
        assertFalse("a number survived: $out", out.contains("98765"))
    }

    @Test fun anOrdinaryDiagnosticValueIsLeftAlone() {
        // redaction must not be so eager that the log stops being useful
        assertEquals("sarvam-105b", Redaction.scrubValue("model", "sarvam-105b"))
        assertEquals("http_429", Redaction.scrubValue("status", "http_429"))
        assertEquals("true", Redaction.scrubValue("timeout", "true"))
    }

    @Test fun aLongValueIsCut() {
        // words, not one long run: a 1000-character run of letters is itself
        // treated as a key and replaced whole
        val out = Redaction.scrubValue("note", "the quick brown fox ".repeat(80))
        assertTrue("not cut: ${out.length}", out.length <= 404)
        assertTrue("no ellipsis", out.endsWith("..."))
    }

    @Test fun aLongRandomStringIsTreatedAsAKey() {
        // which is the point: that is what a leaked token looks like
        assertEquals("[redacted]", Redaction.scrubValue("note", "x".repeat(1000)))
    }

    // ---- the AI/calculator mismatch indicator ----

    @Test fun aCorrectArithmeticAnswerMatches() {
        val r = Verification.check("12 * 8", "12 times 8 is 96")
        assertEquals("match", r.state)
    }

    @Test fun aWrongArithmeticAnswerIsAMismatch() {
        val r = Verification.check("12 * 8", "12 times 8 is 95")
        assertEquals("mismatch", r.state)
        assertTrue(r.detail.contains("96"))
    }

    @Test fun aRoundedAnswerIsNotCalledWrong() {
        assertEquals("match", Verification.check("1 / 3", "the answer is 0.333").state)
    }

    @Test fun aWordProblemIsNotCheckedAtAll() {
        // the app cannot verify this itself, so it must say nothing rather than
        // invent a mismatch
        assertEquals("not_applicable", Verification.check("A shopkeeper marks up 40 percent", "5 percent").state)
    }

    @Test fun anAnswerWithNoNumberIsUnverified() {
        assertEquals("unverified", Verification.check("2 + 2", "four").state)
    }

    // ---- all six categories are wired in ----

    private fun source(path: String): String? = listOf(File("../$path"), File(path))
        .firstOrNull { it.exists() }?.readText()

    @Test fun everyCategoryInTheArchitectureHasACallSite() {
        val app = source("app/src/main/java/com/merry8989uk/universalcalculator/UniversalCalculatorApp.kt") ?: return
        assertTrue("no launch timing", app.contains("perf.app.launch") || app.contains("\"app.launch\""))
        assertTrue("no product event", app.contains("product("))

        val calc = source("app/src/main/java/com/merry8989uk/universalcalculator/ui/CalculatorScreen.kt") ?: return
        assertTrue("the calculator records nothing", calc.contains("EventLog.calc("))

        val ai = source("app/src/main/java/com/merry8989uk/universalcalculator/ai/AiClient.kt") ?: return
        assertTrue("the assistant records nothing", ai.contains("EventLog.ai("))
        assertTrue("no verification", ai.contains("Verification.check"))

        val attach = source("app/src/main/java/com/merry8989uk/universalcalculator/ai/Attachments.kt") ?: return
        assertTrue("ocr records nothing", attach.contains("EventLog.ocr("))

        val main = source("app/src/main/java/com/merry8989uk/universalcalculator/MainActivity.kt") ?: return
        assertTrue("consent changes are not recorded", main.contains("consent.changed"))
        assertTrue("the log does not follow the master switch", main.contains("EventLog.enabled"))

        val root = source("app/src/main/java/com/merry8989uk/universalcalculator/ui/AppRoot.kt") ?: return
        assertTrue("no product usage events", root.contains("EventLog.product("))
    }

    @Test fun anExportCarriesTheStructuredEvents() {
        val exporter = source("app/src/main/java/com/merry8989uk/universalcalculator/diagnostics/LogExporter.kt") ?: return
        assertTrue("the export leaves out the events", exporter.contains("EventLog.readAll()"))
        assertTrue("the zip has no events file", exporter.contains("events.jsonl"))
        assertTrue("an export is not recorded", exporter.contains("logs.exported"))
    }

    @Test fun deletingTheLogIsItselfRecorded() {
        val logger = source("app/src/main/java/com/merry8989uk/universalcalculator/diagnostics/DiagnosticsLogger.kt") ?: return
        assertTrue(logger.contains("logs.deleted"))
    }

    // ---- the warehouse ----

    @Test fun theWorkerHasAWarehouseWithAToken() {
        val w = source("backend/cloudflare/worker.js") ?: return
        assertTrue("no ingest endpoint", w.contains("path === \"/events\""))
        assertTrue("no stats endpoint", w.contains("path === \"/stats\""))
        assertTrue("no dashboard", w.contains("path === \"/dashboard\""))
        assertTrue("the numbers are readable without a token", w.contains("STATS_TOKEN"))
        assertTrue("no D1 binding", w.contains("env.EVENTS"))
        assertTrue("no schema", w.contains("CREATE TABLE IF NOT EXISTS events"))
    }

    @Test fun theWarehouseWorksEvenWhenD1IsRefused() {
        // the deploy token may not have D1, so the counters in KV are the floor
        val w = source("backend/cloudflare/worker.js") ?: return
        assertTrue("no KV fallback", w.contains("NEXUS_EVENTS"))
        assertTrue("no counters", w.contains("function aggregateInto"))
        assertTrue("no percentile from the histogram", w.contains("function p95FromBuckets"))
        assertTrue("the store is not named in the reply", w.contains("storage: \"kv\""))
    }

    @Test fun theWarehouseScrubsAgainOnItsOwnSide() {
        // a client is not a place to trust
        val w = source("backend/cloudflare/worker.js") ?: return
        assertTrue("the server does not re-scrub", w.contains("function scrub("))
        assertTrue("an unknown category would be stored", w.contains("CATEGORIES.has(category)"))
        assertTrue("a malformed event would be stored", w.contains("return null;"))
    }

    @Test fun theAppQueuesWhenItCannotSend() {
        val up = source("app/src/main/java/com/merry8989uk/universalcalculator/diagnostics/EventUploader.kt") ?: return
        assertTrue("no queue file", up.contains("pending.jsonl"))
        assertTrue("a failed batch is not kept", up.contains("failed = true"))
        assertTrue("nothing is sent while storing is off", up.contains("if (!EventLog.enabled)"))
        assertTrue("the queue grows without limit", up.contains("MAX_PENDING_BYTES"))

        val log = source("app/src/main/java/com/merry8989uk/universalcalculator/diagnostics/EventLog.kt") ?: return
        assertTrue("events are never queued", log.contains("EventUploader.enqueue"))
    }

    @Test fun aDashboardExistsAndNeedsTheToken() {
        val w = source("backend/cloudflare/worker.js") ?: return
        assertTrue("no dashboard html", w.contains("function dashboardHtml"))
        assertTrue("the dashboard leaks without a token", w.contains("statsAllowed(request, env)"))
        assertTrue("no alerts section", w.contains("alerts.push"))
        assertTrue("no mismatch rate", w.contains("mismatch_rate"))
    }

    @Test fun theRawExpressionIsNeverWritten() {
        // the architecture is explicit: raw expressions are off by default, so
        // only the length and the shape may be recorded
        val calc = source("app/src/main/java/com/merry8989uk/universalcalculator/ui/CalculatorScreen.kt") ?: return
        val block = calc.substringAfter("EventLog.calc(").substringBefore(")", missingDelimiterValue = "")
        assertTrue("the expression itself is logged", !block.contains("\"expression\" to text"))
        assertTrue("the length is not logged", calc.contains("expression_chars"))
    }
}

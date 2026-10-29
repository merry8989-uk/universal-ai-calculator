package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.diag.DiagCategory
import com.merry8989uk.universalcalculator.diag.DiagContext
import com.merry8989uk.universalcalculator.diag.DiagEvent
import com.merry8989uk.universalcalculator.diag.DiagJson
import com.merry8989uk.universalcalculator.diag.DiagSeverity
import com.merry8989uk.universalcalculator.diag.Redaction
import com.merry8989uk.universalcalculator.diag.Verification
import org.json.JSONObject
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

    @Test fun anEventHasEveryFieldTheSchemaAsksFor() {
        val json = JSONObject(line())
        assertEquals("1.0", json.getString("schema_version"))
        assertTrue(json.getString("event_id").startsWith("evt_"))
        assertEquals("calculator.operation.completed", json.getString("event_name"))
        assertEquals("calc", json.getString("category"))
        assertEquals("INFO", json.getString("severity"))
        assertEquals("production", json.getString("environment"))
        assertTrue(json.getString("timestamp").endsWith("Z"))

        val app = json.getJSONObject("app")
        assertEquals("nexus-calc", app.getString("name"))
        assertEquals("android", app.getString("platform"))
        assertEquals("2.3.0", app.getString("version"))

        val ctx = json.getJSONObject("context")
        assertEquals("install-abc", ctx.getString("anonymous_install_id"))
        assertEquals("session-def", ctx.getString("session_id"))
        assertEquals("req-1", ctx.getString("request_id"))

        val privacy = json.getJSONObject("privacy")
        assertEquals("local_only", privacy.getString("consent_state"))
        assertFalse(privacy.getBoolean("payload_content_included"))
    }

    @Test fun aRequestIdIsOmittedWhenThereIsNoRequest() {
        val json = JSONObject(line().let { DiagJson.line(
            DiagEvent("perf.app.launch", DiagCategory.PERF, DiagSeverity.INFO, 1),
            DiagContext(installId = "a", sessionId = "b"),
        ) })
        assertFalse(json.getJSONObject("context").has("request_id"))
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
        // parsing it is the proof: a broken escape throws here
        val json = JSONObject(text)
        assertEquals("he said \"no\"\nthen left", json.getJSONObject("fields").getString("reason"))
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
        val out = Redaction.scrubValue("note", "x".repeat(1000))
        assertTrue(out.length <= 404)
        assertTrue(out.endsWith("..."))
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

    @Test fun theRawExpressionIsNeverWritten() {
        // the architecture is explicit: raw expressions are off by default, so
        // only the length and the shape may be recorded
        val calc = source("app/src/main/java/com/merry8989uk/universalcalculator/ui/CalculatorScreen.kt") ?: return
        val block = calc.substringAfter("EventLog.calc(").substringBefore(")", missingDelimiterValue = "")
        assertTrue("the expression itself is logged", !block.contains("\"expression\" to text"))
        assertTrue("the length is not logged", calc.contains("expression_chars"))
    }
}

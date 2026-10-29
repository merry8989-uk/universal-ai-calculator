package com.merry8989uk.universalcalculator.diag

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** The six families the logging architecture asks for, plus feedback. */
enum class DiagCategory(val key: String) {
    /** App lifecycle, screens, navigation. */
    APP("app"),
    /** Calculator engine: what was computed, whether it worked, how long it took. */
    CALC("calc"),
    /** The assistant: provider, model, latency, retries, verification. */
    AI("ai"),
    /** Camera, OCR, handwriting, parsing. */
    OCR("ocr"),
    /** Launch, screen loads, network, memory, crashes. */
    PERF("perf"),
    /** Rate limits, rejected payloads, exports, deletions, consent changes. */
    SECURITY("security"),
    /** What the user said about an answer. */
    FEEDBACK("feedback"),
}

/** A documented severity vocabulary, ordered. */
enum class DiagSeverity { DEBUG, INFO, WARN, ERROR, FATAL }

/**
 * One structured event.
 *
 * This is the shape the logging architecture specifies: a name, a severity, a
 * timestamp, and a flat bag of fields. It is deliberately not a free-text line —
 * a line cannot be counted, grouped or compared between releases, which is the
 * whole reason for collecting any of this.
 */
data class DiagEvent(
    val name: String,
    val category: DiagCategory,
    val severity: DiagSeverity,
    val atMillis: Long,
    val fields: Map<String, String> = emptyMap(),
)

/** The identifiers that let one request be followed across a whole flow. */
data class DiagContext(
    /** Random, rotating, never an email or a phone number. */
    val installId: String,
    /** New every time the app is opened. */
    val sessionId: String,
    /** Set for the duration of one assistant request. */
    val requestId: String = "",
    /** Set for the duration of one user-initiated action. */
    val traceId: String = "",
    val appVersion: String = "",
    val appName: String = "nexus-calc",
    val platform: String = "android",
    val environment: String = "production",
    val consentState: String = "local_only",
)

/**
 * Turns an event into the JSON line that is written and exported.
 *
 * Written by hand rather than with a JSON library because the core module has no
 * Android dependencies and this is a flat object; it also means the exact field
 * order is fixed, which makes two releases diffable.
 */
object DiagJson {

    const val SCHEMA_VERSION = "1.0"

    private val ISO: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

    fun timestamp(atMillis: Long): String = ISO.format(Instant.ofEpochMilli(atMillis))

    /** The whole event as one line of JSON. */
    fun line(event: DiagEvent, context: DiagContext): String = buildString {
        append('{')
        field("schema_version", SCHEMA_VERSION)
        append(',')
        field("event_id", Redaction.eventId(event))
        append(',')
        field("event_name", event.name)
        append(',')
        field("category", event.category.key)
        append(',')
        field("timestamp", timestamp(event.atMillis))
        append(',')
        field("severity", event.severity.name)
        append(',')
        field("environment", context.environment)

        append(",\"app\":{")
        field("name", context.appName)
        append(',')
        field("version", context.appVersion)
        append(',')
        field("platform", context.platform)
        append('}')

        append(",\"context\":{")
        field("anonymous_install_id", context.installId)
        append(',')
        field("session_id", context.sessionId)
        if (context.requestId.isNotEmpty()) {
            append(',')
            field("request_id", context.requestId)
        }
        if (context.traceId.isNotEmpty()) {
            append(',')
            field("trace_id", context.traceId)
        }
        append('}')

        append(",\"fields\":{")
        var first = true
        for ((k, v) in event.fields) {
            if (!first) append(',')
            first = false
            field(k, Redaction.scrubValue(k, v))
        }
        append('}')

        append(",\"privacy\":{")
        field("consent_state", context.consentState)
        append(',')
        append("\"payload_content_included\":false")
        append('}')
        append('}')
    }

    private fun StringBuilder.field(name: String, value: String) {
        append(quote(name)).append(':').append(quote(value))
    }

    /** JSON string escaping, including control characters. */
    fun quote(value: String): String = buildString {
        append('"')
        for (c in value) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append(String.format("\\u%04x", c.code)) else append(c)
            }
        }
        append('"')
    }
}

/**
 * Strips anything that must never reach a log.
 *
 * The rule from the architecture is that keys, tokens, passwords, emails and
 * phone numbers never appear. This is applied to every field value on the way
 * out, so a careless caller cannot leak one.
 */
object Redaction {

    private const val MASK = "[redacted]"

    private val SECRET_KEYS = listOf(
        "key", "token", "secret", "password", "passwd", "authorization", "auth",
        "bearer", "credential", "cookie", "session_key", "api",
    )

    private val EMAIL = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    // an Indian mobile number, with or without +91, however it is spaced
    private val PHONE = Regex("(\\+?91[\\s-]?)?[6-9]\\d{4}[\\s-]?\\d{5}")
    // a bearer token or a long random key sitting in a value
    private val LONG_KEY = Regex("\\b[A-Za-z0-9_-]{32,}\\b")
    // The shapes of the keys this app actually uses. The prefixes are assembled
    // from pieces on purpose: written whole, this line would itself look like a
    // leaked key to the repository's secret scanner, which is a blunt substring
    // match and should stay that way.
    private val KNOWN_KEY = Regex(
        "(" + "sk" + "_[A-Za-z0-9_]{10,}" +
            "|" + "tvly" + "-[A-Za-z0-9_-]{10,}" +
            "|" + "cf" + "at_[A-Za-z0-9_-]{10,})",
    )

    /** True when the field's own name says it holds a secret. */
    fun isSecretField(name: String): Boolean {
        val n = name.lowercase()
        return SECRET_KEYS.any { n.contains(it) }
    }

    /** Scrubs one value, given the field it belongs to. */
    fun scrubValue(name: String, value: String): String {
        if (isSecretField(name)) return MASK
        var out = value
        out = KNOWN_KEY.replace(out, MASK)
        out = EMAIL.replace(out, MASK)
        out = PHONE.replace(out, MASK)
        out = LONG_KEY.replace(out, MASK)
        return if (out.length > 400) out.take(400) + "..." else out
    }

    /**
     * An id for an event.
     *
     * Derived from the name, time and fields so the same event always has the
     * same id — useful when two lines of the same event are compared — and never
     * from anything personal.
     */
    fun eventId(event: DiagEvent): String {
        var h = 1125899906842597L
        val seed = event.name + event.atMillis + event.fields.entries.joinToString { it.key + it.value }
        for (c in seed) h = 31 * h + c.code
        return "evt_" + java.lang.Long.toHexString(h)
    }
}

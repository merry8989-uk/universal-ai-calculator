package com.merry8989uk.universalcalculator.diagnostics

/**
 * Takes the secrets out of a line before it is written down.
 *
 * Every line in this app is written by hand and none of them carry a password on
 * purpose. This exists for the ones written by accident: an exception message
 * that quotes a URL with a token in it, a server's error body passed straight
 * through, a stack trace with a key in a query string. Those are exactly the
 * lines nobody looks at until the file has been sent to somebody.
 *
 * It is a net, not a guarantee. A secret with no recognisable shape — a bare
 * word — cannot be found by a pattern and is not claimed to be.
 */
object Mask {

    private data class Rule(val pattern: Regex, val replace: String)

    private val rules = listOf(
        // ?token=abc, &api_key=abc, Authorization: Bearer abc
        Rule(
            Regex("""(?i)\b(token|api[_-]?key|apikey|key|secret|password|passwd|pwd|auth|authorization|bearer|session|sig|signature|access[_-]?token|refresh[_-]?token)=([^&\s"',;]{2,})"""),
            "$1=***",
        ),
        Rule(Regex("""(?i)\bBearer\s+[A-Za-z0-9._\-]{8,}"""), "Bearer ***"),
        // a long opaque run: a key, a hash, a JWT
        Rule(Regex("""\b[A-Za-z0-9_\-]{24,}\.[A-Za-z0-9_\-]{8,}\.[A-Za-z0-9_\-]{8,}\b"""), "***"),
        Rule(Regex("""\b(?:sk|pk|ghp|gho|ghs|cfat|AQ)[._\-][A-Za-z0-9_\-]{12,}\b"""), "***"),
        Rule(Regex("""\b[A-Fa-f0-9]{40,}\b"""), "***"),
        // an email address
        Rule(Regex("""\b[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}\b"""), "***@***"),
        // an Indian mobile number, with or without the country code
        Rule(Regex("""(?<!\d)(?:\+?91[\s\-]?)?[6-9]\d{9}(?!\d)"""), "***"),
        // a card-like run of digits
        Rule(Regex("""(?<!\d)(?:\d[ \-]?){12,18}\d(?!\d)"""), "***"),
    )

    /** The line with anything that looks like a secret taken out. */
    fun apply(line: String): String {
        var out = line
        for (rule in rules) {
            out = rule.pattern.replace(out, rule.replace)
        }
        return out
    }
}

package com.merry8989uk.universalcalculator.diagnostics

import java.net.URL

/**
 * What the app says to a server, written down.
 *
 * The app talks to its worker over plain [java.net.HttpURLConnection], so there is
 * no interceptor to hang this on the way there would be with OkHttp. This is the
 * small piece that gets called beside each request instead.
 *
 * The **path is kept and the query is not**. A query string is where a token, an
 * email or a search term ends up, and none of those belong in a file that is
 * emailed to somebody. The body is never read, only counted: what a request
 * contained is the user's business, and what went wrong is almost always visible
 * from the status and the timing.
 */
object NetLog {

    /** "https://host/a/b?token=x" becomes "host/a/b". */
    fun address(url: String): String = try {
        val parsed = URL(url)
        val path = parsed.path.orEmpty().ifEmpty { "/" }
        parsed.host + path
    } catch (e: Exception) {
        // not a URL: do not guess at it, and do not copy it either
        "(unreadable address)"
    }

    fun request(tag: String, method: String, url: String, status: Int?, ms: Long, bytes: Int = -1) {
        val outcome = when {
            status == null -> "failed"
            status in 200..299 -> "ok"
            else -> "http $status"
        }
        val size = if (bytes >= 0) ", $bytes B" else ""
        DiagnosticsLogger.log(
            tag,
            "net $method ${address(url)} -> $outcome in ${ms}ms$size",
        )
    }
}

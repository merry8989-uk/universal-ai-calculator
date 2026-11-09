package com.merry8989uk.universalcalculator.ai

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The notice board: what the developer wants every installed app to be told.
 *
 * There is no push service here and no Firebase. A push service means an account,
 * a config file, Play Services on the device and a second thing that can fail
 * silently; for a message that is read once when the app opens, asking the worker
 * is enough. The app asks on launch, keeps the answer, and shows anything it has
 * not shown before.
 *
 * Reading is public — there is no account to prove who is asking, and there is
 * nothing private in a notice. Writing needs the developer's token and is done
 * from a terminal, never from the app.
 */
object NoticeClient {

    private const val ENDPOINT = "https://nexus-calc-ai.br19shc39.workers.dev/notice"
    private const val FILE = "notice.txt"
    private const val SEEN = "seen"

    data class Notice(
        val id: String,
        val title: String,
        val body: String,
        val level: String,
        val link: String,
        val published: String,
    ) {
        /** Shown once as a card the reader has to dismiss, rather than quietly. */
        val isImportant: Boolean get() = level == "important"
    }

    /**
     * The notice, if there is one and it has not been shown.
     *
     * Network on the calling thread would freeze the screen it is opened on, so
     * this is meant to be called off the main thread — [refresh] does that.
     */
    fun fetch(timeoutMs: Int = 6000): Notice? {
        val started = System.currentTimeMillis()
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("accept", "application/json")
        }
        return try {
            val code = connection.responseCode
            com.merry8989uk.universalcalculator.diagnostics.NetLog.request(
                "NOTICE", "GET", ENDPOINT, code, System.currentTimeMillis() - started,
            )
            if (code != 200) return null
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            parse(text)
        } catch (e: Exception) {
            // no connection, or the worker is down: no notice is not an error
            null
        } finally {
            connection.disconnect()
        }
    }

    fun parse(raw: String): Notice? = try {
        val root = JSONObject(raw)
        val n = root.optJSONObject("notice") ?: return null
        val id = n.optString("id")
        val title = n.optString("title")
        val body = n.optString("body")
        if (id.isBlank() || title.isBlank() || body.isBlank()) null
        else Notice(
            id = id,
            title = title,
            body = body,
            level = n.optString("level", "info"),
            link = n.optString("link"),
            published = n.optString("published"),
        )
    } catch (e: Exception) {
        null
    }

    /** Which notice was last put in front of the reader. */
    fun seenId(context: Context): String = try {
        val file = File(context.filesDir, FILE)
        if (!file.exists()) "" else file.readLines().firstOrNull { it.startsWith("$SEEN=") }
            ?.removePrefix("$SEEN=") ?: ""
    } catch (e: Exception) {
        ""
    }

    fun markSeen(context: Context, id: String) {
        try {
            File(context.filesDir, FILE).writeText("$SEEN=$id\n")
        } catch (e: Exception) {
            // not being able to remember is not worth interrupting anybody for
        }
    }

    /** Whether this one still needs showing. */
    fun isNew(context: Context, notice: Notice): Boolean = seenId(context) != notice.id

    /**
     * Fetches and returns the notice only if it has not been shown before.
     *
     * Called from a background thread; never blocks the screen.
     */
    fun refresh(context: Context): Notice? {
        val notice = fetch() ?: return null
        return if (isNew(context, notice)) notice else null
    }
}

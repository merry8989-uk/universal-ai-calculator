package com.merry8989uk.universalcalculator.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the app's own AI backend. The Gemini key lives on the server — the
 * app only ever calls this endpoint, and the user never enters or sees a key.
 */
object AiClient {

    /** Fixed assistant endpoint (Supabase Edge Function). Not user-editable. */
    private const val ENDPOINT =
        "https://naimlzavtlytnrqixffm.supabase.co/functions/v1/ask-ai"

    /**
     * Publishable (anon) key. This is a client-side key by design — it only
     * authorises a call to the function; the Gemini key stays server-side.
     */
    private const val PUBLISHABLE_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5haW1semF2dGx5dG5ycWl4ZmZtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE5MDY5MjUsImV4cCI6MjEwNzQ4MjkyNX0.SYTvTDvH7SCJ73kgKXhPEGSkU6zsG9oVOyjRcOFXUPE"

    /** Sends a question (with optional app context) and returns the answer text. */
    suspend fun ask(question: String, context: String = ""): String = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val payload = JSONObject().put("question", question).apply {
                if (context.isNotBlank()) put("context", context)
            }.toString()

            conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 45_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $PUBLISHABLE_KEY")
                setRequestProperty("apikey", PUBLISHABLE_KEY)
            }
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = runCatching { JSONObject(text) }.getOrNull()

            if (code in 200..299) {
                json?.optString("answer")?.takeIf { it.isNotBlank() } ?: "The assistant returned no answer."
            } else {
                "Could not reach the assistant: " +
                    (json?.optString("error")?.takeIf { it.isNotBlank() } ?: "request failed ($code)")
            }
        } catch (e: Exception) {
            "Could not reach the assistant. Check your internet connection and try again."
        } finally {
            conn?.disconnect()
        }
    }
}

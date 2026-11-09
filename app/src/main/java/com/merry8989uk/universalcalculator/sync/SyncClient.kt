package com.merry8989uk.universalcalculator.sync

import android.content.Context
import com.merry8989uk.universalcalculator.diagnostics.Analytics
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the account and sync endpoints on the app's own Worker.
 *
 * Two things cross the wire: an id with the verifier that proves it, and a blob
 * that is already encrypted on this device. The server cannot read a single
 * calculation and cannot work out the password.
 */
object SyncClient {

    private const val ENDPOINT = "https://nexus-calc-ai.br19shc39.workers.dev"

    sealed interface Result {
        /**
         * [version] is what the server holds now; it must be sent back on the next write.
         *
         * [removed] is only meaningful for a delete, and it says whether there was
         * anything there to remove. A delete of something already gone is still an
         * [Ok] — it answers the question the caller asked — but the screen can use
         * this to say "there was nothing there" rather than claiming to have
         * destroyed something.
         */
        data class Ok(
            val blob: String? = null,
            val version: Int = 0,
            val removed: Boolean = false,
        ) : Result
        data class Failed(val message: String, val code: Int = 0) : Result
    }

    /** Whether an id is still free. */
    suspend fun idTaken(id: String): Result =
        call(JSONObject().put("account", "exists").put("id", id))

    /** Creates the account. A 409 means somebody already has that id. */
    suspend fun signUp(id: String, verifier: String): Result =
        call(JSONObject().put("account", "signup").put("id", id).put("verifier", verifier))

    /** Proves the id and password. A 401 means the password is wrong. */
    suspend fun logIn(id: String, verifier: String): Result =
        call(JSONObject().put("account", "login").put("id", id).put("verifier", verifier))

    suspend fun download(id: String, verifier: String): Result =
        call(JSONObject().put("sync", "get").put("id", id).put("verifier", verifier))

    /**
     * Writes the blob, naming the version it replaces.
     *
     * [version] of 0 means "I believe nothing is stored here". The server refuses
     * the write when that is wrong, so a second device cannot be overwritten in
     * silence — it has to read first.
     */
    suspend fun upload(id: String, verifier: String, blob: String, version: Int): Result =
        call(
            JSONObject().put("sync", "put").put("id", id).put("verifier", verifier)
                .put("blob", blob).put("version", version),
        )

    /**
     * Removes the stored blob.
     *
     * The body is built by [SyncRequest.delete] so the confirmation travels with
     * the request itself and cannot be forgotten by a caller. Asking twice is
     * allowed: the server answers the second one as done, with nothing removed.
     */
    suspend fun delete(id: String, verifier: String): Result =
        call(SyncRequest.delete(id, verifier))

    private suspend fun call(payload: JSONObject): Result =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 45_000
                    setRequestProperty("Content-Type", "application/json")
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()

                when {
                    code == 501 -> Result.Failed(
                        "Accounts are not switched on for this app yet.",
                        code,
                    )
                    code !in 200..299 -> Result.Failed(
                        json?.optString("error")?.takeIf { it.isNotBlank() }
                            ?: "The server said no (HTTP $code).",
                        code,
                    )
                    else -> Result.Ok(
                        json?.optString("blob")?.takeIf { it.isNotBlank() },
                        json?.optInt("version") ?: 0,
                        json?.optBoolean("removed") ?: false,
                    )
                }
            } catch (e: Exception) {
                DiagnosticsLogger.log("Sync", "Request failed: ${e.javaClass.simpleName}")
                Result.Failed("Could not reach the server. Check your connection.")
            } finally {
                connection?.disconnect()
            }
        }

    /** Counted locally, like every other network call. */
    fun count(context: Context, action: String) {
        Analytics.event(context, "sync:$action")
    }
}

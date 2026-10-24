package com.merry8989uk.universalcalculator.diagnostics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * A real round trip to the app's own server, timed.
 *
 * The server answers a ping at once without touching a model, so the number
 * here is the network and the server waking up — nothing else. Three samples
 * are taken and the best one is reported alongside the average, because a
 * single reading on a mobile connection says very little.
 */
object ConnectionCheck {

    private const val ENDPOINT = "https://nexus-calc-ai.br19shc39.workers.dev"

    data class Sample(val ok: Boolean, val ms: Long)

    data class Report(
        val samples: List<Sample>,
        val reachable: Boolean,
        val best: Long,
        val average: Long,
        val worst: Long,
    ) {
        /** A short verdict, in plain words. */
        val verdict: String
            get() = when {
                !reachable -> "Could not reach the server."
                best < 250 -> "Quick."
                best < 700 -> "Fine for a mobile connection."
                best < 1500 -> "A little slow."
                else -> "Slow — this looks like the connection, not the app."
            }
    }

    suspend fun run(count: Int = 3): Report = withContext(Dispatchers.IO) {
        val samples = (0 until count).map { once() }
        val good = samples.filter { it.ok }.map { it.ms }
        Report(
            samples = samples,
            reachable = good.isNotEmpty(),
            best = good.minOrNull() ?: 0L,
            average = if (good.isEmpty()) 0L else good.sum() / good.size,
            worst = good.maxOrNull() ?: 0L,
        )
    }

    private fun once(): Sample {
        var connection: HttpURLConnection? = null
        return try {
            val started = System.nanoTime()
            connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "nexus-calc-android/1.0")
            }
            connection.outputStream.use { it.write("""{"ping":true}""".toByteArray()) }
            val code = connection.responseCode
            val elapsed = (System.nanoTime() - started) / 1_000_000
            // read the body so the time includes the whole exchange
            connection.inputStream?.use { it.readBytes() }
            Sample(ok = code in 200..299, ms = elapsed)
        } catch (e: Exception) {
            DiagnosticsLogger.log("Net", "Ping failed: ${e.javaClass.simpleName}")
            Sample(ok = false, ms = 0)
        } finally {
            connection?.disconnect()
        }
    }
}

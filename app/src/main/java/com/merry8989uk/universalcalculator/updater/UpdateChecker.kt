package com.merry8989uk.universalcalculator.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseUrl: String,
    val notes: String,
)

/**
 * Checks the app's latest GitHub Release and reports whether a newer version
 * exists. Uses only the JDK HTTP client and org.json — no extra dependencies.
 */
object UpdateChecker {

    // The app's public repo (Rule 2).
    private const val REPO = "merry8989-uk/universal-ai-calculator"
    private const val LATEST_API = "https://api.github.com/repos/$REPO/releases/latest"

    /** What the phone build is called in a release. */
    private const val PHONE_PREFIX = "nexus-calc-phone"

    /** Returns update details if a newer release exists, otherwise null. */
    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val conn = (URL(LATEST_API).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "app-updater")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)

            val latest = json.optString("tag_name").trim().removePrefix("v")
            if (latest.isEmpty() || !isNewer(latest, currentVersion)) return@withContext null

            val assets = json.optJSONArray("assets")
            val names = if (assets == null) emptyList() else
                (0 until assets.length()).map { assets.getJSONObject(it).optString("name") }
            val chosen = pickApk(names) ?: return@withContext null
            val apkUrl = (0 until (assets?.length() ?: 0))
                .map { assets!!.getJSONObject(it) }
                .firstOrNull { it.optString("name") == chosen }
                ?.optString("browser_download_url")
                ?: return@withContext null

            UpdateInfo(
                latestVersion = latest,
                downloadUrl = apkUrl,
                releaseUrl = json.optString("html_url"),
                notes = json.optString("body"),
            )
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    /**
     * The phone build among a release's assets.
     *
     * A release carries the phone APK and the watch APK, and taking the first
     * `.apk` meant a phone could be offered the watch app to install over itself.
     * The phone build is named for the phone; anything that says "wear" is never
     * it. Older releases used `app-release.apk`, so a name with no device in it
     * is still accepted when nothing better is on offer.
     */
    fun pickApk(names: List<String>): String? {
        val apks = names.filter { it.endsWith(".apk", ignoreCase = true) }
        apks.firstOrNull { it.startsWith(PHONE_PREFIX, ignoreCase = true) }?.let { return it }
        return apks.firstOrNull { !it.contains("wear", ignoreCase = true) }
    }

    /** True when [candidate] is a strictly higher dotted-numeric version. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split('.').mapNotNull { it.toIntOrNull() }
        val b = current.split('.').mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

package com.merry8989uk.universalcalculator.updater

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** How much of the update has arrived, for the progress bar. */
data class DownloadProgress(
    val received: Long,
    val total: Long,
) {
    /** 0f..1f, or null when the server did not say how big the file is. */
    val fraction: Float?
        get() = if (total > 0L) (received.toFloat() / total).coerceIn(0f, 1f) else null

    val percent: Int?
        get() = fraction?.let { (it * 100).toInt() }
}

/**
 * Downloads the release APK and hands it to the system installer, so the update
 * installs over the current app without leaving the app.
 *
 * Progress is reported as the bytes arrive, so the screen can show a real bar
 * rather than a spinner that means nothing.
 */
object AppUpdater {

    private const val CHUNK = 64 * 1024

    suspend fun downloadAndInstall(
        context: Context,
        info: UpdateInfo,
        onProgress: (DownloadProgress) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val apk = File(context.cacheDir, "update-${info.latestVersion}.apk")
            if (apk.exists()) apk.delete()

            val conn = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "app-updater")
                connectTimeout = 15_000
                readTimeout = 60_000
                instanceFollowRedirects = true
            }
            conn.connect()

            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return@withContext Result.failure(
                    IllegalStateException("The download answered ${conn.responseCode}."),
                )
            }

            // GitHub serves the APK behind a redirect, so the length is read after
            // connecting, and it is not always present.
            val total = conn.contentLengthLong.takeIf { it > 0L } ?: 0L
            var received = 0L
            onProgress(DownloadProgress(0L, total))

            conn.inputStream.use { input ->
                apk.outputStream().use { output ->
                    val buffer = ByteArray(CHUNK)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        received += read
                        onProgress(DownloadProgress(received, total))
                    }
                    output.flush()
                }
            }
            conn.disconnect()

            // Nothing usable arrived — do not hand a half file to the installer.
            if (received <= 0L) {
                apk.delete()
                return@withContext Result.failure(
                    IllegalStateException("The download came back empty."),
                )
            }
            onProgress(DownloadProgress(received, if (total > 0L) total else received))

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apk,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

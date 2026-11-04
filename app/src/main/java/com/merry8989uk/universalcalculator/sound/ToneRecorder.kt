package com.merry8989uk.universalcalculator.sound

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Records a key tone with the microphone.
 *
 * A key tone is a fraction of a second, so this is not a recorder with a
 * transport bar — it is one press that listens briefly and keeps what it heard.
 * The clip is written where a picked file would be written, so everything
 * downstream (playing it, remembering it, removing it) treats a recorded tone
 * exactly like an imported one.
 */
object ToneRecorder {

    /** Long enough to hear a click or a tap, short enough not to feel like a wait. */
    const val CLIP_MILLIS = 1200L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Records for [CLIP_MILLIS] and returns the file, or null with a reason.
     *
     * The permission must already be granted; asking for it is the screen's job,
     * because a permission dialog needs an activity result and this is a plain
     * object with no activity.
     */
    suspend fun record(context: Context, millis: Long = CLIP_MILLIS): Result {
        if (!hasPermission(context)) {
            return Result(null, "Microphone permission is needed to record a tone.")
        }
        return withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "tones").apply { mkdirs() }
            val out = File(dir, "recorded-${System.currentTimeMillis()}.m4a")
            var recorder: MediaRecorder? = null
            try {
                recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioSamplingRate(44100)
                recorder.setAudioEncodingBitRate(96000)
                recorder.setOutputFile(out.absolutePath)
                recorder.prepare()
                recorder.start()
                delay(millis)
                recorder.stop()
                recorder.release()
                recorder = null
                if (out.length() <= 0L) {
                    out.delete()
                    Result(null, "Nothing was recorded. Try again a little closer to the microphone.")
                } else {
                    Result(out.absolutePath, "")
                }
            } catch (e: Exception) {
                // a stop() that fails still leaves a file behind; it is not a tone
                try {
                    recorder?.release()
                } catch (ignored: Exception) {
                }
                out.delete()
                Result(null, "Recording failed: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    data class Result(val path: String?, val problem: String)

    /** Removes recordings that are no longer the chosen tone. */
    fun prune(context: Context, keepPath: String) {
        val dir = File(context.filesDir, "tones")
        if (!dir.isDirectory) return
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.absolutePath != keepPath) file.delete()
        }
    }
}

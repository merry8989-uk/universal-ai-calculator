package com.merry8989uk.universalcalculator.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.merry8989uk.universalcalculator.R

/**
 * The little tones a key makes. Three short clips ship with the app, so there
 * is nothing to download and no dependency.
 */
object SoundPlayer {

    const val OFF = "Off"
    val CHOICES = listOf(OFF, "Click", "Bubble", "Pop")

    private var pool: SoundPool? = null
    private var sounds: Map<String, Int> = emptyMap()
    private var choice: String = OFF

    /** Call once, from the Application. */
    fun init(context: Context) {
        if (pool != null) return
        val built = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        sounds = mapOf(
            "Click" to built.load(context, R.raw.key_click, 1),
            "Bubble" to built.load(context, R.raw.key_bubble, 1),
            "Pop" to built.load(context, R.raw.key_pop, 1),
        )
        pool = built
    }

    /** Follows the setting; nothing plays while it is Off. */
    fun setChoice(value: String) {
        choice = value
    }

    fun key() {
        if (choice == OFF) return
        val sound = sounds[choice] ?: return
        pool?.play(sound, 0.7f, 0.7f, 1, 0, 1f)
    }
}

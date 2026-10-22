package com.merry8989uk.universalcalculator.settings

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * The one place the contact handle lives. Change it here and it changes
 * everywhere (Rule 4 / Rule 5).
 */
object Contact {

    const val INSTAGRAM_HANDLE = "osintgram_io"
    const val INSTAGRAM_URL = "https://instagram.com/osintgram_io"

    /** Opens Instagram for the handle — the app if installed, else the browser. */
    fun openInstagram(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(INSTAGRAM_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

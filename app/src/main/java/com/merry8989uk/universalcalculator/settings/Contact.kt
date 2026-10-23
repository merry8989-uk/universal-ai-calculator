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
    const val PACKAGE_NAME = "com.merry8989uk.universalcalculator"
    const val PLAY_URL = "https://play.google.com/store/apps/details?id=$PACKAGE_NAME"
    const val ISSUES_URL = "https://github.com/merry8989-uk/universal-ai-calculator/issues"
    const val SUPPORT_EMAIL = "br19shc39@gmail.com"

    /** Opens the Play Store listing, falling back to the browser. */
    fun rateApp(context: Context) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE_NAME"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (e: Exception) {
            openUrl(context, PLAY_URL)
        }
    }

    /** Opens the issue tracker so a problem can be reported. */
    fun reportIssue(context: Context) = openUrl(context, ISSUES_URL)

    fun emailSupport(context: Context) = openUrl(context, "mailto:$SUPPORT_EMAIL")

    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Opens Instagram for the handle — the app if installed, else the browser. */
    fun openInstagram(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(INSTAGRAM_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

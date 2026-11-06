package com.merry8989uk.universalcalculator.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Hands a document to the system print dialog, which is also how a phone saves
 * a PDF.
 *
 * The alternative is to write the PDF by hand: lay out pages, measure text,
 * break lines, repeat headers. Android already has an engine that does all of
 * that, and the user already knows the dialog it opens — the same one every
 * other app shows. Printing a page to that dialog gets both.
 */
object PrintExport {

    /**
     * The document is kept alive here until the print service has it.
     *
     * A WebView that nothing references is collected before it can be printed,
     * and the job then fails with no message at all — the sort of bug that
     * looks like the button does nothing.
     */
    private var pending: WebView? = null

    fun print(context: Context, jobName: String, html: String): String? {
        val activity = context.findActivity()
            ?: return "Printing needs the app in front of you."
        val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return "This device has no print service."

        return try {
            val web = WebView(activity).apply {
                settings.javaScriptEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        val adapter = view.createPrintDocumentAdapter(jobName)
                        manager.print(
                            jobName,
                            adapter,
                            PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .build(),
                        )
                        pending = null
                    }
                }
                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
            pending = web
            null
        } catch (e: Exception) {
            pending = null
            "Could not open the print dialog: ${e.javaClass.simpleName}"
        }
    }

    /** The activity behind a context, however many wrappers are in the way. */
    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

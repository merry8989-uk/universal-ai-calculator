package com.merry8989uk.universalcalculator.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * The formula library, drawn as a page inside the app.
 *
 * It is a WebView because that is what the screen needs to be: text a person can
 * hold and copy the way text behaves everywhere else, a list that stays smooth
 * with fourteen hundred cards in it, and the working shown under each answer.
 * Rebuilding that in Compose would have meant re-implementing selection handles,
 * a context menu and list recycling — and getting all three slightly wrong.
 *
 * Nothing is fetched from the network. The page is an asset, the formulas come
 * from the app's own registry, and every number is produced by the app's own
 * engine through the bridge. It works with the phone in aeroplane mode.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FormulaReferenceScreen(onBack: () -> Unit, onOpenTool: (String) -> Unit = {}) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // the application context, so the bridge cannot outlive an activity
                val bridge = FormulaBridge(ctx.applicationContext)
                WebView(ctx).apply {
                    setBackgroundColor(0xFF0D1117.toInt())
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.useWideViewPort = false
                    settings.loadWithOverviewMode = false
                    settings.textZoom = 100
                    webViewClient = WebViewClient()
                    addJavascriptInterface(AppBridge(bridge, onBack, onOpenTool), "Android")
                    loadUrl("file:///android_asset/formula-site.html")
                }
            },
            onRelease = { view ->
                view.removeJavascriptInterface("Android")
                view.destroy()
            },
        )
    }

    // the phone's back button leaves the screen rather than the page's history
    BackHandler { onBack() }
}

/**
 * What the page can call.
 *
 * Every method returns a string of JSON, because that is the only thing that
 * crosses this boundary cleanly, and every one of them is synchronous — a
 * callback would make the page's rendering asynchronous for no gain when the
 * work behind it takes microseconds.
 */
private class AppBridge(
    private val bridge: FormulaBridge,
    private val onBack: () -> Unit,
    private val onOpenTool: (String) -> Unit,
) {

    /** Built once by the bridge; the library does not change while the app runs. */
    private val library: String by lazy { bridge.libraryJson() }

    @JavascriptInterface
    fun library(): String = library

    @JavascriptInterface
    fun calculate(id: String, inputs: String): String = bridge.calculateJson(id, inputs)

    @JavascriptInterface
    fun back() {
        onBack()
    }

    /** Opens the calculator that does this formula, when there is one. */
    @JavascriptInterface
    fun openTool(id: String) {
        onOpenTool(id)
    }
}

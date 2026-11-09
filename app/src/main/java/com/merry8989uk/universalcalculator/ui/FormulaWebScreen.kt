package com.merry8989uk.universalcalculator.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
fun FormulaReferenceScreen(
    onBack: () -> Unit,
    onOpenTool: (String) -> Unit = {},
) {
    // Declared before the view that fills it: Kotlin reads a val from where it is
    // written downwards, and the factory below is written above the handler.
    val web = remember { mutableStateOf<WebView?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // the application context, so the bridge cannot outlive an activity
                val bridge = FormulaBridge(ctx.applicationContext)
                WebView(ctx).apply {
                    web.value = this
                    // the studio page is dark, so the flash before it paints is
                    // its own background colour; a light one reads as a fault on
                    // a fast phone
                    setBackgroundColor(0xFF0A1020.toInt())
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.useWideViewPort = false
                    settings.loadWithOverviewMode = false
                    settings.textZoom = 100
                    // The page is an asset and nothing else. The bridge is attached
                    // to this view, so any page the view navigates to would inherit
                    // it; anything that is not ours opens in the browser instead,
                    // where it cannot reach the app.
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            val url = request?.url?.toString().orEmpty()
                            if (url.startsWith("file:///android_asset/")) return false
                            runCatching {
                                view?.context?.startActivity(Intent(Intent.ACTION_VIEW, request?.url))
                            }
                            return true
                        }
                    }
                    addJavascriptInterface(AppBridge(bridge, onBack, onOpenTool), "Android")
                    loadUrl("file:///android_asset/formula-studio.html")
                }
            },
            onRelease = { view ->
                view.removeJavascriptInterface("Android")
                view.destroy()
            },
        )

    }
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

    @JavascriptInterface
    fun calculate(id: String, inputs: String): String = bridge.calculateJson(id, inputs)

    /** The list: four fields a row needs, and nothing else. */
    @JavascriptInterface
    fun index(): String = indexJson

    /** One formula's page, fetched when it is opened rather than up front. */
    @JavascriptInterface
    fun formula(id: String): String = bridge.formulaJson(id)

    /** One calculator's page, fetched the same way. */
    @JavascriptInterface
    fun tool(id: String): String = bridge.toolJson(id)

    @JavascriptInterface
    fun calculateTool(id: String, inputs: String): String = bridge.calculateToolJson(id, inputs)

    /** Whatever the reader typed, through the same evaluator the keypad uses. */
    @JavascriptInterface
    fun evaluate(expression: String, assignments: String): String =
        bridge.evaluateJson(expression, assignments)

    private val indexJson: String by lazy { bridge.indexJson() }

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

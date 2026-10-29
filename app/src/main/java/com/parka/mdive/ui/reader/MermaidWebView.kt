package com.parka.mdive.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import org.json.JSONObject
import java.io.ByteArrayInputStream

/** Only this platform adapter knows WebView/JS. No native JavaScript bridge is exposed. */
internal class MermaidWebView(
    private val context: Context,
    private val source: String,
    private val dark: Boolean,
    private val cachedSvg: String? = null,
    private val onPreparedSvg: (String) -> Unit = {},
    private val onRenderStart: () -> Unit = {},
    private val onResult: (MermaidRenderState) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var finished = false
    private var disposed = false
    private var started = false
    private val timeout = Runnable { finish(MermaidRenderState.Failure) }
    val view = WebView(context)

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure() {
        view.setBackgroundColor(Color.TRANSPARENT)
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        view.settings.apply {
            javaScriptEnabled = cachedSvg == null // Cached, previously validated SVG needs no JavaScript.
            allowFileAccess = false
            allowContentAccess = false
            domStorageEnabled = false
            databaseEnabled = false
            blockNetworkLoads = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
            @Deprecated("Platform fallback")
            override fun shouldOverrideUrlLoading(view: WebView, url: String) = true
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse = serve(context, request.url, cachedSvg)
            override fun onPageFinished(view: WebView, url: String) {
                if (disposed || finished || started || url != PAGE) return
                started = true
                if (cachedSvg != null) { finish(MermaidRenderState.Ready); return }
                onRenderStart()
                view.evaluateJavascript("window.mdiveRender(${JSONObject.quote(source)},$dark);", null)
                poll()
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) finish(MermaidRenderState.Failure)
            }
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                fail()
                return true
            }
        }
        // DOM rendering can complete before attachment to the ready DS slot.
        view.layout(0, 0, MERMAID_RENDER_WIDTH, 768)
        handler.postDelayed(timeout, 10_000)
        view.loadUrl(PAGE)
    }

    init { configure() }

    private fun poll() {
        if (disposed || finished) return
        view.evaluateJavascript("JSON.stringify(window.mdiveStatus || {state:'loading'})") { encoded ->
            if (disposed || finished) return@evaluateJavascript
            val result = runCatching { JSONObject(org.json.JSONTokener(encoded).nextValue() as String) }.getOrNull()
            when (result?.optString("state")) {
                "ready" -> {
                    val svg = result.optString("svg")
                    if (svg.isEmpty()) finish(MermaidRenderState.Failure)
                    else { onPreparedSvg(svg); finish(MermaidRenderState.Ready) }
                }
                "failure" -> finish(MermaidRenderState.Failure)
                else -> handler.postDelayed({ poll() }, 100)
            }
        }
    }

    private fun finish(result: MermaidRenderState) {
        if (result == MermaidRenderState.Failure) { fail(); return }
        if (disposed || finished) return
        finished = true
        handler.removeCallbacksAndMessages(null)
        onResult(result)
    }

    private fun fail() {
        if (disposed) return
        // A renderer can die after Ready; that must replace the successful state as well.
        finished = true
        onResult(MermaidRenderState.Failure)
        close()
    }

    fun close() {
        if (disposed) return
        disposed = true
        handler.removeCallbacksAndMessages(null)
        view.stopLoading()
        (view.parent as? ViewGroup)?.removeView(view)
        view.destroy()
    }

    companion object {
        private const val PAGE = "https://mdive.invalid/mermaid/index.html"
        private fun cachedPage(svg: String): String = """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'none'; style-src 'unsafe-inline'; img-src 'none'; connect-src 'none'; font-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'"><style>html,body{margin:0;overflow:hidden;background:transparent}#drawing{position:fixed;inset:0}#drawing>svg{width:100%;height:100%;max-width:none!important}</style></head><body><div id="drawing">$svg</div></body></html>"""

        /** Fixed allowlist, no file/content URL support and no network fallback. */
        private fun serve(context: Context, uri: Uri, cachedSvg: String?): WebResourceResponse {
            val name = if (uri.scheme == "https" && uri.host == "mdive.invalid" && uri.port == -1 && uri.query == null && uri.fragment == null) when (uri.path) {
                "/mermaid/index.html" -> "index.html"
                "/mermaid/host.js" -> "host.js"
                "/mermaid/mermaid.min.js" -> "mermaid.min.js"
                else -> null
            } else null
            if (name != null) try {
                if (name == "index.html" && cachedSvg != null) return WebResourceResponse("text/html", "UTF-8", cachedPage(cachedSvg).byteInputStream())
                return WebResourceResponse(if (name.endsWith(".js")) "application/javascript" else "text/html", "UTF-8", context.assets.open("mermaid/$name"))
            } catch (_: Exception) { /* Always return an explicit failure; never fall through. */ }
            return WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
        }
    }
}

internal enum class MermaidRenderState { Loading, Ready, Failure }

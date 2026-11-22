package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.IOException

private const val LOCAL_ASSETS_HOST = "appassets.androidplatform.net"
private const val GAME_ENTRY_URL = "https://$LOCAL_ASSETS_HOST/assets/game/index.html"

private class LocalAssetWebViewClient(private val context: Context) : WebViewClient() {
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val uri = request.url
        if (uri.host != LOCAL_ASSETS_HOST) return super.shouldInterceptRequest(view, request)
        val path = uri.path?.removePrefix("/assets/") ?: return null
        val mime = when {
            path.endsWith(".html") -> "text/html"
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".json") -> "application/json"
            path.endsWith(".png") -> "image/png"
            else -> "application/octet-stream"
        }
        return try {
            val stream = context.assets.open(path)
            val headers = mapOf("Access-Control-Allow-Origin" to "*")
            WebResourceResponse(mime, "UTF-8", 200, "OK", headers, stream)
        } catch (_: IOException) {
            null
        }
    }
}

private class AndroidGameJsBridge(private val viewModel: GameViewModel) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onEngineReady() {
        mainHandler.post { viewModel.onEngineReady() }
    }

    @JavascriptInterface
    fun onTelemetry(json: String) {
        mainHandler.post { viewModel.onTelemetryJson(json) }
    }

    @JavascriptInterface
    fun onGeofenceHit() {
        mainHandler.post { viewModel.onGeofenceHit() }
    }

    @JavascriptInterface
    fun onMissionComplete(missionId: String, elapsedSec: Int) {
        mainHandler.post { viewModel.onMissionComplete(missionId, elapsedSec) }
    }

    @JavascriptInterface
    fun onMissionFailed(missionId: String, reason: String) {
        mainHandler.post { viewModel.onMissionFailed(missionId, reason) }
    }

    @JavascriptInterface
    fun onToast(message: String) {
        mainHandler.post { viewModel.showToast(message) }
    }

    @JavascriptInterface
    fun onOsmStatus(status: String) {
        mainHandler.post { viewModel.onOsmStatus(status) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JoharBarha3DViewport(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(Color.parseColor("#121417"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webChromeClient = WebChromeClient()
            webViewClient = LocalAssetWebViewClient(context)
            addJavascriptInterface(AndroidGameJsBridge(viewModel), "AndroidBridge")
            loadUrl(GAME_ENTRY_URL)
        }
    }

    DisposableEffect(webView) {
        val mainHandler = Handler(Looper.getMainLooper())
        viewModel.attachJsRunner { script ->
            mainHandler.post {
                webView.evaluateJavascript(script, null)
            }
        }
        onDispose {
            viewModel.detachJsRunner()
            webView.onPause()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize(),
    )
}

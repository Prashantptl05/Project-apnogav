package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.theme.ChanaEmerald
import com.example.ui.theme.HudGlassDark
import com.example.ui.theme.MarigoldSaffron
import com.example.ui.theme.MutedSandText
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SubtleGoldBorder
import com.example.ui.theme.WarmIvoryText
import org.json.JSONObject
import java.io.IOException

private const val MAP_ASSETS_HOST = "appassets.androidplatform.net"
private const val MAP_ENTRY_URL = "https://$MAP_ASSETS_HOST/assets/game/streetview.html"

private class FreeMapAssetClient(
    private val context: Context,
    private val onPageReady: () -> Unit,
) : WebViewClient() {
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val uri = request.url
        if (uri.host != MAP_ASSETS_HOST) return super.shouldInterceptRequest(view, request)
        val path = uri.path?.removePrefix("/assets/") ?: return null
        val mime = when {
            path.endsWith(".html") -> "text/html"
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".css") -> "text/css"
            else -> "application/octet-stream"
        }
        return try {
            val stream = context.assets.open(path)
            WebResourceResponse(mime, "UTF-8", 200, "OK", mapOf("Access-Control-Allow-Origin" to "*"), stream)
        } catch (_: IOException) {
            null
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageReady()
    }
}

private class FreeMapJsBridge(
    private val onStatusUpdate: (String, String) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onStatus(code: String, detail: String) {
        handler.post { onStatusUpdate(code, detail) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RealStreetViewOverlay(
    lat: Double,
    lng: Double,
    headingDeg: Float,
    onEnterFree3dView: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var activeLayer by remember { mutableStateOf("satellite") } // "satellite" (Esri) | "osm" (OpenStreetMap)
    var pageLoaded by remember { mutableStateOf(false) }
    var statusDetail by remember {
        mutableStateOf("Esri World Imagery Satellite • 100% Free (No API Key)")
    }

    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.parseColor("#142218"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            webChromeClient = WebChromeClient()
            webViewClient = FreeMapAssetClient(context) {
                pageLoaded = true
            }
            addJavascriptInterface(
                FreeMapJsBridge { _, detail ->
                    statusDetail = detail
                },
                "StreetViewBridge",
            )
            loadUrl(MAP_ENTRY_URL)
        }
    }

    LaunchedEffect(pageLoaded, activeLayer, lat, lng) {
        if (pageLoaded) {
            val quotedLayer = JSONObject.quote(activeLayer)
            webView.evaluateJavascript(
                "window.initFreeMap && window.initFreeMap($lat, $lng, $headingDeg, $quotedLayer);",
                null,
            )
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.onPause()
            webView.destroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF142218))) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize(),
        )

        // Floating Top Bar: Live Coordinates + Esri Satellite / OSM / Free 3D 360° Switchers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HudGlassDark)
                    .border(1.dp, SubtleGoldBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "Gaon Darshan • ${"%.5f".format(lat)}°N, ${"%.5f".format(lng)}°E",
                    style = MaterialTheme.typography.labelLarge,
                    color = MarigoldSaffron,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusDetail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSandText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MapLayerPill(
                    icon = Icons.Filled.Public,
                    label = "Esri Satellite",
                    active = activeLayer == "satellite",
                    tag = "map_layer_esri",
                    onClick = { activeLayer = "satellite" },
                )
                MapLayerPill(
                    icon = Icons.Filled.Map,
                    label = "OSM Map",
                    active = activeLayer == "osm",
                    tag = "map_layer_osm",
                    onClick = { activeLayer = "osm" },
                )

                Button(
                    onClick = onEnterFree3dView,
                    colors = ButtonDefaults.buttonColors(containerColor = ChanaEmerald, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp).testTag("map_enter_free_3d_button"),
                ) {
                    Icon(Icons.Filled.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Free 3D 360°", style = MaterialTheme.typography.labelLarge)
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ObsidianElevated)
                        .border(1.dp, SubtleGoldBorder, CircleShape)
                        .clickable(onClick = onClose)
                        .testTag("sv_close_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close_modal),
                        tint = WarmIvoryText,
                    )
                }
            }
        }
    }
}

@Composable
private fun MapLayerPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) MarigoldSaffron else HudGlassDark)
            .border(1.dp, if (active) MarigoldSaffron else SubtleGoldBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) Color(0xFF1A1200) else MarigoldSaffron,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) Color(0xFF1A1200) else WarmIvoryText,
        )
    }
}

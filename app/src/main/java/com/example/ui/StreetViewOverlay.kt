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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Streetview
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

private const val SV_ASSETS_HOST = "appassets.androidplatform.net"
private const val SV_ENTRY_URL = "https://$SV_ASSETS_HOST/assets/game/streetview.html"

private class StreetViewAssetClient(
    private val context: Context,
    private val onPageReady: () -> Unit,
) : WebViewClient() {
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val uri = request.url
        if (uri.host != SV_ASSETS_HOST) return super.shouldInterceptRequest(view, request)
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

private class StreetViewJsBridge(
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
    apiKey: String,
    onOpenSettingsForKey: () -> Unit,
    onOpenExternalStreetView: (Double, Double, Float) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val hasKey = apiKey.isNotBlank()
    var activeMode by remember { mutableStateOf(if (hasKey) "streetview" else "satellite") }
    var pageLoaded by remember { mutableStateOf(false) }
    var statusDetail by remember {
        mutableStateOf(
            if (hasKey) {
                "Loading 360° Street View near Johar Barha..."
            } else {
                "Real Satellite Map active • Tap 'Google 360°' or add API Key for embedded Street View"
            },
        )
    }

    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.parseColor("#0D1117"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            webChromeClient = WebChromeClient()
            webViewClient = StreetViewAssetClient(context) {
                pageLoaded = true
            }
            addJavascriptInterface(
                StreetViewJsBridge { code, detail ->
                    statusDetail = detail
                    if (code == "NO_KEY" || code == "KEY_ERROR" || code == "SATELLITE") {
                        activeMode = "satellite"
                    } else if (code == "SV_OK") {
                        activeMode = "streetview"
                    }
                },
                "StreetViewBridge",
            )
            loadUrl(SV_ENTRY_URL)
        }
    }

    LaunchedEffect(pageLoaded, activeMode, apiKey, lat, lng) {
        if (pageLoaded) {
            val quotedKey = JSONObject.quote(apiKey)
            val quotedMode = JSONObject.quote(activeMode)
            webView.evaluateJavascript(
                "window.initGaonDarshan && window.initGaonDarshan($lat, $lng, $headingDeg, $quotedMode, $quotedKey);",
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize(),
        )

        // Floating Top Control Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Location & Status Card
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HudGlassDark)
                    .border(1.dp, SubtleGoldBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "360° Gaon Darshan • ${"%.5f".format(lat)}°N, ${"%.5f".format(lng)}°E",
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

            // Mode Switchers + External Google 360 + Close
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModeTogglePill(
                    icon = Icons.Filled.Streetview,
                    label = "360° SV",
                    active = activeMode == "streetview",
                    tag = "sv_mode_streetview",
                    onClick = {
                        if (hasKey) {
                            activeMode = "streetview"
                        } else {
                            onOpenExternalStreetView(lat, lng, headingDeg)
                        }
                    },
                )
                ModeTogglePill(
                    icon = Icons.Filled.Public,
                    label = "Satellite",
                    active = activeMode == "satellite",
                    tag = "sv_mode_satellite",
                    onClick = { activeMode = "satellite" },
                )

                Button(
                    onClick = { onOpenExternalStreetView(lat, lng, headingDeg) },
                    colors = ButtonDefaults.buttonColors(containerColor = ChanaEmerald, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp).testTag("sv_open_google_maps_button"),
                ) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Google 360°", style = MaterialTheme.typography.labelLarge)
                }

                if (!hasKey) {
                    ModeTogglePill(
                        icon = Icons.Filled.Key,
                        label = "Add Key",
                        active = false,
                        tag = "sv_add_api_key_button",
                        onClick = onOpenSettingsForKey,
                    )
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
private fun ModeTogglePill(
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

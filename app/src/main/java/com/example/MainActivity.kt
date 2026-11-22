package com.example

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ActiveModal
import com.example.ui.DrivingHudOverlay
import com.example.ui.GameModalHost
import com.example.ui.GameViewModel
import com.example.ui.JoharBarha3DViewport
import com.example.ui.theme.JoharBarhaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureImmersiveMode()

        setContent {
            JoharBarhaTheme {
                val gameViewModel: GameViewModel = viewModel()
                JoharBarhaAppScreen(
                    viewModel = gameViewModel,
                    onShareSwag = { shareDesiSwag(gameViewModel.buildShareSwagText()) },
                    onOpenExternalStreetView = { lat, lng, heading ->
                        openExternalStreetView(lat, lng, heading)
                    },
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            configureImmersiveMode()
        }
    }

    private fun configureImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun shareDesiSwag(shareText: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Johar Barha 3D: Desi Swag & Luxury Drive")
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Share My Desi Swag via WhatsApp"))
    }

    private fun openExternalStreetView(lat: Double, lng: Double, headingDeg: Float) {
        val gmmUri = Uri.parse("google.streetview:cbll=$lat,$lng&cbp=1,${headingDeg.toInt()},,0,1")
        val mapsAppIntent = Intent(Intent.ACTION_VIEW, gmmUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            startActivity(mapsAppIntent)
        } catch (_: ActivityNotFoundException) {
            val webUrl = Uri.parse(
                "https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=$lat,$lng&heading=${headingDeg.toInt()}",
            )
            startActivity(Intent(Intent.ACTION_VIEW, webUrl))
        }
    }
}

@Composable
fun JoharBarhaAppScreen(
    viewModel: GameViewModel,
    onShareSwag: () -> Unit,
    onOpenExternalStreetView: (Double, Double, Float) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-Screen 60 FPS Three.js WebGL 3D Village Engine
        JoharBarha3DViewport(viewModel = viewModel)

        // Active Driving / Paidal Landscape HUD (shown whenever no modal covers the viewport)
        if (state.activeModal == ActiveModal.NONE) {
            DrivingHudOverlay(
                state = state,
                viewModel = viewModel,
            )
        }

        // Title Screen, Garage, Missions, Desi Jhola, Settings, Street View, Geofence & Victory Popups
        GameModalHost(
            state = state,
            viewModel = viewModel,
            onShareSwag = onShareSwag,
            onOpenExternalStreetView = onOpenExternalStreetView,
        )
    }
}

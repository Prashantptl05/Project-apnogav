package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Streetview
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameCatalog
import com.example.ui.theme.ChanaEmerald
import com.example.ui.theme.HudGlassDark
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.MarigoldSaffron
import com.example.ui.theme.MutedSandText
import com.example.ui.theme.SubtleGoldBorder
import com.example.ui.theme.WarmIvoryText
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DrivingHudOverlay(
    state: GameUiState,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
) {
    val tel = state.telemetry
    val activeCar = GameCatalog.vehicleById(tel.activeVehicleId)

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // TOP BAR: Swag + Mission Tracker (Left) | Quick Actions + 3D Radar Mini-Map (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            // Left telemetry column
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Swag Points Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(HudGlassDark)
                            .border(1.dp, MarigoldSaffron.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = "Desi Swag Points",
                            tint = MarigoldSaffron,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "${state.profile.swagPoints} SWAG",
                            style = MaterialTheme.typography.labelLarge,
                            color = MarigoldSaffron,
                        )
                    }

                    // Active Vehicle / Paidal Status Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(HudGlassDark)
                            .border(1.dp, SubtleGoldBorder, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (tel.isWalking) Icons.AutoMirrored.Filled.DirectionsWalk else Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = if (tel.isWalking) ChanaEmerald else WarmIvoryText,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (tel.isWalking) "Paidal Swag" else activeCar.shortName,
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmIvoryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Live Coordinates & Geofence Distance strip
                val nearBorder = tel.distFromCenter > 1250
                Text(
                    text = "Johar Barha ${tel.lat}°N, ${tel.lng}°E  •  ${tel.distFromCenter}m / 1500m",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (nearBorder) LaserCrimson else MutedSandText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(HudGlassDark.copy(alpha = 0.72f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )

                // Active Mission Card (if any mission is running)
                tel.mission?.let { mission ->
                    Row(
                        modifier = Modifier
                            .widthIn(max = 310.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(HudGlassDark)
                            .border(1.2.dp, MarigoldSaffron, RoundedCornerShape(14.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = mission.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MarigoldSaffron,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (mission.timeLeftSec > 0) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${mission.timeLeftSec}s",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (mission.timeLeftSec <= 15) LaserCrimson else Color.White,
                                    )
                                }
                            }
                            Text(
                                text = mission.objective,
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmIvoryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = buildString {
                                    append("${mission.distanceMeters}m away")
                                    if (mission.extraStatus.isNotEmpty()) append(" • ${mission.extraStatus}")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = ChanaEmerald,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LaserCrimson.copy(alpha = 0.25f))
                                .clickable { viewModel.abortMission() }
                                .testTag("abort_mission_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Abort Mission",
                                tint = WarmIvoryText,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            // Right top bar: Quick Action Buttons + 3D Radar Mini-Map
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HudIconButton(
                            icon = Icons.Filled.Flag,
                            label = "Missions",
                            tag = "hud_missions_button",
                            accent = MarigoldSaffron,
                            onClick = { viewModel.openModal(ActiveModal.MISSIONS) },
                        )
                        HudIconButton(
                            icon = Icons.Filled.Garage,
                            label = "Garage",
                            tag = "hud_garage_button",
                            onClick = { viewModel.openModal(ActiveModal.GARAGE) },
                        )
                        HudIconButton(
                            icon = Icons.Filled.Backpack,
                            label = "Jhola",
                            tag = "hud_jhola_button",
                            onClick = { viewModel.openModal(ActiveModal.JHOLA) },
                        )
                        HudIconButton(
                            icon = Icons.Filled.Settings,
                            label = "Settings",
                            tag = "hud_settings_button",
                            onClick = { viewModel.openModal(ActiveModal.SETTINGS) },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Real Street View Peek (360° Gaon Darshan) Toggle Button
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xDD133120))
                                .border(1.2.dp, ChanaEmerald, RoundedCornerShape(12.dp))
                                .clickable { viewModel.openModal(ActiveModal.STREET_VIEW) }
                                .padding(horizontal = 10.dp)
                                .testTag("hud_street_view_button"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Streetview,
                                contentDescription = "Real Street View Peek",
                                tint = ChanaEmerald,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "STREET VIEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmIvoryText,
                            )
                        }

                        // Camera Cycle Pill (Chase / Drift / Hood / 360 Darshan)
                        val camLabel = when (tel.cameraMode) {
                            "DRIFT" -> "DRIFT CAM"
                            "HOOD" -> "HOOD CAM"
                            "DARSHAN_360" -> "360° ORBIT"
                            "WALK" -> "WALK CAM"
                            else -> "CHASE CAM"
                        }
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(HudGlassDark)
                                .border(1.dp, SubtleGoldBorder, RoundedCornerShape(12.dp))
                                .clickable { viewModel.cycleCamera() }
                                .padding(horizontal = 10.dp)
                                .testTag("camera_cycle_button"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Videocam,
                                contentDescription = stringResource(R.string.cd_camera_mode),
                                tint = MarigoldSaffron,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = camLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmIvoryText,
                            )
                        }

                        // Respawn at Panchayat Chowk button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(HudGlassDark)
                                .border(1.dp, SubtleGoldBorder, RoundedCornerShape(12.dp))
                                .clickable { viewModel.respawnAtChowk() }
                                .testTag("respawn_chowk_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = stringResource(R.string.cd_reset_position),
                                tint = WarmIvoryText,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Circular 3D Radar Mini-Map showing 1.5 km Red Laser Geofence
                RadarMiniMap(
                    telemetry = tel,
                    modifier = Modifier.size(92.dp),
                )
            }
        }

        // CENTER-TOP TOAST BANNER
        state.toastMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xE6181209))
                    .border(1.2.dp, MarigoldSaffron, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 7.dp),
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MarigoldSaffron,
                    maxLines = 2,
                )
            }
        }

        // BOTTOM-LEFT: Multi-touch Steer Buttons (default) OR Virtual Steering Wheel + Walk/Horn
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Enter / Exit Car (3D Paidal Mode)
                ActionChipButton(
                    icon = if (tel.isWalking) Icons.Filled.DirectionsCar else Icons.AutoMirrored.Filled.DirectionsWalk,
                    label = if (tel.isWalking) stringResource(R.string.btn_enter_car) else stringResource(R.string.btn_get_out),
                    accent = ChanaEmerald,
                    tag = "toggle_walk_button",
                    onClick = { viewModel.toggleWalkMode() },
                )

                // Horn (or Taali clap in Paidal Mode!)
                HoldableActionButton(
                    icon = Icons.Filled.Campaign,
                    label = if (tel.isWalking) stringResource(R.string.btn_taali) else stringResource(R.string.btn_horn),
                    accent = MarigoldSaffron,
                    tag = "horn_taali_button",
                    onPressChanged = { viewModel.setHorn(it) },
                )

                if (tel.isWalking) {
                    ActionChipButton(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        label = stringResource(R.string.btn_jump),
                        accent = Color(0xFF38BDF8),
                        tag = "jump_button",
                        onClick = { viewModel.triggerJump() },
                    )
                }
            }

            if (state.profile.steeringMode == "WHEEL") {
                VirtualSteeringWheel(
                    onSteerChanged = { viewModel.setWheelSteer(it) },
                    modifier = Modifier.size(124.dp),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HoldableSteerButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDesc = stringResource(R.string.cd_steer_left),
                        tag = "steer_left_button",
                        onPressChanged = { viewModel.setSteerLeft(it) },
                    )
                    HoldableSteerButton(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDesc = stringResource(R.string.cd_steer_right),
                        tag = "steer_right_button",
                        onPressChanged = { viewModel.setSteerRight(it) },
                    )
                }
            }
        }

        // BOTTOM-RIGHT: Speedometer + Multi-Touch DRIFT, BRAKE/REV & GAS Pedals
        Column(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Digital Speedometer & Gear
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(HudGlassDark)
                    .border(1.dp, SubtleGoldBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "${tel.speedKmh}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                    color = WarmIvoryText,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "KM/H",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSandText,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MarigoldSaffron)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = tel.gear,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF1A1200),
                    )
                }
            }

            // Pedals Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                HoldablePedal(
                    label = if (tel.isWalking) "RUN" else stringResource(R.string.btn_drift),
                    accent = MarigoldSaffron,
                    width = 62.dp,
                    height = 56.dp,
                    tag = "drift_pedal",
                    onPressChanged = { viewModel.setDrift(it) },
                )
                HoldablePedal(
                    label = if (tel.isWalking) "BACK" else stringResource(R.string.btn_brake),
                    accent = LaserCrimson,
                    width = 68.dp,
                    height = 68.dp,
                    tag = "brake_pedal",
                    onPressChanged = { viewModel.setBrake(it) },
                )
                HoldablePedal(
                    label = if (tel.isWalking) "WALK" else stringResource(R.string.btn_gas),
                    accent = ChanaEmerald,
                    width = 76.dp,
                    height = 84.dp,
                    tag = "gas_pedal",
                    onPressChanged = { viewModel.setGas(it) },
                )
            }
        }
    }
}

@Composable
private fun HudIconButton(
    icon: ImageVector,
    label: String,
    tag: String,
    accent: Color = WarmIvoryText,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .size(width = 54.dp, height = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HudGlassDark)
            .border(1.dp, SubtleGoldBorder, RoundedCornerShape(12.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = WarmIvoryText,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActionChipButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    tag: String,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HudGlassDark)
            .border(1.2.dp, accent, RoundedCornerShape(14.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(5.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = WarmIvoryText)
    }
}

@Composable
private fun HoldableActionButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    tag: String,
    onPressChanged: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (pressed) accent.copy(alpha = 0.85f) else HudGlassDark)
            .border(1.4.dp, accent, RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPressChanged(true)
                    waitForUpOrCancellation()
                    pressed = false
                    onPressChanged(false)
                }
            }
            .padding(horizontal = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (pressed) Color(0xFF1A1200) else accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (pressed) Color(0xFF1A1200) else WarmIvoryText,
        )
    }
}

@Composable
private fun HoldableSteerButton(
    icon: ImageVector,
    contentDesc: String,
    tag: String,
    onPressChanged: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (pressed) MarigoldSaffron.copy(alpha = 0.88f) else HudGlassDark)
            .border(1.6.dp, if (pressed) Color.White else SubtleGoldBorder, RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPressChanged(true)
                    waitForUpOrCancellation()
                    pressed = false
                    onPressChanged(false)
                }
            }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = if (pressed) Color(0xFF1A1200) else WarmIvoryText,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun HoldablePedal(
    label: String,
    accent: Color,
    width: Dp,
    height: Dp,
    tag: String,
    onPressChanged: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(16.dp))
            .background(if (pressed) accent.copy(alpha = 0.92f) else HudGlassDark)
            .border(2.dp, accent, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPressChanged(true)
                    waitForUpOrCancellation()
                    pressed = false
                    onPressChanged(false)
                }
            }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (pressed) Color.White else accent,
        )
    }
}

@Composable
private fun VirtualSteeringWheel(
    onSteerChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var wheelAngle by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(HudGlassDark)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        wheelAngle = 0f
                        onSteerChanged(0f)
                    },
                    onDragCancel = {
                        wheelAngle = 0f
                        onSteerChanged(0f)
                    },
                ) { change, _ ->
                    change.consume()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val angleRad = atan2(change.position.y - cy, change.position.x - cx)
                    val deg = Math.toDegrees(angleRad.toDouble()).toFloat() + 90f
                    val norm = ((deg + 540f) % 360f - 180f).coerceIn(-110f, 110f)
                    wheelAngle = norm
                    onSteerChanged((norm / 90f).coerceIn(-1f, 1f))
                }
            }
            .testTag("virtual_steering_wheel"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val r = size.minDimension / 2f
            rotate(wheelAngle) {
                drawCircle(color = MarigoldSaffron, radius = r, style = Stroke(width = 10f))
                drawCircle(color = Color(0xFF1F242D), radius = r * 0.28f)
                drawLine(MarigoldSaffron, Offset(center.x - r, center.y), Offset(center.x + r, center.y), strokeWidth = 8f)
                drawLine(MarigoldSaffron, center, Offset(center.x, center.y + r), strokeWidth = 8f)
                // Top center marker
                drawCircle(LaserCrimson, radius = 6f, center = Offset(center.x, center.y - r))
            }
        }
    }
}

@Composable
private fun RadarMiniMap(
    telemetry: TelemetryState,
    modifier: Modifier = Modifier,
) {
    val radarDesc = stringResource(R.string.cd_radar_map)
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xDD0B0E14))
            .border(2.dp, LaserCrimson, CircleShape)
            .semantics { contentDescription = radarDesc }
            .testTag("radar_minimap"),
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val radius = size.minDimension / 2f
            val scale = radius / GameCatalog.GEOFENCE_RADIUS_METERS.toFloat()

            // Red 1.5 km Geofence Ring
            drawCircle(
                color = LaserCrimson.copy(alpha = 0.85f),
                radius = radius - 1f,
                style = Stroke(width = 3f),
            )
            // Inner Village Ring Road guide
            drawCircle(
                color = Color(0xFF64748B).copy(alpha = 0.55f),
                radius = 460f * scale,
                style = Stroke(width = 2.5f),
            )
            // Cross Main Roads (Bareli EW & Amrawad NS)
            drawLine(
                color = Color(0xFF94A3B8).copy(alpha = 0.65f),
                start = Offset(center.x - radius * 0.9f, center.y),
                end = Offset(center.x + radius * 0.9f, center.y),
                strokeWidth = 3f,
            )
            drawLine(
                color = Color(0xFF94A3B8).copy(alpha = 0.65f),
                start = Offset(center.x, center.y - radius * 0.9f),
                end = Offset(center.x, center.y + radius * 0.9f),
                strokeWidth = 3f,
            )

            // Gram Panchayat Chowk dot
            drawCircle(color = MarigoldSaffron.copy(alpha = 0.6f), radius = 3.5f, center = center)

            // Mission Waypoint pulsing dot
            telemetry.mission?.let { m ->
                val mx = center.x + m.targetX * scale
                val mz = center.y + m.targetZ * scale
                drawCircle(color = MarigoldSaffron, radius = 6.5f, center = Offset(mx, mz))
                drawCircle(color = Color.White, radius = 2.5f, center = Offset(mx, mz))
            }

            // Player directional chevron
            val px = (center.x + telemetry.playerX * scale).coerceIn(6f, size.width - 6f)
            val pz = (center.y + telemetry.playerZ * scale).coerceIn(6f, size.height - 6f)
            val headingRad = Math.toRadians(telemetry.headingDeg.toDouble())
            val tipX = px + sin(headingRad).toFloat() * 8f
            val tipY = pz + cos(headingRad).toFloat() * 8f
            val path = Path().apply {
                moveTo(tipX, tipY)
                lineTo(px + sin(headingRad + 2.35).toFloat() * 6f, pz + cos(headingRad + 2.35).toFloat() * 6f)
                lineTo(px + sin(headingRad - 2.35).toFloat() * 6f, pz + cos(headingRad - 2.35).toFloat() * 6f)
                close()
            }
            drawPath(path = path, color = ChanaEmerald)
        }
    }
}

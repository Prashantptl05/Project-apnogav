package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Streetview
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameCatalog
import com.example.data.VehicleSpec
import com.example.ui.theme.ChanaEmerald
import com.example.ui.theme.HudGlassDark
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.MarigoldSaffron
import com.example.ui.theme.MutedSandText
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SubtleGoldBorder
import com.example.ui.theme.WarmIvoryText

@Composable
fun GameModalHost(
    state: GameUiState,
    viewModel: GameViewModel,
    onShareSwag: () -> Unit,
) {
    when (state.activeModal) {
        ActiveModal.NONE -> Unit
        ActiveModal.TITLE -> TitleOverlay(state, viewModel, onShareSwag)
        ActiveModal.GARAGE -> {
            BackHandler { viewModel.closeModal() }
            GarageOverlay(state, viewModel)
        }
        ActiveModal.MISSIONS -> {
            BackHandler { viewModel.closeModal() }
            MissionsOverlay(state, viewModel)
        }
        ActiveModal.JHOLA -> {
            BackHandler { viewModel.closeModal() }
            DesiJholaOverlay(state, viewModel)
        }
        ActiveModal.SETTINGS -> {
            BackHandler { viewModel.closeModal() }
            SettingsOverlay(state, viewModel, onShareSwag)
        }
        ActiveModal.STREET_VIEW -> {
            BackHandler { viewModel.closeModal() }
            RealStreetViewOverlay(
                lat = state.telemetry.lat,
                lng = state.telemetry.lng,
                headingDeg = state.telemetry.headingDeg,
                onEnterFree3dView = { viewModel.enterFree3dView() },
                onClose = { viewModel.closeModal() },
            )
        }
        ActiveModal.GEOFENCE -> {
            BackHandler { viewModel.closeModal() }
            GeofencePopup(onDismiss = { viewModel.closeModal() })
        }
        ActiveModal.VICTORY -> {
            BackHandler { viewModel.closeModal() }
            state.victorySummary?.let { summary ->
                VictoryPopup(
                    summary = summary,
                    onShare = onShareSwag,
                    onContinue = { viewModel.closeModal() },
                )
            }
        }
        ActiveModal.FAILED -> {
            BackHandler { viewModel.closeModal() }
            state.failureSummary?.let { fail ->
                FailurePopup(
                    summary = fail,
                    onRetry = { viewModel.startMission(fail.missionId) },
                    onDismiss = { viewModel.closeModal() },
                )
            }
        }
    }
}

@Composable
private fun TitleOverlay(
    state: GameUiState,
    viewModel: GameViewModel,
    onShareSwag: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xD9101318), Color(0x66101318), Color(0x33101318)),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .widthIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.village_location),
                style = MaterialTheme.typography.labelSmall,
                color = MarigoldSaffron,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xAA291A05))
                    .border(1.dp, MarigoldSaffron.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
            Text(
                text = "JOHAR BARHA 3D",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                color = WarmIvoryText,
            )
            Text(
                text = stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.titleLarge,
                color = MarigoldSaffron,
            )
            Text(
                text = "Scorpio-N, G-Wagon, Rolls-Royce & Aventador lekar Gram Johar Barha ki sadkon aur gulliyon mein desi swag dikhao!",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSandText,
            )

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.startDrivingFromTitle() },
                    colors = ButtonDefaults.buttonColors(containerColor = MarigoldSaffron, contentColor = Color(0xFF1A1200)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(50.dp).testTag("title_drive_now_button"),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_drive_now), style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = { viewModel.openModal(ActiveModal.MISSIONS) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(50.dp).testTag("title_missions_button"),
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = null, tint = MarigoldSaffron)
                    Spacer(Modifier.width(6.dp))
                    Text("8 Desi Missions", color = WarmIvoryText)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactTitleChip(Icons.Filled.Streetview, "Sat / 3D Map", "title_street_view_button") {
                    viewModel.openModal(ActiveModal.STREET_VIEW)
                }
                CompactTitleChip(Icons.Filled.Garage, "Garage", "title_garage_button") {
                    viewModel.openModal(ActiveModal.GARAGE)
                }
                CompactTitleChip(Icons.Filled.Backpack, "Desi Jhola", "title_jhola_button") {
                    viewModel.openModal(ActiveModal.JHOLA)
                }
                CompactTitleChip(Icons.Filled.Settings, "Settings", "title_settings_button") {
                    viewModel.openModal(ActiveModal.SETTINGS)
                }
                CompactTitleChip(Icons.Filled.Share, "Share", "title_share_button") {
                    onShareSwag()
                }
            }
        }

        // Top-right Swag Summary Badge on Title Screen
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(14.dp))
                .background(HudGlassDark)
                .border(1.dp, SubtleGoldBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MarigoldSaffron, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${state.profile.swagPoints} SWAG  •  ${state.missionsProgress.values.count { it.completed }}/8 Missions",
                style = MaterialTheme.typography.labelLarge,
                color = WarmIvoryText,
            )
        }
    }
}

@Composable
private fun CompactTitleChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HudGlassDark)
            .border(1.dp, SubtleGoldBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = MarigoldSaffron, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = WarmIvoryText)
    }
}

@Composable
private fun ModalFrame(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xC80A0C10))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ObsidianCard)
                .border(1.2.dp, SubtleGoldBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = MarigoldSaffron)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MutedSandText)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ObsidianElevated)
                        .clickable(onClick = onClose)
                        .testTag("modal_close_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close_modal),
                        tint = WarmIvoryText,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun GarageOverlay(state: GameUiState, viewModel: GameViewModel) {
    ModalFrame(
        title = "Desi Luxury Garage",
        subtitle = "Your Desi Swag: ${state.profile.swagPoints} pts • Unlock iconic 3D cars by completing Johar Barha missions!",
        onClose = { viewModel.closeModal() },
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(GameCatalog.vehicles) { car ->
                val unlocked = state.profile.swagPoints >= car.requiredSwag
                val selected = state.profile.selectedVehicleId == car.id
                VehicleCard(
                    car = car,
                    unlocked = unlocked,
                    selected = selected,
                    currentSwag = state.profile.swagPoints,
                    onSelect = { viewModel.selectVehicle(car.id) },
                )
            }
        }
    }
}

@Composable
private fun VehicleCard(
    car: VehicleSpec,
    unlocked: Boolean,
    selected: Boolean,
    currentSwag: Int,
    onSelect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(250.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianElevated)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MarigoldSaffron else SubtleGoldBorder,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (unlocked) Icons.Filled.DirectionsCar else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(car.accentColorHex),
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = if (selected) "ACTIVE" else if (unlocked) "UNLOCKED" else "${car.requiredSwag} SWAG",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MarigoldSaffron else if (unlocked) ChanaEmerald else LaserCrimson,
                )
            }
            Text(car.fullName, style = MaterialTheme.typography.titleMedium, color = WarmIvoryText)
            Text(car.subtitle, style = MaterialTheme.typography.labelSmall, color = MarigoldSaffron)
            Text(car.highlights, style = MaterialTheme.typography.bodyMedium, color = MutedSandText)
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            StatBar("Top Speed (${car.topSpeedKmh} km/h)", car.topSpeedKmh / 260f, MarigoldSaffron)
            StatBar("Handling", car.handling / 100f, ChanaEmerald)
            StatBar("Ground Clearance", car.clearance / 100f, Color(0xFF38BDF8))
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onSelect,
                enabled = unlocked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) ChanaEmerald else MarigoldSaffron,
                    contentColor = Color(0xFF1A1200),
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("select_car_${car.id}"),
            ) {
                Text(
                    text = if (selected) "Driving Now" else if (unlocked) "Drive This Car" else "Need ${car.requiredSwag - currentSwag} More Swag",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun StatBar(label: String, progress: Float, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MutedSandText)
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = color,
            trackColor = Color(0xFF334155),
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun MissionsOverlay(state: GameUiState, viewModel: GameViewModel) {
    val done = state.missionsProgress.values.count { it.completed }
    ModalFrame(
        title = "Desi Missions ($done/8)",
        subtitle = "Follow the floating 3D golden arrow & glowing checkpoint rings across Gram Johar Barha!",
        onClose = { viewModel.closeModal() },
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(GameCatalog.missions) { mission ->
                val progress = state.missionsProgress[mission.id]
                val jholaItem = GameCatalog.jholaById(mission.rewardJholaId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianElevated)
                        .border(
                            1.dp,
                            if (progress?.completed == true) ChanaEmerald.copy(alpha = 0.7f) else SubtleGoldBorder,
                            RoundedCornerShape(14.dp),
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${mission.number}. ${mission.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MarigoldSaffron,
                            )
                            if (progress?.completed == true) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = ChanaEmerald,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Text(mission.hinglishBrief, style = MaterialTheme.typography.bodyMedium, color = WarmIvoryText)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("+${mission.rewardSwag} SWAG", style = MaterialTheme.typography.labelSmall, color = MarigoldSaffron)
                            if (jholaItem != null) {
                                Text("${jholaItem.emoji} ${jholaItem.name}", style = MaterialTheme.typography.labelSmall, color = ChanaEmerald)
                            }
                            if (mission.timeLimitSec > 0) {
                                Text("Timer: ${mission.timeLimitSec}s", style = MaterialTheme.typography.labelSmall, color = LaserCrimson)
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { viewModel.startMission(mission.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = MarigoldSaffron, contentColor = Color(0xFF1A1200)),
                        modifier = Modifier.height(48.dp).testTag("start_mission_${mission.id}"),
                    ) {
                        Text(if (progress?.completed == true) "Replay" else "Start", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun DesiJholaOverlay(state: GameUiState, viewModel: GameViewModel) {
    val unlockedCount = state.jholaProgress.values.count { it.unlocked }
    ModalFrame(
        title = "Desi Jhola ($unlockedCount/7)",
        subtitle = "Johar Barha & Bareli Tehsil ke shuddh desi collectibles!",
        onClose = { viewModel.closeModal() },
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 210.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(GameCatalog.jholaItems) { item ->
                val isUnlocked = state.jholaProgress[item.id]?.unlocked == true
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianElevated)
                        .border(
                            1.2.dp,
                            if (isUnlocked) MarigoldSaffron else Color(0xFF334155),
                            RoundedCornerShape(14.dp),
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = item.emoji, fontSize = 28.sp)
                        Text(
                            text = if (isUnlocked) "COLLECTED" else "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUnlocked) ChanaEmerald else MutedSandText,
                        )
                    }
                    Text(item.name, style = MaterialTheme.typography.titleMedium, color = if (isUnlocked) WarmIvoryText else MutedSandText)
                    Text(
                        text = if (isUnlocked) "\"${item.caption}\"" else item.unlockHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUnlocked) MarigoldSaffron else MutedSandText,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsOverlay(
    state: GameUiState,
    viewModel: GameViewModel,
    onShareSwag: () -> Unit,
) {
    val p = state.profile
    ModalFrame(
        title = "Game Settings",
        subtitle = "Customize controls, rural MP atmosphere, 60 FPS graphics & OpenStreetMap roads.",
        onClose = { viewModel.closeModal() },
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                SettingChoiceRow(
                    title = "Steering Layout",
                    options = listOf("BUTTONS" to "Left/Right Buttons", "WHEEL" to "Virtual Wheel"),
                    selected = p.steeringMode,
                    onSelect = { viewModel.setSteeringMode(it) },
                )
            }
            item {
                SettingChoiceRow(
                    title = "Johar Barha Lighting",
                    options = listOf(
                        "AFTERNOON" to "Sunny MP Afternoon",
                        "GOLDEN_HOUR" to "Golden Hour",
                        "NIGHT" to "Festive Night",
                    ),
                    selected = p.timeOfDay,
                    onSelect = { viewModel.setTimeOfDay(it) },
                )
            }
            item {
                SettingChoiceRow(
                    title = "3D Graphics Quality",
                    options = listOf("HIGH" to "60 FPS High", "BALANCED" to "Battery Saver"),
                    selected = p.graphicsQuality,
                    onSelect = { viewModel.setGraphicsQuality(it) },
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Web Audio Sound & Dhol FX", style = MaterialTheme.typography.titleMedium, color = WarmIvoryText)
                        Text("Engine roar, desi horn, drift screech & victory dhol", style = MaterialTheme.typography.bodyMedium, color = MutedSandText)
                    }
                    Switch(
                        checked = p.soundEnabled,
                        onCheckedChange = { viewModel.setSoundEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MarigoldSaffron),
                        modifier = Modifier.testTag("sound_toggle_switch"),
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Esri World Imagery 3D Ground (Free)", style = MaterialTheme.typography.titleMedium, color = MarigoldSaffron)
                        Text("Real satellite imagery of Gram Johar Barha — no API key needed", style = MaterialTheme.typography.bodyMedium, color = MutedSandText)
                    }
                    Switch(
                        checked = state.satelliteTerrainEnabled,
                        onCheckedChange = { viewModel.setSatelliteTerrainEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = ChanaEmerald),
                        modifier = Modifier.testTag("satellite_terrain_switch"),
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("OpenStreetMap Roads (Johar Barha)", style = MaterialTheme.typography.titleMedium, color = WarmIvoryText)
                        Text("Status: ${state.osmStatus}", style = MaterialTheme.typography.bodyMedium, color = ChanaEmerald)
                    }
                    OutlinedButton(
                        onClick = { viewModel.refreshOsmRoads() },
                        modifier = Modifier.height(48.dp).testTag("refresh_osm_button"),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = MarigoldSaffron)
                        Spacer(Modifier.width(5.dp))
                        Text("Sync OSM", color = WarmIvoryText)
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onShareSwag,
                        colors = ButtonDefaults.buttonColors(containerColor = ChanaEmerald, contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("settings_share_button"),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_share_swag), style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetProgress() },
                        modifier = Modifier.height(48.dp).testTag("reset_save_button"),
                    ) {
                        Text("Reset Save", color = LaserCrimson)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingChoiceRow(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianElevated)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = WarmIvoryText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (key, label) ->
                val active = selected == key
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) MarigoldSaffron else ObsidianCard)
                        .border(1.dp, if (active) MarigoldSaffron else SubtleGoldBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelect(key) }
                        .padding(horizontal = 14.dp)
                        .testTag("setting_${key.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) Color(0xFF1A1200) else WarmIvoryText,
                    )
                }
            }
        }
    }
}

@Composable
private fun GeofencePopup(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB80A0404))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(ObsidianCard)
                .border(2.dp, LaserCrimson, RoundedCornerShape(22.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = LaserCrimson, modifier = Modifier.size(44.dp))
            Text(
                text = stringResource(R.string.geofence_title),
                style = MaterialTheme.typography.headlineMedium,
                color = LaserCrimson,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.geofence_message),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = WarmIvoryText,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MarigoldSaffron, contentColor = Color(0xFF1A1200)),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("geofence_dismiss_button"),
            ) {
                Text(stringResource(R.string.btn_turn_back), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun VictoryPopup(
    summary: VictorySummary,
    onShare: () -> Unit,
    onContinue: () -> Unit,
) {
    val jhola = GameCatalog.jholaById(summary.unlockedJholaId)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xC2090C10))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(ObsidianCard)
                .border(2.dp, MarigoldSaffron, RoundedCornerShape(22.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MarigoldSaffron, modifier = Modifier.size(44.dp))
            Text(stringResource(R.string.mission_complete_title), style = MaterialTheme.typography.headlineMedium, color = MarigoldSaffron)
            Text(summary.missionTitle, style = MaterialTheme.typography.titleMedium, color = WarmIvoryText)
            Text(
                text = "+${summary.swagEarned} Desi Swag Points! (Total: ${summary.totalSwag})",
                style = MaterialTheme.typography.labelLarge,
                color = ChanaEmerald,
            )

            jhola?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianElevated)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(it.emoji, fontSize = 30.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Desi Jhola Unlocked: ${it.name}", style = MaterialTheme.typography.titleMedium, color = MarigoldSaffron)
                        Text("\"${it.caption}\"", style = MaterialTheme.typography.bodyMedium, color = MutedSandText)
                    }
                }
            }

            if (summary.newlyUnlockedCars.isNotEmpty()) {
                Text(
                    text = "NEW LUXURY CAR UNLOCKED: ${summary.newlyUnlockedCars.joinToString()}!",
                    style = MaterialTheme.typography.labelLarge,
                    color = MarigoldSaffron,
                    textAlign = TextAlign.Center,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("victory_share_button"),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = ChanaEmerald)
                    Spacer(Modifier.width(5.dp))
                    Text("Share Swag", color = WarmIvoryText)
                }
                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = MarigoldSaffron, contentColor = Color(0xFF1A1200)),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("victory_continue_button"),
                ) {
                    Text(stringResource(R.string.btn_continue), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun FailurePopup(
    summary: FailureSummary,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xC2090505))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(ObsidianCard)
                .border(2.dp, LaserCrimson, RoundedCornerShape(22.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.mission_failed_title), style = MaterialTheme.typography.headlineMedium, color = LaserCrimson)
            Text(summary.missionTitle, style = MaterialTheme.typography.titleMedium, color = MarigoldSaffron)
            Text(summary.reason, style = MaterialTheme.typography.bodyLarge, color = WarmIvoryText, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("fail_dismiss_button"),
                ) {
                    Text("Free Roam", color = WarmIvoryText)
                }
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = MarigoldSaffron, contentColor = Color(0xFF1A1200)),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("fail_retry_button"),
                ) {
                    Text(stringResource(R.string.btn_retry_mission), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

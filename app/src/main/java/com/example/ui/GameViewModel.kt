package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameCatalog
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.JholaItemEntity
import com.example.data.MissionProgressEntity
import com.example.data.PlayerProfileEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class ActiveModal {
    TITLE,
    NONE,
    GARAGE,
    MISSIONS,
    JHOLA,
    SETTINGS,
    GEOFENCE,
    VICTORY,
    FAILED,
}

data class ActiveMissionHud(
    val missionId: String,
    val title: String,
    val objective: String,
    val timeLeftSec: Int,
    val distanceMeters: Int,
    val targetX: Float,
    val targetZ: Float,
    val extraStatus: String,
)

data class TelemetryState(
    val speedKmh: Int = 0,
    val gear: String = "N",
    val isWalking: Boolean = false,
    val cameraMode: String = "CHASE",
    val activeVehicleId: String = "scorpio",
    val playerX: Float = 0f,
    val playerZ: Float = 22f,
    val headingDeg: Float = 180f,
    val distFromCenter: Int = 22,
    val lat: Double = GameCatalog.CENTER_LAT,
    val lng: Double = GameCatalog.CENTER_LNG,
    val mission: ActiveMissionHud? = null,
)

data class VictorySummary(
    val missionTitle: String,
    val swagEarned: Int,
    val totalSwag: Int,
    val unlockedJholaId: String,
    val newlyUnlockedCars: List<String>,
)

data class FailureSummary(
    val missionId: String,
    val missionTitle: String,
    val reason: String,
)

data class GameUiState(
    val engineReady: Boolean = false,
    val profile: PlayerProfileEntity = PlayerProfileEntity(),
    val missionsProgress: Map<String, MissionProgressEntity> = emptyMap(),
    val jholaProgress: Map<String, JholaItemEntity> = emptyMap(),
    val telemetry: TelemetryState = TelemetryState(),
    val activeModal: ActiveModal = ActiveModal.TITLE,
    val toastMessage: String? = null,
    val osmStatus: String = "Procedural Johar Barha Roads",
    val victorySummary: VictorySummary? = null,
    val failureSummary: FailureSummary? = null,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(GameDatabase.getInstance(application).gameDao())

    private val engineReady = MutableStateFlow(false)
    private val telemetry = MutableStateFlow(TelemetryState())
    private val activeModal = MutableStateFlow(ActiveModal.TITLE)
    private val toastMessage = MutableStateFlow<String?>(null)
    private val osmStatus = MutableStateFlow("Procedural Johar Barha Roads")
    private val victorySummary = MutableStateFlow<VictorySummary?>(null)
    private val failureSummary = MutableStateFlow<FailureSummary?>(null)

    private var jsRunner: ((String) -> Unit)? = null

    // Multi-touch live input state
    private var steerLeft = false
    private var steerRight = false
    private var wheelSteer: Float? = null
    private var gasPressed = false
    private var brakePressed = false
    private var driftPressed = false

    val uiState: StateFlow<GameUiState> = combine(
        repository.profileFlow,
        repository.missionsFlow,
        repository.jholaFlow,
        combine(engineReady, telemetry, activeModal, toastMessage, osmStatus) { ready, tel, modal, toast, osm ->
            InternalUi(ready, tel, modal, toast, osm)
        },
        combine(victorySummary, failureSummary) { v, f -> v to f },
    ) { profile, missions, jhola, internal, outcomes ->
        GameUiState(
            engineReady = internal.ready,
            profile = profile,
            missionsProgress = missions,
            jholaProgress = jhola,
            telemetry = internal.telemetry,
            activeModal = internal.modal,
            toastMessage = internal.toast,
            osmStatus = internal.osm,
            victorySummary = outcomes.first,
            failureSummary = outcomes.second,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameUiState(),
    )

    private data class InternalUi(
        val ready: Boolean,
        val telemetry: TelemetryState,
        val modal: ActiveModal,
        val toast: String?,
        val osm: String,
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    fun attachJsRunner(runner: (String) -> Unit) {
        jsRunner = runner
        if (engineReady.value) {
            syncProfileToEngine(uiState.value.profile)
        }
    }

    fun detachJsRunner() {
        jsRunner = null
    }

    private fun evalJs(script: String) {
        jsRunner?.invoke(script)
    }

    fun onEngineReady() {
        engineReady.value = true
        val p = uiState.value.profile
        syncProfileToEngine(p)
        if (p.osmEnabled) {
            evalJs("window.JoharGame && window.JoharGame.loadOsm();")
        }
    }

    private fun syncProfileToEngine(p: PlayerProfileEntity) {
        evalJs(
            """
            if (window.JoharGame) {
              window.JoharGame.setVehicle('${p.selectedVehicleId}');
              window.JoharGame.setTimeOfDay('${p.timeOfDay}');
              window.JoharGame.setGraphicsQuality('${p.graphicsQuality}');
              window.JoharGame.setSoundEnabled(${p.soundEnabled});
            }
            """.trimIndent(),
        )
    }

    fun onTelemetryJson(jsonStr: String) {
        try {
            val obj = JSONObject(jsonStr)
            val mObj = obj.optJSONObject("mission")
            val missionHud = if (mObj != null) {
                val mId = mObj.optString("missionId", "")
                val spec = GameCatalog.missionById(mId)
                ActiveMissionHud(
                    missionId = mId,
                    title = spec?.title ?: mId,
                    objective = mObj.optString("objective", ""),
                    timeLeftSec = mObj.optInt("timeLeftSec", 0),
                    distanceMeters = mObj.optInt("distanceMeters", 0),
                    targetX = mObj.optDouble("targetX", 0.0).toFloat(),
                    targetZ = mObj.optDouble("targetZ", 0.0).toFloat(),
                    extraStatus = mObj.optString("extraStatus", ""),
                )
            } else {
                null
            }

            telemetry.value = TelemetryState(
                speedKmh = obj.optInt("speedKmh", 0),
                gear = obj.optString("gear", "N"),
                isWalking = obj.optBoolean("isWalking", false),
                cameraMode = obj.optString("cameraMode", "CHASE"),
                activeVehicleId = obj.optString("vehicleId", "scorpio"),
                playerX = obj.optDouble("x", 0.0).toFloat(),
                playerZ = obj.optDouble("z", 22.0).toFloat(),
                headingDeg = obj.optDouble("headingDeg", 180.0).toFloat(),
                distFromCenter = obj.optInt("distFromCenter", 0),
                lat = obj.optDouble("lat", GameCatalog.CENTER_LAT),
                lng = obj.optDouble("lng", GameCatalog.CENTER_LNG),
                mission = missionHud,
            )

            val deltaDist = obj.optDouble("deltaDistMeters", 0.0).toFloat()
            if (deltaDist >= 25f) {
                viewModelScope.launch {
                    repository.addDistanceMeters(deltaDist)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun onGeofenceHit() {
        releaseAllControls()
        evalJs("window.JoharGame && window.JoharGame.setPaused(true);")
        activeModal.value = ActiveModal.GEOFENCE
    }

    fun onMissionComplete(missionId: String, elapsedSec: Int) {
        releaseAllControls()
        val spec = GameCatalog.missionById(missionId) ?: return
        viewModelScope.launch {
            val (newSwag, unlockedCars) = repository.recordMissionVictory(
                missionId = spec.id,
                swagReward = spec.rewardSwag,
                jholaItemId = spec.rewardJholaId,
                elapsedSeconds = elapsedSec,
            )
            victorySummary.value = VictorySummary(
                missionTitle = spec.title,
                swagEarned = spec.rewardSwag,
                totalSwag = newSwag,
                unlockedJholaId = spec.rewardJholaId,
                newlyUnlockedCars = unlockedCars,
            )
            activeModal.value = ActiveModal.VICTORY
        }
    }

    fun onMissionFailed(missionId: String, reason: String) {
        releaseAllControls()
        val spec = GameCatalog.missionById(missionId)
        failureSummary.value = FailureSummary(
            missionId = missionId,
            missionTitle = spec?.title ?: "Desi Mission",
            reason = reason,
        )
        activeModal.value = ActiveModal.FAILED
    }

    fun onOsmStatus(status: String) {
        osmStatus.value = status
    }

    fun showToast(message: String) {
        toastMessage.value = message
        viewModelScope.launch {
            delay(3400)
            toastMessage.update { current -> if (current == message) null else current }
        }
    }

    // --- Navigation & Modal Actions ---

    fun startDrivingFromTitle() {
        activeModal.value = ActiveModal.NONE
        evalJs("window.JoharGame && window.JoharGame.startPlaying();")
        showToast("Gram Johar Barha mein swagat hai! Driving shuru karo!")
    }

    fun openModal(modal: ActiveModal) {
        releaseAllControls()
        activeModal.value = modal
        evalJs("window.JoharGame && window.JoharGame.setPaused(true);")
    }

    fun closeModal() {
        activeModal.value = ActiveModal.NONE
        evalJs("window.JoharGame && window.JoharGame.setPaused(false);")
    }

    fun selectVehicle(vehicleId: String) {
        val spec = GameCatalog.vehicleById(vehicleId)
        if (uiState.value.profile.swagPoints < spec.requiredSwag) {
            showToast("${spec.shortName} unlock karne ke liye ${spec.requiredSwag} Desi Swag Points chahiye!")
            return
        }
        viewModelScope.launch {
            repository.updateProfile { it.copy(selectedVehicleId = vehicleId) }
        }
        evalJs("window.JoharGame && window.JoharGame.setVehicle('$vehicleId');")
        closeModal()
        showToast("${spec.shortName} ready at Johar Barha!")
    }

    fun startMission(missionId: String) {
        activeModal.value = ActiveModal.NONE
        val spec = GameCatalog.missionById(missionId)
        evalJs("window.JoharGame && window.JoharGame.startMission('$missionId');")
        if (spec != null) {
            showToast("Mission: ${spec.title} — Golden arrow follow karo!")
        }
    }

    fun abortMission() {
        evalJs("window.JoharGame && window.JoharGame.abortMission();")
        showToast("Mission cancelled. Free roam in Johar Barha!")
    }

    fun respawnAtChowk() {
        evalJs("window.JoharGame && window.JoharGame.respawnAtChowk();")
    }

    fun cycleCamera() {
        evalJs("window.JoharGame && window.JoharGame.cycleCamera();")
    }

    fun toggleWalkMode() {
        releaseAllControls()
        evalJs("window.JoharGame && window.JoharGame.toggleWalkMode();")
    }

    // --- Settings ---

    fun setSteeringMode(mode: String) {
        viewModelScope.launch { repository.updateProfile { it.copy(steeringMode = mode) } }
    }

    fun setTimeOfDay(mode: String) {
        viewModelScope.launch { repository.updateProfile { it.copy(timeOfDay = mode) } }
        evalJs("window.JoharGame && window.JoharGame.setTimeOfDay('$mode');")
    }

    fun setGraphicsQuality(quality: String) {
        viewModelScope.launch { repository.updateProfile { it.copy(graphicsQuality = quality) } }
        evalJs("window.JoharGame && window.JoharGame.setGraphicsQuality('$quality');")
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateProfile { it.copy(soundEnabled = enabled) } }
        evalJs("window.JoharGame && window.JoharGame.setSoundEnabled($enabled);")
    }

    fun refreshOsmRoads() {
        osmStatus.value = "Fetching Johar Barha OSM roads..."
        evalJs("window.JoharGame && window.JoharGame.loadOsm();")
    }

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetProgress()
            evalJs("window.JoharGame && window.JoharGame.setVehicle('scorpio'); window.JoharGame.abortMission(); window.JoharGame.respawnAtChowk();")
            showToast("Save data reset ho gaya!")
        }
    }

    // --- Real-time Multi-Touch Controls ---

    private fun pushControlsToJs() {
        val steerVal = wheelSteer ?: when {
            steerLeft && !steerRight -> -1f
            steerRight && !steerLeft -> 1f
            else -> 0f
        }
        evalJs("window.JoharGame && window.JoharGame.setControls($steerVal, $gasPressed, $brakePressed, $driftPressed);")
    }

    fun setSteerLeft(pressed: Boolean) {
        steerLeft = pressed
        wheelSteer = null
        pushControlsToJs()
    }

    fun setSteerRight(pressed: Boolean) {
        steerRight = pressed
        wheelSteer = null
        pushControlsToJs()
    }

    fun setWheelSteer(value: Float) {
        wheelSteer = value.coerceIn(-1f, 1f)
        pushControlsToJs()
    }

    fun setGas(pressed: Boolean) {
        gasPressed = pressed
        pushControlsToJs()
    }

    fun setBrake(pressed: Boolean) {
        brakePressed = pressed
        pushControlsToJs()
    }

    fun setDrift(pressed: Boolean) {
        driftPressed = pressed
        pushControlsToJs()
    }

    fun setHorn(pressed: Boolean) {
        evalJs("window.JoharGame && window.JoharGame.setHorn($pressed);")
    }

    fun triggerJump() {
        evalJs("window.JoharGame && window.JoharGame.triggerJump();")
    }

    fun releaseAllControls() {
        steerLeft = false
        steerRight = false
        wheelSteer = null
        gasPressed = false
        brakePressed = false
        driftPressed = false
        pushControlsToJs()
        evalJs("window.JoharGame && window.JoharGame.setHorn(false);")
    }

    fun buildShareSwagText(): String {
        val state = uiState.value
        val completedCount = state.missionsProgress.values.count { it.completed }
        val jholaCount = state.jholaProgress.values.count { it.unlocked }
        val unlockedCars = GameCatalog.vehicles.filter { state.profile.swagPoints >= it.requiredSwag }
        val topCar = unlockedCars.lastOrNull()?.shortName ?: "Scorpio-N / Thar"
        return "Johar Barha 3D: Desi Swag & Luxury Drive!\n" +
            "Gram Johar Barha, Bareli, Raisen (MP) mein mera record:\n" +
            "• Desi Swag Points: ${state.profile.swagPoints}\n" +
            "• Top Luxury Car: $topCar (${unlockedCars.size}/4 unlocked)\n" +
            "• Desi Missions Done: $completedCount/8\n" +
            "• Desi Jhola Items: $jholaCount/7\n" +
            "Sarpanch ji ke 1.5 km border ke andar full swag chalao — kya tum mera score tod sakte ho?"
    }
}

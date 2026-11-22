package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val dao: GameDao) {
    val profileFlow: Flow<PlayerProfileEntity> =
        dao.observeProfile().map { it ?: PlayerProfileEntity() }

    val missionsFlow: Flow<Map<String, MissionProgressEntity>> =
        dao.observeMissions().map { list -> list.associateBy { it.missionId } }

    val jholaFlow: Flow<Map<String, JholaItemEntity>> =
        dao.observeJhola().map { list -> list.associateBy { it.itemId } }

    suspend fun ensureInitialized() {
        if (dao.getProfileOnce() == null) {
            dao.upsertProfile(PlayerProfileEntity())
        }
    }

    suspend fun updateProfile(transform: (PlayerProfileEntity) -> PlayerProfileEntity) {
        val current = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(transform(current))
    }

    suspend fun recordMissionVictory(
        missionId: String,
        swagReward: Int,
        jholaItemId: String,
        elapsedSeconds: Int,
    ): Pair<Int, List<String>> {
        val prevProfile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val prevSwag = prevProfile.swagPoints
        val newSwag = prevSwag + swagReward

        val newlyUnlockedCars = GameCatalog.vehicles
            .filter { it.requiredSwag in (prevSwag + 1)..newSwag }
            .map { it.shortName }

        dao.upsertProfile(prevProfile.copy(swagPoints = newSwag))

        val prevMission = dao.getMissionOnce(missionId)
        val bestTime = when {
            elapsedSeconds <= 0 -> prevMission?.bestTimeSec
            prevMission?.bestTimeSec == null -> elapsedSeconds
            else -> minOf(prevMission.bestTimeSec, elapsedSeconds)
        }
        dao.upsertMission(
            MissionProgressEntity(
                missionId = missionId,
                completed = true,
                bestTimeSec = bestTime,
                completions = (prevMission?.completions ?: 0) + 1,
            ),
        )

        dao.upsertJholaItem(
            JholaItemEntity(
                itemId = jholaItemId,
                unlocked = true,
                unlockedAtMillis = System.currentTimeMillis(),
            ),
        )

        return newSwag to newlyUnlockedCars
    }

    suspend fun addFreeRoamSwag(points: Int) {
        if (points <= 0) return
        updateProfile { it.copy(swagPoints = it.swagPoints + points) }
    }

    suspend fun addDistanceMeters(deltaMeters: Float) {
        if (deltaMeters <= 0f) return
        updateProfile { it.copy(totalDistanceMeters = it.totalDistanceMeters + deltaMeters) }
    }

    suspend fun resetProgress() {
        dao.clearMissions()
        dao.clearJhola()
        val current = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(
            PlayerProfileEntity(
                steeringMode = current.steeringMode,
                timeOfDay = current.timeOfDay,
                graphicsQuality = current.graphicsQuality,
                soundEnabled = current.soundEnabled,
                osmEnabled = current.osmEnabled,
            ),
        )
    }
}

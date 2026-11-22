package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val swagPoints: Int = 0,
    val selectedVehicleId: String = "scorpio",
    val steeringMode: String = "BUTTONS", // BUTTONS | WHEEL
    val timeOfDay: String = "AFTERNOON", // AFTERNOON | GOLDEN_HOUR | NIGHT
    val graphicsQuality: String = "HIGH", // HIGH | BALANCED
    val soundEnabled: Boolean = true,
    val osmEnabled: Boolean = true,
    val totalDistanceMeters: Float = 0f,
)

@Entity(tableName = "mission_progress")
data class MissionProgressEntity(
    @PrimaryKey val missionId: String,
    val completed: Boolean = false,
    val bestTimeSec: Int? = null,
    val completions: Int = 0,
)

@Entity(tableName = "jhola_inventory")
data class JholaItemEntity(
    @PrimaryKey val itemId: String,
    val unlocked: Boolean = false,
    val unlockedAtMillis: Long? = null,
)

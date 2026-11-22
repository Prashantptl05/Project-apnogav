package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM mission_progress")
    fun observeMissions(): Flow<List<MissionProgressEntity>>

    @Query("SELECT * FROM mission_progress WHERE missionId = :missionId")
    suspend fun getMissionOnce(missionId: String): MissionProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMission(mission: MissionProgressEntity)

    @Query("SELECT * FROM jhola_inventory")
    fun observeJhola(): Flow<List<JholaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJholaItem(item: JholaItemEntity)

    @Query("DELETE FROM mission_progress")
    suspend fun clearMissions()

    @Query("DELETE FROM jhola_inventory")
    suspend fun clearJhola()
}

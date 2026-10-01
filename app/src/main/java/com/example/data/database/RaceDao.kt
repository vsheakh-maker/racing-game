package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RaceDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun getPlayerProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getPlayerProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM race_records ORDER BY bestLapTimeMs ASC LIMIT 20")
    fun getBestLapRecords(): Flow<List<RaceRecordEntity>>

    @Query("SELECT * FROM race_records ORDER BY timestamp DESC LIMIT 20")
    fun getRecentRaces(): Flow<List<RaceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaceRecord(record: RaceRecordEntity)
}

package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val credits: Int = 15000,
    val stars: Int = 0,
    val selectedCarId: String = "apex_gt",
    val unlockedCarsCsv: String = "apex_gt",
    val carUpgradesCsv: String = "apex_gt:1:1:1", // carId:engine:handling:nitro
    val carColorsCsv: String = "apex_gt:${0xFFFF1E40L}",
    val completedCupsCsv: String = ""
)

@Entity(tableName = "race_records")
data class RaceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackName: String = "Glen Canyon Dam",
    val gameMode: String,
    val bestLapTimeMs: Long,
    val totalTimeMs: Long,
    val carId: String,
    val finishPosition: Int,
    val driftScore: Int,
    val timestamp: Long = System.currentTimeMillis()
)

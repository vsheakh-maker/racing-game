package com.example.data.database

import com.example.data.model.CarModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class RaceRepository(private val raceDao: RaceDao) {
    val playerProfile: Flow<PlayerProfileEntity?> = raceDao.getPlayerProfile()
    val bestLapRecords: Flow<List<RaceRecordEntity>> = raceDao.getBestLapRecords()
    val recentRaces: Flow<List<RaceRecordEntity>> = raceDao.getRecentRaces()

    suspend fun getOrCreateProfile(): PlayerProfileEntity {
        var profile = raceDao.getPlayerProfileOnce()
        if (profile == null) {
            profile = PlayerProfileEntity()
            raceDao.insertOrUpdateProfile(profile)
        }
        return profile
    }

    suspend fun addCredits(amount: Int) {
        val current = getOrCreateProfile()
        val updated = current.copy(credits = current.credits + amount)
        raceDao.insertOrUpdateProfile(updated)
    }

    suspend fun unlockCar(carId: String, price: Int): Boolean {
        val current = getOrCreateProfile()
        if (current.credits < price) return false
        val unlockedList = current.unlockedCarsCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        unlockedList.add(carId)
        val updated = current.copy(
            credits = current.credits - price,
            unlockedCarsCsv = unlockedList.joinToString(",")
        )
        raceDao.insertOrUpdateProfile(updated)
        return true
    }

    suspend fun selectCar(carId: String) {
        val current = getOrCreateProfile()
        raceDao.insertOrUpdateProfile(current.copy(selectedCarId = carId))
    }

    suspend fun upgradeCar(carId: String, type: String, price: Int): Boolean {
        val current = getOrCreateProfile()
        if (current.credits < price) return false

        // Parse upgrades map
        val map = current.carUpgradesCsv.split(",").filter { it.isNotBlank() }.associate {
            val parts = it.split(":")
            parts[0] to Triple(parts.getOrElse(1) { "1" }.toInt(), parts.getOrElse(2) { "1" }.toInt(), parts.getOrElse(3) { "1" }.toInt())
        }.toMutableMap()

        val (eng, hnd, nit) = map[carId] ?: Triple(1, 1, 1)
        val newTriple = when (type) {
            "engine" -> Triple(eng + 1, hnd, nit)
            "handling" -> Triple(eng, hnd + 1, nit)
            "nitro" -> Triple(eng, hnd, nit + 1)
            else -> Triple(eng, hnd, nit)
        }
        map[carId] = newTriple

        val newCsv = map.entries.joinToString(",") { "${it.key}:${it.value.first}:${it.value.second}:${it.value.third}" }
        val updated = current.copy(credits = current.credits - price, carUpgradesCsv = newCsv)
        raceDao.insertOrUpdateProfile(updated)
        return true
    }

    suspend fun setCarColor(carId: String, colorHex: Long) {
        val current = getOrCreateProfile()
        val map = current.carColorsCsv.split(",").filter { it.isNotBlank() }.associate {
            val p = it.split(":")
            p[0] to p.getOrElse(1) { "0" }.toLong()
        }.toMutableMap()
        map[carId] = colorHex
        val newCsv = map.entries.joinToString(",") { "${it.key}:${it.value}" }
        raceDao.insertOrUpdateProfile(current.copy(carColorsCsv = newCsv))
    }

    suspend fun recordRaceResult(
        gameMode: String,
        bestLapTimeMs: Long,
        totalTimeMs: Long,
        carId: String,
        position: Int,
        driftScore: Int,
        rewardCredits: Int,
        cupId: String? = null
    ) {
        raceDao.insertRaceRecord(
            RaceRecordEntity(
                gameMode = gameMode,
                bestLapTimeMs = bestLapTimeMs,
                totalTimeMs = totalTimeMs,
                carId = carId,
                finishPosition = position,
                driftScore = driftScore
            )
        )
        val profile = getOrCreateProfile()
        var completedCups = profile.completedCupsCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        var starsEarned = 0
        if (cupId != null && position <= 3) {
            if (!completedCups.contains(cupId)) {
                completedCups.add(cupId)
                starsEarned = when (position) {
                    1 -> 3
                    2 -> 2
                    else -> 1
                }
            }
        }
        val updated = profile.copy(
            credits = profile.credits + rewardCredits,
            stars = profile.stars + starsEarned,
            completedCupsCsv = completedCups.joinToString(",")
        )
        raceDao.insertOrUpdateProfile(updated)
    }
}

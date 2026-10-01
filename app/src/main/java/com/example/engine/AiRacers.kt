package com.example.engine

import com.example.data.model.AiDifficulty
import com.example.data.model.CarModel
import com.example.data.model.RaceRacerInfo
import kotlin.math.*

class AiOpponent(
    val id: String,
    val name: String,
    val carModel: CarModel,
    val colorHex: Long,
    var posX: Float,
    var posY: Float,
    var posZ: Float,
    var headingDeg: Float,
    var lineOffset: Float = 0f // lateral offset from centerline (-3.5m to +3.5m)
) {
    var speedMps: Float = 0f
    var speedKmh: Float = 0f
    var currentLap: Int = 1
    var lastPassedCpIndex: Int = 0
    var totalDistanceMeters: Float = 0f
    var lapProgress: Float = 0f

    fun update(
        dt: Float,
        trackData: TrackData,
        difficulty: AiDifficulty,
        allRacers: List<AiOpponent>,
        playerPos: Triple<Float, Float, Float>
    ) {
        val n = trackData.checkpoints.size
        val nearestIdx = trackData.findNearestCheckpointIndex(posX, posZ, lastPassedCpIndex)
        lastPassedCpIndex = nearestIdx

        // Checkpoint navigation
        val targetCpIdx = (nearestIdx + 3) % n
        val targetCp = trackData.checkpoints[targetCpIdx]

        // Target position with lateral lane offset
        val targetX = targetCp.x + targetCp.nx * lineOffset
        val targetZ = targetCp.z + targetCp.nz * lineOffset

        val dx = targetX - posX
        val dz = targetZ - posZ
        val distToTarget = hypot(dx, dz)
        val desiredHeadingRad = atan2(dx.toDouble(), dz.toDouble())
        val desiredHeadingDeg = Math.toDegrees(desiredHeadingRad).toFloat()

        // Turn smoothly towards desired heading
        var angleDiff = desiredHeadingDeg - headingDeg
        while (angleDiff > 180f) angleDiff -= 360f
        while (angleDiff < -180f) angleDiff += 360f

        val turnSpeed = 90f * dt
        headingDeg += angleDiff.coerceIn(-turnSpeed, turnSpeed)

        // Corner curvature speed regulation
        val cornerSharpness = abs(angleDiff)
        val maxSpeedForCorner = when {
            cornerSharpness > 45f -> 95f // tight hairpin
            cornerSharpness > 25f -> 160f // medium curve
            else -> carModel.topSpeedKmh // straight
        }

        val baseMaxSpeedKmh = min(carModel.topSpeedKmh * difficulty.speedMultiplier, maxSpeedForCorner)
        val targetSpeedMps = baseMaxSpeedKmh / 3.6f

        if (speedMps < targetSpeedMps) {
            speedMps += (7.5f * difficulty.speedMultiplier) * dt
        } else {
            speedMps -= 15.0f * dt
        }
        speedKmh = speedMps * 3.6f

        // Move forward along heading
        val moveRad = Math.toRadians(headingDeg.toDouble())
        posX += (sin(moveRad) * speedMps * dt).toFloat()
        posZ += (cos(moveRad) * speedMps * dt).toFloat()

        // Clamp AI strictly inside road corridor boundaries
        val cp = trackData.checkpoints[nearestIdx]
        val dxCp = posX - cp.x
        val dzCp = posZ - cp.z
        val lat = dxCp * cp.nx + dzCp * cp.nz
        val longOff = dxCp * cp.tx + dzCp * cp.tz
        val maxSafe = (cp.width * 0.5f - 1.2f).coerceAtLeast(2.0f)
        if (abs(lat) > maxSafe) {
            val clampedLat = lat.coerceIn(-maxSafe, maxSafe)
            posX = cp.x + cp.tx * longOff + cp.nx * clampedLat
            posZ = cp.z + cp.tz * longOff + cp.nz * clampedLat
        }

        // Mutual collision separation between rival AI cars (physical touch, never cross)
        val carCollisionRadius = 2.15f
        for (other in allRacers) {
            if (other.id != this.id) {
                val dX = posX - other.posX
                val dZ = posZ - other.posZ
                val d = hypot(dX, dZ)
                if (d < carCollisionRadius && d > 0.001f) {
                    val overlap = (carCollisionRadius - d) * 0.5f
                    posX += (dX / d) * overlap
                    posZ += (dZ / d) * overlap
                }
            }
        }

        // Follow ground elevation
        val groundY = trackData.getInterpolatedElevation(posX, posZ, nearestIdx)
        posY += (groundY - posY) * 12f * dt

        // Lap progress calculation
        lapProgress = nearestIdx.toFloat() / n.toFloat()
        totalDistanceMeters += speedMps * dt
    }
}

class AiManager {
    val opponents = ArrayList<AiOpponent>()

    fun initAiRacers(trackData: TrackData, playerCarId: String) {
        opponents.clear()

        val aiNames = listOf("Viper", "Ghost", "Storm", "NitroRex", "Phoenix")
        val availableCars = CarModel.ALL_CARS
        val colors = listOf(0xFFFFD700L, 0xFF00E5FFL, 0xFF00E676L, 0xFF7C4DFFL, 0xFFFFFFFFL)

        // Starting grid slots: slots 1 to 5 (player is usually in slot 0 or 1)
        for (i in 0 until min(5, trackData.gridSlots.size - 1)) {
            val slot = trackData.gridSlots[i + 1]
            val car = availableCars[(i + 1) % availableCars.size]
            val color = colors[i % colors.size]
            val offset = if (i % 2 == 0) -2.5f else 2.5f

            opponents.add(
                AiOpponent(
                    id = "ai_$i",
                    name = aiNames[i],
                    carModel = car,
                    colorHex = color,
                    posX = slot.x,
                    posY = slot.y,
                    posZ = slot.z,
                    headingDeg = slot.heading,
                    lineOffset = offset
                )
            )
        }
    }

    fun updateAll(
        dt: Float,
        trackData: TrackData,
        difficulty: AiDifficulty,
        playerPos: Triple<Float, Float, Float>
    ) {
        opponents.forEach { ai ->
            ai.update(dt, trackData, difficulty, opponents, playerPos)
        }
    }

    fun getRacerInfos(playerInfo: RaceRacerInfo): List<RaceRacerInfo> {
        val list = ArrayList<RaceRacerInfo>()
        list.add(playerInfo)
        opponents.forEach { ai ->
            list.add(
                RaceRacerInfo(
                    id = ai.id,
                    name = ai.name,
                    carName = ai.carModel.name,
                    colorHex = ai.colorHex,
                    isPlayer = false,
                    currentPosition = 1,
                    currentLap = ai.currentLap,
                    lapProgress = ai.lapProgress,
                    totalDistanceMeters = ai.totalDistanceMeters,
                    currentSpeedKmh = ai.speedKmh,
                    posX = ai.posX,
                    posY = ai.posY,
                    posZ = ai.posZ,
                    headingDeg = ai.headingDeg
                )
            )
        }
        // Rank by total distance traveled
        list.sortByDescending { it.totalDistanceMeters }
        return list.mapIndexed { index, racer ->
            racer.copy(currentPosition = index + 1)
        }
    }
}

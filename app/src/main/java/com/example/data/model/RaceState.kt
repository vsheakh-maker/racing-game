package com.example.data.model

enum class RaceStatus {
    COUNTDOWN,
    RACING,
    PAUSED,
    FINISHED
}

data class RaceRacerInfo(
    val id: String,
    val name: String,
    val carName: String,
    val colorHex: Long,
    val isPlayer: Boolean,
    val currentPosition: Int,
    val currentLap: Int,
    val lapProgress: Float, // 0.0 to 1.0 along circuit
    val totalDistanceMeters: Float,
    val currentSpeedKmh: Float,
    val posX: Float,
    val posY: Float,
    val posZ: Float,
    val headingDeg: Float
)

data class RaceTelemetry(
    val speedKmh: Float = 0f,
    val rpm: Float = 1000f,
    val maxRpm: Float = 8500f,
    val gear: Int = 1,
    val nitroAmount: Float = 100f,
    val maxNitro: Float = 100f,
    val isNitroActive: Boolean = false,
    val isOverdriveActive: Boolean = false,
    val isLaunchControlActive: Boolean = false,
    val isDrafting: Boolean = false,
    val isDrifting: Boolean = false,
    val driftScore: Int = 0,
    val driftAngle: Float = 0f,
    val isOffTrack: Boolean = false
)

data class RaceState(
    val status: RaceStatus = RaceStatus.COUNTDOWN,
    val countdownNumber: Int = 3, // 3, 2, 1, 0 (GO!)
    val currentLap: Int = 1,
    val totalLaps: Int = 3,
    val currentPosition: Int = 1,
    val totalRacers: Int = 5,
    val currentLapTimeMs: Long = 0L,
    val bestLapTimeMs: Long = 0L,
    val totalRaceTimeMs: Long = 0L,
    val sector1TimeMs: Long = 0L,
    val sector2TimeMs: Long = 0L,
    val lastLapDeltaMs: Long = 0L,
    val telemetry: RaceTelemetry = RaceTelemetry(),
    val racers: List<RaceRacerInfo> = emptyList(),
    val cameraMode: CameraMode = CameraMode.CHASE_CAM
)

enum class CameraMode(val label: String) {
    CHASE_CAM("Chase"),
    HOOD_CAM("Cockpit/Hood"),
    ORBIT_CAM("Far View")
}

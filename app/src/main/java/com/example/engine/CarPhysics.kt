package com.example.engine

import com.example.data.model.CarModel
import com.example.data.model.WeatherCondition
import kotlin.math.*
import kotlin.random.Random

enum class DrivingAssistMode(val label: String) {
    FULL_ASSIST("Full Assists"),
    SPORT("Sport ESC"),
    SIMULATION("Pro Raw")
}

class CarPhysics(
    val carModel: CarModel,
    var posX: Float,
    var posY: Float,
    var posZ: Float,
    var headingDeg: Float,
    var userHeightOffset: Float = 0.0f
) {
    var speedMps: Float = 0f
    var speedKmh: Float = 0f
        private set
    var rpm: Float = 1000f
        private set
    var gear: Int = 1
        private set

    // Multi-Stage Nitro Booster System
    var nitroAmount: Float = carModel.nitroCapacity
    var isNitroActive: Boolean = false
        private set
    var isOverdriveActive: Boolean = false
        private set

    // Launch Control System
    var isLaunchControlActive: Boolean = false
        private set
    var launchControlCharge: Float = 0f // 0f to 1f
        private set

    // Transmission
    var isManualTransmission: Boolean = false
    var assistMode: DrivingAssistMode = DrivingAssistMode.FULL_ASSIST

    // Drift & Dynamics
    var isDrifting: Boolean = false
        private set
    var driftAngle: Float = 0f // degrees
        private set
    var driftScore: Int = 0
        private set
    var tireSlipAmount: Float = 0f
        private set
    var isOffTrack: Boolean = false
        private set
    var collisionImpact: Float = 0f
    var barrierContact: Boolean = false
        private set

    // Slipstream Aerodynamic Drafting
    var isDrafting: Boolean = false
        private set
    var draftingDistance: Float = 0f
        private set
    var opponents: List<AiOpponent> = emptyList()

    // Audio & Particle Triggers
    var triggerCrash: Boolean = false
    var triggerExhaustPop: Boolean = false
    var triggerGearShift: Boolean = false
    private var lastThrottle: Float = 0f

    // Weather condition influencing track friction & tire grip
    var weatherCondition: WeatherCondition = WeatherCondition.SUNNY

    // Pitch and roll for suspension/elevation changes
    var pitchDeg: Float = 0f
    var rollDeg: Float = 0f
    var wheelRotationRad: Float = 0f

    // 4-Wheel Independent Suspension Compression (0f to 1f)
    var flSuspension: Float = 0.5f
    var frSuspension: Float = 0.5f
    var rlSuspension: Float = 0.5f
    var rrSuspension: Float = 0.5f

    // Smoothed steering for high-stability handling
    private var currentSteerAngle: Float = 0f
    var currentCheckpointIdx: Int = 0

    // Chassis dimensions
    val wheelbase: Float = 2.55f
    val trackWidth: Float = 1.50f
    val wheelRadius: Float = 0.29f

    // Pre-allocated alignment object for ZERO GC in hot update loop
    val alignment: CarAlignment = CarAlignment()
    var latestAlignment: CarAlignment? = alignment

    // Gear ratios for 5-speed transmission
    val maxGears = 5
    private val gearMaxSpeedsKmh = floatArrayOf(65f, 115f, 170f, 225f, 320f)

    fun shiftUp(): Boolean {
        if (gear < maxGears) {
            gear++
            triggerGearShift = true
            return true
        }
        return false
    }

    fun shiftDown(): Boolean {
        if (gear > 1) {
            gear--
            triggerGearShift = true
            triggerExhaustPop = true
            return true
        }
        return false
    }

    fun update(
        dt: Float,
        throttleInput: Float,
        brakeInput: Float,
        steerInput: Float,
        handbrakeInput: Boolean,
        nitroInput: Boolean,
        trackData: TrackData,
        roadCollision: RoadCollisionSystem? = null,
        overdriveInput: Boolean = false
    ) {
        val friction = weatherCondition.frictionMultiplier
        val isBoosting = nitroInput && nitroAmount > 0f && speedKmh > 15f
        val isOverdrive = overdriveInput && isBoosting && nitroAmount > 15f

        isNitroActive = isBoosting && !isOverdrive
        isOverdriveActive = isOverdrive

        val boostMultiplier = when {
            isOverdriveActive -> 1.85f
            isNitroActive -> 1.35f
            else -> 1.0f
        }

        val topSpeedMultiplier = when {
            isOverdriveActive -> 1.25f
            isNitroActive -> 1.12f
            else -> 1.0f
        }

        val maxSpeedKmh = carModel.topSpeedKmh * topSpeedMultiplier
        val maxSpeedMps = maxSpeedKmh / 3.6f

        // Nitro consumption
        if (isOverdriveActive) {
            nitroAmount = (nitroAmount - 38f * dt).coerceAtLeast(0f)
        } else if (isNitroActive) {
            nitroAmount = (nitroAmount - 20f * dt).coerceAtLeast(0f)
        } else {
            // Passive recharge (enhanced by high speed and drifting)
            val rechargeRate = 4.0f + (if (isDrifting) 8.0f else 0.0f)
            nitroAmount = (nitroAmount + rechargeRate * dt).coerceAtMost(carModel.nitroCapacity)
        }

        // LAUNCH CONTROL (Holding brake + full throttle while stopped)
        val readyToLaunch = speedKmh < 2f && throttleInput > 0.8f && brakeInput > 0.8f
        if (readyToLaunch) {
            isLaunchControlActive = true
            launchControlCharge = (launchControlCharge + 1.8f * dt).coerceAtMost(1.0f)
            if (Random.nextFloat() < 0.15f) {
                triggerExhaustPop = true
            }
        } else if (isLaunchControlActive && brakeInput < 0.2f && throttleInput > 0.8f) {
            // ROCKET LAUNCH EXPLOSION!
            isLaunchControlActive = false
            speedMps = (14.0f * launchControlCharge).coerceAtLeast(8.0f)
            speedKmh = speedMps * 3.6f
            launchControlCharge = 0f
            triggerExhaustPop = true
        } else {
            isLaunchControlActive = false
            launchControlCharge = 0f
        }

        // Acceleration calculation with Traction Control System (TCS)
        val tcsFactor = when (assistMode) {
            DrivingAssistMode.FULL_ASSIST -> (0.80f + 0.20f * friction)
            DrivingAssistMode.SPORT -> (0.70f + 0.30f * friction)
            DrivingAssistMode.SIMULATION -> 1.0f
        }

        val baseAccel = (27.78f / carModel.acceleration) * boostMultiplier * tcsFactor
        val powerCurve = (1.0f - (speedMps / (maxSpeedMps + 5f)).pow(1.35f)).coerceIn(0.06f, 1.0f)

        if (throttleInput > 0f && brakeInput <= 0.1f && !isLaunchControlActive) {
            val forwardForce = throttleInput * baseAccel * powerCurve
            speedMps += forwardForce * dt
        }

        // SLIPSTREAM DRAFTING TOW (Vacuum aerodynamic tow behind lead cars)
        var closeLeadDist = 999f
        var inDraft = false
        val fwdRad = Math.toRadians(headingDeg.toDouble())
        val fwdX = sin(fwdRad).toFloat()
        val fwdZ = cos(fwdRad).toFloat()

        for (ai in opponents) {
            val dX = ai.posX - posX
            val dZ = ai.posZ - posZ
            val dist = sqrt(dX * dX + dZ * dZ)
            if (dist in 3.0f..32.0f && dist < closeLeadDist) {
                val dot = (dX * fwdX + dZ * fwdZ) / dist
                if (dot > 0.80f) { // directly aligned behind lead car
                    closeLeadDist = dist
                    inDraft = true
                }
            }
        }

        if (inDraft && speedKmh > 65f) {
            isDrafting = true
            draftingDistance = closeLeadDist
            val draftIntensity = (1.0f - closeLeadDist / 32f).coerceIn(0.15f, 1.0f)
            // Aerodynamic slipstream acceleration boost (+2.5 m/s^2)
            speedMps += (2.8f * draftIntensity) * dt
        } else {
            isDrafting = false
            draftingDistance = 0f
        }

        // Check for exhaust backfire on sudden throttle release at high RPM
        if (lastThrottle > 0.75f && throttleInput < 0.15f && rpm > 4400f) {
            triggerExhaustPop = true
        }
        lastThrottle = throttleInput

        // Braking with Anti-lock Braking System (ABS)
        if (brakeInput > 0f && !isLaunchControlActive) {
            if (speedMps > 1.0f) {
                val absEfficiency = if (assistMode == DrivingAssistMode.SIMULATION) 0.85f else 1.0f
                val brakeForce = brakeInput * 25f * (0.75f + 0.25f * friction) * absEfficiency
                speedMps = (speedMps - brakeForce * dt).coerceAtLeast(0f)
            } else {
                // Reverse
                speedMps = (speedMps - brakeInput * 7f * dt).coerceAtLeast(-12f)
            }
        }

        // Natural resistance (aerodynamic drag & rolling friction)
        val drag = 0.00028f * speedMps * speedMps
        val rollingResistance = 0.65f
        if (speedMps > 0f) {
            speedMps = (speedMps - (drag + rollingResistance) * dt).coerceAtLeast(0f)
        } else if (speedMps < 0f) {
            speedMps = (speedMps + rollingResistance * 2f * dt).coerceAtMost(0f)
        }

        speedKmh = speedMps * 3.6f

        // PROGRESSIVE ADVANCED STEERING
        val targetSteer = steerInput.coerceIn(-1.0f, 1.0f)
        val steerRate = 8.0f // Smooth steering filter
        currentSteerAngle += (targetSteer - currentSteerAngle) * (steerRate * dt).coerceAtMost(1.0f)

        // Speed-sensitive steering ratio
        val speedSensitivity = (1.0f - (speedKmh / 280f) * 0.38f).coerceIn(0.52f, 1.0f)
        val turnRateDeg = currentSteerAngle * (54f * carModel.handling * friction) * speedSensitivity

        // DRIFT DYNAMICS & TIRE SLIP
        val wantsDrift = handbrakeInput && speedKmh > 35f && abs(currentSteerAngle) > 0.10f
        if (wantsDrift || (isDrifting && speedKmh > 25f && abs(driftAngle) > 1.8f)) {
            isDrifting = true
            val maxAngle = if (assistMode == DrivingAssistMode.SIMULATION) 42f else 32f
            val targetDrift = currentSteerAngle * maxAngle * (1.15f - 0.15f * friction)
            driftAngle += (targetDrift - driftAngle) * 5.8f * dt
            tireSlipAmount = (abs(driftAngle) / 25f).coerceIn(0.3f, 1.0f)

            // Drift score with wet multiplier
            val wetBonus = if (weatherCondition.isRainActive) 1.5f else 1.0f
            driftScore += (abs(driftAngle) * (speedKmh / 40f) * 12f * wetBonus * dt).toInt()
            speedMps -= abs(driftAngle) * 0.022f * dt
        } else {
            isDrifting = false
            // Active Electronic Stability Program (ESP) self-centering dampener
            val espDamping = when (assistMode) {
                DrivingAssistMode.FULL_ASSIST -> 12f
                DrivingAssistMode.SPORT -> 8f
                DrivingAssistMode.SIMULATION -> 4.5f
            }
            driftAngle += (0f - driftAngle) * espDamping * dt
            tireSlipAmount = if (brakeInput > 0.7f && speedKmh > 60f) 0.65f else 0.0f
        }

        // Active yaw damping prevents uncontrolled snap-spin
        val yawDelta = (turnRateDeg + driftAngle * 0.22f) * dt
        headingDeg += yawDelta
        if (headingDeg >= 360f) headingDeg -= 360f
        if (headingDeg < 0f) headingDeg += 360f

        // Movement velocity vector
        val moveAngleDeg = headingDeg - driftAngle * 0.45f
        val moveRad = Math.toRadians(moveAngleDeg.toDouble())
        val moveDx = sin(moveRad).toFloat()
        val moveDz = cos(moveRad).toFloat()

        posX += moveDx * speedMps * dt
        posZ += moveDz * speedMps * dt

        // EXACT ROAD COLLISION & 4-WHEEL ALIGNMENT
        if (roadCollision != null && roadCollision.isLoaded) {
            roadCollision.alignCarOnRoad(
                posX = posX,
                posZ = posZ,
                headingDeg = headingDeg,
                wheelbase = wheelbase,
                trackWidth = trackWidth,
                wheelRadius = wheelRadius,
                userHeightOffset = userHeightOffset,
                targetAlignment = alignment
            )

            // Responsive vertical suspension
            posY += (alignment.carY - posY) * (26f * dt).coerceAtMost(1f)

            // Dynamic Pitch: Road gradient + acceleration squat/dive
            val accelPitch = (throttleInput - brakeInput) * 1.5f + (if (isNitroActive) 0.8f else 0f)
            pitchDeg += (alignment.pitchDeg + accelPitch - pitchDeg) * (18f * dt).coerceAtMost(1f)

            // Dynamic Roll: Road banking + cornering lateral G body roll
            val cornerRoll = -currentSteerAngle * (speedKmh / 170f) * 3.4f + driftAngle * 0.08f
            rollDeg += (alignment.rollDeg + cornerRoll - rollDeg) * (18f * dt).coerceAtMost(1f)

            // 4-Wheel independent suspension travel compression
            flSuspension = (0.5f + (accelPitch * 0.1f) - (cornerRoll * 0.1f)).coerceIn(0.1f, 0.9f)
            frSuspension = (0.5f + (accelPitch * 0.1f) + (cornerRoll * 0.1f)).coerceIn(0.1f, 0.9f)
            rlSuspension = (0.5f - (accelPitch * 0.1f) - (cornerRoll * 0.1f)).coerceIn(0.1f, 0.9f)
            rrSuspension = (0.5f - (accelPitch * 0.1f) + (cornerRoll * 0.1f)).coerceIn(0.1f, 0.9f)

            isOffTrack = !alignment.isOnRoad
        } else {
            val nearestCp = trackData.findNearestCheckpointIndex(posX, posZ)
            val targetY = trackData.getInterpolatedElevation(posX, posZ, nearestCp) + wheelRadius + userHeightOffset
            posY += (targetY - posY) * (16f * dt).coerceAtMost(1f)
        }

        // 1. SOLID ROAD BARRIER SYSTEM: STRICTLY KEEPS CAR ON TRACK AND FEELS PHYSICAL TOUCH
        val nearestCp = trackData.findNearestCheckpointIndex(posX, posZ, currentCheckpointIdx)
        currentCheckpointIdx = nearestCp
        val cp = trackData.checkpoints[nearestCp]
        
        val dx = posX - cp.x
        val dz = posZ - cp.z
        val lateralOffset = dx * cp.nx + dz * cp.nz
        val longitudinalOffset = dx * cp.tx + dz * cp.tz

        val roadHalfWidth = cp.width * 0.5f
        val maxSafeLateral = (roadHalfWidth - 0.70f).coerceAtLeast(2.4f)

        if (abs(lateralOffset) > maxSafeLateral) {
            barrierContact = true
            collisionImpact = (speedKmh / 50f).coerceIn(0.25f, 1.0f)
            triggerCrash = true
            triggerExhaustPop = true

            // Absolute mathematical clamp: car strictly remains on track and cannot cross barrier
            val clampedLat = lateralOffset.coerceIn(-maxSafeLateral, maxSafeLateral)
            posX = cp.x + cp.tx * longitudinalOffset + cp.nx * clampedLat
            posZ = cp.z + cp.tz * longitudinalOffset + cp.nz * clampedLat

            // Realistic physical bounce & deflection away from the guardrail
            val normalSign = if (lateralOffset > 0f) -1.0f else 1.0f
            val trackHeadingDeg = Math.toDegrees(atan2(cp.tx.toDouble(), cp.tz.toDouble())).toFloat().let {
                if (it < 0) it + 360f else it
            }
            var diff = (trackHeadingDeg + normalSign * 6.0f) - headingDeg
            while (diff > 180f) diff -= 360f
            while (diff < -180f) diff += 360f
            headingDeg += diff * (10.0f * dt).coerceAtMost(1.0f)

            // Speed friction from scraping wall
            speedMps *= (0.88f + 0.05f * friction)
            driftAngle = -driftAngle * 0.25f
        } else {
            barrierContact = false
            collisionImpact = 0f
        }

        // 2. CAR-TO-CAR PHYSICAL TOUCH & COLLISION RESPONSE (Player vs Rival AI Cars)
        val carCollisionRadius = 2.15f
        for (ai in opponents) {
            val dX = posX - ai.posX
            val dZ = posZ - ai.posZ
            val dist = sqrt(dX * dX + dZ * dZ)
            if (dist < carCollisionRadius && dist > 0.001f) {
                // Physical touch: push both cars apart so they never cross or overlap!
                val overlap = carCollisionRadius - dist
                val nX = dX / dist
                val nZ = dZ / dist

                posX += nX * (overlap * 0.70f)
                posZ += nZ * (overlap * 0.70f)

                ai.posX -= nX * (overlap * 0.30f)
                ai.posZ -= nZ * (overlap * 0.30f)

                barrierContact = true
                collisionImpact = min(1.0f, (speedKmh + ai.speedKmh) / 60f)
                triggerCrash = true
                triggerExhaustPop = true

                // Elastic momentum bump
                val relSpeed = abs(speedMps - ai.speedMps)
                speedMps = (speedMps - relSpeed * 0.20f).coerceAtLeast(0f)
                ai.speedMps = (ai.speedMps + relSpeed * 0.15f)
            }
        }

        // Transmission & Engine RPM
        updateTransmissionAndRpm(throttleInput)

        // Wheel spin
        wheelRotationRad += (speedMps / wheelRadius) * dt
    }

    private fun updateTransmissionAndRpm(throttle: Float) {
        val forwardSpeedKmh = speedKmh.coerceAtLeast(0f)

        // Automatic shifting
        if (!isManualTransmission) {
            var currentGear = 1
            for (g in 1..4) {
                if (forwardSpeedKmh > gearMaxSpeedsKmh[g - 1] * 0.86f) {
                    currentGear = g + 1
                }
            }
            if (currentGear != gear) {
                triggerGearShift = true
                if (currentGear < gear) triggerExhaustPop = true
                gear = currentGear
            }
        }

        // RPM calculation
        val minSpeedInGear = if (gear == 1) 0f else gearMaxSpeedsKmh[gear - 2] * 0.58f
        val maxSpeedInGear = gearMaxSpeedsKmh[gear - 1]
        val ratio = ((forwardSpeedKmh - minSpeedInGear) / (maxSpeedInGear - minSpeedInGear + 0.1f)).coerceIn(0f, 1f)

        val targetRpm = when {
            isLaunchControlActive -> 4500f + Random.nextFloat() * 400f
            speedKmh < 2f && throttle > 0.1f -> 2500f + throttle * 3800f
            else -> 1100f + ratio * 6400f + (if (isNitroActive || isOverdriveActive) 500f else 0f)
        }

        rpm += (targetRpm - rpm) * 0.28f
        rpm = rpm.coerceIn(850f, 7800f)
    }

    fun resetToPosition(x: Float, y: Float, z: Float, heading: Float) {
        posX = x
        posY = y
        posZ = z
        headingDeg = heading
        speedMps = 0f
        speedKmh = 0f
        pitchDeg = 0f
        rollDeg = 0f
        driftAngle = 0f
        isDrifting = false
        currentSteerAngle = 0f
        barrierContact = false
        collisionImpact = 0f
        isLaunchControlActive = false
        launchControlCharge = 0f
        gear = 1
    }

    fun resetToTrack(trackData: TrackData) {
        val nearestCp = trackData.findNearestCheckpointIndex(posX, posZ)
        val cp = trackData.checkpoints[nearestCp]
        val trackHeading = Math.toDegrees(atan2(cp.tx.toDouble(), cp.tz.toDouble())).toFloat().let {
            if (it < 0) it + 360f else it
        }
        resetToPosition(
            x = cp.x,
            y = cp.y + wheelRadius + userHeightOffset,
            z = cp.z,
            heading = trackHeading
        )
    }
}

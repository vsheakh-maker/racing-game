package com.example.engine

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.data.model.CameraMode
import com.example.data.model.RaceStatus
import com.example.data.model.TimeOfDay
import com.example.data.model.WeatherCondition
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*
import kotlin.random.Random

class GlenCanyonRenderer(
    private val context: Context,
    val trackData: TrackData
) : GLSurfaceView.Renderer {

    var playerPhysics: CarPhysics? = null
    var roadCollision: RoadCollisionSystem? = null
    var audioEngine: ProceduralAudio? = null
    var cameraMode: CameraMode = CameraMode.CHASE_CAM
    var timeOfDay: TimeOfDay = TimeOfDay.SUNNY
    var weatherCondition: WeatherCondition = WeatherCondition.SUNNY
    var isDynamicWeather: Boolean = false
    var isVisualInspectionEnabled: Boolean = false
    var raceStatus: RaceStatus = RaceStatus.COUNTDOWN

    // Callback to push telemetry to ViewModel at throttled rate
    var onTelemetryTick: ((speedKmh: Float, rpm: Float, gear: Int, nitro: Float, isNitro: Boolean, isDrift: Boolean, driftScore: Int, driftAngle: Float, isOffTrack: Boolean) -> Unit)? = null
    var onWeatherChanged: ((WeatherCondition) -> Unit)? = null

    private var trackProgram = 0
    private var carProgram = 0
    private var shadowProgram = 0
    private var debugProgram = 0

    private val meshLoader = MeshLoader()
    private val mustangLoader = MustangMeshLoader()
    private val roadBarrierSystem = RoadBarrierSystem()
    private val particleSystem = ParticleSystem()
    private val customCarMeshes = HashMap<String, CustomCarMesh>()

    // Pre-allocated matrices for ZERO GC allocations in onDrawFrame
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Camera smoothing
    private var camX = -206f
    private var camY = 42.0f
    private var camZ = -3f
    private var targetLookX = -180f
    private var targetLookY = 40.5f
    private var targetLookZ = -3f
    private var currentFov = 60.0f
    private var viewportWidth = 1920
    private var viewportHeight = 1080

    // Shader uniform handles
    private var uMVPTrack = 0
    private var uModelTrack = 0
    private var uTextureTrack = 0
    private var uSunDirTrack = 0
    private var uSunColorTrack = 0
    private var uAmbientTrack = 0
    private var uFogColorTrack = 0
    private var uFogStartTrack = 0
    private var uFogEndTrack = 0
    private var uAlphaCutoffTrack = 0
    private var uTintTrack = 0

    private var uMVPCar = 0
    private var uModelCar = 0
    private var uCameraPosCar = 0
    private var uCarColorCar = 0
    private var uSunDirCar = 0
    private var uSunColorCar = 0
    private var uAmbientCar = 0
    private var uIsGlassCar = 0
    private var uIsWheelCar = 0
    private var uIsLightCar = 0

    private var aCarPos = -1
    private var aCarNorm = -1
    private var aCarTex = -1

    // Debug shader handles
    private var uMVPDebug = 0
    private var uColorDebug = 0
    private var aDebugPos = -1
    private var debugLinesVbo = 0
    private var debugLinesVertexCount = 0

    private val debugPointerArray = FloatArray(6)
    private val debugPointerBuffer: FloatBuffer = ByteBuffer.allocateDirect(6 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    private val debugWheelArray = FloatArray(4 * 4 * 6)
    private val debugWheelBuffer: FloatBuffer = ByteBuffer.allocateDirect(96 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    // Rain particles
    private val rainStreakCount = 380
    private val rainPositions = FloatArray(rainStreakCount * 6)
    private val rainVelocities = FloatArray(rainStreakCount)
    private val rainBuffer: FloatBuffer = ByteBuffer.allocateDirect(rainStreakCount * 6 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    // Dynamic weather random interval management
    private var lastWeatherSwitchTime = 0L
    private var dynamicWeatherIntervalMs = 45_000L

    // Frame timing
    private var lastFrameTimeNanos = 0L
    private var lastTelemetryPostTime = 0L

    // In-game input hooks
    var throttleInput: Float = 0f
    var brakeInput: Float = 0f
    var steerInput: Float = 0f
    var handbrakeInput: Boolean = false
    var nitroInput: Boolean = false
    var overdriveInput: Boolean = false

    // AI Racers currently active on the circuit
    var aiRacers: List<AiOpponent> = emptyList()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        trackProgram = ShaderHelper.createProgram(ShaderHelper.TRACK_VERTEX_SHADER, ShaderHelper.TRACK_FRAGMENT_SHADER)
        carProgram = ShaderHelper.createProgram(ShaderHelper.CAR_VERTEX_SHADER, ShaderHelper.CAR_FRAGMENT_SHADER)
        shadowProgram = ShaderHelper.createProgram(ShaderHelper.SHADOW_VERTEX_SHADER, ShaderHelper.SHADOW_FRAGMENT_SHADER)
        debugProgram = ShaderHelper.createProgram(ShaderHelper.DEBUG_VERTEX_SHADER, ShaderHelper.DEBUG_FRAGMENT_SHADER)

        uMVPTrack = GLES20.glGetUniformLocation(trackProgram, "uMVPMatrix")
        uModelTrack = GLES20.glGetUniformLocation(trackProgram, "uModelMatrix")
        uTextureTrack = GLES20.glGetUniformLocation(trackProgram, "uTexture")
        uSunDirTrack = GLES20.glGetUniformLocation(trackProgram, "uSunDirection")
        uSunColorTrack = GLES20.glGetUniformLocation(trackProgram, "uSunColor")
        uAmbientTrack = GLES20.glGetUniformLocation(trackProgram, "uAmbientColor")
        uFogColorTrack = GLES20.glGetUniformLocation(trackProgram, "uFogColor")
        uFogStartTrack = GLES20.glGetUniformLocation(trackProgram, "uFogStart")
        uFogEndTrack = GLES20.glGetUniformLocation(trackProgram, "uFogEnd")
        uAlphaCutoffTrack = GLES20.glGetUniformLocation(trackProgram, "uAlphaCutoff")
        uTintTrack = GLES20.glGetUniformLocation(trackProgram, "uTint")

        uMVPCar = GLES20.glGetUniformLocation(carProgram, "uMVPMatrix")
        uModelCar = GLES20.glGetUniformLocation(carProgram, "uModelMatrix")
        uCameraPosCar = GLES20.glGetUniformLocation(carProgram, "uCameraPos")
        uCarColorCar = GLES20.glGetUniformLocation(carProgram, "uCarColor")
        uSunDirCar = GLES20.glGetUniformLocation(carProgram, "uSunDirection")
        uSunColorCar = GLES20.glGetUniformLocation(carProgram, "uSunColor")
        uAmbientCar = GLES20.glGetUniformLocation(carProgram, "uAmbientColor")
        uIsGlassCar = GLES20.glGetUniformLocation(carProgram, "uIsGlass")
        uIsWheelCar = GLES20.glGetUniformLocation(carProgram, "uIsWheel")
        uIsLightCar = GLES20.glGetUniformLocation(carProgram, "uIsLight")

        aCarPos = GLES20.glGetAttribLocation(carProgram, "aPosition")
        aCarNorm = GLES20.glGetAttribLocation(carProgram, "aNormal")
        aCarTex = GLES20.glGetAttribLocation(carProgram, "aTexCoord")

        uMVPDebug = GLES20.glGetUniformLocation(debugProgram, "uMVPMatrix")
        uColorDebug = GLES20.glGetUniformLocation(debugProgram, "uColor")
        aDebugPos = GLES20.glGetAttribLocation(debugProgram, "aPosition")

        // Load 3D Meshes & Systems
        meshLoader.load(context)
        mustangLoader.load(context)
        roadBarrierSystem.init(trackData)
        particleSystem.init()

        // Preload all custom car models
        com.example.data.model.CarModel.ALL_CARS.forEach { car ->
            if (car.id != "mustang_1965") {
                customCarMeshes[car.id] = CarMeshBuilder.buildCar(car.id)
            }
        }

        initRainParticles()
        initDebugWireframe()

        lastWeatherSwitchTime = System.currentTimeMillis()
        dynamicWeatherIntervalMs = 40_000L + Random.nextLong(25_000L)
    }

    private fun initRainParticles() {
        val radius = 26f
        val height = 22f
        for (i in 0 until rainStreakCount) {
            val idx = i * 6
            val rx = (Random.nextFloat() - 0.5f) * radius * 2f
            val ry = Random.nextFloat() * height
            val rz = (Random.nextFloat() - 0.5f) * radius * 2f
            val len = 0.9f + Random.nextFloat() * 0.8f

            rainPositions[idx] = rx
            rainPositions[idx + 1] = ry
            rainPositions[idx + 2] = rz
            rainPositions[idx + 3] = rx - 0.05f
            rainPositions[idx + 4] = ry - len
            rainPositions[idx + 5] = rz

            rainVelocities[i] = 34f + Random.nextFloat() * 15f
        }
    }

    private fun updateRainParticles(dt: Float, centerCenterX: Float, centerCenterY: Float, centerCenterZ: Float) {
        val radius = 26f
        val height = 22f

        for (i in 0 until rainStreakCount) {
            val idx = i * 6
            val vy = rainVelocities[i] * dt
            rainPositions[idx + 1] -= vy
            rainPositions[idx + 4] -= vy

            if (rainPositions[idx + 1] < centerCenterY - 4f) {
                val rx = centerCenterX + (Random.nextFloat() - 0.5f) * radius * 2f
                val ry = centerCenterY + height + Random.nextFloat() * 4f
                val rz = centerCenterZ + (Random.nextFloat() - 0.5f) * radius * 2f
                val len = 0.9f + Random.nextFloat() * 0.8f

                rainPositions[idx] = rx
                rainPositions[idx + 1] = ry
                rainPositions[idx + 2] = rz
                rainPositions[idx + 3] = rx - 0.05f
                rainPositions[idx + 4] = ry - len
                rainPositions[idx + 5] = rz
            }
        }

        rainBuffer.position(0)
        rainBuffer.put(rainPositions)
        rainBuffer.position(0)
    }

    private fun initDebugWireframe() {
        val rc = roadCollision ?: return
        val lines = rc.getDebugWireframe()
        debugLinesVertexCount = lines.size / 3
        if (debugLinesVertexCount > 0) {
            val bb = ByteBuffer.allocateDirect(lines.size * 4).order(ByteOrder.nativeOrder())
            val fb = bb.asFloatBuffer()
            fb.put(lines)
            fb.position(0)

            val bufs = IntArray(1)
            GLES20.glGenBuffers(1, bufs, 0)
            debugLinesVbo = bufs[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, debugLinesVbo)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, lines.size * 4, fb, GLES20.GL_STATIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
        updateProjection()
    }

    private fun updateProjection() {
        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(projectionMatrix, 0, currentFov, aspect, 0.5f, 3500.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val player = playerPhysics ?: return

        // 1. Synchronized Physics Step
        val now = System.nanoTime()
        val dt = if (lastFrameTimeNanos > 0L) {
            ((now - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.005f, 0.033f)
        } else {
            0.0166f
        }
        lastFrameTimeNanos = now

        // Dynamic Weather state management
        val nowMs = System.currentTimeMillis()
        if (isDynamicWeather && nowMs - lastWeatherSwitchTime >= dynamicWeatherIntervalMs) {
            lastWeatherSwitchTime = nowMs
            dynamicWeatherIntervalMs = 38_000L + Random.nextLong(28_000L)
            val conditions = WeatherCondition.values().filter { it != weatherCondition }
            val nextCondition = conditions.random()
            weatherCondition = nextCondition
            onWeatherChanged?.invoke(nextCondition)
        }

        player.weatherCondition = weatherCondition
        player.opponents = aiRacers

        if (raceStatus == RaceStatus.RACING) {
            player.update(
                dt = dt,
                throttleInput = throttleInput,
                brakeInput = brakeInput,
                steerInput = steerInput,
                handbrakeInput = handbrakeInput,
                nitroInput = nitroInput,
                overdriveInput = overdriveInput,
                trackData = trackData,
                roadCollision = roadCollision
            )

            // Spawn exhaust flames & particles
            val hRad = Math.toRadians(player.headingDeg.toDouble())
            val fwdX = sin(hRad).toFloat()
            val fwdZ = cos(hRad).toFloat()
            val rightX = cos(hRad).toFloat()
            val rightZ = -sin(hRad).toFloat()

            // Left & Right Tailpipe positions
            val rearDist = 2.15f
            val tailLeftX = player.posX - fwdX * rearDist - rightX * 0.45f
            val tailLeftZ = player.posZ - fwdZ * rearDist - rightZ * 0.45f
            val tailRightX = player.posX - fwdX * rearDist + rightX * 0.45f
            val tailRightZ = player.posZ - fwdZ * rearDist + rightZ * 0.45f
            val tailY = player.posY + 0.28f

            if (player.isOverdriveActive) {
                particleSystem.spawnExhaustFlame(tailLeftX, tailY, tailLeftZ, fwdX, fwdZ, isOverdrive = true, isBackfire = false)
                particleSystem.spawnExhaustFlame(tailRightX, tailY, tailRightZ, fwdX, fwdZ, isOverdrive = true, isBackfire = false)
            } else if (player.isNitroActive) {
                particleSystem.spawnExhaustFlame(tailLeftX, tailY, tailLeftZ, fwdX, fwdZ, isOverdrive = false, isBackfire = false)
                particleSystem.spawnExhaustFlame(tailRightX, tailY, tailRightZ, fwdX, fwdZ, isOverdrive = false, isBackfire = false)
            } else if (player.triggerExhaustPop) {
                particleSystem.spawnExhaustFlame(tailLeftX, tailY, tailLeftZ, fwdX, fwdZ, isOverdrive = false, isBackfire = true)
                particleSystem.spawnExhaustFlame(tailRightX, tailY, tailRightZ, fwdX, fwdZ, isOverdrive = false, isBackfire = true)
            }

            // Tire smoke on hard drift or burnout
            if (player.isDrifting || player.tireSlipAmount > 0.2f) {
                val rlX = player.posX - fwdX * 1.3f - rightX * 0.75f
                val rlZ = player.posZ - fwdZ * 1.3f - rightZ * 0.75f
                val rrX = player.posX - fwdX * 1.3f + rightX * 0.75f
                val rrZ = player.posZ - fwdZ * 1.3f + rightZ * 0.75f
                particleSystem.spawnTireSmoke(rlX, player.posY, rlZ, player.speedKmh)
                particleSystem.spawnTireSmoke(rrX, player.posY, rrZ, player.speedKmh)
            }

            // Water spray in rain
            if (weatherCondition.isRainActive && player.speedKmh > 35f) {
                particleSystem.spawnWaterSpray(tailLeftX, tailY, tailLeftZ, fwdX, fwdZ)
                particleSystem.spawnWaterSpray(tailRightX, tailY, tailRightZ, fwdX, fwdZ)
            }

            // Sync audio
            audioEngine?.let { audio ->
                audio.targetRpm = player.rpm
                audio.throttle = throttleInput
                audio.isDrifting = player.isDrifting
                audio.tireSquealAmount = player.tireSlipAmount
                audio.isNitro = player.isNitroActive
                audio.isOverdrive = player.isOverdriveActive
                audio.isRaining = weatherCondition.isRainActive
                audio.soundFreqMultiplier = player.carModel.soundFreqMultiplier
                audio.hasSuperchargerWhine = player.carModel.hasSuperchargerWhine
                audio.hasTurboWhistle = player.carModel.hasTurboWhistle

                if (player.triggerExhaustPop) {
                    audio.triggerExhaustPop = true
                    player.triggerExhaustPop = false
                }
                if (player.triggerGearShift) {
                    audio.triggerGearShift = true
                    player.triggerGearShift = false
                }
                if (player.collisionImpact > 0.25f) {
                    audio.triggerCrash = true
                }
            }

            // Post telemetry throttled
            if (nowMs - lastTelemetryPostTime >= 40L) {
                lastTelemetryPostTime = nowMs
                onTelemetryTick?.invoke(
                    player.speedKmh,
                    player.rpm,
                    player.gear,
                    player.nitroAmount,
                    player.isNitroActive,
                    player.isDrifting,
                    player.driftScore,
                    player.driftAngle,
                    player.isOffTrack
                )
            }
        }

        // Update particle physics
        particleSystem.update(dt)

        // 2. Clear Scene
        val skyColors = weatherCondition.skyColor
        val sunColors = weatherCondition.sunColor
        val ambientColors = weatherCondition.ambientColor
        val fogColors = weatherCondition.fogColor

        GLES20.glClearColor(skyColors[0], skyColors[1], skyColors[2], 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // 3. Dynamic Camera & FOV Speed Dilation
        val targetFov = when {
            player.isOverdriveActive -> 74.0f
            player.isNitroActive -> 68.0f
            player.speedKmh > 200f -> 64.0f
            else -> 60.0f
        }
        currentFov += (targetFov - currentFov) * 0.12f
        updateProjection()

        val hRad = Math.toRadians(player.headingDeg.toDouble())
        val forwardX = sin(hRad).toFloat()
        val forwardZ = cos(hRad).toFloat()

        when (cameraMode) {
            CameraMode.CHASE_CAM -> {
                val speedZoom = (player.speedKmh / 260f) * 1.6f
                // Subtle high-speed camera shake during overdrive
                val shake = if (player.isOverdriveActive) (Random.nextFloat() - 0.5f) * 0.08f else 0.0f

                val desiredCamX = player.posX - forwardX * (5.5f + speedZoom) + shake
                val desiredCamY = player.posY + 1.85f + (player.speedKmh / 320f) * 0.35f + shake
                val desiredCamZ = player.posZ - forwardZ * (5.5f + speedZoom)

                camX += (desiredCamX - camX) * 0.24f
                camY += (desiredCamY - camY) * 0.26f
                camZ += (desiredCamZ - camZ) * 0.24f

                camY = max(camY, player.posY + 0.75f)

                val lookDist = 16f
                targetLookX = player.posX + forwardX * lookDist
                targetLookY = player.posY + 1.05f
                targetLookZ = player.posZ + forwardZ * lookDist
            }
            CameraMode.HOOD_CAM -> {
                camX = player.posX + forwardX * 1.0f
                camY = player.posY + 0.95f
                camZ = player.posZ + forwardZ * 1.0f

                targetLookX = player.posX + forwardX * 28f
                targetLookY = player.posY + 0.90f
                targetLookZ = player.posZ + forwardZ * 28f
            }
            CameraMode.ORBIT_CAM -> {
                camX = player.posX - forwardX * 14f
                camY = player.posY + 8.5f
                camZ = player.posZ - forwardZ * 14f

                targetLookX = player.posX
                targetLookY = player.posY + 0.8f
                targetLookZ = player.posZ
            }
        }

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, targetLookX, targetLookY, targetLookZ, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Directional Sun Light
        val sunDirX = 0.55f; val sunDirY = 0.75f; val sunDirZ = 0.35f
        val lLen = sqrt(sunDirX * sunDirX + sunDirY * sunDirY + sunDirZ * sunDirZ)
        val nSunX = sunDirX / lLen
        val nSunY = sunDirY / lLen
        val nSunZ = sunDirZ / lLen

        // 4. Render Track
        GLES20.glUseProgram(trackProgram)
        GLES20.glUniform3f(uSunDirTrack, nSunX, nSunY, nSunZ)
        GLES20.glUniform3f(uSunColorTrack, sunColors[0], sunColors[1], sunColors[2])
        GLES20.glUniform3f(uAmbientTrack, ambientColors[0], ambientColors[1], ambientColors[2])
        GLES20.glUniform3f(uFogColorTrack, fogColors[0], fogColors[1], fogColors[2])
        GLES20.glUniform1f(uFogStartTrack, weatherCondition.fogStart)
        GLES20.glUniform1f(uFogEndTrack, weatherCondition.fogEnd)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)

        meshLoader.render(
            trackProgram,
            uMVPTrack,
            uModelTrack,
            uTextureTrack,
            uAlphaCutoffTrack,
            uTintTrack,
            mvpMatrix,
            modelMatrix
        )

        // 5. Render Road Barriers
        roadBarrierSystem.render(
            viewProjectionMatrix = viewProjectionMatrix,
            sunDirX = nSunX,
            sunDirY = nSunY,
            sunDirZ = nSunZ,
            sunColors = sunColors,
            ambientColors = ambientColors
        )

        // 6. Render 3D AI Opponent Racers on Track
        for (ai in aiRacers) {
            drawAiCar(
                ai = ai,
                nSunX = nSunX,
                nSunY = nSunY,
                nSunZ = nSunZ
            )
        }

        // 7. Render Player Car (Mustang or Custom Muscle/Supercar)
        if (cameraMode != CameraMode.HOOD_CAM) {
            drawCar(
                player = player,
                nSunX = nSunX,
                nSunY = nSunY,
                nSunZ = nSunZ
            )
        }

        // 7. Render 3D Exhaust Flames, Sparks & Smoke Particles
        particleSystem.render(viewProjectionMatrix)

        // 8. Render 3D Rain Streaks
        if (weatherCondition.isRainActive) {
            renderRain(dt)
        }

        // 9. Visual Inspection Overlay
        if (isVisualInspectionEnabled || player.barrierContact) {
            renderDebugInspection(player)
        }
    }

    private fun drawCar(
        player: CarPhysics,
        nSunX: Float, nSunY: Float, nSunZ: Float
    ) {
        GLES20.glUseProgram(carProgram)
        GLES20.glUniform3f(uCameraPosCar, camX, camY, camZ)
        GLES20.glUniform3f(uSunDirCar, nSunX, nSunY, nSunZ)
        val sunColors = weatherCondition.sunColor
        val ambientColors = weatherCondition.ambientColor
        GLES20.glUniform3f(uSunColorCar, sunColors[0], sunColors[1], sunColors[2])
        GLES20.glUniform3f(uAmbientCar, ambientColors[0] + 0.1f, ambientColors[1] + 0.1f, ambientColors[2] + 0.1f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, player.posX, player.posY, player.posZ)
        Matrix.rotateM(modelMatrix, 0, player.headingDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, player.pitchDeg, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, player.rollDeg, 0f, 0f, 1f)

        Matrix.multiplyMM(mvpMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPCar, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelCar, 1, false, modelMatrix, 0)

        GLES20.glEnableVertexAttribArray(aCarPos)
        GLES20.glEnableVertexAttribArray(aCarNorm)
        GLES20.glEnableVertexAttribArray(aCarTex)

        val carId = player.carModel.id
        if (carId == "mustang_1965") {
            mustangLoader.render(
                carColorHex = player.carModel.selectedColorHex,
                aPosition = aCarPos,
                aNormal = aCarNorm,
                aTexCoord = aCarTex,
                uCarColor = uCarColorCar,
                uIsGlass = uIsGlassCar,
                uIsWheel = uIsWheelCar,
                uIsLight = uIsLightCar
            )
        } else {
            // Render custom procedural mesh for Camaro, Charger HEMI, Shelby, Veyron
            val mesh = customCarMeshes[carId] ?: customCarMeshes.values.firstOrNull()
            if (mesh != null) {
                renderCustomMesh(mesh, player.carModel.selectedColorHex)
            }
        }

        GLES20.glDisableVertexAttribArray(aCarPos)
        GLES20.glDisableVertexAttribArray(aCarNorm)
        GLES20.glDisableVertexAttribArray(aCarTex)
    }

    private fun drawAiCar(
        ai: AiOpponent,
        nSunX: Float, nSunY: Float, nSunZ: Float
    ) {
        GLES20.glUseProgram(carProgram)
        GLES20.glUniform3f(uCameraPosCar, camX, camY, camZ)
        GLES20.glUniform3f(uSunDirCar, nSunX, nSunY, nSunZ)
        val sunColors = weatherCondition.sunColor
        val ambientColors = weatherCondition.ambientColor
        GLES20.glUniform3f(uSunColorCar, sunColors[0], sunColors[1], sunColors[2])
        GLES20.glUniform3f(uAmbientCar, ambientColors[0] + 0.1f, ambientColors[1] + 0.1f, ambientColors[2] + 0.1f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, ai.posX, ai.posY, ai.posZ)
        Matrix.rotateM(modelMatrix, 0, ai.headingDeg, 0f, 1f, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPCar, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelCar, 1, false, modelMatrix, 0)

        GLES20.glEnableVertexAttribArray(aCarPos)
        GLES20.glEnableVertexAttribArray(aCarNorm)
        GLES20.glEnableVertexAttribArray(aCarTex)

        val carId = ai.carModel.id
        if (carId == "mustang_1965") {
            mustangLoader.render(
                carColorHex = ai.colorHex,
                aPosition = aCarPos,
                aNormal = aCarNorm,
                aTexCoord = aCarTex,
                uCarColor = uCarColorCar,
                uIsGlass = uIsGlassCar,
                uIsWheel = uIsWheelCar,
                uIsLight = uIsLightCar
            )
        } else {
            val mesh = customCarMeshes[carId] ?: customCarMeshes.values.firstOrNull()
            if (mesh != null) {
                renderCustomMesh(mesh, ai.colorHex)
            }
        }

        GLES20.glDisableVertexAttribArray(aCarPos)
        GLES20.glDisableVertexAttribArray(aCarNorm)
        GLES20.glDisableVertexAttribArray(aCarTex)
    }

    private fun renderCustomMesh(mesh: CustomCarMesh, colorHex: Long) {
        val stride = 8 * 4
        val cr = ((colorHex shr 16) and 0xFF) / 255f
        val cg = ((colorHex shr 8) and 0xFF) / 255f
        val cb = (colorHex and 0xFF) / 255f

        fun drawBuffer(vbo: Int, ibo: Int, count: Int) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            GLES20.glVertexAttribPointer(aCarPos, 3, GLES20.GL_FLOAT, false, stride, 0)
            GLES20.glVertexAttribPointer(aCarNorm, 3, GLES20.GL_FLOAT, false, stride, 3 * 4)
            GLES20.glVertexAttribPointer(aCarTex, 2, GLES20.GL_FLOAT, false, stride, 6 * 4)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, 0)
        }

        // Body
        GLES20.glUniform4f(uCarColorCar, cr, cg, cb, 1f)
        GLES20.glUniform1i(uIsGlassCar, 0)
        GLES20.glUniform1i(uIsWheelCar, 0)
        GLES20.glUniform1i(uIsLightCar, 0)
        drawBuffer(mesh.bodyVbo, mesh.bodyIbo, mesh.bodyIndexCount)

        // Chrome / Scoop / Diffuser
        GLES20.glUniform4f(uCarColorCar, 0.92f, 0.92f, 0.95f, 1f)
        GLES20.glUniform1i(uIsWheelCar, 1)
        drawBuffer(mesh.chromeVbo, mesh.chromeIbo, mesh.chromeIndexCount)

        // Wheels
        GLES20.glUniform4f(uCarColorCar, 0.12f, 0.12f, 0.12f, 1f)
        drawBuffer(mesh.wheelVbo, mesh.wheelIbo, mesh.wheelIndexCount)

        // Lights
        GLES20.glUniform4f(uCarColorCar, 1f, 0.95f, 0.85f, 1f)
        GLES20.glUniform1i(uIsLightCar, 1)
        drawBuffer(mesh.lightsVbo, mesh.lightsIbo, mesh.lightsIndexCount)

        // Glass
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUniform4f(uCarColorCar, 0.15f, 0.22f, 0.32f, 0.85f)
        GLES20.glUniform1i(uIsGlassCar, 1)
        drawBuffer(mesh.glassVbo, mesh.glassIbo, mesh.glassIndexCount)
        GLES20.glDisable(GLES20.GL_BLEND)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun renderRain(dt: Float) {
        if (debugProgram == 0) return
        updateRainParticles(dt, camX, camY, camZ)

        GLES20.glUseProgram(debugProgram)
        GLES20.glUniformMatrix4fv(uMVPDebug, 1, false, viewProjectionMatrix, 0)
        GLES20.glUniform4f(uColorDebug, 0.72f, 0.82f, 0.94f, 0.65f)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glLineWidth(1.8f)

        GLES20.glEnableVertexAttribArray(aDebugPos)
        GLES20.glVertexAttribPointer(aDebugPos, 3, GLES20.GL_FLOAT, false, 0, rainBuffer)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, rainStreakCount * 2)
        GLES20.glDisableVertexAttribArray(aDebugPos)

        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun renderDebugInspection(player: CarPhysics) {
        if (debugProgram == 0) return
        GLES20.glUseProgram(debugProgram)

        if (debugLinesVbo != 0 && debugLinesVertexCount > 0 && isVisualInspectionEnabled) {
            GLES20.glUniformMatrix4fv(uMVPDebug, 1, false, viewProjectionMatrix, 0)
            GLES20.glUniform4f(uColorDebug, 0.0f, 1.0f, 0.5f, 0.65f)

            GLES20.glEnableVertexAttribArray(aDebugPos)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, debugLinesVbo)
            GLES20.glVertexAttribPointer(aDebugPos, 3, GLES20.GL_FLOAT, false, 3 * 4, 0)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, debugLinesVertexCount)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glDisableVertexAttribArray(aDebugPos)
        }

        player.latestAlignment?.let { alignment ->
            GLES20.glEnableVertexAttribArray(aDebugPos)

            val hRad = Math.toRadians(player.headingDeg.toDouble())
            val fwdX = sin(hRad).toFloat()
            val fwdZ = cos(hRad).toFloat()

            debugPointerArray[0] = player.posX
            debugPointerArray[1] = player.posY + 0.35f
            debugPointerArray[2] = player.posZ
            debugPointerArray[3] = player.posX + fwdX * 5.0f
            debugPointerArray[4] = player.posY + 0.35f
            debugPointerArray[5] = player.posZ + fwdZ * 5.0f

            debugPointerBuffer.position(0)
            debugPointerBuffer.put(debugPointerArray)
            debugPointerBuffer.position(0)

            GLES20.glUniformMatrix4fv(uMVPDebug, 1, false, viewProjectionMatrix, 0)
            GLES20.glUniform4f(uColorDebug, 1.0f, 0.15f, 0.15f, 1.0f)
            GLES20.glVertexAttribPointer(aDebugPos, 3, GLES20.GL_FLOAT, false, 0, debugPointerBuffer)
            GLES20.glLineWidth(4.0f)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)

            var ptr = 0
            val s = 0.28f
            for (i in 0 until 4) {
                val wc = alignment.wheelContacts[i]
                val wy = wc.roadY + 0.06f
                debugWheelArray[ptr++] = wc.x - s; debugWheelArray[ptr++] = wy; debugWheelArray[ptr++] = wc.z
                debugWheelArray[ptr++] = wc.x + s; debugWheelArray[ptr++] = wy; debugWheelArray[ptr++] = wc.z
                debugWheelArray[ptr++] = wc.x; debugWheelArray[ptr++] = wy; debugWheelArray[ptr++] = wc.z - s
                debugWheelArray[ptr++] = wc.x; debugWheelArray[ptr++] = wy; debugWheelArray[ptr++] = wc.z + s
            }

            debugWheelBuffer.position(0)
            debugWheelBuffer.put(debugWheelArray)
            debugWheelBuffer.position(0)

            val wheelColor = if (player.barrierContact) floatArrayOf(1.0f, 0.4f, 0.0f, 1.0f) else floatArrayOf(0.0f, 0.9f, 1.0f, 1.0f)
            GLES20.glUniform4f(uColorDebug, wheelColor[0], wheelColor[1], wheelColor[2], 1.0f)
            GLES20.glVertexAttribPointer(aDebugPos, 3, GLES20.GL_FLOAT, false, 0, debugWheelBuffer)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, ptr / 3)

            GLES20.glDisableVertexAttribArray(aDebugPos)
        }
    }

    fun release() {
        meshLoader.release()
        mustangLoader.release()
        roadBarrierSystem.release()
        particleSystem.release()
        for (m in customCarMeshes.values) {
            m.release()
        }
        customCarMeshes.clear()
        if (debugLinesVbo != 0) {
            GLES20.glDeleteBuffers(1, intArrayOf(debugLinesVbo), 0)
            debugLinesVbo = 0
        }
    }
}

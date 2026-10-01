package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.PlayerProfileEntity
import com.example.data.database.RaceRecordEntity
import com.example.data.database.RaceRepository
import com.example.data.model.*
import com.example.engine.*
import com.example.ui.components.SteeringControlMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class AppScreen {
    MAIN_MENU,
    GARAGE,
    CAREER_SELECT,
    QUICK_RACE_CONFIG,
    RACING,
    RACE_RESULTS,
    SETTINGS
}

class RaceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RaceRepository(AppDatabase.getDatabase(application).raceDao())
    val trackData: TrackData = TrackData.loadFromAssets(application)
    val audioEngine = ProceduralAudio()
    val roadCollision = RoadCollisionSystem()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null && _steeringControlMode.value == SteeringControlMode.TILT) {
                // In landscape mode, tilting along Y-axis corresponds to vehicle steering
                val tiltY = event.values[1]
                inputSteer = (-tiltY / 4.2f).coerceIn(-1.0f, 1.0f)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Database Player profile
    val profile: StateFlow<PlayerProfileEntity?> = repository.playerProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Best records
    val bestRecords: StateFlow<List<RaceRecordEntity>> = repository.bestLapRecords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Race Configuration
    var currentGameMode = GameMode.QUICK_RACE
    var currentLaps = 2
    var currentDifficulty = AiDifficulty.MEDIUM
    var currentTimeOfDay = TimeOfDay.SUNSET
    var selectedCup: CareerCup? = null

    // Weather State Management System
    private val _currentWeather = MutableStateFlow(WeatherCondition.SUNNY)
    val currentWeather: StateFlow<WeatherCondition> = _currentWeather.asStateFlow()
    var isDynamicWeather: Boolean = true

    // Weather change alert toast
    private val _weatherNotification = MutableStateFlow<String?>(null)
    val weatherNotification: StateFlow<String?> = _weatherNotification.asStateFlow()
    private var weatherNotificationJob: Job? = null

    // Starting Position & Road Inspection Configuration
    private val _startingConfig = MutableStateFlow(StartingConfig())
    val startingConfig: StateFlow<StartingConfig> = _startingConfig.asStateFlow()

    // Legendary Cars Roster
    private val _allCars = MutableStateFlow(CarModel.ALL_CARS)
    val allCars: StateFlow<List<CarModel>> = _allCars.asStateFlow()

    private val _selectedCar = MutableStateFlow(CarModel.MUSTANG_1965)
    val selectedCar: StateFlow<CarModel> = _selectedCar.asStateFlow()

    // Advanced Driving & Control Settings
    private val _steeringControlMode = MutableStateFlow(SteeringControlMode.BUTTONS)
    val steeringControlMode: StateFlow<SteeringControlMode> = _steeringControlMode.asStateFlow()

    private val _isManualTransmission = MutableStateFlow(false)
    val isManualTransmission: StateFlow<Boolean> = _isManualTransmission.asStateFlow()

    private val _assistMode = MutableStateFlow(DrivingAssistMode.FULL_ASSIST)
    val assistMode: StateFlow<DrivingAssistMode> = _assistMode.asStateFlow()

    // Live Race Simulation State
    private val _raceState = MutableStateFlow(RaceState())
    val raceState: StateFlow<RaceState> = _raceState.asStateFlow()

    // In-game control inputs
    var inputThrottle: Float = 0f
    var inputBrake: Float = 0f
    var inputSteer: Float = 0f
    var inputHandbrake: Boolean = false
    var inputNitro: Boolean = false
    var inputOverdrive: Boolean = false

    // Engine entities
    var playerPhysics: CarPhysics? = null
    val aiManager = AiManager()

    // Settings
    var useMph = false
    var hapticsEnabled = true

    private var gameLoopJob: Job? = null
    private var lastCpIdx = 0
    private var lapStartTime = 0L
    private var currentLapNumber = 1
    private var bestLapTime = 0L
    private var totalRaceStartTime = 0L

    init {
        // Preload road collision triangles
        viewModelScope.launch(Dispatchers.IO) {
            roadCollision.load(application)
        }
        viewModelScope.launch {
            repository.getOrCreateProfile()
            profile.collect { p ->
                if (p != null) {
                    syncCarsWithProfile(p)
                }
            }
        }
        // Register tilt sensor
        accelerometer?.let {
            sensorManager?.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun syncCarsWithProfile(p: PlayerProfileEntity) {
        val upgradesMap = p.carUpgradesCsv.split(",").filter { it.isNotBlank() }.associate {
            val parts = it.split(":")
            parts[0] to Triple(parts.getOrElse(1) { "1" }.toInt(), parts.getOrElse(2) { "1" }.toInt(), parts.getOrElse(3) { "1" }.toInt())
        }
        val colorsMap = p.carColorsCsv.split(",").filter { it.isNotBlank() }.associate {
            val parts = it.split(":")
            parts[0] to parts.getOrElse(1) { "0" }.toLong()
        }
        val unlockedSet = p.unlockedCarsCsv.split(",").filter { it.isNotBlank() }.toSet()

        val updatedCars = CarModel.ALL_CARS.map { baseCar ->
            val (eng, hnd, nit) = upgradesMap[baseCar.id] ?: Triple(1, 1, 1)
            val color = colorsMap[baseCar.id] ?: baseCar.selectedColorHex
            val isUnlocked = baseCar.priceCredits == 0 || unlockedSet.contains(baseCar.id) || baseCar.isUnlocked
            baseCar.copy(
                engineLevel = eng,
                handlingLevel = hnd,
                nitroLevel = nit,
                selectedColorHex = color,
                isUnlocked = isUnlocked
            )
        }
        _allCars.value = updatedCars
        _selectedCar.value = updatedCars.find { it.id == p.selectedCarId } ?: updatedCars.first()
    }

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value == AppScreen.RACING && screen != AppScreen.RACING) {
            stopRace()
        }
        _currentScreen.value = screen
    }

    fun selectCar(car: CarModel) {
        _selectedCar.value = car
        viewModelScope.launch {
            repository.selectCar(car.id)
        }
    }

    fun unlockCar(car: CarModel) {
        viewModelScope.launch {
            val success = repository.unlockCar(car.id, car.priceCredits)
            if (success) {
                if (hapticsEnabled) vibrate(120)
                selectCar(car.copy(isUnlocked = true))
            }
        }
    }

    fun upgradeCar(car: CarModel, type: String, price: Int) {
        viewModelScope.launch {
            val success = repository.upgradeCar(car.id, type, price)
            if (success && hapticsEnabled) {
                vibrate(80)
            }
        }
    }

    fun setCarColor(car: CarModel, colorHex: Long) {
        viewModelScope.launch {
            repository.setCarColor(car.id, colorHex)
        }
    }

    // Controls and Assists configuration
    fun setSteeringControlMode(mode: SteeringControlMode) {
        _steeringControlMode.value = mode
    }

    fun toggleManualTransmission(enabled: Boolean) {
        _isManualTransmission.value = enabled
        playerPhysics?.isManualTransmission = enabled
    }

    fun setDrivingAssistMode(mode: DrivingAssistMode) {
        _assistMode.value = mode
        playerPhysics?.assistMode = mode
    }

    fun shiftUp() {
        playerPhysics?.shiftUp()
        if (hapticsEnabled) vibrate(40)
    }

    fun shiftDown() {
        playerPhysics?.shiftDown()
        if (hapticsEnabled) vibrate(40)
    }

    // Weather management functions
    fun setWeather(weather: WeatherCondition) {
        _currentWeather.value = weather
        playerPhysics?.weatherCondition = weather
        postWeatherNotification("Track Weather: ${weather.label} (${(weather.frictionMultiplier * 100).toInt()}% Friction)")
    }

    fun toggleDynamicWeather(enabled: Boolean) {
        isDynamicWeather = enabled
        if (enabled) {
            postWeatherNotification("Dynamic Random Weather Enabled")
        } else {
            postWeatherNotification("Weather Locked to ${_currentWeather.value.label}")
        }
    }

    private fun postWeatherNotification(message: String) {
        weatherNotificationJob?.cancel()
        weatherNotificationJob = viewModelScope.launch {
            _weatherNotification.value = message
            delay(3500)
            _weatherNotification.value = null
        }
    }

    // Starting Position & Calibration controls
    fun updateStartingPosition(x: Float, y: Float, z: Float, heading: Float, heightOffset: Float) {
        _startingConfig.update {
            it.copy(
                customX = x,
                customY = y,
                customZ = z,
                headingDeg = heading,
                carHeightOffset = heightOffset
            )
        }
        playerPhysics?.let { p ->
            p.userHeightOffset = heightOffset
            p.resetToPosition(x, y, z, heading)
        }
    }

    fun selectStartingPreset(preset: StartingPreset) {
        _startingConfig.update {
            it.copy(
                selectedPresetId = preset.id,
                customX = preset.x,
                customY = preset.y,
                customZ = preset.z,
                headingDeg = preset.headingDeg,
                carHeightOffset = preset.heightOffset
            )
        }
        playerPhysics?.let { p ->
            p.userHeightOffset = preset.heightOffset
            p.resetToPosition(preset.x, preset.y, preset.z, preset.headingDeg)
        }
    }

    fun toggleVisualInspection(enabled: Boolean) {
        _startingConfig.update { it.copy(isVisualInspectionEnabled = enabled) }
    }

    fun startCareerRace(cup: CareerCup) {
        selectedCup = cup
        currentGameMode = GameMode.CAREER
        currentLaps = cup.laps
        currentDifficulty = cup.difficulty
        currentTimeOfDay = cup.timeOfDay
        setWeather(cup.weather)
        startRace()
    }

    fun startQuickRace(laps: Int, difficulty: AiDifficulty, timeOfDay: TimeOfDay, weather: WeatherCondition? = null) {
        currentGameMode = GameMode.QUICK_RACE
        currentLaps = laps
        currentDifficulty = difficulty
        currentTimeOfDay = timeOfDay
        if (weather != null) {
            setWeather(weather)
        }
        startRace()
    }

    fun startTimeAttack() {
        currentGameMode = GameMode.TIME_ATTACK
        currentLaps = 3
        currentDifficulty = AiDifficulty.EASY
        currentTimeOfDay = TimeOfDay.SUNNY
        startRace()
    }

    fun startFreeDrive() {
        currentGameMode = GameMode.FREE_DRIVE
        currentLaps = 99
        currentDifficulty = AiDifficulty.EASY
        currentTimeOfDay = TimeOfDay.SUNSET
        startRace()
    }

    private fun startRace() {
        val car = selectedCar.value
        val config = _startingConfig.value

        roadCollision.load(getApplication())

        val spawnX = config.customX
        val spawnZ = config.customZ
        val spawnHeading = config.headingDeg

        val alignment = roadCollision.alignCarOnRoad(
            posX = spawnX,
            posZ = spawnZ,
            headingDeg = spawnHeading,
            userHeightOffset = config.carHeightOffset
        )
        val spawnY = alignment.carY

        val physics = CarPhysics(
            carModel = car,
            posX = spawnX,
            posY = spawnY,
            posZ = spawnZ,
            headingDeg = spawnHeading,
            userHeightOffset = config.carHeightOffset
        ).apply {
            weatherCondition = _currentWeather.value
            assistMode = _assistMode.value
            isManualTransmission = _isManualTransmission.value
        }
        playerPhysics = physics

        if (currentGameMode == GameMode.CAREER || currentGameMode == GameMode.QUICK_RACE) {
            aiManager.initAiRacers(trackData, car.id)
            physics.opponents = aiManager.opponents
        } else {
            aiManager.opponents.clear()
            physics.opponents = emptyList()
        }

        lastCpIdx = trackData.findNearestCheckpointIndex(spawnX, spawnZ)
        currentLapNumber = 1
        bestLapTime = 0L

        _raceState.value = RaceState(
            status = RaceStatus.COUNTDOWN,
            countdownNumber = 3,
            currentLap = 1,
            totalLaps = currentLaps,
            currentPosition = 1,
            totalRacers = if (aiManager.opponents.isNotEmpty()) aiManager.opponents.size + 1 else 1
        )

        audioEngine.start()
        _currentScreen.value = AppScreen.RACING

        launchCountdownAndGameLoop()
    }

    private fun launchCountdownAndGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            for (count in 3 downTo 1) {
                _raceState.update { it.copy(countdownNumber = count) }
                audioEngine.triggerCountdownBeep = count
                if (hapticsEnabled) vibrate(60)
                delay(1000)
            }
            _raceState.update { it.copy(countdownNumber = 0, status = RaceStatus.RACING) }
            audioEngine.triggerCountdownBeep = 0
            if (hapticsEnabled) vibrate(120)
            delay(800)

            totalRaceStartTime = System.currentTimeMillis()
            lapStartTime = totalRaceStartTime

            while (isActive && _raceState.value.status != RaceStatus.FINISHED) {
                if (_raceState.value.status == RaceStatus.RACING) {
                    checkRaceProgress()
                }
                delay(40)
            }
        }
    }

    private fun checkRaceProgress() {
        val player = playerPhysics ?: return
        val nearestCp = trackData.findNearestCheckpointIndex(player.posX, player.posZ, lastCpIdx)
        val n = trackData.checkpoints.size

        if (aiManager.opponents.isNotEmpty()) {
            aiManager.updateAll(
                dt = 0.04f,
                trackData = trackData,
                difficulty = currentDifficulty,
                playerPos = Triple(player.posX, player.posY, player.posZ)
            )
        }

        if (lastCpIdx > n - 15 && nearestCp < 15) {
            val now = System.currentTimeMillis()
            val lapTime = now - lapStartTime
            lapStartTime = now

            if (bestLapTime == 0L || lapTime < bestLapTime) {
                bestLapTime = lapTime
            }

            if (currentLapNumber >= currentLaps && currentGameMode != GameMode.FREE_DRIVE) {
                finishRace()
                return
            } else {
                currentLapNumber++
                if (hapticsEnabled) vibrate(150)
            }
        }
        lastCpIdx = nearestCp
    }

    fun onTelemetryFromRenderer(
        speedKmh: Float,
        rpm: Float,
        gear: Int,
        nitro: Float,
        isNitro: Boolean,
        isDrift: Boolean,
        driftScore: Int,
        driftAngle: Float,
        isOffTrack: Boolean
    ) {
        val player = playerPhysics ?: return
        val nowMs = System.currentTimeMillis()
        val currentLapTime = if (lapStartTime > 0) nowMs - lapStartTime else 0L
        val totalTime = if (totalRaceStartTime > 0) nowMs - totalRaceStartTime else 0L
        val n = trackData.checkpoints.size
        val nearestCp = lastCpIdx

        val playerInfo = RaceRacerInfo(
            id = "player",
            name = "You",
            carName = selectedCar.value.name,
            colorHex = selectedCar.value.selectedColorHex,
            isPlayer = true,
            currentPosition = 1,
            currentLap = currentLapNumber,
            lapProgress = (nearestCp.toFloat() / n.toFloat()).coerceIn(0f, 1f),
            totalDistanceMeters = (currentLapNumber - 1) * trackData.circuitLengthMeters + (nearestCp.toFloat() / n.toFloat()) * trackData.circuitLengthMeters,
            currentSpeedKmh = speedKmh,
            posX = player.posX,
            posY = player.posY,
            posZ = player.posZ,
            headingDeg = player.headingDeg
        )

        val racerList = if (aiManager.opponents.isNotEmpty()) {
            aiManager.getRacerInfos(playerInfo)
        } else {
            listOf(playerInfo)
        }
        val myRank = racerList.firstOrNull { it.isPlayer }?.currentPosition ?: 1

        _raceState.update {
            it.copy(
                currentLap = currentLapNumber,
                currentPosition = myRank,
                totalRacers = racerList.size,
                currentLapTimeMs = currentLapTime,
                bestLapTimeMs = bestLapTime,
                totalRaceTimeMs = totalTime,
                telemetry = RaceTelemetry(
                    speedKmh = speedKmh,
                    rpm = rpm,
                    gear = gear,
                    nitroAmount = nitro,
                    maxNitro = player.carModel.nitroCapacity,
                    isNitroActive = isNitro,
                    isOverdriveActive = player.isOverdriveActive,
                    isLaunchControlActive = player.isLaunchControlActive,
                    isDrafting = player.isDrafting,
                    isDrifting = isDrift,
                    driftScore = driftScore,
                    driftAngle = driftAngle,
                    isOffTrack = isOffTrack
                ),
                racers = racerList
            )
        }
    }

    private fun finishRace() {
        val totalTime = System.currentTimeMillis() - totalRaceStartTime
        val playerPos = _raceState.value.currentPosition
        val drift = playerPhysics?.driftScore ?: 0

        _raceState.update { it.copy(status = RaceStatus.FINISHED) }
        audioEngine.stop()

        val basePrize = 35000
        val cupPrize = if (currentGameMode == GameMode.CAREER && selectedCup != null) selectedCup!!.rewardCredits else 0
        val driftBonus = (drift / 8).coerceAtMost(25000)
        val totalReward = basePrize + cupPrize + driftBonus

        viewModelScope.launch {
            repository.recordRaceResult(
                gameMode = currentGameMode.name,
                bestLapTimeMs = if (bestLapTime > 0) bestLapTime else totalTime / currentLaps,
                totalTimeMs = totalTime,
                carId = selectedCar.value.id,
                position = playerPos,
                driftScore = drift,
                rewardCredits = totalReward,
                cupId = selectedCup?.id
            )
            repository.addCredits(totalReward)
        }
        _currentScreen.value = AppScreen.RACE_RESULTS
    }

    fun toggleCamera() {
        val nextMode = when (_raceState.value.cameraMode) {
            CameraMode.CHASE_CAM -> CameraMode.HOOD_CAM
            CameraMode.HOOD_CAM -> CameraMode.ORBIT_CAM
            CameraMode.ORBIT_CAM -> CameraMode.CHASE_CAM
        }
        _raceState.update { it.copy(cameraMode = nextMode) }
    }

    fun resetCar() {
        val config = _startingConfig.value
        val alignment = roadCollision.alignCarOnRoad(
            posX = config.customX,
            posZ = config.customZ,
            headingDeg = config.headingDeg,
            userHeightOffset = config.carHeightOffset
        )
        playerPhysics?.resetToPosition(
            x = config.customX,
            y = alignment.carY,
            z = config.customZ,
            heading = config.headingDeg
        )
        if (hapticsEnabled) vibrate(100)
    }

    fun pauseRace() {
        if (_raceState.value.status == RaceStatus.RACING) {
            _raceState.update { it.copy(status = RaceStatus.PAUSED) }
        }
    }

    fun resumeRace() {
        if (_raceState.value.status == RaceStatus.PAUSED) {
            _raceState.update { it.copy(status = RaceStatus.RACING) }
        }
    }

    fun restartRace() {
        stopRace()
        startRace()
    }

    fun stopRace() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        audioEngine.stop()
        inputThrottle = 0f
        inputBrake = 0f
        inputSteer = 0f
        inputHandbrake = false
        inputNitro = false
        inputOverdrive = false
    }

    private fun vibrate(durationMs: Long) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(durationMs)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager?.unregisterListener(sensorListener)
        stopRace()
    }
}

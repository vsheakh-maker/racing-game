package com.example.ui.screens

import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.RaceStatus
import com.example.data.model.WeatherCondition
import com.example.engine.DrivingAssistMode
import com.example.engine.GlenCanyonRenderer
import com.example.ui.components.RaceHudOverlay
import com.example.ui.components.RoadInspectionDialog
import com.example.ui.components.SpeedometerGauge
import com.example.ui.components.SteeringControlMode
import com.example.ui.components.TouchControls
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.RaceViewModel

@Composable
fun RaceScreen(
    viewModel: RaceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val raceState by viewModel.raceState.collectAsState()
    val startingConfig by viewModel.startingConfig.collectAsState()
    val currentWeather by viewModel.currentWeather.collectAsState()
    val weatherNotification by viewModel.weatherNotification.collectAsState()
    val steeringMode by viewModel.steeringControlMode.collectAsState()
    val isManualTransmission by viewModel.isManualTransmission.collectAsState()
    val assistMode by viewModel.assistMode.collectAsState()
    var showRoadInspectionDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (raceState.status == RaceStatus.RACING) {
            viewModel.pauseRace()
        } else {
            viewModel.navigateTo(AppScreen.MAIN_MENU)
        }
    }

    // Retain GLSurfaceView and Renderer
    val renderer = remember {
        GlenCanyonRenderer(context, viewModel.trackData).apply {
            playerPhysics = viewModel.playerPhysics
            roadCollision = viewModel.roadCollision
            audioEngine = viewModel.audioEngine
            timeOfDay = viewModel.currentTimeOfDay
            weatherCondition = currentWeather
            isDynamicWeather = viewModel.isDynamicWeather
            isVisualInspectionEnabled = startingConfig.isVisualInspectionEnabled
            raceStatus = raceState.status
            onTelemetryTick = { spd, rpm, gr, nit, isN, isD, ds, da, off ->
                viewModel.onTelemetryFromRenderer(spd, rpm, gr, nit, isN, isD, ds, da, off)
            }
            onWeatherChanged = { newWeather ->
                viewModel.setWeather(newWeather)
            }
        }
    }

    // Keep renderer synced
    LaunchedEffect(viewModel.playerPhysics) {
        renderer.playerPhysics = viewModel.playerPhysics
    }
    LaunchedEffect(viewModel.aiManager.opponents.size, raceState.status) {
        renderer.aiRacers = viewModel.aiManager.opponents
    }
    LaunchedEffect(raceState.status) {
        renderer.raceStatus = raceState.status
    }
    LaunchedEffect(raceState.cameraMode) {
        renderer.cameraMode = raceState.cameraMode
    }
    LaunchedEffect(currentWeather) {
        renderer.weatherCondition = currentWeather
    }
    LaunchedEffect(viewModel.isDynamicWeather) {
        renderer.isDynamicWeather = viewModel.isDynamicWeather
    }
    LaunchedEffect(startingConfig.isVisualInspectionEnabled) {
        renderer.isVisualInspectionEnabled = startingConfig.isVisualInspectionEnabled
    }

    DisposableEffect(Unit) {
        onDispose {
            renderer.release()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        // 1. OpenGL ES 3D Surface View
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(2)
                    setPreserveEGLContextOnPause(true)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }
            },
            onRelease = { glSurfaceView ->
                glSurfaceView.onPause()
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Race HUD Overlay (Position, Lap, Times, Radar Mini-map, Weather Badge)
        RaceHudOverlay(
            raceState = raceState,
            trackData = viewModel.trackData,
            currentWeather = currentWeather,
            weatherNotification = weatherNotification,
            isDynamicWeather = viewModel.isDynamicWeather,
            onWeatherSelect = { selectedWeather ->
                viewModel.setWeather(selectedWeather)
            },
            onToggleDynamicWeather = { dynamic ->
                viewModel.toggleDynamicWeather(dynamic)
            },
            onPauseClick = { viewModel.pauseRace() },
            onCameraClick = { viewModel.toggleCamera() },
            onResetClick = { viewModel.resetCar() },
            modifier = Modifier.fillMaxSize()
        )

        // 3. Speedometer & Tachometer Gauge (Positioned at top-left side under lap counter, leaving center completely clear)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 65.dp)
        ) {
            SpeedometerGauge(
                telemetry = raceState.telemetry,
                useMph = viewModel.useMph
            )
        }

        // 4. Touch Controls (Synchronized directly with renderer physics inputs)
        TouchControls(
            controlMode = steeringMode,
            onControlModeChange = { viewModel.setSteeringControlMode(it) },
            onSteerChanged = {
                viewModel.inputSteer = it
                renderer.steerInput = it
            },
            onThrottleChanged = {
                viewModel.inputThrottle = it
                renderer.throttleInput = it
            },
            onBrakeChanged = {
                viewModel.inputBrake = it
                renderer.brakeInput = it
            },
            onHandbrakeChanged = {
                viewModel.inputHandbrake = it
                renderer.handbrakeInput = it
            },
            onNitroChanged = {
                viewModel.inputNitro = it
                renderer.nitroInput = it
            },
            onOverdriveChanged = {
                viewModel.inputOverdrive = it
                renderer.overdriveInput = it
            },
            isNitroActive = raceState.telemetry.isNitroActive,
            isOverdriveActive = viewModel.playerPhysics?.isOverdriveActive ?: false,
            isLaunchControlActive = viewModel.playerPhysics?.isLaunchControlActive ?: false,
            isManualTransmission = isManualTransmission,
            onShiftUp = { viewModel.shiftUp() },
            onShiftDown = { viewModel.shiftDown() },
            modifier = Modifier.fillMaxSize()
        )

        // 5. Countdown Overlay (3, 2, 1, GO!)
        if (raceState.status == RaceStatus.COUNTDOWN) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x44000000)),
                contentAlignment = Alignment.Center
            ) {
                val text = if (raceState.countdownNumber > 0) "${raceState.countdownNumber}" else "GO!"
                val color = if (raceState.countdownNumber > 0) SpeedYellow else SpeedGreen
                Text(
                    text = text,
                    color = color,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp
                )
            }
        }

        // 6. Pause Dialog (with In-Game Weather & Racing Controls Customization)
        if (raceState.status == RaceStatus.PAUSED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(460.dp)
                        .testTag("pause_dialog"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "RACE PAUSED",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )

                        // In-Game Weather Selector
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CarbonDark, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "TRACK WEATHER & FRICTION",
                                color = NitroCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                WeatherCondition.values().forEach { cond ->
                                    val isSelected = currentWeather == cond
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) RacingOrange else CarbonCard)
                                            .border(1.dp, if (isSelected) Color.White else CarbonBorder, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setWeather(cond) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (cond) {
                                                WeatherCondition.SUNNY -> "☀️ SUNNY"
                                                WeatherCondition.OVERCAST -> "☁️ CLOUDS"
                                                WeatherCondition.RAINY -> "🌧️ RAIN"
                                            },
                                            color = TextWhite,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Driving Assists Selector
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CarbonDark, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "DRIVING ASSISTS (ESP & TRACTION)",
                                color = NitroCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DrivingAssistMode.values().forEach { mode ->
                                    val isSelected = assistMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) SpeedGreen else CarbonCard)
                                            .border(1.dp, if (isSelected) Color.White else CarbonBorder, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setDrivingAssistMode(mode) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.label,
                                            color = if (isSelected) Color.Black else TextWhite,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Transmission Mode (Automatic vs Manual Shifter)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CarbonDark, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TRANSMISSION", color = NitroCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(if (isManualTransmission) "Manual with Paddle Shifters" else "Automatic Shifter", color = TextMuted, fontSize = 9.sp)
                            }
                            Switch(
                                checked = isManualTransmission,
                                onCheckedChange = { viewModel.toggleManualTransmission(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = SpeedGreen)
                            )
                        }

                        Button(
                            onClick = { viewModel.resumeRace() },
                            colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("resume_button")
                        ) {
                            Text("RESUME", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.restartRace() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("restart_button")
                        ) {
                            Text("RESTART RACE", color = TextWhite)
                        }
                        OutlinedButton(
                            onClick = { showRoadInspectionDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("road_calibration_pause_button")
                        ) {
                            Text("ROAD & SPAWN CALIBRATION", color = NitroCyan)
                        }
                        TextButton(
                            onClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) },
                            modifier = Modifier.testTag("quit_button")
                        ) {
                            Text("QUIT TO MENU", color = RacingRed)
                        }
                    }
                }
            }
        }

        // 7. Road Inspection & Calibration Dialog Overlay
        if (showRoadInspectionDialog) {
            RoadInspectionDialog(
                viewModel = viewModel,
                startingConfig = startingConfig,
                onDismiss = { showRoadInspectionDialog = false }
            )
        }
    }
}

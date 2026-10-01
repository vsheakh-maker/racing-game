package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CameraMode
import com.example.data.model.RaceRacerInfo
import com.example.data.model.RaceState
import com.example.data.model.RaceStatus
import com.example.data.model.WeatherCondition
import com.example.engine.TrackData
import com.example.ui.theme.*

@Composable
fun RaceHudOverlay(
    raceState: RaceState,
    trackData: TrackData,
    currentWeather: WeatherCondition = WeatherCondition.SUNNY,
    weatherNotification: String? = null,
    isDynamicWeather: Boolean = false,
    onWeatherSelect: (WeatherCondition) -> Unit = {},
    onToggleDynamicWeather: (Boolean) -> Unit = {},
    onPauseClick: () -> Unit,
    onCameraClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWeatherPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // TOP HUD BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Lap & Weather Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Lap Counter
                Box(
                    modifier = Modifier
                        .testTag("lap_counter")
                        .background(Color(0xCC0D1117), RoundedCornerShape(12.dp))
                        .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LAP",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${raceState.currentLap} / ${raceState.totalLaps}",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Weather Condition Badge (Click to open quick weather chooser)
                Box(
                    modifier = Modifier
                        .testTag("weather_badge")
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC0D1117))
                        .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp))
                        .clickable { showWeatherPicker = !showWeatherPicker }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val icon = when (currentWeather) {
                            WeatherCondition.SUNNY -> "☀️ SUNNY"
                            WeatherCondition.OVERCAST -> "☁️ OVERCAST"
                            WeatherCondition.RAINY -> "🌧️ RAIN"
                        }
                        Text(text = icon, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(
                            text = "${(currentWeather.frictionMultiplier * 100).toInt()}% GRIP" + if (isDynamicWeather) " • AUTO" else "",
                            color = if (currentWeather.isRainActive) SpeedYellow else SpeedGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Center: Prominent Race Duration Timer Component
            RaceDurationTimer(
                raceState = raceState,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Right: Real-time Circuit Mini-Map HUD & Control buttons
            Row(verticalAlignment = Alignment.Top) {
                // Glen Canyon Circuit Real-time Layout Mini-Map
                CircuitMiniMap(
                    trackData = trackData,
                    racers = raceState.racers
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Action buttons column
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Pause Button
                    IconButton(
                        onClick = onPauseClick,
                        modifier = Modifier
                            .testTag("pause_button")
                            .size(44.dp)
                            .background(Color(0xCC0D1117), CircleShape)
                            .border(1.dp, CarbonBorder, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", tint = TextWhite)
                    }

                    // Camera Switcher
                    IconButton(
                        onClick = onCameraClick,
                        modifier = Modifier
                            .testTag("camera_switch_button")
                            .size(44.dp)
                            .background(Color(0xCC0D1117), CircleShape)
                            .border(1.dp, CarbonBorder, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = "Camera", tint = TextWhite)
                    }

                    // Car Reset
                    IconButton(
                        onClick = onResetClick,
                        modifier = Modifier
                            .testTag("reset_car_button")
                            .size(44.dp)
                            .background(Color(0xCC0D1117), CircleShape)
                            .border(1.dp, CarbonBorder, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Car", tint = TextWhite)
                    }
                }
            }
        }

        // QUICK WEATHER SELECTOR DIALOG DROPDOWN
        if (showWeatherPicker) {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp, top = 65.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xEE161B22))
                    .border(1.dp, CarbonBorder, RoundedCornerShape(14.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SELECT WEATHER",
                        color = NitroCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )

                    WeatherCondition.values().forEach { cond ->
                        val isSelected = currentWeather == cond
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) RacingOrange else CarbonSurface)
                                .border(1.dp, if (isSelected) Color.White else CarbonBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    onWeatherSelect(cond)
                                    showWeatherPicker = false
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when (cond) {
                                    WeatherCondition.SUNNY -> "☀️ Sunny (100% Grip)"
                                    WeatherCondition.OVERCAST -> "☁️ Overcast (88% Grip)"
                                    WeatherCondition.RAINY -> "🌧️ Rainy (68% Grip)"
                                },
                                color = TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDynamicWeather) SpeedGreen else CarbonSurface)
                            .clickable {
                                onToggleDynamicWeather(!isDynamicWeather)
                                showWeatherPicker = false
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isDynamicWeather) "🔄 Dynamic Random: ON" else "🔄 Dynamic Random: OFF",
                            color = if (isDynamicWeather) Color.Black else TextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // WEATHER NOTIFICATION ALERT BANNER
        AnimatedVisibility(
            visible = weatherNotification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            weatherNotification?.let { msg ->
                Box(
                    modifier = Modifier
                        .background(Color(0xEE0D1117), RoundedCornerShape(20.dp))
                        .border(1.5.dp, RacingOrange, RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = msg,
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Countdown Overlay
        if (raceState.status == RaceStatus.COUNTDOWN && raceState.countdownNumber in 1..3) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                Text(
                    text = "${raceState.countdownNumber}",
                    color = when (raceState.countdownNumber) {
                        3 -> RacingRed
                        2 -> SpeedYellow
                        1 -> SpeedGreen
                        else -> TextWhite
                    },
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Drift Score Badge (Docked at side under speedometer)
        if (raceState.telemetry.isDrifting && raceState.telemetry.driftScore > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 145.dp)
                    .background(Color(0xCCFF6F00), RoundedCornerShape(16.dp))
                    .border(2.dp, SpeedYellow, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "DRIFT +${raceState.telemetry.driftScore}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${raceState.telemetry.driftAngle.toInt()}° ANGLE",
                        color = SpeedYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Slipstream Drafting Tow Indicator (Positioned at top under timer bar)
        if (raceState.telemetry.isDrafting && raceState.status == RaceStatus.RACING) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .background(Color(0xDD0D1117), RoundedCornerShape(16.dp))
                    .border(2.dp, NitroCyan, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = NitroCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⚡ SLIPSTREAM TOW ACTIVE • DRAFTING BOOST",
                        color = NitroCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Hyper Overdrive Warp Banner (Positioned at top under timer bar)
        if (raceState.telemetry.isOverdriveActive && raceState.status == RaceStatus.RACING) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .background(Color(0xDD311B92), RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFFE040FB), RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFE040FB), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🚀 HYPER OVERDRIVE • WARP SPEED BOOST",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Off-Track Guardrail Warning (Positioned at top under timer bar)
        if (raceState.telemetry.isOffTrack && raceState.status == RaceStatus.RACING) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .background(Color(0xDDCC0000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ROAD CORRIDOR BARRIER ACTIVE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun MiniMapRadar(
    trackData: TrackData,
    racers: List<RaceRacerInfo>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xCC0D1117), RoundedCornerShape(12.dp))
            .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp))
            .padding(6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Bounds of Glen Canyon circuit
            val minX = -480f
            val maxX = 600f
            val minZ = -340f
            val maxZ = 60f

            fun mapCoord(x: Float, z: Float): Offset {
                val nx = (x - minX) / (maxX - minX)
                val nz = (z - minZ) / (maxZ - minZ)
                return Offset(nx * w, (1f - nz) * h)
            }

            // Draw track line
            if (trackData.checkpoints.isNotEmpty()) {
                val path = Path()
                val first = mapCoord(trackData.checkpoints[0].x, trackData.checkpoints[0].z)
                path.moveTo(first.x, first.y)
                for (i in 1 until trackData.checkpoints.size) {
                    val pt = mapCoord(trackData.checkpoints[i].x, trackData.checkpoints[i].z)
                    path.lineTo(pt.x, pt.y)
                }
                path.close()

                drawPath(
                    path = path,
                    color = Color(0xFF4A5568),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Draw Player 1965 Mustang (cyan glowing dot)
            racers.find { it.isPlayer }?.let { player ->
                val pt = mapCoord(player.posX, player.posZ)
                drawCircle(color = NitroCyanGlow, radius = 5.dp.toPx(), center = pt)
                drawCircle(color = NitroCyan, radius = 3.dp.toPx(), center = pt)
            }
        }
    }
}

fun formatLapTime(ms: Long): String {
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val millis = (ms % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, millis)
}

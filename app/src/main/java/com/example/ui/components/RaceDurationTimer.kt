package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RaceState
import com.example.data.model.RaceStatus
import com.example.ui.theme.*

/**
 * Prominent Race Duration Timer Component for Glen Canyon Racing.
 * Displays high-precision race duration, lap timing, and session bests
 * with realistic motorsport stopwatch aesthetics.
 */
@Composable
fun RaceDurationTimer(
    raceState: RaceState,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Pulsing live indicator for active racing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val isRacing = raceState.status == RaceStatus.RACING
    val isFinished = raceState.status == RaceStatus.FINISHED
    val isPaused = raceState.status == RaceStatus.PAUSED

    val statusDotColor = when {
        isFinished -> SpeedYellow
        isPaused -> RacingOrange
        isRacing -> SpeedGreen
        else -> TextMuted
    }

    // High precision milliseconds formatting
    fun formatDetailedTime(ms: Long): Triple<String, String, String> {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = ms % 1000

        val minStr = minutes.toString().padStart(2, '0')
        val secStr = seconds.toString().padStart(2, '0')
        val milStr = (millis / 10).toString().padStart(2, '0') // Centiseconds display

        return Triple(minStr, secStr, milStr)
    }

    val (raceMin, raceSec, raceMil) = formatDetailedTime(raceState.totalRaceTimeMs)
    val (lapMin, lapSec, lapMil) = formatDetailedTime(raceState.currentLapTimeMs)

    Box(
        modifier = modifier
            .testTag("race_duration_timer")
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF0161B22),
                        Color(0xF50D1117)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        NitroCyan.copy(alpha = 0.8f),
                        RacingOrange.copy(alpha = 0.5f),
                        NitroCyan.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { isExpanded = !isExpanded }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top Label Bar: Status Dot + "TOTAL RACE DURATION"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Blinking Live Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusDotColor.copy(alpha = if (isRacing) pulseAlpha else 1.0f))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        isFinished -> "RACE COMPLETE"
                        isPaused -> "PAUSED"
                        isRacing -> "RACE DURATION"
                        else -> "STARTING"
                    },
                    color = if (isRacing) NitroCyan else TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Primary Prominent Race Duration (MM:SS . ms)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Stopwatch",
                    tint = TextWhite.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(18.dp)
                        .padding(bottom = 2.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Main Minutes : Seconds
                Text(
                    text = "$raceMin:$raceSec",
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                // High-speed Milliseconds / Centiseconds
                Text(
                    text = ".$raceMil",
                    color = NitroCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }

            // Secondary Sub-Timers (Lap Time + Best Lap)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                // Current Lap
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LAP: ",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "$lapMin:$lapSec.$lapMil",
                        color = SpeedGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Best Lap (if recorded)
                if (raceState.bestLapTimeMs > 0L) {
                    val (bMin, bSec, bMil) = formatDetailedTime(raceState.bestLapTimeMs)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Best",
                            tint = GoldTrophy,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "$bMin:$bSec.$bMil",
                            color = GoldTrophy,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Expandable Sector Time Breakdown
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CarbonBorder)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SECTOR 1: ${formatSectorTime(raceState.sector1TimeMs)} • SECTOR 2: ${formatSectorTime(raceState.sector2TimeMs)}",
                        color = TextMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun formatSectorTime(ms: Long): String {
    if (ms <= 0L) return "--:--"
    val sec = ms / 1000
    val rem = (ms % 1000) / 10
    return "${sec}.${rem.toString().padStart(2, '0')}s"
}

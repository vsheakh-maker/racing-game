package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RaceRacerInfo
import com.example.engine.TrackData
import com.example.ui.theme.*
import kotlin.math.*

/**
 * Real-time Circuit Mini-Map HUD Component for Glen Canyon Racing.
 * Accurately displays the Glen Canyon closed-loop circuit layout,
 * player position & directional heading chevron, and live rival car icons.
 */
@Composable
fun CircuitMiniMap(
    trackData: TrackData,
    racers: List<RaceRacerInfo>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Pulsing radar animation for player location awareness
    val infiniteTransition = rememberInfiniteTransition(label = "minimap_pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Compute coordinate bounding box with safe padding
    val bounds = remember(trackData) {
        if (trackData.checkpoints.isEmpty()) {
            Triple(-470f..590f, -330f..70f, 1060f to 400f)
        } else {
            var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
            var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
            for (cp in trackData.checkpoints) {
                if (cp.x < minX) minX = cp.x
                if (cp.x > maxX) maxX = cp.x
                if (cp.z < minZ) minZ = cp.z
                if (cp.z > maxZ) maxZ = cp.z
            }
            val padX = (maxX - minX) * 0.08f
            val padZ = (maxZ - minZ) * 0.08f
            Triple(
                (minX - padX)..(maxX + padX),
                (minZ - padZ)..(maxZ + padZ),
                (maxX - minX + padX * 2) to (maxZ - minZ + padZ * 2)
            )
        }
    }

    val (rangeX, rangeZ, span) = bounds
    val widthSpan = span.first
    val heightSpan = span.second

    val mapWidth = if (isExpanded) 220.dp else 155.dp
    val mapHeight = if (isExpanded) 125.dp else 88.dp

    Box(
        modifier = modifier
            .testTag("circuit_mini_map")
            .size(mapWidth, mapHeight)
            .shadow(12.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF00D1117),
                        Color(0xF8161B22)
                    )
                )
            )
            .border(1.5.dp, CarbonBorder, RoundedCornerShape(14.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(6.dp)
    ) {
        // Track Header & Legend (Top edge of mini-map)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(SpeedGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "GLEN CANYON",
                    color = TextWhite,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "3.2 KM",
                color = NitroCyan,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Radar Canvas (Circuit + Player + Rivals)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // Mapping function from 3D world coordinates (x, z) to 2D mini-map pixels
            fun mapPoint(worldX: Float, worldZ: Float): Offset {
                val nx = (worldX - rangeX.start) / widthSpan
                val nz = (worldZ - rangeZ.start) / heightSpan
                // In world space +Z is south, -Z is north, flip Y for canvas top-down view
                val px = nx * canvasW
                val py = (1f - nz) * canvasH
                return Offset(px, py)
            }

            val cps = trackData.checkpoints
            if (cps.isNotEmpty()) {
                // 1. Draw Outer Track Road Ribbon Glow
                val trackPath = Path()
                val startPt = mapPoint(cps[0].x, cps[0].z)
                trackPath.moveTo(startPt.x, startPt.y)
                for (i in 1 until cps.size) {
                    val p = mapPoint(cps[i].x, cps[i].z)
                    trackPath.lineTo(p.x, p.y)
                }
                trackPath.close()

                // Outer boundary glow
                drawPath(
                    path = trackPath,
                    color = Color(0x3300E5FF),
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Solid Road Surface Corridor
                drawPath(
                    path = trackPath,
                    color = Color(0xFF38444D),
                    style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Track Centerline
                drawPath(
                    path = trackPath,
                    color = Color(0xFF8B949E),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // 2. Start / Finish Line Checkered Marker
                val finishPt = mapPoint(cps[0].x, cps[0].z)
                drawCircle(color = GoldTrophy, radius = 3.dp.toPx(), center = finishPt)

                // Sector 1 and Sector 2 Split Gates
                if (trackData.sector1Index in cps.indices) {
                    val s1Pt = mapPoint(cps[trackData.sector1Index].x, cps[trackData.sector1Index].z)
                    drawCircle(color = SpeedYellow, radius = 2.dp.toPx(), center = s1Pt)
                }
                if (trackData.sector2Index in cps.indices) {
                    val s2Pt = mapPoint(cps[trackData.sector2Index].x, cps[trackData.sector2Index].z)
                    drawCircle(color = SpeedYellow, radius = 2.dp.toPx(), center = s2Pt)
                }
            }

            // 3. Render Rival Car Icons (AI Opponents)
            val rivals = racers.filter { !it.isPlayer }
            for (rival in rivals) {
                val rivalPt = mapPoint(rival.posX, rival.posZ)
                val rivalColor = Color(rival.colorHex)

                // Outer dark contrast ring
                drawCircle(
                    color = Color(0xFF000000),
                    radius = 4.5.dp.toPx(),
                    center = rivalPt
                )
                // Vivid rival color dot
                drawCircle(
                    color = rivalColor,
                    radius = 3.2.dp.toPx(),
                    center = rivalPt
                )
            }

            // 4. Render Player Position & Directional Heading Chevron
            val player = racers.find { it.isPlayer }
            if (player != null) {
                val playerPt = mapPoint(player.posX, player.posZ)

                // Live Pulsing Halo
                drawCircle(
                    color = SpeedGreen.copy(alpha = pulseAlpha),
                    radius = pulseRadius.dp.toPx(),
                    center = playerPt
                )

                // Player Heading Chevron (Pointing along headingDeg)
                // headingDeg in world: 0 deg = North (+Z in screen Y), 90 deg = East (+X in screen X)
                val headingRad = Math.toRadians(player.headingDeg.toDouble())
                val tipLen = 7.dp.toPx()
                val wingLen = 5.dp.toPx()

                val dirX = sin(headingRad).toFloat()
                val dirY = -cos(headingRad).toFloat() // Screen Y is inverted relative to world Z
                val normX = -dirY
                val normY = dirX

                val tip = Offset(playerPt.x + dirX * tipLen, playerPt.y + dirY * tipLen)
                val leftWing = Offset(playerPt.x - dirX * 3f + normX * wingLen, playerPt.y - dirY * 3f + normY * wingLen)
                val rightWing = Offset(playerPt.x - dirX * 3f - normX * wingLen, playerPt.y - dirY * 3f - normY * wingLen)

                val chevronPath = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(leftWing.x, leftWing.y)
                    lineTo(playerPt.x, playerPt.y)
                    lineTo(rightWing.x, rightWing.y)
                    close()
                }

                // Shadow
                drawPath(path = chevronPath, color = Color.Black)
                // Vivid SpeedGreen Chevron
                drawPath(path = chevronPath, color = SpeedGreen)
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = playerPt)
            }
        }
    }
}

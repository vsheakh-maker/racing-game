package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RaceTelemetry
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun SpeedometerGauge(
    telemetry: RaceTelemetry,
    useMph: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displaySpeed = if (useMph) (telemetry.speedKmh * 0.621371f).toInt() else telemetry.speedKmh.toInt()
    val speedUnit = if (useMph) "MPH" else "KM/H"
    val maxSpeed = if (useMph) 220f else 350f

    val rpmRatio = (telemetry.rpm / telemetry.maxRpm).coerceIn(0f, 1f)
    val isRedline = telemetry.rpm > 7600f

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val redlineAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "redlineAlpha"
    )

    Row(
        modifier = modifier
            .testTag("speedometer_gauge")
            .background(Color(0xCC0D1117), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Circular Speedometer Dial
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 10.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Background track arc (from 140 deg to 40 deg, span 260 deg)
                val startAngle = 140f
                val totalSpan = 260f

                drawArc(
                    color = CarbonBorder,
                    startAngle = startAngle,
                    sweepAngle = totalSpan,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Speed fill arc
                val speedRatio = (displaySpeed / maxSpeed).coerceIn(0f, 1f)
                val fillAngle = totalSpan * speedRatio

                val speedBrush = Brush.sweepGradient(
                    listOf(SpeedGreen, SpeedYellow, RacingOrange, RacingRed)
                )

                if (fillAngle > 0f) {
                    drawArc(
                        brush = speedBrush,
                        startAngle = startAngle,
                        sweepAngle = fillAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Needle indicator dot
                val needleAngle = Math.toRadians((startAngle + fillAngle).toDouble())
                val needlePos = Offset(
                    (center.x + radius * cos(needleAngle)).toFloat(),
                    (center.y + radius * sin(needleAngle)).toFloat()
                )
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = needlePos)
            }

            // Digital Speed Readout
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$displaySpeed",
                    color = if (isRedline) RacingRed.copy(alpha = redlineAlpha) else TextWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = speedUnit,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. RPM & Transmission & Nitro Column
        Column(
            modifier = Modifier.width(115.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Gear badge & RPM display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gear
                Box(
                    modifier = Modifier
                        .background(if (isRedline) RacingRed else CarbonCard, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (telemetry.speedKmh < 0.5f && telemetry.gear == 1) "N" else "G${telemetry.gear}",
                        color = TextWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // RPM readout
                Text(
                    text = "${telemetry.rpm.toInt()} RPM",
                    color = if (isRedline) RacingRed else TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // RPM Linear Bar
            Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                val w = size.width
                val h = size.height
                // Track
                drawRoundRect(color = CarbonBorder, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))
                // Fill
                val fillW = w * rpmRatio
                val barColor = when {
                    rpmRatio > 0.88f -> RacingRed
                    rpmRatio > 0.70f -> RacingOrange
                    else -> SpeedGreen
                }
                drawRoundRect(color = barColor, size = Size(fillW, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nitro NOS Tank
            val nitroRatio = (telemetry.nitroAmount / telemetry.maxNitro).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = "Nitro",
                    tint = if (telemetry.isNitroActive) NitroCyan else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Nitro bar
                Canvas(modifier = Modifier.weight(1f).height(6.dp)) {
                    drawRoundRect(color = CarbonBorder, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f))
                    val nw = size.width * nitroRatio
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(NitroCyan, Color.White)),
                        size = Size(nw, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                    )
                }
            }

            // Drift score popup
            if (telemetry.isDrifting || telemetry.driftScore > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Drift",
                        tint = SpeedYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "+${telemetry.driftScore}",
                        color = SpeedYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

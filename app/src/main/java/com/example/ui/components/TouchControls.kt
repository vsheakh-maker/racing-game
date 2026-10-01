package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.atan2

enum class SteeringControlMode(val label: String) {
    BUTTONS("Buttons"),
    STEERING_WHEEL("Virtual Wheel"),
    TILT("Gyro / Tilt")
}

@Composable
fun TouchControls(
    controlMode: SteeringControlMode = SteeringControlMode.BUTTONS,
    onControlModeChange: (SteeringControlMode) -> Unit = {},
    onSteerChanged: (Float) -> Unit,
    onThrottleChanged: (Float) -> Unit,
    onBrakeChanged: (Float) -> Unit,
    onHandbrakeChanged: (Boolean) -> Unit,
    onNitroChanged: (Boolean) -> Unit,
    onOverdriveChanged: (Boolean) -> Unit = {},
    isNitroActive: Boolean,
    isOverdriveActive: Boolean = false,
    isLaunchControlActive: Boolean = false,
    isManualTransmission: Boolean = false,
    onShiftUp: () -> Unit = {},
    onShiftDown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isSteerLeftPressed by remember { mutableStateOf(false) }
    var isSteerRightPressed by remember { mutableStateOf(false) }
    var wheelAngleDeg by remember { mutableFloatStateOf(0f) }
    var isWheelDragging by remember { mutableStateOf(false) }

    var isGasPressed by remember { mutableStateOf(false) }
    var isBrakePressed by remember { mutableStateOf(false) }
    var isHandbrakePressed by remember { mutableStateOf(false) }
    var isNitroPressed by remember { mutableStateOf(false) }
    var isOverdrivePressed by remember { mutableStateOf(false) }

    // Spring back wheel when finger is lifted
    val animatedWheelAngle by animateFloatAsState(
        targetValue = if (isWheelDragging) wheelAngleDeg else 0f,
        animationSpec = tween(durationMillis = if (isWheelDragging) 0 else 200),
        label = "wheelAngle"
    )

    LaunchedEffect(controlMode, isSteerLeftPressed, isSteerRightPressed, animatedWheelAngle) {
        when (controlMode) {
            SteeringControlMode.BUTTONS -> {
                val steer = when {
                    isSteerLeftPressed && !isSteerRightPressed -> -1.0f
                    isSteerRightPressed && !isSteerLeftPressed -> 1.0f
                    else -> 0.0f
                }
                onSteerChanged(steer)
            }
            SteeringControlMode.STEERING_WHEEL -> {
                val steerNormalized = (animatedWheelAngle / 75f).coerceIn(-1.0f, 1.0f)
                onSteerChanged(steerNormalized)
            }
            SteeringControlMode.TILT -> {
                // Handled via sensor listener in ViewModel/Screen
            }
        }
    }

    LaunchedEffect(isGasPressed) {
        onThrottleChanged(if (isGasPressed) 1.0f else 0.0f)
    }

    LaunchedEffect(isBrakePressed) {
        onBrakeChanged(if (isBrakePressed) 1.0f else 0.0f)
    }

    LaunchedEffect(isHandbrakePressed) {
        onHandbrakeChanged(isHandbrakePressed)
    }

    LaunchedEffect(isNitroPressed) {
        onNitroChanged(isNitroPressed)
    }

    LaunchedEffect(isOverdrivePressed) {
        onOverdriveChanged(isOverdrivePressed)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 16.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // LAUNCH CONTROL NOTIFICATION (Positioned at top to keep center screen clear)
        if (isLaunchControlActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xDD0D1117))
                    .border(2.dp, GoldTrophy, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = GoldTrophy, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⚡ LAUNCH CONTROL READY • RELEASE BRAKE TO LAUNCH!",
                        color = GoldTrophy,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // LEFT CONTROLS: Steering
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Steering Mode Quick Toggle Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC0D1117))
                        .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            val nextMode = when (controlMode) {
                                SteeringControlMode.BUTTONS -> SteeringControlMode.STEERING_WHEEL
                                SteeringControlMode.STEERING_WHEEL -> SteeringControlMode.TILT
                                SteeringControlMode.TILT -> SteeringControlMode.BUTTONS
                            }
                            onControlModeChange(nextMode)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🎮 Steer: ${controlMode.label}",
                        color = NitroCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                when (controlMode) {
                    SteeringControlMode.BUTTONS -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Steer Left Button
                            Box(
                                modifier = Modifier
                                    .testTag("steer_left_button")
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .background(if (isSteerLeftPressed) RacingOrange else Color(0xCC0D1117))
                                    .border(2.dp, if (isSteerLeftPressed) Color.White else CarbonBorder, CircleShape)
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                isSteerLeftPressed = event.changes.any { it.pressed }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Steer Left",
                                    tint = TextWhite,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            // Steer Right Button
                            Box(
                                modifier = Modifier
                                    .testTag("steer_right_button")
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .background(if (isSteerRightPressed) RacingOrange else Color(0xCC0D1117))
                                    .border(2.dp, if (isSteerRightPressed) Color.White else CarbonBorder, CircleShape)
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                isSteerRightPressed = event.changes.any { it.pressed }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Steer Right",
                                    tint = TextWhite,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    }
                    SteeringControlMode.STEERING_WHEEL -> {
                        // Interactive Rotating Virtual Steering Wheel
                        Box(
                            modifier = Modifier
                                .testTag("virtual_steering_wheel")
                                .size(130.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { isWheelDragging = true },
                                        onDragEnd = { isWheelDragging = false },
                                        onDragCancel = { isWheelDragging = false },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            wheelAngleDeg = (wheelAngleDeg + dragAmount.x * 0.9f).coerceIn(-85f, 85f)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(animatedWheelAngle)
                            ) {
                                val center = Offset(size.width / 2, size.height / 2)
                                val radius = size.width / 2 - 8.dp.toPx()

                                // Outer Rim
                                drawCircle(
                                    color = Color(0xFF21262D),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 14.dp.toPx())
                                )
                                drawCircle(
                                    color = RacingOrange,
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                // Top Center Alignment Stripe
                                drawLine(
                                    color = Color.White,
                                    start = Offset(center.x, center.y - radius - 7.dp.toPx()),
                                    end = Offset(center.x, center.y - radius + 7.dp.toPx()),
                                    strokeWidth = 4.dp.toPx()
                                )
                                // Spokes
                                drawLine(
                                    color = Color(0xFF8B949E),
                                    start = center,
                                    end = Offset(center.x - radius, center.y),
                                    strokeWidth = 6.dp.toPx()
                                )
                                drawLine(
                                    color = Color(0xFF8B949E),
                                    start = center,
                                    end = Offset(center.x + radius, center.y),
                                    strokeWidth = 6.dp.toPx()
                                )
                                drawLine(
                                    color = Color(0xFF8B949E),
                                    start = center,
                                    end = Offset(center.x, center.y + radius),
                                    strokeWidth = 6.dp.toPx()
                                )
                                // Center Hub
                                drawCircle(
                                    color = Color(0xFF161B22),
                                    radius = 20.dp.toPx(),
                                    center = center
                                )
                                drawCircle(
                                    color = NitroCyan,
                                    radius = 12.dp.toPx(),
                                    center = center
                                )
                            }
                        }
                    }
                    SteeringControlMode.TILT -> {
                        Box(
                            modifier = Modifier
                                .size(120.dp, 60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xCC0D1117))
                                .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = NitroCyan, modifier = Modifier.size(24.dp))
                                Text("TILT PHONE TO STEER", color = TextWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // CENTER: Manual Gear Shift Paddles (if Manual Transmission enabled)
            if (isManualTransmission) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("shift_down_paddle")
                            .size(56.dp, 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xDD161B22))
                            .border(1.dp, CarbonBorder, RoundedCornerShape(10.dp))
                            .clickable { onShiftDown() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("[-] DOWN", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                    Box(
                        modifier = Modifier
                            .testTag("shift_up_paddle")
                            .size(56.dp, 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xDD161B22))
                            .border(1.dp, CarbonBorder, RoundedCornerShape(10.dp))
                            .clickable { onShiftUp() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("[+] UP", color = SpeedGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // RIGHT CONTROLS: Drift, Boosters (Nitro & Overdrive), Brake, Gas
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Secondary Column: Handbrake Drift & Stage 2 Overdrive Hyperboost
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Stage 2 OVERDRIVE HYPERBOOST Button
                    Box(
                        modifier = Modifier
                            .testTag("overdrive_boost_button")
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOverdrivePressed || isOverdriveActive) {
                                    Brush.radialGradient(listOf(Color(0xFF9C27B0), Color(0xFF00E5FF)))
                                } else {
                                    Brush.radialGradient(listOf(Color(0xCC311B92), Color(0xCC0D1117)))
                                }
                            )
                            .border(2.dp, if (isOverdriveActive) Color.White else Color(0xFF7C4DFF), CircleShape)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        isOverdrivePressed = event.changes.any { it.pressed }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Overdrive",
                                tint = if (isOverdriveActive) Color.White else Color(0xFFE040FB),
                                modifier = Modifier.size(24.dp)
                            )
                            Text("HYPER", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Handbrake / Drift Button
                    Box(
                        modifier = Modifier
                            .testTag("handbrake_button")
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(if (isHandbrakePressed) RacingOrange else Color(0xCC0D1117))
                            .border(2.dp, if (isHandbrakePressed) Color.White else CarbonBorder, CircleShape)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        isHandbrakePressed = event.changes.any { it.pressed }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "DRIFT",
                                color = if (isHandbrakePressed) Color.White else TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Stage 1 Nitro Button
                Box(
                    modifier = Modifier
                        .testTag("nitro_button")
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (isNitroPressed || isNitroActive) {
                                Brush.radialGradient(listOf(NitroCyan, Color(0xFF0055A5)))
                            } else {
                                Brush.radialGradient(listOf(Color(0xCC0055A5), Color(0xCC0D1117)))
                            }
                        )
                        .border(2.dp, if (isNitroActive) Color.White else NitroCyan, CircleShape)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    isNitroPressed = event.changes.any { it.pressed }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Nitro",
                            tint = if (isNitroActive) Color.White else NitroCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "NITRO",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Foot Brake / Reverse Button
                Box(
                    modifier = Modifier
                        .testTag("brake_button")
                        .size(74.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isBrakePressed) RacingRed else Color(0xCC0D1117))
                        .border(2.dp, if (isBrakePressed) Color.White else CarbonBorder, RoundedCornerShape(18.dp))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    isBrakePressed = event.changes.any { it.pressed }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = TextWhite, modifier = Modifier.size(28.dp))
                        Text(
                            text = "BRAKE",
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Accelerator / Throttle Pedal
                Box(
                    modifier = Modifier
                        .testTag("throttle_pedal")
                        .size(80.dp, 105.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isGasPressed) {
                                Brush.verticalGradient(listOf(SpeedGreen, Color(0xFF007E33)))
                            } else {
                                Brush.verticalGradient(listOf(Color(0xCC161B22), Color(0xCC0D1117)))
                            }
                        )
                        .border(2.5.dp, if (isGasPressed) Color.White else SpeedGreen, RoundedCornerShape(20.dp))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    isGasPressed = event.changes.any { it.pressed }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Gas",
                            tint = if (isGasPressed) Color.White else SpeedGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "GAS",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StartingConfig
import com.example.data.model.StartingPreset
import com.example.ui.theme.*
import com.example.viewmodel.RaceViewModel

@Composable
fun RoadInspectionDialog(
    viewModel: RaceViewModel,
    startingConfig: StartingConfig,
    onDismiss: () -> Unit
) {
    var posX by remember { mutableFloatStateOf(startingConfig.customX) }
    var posZ by remember { mutableFloatStateOf(startingConfig.customZ) }
    var heading by remember { mutableFloatStateOf(startingConfig.headingDeg) }
    var heightOffset by remember { mutableFloatStateOf(startingConfig.carHeightOffset) }
    var visualDebug by remember { mutableStateOf(startingConfig.isVisualInspectionEnabled) }

    val roadCollision = viewModel.roadCollision
    val alignment = remember(posX, posZ, heading, heightOffset) {
        roadCollision.alignCarOnRoad(
            posX = posX,
            posZ = posZ,
            headingDeg = heading,
            userHeightOffset = heightOffset
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(580.dp)
                .fillMaxHeight(0.92f)
                .clickable(enabled = false) {}
                .testTag("road_inspection_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ROAD CALIBRATION & SPAWN INSPECTION",
                            color = TextWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Position the player car on the actual Glen Canyon Dam road surface",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextWhite)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Presets Row
                Column {
                    Text("ROAD LOCATION PRESETS", color = NitroCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StartingPreset.PRESETS) { preset ->
                            val isSelected = startingConfig.selectedPresetId == preset.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) RacingOrange else CarbonCard)
                                    .clickable {
                                        posX = preset.x
                                        posZ = preset.z
                                        heading = preset.headingDeg
                                        heightOffset = preset.heightOffset
                                        viewModel.selectStartingPreset(preset)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(preset.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(preset.description, color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextMuted, fontSize = 9.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Road Surface Feedback Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CarbonDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (alignment.isOnRoad) SpeedGreen else RacingRed)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (alignment.isOnRoad) "● ON ACTUAL ROAD SURFACE" else "▲ OFF-ROAD / FLOATING",
                                color = if (alignment.isOnRoad) SpeedGreen else RacingRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Car Elevation Y: ${String.format("%.2f", alignment.carY)}m",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Road Pitch: ${String.format("%.1f", alignment.pitchDeg)}°", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("Road Roll: ${String.format("%.1f", alignment.rollDeg)}°", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            val contactingCount = alignment.wheelContacts.count { it.isContacting }
                            Text("4-Wheel Ground Contact: $contactingCount / 4", color = if (contactingCount == 4) SpeedGreen else SpeedYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Adjustment Sliders
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Position X
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("X Position: ${posX.toInt()}m", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Slider(
                            value = posX,
                            onValueChange = { posX = it },
                            valueRange = -350f..550f,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                    }

                    // Position Z
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Z Position: ${posZ.toInt()}m", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Slider(
                            value = posZ,
                            onValueChange = { posZ = it },
                            valueRange = -320f..80f,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                    }

                    // Heading Rotation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rotation: ${heading.toInt()}°", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Slider(
                            value = heading,
                            onValueChange = { heading = it },
                            valueRange = 0f..360f,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                    }

                    // Height Offset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Height Offset: ${String.format("%.2f", heightOffset)}m", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Slider(
                            value = heightOffset,
                            onValueChange = { heightOffset = it },
                            valueRange = -0.20f..0.20f,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Visual Debug Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarbonCard)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Visual Road Collision & Contact Debugger", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Draws 3D road collision wireframe & 4 wheel contact points in-game", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = visualDebug,
                        onCheckedChange = {
                            visualDebug = it
                            viewModel.toggleVisualInspection(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = SpeedGreen, checkedTrackColor = CarbonDark)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Reset to default Glen Canyon Bridge
                            val bridgePreset = StartingPreset.PRESETS[0]
                            posX = bridgePreset.x
                            posZ = bridgePreset.z
                            heading = bridgePreset.headingDeg
                            heightOffset = 0.0f
                            viewModel.selectStartingPreset(bridgePreset)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESET TO BRIDGE", color = TextWhite, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.updateStartingPosition(posX, alignment.carY, posZ, heading, heightOffset)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("save_starting_position_button")
                    ) {
                        Text("APPLY & SPAWN", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

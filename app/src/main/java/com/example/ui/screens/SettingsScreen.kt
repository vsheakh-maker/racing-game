package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.RaceRecordEntity
import com.example.ui.components.RoadInspectionDialog
import com.example.ui.components.formatLapTime
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.RaceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: RaceViewModel,
    records: List<RaceRecordEntity>,
    modifier: Modifier = Modifier
) {
    var useMph by remember { mutableStateOf(viewModel.useMph) }
    var haptics by remember { mutableStateOf(viewModel.hapticsEnabled) }
    var soundMuted by remember { mutableStateOf(viewModel.audioEngine.isMuted) }
    val startingConfig by viewModel.startingConfig.collectAsState()
    var showRoadInspectionDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        val isLandscape = maxWidth > 650.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) },
                    modifier = Modifier
                        .testTag("settings_back_button")
                        .size(42.dp)
                        .background(CarbonSurface, CircleShape)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "SETTINGS & ROAD CALIBRATION",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Configure controls, road spawn positions, and lap records",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CONTENT
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PreferencesCard(
                        useMph = useMph,
                        haptics = haptics,
                        soundMuted = soundMuted,
                        isVisualDebug = startingConfig.isVisualInspectionEnabled,
                        onMphChange = { useMph = it; viewModel.useMph = it },
                        onHapticsChange = { haptics = it; viewModel.hapticsEnabled = it },
                        onMuteChange = { soundMuted = it; viewModel.audioEngine.isMuted = it },
                        onVisualDebugChange = { viewModel.toggleVisualInspection(it) },
                        onOpenCalibration = { showRoadInspectionDialog = true },
                        modifier = Modifier.weight(1.1f).fillMaxHeight()
                    )

                    LapRecordsCard(
                        records = records,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PreferencesCard(
                        useMph = useMph,
                        haptics = haptics,
                        soundMuted = soundMuted,
                        isVisualDebug = startingConfig.isVisualInspectionEnabled,
                        onMphChange = { useMph = it; viewModel.useMph = it },
                        onHapticsChange = { haptics = it; viewModel.hapticsEnabled = it },
                        onMuteChange = { soundMuted = it; viewModel.audioEngine.isMuted = it },
                        onVisualDebugChange = { viewModel.toggleVisualInspection(it) },
                        onOpenCalibration = { showRoadInspectionDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    LapRecordsCard(
                        records = records,
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    )
                }
            }
        }

        // Road Inspection Dialog
        if (showRoadInspectionDialog) {
            RoadInspectionDialog(
                viewModel = viewModel,
                startingConfig = startingConfig,
                onDismiss = { showRoadInspectionDialog = false }
            )
        }
    }
}

@Composable
fun PreferencesCard(
    useMph: Boolean,
    haptics: Boolean,
    soundMuted: Boolean,
    isVisualDebug: Boolean,
    onMphChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onMuteChange: (Boolean) -> Unit,
    onVisualDebugChange: (Boolean) -> Unit,
    onOpenCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CarbonSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("GAMEPLAY & CONTROLS", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 13.sp)

        // Speed Units
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Speedometer Units", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(if (useMph) "Imperial (MPH)" else "Metric (KM/H)", color = TextMuted, fontSize = 10.sp)
            }
            Switch(
                checked = useMph,
                onCheckedChange = onMphChange,
                colors = SwitchDefaults.colors(checkedThumbColor = RacingOrange, checkedTrackColor = CarbonCard)
            )
        }

        // Haptics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Haptic Feedback", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Vibration on collisions, shifts & nitro", color = TextMuted, fontSize = 10.sp)
            }
            Switch(
                checked = haptics,
                onCheckedChange = onHapticsChange,
                colors = SwitchDefaults.colors(checkedThumbColor = RacingOrange, checkedTrackColor = CarbonCard)
            )
        }

        // Sound Effects
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Engine & SFX Audio", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Procedural V8/W16 audio & drift screech", color = TextMuted, fontSize = 10.sp)
            }
            Switch(
                checked = !soundMuted,
                onCheckedChange = { onMuteChange(!it) },
                colors = SwitchDefaults.colors(checkedThumbColor = RacingOrange, checkedTrackColor = CarbonCard)
            )
        }

        // Visual Road Collision Debugger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Visual Road Inspection Mode", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Show 3D collision wireframe & 4-wheel contact points", color = TextMuted, fontSize = 10.sp)
            }
            Switch(
                checked = isVisualDebug,
                onCheckedChange = onVisualDebugChange,
                colors = SwitchDefaults.colors(checkedThumbColor = SpeedGreen, checkedTrackColor = CarbonCard)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Open Calibration Tool Button
        Button(
            onClick = onOpenCalibration,
            colors = ButtonDefaults.buttonColors(containerColor = CarbonCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, NitroCyan),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("open_road_calibration_button")
        ) {
            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = NitroCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("CALIBRATE ROAD & CAR SPAWN POSITION", color = NitroCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Glen Canyon Dam Circuit • 3D OpenGL ES 2.0 Engine • GT Racing 2 Map",
            color = TextMuted,
            fontSize = 9.sp
        )
    }
}

@Composable
fun LapRecordsCard(records: List<RaceRecordEntity>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(CarbonSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldTrophy, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("FASTEST LAP RECORDS", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No lap records yet. Complete a race to set a time!", color = TextMuted, fontSize = 11.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(records) { rec ->
                    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(rec.timestamp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarbonCard)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(rec.carId.uppercase(), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("$dateStr • ${rec.gameMode}", color = TextMuted, fontSize = 9.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    formatLapTime(rec.bestLapTimeMs),
                                    color = GoldTrophy,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text("Pos: #${rec.finishPosition}", color = TextMuted, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

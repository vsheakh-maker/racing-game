package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiDifficulty
import com.example.data.model.CarModel
import com.example.data.model.TimeOfDay
import com.example.data.model.WeatherCondition
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.RaceViewModel

@Composable
fun QuickRaceScreen(
    viewModel: RaceViewModel,
    selectedCar: CarModel,
    modifier: Modifier = Modifier
) {
    var selectedLaps by remember { mutableIntStateOf(2) }
    var selectedDifficulty by remember { mutableStateOf(AiDifficulty.MEDIUM) }
    var selectedTimeOfDay by remember { mutableStateOf(TimeOfDay.SUNSET) }
    val currentWeather by viewModel.currentWeather.collectAsState()
    var isDynamic by remember { mutableStateOf(viewModel.isDynamicWeather) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
    ) {
        val isLandscape = maxWidth > 650.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) },
                        modifier = Modifier
                            .testTag("quick_race_back_button")
                            .size(42.dp)
                            .background(CarbonSurface, CircleShape)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "RACE & WEATHER SETUP",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Glen Canyon Dam Circuit • ${selectedCar.name}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Text(
                    text = selectedCar.name,
                    color = NitroCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SETTINGS CARDS
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    WeatherOptionCard(
                        selectedWeather = currentWeather,
                        isDynamic = isDynamic,
                        onSelectWeather = {
                            isDynamic = false
                            viewModel.toggleDynamicWeather(false)
                            viewModel.setWeather(it)
                        },
                        onToggleDynamic = {
                            isDynamic = it
                            viewModel.toggleDynamicWeather(it)
                        },
                        modifier = Modifier.weight(1.2f).fillMaxHeight()
                    )
                    LapsOptionCard(selectedLaps, onSelect = { selectedLaps = it }, modifier = Modifier.weight(1f).fillMaxHeight())
                    TimeOfDayOptionCard(selectedTimeOfDay, onSelect = { selectedTimeOfDay = it }, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WeatherOptionCard(
                        selectedWeather = currentWeather,
                        isDynamic = isDynamic,
                        onSelectWeather = {
                            isDynamic = false
                            viewModel.toggleDynamicWeather(false)
                            viewModel.setWeather(it)
                        },
                        onToggleDynamic = {
                            isDynamic = it
                            viewModel.toggleDynamicWeather(it)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    LapsOptionCard(selectedLaps, onSelect = { selectedLaps = it }, modifier = Modifier.fillMaxWidth())
                    TimeOfDayOptionCard(selectedTimeOfDay, onSelect = { selectedTimeOfDay = it }, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // START BUTTON
            Button(
                onClick = {
                    viewModel.startQuickRace(selectedLaps, selectedDifficulty, selectedTimeOfDay, currentWeather)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("start_quick_race_button")
            ) {
                Text(
                    text = "START DRIVE • ${selectedCar.name.uppercase()} • ${currentWeather.label.uppercase()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun WeatherOptionCard(
    selectedWeather: WeatherCondition,
    isDynamic: Boolean,
    onSelectWeather: (WeatherCondition) -> Unit,
    onToggleDynamic: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CarbonSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("TRACK WEATHER & FRICTION", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(
                text = if (isDynamic) "Auto-random weather transitions every 50s" else "Custom fixed weather condition",
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WeatherCondition.values().forEach { weather ->
                val isSelected = selectedWeather == weather && !isDynamic
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) RacingOrange else CarbonCard)
                        .border(1.dp, if (isSelected) Color.White else CarbonBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelectWeather(weather) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val icon = when (weather) {
                                WeatherCondition.SUNNY -> "☀️ Sunny Clear"
                                WeatherCondition.OVERCAST -> "☁️ Overcast Clouds"
                                WeatherCondition.RAINY -> "🌧️ Thunderstorm Rain"
                            }
                            Text(text = icon, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = weather.description, color = if (isSelected) TextWhite.copy(alpha = 0.85f) else TextMuted, fontSize = 9.sp, maxLines = 1)
                        }
                        Text(
                            text = "${(weather.frictionMultiplier * 100).toInt()}% GRIP",
                            color = if (isSelected) TextWhite else if (weather.isRainActive) SpeedYellow else SpeedGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Dynamic Weather Option
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDynamic) NitroCyan else CarbonCard)
                    .border(1.dp, if (isDynamic) Color.White else CarbonBorder, RoundedCornerShape(10.dp))
                    .clickable { onToggleDynamic(!isDynamic) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🎲 Dynamic Random Weather", color = if (isDynamic) CarbonDark else TextWhite, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Text("Changes dynamically while racing", color = if (isDynamic) CarbonDark.copy(alpha = 0.8f) else TextMuted, fontSize = 9.sp)
                    }
                    Text(
                        text = if (isDynamic) "ACTIVE" else "OFF",
                        color = if (isDynamic) CarbonDark else TextMuted,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LapsOptionCard(selectedLaps: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(CarbonSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("CIRCUIT LAPS", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text("Distance per lap: 2.31 km", color = TextMuted, fontSize = 10.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "1 Lap (Sprint)", 2 to "2 Laps (Standard)", 3 to "3 Laps (Championship)", 5 to "5 Laps (Endurance)").forEach { (laps, label) ->
                val isSelected = selectedLaps == laps
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) RacingOrange else CarbonCard)
                        .border(1.dp, if (isSelected) Color.White else CarbonBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelect(laps) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(label, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DifficultyOptionCard(selectedDifficulty: AiDifficulty, onSelect: (AiDifficulty) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(CarbonSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("DRIVING DYNAMICS", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text("Performance & handling calibration", color = TextMuted, fontSize = 10.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AiDifficulty.values().forEach { diff ->
                val isSelected = selectedDifficulty == diff
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) RacingOrange else CarbonCard)
                        .border(1.dp, if (isSelected) Color.White else CarbonBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelect(diff) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(diff.label, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TimeOfDayOptionCard(selectedTime: TimeOfDay, onSelect: (TimeOfDay) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(CarbonSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("TIME OF DAY", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text("Atmospheric lighting & canyon shadows", color = TextMuted, fontSize = 10.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeOfDay.values().forEach { tod ->
                val isSelected = selectedTime == tod
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) RacingOrange else CarbonCard)
                        .border(1.dp, if (isSelected) Color.White else CarbonBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelect(tod) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(tod.label, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RaceState
import com.example.ui.components.formatLapTime
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.RaceViewModel

@Composable
fun RaceResultScreen(
    viewModel: RaceViewModel,
    raceState: RaceState,
    modifier: Modifier = Modifier
) {
    val isPodium = raceState.currentPosition <= 3
    val trophyColor = when (raceState.currentPosition) {
        1 -> GoldTrophy
        2 -> SilverTrophy
        3 -> BronzeTrophy
        else -> TextMuted
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        val isLandscape = maxWidth > 650.dp

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PodiumSummaryCard(
                    viewModel = viewModel,
                    raceState = raceState,
                    isPodium = isPodium,
                    trophyColor = trophyColor,
                    modifier = Modifier.weight(1.1f).fillMaxHeight()
                )

                StandingsCard(
                    raceState = raceState,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PodiumSummaryCard(
                    viewModel = viewModel,
                    raceState = raceState,
                    isPodium = isPodium,
                    trophyColor = trophyColor,
                    modifier = Modifier.fillMaxWidth()
                )

                StandingsCard(
                    raceState = raceState,
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                )
            }
        }
    }
}

@Composable
fun PodiumSummaryCard(
    viewModel: RaceViewModel,
    raceState: RaceState,
    isPodium: Boolean,
    trophyColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CarbonSurface, RoundedCornerShape(18.dp))
            .border(1.dp, CarbonBorder, RoundedCornerShape(18.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Trophy",
                tint = trophyColor,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isPodium) "PODIUM FINISH!" else "RACE COMPLETED",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Glen Canyon Dam Circuit",
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${raceState.currentPosition}${when (raceState.currentPosition) { 1 -> "ST"; 2 -> "ND"; 3 -> "RD"; else -> "TH" }} PLACE",
                color = trophyColor,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stats summary
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Best Lap Time", color = TextMuted, fontSize = 12.sp)
                Text(formatLapTime(raceState.bestLapTimeMs), color = TextWhite, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Race Time", color = TextMuted, fontSize = 12.sp)
                Text(formatLapTime(raceState.totalRaceTimeMs), color = TextWhite, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Drift Score Bonus", color = TextMuted, fontSize = 12.sp)
                Text("+${raceState.telemetry.driftScore} PTS", color = SpeedYellow, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = CarbonBorder)
            Spacer(modifier = Modifier.height(4.dp))

            val rewardCredits = when (raceState.currentPosition) {
                1 -> 25000
                2 -> 15000
                3 -> 10000
                else -> 4000
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldTrophy, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PRIZE MONEY", color = GoldTrophy, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
                Text("+$rewardCredits CR", color = GoldTrophy, fontWeight = FontWeight.Black, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.restartRace() },
                modifier = Modifier.weight(1f).testTag("replay_race_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("RETRY", color = TextWhite, fontSize = 12.sp)
            }
            Button(
                onClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) },
                colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("continue_menu_button")
            ) {
                Text("CONTINUE", fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StandingsCard(raceState: RaceState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(CarbonSurface, RoundedCornerShape(18.dp))
            .border(1.dp, CarbonBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "FINAL STANDINGS",
            color = TextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(raceState.racers) { racer ->
                val isMe = racer.isPlayer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMe) CarbonCard else Color(0x66161B22))
                        .border(
                            width = if (isMe) 2.dp else 1.dp,
                            color = if (isMe) NitroCyan else CarbonBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${racer.currentPosition}",
                                color = when (racer.currentPosition) {
                                    1 -> GoldTrophy
                                    2 -> SilverTrophy
                                    3 -> BronzeTrophy
                                    else -> TextMuted
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isMe) "${racer.name} (YOU)" else racer.name,
                                    color = if (isMe) NitroCyan else TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = racer.carName,
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Text(
                            text = "${racer.currentSpeedKmh.toInt()} km/h",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

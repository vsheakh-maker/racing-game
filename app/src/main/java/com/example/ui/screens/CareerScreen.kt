package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
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
import com.example.data.database.PlayerProfileEntity
import com.example.data.model.CareerCup
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.RaceViewModel

@Composable
fun CareerScreen(
    viewModel: RaceViewModel,
    profile: PlayerProfileEntity?,
    modifier: Modifier = Modifier
) {
    val completedCups = profile?.completedCupsCsv?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    val playerStars = profile?.stars ?: 0

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
    ) {
        val isLandscape = maxWidth > 650.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
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
                            .testTag("career_back_button")
                            .size(42.dp)
                            .background(CarbonSurface, CircleShape)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "GLEN CANYON CUP",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Championship series on the Dam Circuit",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Stars total
                Box(
                    modifier = Modifier
                        .background(CarbonSurface, RoundedCornerShape(18.dp))
                        .border(1.dp, CarbonBorder, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = SpeedYellow, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$playerStars ★",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CUPS DISPLAY
            if (isLandscape) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(CareerCup.ALL_CUPS) { cup ->
                        CareerCupCard(
                            cup = cup,
                            isUnlocked = playerStars >= cup.requiredStars,
                            isCompleted = completedCups.contains(cup.id),
                            onSelect = { viewModel.startCareerRace(cup) },
                            modifier = Modifier.width(260.dp).fillMaxHeight()
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(CareerCup.ALL_CUPS) { cup ->
                        CareerCupCard(
                            cup = cup,
                            isUnlocked = playerStars >= cup.requiredStars,
                            isCompleted = completedCups.contains(cup.id),
                            onSelect = { viewModel.startCareerRace(cup) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CareerCupCard(
    cup: CareerCup,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("cup_card_${cup.id}")
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isUnlocked, onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isUnlocked) CarbonSurface else Color(0x99161B22)),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCompleted) 2.dp else 1.dp,
            color = if (isCompleted) GoldTrophy else if (isUnlocked) CarbonBorder else Color.DarkGray
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (isCompleted) GoldTrophy else if (isUnlocked) RacingOrange else TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                    if (isCompleted) {
                        Text("COMPLETED", color = GoldTrophy, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    } else if (!isUnlocked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${cup.requiredStars} ★", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = cup.name,
                    color = if (isUnlocked) TextWhite else TextMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cup.description,
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Laps: ${cup.laps}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("AI: ${cup.difficulty.label}", color = NitroCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(cup.timeOfDay.label, color = TextMuted, fontSize = 11.sp)
                    Text("${cup.rewardCredits} CR", color = GoldTrophy, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = onSelect,
                    enabled = isUnlocked,
                    colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("enter_cup_button_${cup.id}")
                ) {
                    Text(if (isUnlocked) "ENTER RACE" else "LOCKED", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

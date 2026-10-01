package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.database.PlayerProfileEntity
import com.example.data.model.CarModel
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.RaceViewModel

@Composable
fun MainMenuScreen(
    viewModel: RaceViewModel,
    profile: PlayerProfileEntity?,
    selectedCar: CarModel,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
    ) {
        val isLandscape = maxWidth > maxHeight

        // Hero Background Banner
        Image(
            painter = painterResource(id = R.drawable.img_canyon_banner),
            contentDescription = "Glen Canyon Dam Racing Banner",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient darken overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDD0D1117),
                            Color(0xEE0D1117),
                            Color(0xFF0D1117)
                        )
                    )
                )
        )

        if (isLandscape) {
            // LANDSCAPE LAYOUT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                MainMenuHeader(profile)

                // Car Preview Card
                SelectedCarPreviewCard(selectedCar) { viewModel.navigateTo(AppScreen.GARAGE) }

                // Modes Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuModeCard(
                        title = "GLEN CANYON CUP",
                        subtitle = "Championship Series",
                        icon = Icons.Default.EmojiEvents,
                        accentColor = GoldTrophy,
                        testTag = "career_cup_button",
                        modifier = Modifier.weight(1.1f),
                        onClick = { viewModel.navigateTo(AppScreen.CAREER_SELECT) }
                    )
                    MenuModeCard(
                        title = "QUICK RACE",
                        subtitle = "6-Car Canyon Battle",
                        icon = Icons.Default.Speed,
                        accentColor = RacingRed,
                        testTag = "quick_race_button",
                        modifier = Modifier.weight(1.0f),
                        onClick = { viewModel.navigateTo(AppScreen.QUICK_RACE_CONFIG) }
                    )
                    MenuModeCard(
                        title = "TIME ATTACK",
                        subtitle = "Solo Lap Record",
                        icon = Icons.Default.Timer,
                        accentColor = NitroCyan,
                        testTag = "time_attack_button",
                        modifier = Modifier.weight(1.0f),
                        onClick = { viewModel.startTimeAttack() }
                    )
                    MenuModeCard(
                        title = "FREE CRUISE",
                        subtitle = "Open Canyon",
                        icon = Icons.Default.Explore,
                        accentColor = SpeedGreen,
                        testTag = "free_drive_button",
                        modifier = Modifier.weight(1.0f),
                        onClick = { viewModel.startFreeDrive() }
                    )
                    MenuModeCard(
                        title = "GARAGE",
                        subtitle = "Cars & Tuning",
                        icon = Icons.Default.Build,
                        accentColor = RacingOrange,
                        testTag = "garage_button",
                        modifier = Modifier.weight(0.9f),
                        onClick = { viewModel.navigateTo(AppScreen.GARAGE) }
                    )
                    MenuModeCard(
                        title = "SETTINGS",
                        subtitle = "Controls & SFX",
                        icon = Icons.Default.Settings,
                        accentColor = TextMuted,
                        testTag = "settings_button",
                        modifier = Modifier.weight(0.9f),
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }
                    )
                }
            }
        } else {
            // PORTRAIT / VERTICAL MOBILE LAYOUT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MainMenuHeader(profile)

                SelectedCarPreviewCard(selectedCar) { viewModel.navigateTo(AppScreen.GARAGE) }

                Text(
                    text = "SELECT GAME MODE",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // 2-column or stacked cards for portrait
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuModeCard(
                        title = "GLEN CANYON CUP",
                        subtitle = "Championship Series",
                        icon = Icons.Default.EmojiEvents,
                        accentColor = GoldTrophy,
                        testTag = "career_cup_button",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.CAREER_SELECT) }
                    )
                    MenuModeCard(
                        title = "QUICK RACE",
                        subtitle = "6-Car Canyon Battle",
                        icon = Icons.Default.Speed,
                        accentColor = RacingRed,
                        testTag = "quick_race_button",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.QUICK_RACE_CONFIG) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuModeCard(
                        title = "TIME ATTACK",
                        subtitle = "Solo Lap Record",
                        icon = Icons.Default.Timer,
                        accentColor = NitroCyan,
                        testTag = "time_attack_button",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.startTimeAttack() }
                    )
                    MenuModeCard(
                        title = "FREE CRUISE",
                        subtitle = "Explore the Canyon",
                        icon = Icons.Default.Explore,
                        accentColor = SpeedGreen,
                        testTag = "free_drive_button",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.startFreeDrive() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuModeCard(
                        title = "GARAGE & TUNING",
                        subtitle = "Cars & Paint Shop",
                        icon = Icons.Default.Build,
                        accentColor = RacingOrange,
                        testTag = "garage_button",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.GARAGE) }
                    )
                    MenuModeCard(
                        title = "SETTINGS",
                        subtitle = "Controls, Units & SFX",
                        icon = Icons.Default.Settings,
                        accentColor = TextMuted,
                        testTag = "settings_button",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }
                    )
                }
            }
        }
    }
}

@Composable
fun MainMenuHeader(profile: PlayerProfileEntity?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "GLEN CANYON",
                    color = RacingOrange,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(RacingRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "GT RACING 2",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
            }
            Text(
                text = "Page, Arizona • 2.31 km Dam & Bridge Circuit",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .background(Color(0xCC161B22), RoundedCornerShape(16.dp))
                    .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldTrophy, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${profile?.credits ?: 0} CR",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(Color(0xCC161B22), RoundedCornerShape(16.dp))
                    .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = SpeedYellow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${profile?.stars ?: 0}",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun SelectedCarPreviewCard(selectedCar: CarModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xDD161B22))
            .border(1.dp, CarbonBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x33000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = Color(selectedCar.selectedColorHex),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = selectedCar.name,
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${selectedCar.category} • Top: ${selectedCar.topSpeedKmh.toInt()} km/h • 0-100: ${selectedCar.acceleration}s",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
        Text(
            text = "GARAGE >",
            color = NitroCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MenuModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .height(84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xDD161B22)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    color = TextWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }
    }
}

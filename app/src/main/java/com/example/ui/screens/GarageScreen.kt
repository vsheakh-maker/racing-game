package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.data.database.PlayerProfileEntity
import com.example.data.model.CarModel
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.RaceViewModel

@Composable
fun GarageScreen(
    viewModel: RaceViewModel,
    profile: PlayerProfileEntity?,
    allCars: List<CarModel>,
    selectedCar: CarModel,
    modifier: Modifier = Modifier
) {
    var inspectedCar by remember(selectedCar) { mutableStateOf(selectedCar) }

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
                            .testTag("garage_back_button")
                            .size(42.dp)
                            .background(CarbonSurface, CircleShape)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "GLEN CANYON GARAGE",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Customize, tune & upgrade your machines",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Player balance
                Box(
                    modifier = Modifier
                        .background(CarbonSurface, RoundedCornerShape(18.dp))
                        .border(1.dp, CarbonBorder, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldTrophy, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile?.credits ?: 0} CR",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CAR SELECTOR ROW
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allCars) { car ->
                    val isInspected = car.id == inspectedCar.id
                    val isCurrent = car.id == selectedCar.id

                    Box(
                        modifier = Modifier
                            .testTag("car_card_${car.id}")
                            .width(145.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isInspected) CarbonCard else CarbonSurface)
                            .border(
                                width = if (isInspected) 2.dp else 1.dp,
                                color = if (isInspected) RacingOrange else CarbonBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { inspectedCar = car }
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(car.selectedColorHex))
                                )
                                if (isCurrent) {
                                    Text(
                                        text = "EQUIPPED",
                                        color = SpeedGreen,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (!car.isUnlocked) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = car.name,
                                color = TextWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Text(
                                text = car.category,
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // MAIN CAR DETAILS & TUNING BODY
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CarVisualCard(
                        inspectedCar = inspectedCar,
                        selectedCar = selectedCar,
                        profile = profile,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )

                    TuningCard(
                        inspectedCar = inspectedCar,
                        profile = profile,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1.1f).fillMaxHeight()
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
                    CarVisualCard(
                        inspectedCar = inspectedCar,
                        selectedCar = selectedCar,
                        profile = profile,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth()
                    )

                    TuningCard(
                        inspectedCar = inspectedCar,
                        profile = profile,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun CarVisualCard(
    inspectedCar: CarModel,
    selectedCar: CarModel,
    profile: PlayerProfileEntity?,
    viewModel: RaceViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CarbonSurface, RoundedCornerShape(14.dp))
            .border(1.dp, CarbonBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = inspectedCar.name,
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = inspectedCar.category,
                        color = NitroCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (inspectedCar.isUnlocked) {
                    if (inspectedCar.id != selectedCar.id) {
                        Button(
                            onClick = { viewModel.selectCar(inspectedCar) },
                            colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("select_car_button")
                        ) {
                            Text("EQUIP", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    } else {
                        Text("EQUIPPED", color = SpeedGreen, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                } else {
                    val canAfford = (profile?.credits ?: 0) >= inspectedCar.priceCredits
                    Button(
                        onClick = { viewModel.unlockCar(inspectedCar) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) GoldTrophy else CarbonCard),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("unlock_car_button")
                    ) {
                        Text(
                            "${inspectedCar.priceCredits} CR",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = if (canAfford) Color.Black else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Silhouette
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .background(Color(0x33000000), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = Color(inspectedCar.selectedColorHex),
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Paint Palette
        Column {
            Text(text = "CUSTOM PAINT FINISH", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CarModel.PAINT_PALETTE.forEach { colorHex ->
                    val isSelected = inspectedCar.selectedColorHex == colorHex
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else CarbonBorder,
                                shape = CircleShape
                            )
                            .clickable {
                                viewModel.setCarColor(inspectedCar, colorHex)
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun TuningCard(
    inspectedCar: CarModel,
    profile: PlayerProfileEntity?,
    viewModel: RaceViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CarbonSurface, RoundedCornerShape(14.dp))
            .border(1.dp, CarbonBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "PERFORMANCE TUNING",
            color = TextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            UpgradeRow(
                title = "ENGINE (TOP SPEED)",
                value = "${inspectedCar.topSpeedKmh.toInt()} km/h",
                level = inspectedCar.engineLevel,
                price = inspectedCar.engineLevel * 10000,
                canAfford = (profile?.credits ?: 0) >= inspectedCar.engineLevel * 10000,
                isUnlocked = inspectedCar.isUnlocked,
                onUpgrade = { viewModel.upgradeCar(inspectedCar, "engine", inspectedCar.engineLevel * 10000) }
            )

            UpgradeRow(
                title = "TIRES & SUSPENSION",
                value = "${(inspectedCar.handling * 100).toInt()}% GRIP",
                level = inspectedCar.handlingLevel,
                price = inspectedCar.handlingLevel * 8000,
                canAfford = (profile?.credits ?: 0) >= inspectedCar.handlingLevel * 8000,
                isUnlocked = inspectedCar.isUnlocked,
                onUpgrade = { viewModel.upgradeCar(inspectedCar, "handling", inspectedCar.handlingLevel * 8000) }
            )

            UpgradeRow(
                title = "NOS TURBO BOOST",
                value = "${inspectedCar.nitroCapacity.toInt()} PSI",
                level = inspectedCar.nitroLevel,
                price = inspectedCar.nitroLevel * 6000,
                canAfford = (profile?.credits ?: 0) >= inspectedCar.nitroLevel * 6000,
                isUnlocked = inspectedCar.isUnlocked,
                onUpgrade = { viewModel.upgradeCar(inspectedCar, "nitro", inspectedCar.nitroLevel * 6000) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Upgrades apply instantly to the Glen Canyon Dam circuit physics.",
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
fun UpgradeRow(
    title: String,
    value: String,
    level: Int,
    price: Int,
    canAfford: Boolean,
    isUnlocked: Boolean,
    onUpgrade: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(text = value, color = NitroCyan, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 1..5) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (i <= level) RacingOrange else CarbonCard)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (level < 5 && isUnlocked) {
            Button(
                onClick = onUpgrade,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) CarbonCard else Color(0x33FFFFFF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (canAfford) RacingOrange else CarbonBorder),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = "$price CR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canAfford) GoldTrophy else TextMuted
                )
            }
        } else if (level >= 5) {
            Text(text = "MAX", color = SpeedGreen, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
    }
}

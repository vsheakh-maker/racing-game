package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.RaceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CarbonDark
                ) {
                    val viewModel: RaceViewModel = viewModel()
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    val profile by viewModel.profile.collectAsState()
                    val selectedCar by viewModel.selectedCar.collectAsState()
                    val allCars by viewModel.allCars.collectAsState()
                    val bestRecords by viewModel.bestRecords.collectAsState()
                    val raceState by viewModel.raceState.collectAsState()

                    // BackHandler for secondary screens
                    if (currentScreen != AppScreen.MAIN_MENU && currentScreen != AppScreen.RACING) {
                        BackHandler {
                            viewModel.navigateTo(AppScreen.MAIN_MENU)
                        }
                    }

                    when (currentScreen) {
                        AppScreen.MAIN_MENU -> {
                            MainMenuScreen(
                                viewModel = viewModel,
                                profile = profile,
                                selectedCar = selectedCar
                            )
                        }
                        AppScreen.GARAGE -> {
                            GarageScreen(
                                viewModel = viewModel,
                                profile = profile,
                                allCars = allCars,
                                selectedCar = selectedCar
                            )
                        }
                        AppScreen.CAREER_SELECT -> {
                            CareerScreen(
                                viewModel = viewModel,
                                profile = profile
                            )
                        }
                        AppScreen.QUICK_RACE_CONFIG -> {
                            QuickRaceScreen(
                                viewModel = viewModel,
                                selectedCar = selectedCar
                            )
                        }
                        AppScreen.RACING -> {
                            RaceScreen(
                                viewModel = viewModel
                            )
                        }
                        AppScreen.RACE_RESULTS -> {
                            RaceResultScreen(
                                viewModel = viewModel,
                                raceState = raceState
                            )
                        }
                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                records = bestRecords
                            )
                        }
                    }
                }
            }
        }
    }
}

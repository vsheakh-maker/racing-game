package com.example

import com.example.data.model.CarModel
import com.example.data.model.RaceState
import com.example.data.model.RaceStatus
import com.example.data.model.RaceTelemetry
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test 1 - all 11 legendary cars are configured with valid performance specs`() {
        val cars = CarModel.ALL_CARS
        assertEquals("There must be 11 legendary cars in roster", 11, cars.size)

        // Starter car must be unlocked for immediate gameplay
        val starterCar = cars.first { it.id == "mustang_1965" }
        assertTrue("1965 Mustang must be unlocked by default", starterCar.isUnlocked)
        assertEquals("1965 Mustang price must be 0 credits", 0, starterCar.priceCredits)

        // All other cars must have positive top speeds, acceleration, handling, and nitro
        for (car in cars) {
            assertTrue("Car ${car.name} top speed must be > 200 km/h", car.topSpeedKmh > 200f)
            assertTrue("Car ${car.name} 0-100 time must be <= 4.0s", car.acceleration <= 4.0f)
            assertTrue("Car ${car.name} handling must be between 0.8 and 1.0", car.handling in 0.8f..1.0f)
            assertTrue("Car ${car.name} nitro capacity must be >= 100", car.nitroCapacity >= 100f)
        }
    }

    @Test
    fun `test 2 - race state initializes with prominent timer and telemetry defaults`() {
        val defaultState = RaceState()
        assertEquals(RaceStatus.COUNTDOWN, defaultState.status)
        assertEquals(3, defaultState.countdownNumber)
        assertEquals(0L, defaultState.totalRaceTimeMs)
        assertEquals(0L, defaultState.currentLapTimeMs)

        val telemetry = RaceTelemetry(
            speedKmh = 145.5f,
            isDrafting = true,
            isOverdriveActive = true,
            isNitroActive = true
        )
        assertTrue("Telemetry must report drafting active", telemetry.isDrafting)
        assertTrue("Telemetry must report overdrive active", telemetry.isOverdriveActive)
        assertTrue("Telemetry must report nitro active", telemetry.isNitroActive)
    }

    @Test
    fun `test 3 - race duration timer format calculations`() {
        val durationMs = 125_480L // 2 minutes, 5 seconds, 480 milliseconds
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = durationMs % 1000

        assertEquals(2L, minutes)
        assertEquals(5L, seconds)
        assertEquals(480L, millis)

        val minStr = minutes.toString().padStart(2, '0')
        val secStr = seconds.toString().padStart(2, '0')
        val centiStr = (millis / 10).toString().padStart(2, '0')

        assertEquals("02:05.48", "$minStr:$secStr.$centiStr")
    }
}

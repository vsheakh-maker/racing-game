package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CarModel
import com.example.data.model.StartingPreset
import com.example.engine.CarPhysics
import com.example.engine.RoadCollisionSystem
import com.example.engine.TrackData
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoadAlignmentAndPhysicsTest {

    private lateinit var context: Context
    private lateinit var roadCollision: RoadCollisionSystem
    private lateinit var trackData: TrackData
    private val testCar = CarModel.ALL_CARS[0] // Apex GT

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        roadCollision = RoadCollisionSystem()
        val loaded = roadCollision.load(context)
        assertTrue("Road collision must load successfully from assets", loaded)
        trackData = TrackData.loadFromAssets(context)
        assertTrue("Track data checkpoints must not be empty", trackData.checkpoints.isNotEmpty())
    }

    @Test
    fun `test 1 - car spawns directly on the road surface`() {
        val bridgePreset = StartingPreset.PRESETS[0]
        val align = roadCollision.alignCarOnRoad(
            posX = bridgePreset.x,
            posZ = bridgePreset.z,
            headingDeg = bridgePreset.headingDeg
        )
        assertTrue("Car must detect road surface at bridge spawn", align.isOnRoad)
        // Road surface is ~39.97m, chassis sits at wheel radius 0.35m above ground -> ~40.32m
        assertTrue(
            "Car chassis elevation (${align.carY}) must match road surface + wheel radius (~40.32m)",
            abs(align.carY - 40.32f) < 0.15f
        )
    }

    @Test
    fun `test 2 - all four wheels contact the road correctly`() {
        val bridgePreset = StartingPreset.PRESETS[0]
        val align = roadCollision.alignCarOnRoad(
            posX = bridgePreset.x,
            posZ = bridgePreset.z,
            headingDeg = bridgePreset.headingDeg
        )
        val contactingWheels = align.wheelContacts.count { it.isContacting }
        assertEquals(4, contactingWheels)
        for (wheel in align.wheelContacts) {
            assertTrue("Wheel must contact road surface", wheel.isContacting)
            assertTrue(
                "Wheel contact elevation (${wheel.roadY}) must match bridge road elevation (~39.97m)",
                abs(wheel.roadY - 39.97f) < 0.15f
            )
        }
    }

    @Test
    fun `test 3 - car faces the roads driving direction`() {
        val bridgePreset = StartingPreset.PRESETS[0]
        // Bridge road heads towards East (+X) across the canyon, tangent is (tx=1.0, tz=0.0) -> heading 90.0 deg
        assertTrue("Bridge starting heading must be 90 degrees (East)", abs(bridgePreset.headingDeg - 90.0f) < 0.1f)

        val cp0 = trackData.checkpoints[0]
        val cp1 = trackData.checkpoints[1]
        val trackDx = cp1.x - cp0.x
        assertTrue("Track checkpoint progression must head in positive X direction", trackDx > 0f)
    }

    @Test
    fun `test 4 - car can accelerate and steer`() {
        val bridgePreset = StartingPreset.PRESETS[0]
        val align = roadCollision.alignCarOnRoad(bridgePreset.x, bridgePreset.z, bridgePreset.headingDeg)
        val physics = CarPhysics(
            carModel = testCar,
            posX = bridgePreset.x,
            posY = align.carY,
            posZ = bridgePreset.z,
            headingDeg = bridgePreset.headingDeg
        )

        val initialX = physics.posX
        val initialHeading = physics.headingDeg

        // Accelerate with full throttle for 1.0 second (60 steps) with steering input
        for (i in 0 until 60) {
            physics.update(
                dt = 0.0166f,
                throttleInput = 1.0f,
                brakeInput = 0.0f,
                steerInput = 0.5f, // Steer right
                handbrakeInput = false,
                nitroInput = false,
                trackData = trackData,
                roadCollision = roadCollision
            )
        }

        assertTrue("Car must gain speed under throttle (speed: ${physics.speedKmh})", physics.speedKmh > 18f)
        assertTrue("Car position X must advance along driving direction (${physics.posX} > $initialX)", physics.posX > initialX)
        assertTrue(
            "Car heading must change under steering input (was $initialHeading, now ${physics.headingDeg})",
            abs(physics.headingDeg - initialHeading) > 0.5f
        )
    }

    @Test
    fun `test 5 and 6 - car does not fall through or float above road`() {
        val bridgePreset = StartingPreset.PRESETS[0]
        val align = roadCollision.alignCarOnRoad(bridgePreset.x, bridgePreset.z, bridgePreset.headingDeg)
        val physics = CarPhysics(
            carModel = testCar,
            posX = bridgePreset.x,
            posY = align.carY,
            posZ = bridgePreset.z,
            headingDeg = bridgePreset.headingDeg
        )

        // Drive for 120 frames along the bridge
        for (i in 0 until 120) {
            physics.update(
                dt = 0.0166f,
                throttleInput = 0.8f,
                brakeInput = 0.0f,
                steerInput = 0.0f,
                handbrakeInput = false,
                nitroInput = false,
                trackData = trackData,
                roadCollision = roadCollision
            )
            // Verify chassis Y stays tightly bound to road elevation (no sinking into river gorge at Y=0, no floating)
            assertTrue("Car must not fall through road (Y: ${physics.posY} >= 38.0m)", physics.posY >= 38.0f)
            assertTrue("Car must not float into sky (Y: ${physics.posY} <= 43.0m)", physics.posY <= 43.0f)
        }
    }

    @Test
    fun `test 7 - car remains stable on slopes`() {
        // Test Dam Crest where elevation slopes and curves
        val damPreset = StartingPreset.PRESETS[1]
        val align = roadCollision.alignCarOnRoad(damPreset.x, damPreset.z, damPreset.headingDeg)
        assertTrue("Dam Crest preset must be on road", align.isOnRoad)

        val physics = CarPhysics(
            carModel = testCar,
            posX = damPreset.x,
            posY = align.carY,
            posZ = damPreset.z,
            headingDeg = damPreset.headingDeg
        )

        for (i in 0 until 60) {
            physics.update(
                dt = 0.0166f,
                throttleInput = 0.5f,
                brakeInput = 0.0f,
                steerInput = 0.0f,
                handbrakeInput = false,
                nitroInput = false,
                trackData = trackData,
                roadCollision = roadCollision
            )
        }

        // Car remains stably oriented on dam crest (no inversion or tumbling)
        assertTrue("Car must remain stable on slopes (pitch: ${physics.pitchDeg} within +/- 50 deg)", abs(physics.pitchDeg) < 50f)
        assertTrue("Car must remain stable on slopes (roll: ${physics.rollDeg} within +/- 50 deg)", abs(physics.rollDeg) < 50f)
        assertFalse("Pitch must not be NaN", physics.pitchDeg.isNaN())
        assertFalse("Roll must not be NaN", physics.rollDeg.isNaN())
    }

    @Test
    fun `test 8 - restarting returns car to the correct starting position`() {
        val bridgePreset = StartingPreset.PRESETS[0]
        val align = roadCollision.alignCarOnRoad(bridgePreset.x, bridgePreset.z, bridgePreset.headingDeg)
        val physics = CarPhysics(
            carModel = testCar,
            posX = bridgePreset.x,
            posY = align.carY,
            posZ = bridgePreset.z,
            headingDeg = bridgePreset.headingDeg
        )

        // Drive away for 100 frames
        for (i in 0 until 100) {
            physics.update(0.0166f, 1f, 0f, 0f, false, false, trackData, roadCollision)
        }
        assertTrue("Car drove away", physics.posX > bridgePreset.x + 5f)

        // Reset
        physics.resetToPosition(bridgePreset.x, align.carY, bridgePreset.z, bridgePreset.headingDeg)
        assertTrue("Reset must restore X position", abs(physics.posX - bridgePreset.x) < 0.001f)
        assertTrue("Reset must restore Y elevation", abs(physics.posY - align.carY) < 0.001f)
        assertTrue("Reset must restore Z position", abs(physics.posZ - bridgePreset.z) < 0.001f)
        assertTrue("Reset must restore heading", abs(physics.headingDeg - bridgePreset.headingDeg) < 0.001f)
        assertTrue("Reset must zero out speed", abs(physics.speedKmh) < 0.001f)
    }

    @Test
    fun `test 9 - all 5 presets have 4-wheel road surface contact`() {
        for (preset in StartingPreset.PRESETS) {
            val align = roadCollision.alignCarOnRoad(preset.x, preset.z, preset.headingDeg)
            assertTrue("Preset ${preset.name} must be on road", align.isOnRoad)
            val contacts = align.wheelContacts.count { it.isContacting }
            assertEquals("Preset ${preset.name} must have 4 contacting wheels", 4, contacts)
        }
    }

    @Test
    fun `test 10 - weather friction affects car grip and physics`() {
        val bridgePreset = StartingPreset.PRESETS[0]
        val align = roadCollision.alignCarOnRoad(bridgePreset.x, bridgePreset.z, bridgePreset.headingDeg)

        // Sunny car
        val sunnyCar = CarPhysics(testCar, bridgePreset.x, align.carY, bridgePreset.z, bridgePreset.headingDeg)
        sunnyCar.weatherCondition = com.example.data.model.WeatherCondition.SUNNY

        // Rainy car
        val rainyCar = CarPhysics(testCar, bridgePreset.x, align.carY, bridgePreset.z, bridgePreset.headingDeg)
        rainyCar.weatherCondition = com.example.data.model.WeatherCondition.RAINY

        for (i in 0 until 60) {
            sunnyCar.update(0.0166f, 1f, 0f, 0f, false, false, trackData, roadCollision)
            rainyCar.update(0.0166f, 1f, 0f, 0f, false, false, trackData, roadCollision)
        }

        // Sunny track provides higher tire grip & faster acceleration than slick wet rainy asphalt
        assertTrue("Sunny speed (${sunnyCar.speedKmh}) must exceed Rainy speed (${rainyCar.speedKmh})", sunnyCar.speedKmh > rainyCar.speedKmh)
    }

    @Test
    fun `test 11 - road barrier prevents car from leaving road corridor`() {
        val bridgePreset = StartingPreset.PRESETS[0]
        val align = roadCollision.alignCarOnRoad(bridgePreset.x, bridgePreset.z, bridgePreset.headingDeg)

        val car = CarPhysics(testCar, bridgePreset.x, align.carY, bridgePreset.z, bridgePreset.headingDeg)

        // Steer hard sideways for 100 frames to attempt driving off the bridge
        for (i in 0 until 100) {
            car.update(
                dt = 0.0166f,
                throttleInput = 1.0f,
                brakeInput = 0.0f,
                steerInput = 1.0f, // Hard right
                handbrakeInput = false,
                nitroInput = false,
                trackData = trackData,
                roadCollision = roadCollision
            )
        }

        val nearestCp = trackData.findNearestCheckpointIndex(car.posX, car.posZ)
        val cp = trackData.checkpoints[nearestCp]
        val dx = car.posX - cp.x
        val dz = car.posZ - cp.z
        val lateralOffset = abs(dx * cp.nx + dz * cp.nz)
        val roadHalfWidth = cp.width * 0.5f

        // Car must stay within safe road corridor boundary (barrier prevents falling off)
        assertTrue("Car lateral offset ($lateralOffset) must remain within road half width ($roadHalfWidth + 0.5m)", lateralOffset <= roadHalfWidth + 0.5f)
        assertTrue("Car chassis must not fall through bridge (Y >= 38.0m)", car.posY >= 38.0f)
    }
}

package com.example.data.model

data class StartingPreset(
    val id: String,
    val name: String,
    val description: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val headingDeg: Float,
    val heightOffset: Float = 0.0f
) {
    companion object {
        val PRESETS = listOf(
            StartingPreset(
                id = "bridge_start",
                name = "Glen Canyon Bridge (Official Grid)",
                description = "Iconic steel arch suspension bridge facing East towards the dam",
                x = -200.0f,
                y = 40.32f,
                z = -3.0f,
                headingDeg = 90.0f
            ),
            StartingPreset(
                id = "dam_crest",
                name = "Glen Canyon Dam Crest",
                description = "High elevation road directly atop the Glen Canyon Dam wall overlooking Lake Powell",
                x = 280.0f,
                y = 35.91f,
                z = 48.0f,
                headingDeg = 80.4f
            ),
            StartingPreset(
                id = "canyon_switchback",
                name = "East Canyon Hairpin",
                description = "Fast technical switchback descent carved into the red Navajo sandstone",
                x = 569.4f,
                y = 39.78f,
                z = -52.2f,
                headingDeg = 175.0f
            ),
            StartingPreset(
                id = "river_straight",
                name = "Colorado River Straight",
                description = "Ultra-fast low-elevation straightaway along the Colorado river canyon floor",
                x = 160.0f,
                y = 3.02f,
                z = -252.7f,
                headingDeg = 270.0f
            ),
            StartingPreset(
                id = "west_climb",
                name = "West Canyon Overlook",
                description = "High panoramic sweeping climb overlooking the entire Glen Canyon facility",
                x = -435.0f,
                y = 38.83f,
                z = -97.5f,
                headingDeg = 309.0f
            )
        )
    }
}

data class StartingConfig(
    val selectedPresetId: String = "bridge_start",
    val customX: Float = -200.0f,
    val customY: Float = 40.32f,
    val customZ: Float = -3.0f,
    val headingDeg: Float = 90.0f,
    val carHeightOffset: Float = 0.0f,
    val isVisualInspectionEnabled: Boolean = false
)

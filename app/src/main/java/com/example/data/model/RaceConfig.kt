package com.example.data.model

enum class WeatherCondition(
    val label: String,
    val description: String,
    val frictionMultiplier: Float, // Sunny = 1.0f, Overcast = 0.90f, Rainy = 0.72f
    val skyColor: FloatArray,
    val sunColor: FloatArray,
    val ambientColor: FloatArray,
    val fogColor: FloatArray,
    val fogStart: Float,
    val fogEnd: Float,
    val isRainActive: Boolean
) {
    SUNNY(
        label = "Sunny Clear",
        description = "Dry hot asphalt, full tire grip & clear desert visibility",
        frictionMultiplier = 1.0f,
        skyColor = floatArrayOf(0.42f, 0.71f, 1.0f),
        sunColor = floatArrayOf(1.0f, 0.95f, 0.85f),
        ambientColor = floatArrayOf(0.42f, 0.42f, 0.48f),
        fogColor = floatArrayOf(0.78f, 0.85f, 0.95f),
        fogStart = 100f,
        fogEnd = 1900f,
        isRainActive = false
    ),
    OVERCAST(
        label = "Overcast Clouds",
        description = "Cooler cloudy track, mild humidity & balanced friction",
        frictionMultiplier = 0.90f,
        skyColor = floatArrayOf(0.55f, 0.60f, 0.66f),
        sunColor = floatArrayOf(0.75f, 0.78f, 0.82f),
        ambientColor = floatArrayOf(0.38f, 0.40f, 0.45f),
        fogColor = floatArrayOf(0.58f, 0.62f, 0.68f),
        fogStart = 60f,
        fogEnd = 1200f,
        isRainActive = false
    ),
    RAINY(
        label = "Thunderstorm Rain",
        description = "Slick wet asphalt, reduced friction & glistening reflections",
        frictionMultiplier = 0.72f,
        skyColor = floatArrayOf(0.20f, 0.24f, 0.30f),
        sunColor = floatArrayOf(0.45f, 0.50f, 0.60f),
        ambientColor = floatArrayOf(0.25f, 0.28f, 0.35f),
        fogColor = floatArrayOf(0.22f, 0.26f, 0.32f),
        fogStart = 35f,
        fogEnd = 650f,
        isRainActive = true
    )
}

enum class GameMode(val title: String, val description: String) {
    CAREER("Glen Canyon Tour", "Master the championship across dynamic weather and fierce opponents"),
    QUICK_RACE("Quick Drive", "Select your machine, weather, and circuit laps for high-speed action"),
    TIME_ATTACK("Time Attack", "Beat the lap record and test your lines in varied weather conditions"),
    FREE_DRIVE("Free Cruise", "Scenic open cruise across the suspension bridge, dam crest, and canyon floor")
}

enum class AiDifficulty(val label: String, val speedMultiplier: Float) {
    EASY("Cruising", 0.75f),
    MEDIUM("Sport", 0.88f),
    HARD("Racer", 0.98f),
    EXPERT("Legend", 1.05f)
}

enum class TimeOfDay(val label: String, val skyColorHex: Long) {
    SUNNY("Arizona Noon", 0xFF6BB5FFL),
    SUNSET("Golden Canyon", 0xFFFF8A50L),
    NIGHT("Midnight Gorge", 0xFF0D1B2AL)
}

data class CareerCup(
    val id: String,
    val name: String,
    val laps: Int,
    val difficulty: AiDifficulty,
    val rewardCredits: Int,
    val timeOfDay: TimeOfDay,
    val weather: WeatherCondition,
    val description: String,
    val requiredStars: Int
) {
    companion object {
        val ALL_CUPS = listOf(
            CareerCup(
                id = "cup_1",
                name = "Bridge Sunny Sprint",
                laps = 2,
                difficulty = AiDifficulty.EASY,
                rewardCredits = 15000,
                timeOfDay = TimeOfDay.SUNNY,
                weather = WeatherCondition.SUNNY,
                description = "Warm desert sunshine across the Glen Canyon suspension bridge with maximum tire grip",
                requiredStars = 0
            ),
            CareerCup(
                id = "cup_2",
                name = "Overcast Switchback Trophy",
                laps = 3,
                difficulty = AiDifficulty.MEDIUM,
                rewardCredits = 30000,
                timeOfDay = TimeOfDay.SUNSET,
                weather = WeatherCondition.OVERCAST,
                description = "Moody clouds and twilight descent down the tight red rock switchbacks",
                requiredStars = 1
            ),
            CareerCup(
                id = "cup_3",
                name = "Thunderstorm Canyon Run",
                laps = 3,
                difficulty = AiDifficulty.HARD,
                rewardCredits = 60000,
                timeOfDay = TimeOfDay.SUNNY,
                weather = WeatherCondition.RAINY,
                description = "Intense rainstorm with wet glistening asphalt and reduced tire traction",
                requiredStars = 3
            ),
            CareerCup(
                id = "cup_4",
                name = "Midnight Dam Storm",
                laps = 4,
                difficulty = AiDifficulty.EXPERT,
                rewardCredits = 120000,
                timeOfDay = TimeOfDay.NIGHT,
                weather = WeatherCondition.RAINY,
                description = "Night storm driving over the dam crest wall under lightning flashes",
                requiredStars = 6
            )
        )
    }
}

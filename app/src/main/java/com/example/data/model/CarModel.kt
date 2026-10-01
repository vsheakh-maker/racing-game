package com.example.data.model

data class CarModel(
    val id: String,
    val name: String,
    val category: String,
    val engineType: String,
    val baseTopSpeedKmh: Float,
    val baseAcceleration: Float, // 0-100 km/h in sec
    val baseHandling: Float, // 0.0 - 1.0
    val baseNitroCapacity: Float,
    val priceCredits: Int,
    val isUnlocked: Boolean = false,
    val engineLevel: Int = 1,
    val handlingLevel: Int = 1,
    val nitroLevel: Int = 1,
    val selectedColorHex: Long = 0xFFD42E1EL,
    val soundFreqMultiplier: Float = 1.0f,
    val hasSuperchargerWhine: Boolean = false,
    val hasTurboWhistle: Boolean = false
) {
    val topSpeedKmh: Float
        get() = baseTopSpeedKmh + (engineLevel - 1) * 12f

    val acceleration: Float
        get() = (baseAcceleration - (engineLevel - 1) * 0.35f).coerceAtLeast(2.0f)

    val handling: Float
        get() = (baseHandling + (handlingLevel - 1) * 0.035f).coerceAtMost(1.0f)

    val nitroCapacity: Float
        get() = baseNitroCapacity + (nitroLevel - 1) * 20f

    companion object {
        val MUSTANG_1965 = CarModel(
            id = "mustang_1965",
            name = "1965 Ford Mustang Fastback",
            category = "Classic American Muscle",
            engineType = "289ci High-Performance Windsor V8",
            baseTopSpeedKmh = 255f,
            baseAcceleration = 3.8f,
            baseHandling = 0.92f,
            baseNitroCapacity = 100f,
            priceCredits = 0,
            isUnlocked = true,
            selectedColorHex = 0xFFD42E1EL, // Poppy Red
            soundFreqMultiplier = 1.0f
        )

        val CAMARO_1969 = CarModel(
            id = "camaro_1969",
            name = "1969 Chevrolet Camaro SS 396",
            category = "Heavyweight Big-Block Legend",
            engineType = "396ci Turbo-Jet V8 (375 HP)",
            baseTopSpeedKmh = 268f,
            baseAcceleration = 3.5f,
            baseHandling = 0.90f,
            baseNitroCapacity = 110f,
            priceCredits = 25000,
            isUnlocked = false,
            selectedColorHex = 0xFFFF5A00L, // Hugger Orange
            soundFreqMultiplier = 0.92f
        )

        val CORVETTE_1963 = CarModel(
            id = "corvette_1963",
            name = "1963 Corvette Sting Ray Split-Window",
            category = "Iconic American Sports Classic",
            engineType = "327ci Fuel-Injected Small-Block V8 (360 HP)",
            baseTopSpeedKmh = 275f,
            baseAcceleration = 3.4f,
            baseHandling = 0.93f,
            baseNitroCapacity = 115f,
            priceCredits = 40000,
            isUnlocked = false,
            selectedColorHex = 0xFF154889L, // Daytona Blue
            soundFreqMultiplier = 1.08f
        )

        val CHARGER_1970 = CarModel(
            id = "charger_1970",
            name = "1970 Dodge Charger R/T 426 HEMI",
            category = "Blown Street Brawler",
            engineType = "426ci Street HEMI with Roots Blower",
            baseTopSpeedKmh = 282f,
            baseAcceleration = 3.2f,
            baseHandling = 0.87f,
            baseNitroCapacity = 120f,
            priceCredits = 45000,
            isUnlocked = false,
            selectedColorHex = 0xFF1A1A1AL, // Pitch Black with Red R/T Stripe
            soundFreqMultiplier = 0.82f,
            hasSuperchargerWhine = true
        )

        val CUDA_1971 = CarModel(
            id = "cuda_1971",
            name = "1971 Plymouth Hemi 'Cuda",
            category = "High-Impact Mopar Muscle",
            engineType = "426ci Dual-Quad HEMI with Shaker Hood",
            baseTopSpeedKmh = 285f,
            baseAcceleration = 3.2f,
            baseHandling = 0.89f,
            baseNitroCapacity = 120f,
            priceCredits = 50000,
            isUnlocked = false,
            selectedColorHex = 0xFF7C1E8AL, // In-Violet / Plum Crazy Purple
            soundFreqMultiplier = 0.86f,
            hasSuperchargerWhine = false
        )

        val BOSS_429 = CarModel(
            id = "boss_429",
            name = "1969 Ford Mustang Boss 429",
            category = "NASCAR Semi-Hemi Homologation Beast",
            engineType = "429ci Semi-Hemi Crescent V8 with Ram-Air",
            baseTopSpeedKmh = 290f,
            baseAcceleration = 3.1f,
            baseHandling = 0.91f,
            baseNitroCapacity = 125f,
            priceCredits = 60000,
            isUnlocked = false,
            selectedColorHex = 0xFF1A1A1AL, // Raven Black Boss
            soundFreqMultiplier = 0.95f
        )

        val SHELBY_GT500 = CarModel(
            id = "shelby_gt500",
            name = "1967 Shelby GT500 \"Eleanor\"",
            category = "Ultra High-Output Fastback",
            engineType = "428ci Cobra Jet V8 with Side Exhausts",
            baseTopSpeedKmh = 295f,
            baseAcceleration = 2.9f,
            baseHandling = 0.94f,
            baseNitroCapacity = 130f,
            priceCredits = 70000,
            isUnlocked = false,
            selectedColorHex = 0xFF8E959CL, // Pepper Gray with Le Mans Stripes
            soundFreqMultiplier = 1.15f
        )

        val SKYLINE_R34 = CarModel(
            id = "skyline_r34",
            name = "1999 Nissan Skyline GT-R R34 V-Spec",
            category = "Japanese Twin-Turbo AWD Legend",
            engineType = "2.6L RB26DETT Twin-Turbocharged Inline-6",
            baseTopSpeedKmh = 310f,
            baseAcceleration = 2.8f,
            baseHandling = 0.96f,
            baseNitroCapacity = 135f,
            priceCredits = 85000,
            isUnlocked = false,
            selectedColorHex = 0xFF154889L, // Bayside Blue
            soundFreqMultiplier = 1.30f,
            hasTurboWhistle = true
        )

        val SUPRA_MK4 = CarModel(
            id = "supra_mk4",
            name = "1994 Toyota Supra Turbo MK4",
            category = "Sequential Twin-Turbo Tuner King",
            engineType = "3.0L 2JZ-GTE Twin-Turbo Inline-6 (Overbored)",
            baseTopSpeedKmh = 318f,
            baseAcceleration = 2.7f,
            baseHandling = 0.95f,
            baseNitroCapacity = 140f,
            priceCredits = 90000,
            isUnlocked = false,
            selectedColorHex = 0xFFFF5A00L, // Solar Orange Pearl
            soundFreqMultiplier = 1.38f,
            hasTurboWhistle = true
        )

        val MCLAREN_F1 = CarModel(
            id = "mclaren_f1",
            name = "1995 McLaren F1 LM",
            category = "Naturally Aspirated V12 Masterpiece",
            engineType = "6.1L BMW S70/2 60° V12 (Gold Foil Bay)",
            baseTopSpeedKmh = 391f,
            baseAcceleration = 2.5f,
            baseHandling = 0.97f,
            baseNitroCapacity = 145f,
            priceCredits = 110000,
            isUnlocked = false,
            selectedColorHex = 0xFFFF9800L, // Papaya Orange
            soundFreqMultiplier = 1.65f
        )

        val VEYRON_SS = CarModel(
            id = "veyron_ss",
            name = "2024 Veyron Supersport",
            category = "Quad-Turbo Hypercar Demon",
            engineType = "8.0L Quad-Turbocharged W16 (1200 HP)",
            baseTopSpeedKmh = 405f,
            baseAcceleration = 2.4f,
            baseHandling = 0.97f,
            baseNitroCapacity = 150f,
            priceCredits = 125000,
            isUnlocked = false,
            selectedColorHex = 0xFF0055A5L, // French Racing Blue
            soundFreqMultiplier = 1.45f,
            hasTurboWhistle = true
        )

        val ALL_CARS = listOf(
            MUSTANG_1965,
            CAMARO_1969,
            CORVETTE_1963,
            CHARGER_1970,
            CUDA_1971,
            BOSS_429,
            SHELBY_GT500,
            SKYLINE_R34,
            SUPRA_MK4,
            MCLAREN_F1,
            VEYRON_SS
        )

        val PAINT_PALETTE = listOf(
            0xFFD42E1EL, // Classic Poppy Red
            0xFFF0EFEAL, // Wimbledon White
            0xFF154889L, // Guardsman / Bayside Blue
            0xFF1A3828L, // Highland Green (Bullitt)
            0xFF1A1A1AL, // Raven Black
            0xFF8E959CL, // Pepper Gray
            0xFFFF5A00L, // Hugger / Solar Orange
            0xFF7C1E8AL, // In-Violet / Plum Crazy Purple
            0xFF00E5FFL, // Electric Nitro Cyan
            0xFF0055A5L, // French Racing Blue
            0xFFFFD700L, // Sunburst Gold / Papaya
            0xFF2E7D32L  // Sublime Green
        )
    }
}

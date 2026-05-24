package com.evan.brightnesscurve.data

object DefaultPresets {
    val comfort20Points = listOf(
        BrightnessPoint(0f, 8f),
        BrightnessPoint(5f, 12f),
        BrightnessPoint(50f, 18f),
        BrightnessPoint(100f, 20f),
        BrightnessPoint(500f, 20f),
        BrightnessPoint(1000f, 35f),
        BrightnessPoint(5000f, 65f),
        BrightnessPoint(10000f, 90f),
        BrightnessPoint(20000f, 100f)
    )

    private val balancedPoints = listOf(
        BrightnessPoint(0f, 10f),
        BrightnessPoint(10f, 16f),
        BrightnessPoint(80f, 24f),
        BrightnessPoint(300f, 32f),
        BrightnessPoint(1000f, 48f),
        BrightnessPoint(7000f, 78f),
        BrightnessPoint(20000f, 100f)
    )

    private val outdoorPoints = listOf(
        BrightnessPoint(0f, 12f),
        BrightnessPoint(20f, 20f),
        BrightnessPoint(100f, 30f),
        BrightnessPoint(800f, 48f),
        BrightnessPoint(3000f, 70f),
        BrightnessPoint(8000f, 92f),
        BrightnessPoint(16000f, 100f)
    )

    fun builtIns(now: Long): List<BrightnessPresetEntity> =
        listOf(
            entity(
                name = "护眼室内 20%",
                points = comfort20Points,
                isActive = true,
                now = now
            ),
            entity(
                name = "均衡",
                points = balancedPoints,
                isActive = false,
                now = now
            ),
            entity(
                name = "室外优先",
                points = outdoorPoints,
                isActive = false,
                now = now
            )
        )

    private fun entity(
        name: String,
        points: List<BrightnessPoint>,
        isActive: Boolean,
        now: Long
    ): BrightnessPresetEntity =
        BrightnessPresetEntity(
            name = name,
            isBuiltIn = true,
            isActive = isActive,
            pointsJson = PresetCodec.encodePoints(points),
            smoothingLevel = 0.35f,
            maxChangePerUpdate = 8f,
            minUpdateDelta = 3f,
            createdAt = now,
            updatedAt = now
        )
}

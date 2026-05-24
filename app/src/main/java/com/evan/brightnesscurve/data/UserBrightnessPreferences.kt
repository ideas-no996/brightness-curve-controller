package com.evan.brightnesscurve.data

enum class ResponseSpeed(val label: String, val alpha: Float, val brightenStep: Float, val darkenStep: Float) {
    Gentle("柔和", alpha = 0.25f, brightenStep = 2.5f, darkenStep = 2f),
    Standard("标准", alpha = 0.35f, brightenStep = 4f, darkenStep = 3f),
    Fast("快速", alpha = 0.45f, brightenStep = 6f, darkenStep = 4f);

    companion object {
        fun fromStored(value: String?): ResponseSpeed =
            entries.firstOrNull { it.name == value } ?: Standard
    }
}

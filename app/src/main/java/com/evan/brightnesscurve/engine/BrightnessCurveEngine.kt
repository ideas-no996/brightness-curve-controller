package com.evan.brightnesscurve.engine

import com.evan.brightnesscurve.brightness.BrightnessMapping
import com.evan.brightnesscurve.data.BrightnessPoint
import com.evan.brightnesscurve.data.ResponseSpeed
import kotlin.math.abs
import kotlin.math.roundToInt

enum class NoWriteReason {
    Throttled,
    ChangeTooSmall
}

data class BrightnessCurveDecision(
    val rawLux: Float,
    val smoothedLux: Float,
    val mappedPercent: Float,
    val targetPercent: Float,
    val canWriteByTime: Boolean,
    val shouldWrite: Boolean,
    val noWriteReason: NoWriteReason?
)

class BrightnessCurveEngine(
    private val minWriteIntervalMillis: Long = 220L,
    private val minPercentDelta: Float = 1f,
    private val minSystemDelta: Int = 2,
    private val fastAlpha: Float = 0.65f,
    private val sharpChangeRatio: Float = 0.6f,
    private val sharpChangeLux: Float = 80f
) {
    private var smoothedLux: Float? = null

    fun reset() {
        smoothedLux = null
    }

    fun currentSmoothedLux(): Float? = smoothedLux

    fun observeLux(rawLux: Float, responseSpeed: ResponseSpeed): Float {
        val previous = smoothedLux
        val next = if (previous == null) {
            rawLux
        } else {
            val alpha = if (isSharpChange(previous, rawLux)) fastAlpha else responseSpeed.alpha
            previous + (rawLux - previous) * alpha.coerceIn(0.05f, 0.95f)
        }
        smoothedLux = next
        return next
    }

    fun decide(
        rawLux: Float,
        points: List<BrightnessPoint>,
        minPercent: Float,
        maxPercent: Float,
        responseSpeed: ResponseSpeed,
        lastWrittenPercent: Float?,
        nowElapsedMillis: Long,
        lastWriteElapsedMillis: Long
    ): BrightnessCurveDecision {
        val smoothedLux = observeLux(rawLux, responseSpeed)
        val mappedPercent = BrightnessMapping.targetPercent(
            lux = smoothedLux,
            points = points,
            minPercent = minPercent,
            maxPercent = maxPercent
        )
        val targetPercent = rampTarget(
            previousPercent = lastWrittenPercent,
            mappedPercent = mappedPercent,
            responseSpeed = responseSpeed
        )
        val rampShouldWrite = shouldWriteByRamp(lastWrittenPercent, targetPercent)
        val canWriteByTime = nowElapsedMillis - lastWriteElapsedMillis >= minWriteIntervalMillis
        val shouldWrite = rampShouldWrite && canWriteByTime
        val noWriteReason = when {
            shouldWrite -> null
            !canWriteByTime -> NoWriteReason.Throttled
            else -> NoWriteReason.ChangeTooSmall
        }

        return BrightnessCurveDecision(
            rawLux = rawLux,
            smoothedLux = smoothedLux,
            mappedPercent = mappedPercent,
            targetPercent = targetPercent,
            canWriteByTime = canWriteByTime,
            shouldWrite = shouldWrite,
            noWriteReason = noWriteReason
        )
    }

    private fun rampTarget(
        previousPercent: Float?,
        mappedPercent: Float,
        responseSpeed: ResponseSpeed
    ): Float {
        val previous = previousPercent ?: return mappedPercent
        val delta = mappedPercent - previous
        val step = if (delta > 0f) responseSpeed.brightenStep else responseSpeed.darkenStep
        return previous + delta.coerceIn(-step, step)
    }

    private fun shouldWriteByRamp(previousPercent: Float?, targetPercent: Float): Boolean {
        val previous = previousPercent ?: return true
        val systemDelta = abs(percentToSystemValue(targetPercent) - percentToSystemValue(previous))
        return abs(targetPercent - previous) >= minPercentDelta && systemDelta >= minSystemDelta
    }

    private fun isSharpChange(previous: Float, rawLux: Float): Boolean {
        val delta = abs(rawLux - previous)
        val ratio = delta / previous.coerceAtLeast(1f)
        return delta >= sharpChangeLux || ratio >= sharpChangeRatio
    }

    private fun percentToSystemValue(percent: Float): Int =
        (percent.coerceIn(1f, 100f) / 100f * MAX_SYSTEM_BRIGHTNESS)
            .roundToInt()
            .coerceIn(MIN_SYSTEM_BRIGHTNESS, MAX_SYSTEM_BRIGHTNESS)

    private companion object {
        const val MIN_SYSTEM_BRIGHTNESS = 1
        const val MAX_SYSTEM_BRIGHTNESS = 255
    }
}

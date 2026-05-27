package com.evan.brightnesscurve.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

internal fun formatNullableBoolean(value: Boolean?): String =
    value?.toString() ?: "检测中"

internal fun formatBrightnessMode(value: Int?): String =
    when (value) {
        android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC -> "自动($value)"
        android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL -> "手动($value)"
        null -> "未知"
        else -> value.toString()
    }

internal fun formatTime(value: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(value))

internal fun formatReleaseTime(value: String?): String {
    return value
        ?.replace("T", " ")
        ?.removeSuffix("Z")
        ?: "未知"
}

internal fun formatFileSize(value: Long): String {
    if (value <= 0L) return "未知大小"
    val mb = value / 1024f / 1024f
    return "%.1f MB".format(mb)
}

package com.evan.brightnesscurve.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evan.brightnesscurve.service.RuntimeStatus

@Composable
internal fun HomeTab(
    state: MainUiState,
    padding: PaddingValues,
    onRefreshPermission: () -> Unit,
    onOpenWriteSettings: () -> Unit,
    onToggleService: (Boolean) -> Unit,
    onComfortPercentChange: (Float) -> Unit,
    onRetryLightSensor: () -> Unit,
    onQuickCalibrate: (Float) -> Unit,
    onCalibrate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            if (!state.canWriteSettings) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("需要授权修改系统设置", fontWeight = FontWeight.Bold)
                        Text("授权后，App 才能写入全局屏幕亮度。")
                        if (state.runtime.windowFallbackActive) {
                            Text("当前只能预览本 App 窗口亮度，不代表系统亮度已经被控制。")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onOpenWriteSettings) {
                                Text("去授权")
                            }
                            OutlinedButton(onClick = onRefreshPermission) {
                                Text("刷新")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(environmentTitle(state), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        brightnessFeeling(state.runtime.writtenPercent ?: state.runtime.targetPercent),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(state.activePreset?.name ?: "护眼室内", style = MaterialTheme.typography.titleMedium)
                            Text(controlStatus(state))
                        }
                        Switch(
                            checked = state.settings.autoControlEnabled,
                            onCheckedChange = onToggleService,
                            enabled = true
                        )
                    }
                    state.runtime.status.recommendedAction?.let {
                        Text(it, color = MaterialTheme.colorScheme.secondary)
                    }
                    if (state.runtime.status == RuntimeStatus.SensorTimeout) {
                        OutlinedButton(onClick = onRetryLightSensor, modifier = Modifier.fillMaxWidth()) {
                            Text("重试检测")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("快速调节", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onQuickCalibrate(6f) }, modifier = Modifier.weight(1f)) {
                            Text("太暗了")
                        }
                        Button(onClick = onCalibrate, modifier = Modifier.weight(1f)) {
                            Text("刚刚好")
                        }
                        OutlinedButton(onClick = { onQuickCalibrate(-6f) }, modifier = Modifier.weight(1f)) {
                            Text("太亮了")
                        }
                    }
                    Text("当前舒适亮度：${state.settings.lastComfortPercent.toInt()}%")
                    Slider(
                        value = state.settings.lastComfortPercent.coerceIn(
                            state.settings.minAllowedPercent,
                            state.settings.maxAllowedPercent
                        ),
                        onValueChange = onComfortPercentChange,
                        valueRange = state.settings.minAllowedPercent..state.settings.maxAllowedPercent
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("自动曲线", style = MaterialTheme.typography.titleMedium)
                    MetricRow("当前环境", environmentLabel(state.runtime.smoothedLux ?: state.runtime.rawLux))
                    MetricRow("目标亮度", state.runtime.targetPercent?.let { "%.0f%%".format(it) } ?: "等待环境光")
                    MetricRow("曲线状态", if (state.activePreset?.isBuiltIn == false) "已学习" else "默认曲线")
                    state.runtime.message?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
                }
            }
        }

        item {
            DebugInfoPanel(state)
        }
    }
}

@Composable
private fun DebugInfoPanel(state: MainUiState) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("调试信息", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "收起" else "展开")
                }
            }
            if (expanded) {
                MetricRow("当前预设", state.activePreset?.name ?: "未初始化")
                MetricRow("runtimeStatus", state.runtime.status.name)
                MetricRow("hasLightSensor", formatNullableBoolean(state.runtime.hasLightSensor))
                MetricRow("sensorName", state.runtime.lightSensorName ?: "-")
                MetricRow("sensorRegistered", state.runtime.lightSensorRegistered.toString())
                MetricRow("raw lux", state.runtime.rawLux?.let { "%.1f".format(it) } ?: "-")
                MetricRow("smoothed lux", state.runtime.smoothedLux?.let { "%.1f".format(it) } ?: "-")
                MetricRow("lastLuxUpdateTime", state.runtime.lastLuxUpdateTime?.let(::formatTime) ?: "-")
                MetricRow("lastAutoEvaluateTime", state.runtime.lastAutoEvaluateTime?.let(::formatTime) ?: "-")
                MetricRow("lastBrightnessWriteTime", state.runtime.lastBrightnessWriteTime?.let(::formatTime) ?: "-")
                MetricRow("autoDesired", state.settings.autoControlEnabled.toString())
                MetricRow("serviceRunning", state.runtime.isRunning.toString())
                MetricRow("target", state.runtime.targetPercent?.let { "%.0f%%".format(it) } ?: "-")
                MetricRow("targetSystemBrightness", state.runtime.targetSystemValue?.toString() ?: "-")
                MetricRow("decisionDeltaPercent", state.runtime.lastDecisionDeltaPercent?.let { "%.1f%%".format(it) } ?: "-")
                MetricRow("deadbandPercent", state.runtime.lastDecisionDeadbandPercent?.let { "%.1f%%".format(it) } ?: "-")
                MetricRow("throttleMs", state.runtime.lastDecisionThrottleMillis?.toString() ?: "-")
                MetricRow("written", state.runtime.writtenPercent?.let { "%.0f%%".format(it) } ?: "-")
                MetricRow("currentSystemBrightness", state.runtime.currentBrightnessValue?.toString() ?: "-")
                MetricRow("lastWriteTargetValue", state.runtime.lastWriteTargetValue?.toString() ?: "-")
                MetricRow("lastWriteReadBackValue", state.runtime.lastWriteReadBackValue?.toString() ?: "-")
                MetricRow("lastWriteSucceeded", state.runtime.lastWriteSucceeded?.toString() ?: "-")
                MetricRow("lastNoWriteReason", state.runtime.lastNoWriteReason ?: "-")
                MetricRow("applied", state.runtime.appliedBrightnessValue?.toString() ?: "-")
                MetricRow("canWrite", state.canWriteSettings.toString())
                MetricRow("brightnessMode", formatBrightnessMode(state.runtime.brightnessMode))
                MetricRow("failureReason", state.runtime.failureReason?.name ?: "-")
                MetricRow("lastError", state.runtime.lastError ?: "-")
                MetricRow("response", state.settings.responseSpeed.label)
            }
        }
    }
}

private fun environmentTitle(state: MainUiState): String {
    when (state.runtime.status) {
        RuntimeStatus.PermissionMissing,
        RuntimeStatus.NoSensor,
        RuntimeStatus.SensorTimeout,
        RuntimeStatus.WriteFailed,
        RuntimeStatus.PausedScreenOff -> return state.runtime.status.title
        else -> Unit
    }
    return environmentLabel(state.runtime.smoothedLux ?: state.runtime.rawLux)
}

private fun controlStatus(state: MainUiState): String {
    return state.runtime.message ?: state.runtime.status.description
}

private fun environmentLabel(lux: Float?): String {
    val value = lux ?: return "等待环境光"
    return when {
        value < 10f -> "夜间或暗光环境"
        value < 80f -> "室内偏暗"
        value < 500f -> "室内正常"
        value < 2_000f -> "明亮室内"
        else -> "强光环境"
    }
}

private fun brightnessFeeling(percent: Float?): String {
    val value = percent ?: return "正在等待第一次亮度判断"
    return when {
        value < 15f -> "当前亮度柔和"
        value < 45f -> "当前亮度舒适"
        value < 75f -> "当前亮度明亮"
        else -> "当前亮度偏户外"
    }
}

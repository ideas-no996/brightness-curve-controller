package com.evan.brightnesscurve.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.evan.brightnesscurve.data.ResponseSpeed
import com.evan.brightnesscurve.service.RuntimeStatus

@Composable
internal fun SettingsTab(
    state: MainUiState,
    padding: PaddingValues,
    onRefreshPermission: () -> Unit,
    onOpenWriteSettings: () -> Unit,
    onToggleStartOnBoot: (Boolean) -> Unit,
    onToggleOutdoorFull: (Boolean) -> Unit,
    onMinAllowedChange: (Float) -> Unit,
    onMaxAllowedChange: (Float) -> Unit,
    onResponseSpeedChange: (ResponseSpeed) -> Unit,
    onTutorialStartupChange: (Boolean) -> Unit,
    onReviewTutorial: () -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenInstallPermission: () -> Unit,
    onRefreshInstallPermission: () -> Unit,
    onRetryLightSensor: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("权限", style = MaterialTheme.typography.titleMedium)
                    Text(if (state.canWriteSettings) "已允许修改系统亮度" else "还不能写入系统亮度")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpenWriteSettings) { Text("系统授权") }
                        OutlinedButton(onClick = onRefreshPermission) { Text("刷新") }
                    }
                }
            }
        }

        item {
            DiagnosticsPanel(state = state, onRetryLightSensor = onRetryLightSensor)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("亮度边界", style = MaterialTheme.typography.titleMedium)
                    Text("最低允许亮度：${state.settings.minAllowedPercent.toInt()}%")
                    Slider(
                        value = state.settings.minAllowedPercent,
                        onValueChange = onMinAllowedChange,
                        valueRange = 1f..state.settings.maxAllowedPercent
                    )
                    Text("最高允许亮度：${state.settings.maxAllowedPercent.toInt()}%")
                    Slider(
                        value = state.settings.maxAllowedPercent,
                        onValueChange = onMaxAllowedChange,
                        valueRange = state.settings.minAllowedPercent..100f
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("响应速度", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResponseSpeed.entries.forEach { speed ->
                            FilterChip(
                                selected = state.settings.responseSpeed == speed,
                                onClick = { onResponseSpeedChange(speed) },
                                label = { Text(speed.label) }
                            )
                        }
                    }
                    SettingSwitchRow("开机后自动恢复控制", state.settings.startOnBoot, onToggleStartOnBoot)
                    SettingSwitchRow("室外允许拉到满亮", state.settings.allowOutdoorFull, onToggleOutdoorFull)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val update = state.updateState
                    val latest = update.latest

                    Text("软件更新", style = MaterialTheme.typography.titleMedium)
                    Text("联网仅用于手动检查 GitHub Release、下载并校验 APK；亮度控制本身离线工作，安装更新也必须经过 Android 系统确认。")
                    MetricRow("当前版本", update.currentVersionName)
                    MetricRow("更新状态", update.statusText)
                    update.lastCheckedAt?.let {
                        MetricRow("上次检查", formatTime(it))
                    }

                    if (latest != null) {
                        MetricRow("最新版本", latest.versionName)
                        MetricRow("发布时间", formatReleaseTime(latest.publishedAt))
                        MetricRow("安装包", formatFileSize(latest.apkSizeBytes))
                        MetricRow("APK 校验", if (latest.apkSha256 == null) "缺失" else "SHA-256")
                    }

                    if (update.isDownloading) {
                        LinearProgressIndicator(
                            progress = {
                                ((update.downloadProgressPercent ?: 0) / 100f).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("正在下载：${update.downloadProgressPercent ?: 0}%")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onCheckUpdate,
                            enabled = !update.isChecking && !update.isDownloading
                        ) {
                            Text(if (update.isChecking) "检查中" else "检查更新")
                        }

                        if (latest != null && !update.isDownloaded) {
                            Button(
                                onClick = onDownloadUpdate,
                                enabled = !update.isChecking && !update.isDownloading
                            ) {
                                Text("下载")
                            }
                        }

                        if (update.isDownloaded) {
                            Button(
                                onClick = onInstallUpdate,
                                enabled = !update.isChecking && !update.isDownloading
                            ) {
                                Text("安装")
                            }
                        }
                    }

                    if (!update.canInstallPackages && latest != null) {
                        Text("安装更新前，需要允许此 App 安装下载的 APK。")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onOpenInstallPermission) {
                                Text("允许安装来源")
                            }
                            TextButton(onClick = onRefreshInstallPermission) {
                                Text("刷新")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("教程", style = MaterialTheme.typography.titleMedium)
                    SettingSwitchRow(
                        "启动时显示教程",
                        state.settings.showTutorialOnStartup,
                        onTutorialStartupChange
                    )
                    OutlinedButton(onClick = onReviewTutorial, modifier = Modifier.fillMaxWidth()) {
                        Text("重新查看教程")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DiagnosticsPanel(state: MainUiState, onRetryLightSensor: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("诊断", style = MaterialTheme.typography.titleMedium)
            MetricRow("runtimeStatus", state.runtime.status.name)
            MetricRow("hasLightSensor", formatNullableBoolean(state.runtime.hasLightSensor))
            MetricRow("sensorRegistered", state.runtime.lightSensorRegistered.toString())
            MetricRow("sensorName", state.runtime.lightSensorName ?: "-")
            MetricRow("lastLux", state.runtime.lastLux?.let { "%.1f lux".format(it) } ?: "-")
            MetricRow("lastLuxUpdateTime", state.runtime.lastLuxUpdateTime?.let(::formatTime) ?: "-")
            MetricRow("autoDesired", state.settings.autoControlEnabled.toString())
            MetricRow("serviceRunning", state.runtime.isRunning.toString())
            MetricRow("currentSystemBrightness", state.runtime.currentBrightnessValue?.toString() ?: "-")
            MetricRow("currentBrightnessPercent", formatSystemBrightnessPercent(state.runtime.currentBrightnessValue))
            MetricRow("targetBrightnessPercent", state.runtime.targetPercent?.let { "%.0f%%".format(it) } ?: "-")
            MetricRow("targetSystemBrightness", state.runtime.targetSystemValue?.toString() ?: "-")
            MetricRow("appliedBrightnessValue", state.runtime.appliedBrightnessValue?.toString() ?: "-")
            MetricRow("lastWriteTargetValue", state.runtime.lastWriteTargetValue?.toString() ?: "-")
            MetricRow("lastWriteReadBackValue", state.runtime.lastWriteReadBackValue?.toString() ?: "-")
            MetricRow("lastWriteSucceeded", state.runtime.lastWriteSucceeded?.toString() ?: "-")
            MetricRow("lastNoWriteReason", state.runtime.lastNoWriteReason ?: "-")
            MetricRow("canWriteSettings", state.canWriteSettings.toString())
            MetricRow("brightnessMode", formatBrightnessMode(state.runtime.brightnessMode))
            MetricRow("failureReason", state.runtime.failureReason?.name ?: "-")
            MetricRow("lastError", state.runtime.lastError ?: "-")
            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(buildDiagnosticReport(state)))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("复制诊断报告")
            }
            if (state.runtime.status == RuntimeStatus.SensorTimeout) {
                OutlinedButton(onClick = onRetryLightSensor, modifier = Modifier.fillMaxWidth()) {
                    Text("重试检测")
                }
            }
        }
    }
}

private fun formatSystemBrightnessPercent(value: Int?): String {
    val brightness = value ?: return "-"
    return "%.0f%%".format(brightness.coerceIn(0, 255) / 255f * 100f)
}

package com.evan.brightnesscurve.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.evan.brightnesscurve.data.BrightnessPoint
import com.evan.brightnesscurve.data.BrightnessPreset
import com.evan.brightnesscurve.data.BrightnessRevisionEntity
import com.evan.brightnesscurve.data.ResponseSpeed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    onRefreshPermission: () -> Unit,
    onOpenWriteSettings: () -> Unit,
    onToggleService: (Boolean) -> Unit,
    onToggleStartOnBoot: (Boolean) -> Unit,
    onToggleOutdoorFull: (Boolean) -> Unit,
    onComfortPercentChange: (Float) -> Unit,
    onMinAllowedChange: (Float) -> Unit,
    onMaxAllowedChange: (Float) -> Unit,
    onResponseSpeedChange: (ResponseSpeed) -> Unit,
    onTutorialStartupChange: (Boolean) -> Unit,
    onFinishTutorial: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenInstallPermission: () -> Unit,
    onRefreshInstallPermission: () -> Unit,
    onRetryLightSensor: () -> Unit,
    onQuickCalibrate: (Float) -> Unit,
    onCalibrate: () -> Unit,
    onActivatePreset: (Long) -> Unit,
    onCopyPreset: (Long) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onRenamePreset: (Long, String) -> Unit,
    onSelectEditorPreset: (Long) -> Unit,
    onSavePreset: (Long, List<BrightnessPoint>, Float, Float, Float) -> Unit,
    onRestoreRevision: (Long) -> Unit,
    onDismissMessage: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var tutorialVisible by rememberSaveable { mutableStateOf(false) }
    var tutorialAutoChecked by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        scope.launch { snackbarHostState.showSnackbar(message) }
        onDismissMessage()
    }

    LaunchedEffect(state.settings.hasSeenTutorial, state.settings.showTutorialOnStartup) {
        if (!tutorialAutoChecked) {
            tutorialAutoChecked = true
            tutorialVisible = state.settings.showTutorialOnStartup || !state.settings.hasSeenTutorial
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("亮度曲线控制器") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                listOf("首页", "预设", "曲线", "设置").forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {},
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeTab(
                state = state,
                padding = padding,
                onRefreshPermission = onRefreshPermission,
                onOpenWriteSettings = onOpenWriteSettings,
                onToggleService = onToggleService,
                onComfortPercentChange = onComfortPercentChange,
                onRetryLightSensor = onRetryLightSensor,
                onQuickCalibrate = onQuickCalibrate,
                onCalibrate = onCalibrate
            )
            1 -> PresetsTab(
                state = state,
                padding = padding,
                onActivatePreset = onActivatePreset,
                onCopyPreset = onCopyPreset,
                onDeletePreset = onDeletePreset,
                onRenamePreset = onRenamePreset,
                onSelectEditorPreset = {
                    onSelectEditorPreset(it)
                    selectedTab = 2
                }
            )
            2 -> EditorTab(
                state = state,
                padding = padding,
                onCopyPreset = onCopyPreset,
                onSavePreset = onSavePreset,
                onRestoreRevision = onRestoreRevision
            )
            3 -> SettingsTab(
                state = state,
                padding = padding,
                onRefreshPermission = onRefreshPermission,
                onOpenWriteSettings = onOpenWriteSettings,
                onToggleStartOnBoot = onToggleStartOnBoot,
                onToggleOutdoorFull = onToggleOutdoorFull,
                onMinAllowedChange = onMinAllowedChange,
                onMaxAllowedChange = onMaxAllowedChange,
                onResponseSpeedChange = onResponseSpeedChange,
                onTutorialStartupChange = onTutorialStartupChange,
                onReviewTutorial = { tutorialVisible = true },
                onCheckUpdate = onCheckUpdate,
                onDownloadUpdate = onDownloadUpdate,
                onInstallUpdate = onInstallUpdate,
                onOpenInstallPermission = onOpenInstallPermission,
                onRefreshInstallPermission = onRefreshInstallPermission,
                onRetryLightSensor = onRetryLightSensor
            )
        }
    }

    if (tutorialVisible) {
        TutorialOverlay(
            onSkip = {
                tutorialVisible = false
                onFinishTutorial(false)
            },
            onFinish = { showOnStartup ->
                tutorialVisible = false
                onFinishTutorial(showOnStartup)
            }
        )
    }
}

@Composable
private fun HomeTab(
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
                    Text(brightnessFeeling(state.runtime.writtenPercent ?: state.runtime.targetPercent), color = MaterialTheme.colorScheme.secondary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(state.activePreset?.name ?: "护眼室内", style = MaterialTheme.typography.titleMedium)
                            Text(controlStatus(state))
                        }
                        Switch(
                            checked = state.settings.serviceEnabled || state.runtime.isRunning,
                            onCheckedChange = onToggleService,
                            enabled = true
                        )
                    }
                    if (state.runtime.lightSensorTimedOut) {
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
private fun SettingsTab(
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
                    MetricRow("当前版本", update.currentVersionName)
                    MetricRow("更新状态", update.statusText)
                    update.lastCheckedAt?.let {
                        MetricRow("上次检查", formatTime(it))
                    }

                    if (latest != null) {
                        MetricRow("最新版本", latest.versionName)
                        MetricRow("发布时间", formatReleaseTime(latest.publishedAt))
                        MetricRow("安装包", formatFileSize(latest.apkSizeBytes))
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
private fun PresetsTab(
    state: MainUiState,
    padding: PaddingValues,
    onActivatePreset: (Long) -> Unit,
    onCopyPreset: (Long) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onRenamePreset: (Long, String) -> Unit,
    onSelectEditorPreset: (Long) -> Unit
) {
    var renameTarget by remember { mutableStateOf<BrightnessPreset?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.presets, key = { it.id }) { preset ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(preset.name, style = MaterialTheme.typography.titleMedium)
                            Text(if (preset.isBuiltIn) "内置预设" else "自定义预设")
                        }
                        if (preset.isActive) {
                            FilterChip(selected = true, onClick = {}, label = { Text("启用中") })
                        }
                    }
                    Text("${preset.points.size} 个控制点 · 室内点约 ${preset.points.firstOrNull { it.lux >= 100f }?.brightnessPercent?.toInt() ?: "-"}%")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onSelectEditorPreset(preset.id) }) { Text("编辑") }
                        OutlinedButton(onClick = { onCopyPreset(preset.id) }) { Text("复制") }
                        if (!preset.isActive) {
                            Button(onClick = { onActivatePreset(preset.id) }) { Text("启用") }
                        }
                    }
                    if (!preset.isBuiltIn) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { renameTarget = preset }) { Text("重命名") }
                            TextButton(onClick = { onDeletePreset(preset.id) }) { Text("删除") }
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { preset ->
        RenameDialog(
            currentName = preset.name,
            onDismiss = { renameTarget = null },
            onSave = {
                onRenamePreset(preset.id, it)
                renameTarget = null
            }
        )
    }
}

@Composable
private fun EditorTab(
    state: MainUiState,
    padding: PaddingValues,
    onCopyPreset: (Long) -> Unit,
    onSavePreset: (Long, List<BrightnessPoint>, Float, Float, Float) -> Unit,
    onRestoreRevision: (Long) -> Unit
) {
    val preset = state.editorPreset

    if (preset == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("还没有可编辑的预设")
        }
        return
    }

    val draftPoints = remember(preset.id, preset.updatedAt) {
        mutableStateListOf(*preset.points.map { DraftPoint(it.lux.toString(), it.brightnessPercent) }.toTypedArray())
    }
    var smoothing by remember(preset.id, preset.updatedAt) { mutableFloatStateOf(preset.smoothingLevel) }
    var maxChange by remember(preset.id, preset.updatedAt) { mutableFloatStateOf(preset.maxChangePerUpdate) }
    var minDelta by remember(preset.id, preset.updatedAt) { mutableFloatStateOf(preset.minUpdateDelta) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(preset.name, style = MaterialTheme.typography.titleMedium)
                    if (preset.isBuiltIn) {
                        Text("内置预设不能直接修改。复制后会生成自定义版本。")
                        Button(onClick = { onCopyPreset(preset.id) }) { Text("复制为自定义预设") }
                    } else {
                        Text("修改控制点后保存，会自动生成一个可回滚版本。")
                    }
                }
            }
        }

        items(draftPoints.size, key = { index -> "$index-${draftPoints[index].luxText}" }) { index ->
            val point = draftPoints[index]
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = point.luxText,
                        onValueChange = { draftPoints[index] = point.copy(luxText = it) },
                        label = { Text("环境光 lux") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !preset.isBuiltIn
                    )
                    Text("亮度：${point.brightnessPercent.toInt()}%")
                    Slider(
                        value = point.brightnessPercent,
                        onValueChange = { draftPoints[index] = point.copy(brightnessPercent = it) },
                        valueRange = 1f..100f,
                        enabled = !preset.isBuiltIn
                    )
                    if (!preset.isBuiltIn && draftPoints.size > 3) {
                        TextButton(onClick = { draftPoints.removeAt(index) }) { Text("删除这个点") }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("变化策略", style = MaterialTheme.typography.titleMedium)
                    Text("平滑强度：${"%.2f".format(smoothing)}")
                    Slider(value = smoothing, onValueChange = { smoothing = it }, valueRange = 0.05f..0.95f, enabled = !preset.isBuiltIn)
                    Text("每次最多变化：${maxChange.toInt()}%")
                    Slider(value = maxChange, onValueChange = { maxChange = it }, valueRange = 1f..30f, enabled = !preset.isBuiltIn)
                    Text("最小更新差值：${minDelta.toInt()}%")
                    Slider(value = minDelta, onValueChange = { minDelta = it }, valueRange = 1f..20f, enabled = !preset.isBuiltIn)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    enabled = !preset.isBuiltIn,
                    onClick = {
                        val nextLux = draftPoints.lastOrNull()?.luxText?.toFloatOrNull()?.let { it * 2f } ?: 100f
                        draftPoints.add(DraftPoint(nextLux.toString(), 20f))
                    }
                ) {
                    Text("新增点")
                }
                Button(
                    enabled = !preset.isBuiltIn,
                    onClick = {
                        val points = draftPoints.mapNotNull {
                            val lux = it.luxText.toFloatOrNull()
                            if (lux == null) null else BrightnessPoint(lux, it.brightnessPercent)
                        }
                        onSavePreset(preset.id, points, smoothing, maxChange, minDelta)
                    }
                ) {
                    Text("保存曲线")
                }
            }
        }

        item {
            RevisionList(revisions = state.revisions, onRestoreRevision = onRestoreRevision)
        }
    }
}

@Composable
private fun RevisionList(
    revisions: List<BrightnessRevisionEntity>,
    onRestoreRevision: (Long) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("历史版本", style = MaterialTheme.typography.titleMedium)
            if (revisions.isEmpty()) {
                Text("保存或校准后会在这里出现可回滚版本。")
            } else {
                revisions.take(8).forEach { revision ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(formatTime(revision.createdAt), fontWeight = FontWeight.Bold)
                            Text(revision.note)
                        }
                        TextButton(onClick = { onRestoreRevision(revision.id) }) {
                            Text("恢复")
                        }
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("诊断", style = MaterialTheme.typography.titleMedium)
            MetricRow("hasLightSensor", formatNullableBoolean(state.runtime.hasLightSensor))
            MetricRow("sensorRegistered", state.runtime.lightSensorRegistered.toString())
            MetricRow("sensorName", state.runtime.lightSensorName ?: "-")
            MetricRow("lastLux", state.runtime.lastLux?.let { "%.1f lux".format(it) } ?: "-")
            MetricRow("lastLuxUpdateTime", state.runtime.lastLuxUpdateTime?.let(::formatTime) ?: "-")
            MetricRow("autoEnabled", (state.settings.serviceEnabled || state.runtime.autoEnabled).toString())
            MetricRow("targetBrightnessPercent", state.runtime.targetPercent?.let { "%.0f%%".format(it) } ?: "-")
            MetricRow("appliedBrightnessValue", state.runtime.appliedBrightnessValue?.toString() ?: "-")
            MetricRow("canWriteSettings", state.canWriteSettings.toString())
            MetricRow("brightnessMode", formatBrightnessMode(state.runtime.brightnessMode))
            MetricRow("lastError", state.runtime.lastError ?: "-")
            if (state.runtime.lightSensorTimedOut) {
                OutlinedButton(onClick = onRetryLightSensor, modifier = Modifier.fillMaxWidth()) {
                    Text("重试检测")
                }
            }
        }
    }
}

@Composable
private fun TutorialOverlay(
    onSkip: () -> Unit,
    onFinish: (Boolean) -> Unit
) {
    val steps = remember {
        listOf(
            TutorialStep(
                title = "跟着光线走",
                body = "环境变亮或变暗时，我会帮你调整屏幕亮度。"
            ),
            TutorialStep(
                title = "告诉我你的感觉",
                body = "觉得太暗、刚刚好或太亮，点一下就能校准。"
            ),
            TutorialStep(
                title = "慢慢变成你的曲线",
                body = "每次校准都会让亮度更贴近你的习惯。"
            ),
            TutorialStep(
                title = "随时交还给你",
                body = "你可以关掉自动控制，也能在设置里重看教程。"
            )
        )
    }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[stepIndex]
    val isLastStep = stepIndex == steps.lastIndex

    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(step.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(step.body)
                Text(
                    text = "第 ${stepIndex + 1} / ${steps.size} 步",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            if (isLastStep) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onFinish(true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("以后每次打开都显示")
                    }
                    OutlinedButton(
                        onClick = { onFinish(false) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("以后不再自动显示")
                    }
                }
            } else {
                Button(onClick = { stepIndex += 1 }) {
                    Text("下一步")
                }
            }
        },
        dismissButton = {
            if (!isLastStep) {
                TextButton(onClick = onSkip) {
                    Text("跳过")
                }
            }
        }
    )
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = FontWeight.Bold)
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
                MetricRow("hasLightSensor", formatNullableBoolean(state.runtime.hasLightSensor))
                MetricRow("sensorName", state.runtime.lightSensorName ?: "-")
                MetricRow("sensorRegistered", state.runtime.lightSensorRegistered.toString())
                MetricRow("raw lux", state.runtime.rawLux?.let { "%.1f".format(it) } ?: "-")
                MetricRow("smoothed lux", state.runtime.smoothedLux?.let { "%.1f".format(it) } ?: "-")
                MetricRow("lastLuxUpdateTime", state.runtime.lastLuxUpdateTime?.let(::formatTime) ?: "-")
                MetricRow("autoEnabled", (state.settings.serviceEnabled || state.runtime.autoEnabled).toString())
                MetricRow("target", state.runtime.targetPercent?.let { "%.0f%%".format(it) } ?: "-")
                MetricRow("written", state.runtime.writtenPercent?.let { "%.0f%%".format(it) } ?: "-")
                MetricRow("applied", state.runtime.appliedBrightnessValue?.toString() ?: "-")
                MetricRow("canWrite", state.canWriteSettings.toString())
                MetricRow("brightnessMode", formatBrightnessMode(state.runtime.brightnessMode))
                MetricRow("lastError", state.runtime.lastError ?: "-")
                MetricRow("response", state.settings.responseSpeed.label)
            }
        }
    }
}

private fun environmentTitle(state: MainUiState): String {
    if (state.runtime.lightSensorTimedOut) return "未收到环境光数据"
    return environmentLabel(state.runtime.smoothedLux ?: state.runtime.rawLux)
}

private fun controlStatus(state: MainUiState): String {
    return when {
        state.runtime.isRunning -> "正在自动照顾屏幕亮度"
        state.runtime.lastLux != null -> "已读取环境光，但未自动调节"
        state.runtime.lightSensorTimedOut -> "未收到环境光，建议重试检测"
        else -> "正在读取环境光"
    }
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

private fun formatNullableBoolean(value: Boolean?): String =
    value?.toString() ?: "检测中"

private fun formatBrightnessMode(value: Int?): String =
    when (value) {
        android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC -> "自动($value)"
        android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL -> "手动($value)"
        null -> "未知"
        else -> value.toString()
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

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名预设") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onSave(name) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private data class DraftPoint(
    val luxText: String,
    val brightnessPercent: Float
)

private data class TutorialStep(
    val title: String,
    val body: String
)

private fun formatTime(value: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(value))

private fun formatReleaseTime(value: String?): String {
    return value
        ?.replace("T", " ")
        ?.removeSuffix("Z")
        ?: "未知"
}

private fun formatFileSize(value: Long): String {
    if (value <= 0L) return "未知大小"
    val mb = value / 1024f / 1024f
    return "%.1f MB".format(mb)
}

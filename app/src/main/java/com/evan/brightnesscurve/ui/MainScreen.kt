package com.evan.brightnesscurve.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.evan.brightnesscurve.data.BrightnessPoint
import com.evan.brightnesscurve.data.ResponseSpeed
import kotlinx.coroutines.launch

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

    LaunchedEffect(
        state.settingsLoaded,
        state.settings.hasSeenTutorial,
        state.settings.showTutorialOnStartup
    ) {
        if (state.settingsLoaded && !tutorialAutoChecked) {
            tutorialAutoChecked = true
            tutorialVisible = shouldAutoShowTutorial(
                settingsLoaded = state.settingsLoaded,
                hasSeenTutorial = state.settings.hasSeenTutorial,
                showTutorialOnStartup = state.settings.showTutorialOnStartup
            )
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

internal fun shouldAutoShowTutorial(
    settingsLoaded: Boolean,
    hasSeenTutorial: Boolean,
    showTutorialOnStartup: Boolean
): Boolean = settingsLoaded && (showTutorialOnStartup || !hasSeenTutorial)

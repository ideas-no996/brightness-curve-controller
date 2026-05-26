package com.evan.brightnesscurve

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evan.brightnesscurve.system.BrightnessSettings
import com.evan.brightnesscurve.ui.BrightnessAppTheme
import com.evan.brightnesscurve.ui.MainScreen
import com.evan.brightnesscurve.ui.MainViewModel
import com.evan.brightnesscurve.ui.MainViewModelFactory
import com.evan.brightnesscurve.update.UpdateInstaller

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(application as BrightnessCurveApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val writeSettingsLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                viewModel.refreshWritePermission()
            }
            val installPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                viewModel.refreshInstallPermission()
            }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {}

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            BrightnessAppTheme {
                MainScreen(
                    state = uiState,
                    onRefreshPermission = viewModel::refreshWritePermission,
                    onOpenWriteSettings = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_WRITE_SETTINGS,
                            Uri.parse("package:$packageName")
                        )
                        writeSettingsLauncher.launch(intent)
                    },
                    onToggleService = { enabled ->
                        if (enabled && !BrightnessSettings.canWrite(context)) {
                            viewModel.refreshWritePermission()
                            val intent = Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:$packageName")
                            )
                            writeSettingsLauncher.launch(intent)
                        } else {
                            viewModel.setServiceEnabled(enabled)
                        }
                    },
                    onToggleStartOnBoot = viewModel::setStartOnBoot,
                    onToggleOutdoorFull = viewModel::setAllowOutdoorFull,
                    onComfortPercentChange = viewModel::setComfortPercent,
                    onMinAllowedChange = viewModel::setMinAllowedPercent,
                    onMaxAllowedChange = viewModel::setMaxAllowedPercent,
                    onResponseSpeedChange = viewModel::setResponseSpeed,
                    onTutorialStartupChange = viewModel::setShowTutorialOnStartup,
                    onFinishTutorial = viewModel::finishTutorial,
                    onCheckUpdate = viewModel::checkForUpdate,
                    onDownloadUpdate = viewModel::downloadLatestUpdate,
                    onInstallUpdate = {
                        if (!UpdateInstaller.canRequestPackageInstalls(context)) {
                            viewModel.refreshInstallPermission()
                            installPermissionLauncher.launch(UpdateInstaller.unknownAppSourcesIntent(context))
                        } else {
                            viewModel.installDownloadedUpdate()
                        }
                    },
                    onOpenInstallPermission = {
                        installPermissionLauncher.launch(UpdateInstaller.unknownAppSourcesIntent(context))
                    },
                    onRefreshInstallPermission = viewModel::refreshInstallPermission,
                    onQuickCalibrate = viewModel::quickCalibrate,
                    onCalibrate = viewModel::calibrateCurrentEnvironment,
                    onActivatePreset = viewModel::activatePreset,
                    onCopyPreset = viewModel::copyPreset,
                    onDeletePreset = viewModel::deletePreset,
                    onRenamePreset = viewModel::renamePreset,
                    onSelectEditorPreset = viewModel::selectEditorPreset,
                    onSavePreset = viewModel::savePresetDraft,
                    onRestoreRevision = viewModel::restoreRevision,
                    onDismissMessage = viewModel::dismissMessage
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshWritePermission()
        viewModel.refreshInstallPermission()
    }
}

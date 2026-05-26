package com.evan.brightnesscurve.update

data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val publishedAt: String?,
    val releaseUrl: String,
    val apkName: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long
)

data class AppUpdateState(
    val currentVersionName: String,
    val latest: UpdateInfo? = null,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int? = null,
    val downloadedApkPath: String? = null,
    val canInstallPackages: Boolean = true,
    val lastCheckedAt: Long? = null,
    val statusText: String = "还没有检查更新"
) {
    val hasUpdate: Boolean
        get() = latest != null

    val isDownloaded: Boolean
        get() = downloadedApkPath != null
}


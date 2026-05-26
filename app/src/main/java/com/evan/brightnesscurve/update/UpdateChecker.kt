package com.evan.brightnesscurve.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class UpdateChecker(
    private val latestReleaseUrl: String = "https://api.github.com/repos/ideas-no996/brightness-curve-controller/releases/latest"
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkLatest(currentVersionName: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val release = fetchLatestRelease()
        val asset = release.assets.firstOrNull { asset ->
            asset.name.startsWith("BrightnessCurveController-") && asset.name.endsWith(".apk")
        } ?: release.assets.firstOrNull { it.name.endsWith(".apk") }
            ?: error("最新 Release 里没有找到可下载的 APK")

        val remoteVersion = release.tagName.removePrefix("v").removePrefix("V")
        if (!VersionComparator.isRemoteNewer(currentVersionName, remoteVersion)) return@withContext null

        UpdateInfo(
            tagName = release.tagName,
            versionName = remoteVersion,
            publishedAt = release.publishedAt,
            releaseUrl = release.htmlUrl,
            apkName = asset.name,
            apkDownloadUrl = asset.downloadUrl,
            apkSizeBytes = asset.size
        )
    }

    private fun fetchLatestRelease(): GitHubReleaseResponse {
        val connection = (URL(latestReleaseUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "BrightnessCurveController")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }

        return connection.use {
            val responseCode = it.responseCode
            if (responseCode == HttpURLConnection.HTTP_FORBIDDEN || responseCode == 429) {
                error("GitHub 暂时限制了请求，请稍后再试")
            }
            if (responseCode !in 200..299) {
                error("检查更新失败，HTTP $responseCode")
            }
            val body = it.inputStream.bufferedReader().use { reader -> reader.readText() }
            json.decodeFromString(GitHubReleaseResponse.serializer(), body)
        }
    }
}

@Serializable
private data class GitHubReleaseResponse(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GitHubReleaseAsset> = emptyList()
)

@Serializable
private data class GitHubReleaseAsset(
    val name: String,
    val size: Long = 0,
    @SerialName("browser_download_url") val downloadUrl: String
)

private inline fun <T : HttpURLConnection, R> T.use(block: (T) -> R): R {
    return try {
        block(this)
    } finally {
        disconnect()
    }
}


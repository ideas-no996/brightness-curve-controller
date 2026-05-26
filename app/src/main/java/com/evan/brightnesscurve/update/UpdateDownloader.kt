package com.evan.brightnesscurve.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UpdateDownloader(private val context: Context) {
    suspend fun download(updateInfo: UpdateInfo, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            updateDir.listFiles()?.forEach { file ->
                if (file.extension.equals("apk", ignoreCase = true)) file.delete()
            }

            val targetFile = File(updateDir, updateInfo.apkName.safeFileName())
            val connection = (URL(updateInfo.apkDownloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "BrightnessCurveController")
            }

            connection.use {
                val responseCode = it.responseCode
                if (responseCode !in 200..299) error("下载更新失败，HTTP $responseCode")

                val total = it.contentLengthLong.takeIf { length -> length > 0 } ?: updateInfo.apkSizeBytes
                var copied = 0L
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                it.inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            if (total > 0) {
                                onProgress(((copied * 100) / total).toInt().coerceIn(0, 100))
                            }
                        }
                    }
                }
            }

            if (!targetFile.exists() || targetFile.length() == 0L) {
                error("下载的安装包为空")
            }

            onProgress(100)
            targetFile
        }

    private fun String.safeFileName(): String {
        return replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}

private inline fun <T : HttpURLConnection, R> T.use(block: (T) -> R): R {
    return try {
        block(this)
    } finally {
        disconnect()
    }
}


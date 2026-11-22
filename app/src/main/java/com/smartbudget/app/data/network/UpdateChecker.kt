package com.smartbudget.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val apkDownloadUrl: String?,
    val isNewer: Boolean
)

object UpdateChecker {

    const val CURRENT_VERSION_TAG = "v1.1.0"
    private const val GITHUB_API_URL = "https://api.github.com/repos/retr0hck3r/SmartBudget/releases/latest"

    suspend fun checkLatestRelease(): Result<GitHubReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SmartBudget-Android-App")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)

                val tagName = json.optString("tag_name", "")
                val name = json.optString("name", "Релиз $tagName")
                val body = json.optString("body", "Описание обновления недоступно.")

                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val downloadUrl = asset.optString("browser_download_url", "")
                        if (downloadUrl.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = downloadUrl
                            break
                        }
                    }
                }

                val isNewer = compareVersions(tagName, CURRENT_VERSION_TAG) > 0

                Result.success(
                    GitHubReleaseInfo(
                        tagName = tagName,
                        name = name,
                        body = body,
                        apkDownloadUrl = apkUrl,
                        isNewer = isNewer
                    )
                )
            } else if (responseCode == 404) {
                Result.failure(Exception("Релизы в репозитории пока не опубликованы."))
            } else {
                Result.failure(Exception("HTTP $responseCode при проверке обновлений"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Сравнивает две версии в формате vX.Y.Z.
     * Возвращает > 0 если v1 новее v2, 0 если равны, < 0 если v1 старее.
     */
    fun compareVersions(v1: String, v2: String): Int {
        val clean1 = v1.removePrefix("v").removePrefix("V").split("-")[0].trim()
        val clean2 = v2.removePrefix("v").removePrefix("V").split("-")[0].trim()

        val parts1 = clean1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = clean2.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }
        return 0
    }
}

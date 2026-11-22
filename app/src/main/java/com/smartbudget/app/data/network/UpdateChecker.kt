package com.smartbudget.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
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

    const val CURRENT_VERSION_TAG = "v1.1.2"
    private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/retr0hck3r/SmartBudget/releases/latest"
    private const val GITHUB_TAGS_URL = "https://api.github.com/repos/retr0hck3r/SmartBudget/tags"

    suspend fun checkLatestRelease(): Result<GitHubReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            // 1. Сначала пробуем получить официальный Release
            val releaseInfo = fetchReleaseFromUrl(GITHUB_RELEASES_URL)
            if (releaseInfo != null) {
                return@withContext Result.success(releaseInfo)
            }

            // 2. Если отдельного релиза в веб-интерфейсе GitHub не создано (404),
            // опрашиваем теги репозитория (Git Tags)
            val tagInfo = fetchReleaseFromTags(GITHUB_TAGS_URL)
            if (tagInfo != null) {
                return@withContext Result.success(tagInfo)
            }

            Result.failure(Exception("Релизы или теги версий пока не найдены."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchReleaseFromUrl(urlStr: String): GitHubReleaseInfo? {
        return try {
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SmartBudget-Android-App")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode != 200) return null

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

            // Если в ассетах релиза нет файла, берем прямой файл из репозитория
            if (apkUrl == null && tagName.isNotBlank()) {
                apkUrl = "https://github.com/retr0hck3r/SmartBudget/raw/main/SmartBudget-$tagName.apk"
            }

            val isNewer = compareVersions(tagName, CURRENT_VERSION_TAG) > 0

            GitHubReleaseInfo(
                tagName = tagName,
                name = name,
                body = body,
                apkDownloadUrl = apkUrl,
                isNewer = isNewer
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchReleaseFromTags(urlStr: String): GitHubReleaseInfo? {
        return try {
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SmartBudget-Android-App")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode != 200) return null

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(responseText)
            if (array.length() == 0) return null

            var highestTag = ""
            for (i in 0 until array.length()) {
                val tagObj = array.getJSONObject(i)
                val tagName = tagObj.optString("name", "")
                if (compareVersions(tagName, highestTag) > 0) {
                    highestTag = tagName
                }
            }

            if (highestTag.isBlank()) return null

            val isNewer = compareVersions(highestTag, CURRENT_VERSION_TAG) > 0
            val apkUrl = "https://github.com/retr0hck3r/SmartBudget/raw/main/SmartBudget-$highestTag.apk"

            GitHubReleaseInfo(
                tagName = highestTag,
                name = "Версия $highestTag",
                body = "Опубликована новая версия приложения $highestTag в репозитории.",
                apkDownloadUrl = apkUrl,
                isNewer = isNewer
            )
        } catch (_: Exception) {
            null
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

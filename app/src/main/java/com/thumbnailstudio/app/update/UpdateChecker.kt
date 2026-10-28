package com.thumbnailstudio.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val title: String,
    val notes: String,
    val htmlUrl: String,
    val apkUrl: String?,
    val apkSizeBytes: Long
)

sealed class UpdateStatus {
    data class UpdateAvailable(val current: String, val release: ReleaseInfo) : UpdateStatus()
    data class UpToDate(val current: String, val release: ReleaseInfo?) : UpdateStatus()
    data class Failed(val message: String) : UpdateStatus()
}

/**
 * Optional online feature: reads the public GitHub Releases API to compare the
 * installed version with the latest release. Never required for editing — the
 * whole app works in airplane mode.
 */
object UpdateChecker {

    const val REPO_OWNER = "technicalnihilist-lgtm"
    const val REPO_NAME = "youtube-thumbnail-maker"
    const val REPO_URL = "https://github.com/$REPO_OWNER/$REPO_NAME"
    const val RELEASES_URL = "$REPO_URL/releases"

    private const val API = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    suspend fun check(currentVersion: String): UpdateStatus = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "ThumbnailStudio-Android")
            }
            val code = connection.responseCode
            if (code != 200) {
                connection.disconnect()
                return@withContext UpdateStatus.Failed("GitHub responded with HTTP $code")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(body).jsonObject
            val release = parseRelease(root) ?: return@withContext UpdateStatus.Failed("Unexpected release payload")
            if (isNewer(release.tagName, currentVersion)) {
                UpdateStatus.UpdateAvailable(currentVersion, release)
            } else {
                UpdateStatus.UpToDate(currentVersion, release)
            }
        } catch (t: Throwable) {
            UpdateStatus.Failed(t.message ?: "Could not reach GitHub")
        }
    }

    private fun parseRelease(root: JsonObject): ReleaseInfo? {
        val tag = root["tag_name"]?.jsonPrimitive?.contentOrNullSafe() ?: return null
        val name = root["name"]?.jsonPrimitive?.contentOrNullSafe() ?: tag
        val notes = root["body"]?.jsonPrimitive?.contentOrNullSafe() ?: ""
        val htmlUrl = root["html_url"]?.jsonPrimitive?.contentOrNullSafe()
            ?: "$RELEASES_URL/tag/$tag"
        var apkUrl: String? = null
        var apkSize = 0L
        root["assets"]?.jsonArray?.forEach { assetElement ->
            val asset = assetElement.jsonObject
            val assetName = asset["name"]?.jsonPrimitive?.contentOrNullSafe() ?: return@forEach
            if (assetName.endsWith(".apk", ignoreCase = true) && apkUrl == null) {
                apkUrl = asset["browser_download_url"]?.jsonPrimitive?.contentOrNullSafe()
                apkSize = asset["size"]?.jsonPrimitive?.contentOrNullSafe()?.toLongOrNull() ?: 0L
            }
        }
        return ReleaseInfo(tag, name, notes, htmlUrl, apkUrl, apkSize)
    }

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        runCatching { content }.getOrNull()

    /** Compares "1.0.10" style versions; ignores a leading "v". */
    fun isNewer(remote: String, current: String): Boolean {
        val r = numbers(remote)
        val c = numbers(current)
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv > cv) return true
            if (rv < cv) return false
        }
        return false
    }

    private fun numbers(version: String): List<Int> =
        version.trim().removePrefix("v").removePrefix("V")
            .split(Regex("[^0-9]+"))
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toIntOrNull() }
}

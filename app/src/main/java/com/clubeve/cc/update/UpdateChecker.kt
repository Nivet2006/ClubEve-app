package com.clubeve.cc.update

import com.clubeve.cc.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Checks the GitHub Releases API for a newer version of the app.
 *
 * Requires GITHUB_OWNER, GITHUB_REPO, and GITHUB_TOKEN to be set as
 * BuildConfig fields (injected from local.properties at build time).
 *
 * Version comparison is semantic: "1.2.3" > "1.1.9".
 * Tags must follow the pattern "v<semver>" (e.g. v1.2.0).
 */
object UpdateChecker {

    private const val API_URL =
        "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest"

    data class ReleaseInfo(
        val latestVersion: String,
        val apkDownloadUrl: String,
        val releaseNotes: String
    )

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * Returns [ReleaseInfo] if a newer version is available, null otherwise.
     * Never throws — all errors are swallowed and return null so a failed
     * check never disrupts the user's session.
     */
    suspend fun checkForUpdate(currentVersion: String): ReleaseInfo? {
        return try {
            val response = httpClient.get(API_URL) {
                header("Accept", "application/vnd.github.v3+json")
                if (BuildConfig.GITHUB_TOKEN.isNotBlank()) {
                    header("Authorization", "Bearer ${BuildConfig.GITHUB_TOKEN}")
                }
            }

            if (!response.status.isSuccess()) return null

            val body = response.bodyAsText()
            val json = Json.parseToJsonElement(body).jsonObject

            val tagName = json["tag_name"]?.jsonPrimitive?.content ?: return null
            val latestVersion = tagName.removePrefix("v")
            val releaseNotes = json["body"]?.jsonPrimitive?.content
                ?.takeIf { it.isNotBlank() }
                ?: "Bug fixes and improvements."

            // Find the first .apk asset
            val assets = json["assets"]?.jsonArray
            val apkUrl = assets
                ?.map { it.jsonObject }
                ?.firstOrNull { it["name"]?.jsonPrimitive?.content?.endsWith(".apk") == true }
                ?.get("browser_download_url")
                ?.jsonPrimitive?.content

            // If no APK asset yet (release still uploading), skip
            if (apkUrl.isNullOrBlank()) return null

            if (isNewerVersion(latestVersion, currentVersion)) {
                ReleaseInfo(latestVersion, apkUrl, releaseNotes)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Returns true if [latest] is strictly greater than [current] (semantic versioning). */
    private fun isNewerVersion(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(l.size, c.size)
        for (i in 0 until len) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }
}

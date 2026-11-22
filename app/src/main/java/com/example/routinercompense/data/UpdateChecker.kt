package com.example.routinercompense.data

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.routinercompense.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val releaseNotes: String,
    val downloadUrl: String
)

data class AppUpdateResult(
    val success: Boolean,
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val errorMessage: String? = null
)

class UpdateChecker {

    suspend fun checkForUpdates(urls: List<String>): AppUpdateResult = withContext(Dispatchers.IO) {
        for (urlStr in urls) {
            if (urlStr.isBlank()) continue
            val result = tryFetchFromUrl(urlStr)
            if (result != null && result.success) {
                return@withContext result
            }
        }
        return@withContext AppUpdateResult(
            success = false,
            hasUpdate = false,
            latestVersionName = BuildConfig.VERSION_NAME,
            releaseNotes = "",
            downloadUrl = "",
            errorMessage = "Impossible de contacter le serveur de mise à jour."
        )
    }

    private fun tryFetchFromUrl(versionJsonUrl: String): AppUpdateResult? {
        try {
            val url = URL(versionJsonUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.instanceFollowRedirects = true

            val code = connection.responseCode
            if (code in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val rawText = response.toString().trim()
                val obj: JSONObject = if (rawText.startsWith("[")) {
                    val arr = JSONArray(rawText)
                    if (arr.length() > 0) arr.getJSONObject(0) else return null
                } else {
                    JSONObject(rawText)
                }

                val dataObj = obj.optJSONObject("data") ?: obj

                val latestVersionCode = dataObj.optInt("latestVersionCode", 1)
                val latestVersionName = dataObj.optString("latestVersionName", "1.0")
                val releaseNotes = dataObj.optString("releaseNotes", "Nouvelle mise à jour disponible.")
                val downloadUrl = dataObj.optString("downloadUrl", "")

                val currentVersionCode = BuildConfig.VERSION_CODE
                val hasUpdate = latestVersionCode > currentVersionCode

                return AppUpdateResult(
                    success = true,
                    hasUpdate = hasUpdate,
                    latestVersionName = latestVersionName,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit,
    onDownloadClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "🎉 Nouvelle mise à jour v${updateInfo.latestVersionName}", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(text = updateInfo.releaseNotes, fontSize = 14.sp)
        },
        confirmButton = {
            Button(onClick = { onDownloadClick(updateInfo.downloadUrl) }) {
                Text(text = "Télécharger et installer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Plus tard")
            }
        }
    )
}

package com.example.routinercompense.data

import android.content.Context
import android.content.Intent
import android.net.Uri
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

class UpdateChecker {

    suspend fun checkForUpdates(versionJsonUrl: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        if (versionJsonUrl.isBlank()) return@withContext null

        try {
            val url = URL(versionJsonUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val rawObj = JSONObject(response.toString())
                val obj = rawObj.optJSONObject("data") ?: rawObj

                val latestVersionCode = obj.optInt("latestVersionCode", 1)
                val latestVersionName = obj.optString("latestVersionName", "1.0")
                val releaseNotes = obj.optString("releaseNotes", "Nouvelle mise à jour disponible.")
                val downloadUrl = obj.optString("downloadUrl", "")

                val currentVersionCode = BuildConfig.VERSION_CODE
                val hasUpdate = latestVersionCode > currentVersionCode

                return@withContext AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionName = latestVersionName,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext null
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

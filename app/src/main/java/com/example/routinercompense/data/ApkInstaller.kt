package com.example.routinercompense.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ApkInstaller(private val context: Context) {

    suspend fun downloadAndInstallApk(
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permIntent)
                }
            }

            var currentUrl = downloadUrl
            if (currentUrl.contains("drive.google.com") && currentUrl.contains("/file/d/")) {
                val fileId = currentUrl.substringAfter("/file/d/").substringBefore("/")
                currentUrl = "https://drive.google.com/uc?export=download&id=$fileId"
            }

            var connection: HttpURLConnection? = null
            var responseCode = -1
            var redirectCount = 0

            while (redirectCount < 6) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = true
                connection.connect()

                responseCode = connection.responseCode
                if (responseCode in listOf(301, 302, 303, 307, 308)) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http")) location else URL(url, location).toString()
                        redirectCount++
                        continue
                    }
                }
                break
            }

            if (connection == null || responseCode !in 200..299) {
                connection?.disconnect()
                return@withContext false
            }

            val fileLength = connection.contentLength
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            val apkFile = File(downloadsDir, "Routine_Recompenses_update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            var inputStream = connection.inputStream
            val firstBytes = ByteArray(4)
            val readHeader = inputStream.read(firstBytes)

            // Check if header is NOT PK\x03\x04 (Google Drive Virus Warning HTML Page)
            if (readHeader == 4 && !(firstBytes[0] == 0x50.toByte() && firstBytes[1] == 0x4B.toByte() && firstBytes[2] == 0x03.toByte() && firstBytes[3] == 0x04.toByte())) {
                val restBytes = inputStream.readBytes()
                inputStream.close()
                connection.disconnect()

                val html = String(firstBytes + restBytes)
                if (html.contains("download-form") || html.contains("confirm=")) {
                    val uuid = html.substringAfter("name=\"uuid\" value=\"", "").substringBefore("\"")
                    val confirm = html.substringAfter("name=\"confirm\" value=\"", "t").substringBefore("\"")
                    val fileId = html.substringAfter("name=\"id\" value=\"", "").substringBefore("\"")

                    val directDriveUrl = if (fileId.isNotBlank() && uuid.isNotBlank()) {
                        "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=$confirm&uuid=$uuid"
                    } else if (downloadUrl.contains("id=")) {
                        val id = downloadUrl.substringAfter("id=").substringBefore("&")
                        "https://drive.usercontent.google.com/download?id=$id&export=download&confirm=t"
                    } else {
                        null
                    }

                    if (directDriveUrl != null) {
                        connection = URL(directDriveUrl).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        connection.connectTimeout = 15000
                        connection.readTimeout = 15000
                        connection.instanceFollowRedirects = true
                        connection.connect()

                        if (connection.responseCode in 200..299) {
                            inputStream = connection.inputStream
                        } else {
                            return@withContext false
                        }
                    } else {
                        return@withContext false
                    }
                } else {
                    return@withContext false
                }
            }

            val outputStream = FileOutputStream(apkFile)
            val buffer = ByteArray(8192)
            var total: Long = 0
            var count: Int

            if (readHeader == 4 && firstBytes[0] == 0x50.toByte() && firstBytes[1] == 0x4B.toByte() && firstBytes[2] == 0x03.toByte() && firstBytes[3] == 0x04.toByte()) {
                outputStream.write(firstBytes, 0, 4)
                total += 4
            }

            while (inputStream.read(buffer).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    val percent = (total * 100 / fileLength).toInt()
                    onProgress(percent)
                }
                outputStream.write(buffer, 0, count)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            if (apkFile.exists() && apkFile.length() > 500000 && isApkValidZip(apkFile)) {
                launchInstaller(apkFile)
                return@withContext true
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun isApkValidZip(file: File): Boolean {
        return try {
            if (file.length() < 100) return false
            val bytes = ByteArray(4)
            val fis = FileInputStream(file)
            val read = fis.read(bytes)
            fis.close()
            read == 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
        } catch (e: Exception) {
            false
        }
    }

    private fun launchInstaller(apkFile: File) {
        val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }

        context.startActivity(intent)
    }

    fun downloadAndInstallWithDownloadManager(downloadUrl: String): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permIntent)
                }
            }

            var urlStr = downloadUrl
            if (urlStr.contains("drive.google.com") && urlStr.contains("/file/d/")) {
                val fileId = urlStr.substringAfter("/file/d/").substringBefore("/")
                urlStr = "https://drive.google.com/uc?export=download&id=$fileId"
            }

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val oldFile = File(downloadsDir, "Routine_Recompenses_update.apk")
            if (oldFile.exists()) {
                oldFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(urlStr))
                .setTitle("Routine & Récompenses")
                .setDescription("Mise à jour en cours...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "Routine_Recompenses_update.apk")
                .setMimeType("application/vnd.android.package-archive")

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            ctx.unregisterReceiver(this)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        val downloadedFile = File(downloadsDir, "Routine_Recompenses_update.apk")
                        if (downloadedFile.exists() && downloadedFile.length() > 500000 && isApkValidZip(downloadedFile)) {
                            launchInstaller(downloadedFile)
                        } else {
                            val uri = downloadManager.getUriForDownloadedFile(downloadId)
                            if (uri != null) {
                                launchInstallerWithUri(uri)
                            }
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun launchInstallerWithUri(apkUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

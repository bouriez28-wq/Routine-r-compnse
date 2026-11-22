package com.example.routinercompense.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ApkInstaller(private val context: Context) {

    suspend fun downloadAndInstallApk(
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
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
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
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
            val apkFile = File(downloadsDir, "Routine_Recompenses.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(apkFile)
            val buffer = ByteArray(8192)
            var total: Long = 0
            var count: Int

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
        }

        context.startActivity(intent)
    }
}

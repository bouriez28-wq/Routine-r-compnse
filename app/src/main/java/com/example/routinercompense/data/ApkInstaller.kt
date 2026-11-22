package com.example.routinercompense.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

class ApkInstaller(private val context: Context) {

    fun downloadAndInstallWithDownloadManager(downloadUrl: String): Boolean {
        try {
            var urlStr = downloadUrl
            if (urlStr.contains("drive.google.com") && urlStr.contains("/file/d/")) {
                val fileId = urlStr.substringAfter("/file/d/").substringBefore("/")
                urlStr = "https://drive.google.com/uc?export=download&id=$fileId"
            }

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val oldFile = File(downloadsDir, "Routine_Recompenses_update.apk")
            if (oldFile != null && oldFile.exists()) {
                oldFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(urlStr))
                .setTitle("Routine & Récompenses")
                .setDescription("Téléchargement de la mise à jour...")
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
                        if (downloadedFile.exists() && downloadedFile.length() > 500000) {
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
        launchInstallerWithUri(apkUri)
    }

    private fun launchInstallerWithUri(apkUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}

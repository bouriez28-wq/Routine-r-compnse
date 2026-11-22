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
import java.io.File

class ApkInstaller(private val context: Context) {

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

            val request = DownloadManager.Request(Uri.parse(urlStr))
                .setTitle("Routine & Récompenses")
                .setDescription("Téléchargement de la mise à jour...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Routine_Recompenses_update.apk")
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

                        val uri = downloadManager.getUriForDownloadedFile(downloadId)
                        if (uri != null) {
                            launchInstallerWithUri(uri)
                        } else {
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val downloadedFile = File(downloadsDir, "Routine_Recompenses_update.apk")
                            if (downloadedFile.exists()) {
                                launchInstaller(downloadedFile)
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

    fun launchInstaller(apkFile: File) {
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

    fun launchInstallerWithUri(apkUri: Uri) {
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

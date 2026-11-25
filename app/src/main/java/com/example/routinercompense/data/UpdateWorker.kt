package com.example.routinercompense.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val updateUrls = listOf(
        "https://raw.githubusercontent.com/bouriez28-wq/Routine-r-compnse/main/version.json",
        "https://api.restful-api.dev/objects/ff808181a09d98f701a202fed08619f8"
    )

    override suspend fun doWork(): Result {
        return try {
            val updateChecker = UpdateChecker()
            val result = updateChecker.checkForUpdates(updateUrls)

            if (result.success && result.hasUpdate) {
                val notificationHelper = NotificationHelper(context)
                notificationHelper.sendSystemNotification(
                    "🔔 Mise à jour v${result.latestVersionName} disponible !",
                    "Une nouvelle version de l'application est disponible. Toucher pour l'installer.",
                    isOpenUpdateDialog = true,
                    notificationType = NotificationHelper.TYPE_APP_UPDATE
                )
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

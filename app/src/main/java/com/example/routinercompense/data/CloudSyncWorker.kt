package com.example.routinercompense.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.routinercompense.model.DeviceRole

class CloudSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val repository = RoutineRepository(context)
    private val cloudSync = CloudSyncRepository()
    private val notificationHelper = NotificationHelper(context)

    override suspend fun doWork(): Result {
        return try {
            val familyCode = repository.getFamilyCode()
            val deviceRole = repository.getDeviceRole()

            if (familyCode.isNotBlank() && deviceRole != DeviceRole.UNSELECTED) {
                val cloudData = cloudSync.fetchFamilyData(familyCode)
                if (cloudData != null) {
                    val localRoutines = repository.loadRoutines()
                    val localPendingIds = localRoutines.filter { it.isPendingValidation }.map { it.id }.toSet()
                    val localHistory = repository.loadHistory()
                    val localPendingClaimIds = localHistory.filter { it.isPendingApproval }.map { it.id }.toSet()

                    if (deviceRole == DeviceRole.PARENT) {
                        // Check for new pending task validations from child
                        val newPendingTasks = cloudData.routines.filter { it.isPendingValidation && !localPendingIds.contains(it.id) }
                        for (task in newPendingTasks) {
                            notificationHelper.sendSystemNotification(
                                "⏳ Tâche à valider par ${cloudData.childName}",
                                "Tâche : \"${task.title}\" (+${task.starsReward} ⭐)"
                            )
                        }

                        // Check for new pending reward claims from child
                        val newClaims = cloudData.history.filter { it.isPendingApproval && !localPendingClaimIds.contains(it.id) }
                        for (claim in newClaims) {
                            notificationHelper.sendSystemNotification(
                                "🎁 Demande de récompense par ${cloudData.childName}",
                                "Récompense réclamée : \"${claim.rewardTitle}\" (${claim.starCost} ⭐)"
                            )
                        }
                    }

                    if (cloudData.routines.isNotEmpty()) {
                        repository.saveRoutines(cloudData.routines)
                    }
                    if (cloudData.rewards.isNotEmpty()) {
                        repository.saveRewards(cloudData.rewards)
                    }
                    if (cloudData.history.isNotEmpty()) {
                        repository.saveHistory(cloudData.history)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

package com.example.routinercompense.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.routinercompense.R
import com.example.routinercompense.model.DeviceRole
import kotlinx.coroutines.*

class RoutineSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: RoutineRepository
    private lateinit var cloudSync: CloudSyncRepository
    private lateinit var notificationHelper: NotificationHelper

    private val fgChannelId = "routine_fg_sync_channel"
    private val fgNotificationId = 9901

    override fun onCreate() {
        super.onCreate()
        repository = RoutineRepository(applicationContext)
        cloudSync = CloudSyncRepository()
        notificationHelper = NotificationHelper(applicationContext)
        startForegroundServiceNotification()
        startBackgroundSyncLoop()
    }

    private fun startForegroundServiceNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                fgChannelId,
                "Synchronisation en arrière-plan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintient la synchronisation en direct et la réception d'alertes 24h/24"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, fgChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Routine & Récompenses 🟢")
            .setContentText("Synchronisation en direct activée")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(fgNotificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(fgNotificationId, notification)
        }
    }

    private fun startBackgroundSyncLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val familyCode = repository.getFamilyCode()
                    val deviceRole = repository.getDeviceRole()

                    if (familyCode.isNotBlank() && deviceRole != DeviceRole.UNSELECTED) {
                        cloudSync.listenToFamilyCloudStream(familyCode) { cloudData ->
                            handleIncomingCloudData(cloudData, deviceRole)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(3000)
            }
        }
    }

    private fun handleIncomingCloudData(cloudData: FamilyCloudData, deviceRole: DeviceRole) {
        try {
            if (deviceRole == DeviceRole.PARENT) {
                val newPendingTasks = cloudData.routines.filter { it.isPendingValidation }
                for (task in newPendingTasks) {
                    val eventId = "task_pending_${task.id}_${task.completedDate}"
                    if (!repository.isEventNotified(eventId)) {
                        repository.markEventNotified(eventId)
                        notificationHelper.sendSystemNotification(
                            "⏳ Tâche à valider par ${cloudData.childName}",
                            "Tâche : \"${task.title}\" (+${task.starsReward} ⭐)",
                            notificationType = NotificationHelper.TYPE_PENDING_TASK
                        )
                    }
                }

                val newClaims = cloudData.history.filter { it.isPendingApproval }
                for (claim in newClaims) {
                    val eventId = "claim_pending_${claim.id}_${claim.timestamp}"
                    if (!repository.isEventNotified(eventId)) {
                        repository.markEventNotified(eventId)
                        notificationHelper.sendSystemNotification(
                            "🎁 Demande de récompense par ${cloudData.childName}",
                            "Récompense réclamée : \"${claim.rewardTitle}\" (${claim.starCost} ⭐)",
                            notificationType = NotificationHelper.TYPE_PENDING_REWARD
                        )
                    }
                }
            } else if (deviceRole == DeviceRole.CHILD) {
                val unreadStickers = cloudData.stickers.filter { !it.isReadByChild }
                for (st in unreadStickers) {
                    val eventId = "sticker_${st.id}"
                    if (!repository.isEventNotified(eventId)) {
                        repository.markEventNotified(eventId)
                        notificationHelper.sendSystemNotification(
                            "${st.icon} ${st.title}",
                            st.message,
                            notificationType = NotificationHelper.TYPE_STICKER
                        )
                    }
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

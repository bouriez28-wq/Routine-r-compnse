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

    private val notifiedTaskIds = HashSet<String>()
    private val notifiedClaimIds = HashSet<String>()

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
                        val cloudData = cloudSync.fetchFamilyData(familyCode)
                        if (cloudData != null) {
                            val localRoutines = repository.loadRoutines()
                            val localPendingIds = localRoutines.filter { it.isPendingValidation }.map { it.id }.toSet()
                            val localHistory = repository.loadHistory()
                            val localPendingClaimIds = localHistory.filter { it.isPendingApproval }.map { it.id }.toSet()

                            // Remove cleared pending items from notified sets
                            notifiedTaskIds.retainAll(cloudData.routines.filter { it.isPendingValidation }.map { it.id }.toSet())
                            notifiedClaimIds.retainAll(cloudData.history.filter { it.isPendingApproval }.map { it.id }.toSet())

                            // Parent alerts: pending task validations or reward claims (ONE notification per request)
                            if (deviceRole == DeviceRole.PARENT) {
                                val newPendingTasks = cloudData.routines.filter {
                                    it.isPendingValidation && !localPendingIds.contains(it.id) && !notifiedTaskIds.contains(it.id)
                                }
                                for (task in newPendingTasks) {
                                    notifiedTaskIds.add(task.id)
                                    notificationHelper.sendSystemNotification(
                                        "⏳ Tâche à valider par ${cloudData.childName}",
                                        "Tâche : \"${task.title}\" (+${task.starsReward} ⭐)"
                                    )
                                }

                                val newClaims = cloudData.history.filter {
                                    it.isPendingApproval && !localPendingClaimIds.contains(it.id) && !notifiedClaimIds.contains(it.id)
                                }
                                for (claim in newClaims) {
                                    notifiedClaimIds.add(claim.id)
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(3000)
            }
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

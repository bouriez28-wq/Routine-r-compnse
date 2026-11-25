package com.example.routinercompense.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.routinercompense.model.DeviceRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = RoutineRepository(context)
                val cloudSync = CloudSyncRepository()
                val notificationHelper = NotificationHelper(context)

                val familyCode = repository.getFamilyCode()
                val deviceRole = repository.getDeviceRole()

                if (familyCode.isNotBlank() && deviceRole != DeviceRole.UNSELECTED) {
                    val cloudData = cloudSync.fetchFamilyData(familyCode)
                    if (cloudData != null) {
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
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                scheduleNextAlarm(context)
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val ALARM_REQUEST_CODE = 8871

        fun scheduleNextAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, AlarmSyncReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerAtMillis = System.currentTimeMillis() + 8000 // Repeat every 8 seconds

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

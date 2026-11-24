package com.example.routinercompense.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            scheduleUpdateCheck(context)
        }
    }

    companion object {
        fun scheduleUpdateCheck(context: Context) {
            try {
                val updateWorkRequest = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.HOURS)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "BackgroundUpdateCheck",
                    ExistingPeriodicWorkPolicy.KEEP,
                    updateWorkRequest
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

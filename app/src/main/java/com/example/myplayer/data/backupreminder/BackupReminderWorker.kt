package com.example.myplayer.data.backupreminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.myplayer.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * v5.6 package-change warning. MyPlayer moves to a new package
 * (com.myplayer.app) in v6.0, so users must back up, uninstall, then install
 * v6.0 and restore. This worker fires a high-importance reminder every 3
 * hours; WorkManager keeps it alive across app kill, recents-clear and reboot.
 */
@HiltWorker
class BackupReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "com.example.myplayer.BACKUP_REMINDER_WORK"
        const val PREFS_NAME = "backup_reminder_prefs"
        const val KEY_FIRST_NUDGE_DONE = "first_nudge_done"
        private const val CHANNEL_ID = "backup_reminder_channel"
        private const val NOTIFICATION_ID = 9002
    }

    override suspend fun doWork(): Result {
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Backup Reminder", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Reminds you to back up your data before the v6.0 app package change"
                }
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigText = "The next update (v6.0) installs as a separate app — your library, playlists and settings will NOT carry over automatically.\n\n" +
            "1) Open Settings → Backup & Restore → Create Backup\n" +
            "2) Save the backup file somewhere safe\n" +
            "3) Uninstall MyPlayer before installing v6.0\n" +
            "4) Open the new app → Restore Backup and select your file"

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠ Backup your data before v6.0!")
            .setContentText("MyPlayer moves to a new app package in v6.0 — take a backup now so nothing is lost.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }
}

object BackupReminderScheduler {
    private const val REPEAT_INTERVAL_HOURS = 3L
    private const val FIRST_NUDGE_WORK_NAME = "com.example.myplayer.BACKUP_REMINDER_FIRST_NUDGE"
    private const val FIRST_NUDGE_DELAY_SECONDS = 30L

    /**
     * Idempotent: safe to call on every app start (ExistingPeriodicWorkPolicy.KEEP
     * preserves the already-scheduled 3h cadence). Also fires one quick nudge
     * ~30s after the first launch following the update to v5.6.
     */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<BackupReminderWorker>(
            REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BackupReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )

        val prefs = context.getSharedPreferences(BackupReminderWorker.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(BackupReminderWorker.KEY_FIRST_NUDGE_DONE, false)) {
            val oneTime = OneTimeWorkRequestBuilder<BackupReminderWorker>()
                .setInitialDelay(FIRST_NUDGE_DELAY_SECONDS, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                FIRST_NUDGE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                oneTime
            )
            prefs.edit().putBoolean(BackupReminderWorker.KEY_FIRST_NUDGE_DONE, true).apply()
        }
    }
}

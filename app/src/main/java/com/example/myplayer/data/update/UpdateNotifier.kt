package com.example.myplayer.data.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.myplayer.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "update_channel"
        private const val NOTIFICATION_ID = 9001
    }

    init {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "App Updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Notifies you when a new MyPlayer version is available"
                }
            )
        }
    }

    fun show(update: UpdateInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            browserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("MyPlayer ${update.versionName} is available")
            .setContentText("Tap to download the latest version")
            .setStyle(NotificationCompat.BigTextStyle().bigText(update.releaseNotes.ifBlank { "A new version of MyPlayer is available on GitHub." }.take(400)))
            .setContentIntent(pendingIntent)
            .addAction(0, "UPDATE NOW", pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }
}

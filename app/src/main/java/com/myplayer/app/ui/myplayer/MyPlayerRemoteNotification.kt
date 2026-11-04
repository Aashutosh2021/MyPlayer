package com.myplayer.app.ui.myplayer

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.myplayer.app.ui.theme.NeonLimePrimary
import com.myplayer.app.ui.theme.OnNeonLime
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class MyPlayerRemoteNotification(
    val id: String,
    val title: String,
    val message: String,
    val category: String,
    val buttonText: String,
    val actionUrl: String,
    val showSystemNotification: Boolean,
    val showInAppPopup: Boolean,
    val createdAtMs: Long
)

typealias MochyRemoteNotification = MyPlayerRemoteNotification

object MyPlayerRemoteNotificationManager {
    private const val FIRESTORE_PROJECT_ID = "myplayer-660ee"
    private const val FIRESTORE_NOTIFS_URL =
        "https://firestore.googleapis.com/v1/projects/$FIRESTORE_PROJECT_ID/databases/(default)/documents/notifications?pageSize=20"
    private const val CHANNEL_ID = "myplayer_remote_alerts"
    private const val PREFS_NAME = "mochy_remote_notifs_prefs"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var pollingStarted = false

    var enabled by mutableStateOf(true)
    var activeRemotePopup by mutableStateOf<MyPlayerRemoteNotification?>(null)
    var notificationHistory by mutableStateOf<List<MyPlayerRemoteNotification>>(emptyList())
    var unreadCount by mutableIntStateOf(0)
    var lastCheckStatus by mutableStateOf("Connected to myplayer-660ee")

    fun startAutoPolling(context: Context) {
        val appCtx = context.applicationContext
        ensureNotificationChannel(appCtx)
        if (pollingStarted) return
        pollingStarted = true

        scope.launch {
            while (true) {
                if (enabled) {
                    fetchNotificationsNow(appCtx, isManualCheck = false)
                }
                delay(25_000L) // Poll every 25 seconds for live updates from admin.html
            }
        }
    }

    fun checkNowFromUi(context: Context) {
        val appCtx = context.applicationContext
        ensureNotificationChannel(appCtx)
        scope.launch {
            fetchNotificationsNow(appCtx, isManualCheck = true)
        }
    }

    suspend fun fetchNotificationsNow(context: Context, isManualCheck: Boolean = false) {
        try {
            val conn = (URL(FIRESTORE_NOTIFS_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                withContext(Dispatchers.Main) {
                    lastCheckStatus = "Sync status: HTTP $code"
                }
                return
            }

            val bodyText = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(bodyText)
            val docs = root.optJSONArray("documents")
            val parsedList = mutableListOf<MyPlayerRemoteNotification>()

            if (docs != null) {
                for (i in 0 until docs.length()) {
                    val docObj = docs.optJSONObject(i) ?: continue
                    val fullName = docObj.optString("name", "")
                    val docId = fullName.substringAfterLast("/").ifBlank { "notif_$i" }
                    val fields = docObj.optJSONObject("fields") ?: continue

                    val title = fields.optJSONObject("title")?.optString("stringValue", "").orEmpty()
                    val message = fields.optJSONObject("message")?.optString("stringValue", "").orEmpty()
                    if (title.isBlank() && message.isBlank()) continue

                    val category = fields.optJSONObject("category")?.optString("stringValue", "UPDATE 🚀") ?: "UPDATE 🚀"
                    val buttonText = fields.optJSONObject("buttonText")?.optString("stringValue", "Check It Out 🔥") ?: "Check It Out 🔥"
                    val actionUrl = fields.optJSONObject("actionUrl")?.optString("stringValue", "").orEmpty()
                    val showSys = fields.optJSONObject("showSystemNotification")?.optBoolean("booleanValue", true) ?: true
                    val showPopup = fields.optJSONObject("showInAppPopup")?.optBoolean("booleanValue", true) ?: true
                    val createdMs = fields.optJSONObject("createdAtMs")?.optString("integerValue")?.toLongOrNull()
                        ?: fields.optJSONObject("createdAtMs")?.optDouble("doubleValue")?.toLong()
                        ?: System.currentTimeMillis()

                    parsedList.add(
                        MyPlayerRemoteNotification(
                            id = docId,
                            title = title,
                            message = message,
                            category = category,
                            buttonText = buttonText,
                            actionUrl = actionUrl,
                            showSystemNotification = showSys,
                            showInAppPopup = showPopup,
                            createdAtMs = createdMs
                        )
                    )
                }
            }

            val sorted = parsedList.sortedByDescending { it.createdAtMs }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val seenIds = prefs.getStringSet("seen_notif_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

            val newestUnseen = sorted.firstOrNull { it.id !in seenIds }

            withContext(Dispatchers.Main) {
                notificationHistory = sorted
                unreadCount = sorted.count { it.id !in seenIds }
                val timeStr = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
                lastCheckStatus = "Synced $timeStr • ${sorted.size} notification(s)"

                if (newestUnseen != null) {
                    seenIds.add(newestUnseen.id)
                    prefs.edit().putStringSet("seen_notif_ids", seenIds).apply()
                    unreadCount = sorted.count { it.id !in seenIds }

                    if (newestUnseen.showInAppPopup) {
                        activeRemotePopup = newestUnseen
                    }
                    if (newestUnseen.showSystemNotification) {
                        postAndroidSystemNotification(context, newestUnseen)
                    }
                } else if (isManualCheck && sorted.isNotEmpty()) {
                    // When user manually taps "Check Now" in Settings, preview the latest notification
                    activeRemotePopup = sorted.first()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                lastCheckStatus = "Offline / waiting for connection"
            }
        }
    }

    fun requestNotificationPermissionIfNeeded(activity: Activity?) {
        if (activity == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    9021
                )
            }
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MyPlayer Updates & Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live announcements, app updates, and music drops from MyPlayer"
                enableVibration(true)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    fun postAndroidSystemNotification(context: Context, notif: MyPlayerRemoteNotification) {
        try {
            ensureNotificationChannel(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPerm) return
            }

            val intent = if (notif.actionUrl.isNotBlank()) {
                Intent(Intent.ACTION_VIEW, Uri.parse(notif.actionUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                } ?: Intent()
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notif.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(notif.title)
                .setContentText(notif.message)
                .setSubText(notif.category)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notif.message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            mgr?.notify(notif.id.hashCode(), builder.build())
        } catch (_: Throwable) {}
    }

    fun dismissActivePopup() {
        activeRemotePopup = null
    }
}

typealias MochyRemoteNotificationManager = MyPlayerRemoteNotificationManager

@Composable
fun MyPlayerRemoteNotificationPopupDialog(
    notification: MyPlayerRemoteNotification,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .shadow(24.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = MyPlayerBlack,
            border = BorderStroke(
                width = 1.6.dp,
                brush = Brush.linearGradient(listOf(MyPlayerAccentGreen, NeonLimePrimary))
            )
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NeonLimePrimary.copy(alpha = 0.16f),
                                MyPlayerBlack,
                                MyPlayerBlack
                            )
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MyPlayerAccentGreen.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, MyPlayerAccentGreen.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = notification.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MyPlayerAccentGreen,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = notification.title,
                    fontSize = 21.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MyPlayerTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = MyPlayerTextPrimary.copy(alpha = 0.86f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = {
                        val url = notification.actionUrl.trim()
                        onDismiss()
                        if (url.isNotBlank()) {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MyPlayerAccentGreen,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = notification.buttonText.ifBlank { "Got It! 🔥" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                if (notification.actionUrl.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss", color = MyPlayerTextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MochyRemoteNotificationPopupDialog(
    notification: MochyRemoteNotification,
    onDismiss: () -> Unit
) = MyPlayerRemoteNotificationPopupDialog(notification, onDismiss)

@Composable
fun MyPlayerRemoteNotificationsCenterScreen(
    myplayerState: MyPlayerUiStateHolder? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val history = MyPlayerRemoteNotificationManager.notificationHistory

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Remote\nNotifications",
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Connected to https://myplayer-660ee.web.app • ${MyPlayerRemoteNotificationManager.lastCheckStatus}",
                fontSize = 12.sp,
                color = MyPlayerAccentGreen
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Receive Remote Push Notifications",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Fetch live updates, music drops & announcements from myplayer-660ee.web.app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = myplayerState?.remoteNotificationsEnabled ?: MyPlayerRemoteNotificationManager.enabled,
                            onCheckedChange = {
                                myplayerState?.remoteNotificationsEnabled = it
                                MyPlayerRemoteNotificationManager.enabled = it
                            }
                        )
                    }

                    Button(
                        onClick = {
                            MyPlayerRemoteNotificationManager.requestNotificationPermissionIfNeeded(context as? Activity)
                            MyPlayerRemoteNotificationManager.checkNowFromUi(context)
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MyPlayerAccentGreen,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sync & Preview Latest Notification Now", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "RECEIVED NOTIFICATIONS (${history.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (history.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔔", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No remote notifications yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Push a notification from https://myplayer-660ee.web.app/admin.html and it will appear here and in your Android status bar automatically!",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(history, key = { it.id }) { item ->
                val dateText = remember(item.createdAtMs) {
                    SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(item.createdAtMs))
                }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            MyPlayerRemoteNotificationManager.activeRemotePopup = item
                        }
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = MyPlayerAccentGreen.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = item.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MyPlayerAccentGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(dateText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(item.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        if (item.actionUrl.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "${item.buttonText} →",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MyPlayerAccentGreen,
                                modifier = Modifier.clickable {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.actionUrl)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(90.dp)) }
    }
}

@Composable
fun MochyRemoteNotificationsCenterScreen(
    mochyState: MyPlayerUiStateHolder,
    onBack: () -> Unit
) = MyPlayerRemoteNotificationsCenterScreen(mochyState, onBack)


package com.example.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.CapturedNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationCollectorService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val packageName = sbn.packageName ?: return

        // Extract title
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()

        // Extract text (prefer big text for full message bodies)
        var text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        if (text.isNullOrBlank()) {
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        }

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()

        // Skip completely empty notifications with no title and no text
        if (title.isNullOrBlank() && text.isNullOrBlank()) {
            return
        }

        // Resolve friendly application name
        val pm = applicationContext.packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            packageName
        }

        val postTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()
        val dateObj = Date(postTime)
        val dateKey = dateFormat.format(dateObj)
        val timeString = timeFormat.format(dateObj)

        val captured = CapturedNotification(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            subText = subText,
            timestamp = postTime,
            dateKey = dateKey,
            timeString = timeString,
            isBookmarked = false
        )

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.notificationDao().insert(captured)
                Log.d(TAG, "Notification captured: $appName - $title")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist captured notification", e)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "NotificationCollectorService connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "NotificationCollectorService disconnected")
    }

    companion object {
        private const val TAG = "NotiCaptureService"
    }
}

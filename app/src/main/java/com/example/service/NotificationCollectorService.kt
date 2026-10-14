package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.CapturedNotification
import com.example.data.model.CustomCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationCollectorService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Throttling to prevent floods from ongoing progress bars or identical rapid events
    private val recentNotifications = object : LinkedHashMap<String, Long>(30, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
            return size > 50
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val packageName = sbn.packageName ?: return

        // Extract title (prefer big title if present)
        val title = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()

        // Extract text (prefer big text for full message body, or inbox lines)
        var text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        if (text.isNullOrBlank()) {
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (!lines.isNullOrEmpty()) {
                text = lines.filterNotNull().joinToString("\n") { it.toString().trim() }
            }
        }
        if (text.isNullOrBlank()) {
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        }

        // Subtext or info text (e.g., account email or category)
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.trim()

        // Skip completely empty notifications with no title and no text
        if (title.isNullOrBlank() && text.isNullOrBlank()) {
            return
        }

        // Deduplication check: ignore duplicate (package + title + text) received within 3 seconds
        val deduplicationKey = "$packageName|$title|$text"
        val currentTime = System.currentTimeMillis()
        synchronized(recentNotifications) {
            val lastSeen = recentNotifications[deduplicationKey]
            if (lastSeen != null && (currentTime - lastSeen) < 3000L) {
                return
            }
            recentNotifications[deduplicationKey] = currentTime
        }

        // Resolve user-friendly application name
        val pm = applicationContext.packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            packageName
        }

        val postTime = if (sbn.postTime > 0) sbn.postTime else currentTime
        val dateObj = Date(postTime)
        val dateKey = dateFormat.format(dateObj)
        val timeString = timeFormat.format(dateObj)

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val categories = db.categoryDao().getAllCategories().firstOrNull() ?: emptyList()

                // Attempt smart automatic category matching based on keywords
                val matchedCategory = findMatchingCategory(
                    packageName = packageName,
                    appName = appName,
                    title = title.orEmpty(),
                    text = text.orEmpty(),
                    availableCategories = categories
                )

                val captured = CapturedNotification(
                    packageName = packageName,
                    appName = appName,
                    title = title,
                    text = text,
                    subText = subText,
                    timestamp = postTime,
                    dateKey = dateKey,
                    timeString = timeString,
                    categoryId = matchedCategory?.id,
                    categoryName = matchedCategory?.name,
                    categoryColor = matchedCategory?.colorHex,
                    isBookmarked = false
                )

                val id = db.notificationDao().insert(captured)
                val savedItem = captured.copy(id = id)
                _lastCapturedNotification.value = savedItem
                Log.d(TAG, "Notification captured and saved successfully: $appName - $title (ID: $id)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist captured notification", e)
            }
        }
    }

    private fun findMatchingCategory(
        packageName: String,
        appName: String,
        title: String,
        text: String,
        availableCategories: List<CustomCategory>
    ): CustomCategory? {
        val combined = "$packageName $appName $title $text".lowercase(Locale.getDefault())

        val financeKeywords = listOf("pix", "transferência", "banco", "compra", "fatura", "cartão", "pagamento", "boleto", "saldo", "cashback", "ted", "doc", "c6", "nubank", "inter", "itau", "bradesco", "santander")
        val socialKeywords = listOf("whatsapp", "telegram", "instagram", "facebook", "twitter", "mensagens", "sms", "chat", "direct", "conversa")
        val workKeywords = listOf("slack", "teams", "reunião", "meeting", "sprint", "relatório", "jira", "trello", "trabalho", "email", "gmail", "outlook", "documento")
        val urgentKeywords = listOf("código", "urgente", "alerta", "verificação", "2fa", "segurança", "token", "expira", "atenção", "aviso")
        val shoppingKeywords = listOf("mercadolivre", "amazon", "shopee", "aliexpress", "entrega", "pedido", "encomenda", "rastreio", "entregue")
        val promoKeywords = listOf("desconto", "promoção", "cupom", "oferta", "grátis", "spotify", "netflix", "youtube")

        val targetCategoryName = when {
            urgentKeywords.any { combined.contains(it) } -> "Urgente / Importante"
            financeKeywords.any { combined.contains(it) } -> "Finanças & Bancos"
            socialKeywords.any { combined.contains(it) } -> "Social & Chat"
            workKeywords.any { combined.contains(it) } -> "Trabalho"
            shoppingKeywords.any { combined.contains(it) } -> "Compras & Entregas"
            promoKeywords.any { combined.contains(it) } -> "Promoções & Lazer"
            else -> null
        }

        return targetCategoryName?.let { name ->
            availableCategories.firstOrNull { it.name.contains(name, ignoreCase = true) || name.contains(it.name, ignoreCase = true) }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true
        Log.i(TAG, "NotificationCollectorService connected and ready")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceConnected.value = false
        Log.i(TAG, "NotificationCollectorService disconnected")
    }

    companion object {
        private const val TAG = "NotiCaptureService"

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _lastCapturedNotification = MutableStateFlow<CapturedNotification?>(null)
        val lastCapturedNotification: StateFlow<CapturedNotification?> = _lastCapturedNotification.asStateFlow()

        /**
         * Re-binds the service if disconnected on Android 7.0+
         */
        fun requestRebindIfPossible(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestRebind(ComponentName(context, NotificationCollectorService::class.java))
                } catch (e: Exception) {
                    Log.w(TAG, "Could not request rebind: ${e.message}")
                }
            }
        }
    }
}

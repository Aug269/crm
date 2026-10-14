package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.AppSummaryTuple
import com.example.data.db.CategorySummaryTuple
import com.example.data.model.CapturedNotification
import com.example.data.model.CustomCategory
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotificationRepository(private val database: AppDatabase) {

    private val notificationDao = database.notificationDao()
    private val categoryDao = database.categoryDao()

    val allNotifications: Flow<List<CapturedNotification>> = notificationDao.getAllNotifications()
    val bookmarkedNotifications: Flow<List<CapturedNotification>> = notificationDao.getBookmarkedNotifications()
    val appSummaries: Flow<List<AppSummaryTuple>> = notificationDao.getAppSummaries()
    val allCategories: Flow<List<CustomCategory>> = categoryDao.getAllCategories()
    val categoryCounts: Flow<List<CategorySummaryTuple>> = notificationDao.getCategoryCounts()
    val totalCount: Flow<Int> = notificationDao.getTotalCount()

    fun getTodayCount(): Flow<Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return notificationDao.getTodayCount(calendar.timeInMillis)
    }

    fun getFilteredNotifications(
        packageName: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?,
        categoryId: Long?,
        searchQuery: String?
    ): Flow<List<CapturedNotification>> {
        return notificationDao.getFilteredNotifications(
            packageName = packageName,
            fromTimestamp = fromTimestamp,
            toTimestamp = toTimestamp,
            categoryId = categoryId,
            searchQuery = searchQuery?.trim()?.ifEmpty { null }
        )
    }

    suspend fun insertNotification(notification: CapturedNotification): Long {
        return notificationDao.insert(notification)
    }

    suspend fun deleteNotification(notification: CapturedNotification) {
        notificationDao.delete(notification)
    }

    suspend fun deleteById(id: Long) {
        notificationDao.deleteById(id)
    }

    suspend fun deleteByPackage(packageName: String) {
        notificationDao.deleteByPackage(packageName)
    }

    suspend fun deleteByDateRange(fromTimestamp: Long, toTimestamp: Long) {
        notificationDao.deleteByDateRange(fromTimestamp, toTimestamp)
    }

    suspend fun clearAll() {
        notificationDao.clearAll()
    }

    suspend fun setBookmark(id: Long, isBookmarked: Boolean) {
        notificationDao.updateBookmark(id, isBookmarked)
    }

    suspend fun assignCategory(notificationId: Long, category: CustomCategory?) {
        notificationDao.updateNotificationCategory(
            notificationId = notificationId,
            categoryId = category?.id,
            categoryName = category?.name,
            categoryColor = category?.colorHex
        )
    }

    // Category CRUD
    suspend fun createCategory(name: String, colorHex: String): Long {
        return categoryDao.insert(
            CustomCategory(
                name = name.trim(),
                colorHex = colorHex
            )
        )
    }

    suspend fun updateCategory(category: CustomCategory) {
        categoryDao.update(category)
        notificationDao.syncCategoryUpdate(
            categoryId = category.id,
            newName = category.name,
            newColor = category.colorHex
        )
    }

    suspend fun deleteCategory(category: CustomCategory) {
        notificationDao.unassignCategory(category.id)
        categoryDao.delete(category)
    }

    suspend fun ensureDefaultCategories() {
        if (categoryDao.count() == 0) {
            val defaults = listOf(
                CustomCategory(name = "Finanças & Bancos", colorHex = "#10B981"),
                CustomCategory(name = "Trabalho", colorHex = "#6366F1"),
                CustomCategory(name = "Social & Chat", colorHex = "#06B6D4"),
                CustomCategory(name = "Urgente / Importante", colorHex = "#EF4444"),
                CustomCategory(name = "Compras & Entregas", colorHex = "#F59E0B"),
                CustomCategory(name = "Promoções & Lazer", colorHex = "#EC4899")
            )
            categoryDao.insertAll(defaults)
        }
    }

    suspend fun seedSampleData() {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val hourMs = 60 * 60 * 1000L
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val samples = listOf(
            CapturedNotification(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Mariana Silva (Grupo Família)",
                text = "Alguém já comprou as coisas para o almoço de domingo?",
                subText = "WhatsApp • 3 novas mensagens",
                timestamp = now - (15 * 60 * 1000L),
                dateKey = dateFormat.format(now - (15 * 60 * 1000L)),
                timeString = timeFormat.format(now - (15 * 60 * 1000L)),
                categoryName = "Social & Chat",
                categoryColor = "#06B6D4",
                isBookmarked = false
            ),
            CapturedNotification(
                packageName = "com.nu.production",
                appName = "Nubank",
                title = "Transferência Pix recebida",
                text = "Você recebeu um Pix de R$ 350,00 de Lucas Ferreira.",
                subText = "Conta Digital",
                timestamp = now - (45 * 60 * 1000L),
                dateKey = dateFormat.format(now - (45 * 60 * 1000L)),
                timeString = timeFormat.format(now - (45 * 60 * 1000L)),
                categoryName = "Finanças & Bancos",
                categoryColor = "#10B981",
                isBookmarked = true
            ),
            CapturedNotification(
                packageName = "com.google.android.gm",
                appName = "Gmail",
                title = "Equipe de Produto: Relatório Mensal",
                text = "Segue em anexo o relatório consolidado com métricas do último trimestre.",
                subText = "trabalho@empresa.com",
                timestamp = now - (2 * hourMs),
                dateKey = dateFormat.format(now - (2 * hourMs)),
                timeString = timeFormat.format(now - (2 * hourMs)),
                categoryName = "Trabalho",
                categoryColor = "#6366F1",
                isBookmarked = true
            ),
            CapturedNotification(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "carlos_fotografia começou a seguir você",
                text = "Veja as publicações e fotos recentes no perfil.",
                subText = "Notificações",
                timestamp = now - (4 * hourMs),
                dateKey = dateFormat.format(now - (4 * hourMs)),
                timeString = timeFormat.format(now - (4 * hourMs)),
                categoryName = "Social & Chat",
                categoryColor = "#06B6D4",
                isBookmarked = false
            ),
            CapturedNotification(
                packageName = "com.mercadolivre",
                appName = "Mercado Livre",
                title = "Seu pacote está a caminho!",
                text = "O motorista saiu para entrega e deve chegar hoje até as 18:00.",
                subText = "Envios Full",
                timestamp = now - (6 * hourMs),
                dateKey = dateFormat.format(now - (6 * hourMs)),
                timeString = timeFormat.format(now - (6 * hourMs)),
                categoryName = "Compras & Entregas",
                categoryColor = "#F59E0B",
                isBookmarked = false
            ),
            CapturedNotification(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Seu motorista chegou",
                text = "Gabriel está te esperando em um Toyota Corolla Prata (ABC-1234).",
                subText = "UberX",
                timestamp = now - dayMs - (1 * hourMs),
                dateKey = dateFormat.format(now - dayMs - (1 * hourMs)),
                timeString = timeFormat.format(now - dayMs - (1 * hourMs)),
                categoryName = "Urgente / Importante",
                categoryColor = "#EF4444",
                isBookmarked = false
            ),
            CapturedNotification(
                packageName = "com.google.android.apps.messaging",
                appName = "Mensagens",
                title = "Código de segurança 2FA",
                text = "Seu código de verificação é 849201. Válido por 5 minutos.",
                subText = "SMS",
                timestamp = now - dayMs - (3 * hourMs),
                dateKey = dateFormat.format(now - dayMs - (3 * hourMs)),
                timeString = timeFormat.format(now - dayMs - (3 * hourMs)),
                categoryName = "Finanças & Bancos",
                categoryColor = "#10B981",
                isBookmarked = true
            ),
            CapturedNotification(
                packageName = "com.spotify.music",
                appName = "Spotify",
                title = "Nova música lançada",
                text = "O artista que você segue acabou de lançar um novo álbum.",
                subText = "Radar de Novidades",
                timestamp = now - (2 * dayMs) - (2 * hourMs),
                dateKey = dateFormat.format(now - (2 * dayMs) - (2 * hourMs)),
                timeString = timeFormat.format(now - (2 * dayMs) - (2 * hourMs)),
                categoryName = "Promoções & Lazer",
                categoryColor = "#EC4899",
                isBookmarked = false
            )
        )
        notificationDao.insertAll(samples)
    }
}

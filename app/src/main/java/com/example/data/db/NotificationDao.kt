package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CapturedNotification
import kotlinx.coroutines.flow.Flow

data class AppSummaryTuple(
    val packageName: String,
    val appName: String,
    val count: Int,
    val lastTimestamp: Long
)

data class CategorySummaryTuple(
    val categoryId: Long,
    val count: Int
)

@Dao
interface NotificationDao {

    @Query("SELECT * FROM captured_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<CapturedNotification>>

    @Query("""
        SELECT * FROM captured_notifications 
        WHERE (:packageName IS NULL OR packageName = :packageName)
        AND (:fromTimestamp IS NULL OR timestamp >= :fromTimestamp)
        AND (:toTimestamp IS NULL OR timestamp <= :toTimestamp)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (
            :searchQuery IS NULL OR :searchQuery = '' OR
            title LIKE '%' || :searchQuery || '%' OR 
            text LIKE '%' || :searchQuery || '%' OR 
            subText LIKE '%' || :searchQuery || '%' OR
            appName LIKE '%' || :searchQuery || '%'
        )
        ORDER BY timestamp DESC
    """)
    fun getFilteredNotifications(
        packageName: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?,
        categoryId: Long?,
        searchQuery: String?
    ): Flow<List<CapturedNotification>>

    @Query("SELECT * FROM captured_notifications WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedNotifications(): Flow<List<CapturedNotification>>

    @Query("SELECT DISTINCT packageName, appName, COUNT(*) as count, MAX(timestamp) as lastTimestamp FROM captured_notifications GROUP BY packageName, appName ORDER BY count DESC")
    fun getAppSummaries(): Flow<List<AppSummaryTuple>>

    @Query("SELECT categoryId, COUNT(*) as count FROM captured_notifications WHERE categoryId IS NOT NULL GROUP BY categoryId")
    fun getCategoryCounts(): Flow<List<CategorySummaryTuple>>

    @Query("SELECT COUNT(*) FROM captured_notifications")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM captured_notifications WHERE timestamp >= :todayStart")
    fun getTodayCount(todayStart: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: CapturedNotification): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<CapturedNotification>)

    @Delete
    suspend fun delete(notification: CapturedNotification)

    @Query("DELETE FROM captured_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM captured_notifications WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM captured_notifications WHERE timestamp BETWEEN :fromTimestamp AND :toTimestamp")
    suspend fun deleteByDateRange(fromTimestamp: Long, toTimestamp: Long)

    @Query("DELETE FROM captured_notifications")
    suspend fun clearAll()

    @Query("UPDATE captured_notifications SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Long, isBookmarked: Boolean)

    @Query("UPDATE captured_notifications SET categoryId = :categoryId, categoryName = :categoryName, categoryColor = :categoryColor WHERE id = :notificationId")
    suspend fun updateNotificationCategory(notificationId: Long, categoryId: Long?, categoryName: String?, categoryColor: String?)

    @Query("UPDATE captured_notifications SET categoryId = NULL, categoryName = NULL, categoryColor = NULL WHERE categoryId = :categoryId")
    suspend fun unassignCategory(categoryId: Long)

    @Query("UPDATE captured_notifications SET categoryName = :newName, categoryColor = :newColor WHERE categoryId = :categoryId")
    suspend fun syncCategoryUpdate(categoryId: Long, newName: String, newColor: String)
}

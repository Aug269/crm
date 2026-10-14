package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "captured_notifications",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["packageName"]),
        Index(value = ["dateKey"]),
        Index(value = ["categoryId"]),
        Index(value = ["isBookmarked"])
    ]
)
data class CapturedNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String? = null,
    val text: String? = null,
    val subText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val dateKey: String, // e.g. "2026-10-14"
    val timeString: String, // e.g. "14:30"
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val categoryColor: String? = null,
    val isBookmarked: Boolean = false
)

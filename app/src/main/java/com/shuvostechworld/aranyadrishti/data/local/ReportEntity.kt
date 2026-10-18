package com.shuvostechworld.aranyadrishti.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String = java.util.UUID.randomUUID().toString(),
    val category: String,
    val urgency: String,
    val locationLat: Double?,
    val locationLng: Double?,
    val descriptionText: String?,
    val voiceNoteLocalPath: String?,
    val imageLocalPath: String?,
    val timestamp: Long,
    var isSynced: Boolean = false,
    var isAnonymous: Boolean = false
)

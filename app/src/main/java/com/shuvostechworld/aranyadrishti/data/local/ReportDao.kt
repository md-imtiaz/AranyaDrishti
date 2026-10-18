package com.shuvostechworld.aranyadrishti.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity): Long

    @Query("SELECT * FROM reports WHERE isSynced = 0")
    suspend fun getUnsyncedReports(): List<ReportEntity>

    @Query("UPDATE reports SET isSynced = :isSynced WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, isSynced: Boolean)

    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getReportById(id: Long): ReportEntity?

    @androidx.room.Update
    suspend fun updateReport(report: ReportEntity)

    @androidx.room.Delete
    suspend fun deleteReport(report: ReportEntity)
}

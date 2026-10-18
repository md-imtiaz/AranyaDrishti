package com.shuvostechworld.aranyadrishti.data.repository

import com.shuvostechworld.aranyadrishti.data.local.ReportDao
import com.shuvostechworld.aranyadrishti.data.local.ReportEntity
import kotlinx.coroutines.flow.Flow

class ReportRepository(private val reportDao: ReportDao) {

    val allReports: Flow<List<ReportEntity>> = reportDao.getAllReports()

    suspend fun insertReport(report: ReportEntity) {
        reportDao.insertReport(report)
    }

    suspend fun getUnsyncedReports(): List<ReportEntity> {
        return reportDao.getUnsyncedReports()
    }

    suspend fun updateSyncStatus(id: Long, isSynced: Boolean) {
        reportDao.updateSyncStatus(id, isSynced)
    }

    suspend fun getReportById(id: Long): ReportEntity? {
        return reportDao.getReportById(id)
    }

    suspend fun updateReport(report: ReportEntity) {
        reportDao.updateReport(report)
    }

    suspend fun deleteReport(report: ReportEntity) {
        reportDao.deleteReport(report)
    }
}

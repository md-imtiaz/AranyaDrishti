package com.shuvostechworld.aranyadrishti.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shuvostechworld.aranyadrishti.data.remote.FirebaseSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportDeleteWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val remoteId = inputData.getString("remoteId") ?: return@withContext Result.success()

        val firebaseSyncManager = FirebaseSyncManager()
        val result = firebaseSyncManager.deleteReport(remoteId)

        if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}

package com.shuvostechworld.aranyadrishti.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.storage.FirebaseStorage
import com.shuvostechworld.aranyadrishti.data.local.AppDatabase
import com.shuvostechworld.aranyadrishti.data.remote.FirebaseSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.max
class ReportSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val reportDao = database.reportDao()
            val firebaseSyncManager = FirebaseSyncManager()
            val storageRef = FirebaseStorage.getInstance().reference

            val unsyncedReports = reportDao.getUnsyncedReports()

            if (unsyncedReports.isEmpty()) {
                return@withContext Result.success()
            }

            var allSuccessful = true
            for (report in unsyncedReports) {
                try {
                    var imageUrl: String? = null
                    var audioUrl: String? = null

                    // 1. Upload Image if exists
                    if (!report.imageLocalPath.isNullOrEmpty()) {
                        val imageFile = File(report.imageLocalPath)
                        if (imageFile.exists()) {
                            val imageRef = storageRef.child("reports/images/${report.remoteId}.jpg")
                            
                            // Compress Image
                            val options = BitmapFactory.Options()
                            val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath, options)
                            
                            if (originalBitmap != null) {
                                val maxDimension = 1024
                                val width = originalBitmap.width
                                val height = originalBitmap.height
                                
                                val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                                    val ratio = maxDimension.toFloat() / max(width, height)
                                    val newWidth = (width * ratio).toInt()
                                    val newHeight = (height * ratio).toInt()
                                    Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                                } else {
                                    originalBitmap
                                }
                                
                                val baos = ByteArrayOutputStream()
                                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                                val data = baos.toByteArray()
                                
                                imageRef.putBytes(data).await()
                            } else {
                                imageRef.putFile(Uri.fromFile(imageFile)).await() // fallback
                            }
                            
                            imageUrl = imageRef.downloadUrl.await().toString()
                        }
                    }

                    // 2. Upload Audio if exists
                    if (!report.voiceNoteLocalPath.isNullOrEmpty()) {
                        val audioFile = File(report.voiceNoteLocalPath)
                        if (audioFile.exists()) {
                            val audioRef = storageRef.child("reports/audio/${report.remoteId}.aac")
                            audioRef.putFile(Uri.fromFile(audioFile)).await()
                            audioUrl = audioRef.downloadUrl.await().toString()
                        }
                    }

                    // 3. Sync metadata to Firestore
                    val syncResult = firebaseSyncManager.syncReportToFirestore(report, imageUrl, audioUrl)
                    if (syncResult.isSuccess) {
                        reportDao.updateSyncStatus(report.id, true)
                    } else {
                        allSuccessful = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    allSuccessful = false
                }
            }

            if (allSuccessful) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

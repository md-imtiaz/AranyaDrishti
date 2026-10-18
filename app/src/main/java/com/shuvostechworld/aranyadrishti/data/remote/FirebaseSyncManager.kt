package com.shuvostechworld.aranyadrishti.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.shuvostechworld.aranyadrishti.data.local.ReportEntity
import kotlinx.coroutines.tasks.await

class FirebaseSyncManager {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun syncReportToFirestore(
        report: ReportEntity,
        imageUrl: String?,
        audioUrl: String?
    ): Result<Unit> {
        return try {
            val reportData = hashMapOf(
                "id" to report.id,
                "remoteId" to report.remoteId,
                "category" to report.category,
                "urgency" to report.urgency,
                "locationLat" to report.locationLat,
                "locationLng" to report.locationLng,
                "descriptionText" to report.descriptionText,
                "voiceNoteLocalPath" to report.voiceNoteLocalPath,
                "imageLocalPath" to report.imageLocalPath,
                "timestamp" to report.timestamp,
                "isAnonymous" to report.isAnonymous,
                "imageUrl" to imageUrl,
                "audioUrl" to audioUrl
            )

            firestore.collection("reports")
                .document(report.remoteId)
                .set(reportData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deleteReport(remoteId: String): Result<Unit> {
        return try {
            // Delete document
            firestore.collection("reports").document(remoteId).delete().await()
            
            val storageRef = FirebaseStorage.getInstance().reference

            // Delete image
            try {
                storageRef.child("reports/images/$remoteId.jpg").delete().await()
            } catch (e: Exception) {
                // Ignore if not exists
            }
            
            // Delete audio
            try {
                storageRef.child("reports/audio/$remoteId.aac").delete().await()
            } catch (e: Exception) {
                // Ignore if not exists
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

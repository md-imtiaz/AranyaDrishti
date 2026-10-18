package com.shuvostechworld.aranyadrishti.data.local

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun signInAnonymouslyAndSync(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInAnonymously:success")
                    fetchAndSyncUserData(context, onComplete)
                } else {
                    Log.w(TAG, "signInAnonymously:failure", task.exception)
                    onComplete?.invoke(false)
                }
            }
        } else {
            fetchAndSyncUserData(context, onComplete)
        }
    }

    private fun fetchAndSyncUserData(context: Context, onComplete: ((Boolean) -> Unit)?) {
        val uid = auth.currentUser?.uid ?: return
        
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: ""
                    val points = document.getLong("gems")?.toInt() ?: 0
                    
                    if (name.isNotEmpty()) {
                        RewardManager.setUserName(context, name, syncToCloud = false)
                    }
                    if (points > 0) {
                        // Only sync locally if remote points are higher or equal (simple resolution)
                        val localPoints = RewardManager.getPoints(context)
                        if (points > localPoints) {
                            RewardManager.addPoints(context, points - localPoints, syncToCloud = false)
                        } else if (localPoints > points) {
                            // If local is higher (e.g. offline play then sync), push local to cloud
                            syncProgressToCloud(name.ifEmpty { RewardManager.getUserName(context) }, localPoints, RewardManager.getCurrentBadge(localPoints))
                        }
                    }
                    Log.d(TAG, "Fetched user data: Name=$name, Points=$points")
                } else {
                    Log.d(TAG, "No existing user document, sync local data to cloud")
                    val localName = RewardManager.getUserName(context)
                    val localPoints = RewardManager.getPoints(context)
                    if (localName.isNotEmpty() || localPoints > 0) {
                        syncProgressToCloud(localName, localPoints, RewardManager.getCurrentBadge(localPoints))
                    }
                }
                onComplete?.invoke(true)
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
                onComplete?.invoke(false)
            }
    }

    fun syncProgressToCloud(name: String, gems: Int, rank: String) {
        val uid = auth.currentUser?.uid ?: return
        
        val userMap = hashMapOf(
            "name" to name,
            "gems" to gems,
            "rank" to rank,
            "lastUpdated" to System.currentTimeMillis()
        )

        firestore.collection("users").document(uid)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully written!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error writing document", e)
            }
    }
}

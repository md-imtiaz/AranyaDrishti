package com.shuvostechworld.aranyadrishti.data.local

import android.content.Context
import android.content.SharedPreferences

object RewardManager {
    private const val PREFS_NAME = "eco_rewards_prefs"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_ECO_POINTS = "eco_points"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getUserName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NAME, "") ?: ""
    }

    fun setUserName(context: Context, name: String, syncToCloud: Boolean = true) {
        getPrefs(context).edit().putString(KEY_USER_NAME, name).apply()
        if (syncToCloud) {
            val currentPoints = getPoints(context)
            CloudSyncManager.syncProgressToCloud(name, currentPoints, getCurrentBadge(currentPoints))
        }
    }

    fun getPoints(context: Context): Int {
        return getPrefs(context).getInt(KEY_ECO_POINTS, 0)
    }

    fun addPoints(context: Context, amount: Int, syncToCloud: Boolean = true) {
        val currentPoints = getPoints(context)
        val newPoints = currentPoints + amount
        getPrefs(context).edit().putInt(KEY_ECO_POINTS, newPoints).apply()
        if (syncToCloud) {
            val name = getUserName(context)
            CloudSyncManager.syncProgressToCloud(name, newPoints, getCurrentBadge(newPoints))
        }
    }

    fun getCurrentBadge(points: Int): String {
        return when {
            points >= 100 -> "Forest Guardian 👑"
            points >= 50 -> "Wildlife Defender 🛡️"
            else -> "Nature Observer 🍃"
        }
    }
}

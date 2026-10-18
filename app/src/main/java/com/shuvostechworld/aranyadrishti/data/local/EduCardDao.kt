package com.shuvostechworld.aranyadrishti.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EduCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<EduCardEntity>)

    @Query("SELECT * FROM edu_cards")
    suspend fun getAllCards(): List<EduCardEntity>

    @Query("DELETE FROM edu_cards")
    suspend fun deleteAll()
}

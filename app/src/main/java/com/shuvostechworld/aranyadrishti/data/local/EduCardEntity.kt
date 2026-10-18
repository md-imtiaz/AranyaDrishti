package com.shuvostechworld.aranyadrishti.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "edu_cards")
data class EduCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topicName: String,
    val aiDescription: String,
    val imageUrl: String?
)

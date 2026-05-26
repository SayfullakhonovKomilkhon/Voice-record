package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "voice_profiles")
@JsonClass(generateAdapter = true)
data class VoiceProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: String, // "Секретарь (Вы)", "Коллега", "Клиент", "Партнер"
    val audioFilePath: String,
    val durationSeconds: Int,
    val phraseText: String,
    val registeredAt: Long = System.currentTimeMillis()
)

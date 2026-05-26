package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

enum class MeetingStatus {
    RECORDING,
    TRANSCRIPTION_PENDING,
    COMPLETED,
    FAILED
}

@JsonClass(generateAdapter = true)
data class MeetingTask(
    val assignedTo: String,
    val taskText: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val audioFilePath: String? = null,
    val transcript: List<Utterance>? = null,
    val summaryOverview: String? = null,
    val summaryTopics: List<String>? = null,
    val summaryDecisions: List<String>? = null,
    val summaryTasks: List<MeetingTask>? = null,
    val status: MeetingStatus = MeetingStatus.COMPLETED,
    val participantA: String = "Участник 1",
    val participantB: String = "Участник 2",
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList()
)

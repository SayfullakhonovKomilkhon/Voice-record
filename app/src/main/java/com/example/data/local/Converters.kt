package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.MeetingStatus
import com.example.data.model.MeetingTask
import com.example.data.model.Utterance
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // 1. Utterance list converters
    private val utteranceListType = Types.newParameterizedType(List::class.java, Utterance::class.java)
    private val utteranceListAdapter = moshi.adapter<List<Utterance>>(utteranceListType)

    @TypeConverter
    fun fromUtteranceList(value: List<Utterance>?): String? {
        return value?.let { utteranceListAdapter.toJson(it) }
    }

    @TypeConverter
    fun toUtteranceList(value: String?): List<Utterance>? {
        return value?.let { utteranceListAdapter.fromJson(it) }
    }

    // 2. String list converters
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { stringListAdapter.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let { stringListAdapter.fromJson(it) }
    }

    // 3. MeetingTask list converters
    private val taskListType = Types.newParameterizedType(List::class.java, MeetingTask::class.java)
    private val taskListAdapter = moshi.adapter<List<MeetingTask>>(taskListType)

    @TypeConverter
    fun fromTaskList(value: List<MeetingTask>?): String? {
        return value?.let { taskListAdapter.toJson(it) }
    }

    @TypeConverter
    fun toTaskList(value: String?): List<MeetingTask>? {
        return value?.let { taskListAdapter.fromJson(it) }
    }

    // 4. MeetingStatus converters
    @TypeConverter
    fun fromStatus(value: MeetingStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toStatus(value: String?): MeetingStatus? {
        return value?.let { MeetingStatus.valueOf(it) }
    }
}

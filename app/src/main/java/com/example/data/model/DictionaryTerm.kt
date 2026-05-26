package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "dictionary_terms")
@JsonClass(generateAdapter = true)
data class DictionaryTerm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val term: String,
    val explanation: String = ""
)

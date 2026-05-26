package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Utterance(
    val speaker: String,
    val text: String,
    val timestampMs: Long = 0L
)

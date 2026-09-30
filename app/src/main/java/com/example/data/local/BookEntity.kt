package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val totalPages: Int,
    val coverDrawableRes: Int? = null,
    val coverImagePath: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val selectedVoiceId: String = "male_arthur",
    val playbackSpeed: Float = 1.0f,
    val voicePitch: Float = 1.0f,
    val pauseMultiplier: Float = 1.0f,
    val currentPlayingPage: Int = 1,
    val currentPlaybackPositionMs: Int = 0,
    val singleFileAudioPath: String? = null,
    val isSingleFileReady: Boolean = false,
    val singleFileTotalDurationMs: Long = 0L,
    val singleFileSizeBytes: Long = 0L
)

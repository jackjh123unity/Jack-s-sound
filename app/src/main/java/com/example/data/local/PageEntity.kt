package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    indices = [Index(value = ["bookId", "pageNumber"], unique = true)]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val pageNumber: Int,
    val chapterTitle: String = "",
    val rawText: String,
    val enchantedText: String? = null,
    val audioFilePath: String? = null,
    val audioDurationMs: Long = 0L,
    val isAudioGenerated: Boolean = false,
    val wordCount: Int = 0
)

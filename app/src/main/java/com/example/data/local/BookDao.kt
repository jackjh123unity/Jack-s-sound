package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    fun getBookByIdFlow(bookId: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getBookById(bookId: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: Long)

    @Query("SELECT * FROM pages WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getPagesForBook(bookId: Long): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE bookId = :bookId ORDER BY pageNumber ASC")
    suspend fun getPagesForBookSync(bookId: Long): List<PageEntity>

    @Query("SELECT * FROM pages WHERE bookId = :bookId AND pageNumber = :pageNumber LIMIT 1")
    suspend fun getPageByNumber(bookId: Long, pageNumber: Int): PageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>)

    @Update
    suspend fun updatePage(page: PageEntity)

    @Query("DELETE FROM pages WHERE bookId = :bookId")
    suspend fun deletePagesForBook(bookId: Long)

    @Query("UPDATE books SET selectedVoiceId = :voiceId WHERE id = :bookId")
    suspend fun updateVoice(bookId: Long, voiceId: String)

    @Query("UPDATE books SET playbackSpeed = :speed WHERE id = :bookId")
    suspend fun updatePlaybackSpeed(bookId: Long, speed: Float)

    @Query("UPDATE books SET voicePitch = :pitch WHERE id = :bookId")
    suspend fun updateVoicePitch(bookId: Long, pitch: Float)

    @Query("UPDATE books SET currentPlayingPage = :page, currentPlaybackPositionMs = :positionMs WHERE id = :bookId")
    suspend fun updatePlaybackProgress(bookId: Long, page: Int, positionMs: Int)

    @Query("UPDATE books SET singleFileAudioPath = :path, isSingleFileReady = :ready, singleFileTotalDurationMs = :durationMs, singleFileSizeBytes = :sizeBytes WHERE id = :bookId")
    suspend fun updateSingleAudioFile(bookId: Long, path: String?, ready: Boolean, durationMs: Long, sizeBytes: Long)
}

package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.R
import com.example.audio.AudioExportUtil
import com.example.audio.AudiobookSynthesizer
import com.example.audio.GeminiAudioEnchanter
import com.example.data.local.BookDao
import com.example.data.local.BookEntity
import com.example.data.local.PageEntity
import com.example.data.model.VoiceCatalog
import com.example.document.DocumentExtractor
import com.example.document.ExtractedDocumentResult
import com.example.pdf.PdfExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File

class BookRepository(
    private val context: Context,
    private val bookDao: BookDao,
    private val synthesizer: AudiobookSynthesizer
) {

    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getBookFlow(bookId: Long): Flow<BookEntity?> = bookDao.getBookByIdFlow(bookId)

    fun getPagesFlow(bookId: Long): Flow<List<PageEntity>> = bookDao.getPagesForBook(bookId)

    suspend fun getBook(bookId: Long): BookEntity? = bookDao.getBookById(bookId)

    suspend fun checkAndSeedInitialBooks() = withContext(Dispatchers.IO) {
        val existing = bookDao.getAllBooks().firstOrNull()
        if (!existing.isNullOrEmpty()) return@withContext

        // Book 1: The Whispering Woods of Eldoria
        val book1 = BookEntity(
            title = "The Whispering Woods of Eldoria",
            author = "Lord Valen Drake",
            totalPages = 4,
            coverDrawableRes = R.drawable.audiobook_hero_1790734972693,
            selectedVoiceId = "male_arthur",
            playbackSpeed = 1.0f
        )
        val book1Id = bookDao.insertBook(book1)

        val book1Pages = listOf(
            PageEntity(
                bookId = book1Id,
                pageNumber = 1,
                rawText = """
                    The ancient stone gates had remained sealed for ten thousand years.
                    Beneath the crimson moon, frost crept slowly across the obsidian carvings.
                    "Do you hear it?" whispered Aaron, his hand trembling upon the hilt of his runic broadsword.
                    From deep within the subterranean caverns beneath the mountain, a low rumbling awakened. It was not the wind. It was the rhythm of colossal dragon lungs drawing their first breath in an age.
                    "Steady your blade," Lyra answered softly. "If we flee tonight, there will be no kingdom left to defend tomorrow."
                """.trimIndent(),
                enchantedText = """
                    [with quiet wonder] The ancient stone gates had remained sealed for ten thousand years.
                    [short pause] Beneath the crimson moon, frost crept slowly across the obsidian carvings.
                    [whispering] "Do you hear it?" [short pause] whispered Aaron, his hand trembling upon the hilt of his runic broadsword.
                    [deep breath] From deep within the subterranean caverns beneath the mountain, [low, rumbling gravel register] a low rumbling awakened. It was not the wind. [short pause] It was the rhythm of colossal dragon lungs drawing their first breath in an age.
                    [whispering] "Steady your blade," [short pause] Lyra answered softly. [with growing intensity] "If we flee tonight, there will be no kingdom left to defend tomorrow."
                """.trimIndent()
            ),
            PageEntity(
                bookId = book1Id,
                pageNumber = 2,
                rawText = """
                    They stepped across the threshold, their lanterns carving arcs of amber light into the perpetual gloom.
                    The hallway stretched forward into impossible infinity. Upon the limestone walls, murals of long-vanished star-priests depicted the Great Fall.
                    "Look there," Aaron pointed with the tip of his torch. "The third sigil is glowing."
                    A sudden gust of crystalline air rushed past them, extinguishing the flame. Complete, suffocating darkness engulfed the corridor.
                    Then, ten paces ahead, two golden embers ignited in the shadows.
                """.trimIndent(),
                enchantedText = """
                    [cautious and solemn] They stepped across the threshold, their lanterns carving arcs of amber light into the perpetual gloom.
                    [short pause] The hallway stretched forward into impossible infinity. [short pause] Upon the limestone walls, murals of long-vanished star-priests depicted the Great Fall.
                    [with growing intensity] "Look there," [short pause] Aaron pointed with the tip of his torch. [deep breath] "The third sigil is glowing."
                    [sudden sharp gasp] A sudden gust of crystalline air rushed past them, extinguishing the flame! [long pause]
                    Complete, suffocating darkness engulfed the corridor.
                    [whispering] Then, ten paces ahead... [deep breath] two golden embers ignited in the shadows.
                """.trimIndent()
            ),
            PageEntity(
                bookId = book1Id,
                pageNumber = 3,
                rawText = """
                    "Do not draw your steel," Lyra commanded in an urgent whisper. "It does not hunt by sight."
                    The golden embers drifted downward, settling just above the stone flags. A sound like grinding millstones vibrated through Aaron's boots.
                    "I am the Sentinel of the Mists," a voice echoed, neither young nor old, yet carrying the cadence of ringing bronze. "State your vow, wanderers of the upper realms. What seek ye in the Hall of Forgotten Oaths?"
                    Aaron took a steadying breath and raised his medallion high.
                """.trimIndent(),
                enchantedText = """
                    [whispering] "Do not draw your steel," [short pause] Lyra commanded in an urgent whisper. [short pause] "It does not hunt by sight."
                    [deep breath] The golden embers drifted downward, settling just above the stone flags. [low, rumbling gravel register] A sound like grinding millstones vibrated through Aaron's boots.
                    [suddenly deep, booming tone] "I am the Sentinel of the Mists," [long pause] a voice echoed, neither young nor old, yet carrying the cadence of ringing bronze.
                    [with slow majestic authority] "State your vow, wanderers of the upper realms. [deep breath] What seek ye in the Hall of Forgotten Oaths?"
                    [short pause] Aaron took a steadying breath and raised his medallion high.
                """.trimIndent()
            ),
            PageEntity(
                bookId = book1Id,
                pageNumber = 4,
                rawText = """
                    "We seek only the ember to relight the Sun-Beacon of Valdora," Aaron declared, his voice echoing into the vaults.
                    The sentinel lingered in silence for three agonizing heartbeats. Then, the chamber filled with a radiant sapphire glow.
                    "Your intention ringeth true, son of the northern march," the guardian intoned. "Take the flame, but remember this: fire given in mercy can still consume those who lack the courage to carry it."
                    The path ahead opened into a terrace overlooking starlit cloudscapes. Their quest had only just begun.
                """.trimIndent(),
                enchantedText = """
                    [with bold hero's conviction] "We seek only the ember to relight the Sun-Beacon of Valdora," [short pause] Aaron declared, his voice echoing into the vaults.
                    [long pause] The sentinel lingered in silence for three agonizing heartbeats. [deep breath]
                    Then, the chamber filled with a radiant sapphire glow!
                    [warm ancient cadence] "Your intention ringeth true, son of the northern march," [short pause] the guardian intoned. [short pause] "Take the flame, but remember this: [deep breath] [whispering] fire given in mercy can still consume those who lack the courage to carry it."
                    [triumphant warmth] The path ahead opened into a terrace overlooking starlit cloudscapes. [short pause] Their quest had only just begun.
                """.trimIndent()
            )
        )
        bookDao.insertPages(book1Pages)

        // Book 2: Chronicles of the Shadow Alchemist
        val book2 = BookEntity(
            title = "Chronicles of the Shadow Alchemist",
            author = "Morgana Le Fey",
            totalPages = 3,
            coverDrawableRes = R.drawable.audiobook_hero_1790734972693,
            selectedVoiceId = "female_eleanor",
            playbackSpeed = 1.0f
        )
        val book2Id = bookDao.insertBook(book2)

        val book2Pages = listOf(
            PageEntity(
                bookId = book2Id,
                pageNumber = 1,
                rawText = """
                    Glass retorts bubbled in the dimly lit cellar beneath the apothecary.
                    In the vial before Corin, a droplet of liquid moonlight spun against the laws of gravity.
                    "If the guild masters discover this formula," Corin murmured, "they will brand it heresy."
                    Yet outside his window, the bells of the cathedral tolled midnight, counting down the hours until the royal execution.
                """.trimIndent(),
                enchantedText = """
                    [velvety and mysterious] Glass retorts bubbled in the dimly lit cellar beneath the apothecary.
                    [short pause] In the vial before Corin, [whispering] a droplet of liquid moonlight spun against the laws of gravity.
                    [deep breath] [anxiously] "If the guild masters discover this formula," [short pause] Corin murmured, [short pause] "they will brand it heresy."
                    [long pause] Yet outside his window, the heavy bells of the cathedral tolled midnight, counting down the hours until the royal execution.
                """.trimIndent()
            ),
            PageEntity(
                bookId = book2Id,
                pageNumber = 2,
                rawText = """
                    He corked the phial and wrapped it in dark oiled silk.
                    Every cobblestone of the old town seemed damp with impending revolution. Cloaked figures slipped through the foggy alleys like specters.
                    A knocking sounded at his door—three quick taps, followed by the pause of the silent dagger.
                    "Is it ready?" a voice hissed through the keyhole.
                """.trimIndent(),
                enchantedText = """
                    [suspenseful pacing] He corked the phial and wrapped it in dark oiled silk.
                    [short pause] Every cobblestone of the old town seemed damp with impending revolution. [deep breath] Cloaked figures slipped through the foggy alleys like specters.
                    [sudden tension] A knocking sounded at his door—three quick taps, [short pause] followed by the pause of the silent dagger.
                    [whispering] "Is it ready?" [short pause] a voice hissed through the keyhole.
                """.trimIndent()
            ),
            PageEntity(
                bookId = book2Id,
                pageNumber = 3,
                rawText = """
                    "It is done," Corin whispered back, unlatching the iron bolt.
                    The door swung open to reveal the Countess of Ravenscroft, her eyes gleaming with defiant triumph.
                    "Then by dawn," she smiled, "the tyrant's crown shall turn to rust."
                    Together, they stepped out into the misty labyrinth of the capital.
                """.trimIndent(),
                enchantedText = """
                    [whispering] "It is done," [short pause] Corin whispered back, unlatching the heavy iron bolt.
                    [deep breath] The door swung open to reveal the Countess of Ravenscroft, [with growing intensity] her eyes gleaming with defiant triumph.
                    [regal whispering] "Then by dawn," [short pause] she smiled, [short pause] "the tyrant's crown shall turn to rust."
                    [majestic outro] Together, they stepped out into the misty labyrinth of the capital.
                """.trimIndent()
            )
        )
        bookDao.insertPages(book2Pages)
    }

    suspend fun importPdf(uri: Uri): Long = importDocument(uri)

    suspend fun importDocument(uri: Uri): Long = withContext(Dispatchers.IO) {
        val extracted = DocumentExtractor.extractFromUri(context, uri)
        saveExtractedDocument(extracted)
    }

    suspend fun importPastedText(title: String, author: String, text: String): Long = withContext(Dispatchers.IO) {
        val extracted = DocumentExtractor.extractFromText(title, author, text)
        saveExtractedDocument(extracted)
    }

    suspend fun importGoogleDocUrl(url: String): Long? = withContext(Dispatchers.IO) {
        val extracted = DocumentExtractor.extractFromGoogleDocUrl(url) ?: return@withContext null
        saveExtractedDocument(extracted)
    }

    private suspend fun saveExtractedDocument(extracted: ExtractedDocumentResult): Long {
        val book = BookEntity(
            title = extracted.title,
            author = extracted.author,
            totalPages = extracted.totalPages,
            coverImagePath = extracted.coverImagePath,
            selectedVoiceId = "male_arthur",
            playbackSpeed = 1.0f
        )
        val bookId = bookDao.insertBook(book)

        val pages = extracted.pagesText.mapIndexed { idx, text ->
            val pageNum = idx + 1
            val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
            PageEntity(
                bookId = bookId,
                pageNumber = pageNum,
                rawText = text,
                enchantedText = GeminiAudioEnchanter.heuristicEnchant(text, VoiceCatalog.getById("male_arthur")),
                wordCount = words.size
            )
        }
        bookDao.insertPages(pages)
        return bookId
    }

    suspend fun deleteBook(bookId: Long) = withContext(Dispatchers.IO) {
        val pages = bookDao.getPagesForBookSync(bookId)
        for (page in pages) {
            page.audioFilePath?.let { path ->
                try { File(path).delete() } catch (e: Exception) {}
            }
        }
        val book = bookDao.getBookById(bookId)
        book?.singleFileAudioPath?.let { path ->
            try { File(path).delete() } catch (e: Exception) {}
        }
        book?.coverImagePath?.let { path ->
            try { File(path).delete() } catch (e: Exception) {}
        }
        bookDao.deletePagesForBook(bookId)
        bookDao.deleteBookById(bookId)
    }

    suspend fun updateVoice(bookId: Long, voiceId: String) = withContext(Dispatchers.IO) {
        bookDao.updateVoice(bookId, voiceId)
    }

    suspend fun updatePlaybackSpeed(bookId: Long, speed: Float) = withContext(Dispatchers.IO) {
        bookDao.updatePlaybackSpeed(bookId, speed)
    }

    suspend fun updateVoicePitch(bookId: Long, pitch: Float) = withContext(Dispatchers.IO) {
        bookDao.updateVoicePitch(bookId, pitch)
    }

    suspend fun updatePlaybackProgress(bookId: Long, page: Int, positionMs: Int) = withContext(Dispatchers.IO) {
        bookDao.updatePlaybackProgress(bookId, page, positionMs)
    }

    /**
     * Downloads 1 chapter at a time as audio and exports it to the device.
     */
    suspend fun downloadSingleChapterAudio(
        bookId: Long,
        pageNumber: Int,
        voiceId: String,
        speed: Float,
        pitch: Float = 1.0f,
        onProgress: (progress: Float, statusText: String) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val book = bookDao.getBookById(bookId) ?: return@withContext null
        val page = bookDao.getPageByNumber(bookId, pageNumber) ?: return@withContext null
        val voice = VoiceCatalog.getById(voiceId)

        onProgress(0.15f, "Preparing Chapter $pageNumber with ${voice.name}...")

        val audioDir = File(context.filesDir, "audiobooks/book_$bookId")
        audioDir.mkdirs()
        val pageAudioFile = File(audioDir, "Chapter_${pageNumber}_${voice.id}.wav")

        val scriptText = page.enchantedText?.takeIf { it.isNotBlank() }
            ?: GeminiAudioEnchanter.heuristicEnchant(page.rawText, voice)

        onProgress(0.40f, "Synthesizing Chapter $pageNumber voice audio...")
        val success = synthesizer.synthesizePageToFile(scriptText, pageAudioFile, voice, speed, pitch)

        if (success && pageAudioFile.exists()) {
            val duration = getAudioDurationMs(pageAudioFile)
            val updatedPage = page.copy(
                audioFilePath = pageAudioFile.absolutePath,
                audioDurationMs = duration,
                isAudioGenerated = true
            )
            bookDao.updatePage(updatedPage)

            onProgress(0.85f, "Exporting to device audio storage...")
            val safeBookTitle = book.title.replace(Regex("[^a-zA-Z0-9_-]"), " ").trim()
            val exportName = "${safeBookTitle} - Chapter $pageNumber (${voice.name})"
            AudioExportUtil.exportToPublicMusic(context, pageAudioFile, exportName)

            onProgress(1.0f, "Chapter $pageNumber downloaded successfully!")
            pageAudioFile
        } else {
            null
        }
    }

    /**
     * Synthesizes audio for a single page and stores it in app internal storage.
     */
    suspend fun generatePageAudio(
        bookId: Long,
        pageNumber: Int,
        voiceId: String,
        speed: Float,
        pitch: Float = 1.0f
    ): PageEntity? = withContext(Dispatchers.IO) {
        val page = bookDao.getPageByNumber(bookId, pageNumber) ?: return@withContext null
        val voice = VoiceCatalog.getById(voiceId)

        val audioDir = File(context.filesDir, "audiobooks/book_$bookId")
        audioDir.mkdirs()
        val outputFile = File(audioDir, "page_${pageNumber}_${voice.id}.wav")

        val scriptText = page.enchantedText?.takeIf { it.isNotBlank() } ?: page.rawText
        val success = synthesizer.synthesizePageToFile(scriptText, outputFile, voice, speed, pitch)

        if (success && outputFile.exists()) {
            val duration = getAudioDurationMs(outputFile)
            val updatedPage = page.copy(
                audioFilePath = outputFile.absolutePath,
                audioDurationMs = duration,
                isAudioGenerated = true
            )
            bookDao.updatePage(updatedPage)
            updatedPage
        } else {
            null
        }
    }

    /**
     * Fulfills: "Make it so I can download every page as audio in a single file".
     * Synthesizes all pages sequentially and stitches them into a unified complete audiobook file.
     */
    suspend fun generateSingleFileAudiobook(
        bookId: Long,
        voiceId: String,
        speed: Float,
        pitch: Float = 1.0f,
        onProgress: (currentPage: Int, totalPages: Int, statusText: String) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val book = bookDao.getBookById(bookId) ?: return@withContext null
        val pages = bookDao.getPagesForBookSync(bookId)
        if (pages.isEmpty()) return@withContext null

        val voice = VoiceCatalog.getById(voiceId)
        val generatedWavFiles = mutableListOf<File>()

        for ((index, page) in pages.withIndex()) {
            val pageNum = page.pageNumber
            onProgress(pageNum, pages.size, "Synthesizing page $pageNum of ${pages.size} (${voice.name})...")

            val audioDir = File(context.filesDir, "audiobooks/book_$bookId")
            audioDir.mkdirs()
            val pageAudioFile = File(audioDir, "page_${pageNum}_${voice.id}.wav")

            if (!pageAudioFile.exists() || pageAudioFile.length() < 100) {
                val script = page.enchantedText ?: GeminiAudioEnchanter.heuristicEnchant(page.rawText, voice)
                val success = synthesizer.synthesizePageToFile(script, pageAudioFile, voice, speed, pitch)
                if (success && pageAudioFile.exists()) {
                    val duration = getAudioDurationMs(pageAudioFile)
                    bookDao.updatePage(
                        page.copy(
                            audioFilePath = pageAudioFile.absolutePath,
                            audioDurationMs = duration,
                            isAudioGenerated = true
                        )
                    )
                }
                delay(600)
            }

            if (pageAudioFile.exists()) {
                generatedWavFiles.add(pageAudioFile)
            }
        }

        if (generatedWavFiles.isEmpty()) return@withContext null

        onProgress(pages.size, pages.size, "Merging all ${generatedWavFiles.size} pages into single audiobook file...")

        // Destination for complete single audio file
        val outputDir = File(context.filesDir, "audiobooks/downloads")
        outputDir.mkdirs()
        val safeTitle = book.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val finalSingleAudioFile = File(outputDir, "${safeTitle}_${voice.id}_Complete.wav")

        val merged = synthesizer.mergeWavFiles(generatedWavFiles, finalSingleAudioFile)
        if (merged && finalSingleAudioFile.exists()) {
            val totalDuration = getAudioDurationMs(finalSingleAudioFile)
            val sizeBytes = finalSingleAudioFile.length()

            bookDao.updateSingleAudioFile(
                bookId = bookId,
                path = finalSingleAudioFile.absolutePath,
                ready = true,
                durationMs = totalDuration,
                sizeBytes = sizeBytes
            )

            // Export to device public music
            val safeBookTitle = book.title.replace(Regex("[^a-zA-Z0-9_-]"), " ").trim()
            val exportName = "${safeBookTitle} - Complete Audiobook (${voice.name})"
            AudioExportUtil.exportToPublicMusic(context, finalSingleAudioFile, exportName)

            onProgress(pages.size, pages.size, "Ready! Complete audiobook downloaded.")
            finalSingleAudioFile
        } else {
            null
        }
    }

    suspend fun enchantPageText(bookId: Long, pageNumber: Int, voiceId: String) = withContext(Dispatchers.IO) {
        val page = bookDao.getPageByNumber(bookId, pageNumber) ?: return@withContext
        val voice = VoiceCatalog.getById(voiceId)
        val enchanted = GeminiAudioEnchanter.enchantScript(page.rawText, voice)
        bookDao.updatePage(page.copy(enchantedText = enchanted))
    }

    private fun getAudioDurationMs(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}

package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerManager
import com.example.audio.AudiobookSynthesizer
import com.example.audio.PlaybackState
import com.example.data.local.BookEntity
import com.example.data.local.PageEntity
import com.example.data.local.StoryDatabase
import com.example.data.model.VoiceCatalog
import com.example.data.model.VoiceModel
import com.example.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class AppNavScreen {
    LIBRARY,
    READER,
    PLAYER
}

enum class DownloadMode {
    ALL_CHAPTERS_SINGLE_FILE,
    SINGLE_CHAPTER
}

class StoryCastViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StoryDatabase.getInstance(application)
    private val synthesizer = AudiobookSynthesizer(application)
    val playerManager = AudioPlayerManager(application)
    val repository = BookRepository(application, database.bookDao(), synthesizer)

    val allBooks: StateFlow<List<BookEntity>> = repository.allBooks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentScreen = MutableStateFlow(AppNavScreen.LIBRARY)
    val currentScreen: StateFlow<AppNavScreen> = _currentScreen.asStateFlow()

    private val _activeBookId = MutableStateFlow<Long?>(null)
    val activeBookId: StateFlow<Long?> = _activeBookId.asStateFlow()

    private val _activeBook = MutableStateFlow<BookEntity?>(null)
    val activeBook: StateFlow<BookEntity?> = _activeBook.asStateFlow()

    private val _activePages = MutableStateFlow<List<PageEntity>>(emptyList())
    val activePages: StateFlow<List<PageEntity>> = _activePages.asStateFlow()

    private val _currentPageNumber = MutableStateFlow(1)
    val currentPageNumber: StateFlow<Int> = _currentPageNumber.asStateFlow()

    private val _selectedVoice = MutableStateFlow<VoiceModel>(VoiceCatalog.ALL_VOICES.first())
    val selectedVoice: StateFlow<VoiceModel> = _selectedVoice.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _voicePitch = MutableStateFlow(1.0f)
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState

    // Download modal states
    private val _downloadMode = MutableStateFlow(DownloadMode.ALL_CHAPTERS_SINGLE_FILE)
    val downloadMode: StateFlow<DownloadMode> = _downloadMode.asStateFlow()

    private val _downloadingChapterNum = MutableStateFlow<Int?>(null)
    val downloadingChapterNum: StateFlow<Int?> = _downloadingChapterNum.asStateFlow()

    private val _isSingleFileDownloading = MutableStateFlow(false)
    val isSingleFileDownloading: StateFlow<Boolean> = _isSingleFileDownloading.asStateFlow()

    private val _singleFileProgress = MutableStateFlow(0f)
    val singleFileProgress: StateFlow<Float> = _singleFileProgress.asStateFlow()

    private val _singleFileStatusText = MutableStateFlow("")
    val singleFileStatusText: StateFlow<String> = _singleFileStatusText.asStateFlow()

    private val _completedSingleFile = MutableStateFlow<File?>(null)
    val completedSingleFile: StateFlow<File?> = _completedSingleFile.asStateFlow()

    private val _showDownloadDialog = MutableStateFlow(false)
    val showDownloadDialog: StateFlow<Boolean> = _showDownloadDialog.asStateFlow()

    private val _showVoiceSheet = MutableStateFlow(false)
    val showVoiceSheet: StateFlow<Boolean> = _showVoiceSheet.asStateFlow()

    private val _isEnchanting = MutableStateFlow(false)
    val isEnchanting: StateFlow<Boolean> = _isEnchanting.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialBooks()
        }
    }

    fun navigateTo(screen: AppNavScreen) {
        _currentScreen.value = screen
    }

    fun selectBook(bookId: Long) {
        _activeBookId.value = bookId
        viewModelScope.launch {
            val book = repository.getBook(bookId)
            _activeBook.value = book
            book?.let {
                _selectedVoice.value = VoiceCatalog.getById(it.selectedVoiceId)
                _playbackSpeed.value = it.playbackSpeed
                _voicePitch.value = it.voicePitch
                _currentPageNumber.value = it.currentPlayingPage
            }

            repository.getPagesFlow(bookId).collect { pages ->
                _activePages.value = pages
            }
        }
    }

    fun importPdfUri(uri: Uri) = importDocumentUri(uri)

    fun importDocumentUri(uri: Uri) {
        viewModelScope.launch {
            val newBookId = repository.importDocument(uri)
            selectBook(newBookId)
            navigateTo(AppNavScreen.READER)
        }
    }

    fun importPastedDoc(title: String, author: String, content: String) {
        viewModelScope.launch {
            val newBookId = repository.importPastedText(title, author, content)
            selectBook(newBookId)
            navigateTo(AppNavScreen.READER)
        }
    }

    fun importGoogleDocLink(url: String, onFail: () -> Unit) {
        viewModelScope.launch {
            val newBookId = repository.importGoogleDocUrl(url)
            if (newBookId != null) {
                selectBook(newBookId)
                navigateTo(AppNavScreen.READER)
            } else {
                onFail()
            }
        }
    }

    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            if (_activeBookId.value == bookId) {
                playerManager.stop()
                _activeBookId.value = null
                _activeBook.value = null
                _activePages.value = emptyList()
                _currentScreen.value = AppNavScreen.LIBRARY
            }
            repository.deleteBook(bookId)
        }
    }

    fun selectVoice(voice: VoiceModel) {
        val previousVoice = _selectedVoice.value
        _selectedVoice.value = voice
        _showVoiceSheet.value = false
        val bookId = _activeBookId.value ?: return
        viewModelScope.launch {
            if (previousVoice.id != voice.id && playerManager.playbackState.value.isPlaying) {
                playerManager.stop()
            }
            repository.updateVoice(bookId, voice.id)
            _activeBook.value = repository.getBook(bookId)
        }
    }

    fun auditionVoice(
        voice: VoiceModel,
        speed: Float = _playbackSpeed.value,
        pitch: Float = _voicePitch.value
    ) {
        viewModelScope.launch {
            synthesizer.previewVoice(voice, speed, pitch)
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        playerManager.setSpeed(speed)
        val bookId = _activeBookId.value ?: return
        viewModelScope.launch {
            repository.updatePlaybackSpeed(bookId, speed)
        }
    }

    fun setPitch(pitch: Float) {
        _voicePitch.value = pitch
        playerManager.setPitch(pitch)
        val bookId = _activeBookId.value ?: return
        viewModelScope.launch {
            repository.updateVoicePitch(bookId, pitch)
        }
    }

    fun openVoiceSelector() {
        _showVoiceSheet.value = true
    }

    fun closeVoiceSelector() {
        _showVoiceSheet.value = false
    }

    fun setCurrentPage(pageNum: Int) {
        _currentPageNumber.value = pageNum
        val bookId = _activeBookId.value ?: return
        viewModelScope.launch {
            repository.updatePlaybackProgress(bookId, pageNum, 0)
        }
    }

    fun playCurrentPageAudio() {
        val bookId = _activeBookId.value ?: return
        val pageNum = _currentPageNumber.value
        val speed = _playbackSpeed.value
        val pitch = _voicePitch.value
        val voiceId = _selectedVoice.value.id

        viewModelScope.launch {
            val pages = _activePages.value
            val targetPage = pages.firstOrNull { it.pageNumber == pageNum } ?: return@launch

            // Check if audio file matches currently selected voice
            val expectedSuffix = "_${voiceId}.wav"
            val hasValidAudio = targetPage.isAudioGenerated &&
                    targetPage.audioFilePath != null &&
                    targetPage.audioFilePath.endsWith(expectedSuffix) &&
                    File(targetPage.audioFilePath).exists()

            val audioPath = if (hasValidAudio) {
                targetPage.audioFilePath
            } else {
                _singleFileStatusText.value = "Generating natural audio for page $pageNum with ${_selectedVoice.value.name}..."
                val genPage = repository.generatePageAudio(bookId, pageNum, voiceId, speed, pitch)
                genPage?.audioFilePath
            }

            if (audioPath != null) {
                playerManager.playAudioFile(
                    bookId = bookId,
                    pageNumber = pageNum,
                    audioPath = audioPath,
                    isSingleFileMode = false,
                    speed = speed,
                    pitch = pitch,
                    onFinished = {
                        // Auto advance to next page if available
                        if (pageNum < pages.size) {
                            setCurrentPage(pageNum + 1)
                            playCurrentPageAudio()
                        }
                    }
                )
            }
        }
    }

    /**
     * Fulfills: "downloads every chapter in audio and puts it in one single file"
     */
    fun startSingleFileDownload(bookId: Long) {
        _downloadMode.value = DownloadMode.ALL_CHAPTERS_SINGLE_FILE
        _downloadingChapterNum.value = null
        _showDownloadDialog.value = true
        _isSingleFileDownloading.value = true
        _singleFileProgress.value = 0f
        _singleFileStatusText.value = "Preparing all chapters for single-file synthesis..."
        _completedSingleFile.value = null

        viewModelScope.launch {
            val file = repository.generateSingleFileAudiobook(
                bookId = bookId,
                voiceId = _selectedVoice.value.id,
                speed = _playbackSpeed.value,
                pitch = _voicePitch.value,
                onProgress = { current, total, status ->
                    _singleFileProgress.value = if (total > 0) current.toFloat() / total else 0f
                    _singleFileStatusText.value = status
                }
            )

            _isSingleFileDownloading.value = false
            _completedSingleFile.value = file
            _activeBook.value = repository.getBook(bookId)
        }
    }

    /**
     * Fulfills: "downloads 1 chapter at a time"
     */
    fun startChapterDownload(bookId: Long, chapterNumber: Int) {
        _downloadMode.value = DownloadMode.SINGLE_CHAPTER
        _downloadingChapterNum.value = chapterNumber
        _showDownloadDialog.value = true
        _isSingleFileDownloading.value = true
        _singleFileProgress.value = 0.1f
        _singleFileStatusText.value = "Synthesizing Chapter $chapterNumber with ${_selectedVoice.value.name}..."
        _completedSingleFile.value = null

        viewModelScope.launch {
            val file = repository.downloadSingleChapterAudio(
                bookId = bookId,
                pageNumber = chapterNumber,
                voiceId = _selectedVoice.value.id,
                speed = _playbackSpeed.value,
                pitch = _voicePitch.value,
                onProgress = { progress, status ->
                    _singleFileProgress.value = progress
                    _singleFileStatusText.value = status
                }
            )

            _isSingleFileDownloading.value = false
            _completedSingleFile.value = file
            // Refresh active book and active pages so UI updates immediately
            _activeBook.value = repository.getBook(bookId)
            repository.getPagesFlow(bookId).firstOrNull()?.let {
                _activePages.value = it
            }
        }
    }

    fun playSingleFileAudio(file: File) = playDownloadedAudio(file)

    fun playDownloadedAudio(file: File) {
        val bookId = _activeBookId.value ?: return
        val isSingleFile = _downloadMode.value == DownloadMode.ALL_CHAPTERS_SINGLE_FILE
        val pageNum = if (isSingleFile) 1 else (_downloadingChapterNum.value ?: _currentPageNumber.value)
        playerManager.playAudioFile(
            bookId = bookId,
            pageNumber = pageNum,
            audioPath = file.absolutePath,
            isSingleFileMode = isSingleFile,
            speed = _playbackSpeed.value,
            pitch = _voicePitch.value
        )
        navigateTo(AppNavScreen.PLAYER)
    }

    fun dismissDownloadDialog() {
        _showDownloadDialog.value = false
    }

    fun togglePlayPause() {
        if (playbackState.value.currentAudioPath == null) {
            playCurrentPageAudio()
        } else {
            playerManager.togglePlayPause()
        }
    }

    fun seekTo(positionMs: Int) {
        playerManager.seekTo(positionMs)
    }

    fun skipForward() {
        playerManager.skipForward(15000)
    }

    fun skipBackward() {
        playerManager.skipBackward(15000)
    }

    fun setSleepTimer(minutes: Int?) {
        playerManager.setSleepTimer(minutes)
    }

    fun enchantCurrentPage() {
        val bookId = _activeBookId.value ?: return
        val pageNum = _currentPageNumber.value
        val voiceId = _selectedVoice.value.id

        viewModelScope.launch {
            _isEnchanting.value = true
            repository.enchantPageText(bookId, pageNum, voiceId)
            _isEnchanting.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
        synthesizer.shutdown()
    }
}

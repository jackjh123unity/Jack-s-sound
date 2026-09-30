package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppNavScreen
import com.example.ui.StoryCastViewModel
import com.example.ui.components.DownloadDialog
import com.example.ui.components.VoiceSelectorSheet
import com.example.ui.screens.BookReaderScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.theme.MidnightObsidian
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MidnightObsidian
                ) {
                    StoryCastApp()
                }
            }
        }
    }
}

@Composable
fun StoryCastApp(viewModel: StoryCastViewModel = viewModel()) {
    val books by viewModel.allBooks.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeBook by viewModel.activeBook.collectAsStateWithLifecycle()
    val activePages by viewModel.activePages.collectAsStateWithLifecycle()
    val currentPageNum by viewModel.currentPageNumber.collectAsStateWithLifecycle()
    val selectedVoice by viewModel.selectedVoice.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val voicePitch by viewModel.voicePitch.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val isDownloading by viewModel.isSingleFileDownloading.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.singleFileProgress.collectAsStateWithLifecycle()
    val downloadStatus by viewModel.singleFileStatusText.collectAsStateWithLifecycle()
    val completedFile by viewModel.completedSingleFile.collectAsStateWithLifecycle()
    val showDownloadDialog by viewModel.showDownloadDialog.collectAsStateWithLifecycle()
    val downloadMode by viewModel.downloadMode.collectAsStateWithLifecycle()
    val downloadingChapterNum by viewModel.downloadingChapterNum.collectAsStateWithLifecycle()
    val showVoiceSheet by viewModel.showVoiceSheet.collectAsStateWithLifecycle()
    val isEnchanting by viewModel.isEnchanting.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            AppNavScreen.LIBRARY -> {
                LibraryScreen(
                    books = books,
                    onSelectBook = { bookId ->
                        viewModel.selectBook(bookId)
                        viewModel.navigateTo(AppNavScreen.READER)
                    },
                    onImportDocument = { uri ->
                        viewModel.importDocumentUri(uri)
                    },
                    onImportPastedDoc = { title, author, content ->
                        viewModel.importPastedDoc(title, author, content)
                    },
                    onImportGoogleDocLink = { url ->
                        viewModel.importGoogleDocLink(url) {}
                    },
                    onDeleteBook = { bookId ->
                        viewModel.deleteBook(bookId)
                    },
                    onDownloadSingleFile = { bookId ->
                        viewModel.selectBook(bookId)
                        viewModel.startSingleFileDownload(bookId)
                    },
                    onOpenVoiceSelector = {
                        viewModel.openVoiceSelector()
                    }
                )
            }

            AppNavScreen.READER -> {
                BookReaderScreen(
                    book = activeBook,
                    pages = activePages,
                    currentPageNum = currentPageNum,
                    selectedVoice = selectedVoice,
                    playbackSpeed = playbackSpeed,
                    playbackPitch = voicePitch,
                    playbackState = playbackState,
                    isEnchanting = isEnchanting,
                    onBack = { viewModel.navigateTo(AppNavScreen.LIBRARY) },
                    onPageSelected = { pageNum -> viewModel.setCurrentPage(pageNum) },
                    onOpenVoiceSelector = { viewModel.openVoiceSelector() },
                    onSpeedChange = { speed -> viewModel.setSpeed(speed) },
                    onPitchChange = { pitch -> viewModel.setPitch(pitch) },
                    onEnchantPage = { viewModel.enchantCurrentPage() },
                    onPlayPausePage = { viewModel.togglePlayPause() },
                    onDownloadSingleFile = { bookId -> viewModel.startSingleFileDownload(bookId) },
                    onDownloadChapter = { bookId, chapterNum -> viewModel.startChapterDownload(bookId, chapterNum) },
                    onOpenFullPlayer = { viewModel.navigateTo(AppNavScreen.PLAYER) }
                )
            }

            AppNavScreen.PLAYER -> {
                PlayerScreen(
                    book = activeBook,
                    selectedVoice = selectedVoice,
                    playbackSpeed = playbackSpeed,
                    playbackPitch = voicePitch,
                    playbackState = playbackState,
                    onBack = { viewModel.navigateTo(AppNavScreen.READER) },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSeekTo = { posMs -> viewModel.seekTo(posMs) },
                    onSkipForward = { viewModel.skipForward() },
                    onSkipBackward = { viewModel.skipBackward() },
                    onSpeedChange = { speed -> viewModel.setSpeed(speed) },
                    onPitchChange = { pitch -> viewModel.setPitch(pitch) },
                    onOpenVoiceSelector = { viewModel.openVoiceSelector() },
                    onSetSleepTimer = { mins -> viewModel.setSleepTimer(mins) },
                    onDownloadSingleFile = { bookId -> viewModel.startSingleFileDownload(bookId) },
                    onDownloadChapter = { bookId, chapterNum -> viewModel.startChapterDownload(bookId, chapterNum) }
                )
            }
        }

        // Voice Selector BottomSheet (5 Male + 5 Female Voices with Pitch/Speed Tuning)
        if (showVoiceSheet) {
            VoiceSelectorSheet(
                selectedVoiceId = selectedVoice.id,
                currentPitch = voicePitch,
                currentSpeed = playbackSpeed,
                onVoiceSelected = { voice -> viewModel.selectVoice(voice) },
                onAuditionVoice = { voice, spd, ptch -> viewModel.auditionVoice(voice, spd, ptch) },
                onPitchChanged = { pitch -> viewModel.setPitch(pitch) },
                onSpeedChanged = { speed -> viewModel.setSpeed(speed) },
                onDismiss = { viewModel.closeVoiceSelector() }
            )
        }

        // Audiobook Download Dialog (Single Chapter OR All Chapters Single-File)
        if (showDownloadDialog) {
            DownloadDialog(
                isDownloading = isDownloading,
                progress = downloadProgress,
                statusText = downloadStatus,
                completedFile = completedFile,
                downloadMode = downloadMode,
                chapterNumber = downloadingChapterNum,
                onPlaySingleFile = { file ->
                    viewModel.playDownloadedAudio(file)
                },
                onDismiss = { viewModel.dismissDownloadDialog() }
            )
        }
    }
}

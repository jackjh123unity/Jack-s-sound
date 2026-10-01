package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState
import com.example.data.local.BookEntity
import com.example.data.local.PageEntity
import com.example.data.model.VoiceModel
import com.example.ui.components.AudioSpectrumVisualizer
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DeepSlateSurface
import com.example.ui.theme.GoldenAmberPrimary
import com.example.ui.theme.MidnightObsidian
import com.example.ui.theme.MysticPurpleSecondary
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(
    book: BookEntity?,
    pages: List<PageEntity>,
    currentPageNum: Int,
    selectedVoice: VoiceModel,
    playbackSpeed: Float,
    playbackPitch: Float = 1.0f,
    playbackState: PlaybackState,
    isEnchanting: Boolean,
    onBack: () -> Unit,
    onPageSelected: (Int) -> Unit,
    onOpenVoiceSelector: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit = {},
    onEnchantPage: () -> Unit,
    onPlayPausePage: () -> Unit,
    onDownloadSingleFile: (Long) -> Unit,
    onDownloadChapter: (Long, Int) -> Unit = { _, _ -> },
    onOpenFullPlayer: () -> Unit
) {
    BackHandler { onBack() }

    var textTab by remember { mutableIntStateOf(0) }
    var showSpeedSlider by remember { mutableStateOf(false) }

    val currentPage = pages.firstOrNull { it.pageNumber == currentPageNum }
        ?: pages.firstOrNull()

    val isCurrentPagePlaying = playbackState.isPlaying &&
            playbackState.currentBookId == book?.id &&
            playbackState.currentPageNumber == currentPageNum &&
            !playbackState.isSingleFileMode

    Scaffold(
        containerColor = MidnightObsidian,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book?.title ?: "Ebook Reader",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        val chSubtitle = currentPage?.chapterTitle?.takeIf { it.isNotBlank() }
                        Text(
                            text = if (chSubtitle != null) {
                                "Chapter $currentPageNum of ${book?.totalPages ?: pages.size} • $chSubtitle"
                            } else {
                                "Chapter $currentPageNum of ${book?.totalPages ?: pages.size}"
                            },
                            fontSize = 11.sp,
                            color = AmberGlow,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_reader_back")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Library",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenVoiceSelector,
                        modifier = Modifier.testTag("btn_reader_change_voice")
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = "Change Voice",
                            tint = GoldenAmberPrimary
                        )
                    }

                    IconButton(
                        onClick = { showSpeedSlider = !showSpeedSlider },
                        modifier = Modifier.testTag("btn_reader_toggle_speed")
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = "Adjust Speed",
                            tint = MysticPurpleSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightObsidian)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Speed Controls Bar (Expandable)
            if (showSpeedSlider) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E354C))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Pitch row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Voice Pitch / Timbre",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = when {
                                    playbackPitch < 0.80f -> "${String.format("%.2f", playbackPitch)}x (Deep)"
                                    playbackPitch <= 1.10f -> "${String.format("%.2f", playbackPitch)}x (Natural)"
                                    else -> "${String.format("%.2f", playbackPitch)}x (Bright)"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldenAmberPrimary
                            )
                        }
                        Slider(
                            value = playbackPitch,
                            onValueChange = onPitchChange,
                            valueRange = 0.60f..1.60f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldenAmberPrimary,
                                activeTrackColor = GoldenAmberPrimary,
                                inactiveTrackColor = Color(0xFF262C40)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(
                                0.70f to "0.7x Deep",
                                0.85f to "0.85x Warm",
                                1.00f to "1.0x Nat",
                                1.20f to "1.2x Crisp",
                                1.40f to "1.4x High"
                            ).forEach { (preset, label) ->
                                val isMatch = kotlin.math.abs(playbackPitch - preset) < 0.05f
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isMatch) GoldenAmberPrimary else Color(0xFF1E2130))
                                        .clickable { onPitchChange(preset) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMatch) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Speed row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reading Speed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = String.format("%.2fx", playbackSpeed),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MysticPurpleSecondary
                            )
                        }
                        Slider(
                            value = playbackSpeed,
                            onValueChange = onSpeedChange,
                            valueRange = 0.5f..2.5f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = MysticPurpleSecondary,
                                activeTrackColor = MysticPurpleSecondary,
                                inactiveTrackColor = Color(0xFF262C40)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { preset ->
                                val isMatch = kotlin.math.abs(playbackSpeed - preset) < 0.05f
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isMatch) MysticPurpleSecondary else Color(0xFF1E2130))
                                        .clickable { onSpeedChange(preset) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${preset}x",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMatch) Color.White else Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Status Bar: Voice Info & Narrator Quick Tap
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSlateSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenVoiceSelector() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2D2347)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = MysticPurpleSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Narrator: ${selectedVoice.name}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${selectedVoice.accent} • ${playbackSpeed}x",
                                fontSize = 11.sp,
                                color = AmberGlow
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpenVoiceSelector,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF262D42),
                            contentColor = AmberGlow
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_reader_change_voice")
                    ) {
                        Text("Voice Roster", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Audiobook Download Buttons: Both 1 Chapter at a time AND All Chapters in 1 Single File
            book?.let { b ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button: Download 1 Chapter at a time
                    Button(
                        onClick = { onDownloadChapter(b.id, currentPageNum) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF22283C),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAmberPrimary.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_download_chapter")
                    ) {
                        Icon(
                            imageVector = if (currentPage?.isAudioGenerated == true) Icons.Default.CheckCircle else Icons.Default.Download,
                            contentDescription = null,
                            tint = if (currentPage?.isAudioGenerated == true) SuccessGreen else GoldenAmberPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Download Chapter $currentPageNum",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Button: Download every chapter in audio into 1 single file
                    Button(
                        onClick = { onDownloadSingleFile(b.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldenAmberPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_download_all_chapters_single_file")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "All Chapters (1-File)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            // Spectrum Visualizer
            AudioSpectrumVisualizer(
                isPlaying = isCurrentPagePlaying,
                voiceName = selectedVoice.name,
                accent = selectedVoice.accent,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Chapter Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentPageNum > 1) onPageSelected(currentPageNum - 1)
                    },
                    enabled = currentPageNum > 1,
                    modifier = Modifier.testTag("btn_prev_chapter")
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous Chapter",
                        tint = if (currentPageNum > 1) Color.White else Color(0xFF475569)
                    )
                }

                Text(
                    text = "Chapter $currentPageNum / ${pages.size}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                IconButton(
                    onClick = {
                        if (currentPageNum < pages.size) onPageSelected(currentPageNum + 1)
                    },
                    enabled = currentPageNum < pages.size,
                    modifier = Modifier.testTag("btn_next_chapter")
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next Chapter",
                        tint = if (currentPageNum < pages.size) Color.White else Color(0xFF475569)
                    )
                }
            }

            // Script Mode Tabs: Book Text vs Enchanted Script
            TabRow(
                selectedTabIndex = textTab,
                containerColor = Color(0xFF131520),
                contentColor = GoldenAmberPrimary,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = textTab == 0,
                    onClick = { textTab = 0 },
                    text = { Text("Story Text", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_story_text")
                )
                Tab(
                    selected = textTab == 1,
                    onClick = { textTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberGlow)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Enchanted Script", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_enchanted_script")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Page Content Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262C3F))
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    if (textTab == 1) {
                        // Enchanted script banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF241D3B))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Performance tags [breaths, pauses, tone] active",
                                fontSize = 11.sp,
                                color = MysticPurpleSecondary
                            )
                            OutlinedButton(
                                onClick = onEnchantPage,
                                enabled = !isEnchanting,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("btn_re_enchant")
                            ) {
                                if (isEnchanting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = GoldenAmberPrimary
                                    )
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = GoldenAmberPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Re-Enchant", fontSize = 10.sp, color = GoldenAmberPrimary)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    val displayText = if (textTab == 1) {
                        currentPage?.enchantedText ?: currentPage?.rawText ?: "No text on this page."
                    } else {
                        currentPage?.rawText ?: "No text on this page."
                    }

                    Text(
                        text = displayText,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = Color(0xFFE2E8F0),
                        fontFamily = if (textTab == 1) FontFamily.Monospace else FontFamily.Default
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Page Audio Control Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF323952))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GoldenAmberPrimary)
                                .clickable { onPlayPausePage() }
                                .testTag("btn_play_pause_page"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCurrentPagePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isCurrentPagePlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        val currentChTitle = currentPage?.chapterTitle?.takeIf { it.isNotBlank() } ?: "Chapter $currentPageNum"
                        Column {
                            Text(
                                text = if (isCurrentPagePlaying) "Playing $currentChTitle" else "Listen to $currentChTitle",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Read aloud by ${selectedVoice.name} • Natural Spoken Audio",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { book?.let { b -> onDownloadChapter(b.id, currentPageNum) } },
                            modifier = Modifier.testTag("btn_bottom_download_chapter")
                        ) {
                            Icon(
                                imageVector = if (currentPage?.isAudioGenerated == true) Icons.Default.CheckCircle else Icons.Default.Download,
                                contentDescription = "Download Chapter $currentPageNum Audio",
                                tint = if (currentPage?.isAudioGenerated == true) SuccessGreen else GoldenAmberPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenFullPlayer,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGlow),
                            modifier = Modifier.testTag("btn_open_full_player")
                        ) {
                            Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Player", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.audio.PlaybackState
import com.example.data.local.BookEntity
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
fun PlayerScreen(
    book: BookEntity?,
    selectedVoice: VoiceModel,
    playbackSpeed: Float,
    playbackPitch: Float = 1.0f,
    playbackState: PlaybackState,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit = {},
    onOpenVoiceSelector: () -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onDownloadSingleFile: (Long) -> Unit,
    onDownloadChapter: (Long, Int) -> Unit = { _, _ -> }
) {
    BackHandler { onBack() }

    var showVoiceTuningDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val currentMs = playbackState.currentPositionMs
    val totalMs = playbackState.durationMs.coerceAtLeast(1)
    val progressFraction = (currentMs.toFloat() / totalMs).coerceIn(0f, 1f)

    Scaffold(
        containerColor = MidnightObsidian,
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "NOW PLAYING",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = AmberGlow
                        )
                        Text(
                            text = if (playbackState.isSingleFileMode) "Complete Audiobook (1-File)" else "Chapter / Page ${playbackState.currentPageNumber}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_player_back")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenVoiceSelector, modifier = Modifier.testTag("btn_player_voice")) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Roster",
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Book Cover Art with subtle glow
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E2130))
                    .border(2.dp, Color(0xFF33384D), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (book?.coverImagePath != null) {
                    AsyncImage(
                        model = book.coverImagePath,
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = book?.coverDrawableRes ?: R.drawable.audiobook_hero_1790734972693),
                        contentDescription = book?.title ?: "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Title & Narrator
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = book?.title ?: "Audiobook",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "by ${book?.author ?: "Unknown"}",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF262A3C))
                        .clickable { onOpenVoiceSelector() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = AmberGlow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Narrated by ${selectedVoice.name} (${selectedVoice.accent})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Dynamic Audio Spectrum Visualizer
            AudioSpectrumVisualizer(
                isPlaying = playbackState.isPlaying,
                voiceName = selectedVoice.name,
                accent = selectedVoice.accent,
                modifier = Modifier.fillMaxWidth()
            )

            // Progress Slider & Timestamps
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progressFraction,
                    onValueChange = { frac ->
                        onSeekTo((frac * totalMs).toInt())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = GoldenAmberPrimary,
                        activeTrackColor = GoldenAmberPrimary,
                        inactiveTrackColor = Color(0xFF2B3246)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("player_progress_slider")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTimeMs(currentMs),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "-${formatTimeMs((totalMs - currentMs).coerceAtLeast(0))}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Main Playback Controls (-15s, Play/Pause, +15s)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSkipBackward,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("btn_skip_backward")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 15s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(GoldenAmberPrimary)
                        .clickable { onTogglePlayPause() }
                        .testTag("btn_player_main_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                IconButton(
                    onClick = onSkipForward,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("btn_skip_forward")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 15s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Secondary Quick Actions (Speed, Sleep Timer, Download 1-File)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Selector Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E2232))
                        .clickable { showVoiceTuningDialog = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("btn_player_speed_pill")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = GoldenAmberPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.2fx", playbackSpeed),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Pitch Selector Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E2232))
                        .clickable { showVoiceTuningDialog = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("btn_player_pitch_pill")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MysticPurpleSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.2fx", playbackPitch),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Sleep Timer Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (playbackState.sleepTimerMinutesLeft != null) Color(0xFF3B2E58) else Color(0xFF1E2232)
                        )
                        .clickable { showSleepTimerDialog = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("btn_player_timer_pill")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NightsStay, contentDescription = null, tint = MysticPurpleSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (playbackState.sleepTimerMinutesLeft != null) "${playbackState.sleepTimerMinutesLeft}m" else "Timer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (playbackState.sleepTimerMinutesLeft != null) AmberGlow else Color.White
                        )
                    }
                }

                // Download Current Chapter Pill
                book?.let { b ->
                    val pageNum = playbackState.currentPageNumber
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E2232))
                            .clickable { onDownloadChapter(b.id, pageNum) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("btn_player_download_chapter_pill")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = GoldenAmberPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ch. $pageNum",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Download Every Chapter in 1-File Action
                book?.let { b ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E2232))
                            .clickable { onDownloadSingleFile(b.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("btn_player_download_pill")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "All (1-File)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Voice Tone & Speed Tuning Dialog
    if (showVoiceTuningDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceTuningDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Voice Values & Tuning", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onOpenVoiceSelector) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "Change Voice", tint = GoldenAmberPrimary)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Active Voice Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF191D2C)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${selectedVoice.name} (${selectedVoice.accent})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Gemini 3.8: ${selectedVoice.geminiVoiceName}",
                                    fontSize = 11.sp,
                                    color = AmberGlow
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    showVoiceTuningDialog = false
                                    onOpenVoiceSelector()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldenAmberPrimary)
                            ) {
                                Text("Switch", fontSize = 11.sp)
                            }
                        }
                    }

                    // 1. Voice Pitch Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Voice Pitch / Timbre", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

                        // Pitch presets
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
                    }

                    // 2. Playback Speed Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Playback Speed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                            valueRange = 0.5f..2.25f,
                            steps = 6,
                            colors = SliderDefaults.colors(
                                thumbColor = MysticPurpleSecondary,
                                activeTrackColor = MysticPurpleSecondary,
                                inactiveTrackColor = Color(0xFF262C40)
                            )
                        )

                        // Speed presets
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
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        onPitchChange(1.0f)
                        onSpeedChange(1.0f)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showVoiceTuningDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenAmberPrimary)
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DeepSlateSurface
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "Off", 5 to "5 Minutes", 15 to "15 Minutes", 30 to "30 Minutes", 45 to "45 Minutes", 60 to "1 Hour").forEach { (mins, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (playbackState.sleepTimerMinutesLeft == mins) Color(0xFF333D59) else Color(0xFF181B26))
                                .clickable {
                                    onSetSleepTimer(mins)
                                    showSleepTimerDialog = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
                            if (playbackState.sleepTimerMinutesLeft == mins) {
                                Text("Active", color = GoldenAmberPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSleepTimerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenAmberPrimary)
                ) {
                    Text("Done", color = Color.Black)
                }
            },
            containerColor = DeepSlateSurface
        )
    }
}

private fun formatTimeMs(millis: Int): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

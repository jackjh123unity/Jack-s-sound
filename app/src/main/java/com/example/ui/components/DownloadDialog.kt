package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.AudioExportUtil
import com.example.ui.DownloadMode
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.DeepSlateSurface
import com.example.ui.theme.GoldenAmberPrimary
import com.example.ui.theme.MysticPurpleSecondary
import com.example.ui.theme.SuccessGreen
import java.io.File

@Composable
fun DownloadDialog(
    isDownloading: Boolean,
    progress: Float,
    statusText: String,
    completedFile: File?,
    downloadMode: DownloadMode = DownloadMode.ALL_CHAPTERS_SINGLE_FILE,
    chapterNumber: Int? = null,
    onPlaySingleFile: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isSingleChapter = downloadMode == DownloadMode.SINGLE_CHAPTER

    Dialog(onDismissRequest = {
        if (!isDownloading) onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DeepSlateSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF33384F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (completedFile != null) Color(0x334ADE80) else Color(0x33FFB74D)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            color = GoldenAmberPrimary,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    } else if (completedFile != null) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            tint = SuccessGreen,
                            modifier = Modifier.size(34.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isSingleChapter) Icons.Default.Download else Icons.Default.LibraryMusic,
                            contentDescription = "Download",
                            tint = GoldenAmberPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val titleText = when {
                    completedFile != null && isSingleChapter -> "Chapter ${chapterNumber ?: ""} Audio Ready!"
                    completedFile != null -> "Full Audiobook Ready! (1-File)"
                    isSingleChapter -> "Downloading Chapter ${chapterNumber ?: ""} Audio"
                    else -> "Downloading Every Chapter in 1 Single File"
                }

                Text(
                    text = titleText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                val subtitleText = when {
                    completedFile != null && isSingleChapter ->
                        "Chapter ${chapterNumber ?: ""} was synthesized and saved to your device for offline listening."
                    completedFile != null ->
                        "All chapters were synthesized and stitched into a single continuous audiobook file."
                    isSingleChapter ->
                        "Synthesizing Chapter ${chapterNumber ?: ""} with natural voice pacing and exporting to audio storage."
                    else ->
                        "Synthesizing every chapter sequentially and merging them into one single continuous audio file."
                }

                Text(
                    text = subtitleText,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Progress Bar
                if (isDownloading) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isSingleChapter) "SYNTHESIS PROGRESS" else "ALL CHAPTERS PROGRESS",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AmberGlow
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = GoldenAmberPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = GoldenAmberPrimary,
                            trackColor = Color(0xFF1E2232)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Completed state info
                if (completedFile != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF181B28))
                            .border(1.dp, Color(0xFF2B3246), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = completedFile.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Size: ${completedFile.length() / 1024} KB",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "✓ Saved to Music/StoryCastAudio",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onPlaySingleFile(completedFile)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenAmberPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_dialog_play_audio")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play Audio", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val title = if (isSingleChapter) "Chapter ${chapterNumber ?: ""} Audio" else "Complete Audiobook (1-File)"
                                AudioExportUtil.shareAudio(context, completedFile, title)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MysticPurpleSecondary),
                            modifier = Modifier.testTag("btn_dialog_share_audio")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MysticPurpleSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isDownloading) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_dialog_close")
                    ) {
                        Text("Close", color = Color(0xFFCBD5E1))
                    }
                }
            }
        }
    }
}

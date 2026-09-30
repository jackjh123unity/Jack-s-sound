package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VoiceCatalog
import com.example.data.model.VoiceGender
import com.example.data.model.VoiceModel
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DeepSlateSurface
import com.example.ui.theme.GoldenAmberPrimary
import com.example.ui.theme.MysticPurpleSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceSelectorSheet(
    selectedVoiceId: String,
    currentPitch: Float = 1.0f,
    currentSpeed: Float = 1.0f,
    onVoiceSelected: (VoiceModel) -> Unit,
    onAuditionVoice: (VoiceModel, Float, Float) -> Unit,
    onPitchChanged: (Float) -> Unit = {},
    onSpeedChanged: (Float) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    var previewingVoiceId by remember { mutableStateOf<String?>(null) }
    var showTuningPanel by remember { mutableStateOf(false) }

    var tunedPitch by remember(currentPitch) { mutableFloatStateOf(currentPitch) }
    var tunedSpeed by remember(currentSpeed) { mutableFloatStateOf(currentSpeed) }

    val allVoices = VoiceCatalog.ALL_VOICES
    val maleVoices = allVoices.filter { it.gender == VoiceGender.MALE }
    val femaleVoices = allVoices.filter { it.gender == VoiceGender.FEMALE }

    val displayedVoices = when (selectedTab) {
        1 -> maleVoices
        2 -> femaleVoices
        else -> allVoices
    }

    val activeVoice = allVoices.firstOrNull { it.id == selectedVoiceId } ?: allVoices.first()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepSlateSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF475569))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Select Narrator Voice",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "AI Studio 3.8 Multi-Tone Expressive Voices",
                        fontSize = 13.sp,
                        color = AmberGlow
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF2A2146))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Voice Selector",
                        tint = MysticPurpleSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable Pitch & Speed Tuning Panel Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTuningPanel = !showTuningPanel }
                    .testTag("btn_toggle_voice_tuning"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF191D2C)),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (showTuningPanel) GoldenAmberPrimary else Color(0xFF2E344A))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (showTuningPanel) GoldenAmberPrimary else Color(0xFF2B3248)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = if (showTuningPanel) Color.Black else AmberGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Fine-Tune Voice Values",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Pitch: ${String.format("%.2f", tunedPitch)}x • Speed: ${String.format("%.2f", tunedSpeed)}x",
                                    fontSize = 11.sp,
                                    color = AmberGlow
                                )
                            }
                        }

                        Icon(
                            imageVector = if (showTuningPanel) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (showTuningPanel) "Collapse" else "Expand",
                            tint = Color.White
                        )
                    }

                    // Expanded Tuning Controls
                    AnimatedVisibility(visible = showTuningPanel) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            // 1. Pitch Section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = GoldenAmberPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Voice Pitch / Timbre", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text(
                                    text = when {
                                        tunedPitch < 0.80f -> "${String.format("%.2f", tunedPitch)}x (Deep Baritone)"
                                        tunedPitch <= 1.10f -> "${String.format("%.2f", tunedPitch)}x (Natural Resonance)"
                                        else -> "${String.format("%.2f", tunedPitch)}x (Bright & High)"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldenAmberPrimary
                                )
                            }

                            Slider(
                                value = tunedPitch,
                                onValueChange = { newPitch ->
                                    tunedPitch = newPitch
                                    onPitchChanged(newPitch)
                                },
                                valueRange = 0.60f..1.60f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldenAmberPrimary,
                                    activeTrackColor = GoldenAmberPrimary,
                                    inactiveTrackColor = Color(0xFF2B3248)
                                ),
                                modifier = Modifier.testTag("slider_voice_pitch")
                            )

                            // Pitch Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(
                                    0.70f to "Deep",
                                    0.85f to "Warm",
                                    1.00f to "Natural",
                                    1.20f to "Crisp",
                                    1.40f to "High"
                                ).forEach { (presetVal, presetLabel) ->
                                    val isMatch = kotlin.math.abs(tunedPitch - presetVal) < 0.05f
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isMatch) GoldenAmberPrimary else Color(0xFF23283B))
                                            .clickable {
                                                tunedPitch = presetVal
                                                onPitchChanged(presetVal)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = presetLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMatch) Color.Black else Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. Speed Section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = MysticPurpleSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Speech Rate / Speed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text(
                                    text = "${String.format("%.2f", tunedSpeed)}x",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberGlow
                                )
                            }

                            Slider(
                                value = tunedSpeed,
                                onValueChange = { newSpeed ->
                                    tunedSpeed = newSpeed
                                    onSpeedChanged(newSpeed)
                                },
                                valueRange = 0.5f..2.25f,
                                steps = 6,
                                colors = SliderDefaults.colors(
                                    thumbColor = MysticPurpleSecondary,
                                    activeTrackColor = MysticPurpleSecondary,
                                    inactiveTrackColor = Color(0xFF2B3248)
                                ),
                                modifier = Modifier.testTag("slider_voice_speed")
                            )

                            // Speed Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { presetVal ->
                                    val isMatch = kotlin.math.abs(tunedSpeed - presetVal) < 0.05f
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isMatch) MysticPurpleSecondary else Color(0xFF23283B))
                                            .clickable {
                                                tunedSpeed = presetVal
                                                onSpeedChanged(presetVal)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${presetVal}x",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMatch) Color.White else Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action buttons: Test Tuned Voice & Reset
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        tunedPitch = 1.0f
                                        tunedSpeed = 1.0f
                                        onPitchChanged(1.0f)
                                        onSpeedChanged(1.0f)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset (1.0x)", fontSize = 11.sp)
                                }

                                FilledTonalButton(
                                    onClick = {
                                        onAuditionVoice(activeVoice, tunedSpeed, tunedPitch)
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = GoldenAmberPrimary,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_test_tuned_voice")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test ${activeVoice.name} Tuned", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs: All (10) | Male (5) | Female (5)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF131520),
                contentColor = GoldenAmberPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF2B3045), RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All (10)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab_all_voices")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Male, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Male (5)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_male_voices")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Female, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Female (5)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_female_voices")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedVoices, key = { it.id }) { voice ->
                    val isSelected = voice.id == selectedVoiceId
                    val isPreviewing = previewingVoiceId == voice.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVoiceSelected(voice) }
                            .testTag("voice_card_${voice.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF262C42) else CardSurfaceElevated
                        ),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(1.5.dp, GoldenAmberPrimary)
                        } else {
                            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3247))
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Gender icon avatar
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (voice.gender == VoiceGender.MALE) Color(0xFF1E293B) else Color(0xFF3B1E2E)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (voice.gender == VoiceGender.MALE) Icons.Default.Male else Icons.Default.Female,
                                            contentDescription = voice.gender.name,
                                            tint = if (voice.gender == VoiceGender.MALE) Color(0xFF38BDF8) else Color(0xFFF472B6),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = voice.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${voice.accent}",
                                                fontSize = 12.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        Text(
                                            text = voice.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AmberGlow
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(GoldenAmberPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = voice.toneDescription,
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Model & Persona Tag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (voice.gender == VoiceGender.MALE) Color(0xFF0F3057) else Color(0xFF4A154B))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Gemini 3.8: ${voice.geminiVoiceName}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (voice.gender == VoiceGender.MALE) Color(0xFF7DD3FC) else Color(0xFFF9A8D4)
                                    )
                                }

                                voice.tags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1E2130))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            fontSize = 10.sp,
                                            color = Color(0xFFA5B4FC)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Audition Quote & Listen Action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF161824))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "\"${voice.previewQuote}\"",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                FilledTonalButton(
                                    onClick = {
                                        previewingVoiceId = voice.id
                                        onAuditionVoice(voice, tunedSpeed, tunedPitch)
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isSelected) GoldenAmberPrimary else Color(0xFF2C334D),
                                        contentColor = if (isSelected) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_audition_${voice.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Audition",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Audition", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

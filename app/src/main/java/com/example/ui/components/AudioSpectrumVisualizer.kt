package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.DeepSlateSurface
import com.example.ui.theme.GoldenAmberPrimary
import com.example.ui.theme.MysticPurpleSecondary
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AudioSpectrumVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 52,
    voiceName: String = "Arthur",
    accent: String = "British Classical",
    emotionWarmth: Int = 95,
    prosodySota: Float = 99.4f,
    dynamicEnergyDb: Float = -18.4f,
    streamingLatencyMs: Int = 121
) {
    val transition = rememberInfiniteTransition(label = "spectrum")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF10121B))
            .border(1.dp, Color(0xFF282D42), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // Acoustic Telemetry Header matching YouTube AI Voice Showcase
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "52-BAND LOGARITHMIC SPECTRUM ANALYZER (60Hz – 9kHz)",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "$voiceName • $accent (24kHz Studio PCM)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberGlow
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isPlaying) "PEAK: ${dynamicEnergyDb} dBFS" else "STANDBY",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) Color(0xFFFB7185) else Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPlaying) Color(0x33FB7185) else Color(0x2264748B))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isPlaying) "LIVE" else "OFF",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isPlaying) Color(0xFFF43F5E) else Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 52-Band Spectrum Bars Canvas (coral/pink/magenta bars from video)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            val totalWidth = size.width
            val barWidth = (totalWidth / barCount) * 0.72f
            val spacing = (totalWidth / barCount) * 0.28f
            val maxHeight = size.height

            for (i in 0 until barCount) {
                val x = i * (barWidth + spacing)
                val normalizedIndex = i.toFloat() / barCount

                // Natural logarithmic acoustic frequency response curve
                val waveFactor = if (isPlaying) {
                    val primaryWave = sin(phase + i * 0.28f) * 0.5f + 0.5f
                    val harmonicWave = sin(phase * 1.5f + i * 0.55f) * 0.3f + 0.5f
                    val logShape = (1.0f - (normalizedIndex - 0.3f) * (normalizedIndex - 0.3f) * 2.2f).coerceIn(0.25f, 1.0f)
                    ((primaryWave * 0.6f + harmonicWave * 0.4f) * logShape).coerceIn(0.12f, 1.0f)
                } else {
                    0.08f
                }

                val barHeight = maxHeight * waveFactor
                val y = maxHeight - barHeight

                val coralGradient = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFDA4AF), // Soft pink peak
                        Color(0xFFF43F5E), // Vivid coral
                        Color(0xFFBE185D)  // Deep magenta base
                    ),
                    startY = y,
                    endY = maxHeight
                )

                drawRoundRect(
                    brush = coralGradient,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Full Acoustic Telemetry Row matching screenshot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TelemetryGauge(label = "EMOTION WARMTH", value = emotionWarmth / 100f, displayStr = "$emotionWarmth%", color = Color(0xFFFB7185))
            TelemetryGauge(label = "PROSODY SOTA", value = prosodySota / 100f, displayStr = "${prosodySota}%", color = Color(0xFFA78BFA))
            TelemetryGauge(label = "DYNAMIC ENERGY", value = 0.85f, displayStr = "${dynamicEnergyDb} dB", color = Color(0xFF38BDF8))
            TelemetryGauge(label = "STREAMING LATENCY", value = 0.92f, displayStr = "${streamingLatencyMs} ms", color = Color(0xFF4ADE80))
        }
    }
}

@Composable
private fun TelemetryGauge(
    label: String,
    value: Float,
    displayStr: String,
    color: Color
) {
    Column(modifier = Modifier.width(76.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.split(" ").firstOrNull() ?: label,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = displayStr,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp)),
            color = color,
            trackColor = Color(0xFF1E2333),
        )
    }
}

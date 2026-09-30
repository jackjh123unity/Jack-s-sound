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
    barCount: Int = 36,
    voiceName: String = "Arthur",
    accent: String = "British Classical"
) {
    val transition = rememberInfiniteTransition(label = "spectrum")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DeepSlateSurface)
            .border(1.dp, Color(0xFF33384C), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // Acoustic Telemetry Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACOUSTIC PROSODY ANALYZER (24kHz PCM)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF8E9BB0),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$voiceName • $accent",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberGlow
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isPlaying) Color(0x334ADE80) else Color(0x2264748B))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isPlaying) "VOICE ACTIVE" else "STANDBY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isPlaying) Color(0xFF4ADE80) else Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Spectrum Bars Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val totalWidth = size.width
            val barWidth = totalWidth / barCount * 0.7f
            val spacing = totalWidth / barCount * 0.3f
            val maxHeight = size.height

            for (i in 0 until barCount) {
                val x = i * (barWidth + spacing)
                val normalizedIndex = i.toFloat() / barCount

                // Natural curve with frequency resonance
                val waveFactor = if (isPlaying) {
                    val rawWave = sin(phase + i * 0.35f) * 0.5f + 0.5f
                    val bellCurve = sin(normalizedIndex * Math.PI.toFloat())
                    ((rawWave * 0.7f + 0.3f) * bellCurve).coerceIn(0.12f, 1.0f)
                } else {
                    0.08f
                }

                val barHeight = maxHeight * waveFactor
                val y = maxHeight - barHeight

                val gradient = Brush.verticalGradient(
                    colors = listOf(
                        GoldenAmberPrimary,
                        Color(0xFFF43F5E),
                        MysticPurpleSecondary
                    ),
                    startY = y,
                    endY = maxHeight
                )

                drawRoundRect(
                    brush = gradient,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mini telemetry metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TelemetryGauge(label = "Warmth", value = 0.94f, color = GoldenAmberPrimary)
            TelemetryGauge(label = "Prosody", value = 0.98f, color = MysticPurpleSecondary)
            TelemetryGauge(label = "Clarity", value = 0.96f, color = Color(0xFF38BDF8))
        }
    }
}

@Composable
private fun TelemetryGauge(
    label: String,
    value: Float,
    color: Color
) {
    Column(modifier = Modifier.width(90.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { value },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Color(0xFF232738),
        )
    }
}

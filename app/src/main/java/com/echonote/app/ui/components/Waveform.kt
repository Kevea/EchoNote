package com.echonote.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

private const val MAX_BARS = 56
private const val AMPLITUDE_REFERENCE = 14000f
private const val MIN_BAR_FRACTION = 0.08f

@Composable
fun Waveform(
    amplitudes: List<Int>,
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
) {
    val bars = remember(amplitudes) { downsample(amplitudes, MAX_BARS) }
    Canvas(modifier = modifier.fillMaxSize()) {
        drawBars(bars, progress, activeColor, inactiveColor)
    }
}

@Composable
fun LiveWaveform(
    amplitude: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val animated = remember { Animatable(0f) }
    val target = normalize(amplitude)
    LaunchedEffect(target) {
        animated.animateTo(target, animationSpec = tween(120))
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val barCount = 5
        val barWidth = size.width / (barCount * 2f)
        val centerY = size.height / 2f
        for (i in 0 until barCount) {
            val phase = 1f - (kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)) * 0.4f
            val h = (size.height * animated.value * phase).coerceAtLeast(size.height * MIN_BAR_FRACTION)
            val x = i * barWidth * 2f + barWidth / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, centerY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

private fun DrawScope.drawBars(
    bars: List<Float>,
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
) {
    if (bars.isEmpty()) return
    val gap = 3.dp.toPx()
    val barWidth = max(1.5f, (size.width - gap * (bars.size - 1)) / bars.size)
    val activeCount = (bars.size * progress).toInt()
    var x = 0f
    bars.forEachIndexed { index, fraction ->
        val h = (size.height * fraction).coerceAtLeast(size.height * MIN_BAR_FRACTION)
        val color = if (index < activeCount) activeColor else inactiveColor
        drawRoundRect(
            color = color,
            topLeft = Offset(x, (size.height - h) / 2f),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
        )
        x += barWidth + gap
    }
}

private fun normalize(amplitude: Int): Float =
    min(1f, max(0f, amplitude / AMPLITUDE_REFERENCE))

private fun downsample(amplitudes: List<Int>, targetCount: Int): List<Float> {
    if (amplitudes.isEmpty()) return List(targetCount) { MIN_BAR_FRACTION }
    if (amplitudes.size <= targetCount) return amplitudes.map { normalize(it) }
    val chunkSize = amplitudes.size / targetCount
    return (0 until targetCount).map { i ->
        val chunk = amplitudes.subList(i * chunkSize, min((i + 1) * chunkSize, amplitudes.size))
        normalize(chunk.maxOrNull() ?: 0)
    }
}

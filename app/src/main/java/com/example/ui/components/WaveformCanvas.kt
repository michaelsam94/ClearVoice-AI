package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WaveformCanvas(
    samples: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val midY = size.height / 2f
        if (samples.isEmpty()) {
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 4f
            )
            return@Canvas
        }

        val barCount = samples.size
        val gap = 4f
        val totalGapsWidth = gap * (barCount - 1)
        val barWidth = if (barCount > 1) {
            (size.width - totalGapsWidth) / barCount
        } else {
            size.width
        }

        samples.forEachIndexed { i, amp ->
            val normalizedAmp = amp.coerceIn(0f, 1f)
            val halfHeight = (normalizedAmp * midY * 0.9f).coerceAtLeast(3f)
            val x = i * (barWidth + gap)
            
            drawRect(
                color = color,
                topLeft = Offset(x, midY - halfHeight),
                size = Size(barWidth, halfHeight * 2f),
                alpha = 0.85f
            )
        }
    }
}

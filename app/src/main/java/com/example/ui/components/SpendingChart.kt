package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.viewmodel.DaySpending
import java.util.Locale

@Composable
fun SpendingChart(
    modifier: Modifier = Modifier,
    data: List<DaySpending>,
    periodName: String = "Week"
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No spending recorded for this period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedIndex by remember(data) {
        // default select the highest day or the last item
        val maxIdx = data.indexOfMaxBy { it.amount }
        mutableIntStateOf(if (maxIdx >= 0) maxIdx else 0)
    }

    val animationProgress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val maxAmount = remember(data) {
        val max = data.maxOfOrNull { it.amount } ?: 1.0
        if (max <= 0.0) 100.0 else max * 1.15
    }

    val selectedItem = data.getOrNull(selectedIndex)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spending_chart"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with Selected Tooltip Detail
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap any bar to inspect",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedItem != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = selectedItem.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", selectedItem.amount)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Canvas Bar Chart
            val barCount = data.size
            val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            val barDefaultColor = MaterialTheme.colorScheme.surfaceVariant
            val barActiveGradient = listOf(EmeraldPrimary, EmeraldPrimaryDark)

            Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .pointerInput(data) {
                            detectTapGestures { offset ->
                                val slotWidth = size.width / barCount
                                val tappedIndex = (offset.x / slotWidth).toInt().coerceIn(0, barCount - 1)
                                selectedIndex = tappedIndex
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 30.dp.toPx()
                    val chartHeight = height - bottomPadding

                    // Draw 3 horizontal grid lines
                    for (i in 0..2) {
                        val y = chartHeight * (i / 2f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val slotWidth = width / barCount
                    val barWidth = (slotWidth * 0.55f).coerceIn(14.dp.toPx(), 44.dp.toPx())

                    data.forEachIndexed { index, item ->
                        val slotCenter = index * slotWidth + (slotWidth / 2f)
                        val barLeft = slotCenter - (barWidth / 2f)

                        val barProportion = (item.amount / maxAmount).toFloat().coerceIn(0.04f, 1f)
                        val animatedBarHeight = chartHeight * barProportion * animationProgress.value
                        val barTop = chartHeight - animatedBarHeight

                        val isSelected = index == selectedIndex

                        if (isSelected) {
                            // Selected bar with vivid primary gradient
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = barActiveGradient,
                                    startY = barTop,
                                    endY = chartHeight
                                ),
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, animatedBarHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        } else {
                            // Default bar
                            drawRoundRect(
                                color = barDefaultColor,
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, animatedBarHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }
                    }
                }

                // Day Labels below chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(top = 155.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    data.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex
                        Text(
                            text = item.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private inline fun <T> Iterable<T>.indexOfMaxBy(selector: (T) -> Double): Int {
    var maxIndex = -1
    var maxValue = Double.MIN_VALUE
    var index = 0
    for (item in this) {
        val value = selector(item)
        if (value > maxValue) {
            maxValue = value
            maxIndex = index
        }
        index++
    }
    return maxIndex
}

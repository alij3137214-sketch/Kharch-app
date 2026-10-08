package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ThirtyDaySpendingTrend
import java.util.Locale
import kotlin.math.max

/**
 * Minimalist, clean 30-Day Daily Spending Bar Chart component.
 * Visualizes daily spending trends with Cartesian grid, average reference line,
 * and lightweight tap inspection.
 */
@Composable
fun RechartsDailySpendingBarChart(
    modifier: Modifier = Modifier,
    trend: ThirtyDaySpendingTrend
) {
    var activeIndex by remember { mutableIntStateOf(-1) }

    val animationProgress = remember(trend.dailyPoints) { Animatable(0f) }
    LaunchedEffect(trend.dailyPoints) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
        )
    }

    val maxAmount = remember(trend.dailyPoints) {
        val peak = trend.dailyPoints.maxOfOrNull { it.amount } ?: 0.0
        if (peak <= 0.0) 1000.0 else peak * 1.15
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_daily_spending_bar_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clean Minimalist Header: Title on Left, Average on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Spending",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Avg: Rs. ${String.format(Locale.getDefault(), "%,.0f", trend.averageDaily)}/day",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WarningAmber
                )
            }

            // Minimalist Bar Chart Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val availableWidth = constraints.maxWidth.toFloat()
                val totalPoints = trend.dailyPoints.size.coerceAtLeast(1)
                val stepX = availableWidth / totalPoints

                val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                val avgLineColor = WarningAmber.copy(alpha = 0.75f)
                val activeBarColor = EmeraldPrimary
                val standardBarColor = EmeraldPrimary.copy(alpha = 0.8f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .pointerInput(trend.dailyPoints) {
                            detectTapGestures { tapOffset ->
                                val clickedIdx = (tapOffset.x / stepX).toInt().coerceIn(0, totalPoints - 1)
                                activeIndex = if (activeIndex == clickedIdx) -1 else clickedIdx
                            }
                        }
                ) {
                    val chartHeight = size.height - 10.dp.toPx()
                    val chartTop = 6.dp.toPx()

                    // Faint horizontal dashed grid lines (2 lines)
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    for (i in 1..2) {
                        val y = chartTop + (chartHeight * (i.toFloat() / 3f))
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashPathEffect
                        )
                    }

                    // Faint Reference Line for 30-Day Average
                    if (trend.averageDaily > 0 && maxAmount > 0) {
                        val avgRatio = (trend.averageDaily / maxAmount).toFloat().coerceIn(0f, 1f)
                        val avgY = chartTop + chartHeight * (1f - avgRatio)

                        drawLine(
                            color = avgLineColor,
                            start = Offset(0f, avgY),
                            end = Offset(size.width, avgY),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    // Minimalist Bars
                    val barWidth = max(2f, (stepX * 0.62f))
                    val animFactor = animationProgress.value

                    trend.dailyPoints.forEachIndexed { index, point ->
                        val ratio = if (maxAmount > 0) (point.amount / maxAmount).toFloat() else 0f
                        val barHeight = (ratio * chartHeight * animFactor).coerceAtLeast(if (point.amount > 0) 3.dp.toPx() else 0f)

                        val barLeft = (index * stepX) + (stepX - barWidth) / 2f
                        val barTop = chartTop + (chartHeight - barHeight)

                        val isActive = activeIndex == index
                        val hasActive = activeIndex >= 0

                        val barColor = when {
                            isActive -> activeBarColor
                            hasActive -> standardBarColor.copy(alpha = 0.25f)
                            else -> standardBarColor
                        }

                        if (barHeight > 0) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Minimalist Axis: Just 30d ago and Today
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "30 days ago",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )

                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Lightweight Tap Pill (Only shows when user taps a specific bar)
            AnimatedVisibility(
                visible = activeIndex in trend.dailyPoints.indices,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (activeIndex in trend.dailyPoints.indices) {
                    val activePoint = trend.dailyPoints[activeIndex]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("recharts_bar_tooltip"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${activePoint.fullDateLabel} • ${activePoint.transactionCount} txn${if (activePoint.transactionCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", activePoint.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            IconButton(
                                onClick = { activeIndex = -1 },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Minimalist Pattern Note (One discreet line)
            if (trend.patternInsight.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spending_pattern_insights"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = trend.patternInsight,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseCategory
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.CategoryShare
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Recharts color palette for minimalist data visualization.
 * Aligned with Recharts default categorical color specs and Material Design 3.
 */
val RechartsColors = listOf(
    Color(0xFF10B981), // Emerald
    Color(0xFF0284C7), // Sky Blue
    Color(0xFF6366F1), // Indigo
    Color(0xFFF59E0B), // Amber
    Color(0xFF8B5CF6), // Purple
    Color(0xFFF43F5E), // Rose
    Color(0xFF14B8A6), // Teal
    Color(0xFFF97316), // Orange
    Color(0xFF64748B)  // Slate
)

/**
 * Recharts-inspired clean, minimalist Pie Chart component.
 * Displays breakdown of monthly expenses by category with interactive segment
 * selection, Recharts-styled floating tooltip, and custom legend.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RechartsCategoryPieChart(
    modifier: Modifier = Modifier,
    categories: List<CategoryShare>,
    selectedCategory: String? = null,
    onCategorySelected: (String?) -> Unit = {}
) {
    var isDonutMode by remember { mutableStateOf(true) }
    var activeIndex by remember { mutableIntStateOf(-1) }

    val monthLabel = remember {
        val cal = Calendar.getInstance()
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    val totalAmount = remember(categories) { categories.sumOf { it.amount } }

    // Map categories with Recharts colors
    val chartItems = remember(categories) {
        categories.mapIndexed { index, share ->
            val color = RechartsColors[index % RechartsColors.size]
            RechartsSectorData(
                category = share.category,
                amount = share.amount,
                percentage = share.percentage,
                color = color
            )
        }
    }

    // Keep activeIndex synchronized with selectedCategory
    LaunchedEffect(selectedCategory, chartItems) {
        if (selectedCategory == null) {
            activeIndex = -1
        } else {
            val idx = chartItems.indexOfFirst { it.category.equals(selectedCategory, ignoreCase = true) }
            if (idx >= 0) activeIndex = idx
        }
    }

    // Animation progress
    val animationProgress = remember(categories) { Animatable(0f) }
    LaunchedEffect(categories) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_category_pie_chart"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Month Label & Chart Type Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Chart mode toggle (Donut vs Solid Pie)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { isDonutMode = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DonutLarge,
                            contentDescription = "Donut View",
                            tint = if (isDonutMode) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { isDonutMode = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Pie View",
                            tint = if (!isDonutMode) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (chartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses recorded this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Interactive Recharts Pie / Donut Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(210.dp)
                            .pointerInput(chartItems) {
                                detectTapGestures { tapOffset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = tapOffset.x - center.x
                                    val dy = tapOffset.y - center.y
                                    val distance = sqrt(dx * dx + dy * dy)
                                    val outerRadius = (size.width / 2f) - 10f
                                    val innerRadius = if (isDonutMode) outerRadius * 0.62f else 0f

                                    if (distance in innerRadius..outerRadius) {
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < 0) angle += 360f

                                        // Start angle is -90 degrees (top)
                                        var adjustedAngle = (angle - (-90f + 360f)) % 360f
                                        if (adjustedAngle < 0) adjustedAngle += 360f

                                        var cumAngle = 0f
                                        var clickedIndex = -1
                                        for (i in chartItems.indices) {
                                            val sweep = chartItems[i].percentage * 360f
                                            if (adjustedAngle in cumAngle..(cumAngle + sweep)) {
                                                clickedIndex = i
                                                break
                                            }
                                            cumAngle += sweep
                                        }

                                        if (clickedIndex >= 0) {
                                            if (activeIndex == clickedIndex) {
                                                activeIndex = -1
                                                onCategorySelected(null)
                                            } else {
                                                activeIndex = clickedIndex
                                                onCategorySelected(chartItems[clickedIndex].category)
                                            }
                                        }
                                    } else if (distance < innerRadius) {
                                        // Tapped center
                                        activeIndex = -1
                                        onCategorySelected(null)
                                    }
                                }
                            }
                    ) {
                        val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = (size.minDimension / 2f) - 12f
                        val strokeWidth = if (isDonutMode) maxRadius * 0.38f else maxRadius

                        var currentAngle = -90f
                        val paddingAngle = if (chartItems.size > 1) 3f else 0f

                        chartItems.forEachIndexed { index, item ->
                            val rawSweep = item.percentage * 360f * animationProgress.value
                            val effectiveSweep = (rawSweep - paddingAngle).coerceAtLeast(0.5f)
                            val isActive = activeIndex == index
                            val hasActive = activeIndex >= 0

                            val segmentColor = when {
                                isActive -> item.color
                                hasActive -> item.color.copy(alpha = 0.3f)
                                else -> item.color
                            }

                            val outerRadius = if (isActive) maxRadius + 6f else maxRadius
                            val sectorDiameter = (outerRadius - (if (isDonutMode) strokeWidth / 2f else 0f)) * 2f
                            val drawSize = Size(sectorDiameter, sectorDiameter)
                            val topLeft = Offset(
                                canvasCenter.x - sectorDiameter / 2f,
                                canvasCenter.y - sectorDiameter / 2f
                            )

                            if (isDonutMode) {
                                drawArc(
                                    color = segmentColor,
                                    startAngle = currentAngle + (paddingAngle / 2f),
                                    sweepAngle = effectiveSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = drawSize,
                                    style = Stroke(
                                        width = if (isActive) strokeWidth + 6f else strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                )
                            } else {
                                drawArc(
                                    color = segmentColor,
                                    startAngle = currentAngle + (paddingAngle / 2f),
                                    sweepAngle = effectiveSweep,
                                    useCenter = true,
                                    topLeft = topLeft,
                                    size = drawSize,
                                    style = Fill
                                )
                            }

                            currentAngle += rawSweep
                        }
                    }

                    // Center Content in Donut Mode
                    if (isDonutMode) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    activeIndex = -1
                                    onCategorySelected(null)
                                }
                                .padding(8.dp)
                        ) {
                            if (activeIndex in chartItems.indices) {
                                val active = chartItems[activeIndex]
                                Text(
                                    text = active.category,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = active.color,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", active.amount)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%.1f", active.percentage * 100)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Monthly Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", totalAmount)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${chartItems.size} Categories",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Recharts-style Tooltip Card (Active Sector Inspector)
                AnimatedVisibility(
                    visible = activeIndex in chartItems.indices,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (activeIndex in chartItems.indices) {
                        val active = chartItems[activeIndex]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recharts_tooltip"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ),
                            border = BorderStroke(1.dp, active.color.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(active.color)
                                    )
                                    Column {
                                        Text(
                                            text = active.category,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%.1f", active.percentage * 100)}% of monthly spending",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", active.amount)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = active.color
                                )
                            }
                        }
                    }
                }

                // Recharts Minimalist Legend Component
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recharts_legend"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chartItems.forEachIndexed { index, item ->
                            val isSelected = activeIndex == index
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        activeIndex = -1
                                        onCategorySelected(null)
                                    } else {
                                        activeIndex = index
                                        onCategorySelected(item.category)
                                    }
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(item.color)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "${item.category} ${String.format(Locale.getDefault(), "%.0f", item.percentage * 100)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = item.color.copy(alpha = 0.18f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data representation for an individual sector in the Recharts Pie Chart.
 */
data class RechartsSectorData(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

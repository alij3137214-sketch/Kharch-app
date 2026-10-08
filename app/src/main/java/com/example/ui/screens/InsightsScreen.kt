package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseCategory
import com.example.domain.CategoryTotal
import com.example.domain.DayTotal
import com.example.domain.MoneyMath
import com.example.domain.PeriodKind
import com.example.domain.PeriodSummary
import com.example.ui.components.AnimatedAmount
import com.example.ui.components.ExportCsvDialog
import com.example.ui.components.FormSheet
import com.example.ui.components.IconBadge
import com.example.ui.components.KharchCard
import com.example.ui.components.PickChip
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenPadding
import com.example.ui.components.entrance
import com.example.ui.components.formatRs
import com.example.ui.components.pressable
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KharchUiState
import com.example.ui.viewmodel.KharchViewModel
import com.example.util.ShareCard
import java.util.Calendar

/** Charts: how much was spent in a week, month or year, where it went, and a picture to share. */
@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    uiState: KharchUiState,
    viewModel: KharchViewModel
) {
    val kind = uiState.periodKind
    val offset = uiState.periodOffset
    val summary = remember(uiState.transactions, kind, offset) {
        MoneyMath.summarize(uiState.transactions, MoneyMath.periodRange(kind, offset))
    }
    val previous = remember(uiState.transactions, kind, offset) {
        MoneyMath.summarize(uiState.transactions, MoneyMath.periodRange(kind, offset - 1))
    }
    val comparable = remember(uiState.transactions, kind, offset) {
        MoneyMath.comparableSpent(uiState.transactions, kind, offset)
    }
    var showShare by remember { mutableStateOf(false) }
    var showCsv by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("understand_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ScreenPadding, end = 4.dp, top = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Charts", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("See where your money goes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = { showCsv = true }, modifier = Modifier.testTag("understand_export_csv_btn")) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Save as a file", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showShare = true }, modifier = Modifier.testTag("analytics_share_visual_btn")) {
                        Icon(Icons.Default.Share, contentDescription = "Share a picture", tint = EmeraldPrimary)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PickChip("Week", kind == PeriodKind.WEEK, { viewModel.setPeriodKind(PeriodKind.WEEK) })
                PickChip("Month", kind == PeriodKind.MONTH, { viewModel.setPeriodKind(PeriodKind.MONTH) })
                PickChip("Year", kind == PeriodKind.YEAR, { viewModel.setPeriodKind(PeriodKind.YEAR) })
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.changePeriodOffset(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Earlier", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(summary.range.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = { viewModel.changePeriodOffset(1) }, enabled = offset < 0) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Later",
                        tint = if (offset < 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                    )
                }
            }
        }

        item {
            Box(Modifier.padding(horizontal = ScreenPadding, vertical = 6.dp).entrance(0)) {
                KharchCard {
                    Text("You spent", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Rs.", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 5.dp, end = 6.dp))
                        AnimatedAmount(value = summary.spent, style = MaterialTheme.typography.displayMedium)
                    }
                    MoneyMath.changeText(summary.spent, comparable)?.let { text ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (summary.spent <= comparable) EmeraldPrimary else ExpenseRed
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniStat("Each day", formatRs(summary.averagePerDay), Modifier.weight(1f))
                        MiniStat("No spending", "${summary.noSpendDays} ${if (summary.noSpendDays == 1) "day" else "days"}", Modifier.weight(1f))
                        MiniStat("Records", "${summary.transactionCount}", Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Box(Modifier.padding(horizontal = ScreenPadding, vertical = 6.dp).entrance(1)) {
                KharchCard {
                    Text(
                        if (kind == PeriodKind.YEAR) "Each month" else "Each day",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    BarsChart(bars = summary.bars, kind = kind, busiest = summary.busiestBar)
                }
            }
        }

        item {
            Box(Modifier.padding(horizontal = ScreenPadding, vertical = 6.dp).entrance(2)) {
                KharchCard {
                    Text("Where it went", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    if (summary.byCategory.isEmpty()) {
                        Text("No spending in this time.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            summary.byCategory.take(6).forEach { CategoryBar(it) }
                        }
                    }
                }
            }
        }

        val notes = insights(summary, previous)
        if (notes.isNotEmpty()) {
            item {
                Box(Modifier.padding(horizontal = ScreenPadding, vertical = 6.dp).entrance(3)) {
                    KharchCard {
                        Text("Good to know", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            notes.forEach { note ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        Modifier
                                            .padding(top = 8.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldPrimary)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Box(Modifier.padding(horizontal = ScreenPadding, vertical = 10.dp)) {
                PrimaryButton(text = "Share as a picture", onClick = { showShare = true })
            }
        }
    }

    if (showShare) {
        ShareStorySheet(summary = summary, previousSpent = comparable, name = uiState.profile.name, onDismiss = { showShare = false })
    }
    if (showCsv) {
        val cal = remember { Calendar.getInstance() }
        ExportCsvDialog(
            monthLabel = summary.range.label,
            calendar = cal,
            expenses = uiState.transactions.filter { it.timestamp >= summary.range.start && it.timestamp < summary.range.endExclusive },
            onDismiss = { showCsv = false }
        )
    }
}

private fun insights(summary: PeriodSummary, previous: PeriodSummary): List<String> {
    if (summary.spent <= 0) return emptyList()
    val notes = mutableListOf<String>()
    summary.byCategory.firstOrNull()?.let {
        notes += "Most of your money went to ${it.category} (${(it.share * 100).toInt()}%)."
    }
    if (summary.range.kind != PeriodKind.YEAR) {
        fun isWeekend(ms: Long) = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.DAY_OF_WEEK).let {
            it == Calendar.SATURDAY || it == Calendar.SUNDAY
        }
        val weekend = summary.bars.filter { isWeekend(it.dayStart) }.map { it.amount }.average().let { if (it.isNaN()) 0.0 else it }
        val weekday = summary.bars.filter { !isWeekend(it.dayStart) }.map { it.amount }.average().let { if (it.isNaN()) 0.0 else it }
        when {
            weekday > 0 && weekend > weekday * 1.35 -> notes += "You spend more on Saturday and Sunday."
            weekend > 0 && weekday > weekend * 1.35 -> notes += "You spend more on work days than on the weekend."
        }
    }
    summary.biggestExpense?.let { notes += "Your biggest spend was ${it.title} (${formatRs(it.amount)})." }
    if (previous.spent > 0 && summary.range.kind == PeriodKind.MONTH && summary.spent > previous.spent) {
        notes += "You spent ${formatRs(summary.spent - previous.spent)} more than before."
    }
    return notes.take(4)
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
private fun CategoryBar(item: CategoryTotal) {
    val cat = ExpenseCategory.fromString(item.category)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = cat.icon(), tint = cat.color, size = 30.dp, iconSize = 16.dp)
            Spacer(Modifier.width(10.dp))
            Text(item.category, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(formatRs(item.amount), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(8.dp))
            Text("${(item.share * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(36.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(item.share.coerceIn(0.02f, 1f))
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(cat.color)
            )
        }
    }
}

/** Bars you can tap. The tapped bar shows its exact amount above the chart. */
@Composable
private fun BarsChart(bars: List<DayTotal>, kind: PeriodKind, busiest: DayTotal?) {
    var selected by remember(bars) { mutableIntStateOf(bars.indexOfFirst { it == busiest }) }
    val maxValue = (bars.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(1.0)
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val barColor = EmeraldPrimary
    val dimColor = EmeraldPrimary.copy(alpha = 0.35f)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val dateFmt = remember { java.text.SimpleDateFormat(if (kind == PeriodKind.YEAR) "MMMM" else "EEE, d MMM", java.util.Locale.getDefault()) }

    val sel = bars.getOrNull(selected)
    Text(
        text = if (sel != null && sel.amount > 0) "${dateFmt.format(sel.dayStart)}: ${formatRs(sel.amount)}" else "Tap a bar to see the amount",
        style = MaterialTheme.typography.bodyMedium,
        color = if (sel != null && sel.amount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(10.dp))
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .pointerInput(bars) {
                detectTapGestures { pos ->
                    if (bars.isNotEmpty()) {
                        val slot = size.width / bars.size
                        selected = (pos.x / slot).toInt().coerceIn(0, bars.lastIndex)
                    }
                }
            }
    ) {
        if (bars.isEmpty()) return@Canvas
        val labelArea = 26.dp.toPx()
        val chartHeight = size.height - labelArea
        val slot = size.width / bars.size
        val barWidth = (slot * 0.62f).coerceAtMost(26.dp.toPx())
        val every = when {
            bars.size <= 12 -> 1
            bars.size <= 16 -> 2
            else -> 5
        }
        bars.forEachIndexed { i, bar ->
            val x = i * slot + (slot - barWidth) / 2f
            drawRoundRect(trackColor, Offset(x, 0f), Size(barWidth, chartHeight), CornerRadius(barWidth / 2f))
            val h = (bar.amount / maxValue * chartHeight).toFloat()
            if (h > 0f) {
                drawRoundRect(
                    color = if (i == selected) barColor else dimColor,
                    topLeft = Offset(x, chartHeight - h.coerceAtLeast(barWidth)),
                    size = Size(barWidth, h.coerceAtLeast(barWidth)),
                    cornerRadius = CornerRadius(barWidth / 2f)
                )
            }
            if (i % every == 0 || i == selected) {
                val layout = measurer.measure(bar.label, androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = if (i == selected) barColor else labelColor))
                drawText(layout, topLeft = Offset(x + barWidth / 2f - layout.size.width / 2f, chartHeight + 6.dp.toPx()))
            }
        }
    }
}

/** Preview of the picture, a switch to hide the amounts, and the Share button. */
@Composable
private fun ShareStorySheet(summary: PeriodSummary, previousSpent: Double, name: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var showAmounts by remember { mutableStateOf(true) }
    val bitmap = remember(summary, previousSpent, showAmounts, name) {
        ShareCard.render(summary, previousSpent, showAmounts, name)
    }
    FormSheet(title = "Share your story", subtitle = "A picture you can send to anyone.", onDismiss = onDismiss) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Picture to share",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ShareCard.WIDTH.toFloat() / ShareCard.HEIGHT)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show the amounts", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("Turn off to share only percentages.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = showAmounts,
                onCheckedChange = { showAmounts = it },
                colors = SwitchDefaults.colors(checkedTrackColor = EmeraldPrimary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
        PrimaryButton(
            text = "Share picture",
            onClick = {
                val file = ShareCard.save(context, bitmap)
                val text = "My ${summary.range.label.lowercase()} with Kharch"
                context.startActivity(Intent.createChooser(ShareCard.shareIntent(context, file, text), "Share"))
            }
        )
    }
}

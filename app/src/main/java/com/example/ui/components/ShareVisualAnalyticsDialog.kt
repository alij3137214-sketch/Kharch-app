package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.CategoryShare
import com.example.util.VisualAnalyticsExporter
import java.util.Locale

@Composable
fun ShareVisualAnalyticsDialog(
    periodLabel: String,
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double,
    topCategories: List<CategoryShare>,
    avgDailyExpense: Double,
    txCount: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val shareableText = remember(periodLabel, totalIncome, totalExpense, netBalance, topCategories) {
        VisualAnalyticsExporter.generateShareableSummaryText(
            periodLabel = periodLabel,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            topCategories = topCategories,
            avgDailyExpense = avgDailyExpense,
            txCount = txCount
        )
    }

    val savingsRate = remember(totalIncome, netBalance) {
        if (totalIncome > 0) ((netBalance / totalIncome) * 100).coerceIn(-100.0, 100.0) else 0.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("share_visual_analytics_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Share Visual Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Send visual report to WhatsApp or other apps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Visual Report Card Preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("visual_report_preview_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0B1424)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF1E2D44))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KHARCH ANALYTICS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = periodLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Income
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF132238))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "INCOME",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "+Rs. ${String.format(Locale.getDefault(), "%,.0f", totalIncome)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                            }

                            // Expense
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF132238))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "EXPENSES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "-Rs. ${String.format(Locale.getDefault(), "%,.0f", totalExpense)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }
                            }
                        }

                        // Net Row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF132238))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NET CASH FLOW (${String.format(Locale.getDefault(), "%.1f", savingsRate)}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "${if (netBalance >= 0) "+Rs. " else "-Rs. "}${String.format(Locale.getDefault(), "%,.0f", kotlin.math.abs(netBalance))}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netBalance >= 0) EmeraldPrimary else ExpenseRed
                                )
                            }
                        }

                        // Top Spending Categories with progress bars
                        if (topCategories.isNotEmpty()) {
                            Text(
                                text = "TOP SPENDING CATEGORIES",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )

                            topCategories.take(3).forEach { cat ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = cat.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", cat.amount)} (${String.format(Locale.getDefault(), "%.0f", cat.percentage)}%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.LightGray
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (cat.percentage / 100f).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = EmeraldPrimary,
                                        trackColor = Color(0xFF1F304A)
                                    )
                                }
                            }
                        }
                    }
                }

                // Sharing Actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. Share as Visual Image (PNG)
                    Button(
                        onClick = {
                            try {
                                val imageFile = VisualAnalyticsExporter.createVisualAnalyticsImage(
                                    context = context,
                                    periodLabel = periodLabel,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    netBalance = netBalance,
                                    topCategories = topCategories,
                                    avgDailyExpense = avgDailyExpense,
                                    txCount = txCount
                                )
                                val shareIntent = VisualAnalyticsExporter.createShareImageIntent(
                                    context = context,
                                    imageFile = imageFile,
                                    summaryText = shareableText
                                )
                                context.startActivity(Intent.createChooser(shareIntent, "Share Visual Analytics"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to create image: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("share_image_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Visual Image Card", fontWeight = FontWeight.Bold)
                    }

                    // 2. Share Formatted Text (WhatsApp ready)
                    Button(
                        onClick = {
                            val textIntent = VisualAnalyticsExporter.createShareTextIntent(shareableText)
                            // Attempt WhatsApp direct if installed, else fallback to standard chooser
                            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                `package` = "com.whatsapp"
                                putExtra(Intent.EXTRA_TEXT, shareableText)
                            }
                            try {
                                if (whatsappIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(whatsappIntent)
                                } else {
                                    context.startActivity(Intent.createChooser(textIntent, "Share Analytics to WhatsApp or others"))
                                }
                            } catch (e: Exception) {
                                context.startActivity(Intent.createChooser(textIntent, "Share Analytics"))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("share_whatsapp_text_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share to WhatsApp / Apps", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // 3. Copy to clipboard
                    OutlinedButton(
                        onClick = {
                            VisualAnalyticsExporter.copyToClipboard(context, shareableText)
                            Toast.makeText(context, "Analytics summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("copy_analytics_text_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Summary Text")
                    }
                }
            }
        },
        confirmButton = {}
    )
}

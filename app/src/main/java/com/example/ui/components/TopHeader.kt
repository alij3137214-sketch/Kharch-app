package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed

private val NeonEmerald = Color(0xFF00FFA3)
private val NeonCyan = Color(0xFF00E5FF)
private val CyberBg = Color(0xFF060E1A)

@Composable
fun TopHeader(
    modifier: Modifier = Modifier,
    userName: String? = null,
    onSearchClick: () -> Unit = {},
    onOpenVaultEntrance: () -> Unit = {},
    onSeedRandomData: () -> Unit = {},
    onClearAllData: () -> Unit = {}
) {
    var showVaultMenu by remember { mutableStateOf(false) }

    val welcomeText = remember(userName) {
        if (userName.isNullOrBlank()) "Welcome" else "Welcome, $userName"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Interactive Cyberpunk Bank Vault Logo
            InteractiveCyberBankLogo(
                onClick = { showVaultMenu = true }
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = welcomeText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    // Cyber Vault status indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0D2818))
                            .border(1.dp, NeonEmerald.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable { onOpenVaultEntrance() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("cyber_vault_badge")
                    ) {
                        Text(
                            text = "VAULT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = NeonEmerald
                        )
                    }

                    // Smaller remove data icon beside welcome vault
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExpenseRed.copy(alpha = 0.12f))
                            .clickable { onClearAllData() }
                            .padding(4.dp)
                            .testTag("header_clear_data_icon"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove Data",
                            tint = ExpenseRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Vault & Test Data Options button
            IconButton(
                onClick = { showVaultMenu = true },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .testTag("vault_controls_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Bank Vault & Data Controls",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .testTag("search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search transactions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Cyberpunk Bank & Data Controls Dialog
    if (showVaultMenu) {
        CyberVaultControlsDialog(
            onDismiss = { showVaultMenu = false },
            onEnterVault = {
                showVaultMenu = false
                onOpenVaultEntrance()
            },
            onSeedRandomData = {
                showVaultMenu = false
                onSeedRandomData()
            },
            onClearAllData = {
                showVaultMenu = false
                onClearAllData()
            }
        )
    }
}

/**
 * Interactive miniature Cyberpunk Bank Logo with rotating circuit gear and neon pulsing "K" core
 */
@Composable
fun InteractiveCyberBankLogo(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_mini_logo")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mini_ring"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mini_pulse"
    )

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("cyberpunk_header_logo"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension / 2

            // Dark cyber base
            drawCircle(
                color = Color(0xFF040A14),
                radius = r,
                center = center
            )

            // Outer cyan glowing ring with ticks
            rotate(rotation, pivot = center) {
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.5f * glowPulse),
                    radius = r * 0.88f,
                    center = center,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )
            }

            // Hexagon core
            val hexR = r * 0.60f
            val hexPath = Path().apply {
                for (i in 0 until 6) {
                    val angle = (i * 60f - 30f) * (Math.PI / 180f).toFloat()
                    val x = center.x + hexR * kotlin.math.cos(angle)
                    val y = center.y + hexR * kotlin.math.sin(angle)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }

            drawPath(
                path = hexPath,
                color = Color(0xFF0F2642)
            )
            drawPath(
                path = hexPath,
                color = NeonEmerald.copy(alpha = 0.9f * glowPulse),
                style = Stroke(width = 2.dp.toPx())
            )

            // Stylized "K" emblem inside
            val kSize = hexR * 0.65f
            val leftX = center.x - kSize * 0.35f
            val topY = center.y - kSize * 0.65f
            val botY = center.y + kSize * 0.65f

            drawLine(
                color = NeonEmerald,
                start = Offset(leftX, topY),
                end = Offset(leftX, botY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = NeonCyan,
                start = Offset(leftX, center.y),
                end = Offset(center.x + kSize * 0.45f, topY),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = NeonCyan,
                start = Offset(leftX, center.y),
                end = Offset(center.x + kSize * 0.45f, botY),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Cyberpunk Bank Vault & Test Data Management Dialog
 */
@Composable
fun CyberVaultControlsDialog(
    onDismiss: () -> Unit,
    onEnterVault: () -> Unit,
    onSeedRandomData: () -> Unit,
    onClearAllData: () -> Unit
) {
    var showConfirmClear by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = NeonEmerald,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "VAULT & DATA",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Enter Cyberpunk Bank Vault Animation
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onEnterVault() }
                        .testTag("dialog_enter_cyber_vault"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Vault Animation",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald
                            )
                            Text(
                                text = "Interactive 3D view",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // 2. Generate Random Test Data
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSeedRandomData() }
                        .testTag("dialog_seed_random_data"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Sample Data",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Seed test transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Clear All Dummy Data (Ready for Real Data)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showConfirmClear = true }
                        .testTag("dialog_clear_all_data"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Clear Data",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                            Text(
                                text = "Reset all records to 0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_close_button")
            ) {
                Text("Close")
            }
        }
    )

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("Clear All Data?") },
            text = { Text("Reset all records to start fresh?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClear = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    modifier = Modifier.testTag("confirm_clear_button")
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


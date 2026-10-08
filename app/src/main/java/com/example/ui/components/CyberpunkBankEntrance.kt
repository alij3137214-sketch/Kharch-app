package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Cyberpunk Palette
private val CyberBgDark = Color(0xFF030712)
private val CyberBgGradient = Color(0xFF070D1F)
private val NeonEmerald = Color(0xFF00FFA3)
private val NeonCyan = Color(0xFF00E5FF)
private val NeonViolet = Color(0xFFBD00FF)
private val CyberGold = Color(0xFFFFD600)
private val GridLineColor = Color(0xFF0E2238)

data class CyberTouchRipple(
    val x: Float,
    val y: Float,
    val initialRadius: Float = 10f,
    val maxRadius: Float = 220f,
    val id: Long = System.currentTimeMillis()
)

data class CyberParticle(
    var x: Float,
    var y: Float,
    val speed: Float,
    val radius: Float,
    val color: Color
)

@Composable
fun CyberpunkBankEntrance(
    modifier: Modifier = Modifier,
    onEnterVault: () -> Unit,
    userName: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_loop")

    // Animations
    val ringRotationOuter by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_ring"
    )

    val ringRotationInner by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_ring"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline"
    )

    val gridOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_offset"
    )

    // Interactive Touch shockwaves
    val ripples = remember { mutableStateListOf<CyberTouchRipple>() }
    var tapCounter by remember { mutableIntStateOf(0) }

    // Telemetry Typing Steps
    val telemetryMessages = remember(userName) {
        listOf(
            "CONNECTING...",
            "AUTHENTICATING...",
            "ACCESS GRANTED",
            if (userName.isNullOrBlank()) "WELCOME" else "WELCOME, $userName"
        )
    }
    var currentTelemetryIndex by remember { mutableIntStateOf(0) }
    var displayedTelemetryText by remember { mutableStateOf("") }
    var isVaultReady by remember { mutableStateOf(false) }
    var isDoorOpening by remember { mutableStateOf(false) }

    // Particle field
    val particles = remember {
        List(25) {
            CyberParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = Random.nextFloat() * 0.003f + 0.001f,
                radius = Random.nextFloat() * 2f + 1f,
                color = if (it % 2 == 0) NeonCyan else NeonEmerald
            )
        }
    }

    // Telemetry text progression
    LaunchedEffect(Unit) {
        for (i in telemetryMessages.indices) {
            currentTelemetryIndex = i
            val fullText = telemetryMessages[i]
            for (charIdx in 1..fullText.length) {
                displayedTelemetryText = fullText.substring(0, charIdx)
                delay(18)
            }
            delay(500)
        }
        isVaultReady = true
    }

    // Auto-enter countdown or manual
    LaunchedEffect(isVaultReady) {
        if (isVaultReady) {
            delay(2800)
            if (!isDoorOpening) {
                isDoorOpening = true
                delay(400)
                onEnterVault()
            }
        }
    }

    val doorScale by animateFloatAsState(
        targetValue = if (isDoorOpening) 1.6f else 1f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "door_scale"
    )

    val doorAlpha by animateFloatAsState(
        targetValue = if (isDoorOpening) 0f else 1f,
        animationSpec = tween(450, easing = LinearEasing),
        label = "door_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(CyberBgDark, CyberBgGradient, CyberBgDark)
                )
            )
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    tapCounter++
                    ripples.add(CyberTouchRipple(x = offset.x, y = offset.y))
                }
            }
            .scale(doorScale)
            .drawWithContent {
                drawContent()
            }
            .testTag("cyberpunk_bank_entrance")
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Background Cyberpunk Grid & Floating Neon Embers
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCyberGrid(gridOffset, width, height)

            // Draw and update ambient particles
            particles.forEach { p ->
                p.y -= p.speed
                if (p.y < 0f) p.y = 1f
                drawCircle(
                    color = p.color.copy(alpha = 0.35f),
                    radius = p.radius.dp.toPx(),
                    center = Offset(p.x * size.width, p.y * size.height)
                )
            }

            // Draw interactive touch shockwaves
            val iterator = ripples.iterator()
            while (iterator.hasNext()) {
                val ripple = iterator.next()
                val age = (System.currentTimeMillis() - ripple.id).toFloat()
                val progress = (age / 700f).coerceIn(0f, 1f)
                val currentRadius = ripple.initialRadius + (ripple.maxRadius - ripple.initialRadius) * progress
                val alpha = (1f - progress).coerceIn(0f, 1f)

                drawCircle(
                    color = NeonCyan.copy(alpha = alpha * 0.7f),
                    radius = currentRadius,
                    center = Offset(ripple.x, ripple.y),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = NeonEmerald.copy(alpha = alpha * 0.4f),
                    radius = currentRadius * 0.7f,
                    center = Offset(ripple.x, ripple.y),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                if (progress >= 1f) {
                    iterator.remove()
                }
            }
        }

        // Top HUD Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
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
                        .background(NeonEmerald)
                )
                Text(
                    text = "KHARCH // BANK VAULT 2.0",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    ),
                    color = NeonEmerald,
                    fontWeight = FontWeight.Bold
                )
            }

            // Skip / Fast Enter Chip
            Surface(
                onClick = {
                    if (!isDoorOpening) {
                        isDoorOpening = true
                        onEnterVault()
                    }
                },
                shape = RoundedCornerShape(20.dp),
                color = Color(0x2200FFA3),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "FAST ENTER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = NeonEmerald
                    )
                }
            }
        }

        // Center Cyberpunk Bank Vault Core
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Interactive 3D Holographic Vault Logo
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clickable {
                        tapCounter++
                        ripples.add(CyberTouchRipple(x = width / 2f, y = height / 2f))
                    },
                contentAlignment = Alignment.Center
            ) {
                // Interactive Cyberpunk Vault Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val maxR = size.minDimension / 2

                    // 1. Holographic outer aura glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonEmerald.copy(alpha = 0.25f * glowPulse),
                                NeonCyan.copy(alpha = 0.12f * glowPulse),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxR * 1.15f
                        ),
                        radius = maxR * 1.15f,
                        center = center
                    )

                    // 2. Outer Segmented Circuit Gear (Rotates Clockwise)
                    rotate(ringRotationOuter, pivot = center) {
                        drawCircle(
                            color = NeonCyan.copy(alpha = 0.35f),
                            radius = maxR * 0.95f,
                            center = center,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 14f, 8f, 14f), 0f)
                            )
                        )

                        // Outer cardinal security ticks
                        for (i in 0 until 8) {
                            val angle = (i * 45f) * (Math.PI / 180f).toFloat()
                            val p1 = Offset(center.x + (maxR * 0.90f) * cos(angle), center.y + (maxR * 0.90f) * sin(angle))
                            val p2 = Offset(center.x + (maxR * 1.0f) * cos(angle), center.y + (maxR * 1.0f) * sin(angle))
                            drawLine(
                                color = NeonEmerald,
                                start = p1,
                                end = p2,
                                strokeWidth = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // 3. Middle Biometric Hex Ring (Rotates Counter-Clockwise)
                    rotate(ringRotationInner, pivot = center) {
                        drawCircle(
                            color = NeonEmerald.copy(alpha = 0.55f),
                            radius = maxR * 0.76f,
                            center = center,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(35f, 20f), 0f)
                            )
                        )

                        // Secondary micro ticks
                        for (i in 0 until 12) {
                            val angle = (i * 30f) * (Math.PI / 180f).toFloat()
                            val p1 = Offset(center.x + (maxR * 0.70f) * cos(angle), center.y + (maxR * 0.70f) * sin(angle))
                            val p2 = Offset(center.x + (maxR * 0.76f) * cos(angle), center.y + (maxR * 0.76f) * sin(angle))
                            drawLine(
                                color = NeonCyan.copy(alpha = 0.8f),
                                start = p1,
                                end = p2,
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }

                    // 4. Inner Neon Vault Core (Geometric Hexagon Shield)
                    val hexRadius = maxR * 0.52f
                    val hexPath = Path().apply {
                        for (i in 0 until 6) {
                            val angle = (i * 60f - 30f) * (Math.PI / 180f).toFloat()
                            val x = center.x + hexRadius * cos(angle)
                            val y = center.y + hexRadius * sin(angle)
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }

                    // Fill core hexagon with deep cyber acrylic
                    drawPath(
                        path = hexPath,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0F2642),
                                Color(0xFF040A14)
                            ),
                            center = center,
                            radius = hexRadius
                        )
                    )

                    // Hexagon Neon Border
                    drawPath(
                        path = hexPath,
                        color = NeonEmerald.copy(alpha = 0.85f * glowPulse),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // 5. Stylized Kharch "K" Cyber Emblem inside core
                    val emblemSize = hexRadius * 0.65f
                    val leftBarX = center.x - emblemSize * 0.40f
                    val topY = center.y - emblemSize * 0.65f
                    val bottomY = center.y + emblemSize * 0.65f

                    // Vertical Stem
                    drawLine(
                        color = NeonEmerald,
                        start = Offset(leftBarX, topY),
                        end = Offset(leftBarX, bottomY),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Upper diagonal branch
                    drawLine(
                        color = NeonCyan,
                        start = Offset(leftBarX, center.y - 2.dp.toPx()),
                        end = Offset(center.x + emblemSize * 0.50f, topY),
                        strokeWidth = 4.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Lower diagonal branch
                    drawLine(
                        color = NeonCyan,
                        start = Offset(leftBarX + 6.dp.toPx(), center.y - 4.dp.toPx()),
                        end = Offset(center.x + emblemSize * 0.55f, bottomY),
                        strokeWidth = 4.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Glowing center dot node
                    drawCircle(
                        color = CyberGold,
                        radius = 4.dp.toPx(),
                        center = Offset(leftBarX, center.y)
                    )

                    // 6. Laser Scan Beam sweeping across the vault core
                    val scanYPos = center.y - hexRadius + (hexRadius * 2f * scanLineY)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                NeonEmerald.copy(alpha = 0.8f),
                                NeonCyan,
                                NeonEmerald.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(center.x - hexRadius * 0.85f, scanYPos),
                        end = Offset(center.x + hexRadius * 0.85f, scanYPos),
                        strokeWidth = 2.5.dp.toPx()
                    )
                }

                // Interactive tap hint overlay
                if (tapCounter == 0) {
                    Text(
                        text = "TAP TO SCAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        ),
                        color = NeonCyan.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bank Name & Protocol Title
            Text(
                text = "KHARCH",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 6.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = "VAULT",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp
                ),
                color = NeonCyan,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Telemetry Terminal Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x99050C1B)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = NeonCyan.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Security",
                                tint = NeonEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "SYS.AUTH // VERIFIED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = NeonEmerald
                            )
                        }

                        Text(
                            text = if (isVaultReady) "ONLINE" else "DECRYPTING...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isVaultReady) NeonEmerald else CyberGold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = displayedTelemetryText + if (!isVaultReady) "_" else "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        ),
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Interactive Enter Vault Action Button
            Button(
                onClick = {
                    if (!isDoorOpening) {
                        isDoorOpening = true
                        onEnterVault()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp))
                    .testTag("enter_vault_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonEmerald
                ),
                shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Unlock",
                        tint = Color(0xFF030A14),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ENTER VAULT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF030A14)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tap logo to scan",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}

/**
 * Draws futuristic cyber perspective grid lines receding into horizon
 */
private fun DrawScope.drawCyberGrid(offsetY: Float, width: Float, height: Float) {
    val horizon = height * 0.45f
    val gridSpacing = 36.dp.toPx()

    // Horizontal receding lines
    var y = horizon
    var step = 6f
    while (y < height) {
        val alpha = ((y - horizon) / (height - horizon)).coerceIn(0f, 0.4f)
        drawLine(
            color = GridLineColor.copy(alpha = alpha),
            start = Offset(0f, y + (offsetY % step)),
            end = Offset(width, y + (offsetY % step)),
            strokeWidth = 1.dp.toPx()
        )
        y += step
        step *= 1.18f
    }

    // Vertical perspective lines converging to center horizon
    val vanishPoint = Offset(width / 2f, horizon)
    val numLines = 14
    for (i in -numLines / 2..numLines / 2) {
        val bottomX = (width / 2f) + (i * gridSpacing * 1.5f)
        val alpha = (1f - kotlin.math.abs(i) / (numLines.toFloat())).coerceIn(0.1f, 0.35f)
        drawLine(
            color = GridLineColor.copy(alpha = alpha),
            start = vanishPoint,
            end = Offset(bottomX, height),
            strokeWidth = 1.dp.toPx()
        )
    }
}

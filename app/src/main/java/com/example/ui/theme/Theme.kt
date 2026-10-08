package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val KharchColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF05241A),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = CyanAccent,
    onSecondary = Color(0xFF0B2036),
    background = RichNavyBg,
    onBackground = InkPrimary,
    surface = RichNavySurface,
    onSurface = InkPrimary,
    surfaceVariant = RichNavySurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = RichNavyBorder,
    outlineVariant = RichNavyBorder,
    error = ExpenseRed
)

private val KharchShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KharchTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KharchColorScheme,
        typography = Typography,
        shapes = KharchShapes,
        content = content
    )
}

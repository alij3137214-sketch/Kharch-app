package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * BudgetIt Signature Theme: Rich Navy default with vibrant emerald & neon cyan accents.
 */
private val RichNavyColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF031A12),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = CyanAccent,
    onSecondary = Color(0xFF002330),
    background = RichNavyBg,
    onBackground = Color(0xFFF1F5F9),
    surface = RichNavySurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = RichNavySurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = RichNavyBorder,
    error = ExpenseRed
)

@Composable
fun KharchTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RichNavyColorScheme,
        typography = Typography,
        content = content
    )
}

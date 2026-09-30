package com.example.servinow.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ServiNowColors = lightColorScheme(
    primary = Color(0xFF1664C0), onPrimary = Color.White,
    primaryContainer = Color(0xFFEDF5FF), onPrimaryContainer = Color(0xFF172D4D),
    background = Color.White, onBackground = Color(0xFF172D4D),
    surface = Color.White, onSurface = Color(0xFF172D4D),
    onSurfaceVariant = Color(0xFF5C6E85), outline = Color(0xFF83ADE1),
    error = Color(0xFFB52E43),
)

@Composable
fun ServiNowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ServiNowColors, content = content)
}

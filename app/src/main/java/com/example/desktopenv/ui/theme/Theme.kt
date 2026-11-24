package com.example.desktopenv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.desktopenv.data.ThemeChoice

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF0B1F45),
    primaryContainer = Color(0xFF2A3F6B),
    secondary = Color(0xFF9FD7C3),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF232A35),
    onSurfaceVariant = Color(0xFFB7C0CE),
    outline = Color(0xFF3A4454),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2353C4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E3FF),
    secondary = Color(0xFF2B7A67),
    background = Color(0xFFEEF2F8),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE3E8F1),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFC3CBD8),
)

private val DesktopShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

/** Papéis de parede originais, gerados com gradientes. Quantidade igual a WALLPAPER_COUNT. */
val Wallpapers: List<Brush> = listOf(
    Brush.verticalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))),
    Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF134E4A), Color(0xFF042F2E))),
    Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFFF97316))),
    Brush.verticalGradient(listOf(Color(0xFF1F2937), Color(0xFF475569))),
)

@Composable
fun DesktopEnvTheme(choice: ThemeChoice, content: @Composable () -> Unit) {
    val dark = when (choice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = DesktopShapes,
        content = content,
    )
}

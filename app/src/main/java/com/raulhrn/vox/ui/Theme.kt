package com.raulhrn.vox.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class VoxColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    /** Amber: what you sing, primary actions, good pitch. */
    val accent: Color,
    val onAccent: Color,
    /** Sand: the lead melody (regente). */
    val melody: Color,
    /** Rose: other harmony / backing parts. */
    val rose: Color,
    val warn: Color,
    val bad: Color,
    val dark: Boolean,
)

// Warm palette: dark roasted brown or cream paper, amber accent, sand and terracotta tones.
private val Dark = VoxColors(
    bg = Color(0xFF140F0C),
    surface = Color(0xFF1E1713),
    surface2 = Color(0xFF2A201A),
    line = Color(0xFF3A2D24),
    text = Color(0xFFF8EDE2),
    muted = Color(0xFFB09A87),
    faint = Color(0xFF6A574A),
    accent = Color(0xFFF5A33B),
    onAccent = Color(0xFF2B1600),
    melody = Color(0xFFD8BFA6),
    rose = Color(0xFFE88A8F),
    warn = Color(0xFFEE6A35),
    bad = Color(0xFFD8434B),
    dark = true,
)

private val Light = VoxColors(
    bg = Color(0xFFFCF6EE),
    surface = Color(0xFFF5EBDD),
    surface2 = Color(0xFFEDDFCC),
    line = Color(0xFFE3D2BC),
    text = Color(0xFF2B1E15),
    muted = Color(0xFF7D6656),
    faint = Color(0xFFC2AD98),
    accent = Color(0xFFE08A12),
    onAccent = Color(0xFF2B1600),
    melody = Color(0xFF9E7B5D),
    rose = Color(0xFFC4525E),
    warn = Color(0xFFD5612A),
    bad = Color(0xFFB8302F),
    dark = false,
)

val LocalVox = staticCompositionLocalOf { Dark }

object Vox {
    val colors: VoxColors
        @Composable @ReadOnlyComposable
        get() = LocalVox.current
}

object VoxType {
    val display = TextStyle(fontSize = 104.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp, lineHeight = 108.sp)
    val title = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
    val heading = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
    val label = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val small = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    val caps = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
}

@Composable
fun VoxTheme(content: @Composable () -> Unit) {
    val c = if (isSystemInDarkTheme()) Dark else Light
    val scheme = if (c.dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text, surfaceVariant = c.surface2, onSurfaceVariant = c.muted,
            outline = c.line, secondary = c.melody,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text, surfaceVariant = c.surface2, onSurfaceVariant = c.muted,
            outline = c.line, secondary = c.melody,
        )
    }
    CompositionLocalProvider(LocalVox provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

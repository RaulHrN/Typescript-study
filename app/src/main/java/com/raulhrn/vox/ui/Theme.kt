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
    val accent: Color,
    val onAccent: Color,
    val melody: Color,
    val warn: Color,
    val bad: Color,
    val dark: Boolean,
)

private val Dark = VoxColors(
    bg = Color(0xFF0B0B0D),
    surface = Color(0xFF141417),
    surface2 = Color(0xFF1D1D21),
    line = Color(0xFF26262B),
    text = Color(0xFFF2F2F3),
    muted = Color(0xFF8D8D95),
    faint = Color(0xFF4A4A51),
    accent = Color(0xFF7DF0C2),
    onAccent = Color(0xFF06241A),
    melody = Color(0xFFA3A8FF),
    warn = Color(0xFFFFC27A),
    bad = Color(0xFFFF7A7A),
    dark = true,
)

private val Light = VoxColors(
    bg = Color(0xFFFAFAF8),
    surface = Color(0xFFF1F1EE),
    surface2 = Color(0xFFE8E8E4),
    line = Color(0xFFDDDDD8),
    text = Color(0xFF111113),
    muted = Color(0xFF66666E),
    faint = Color(0xFFB4B4BA),
    accent = Color(0xFF0B9A6A),
    onAccent = Color(0xFFFFFFFF),
    melody = Color(0xFF5A60D8),
    warn = Color(0xFFC77700),
    bad = Color(0xFFD83A3A),
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

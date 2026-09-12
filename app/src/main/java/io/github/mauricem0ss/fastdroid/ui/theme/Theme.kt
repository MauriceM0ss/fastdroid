package io.github.mauricem0ss.fastdroid.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mauricem0ss.fastdroid.data.AppTheme

/**
 * Tokens ported from CyberNewsHub's style.css `neon` and `github` blocks, the
 * app family's palette source of truth. Names match the CSS variables.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accentHi: Color,
    val accent2: Color,
    val accentDim: Color,
    val danger: Color,
    val success: Color,
    val star: Color,
    val onAccent: Color,
    val headerBg: Color,
    val headerBorder: Color,
    val headerText: Color,
    val headerFg: Color,
    val headerLogo: Color,
    val bodyFont: FontFamily,
    val radius: Dp,
)

val TerminalPalette = Palette(
    bg = Color(0xFF07090B),
    surface = Color(0xFF0E1114),
    surface2 = Color(0xFF161B20),
    border = Color(0x24788C96),
    text = Color(0xFFE6EDF2),
    muted = Color(0xFF7D8A93),
    accent = Color(0xFF39FF14),
    accentHi = Color(0xFF5DFF3A),
    accent2 = Color(0xFF19F0FF),
    accentDim = Color(0x1F39FF14),
    danger = Color(0xFFFF5563),
    success = Color(0xFF39FF14),
    star = Color(0xFFFFB13D),
    onAccent = Color(0xFF07090B),
    headerBg = Color(0xFF000000),
    headerBorder = Color(0x24FFFFFF),
    headerText = Color(0xFFE6EDF2),
    headerFg = Color(0xFF19F0FF),
    headerLogo = Color(0xFF39FF14),
    bodyFont = FontFamily.Monospace,
    radius = 6.dp,
)

val GithubPalette = Palette(
    bg = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surface2 = Color(0xFF21262D),
    border = Color(0xFF30363D),
    text = Color(0xFFE6EDF3),
    muted = Color(0xFF8B949E),
    accent = Color(0xFF2F81F7),
    accentHi = Color(0xFF58A6FF),
    accent2 = Color(0xFF8B949E),
    accentDim = Color(0x262F81F7),
    danger = Color(0xFFF85149),
    success = Color(0xFF2EA043),
    star = Color(0xFFE3B341),
    onAccent = Color(0xFFFFFFFF),
    headerBg = Color(0xFF010409),
    headerBorder = Color(0xFF30363D),
    headerText = Color(0xFFE6EDF3),
    headerFg = Color(0xFF8B949E),
    headerLogo = Color(0xFF58A6FF),
    bodyFont = FontFamily.SansSerif,
    radius = 6.dp,
)

fun paletteFor(theme: AppTheme): Palette = when (theme) {
    AppTheme.TERMINAL -> TerminalPalette
    AppTheme.GITHUB -> GithubPalette
}

val LocalPalette = staticCompositionLocalOf { TerminalPalette }

/** Digits always use a monospaced face so the running timer doesn't jitter. */
val MonoDigits = FontFamily.Monospace

@Composable
fun FastDroidTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val p = paletteFor(theme)
    val colors = darkColorScheme(
        primary = p.accent,
        onPrimary = p.onAccent,
        primaryContainer = p.accentDim,
        onPrimaryContainer = p.accentHi,
        secondary = p.accent2,
        onSecondary = p.bg,
        tertiary = p.star,
        background = p.bg,
        onBackground = p.text,
        surface = p.surface,
        onSurface = p.text,
        surfaceVariant = p.surface2,
        onSurfaceVariant = p.muted,
        surfaceTint = Color.Transparent,
        surfaceContainerLowest = p.bg,
        surfaceContainerLow = p.surface,
        surfaceContainer = p.surface,
        surfaceContainerHigh = p.surface2,
        surfaceContainerHighest = p.surface2,
        outline = p.border,
        outlineVariant = p.border,
        error = p.danger,
        onError = p.bg,
    )
    // No colour in the text styles: that would override LocalContentColor, e.g. dark text on the accent button.
    val base = TextStyle(fontFamily = p.bodyFont)
    val typography = Typography(
        displayLarge = base.copy(fontFamily = MonoDigits, fontSize = 44.sp, fontWeight = FontWeight.Medium),
        headlineSmall = base.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = base.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = base.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.copy(fontSize = 16.sp),
        bodyMedium = base.copy(fontSize = 14.sp),
        bodySmall = base.copy(fontSize = 12.sp),
        labelLarge = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp),
    )
    val shape = RoundedCornerShape(p.radius)
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = Shapes(extraSmall = shape, small = shape, medium = shape, large = RoundedCornerShape(p.radius * 2)),
    ) {
        CompositionLocalProvider(LocalPalette provides p, LocalContentColor provides p.text, content = content)
    }
}

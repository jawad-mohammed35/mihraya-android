package com.mihraya.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.mihraya.app.data.settings.AccentColor

@Immutable
data class MihrayaColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val glass: Color,
    val glassStrong: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val danger: Color,
)

fun accentFor(accent: AccentColor, dark: Boolean): Color = when (accent) {
    AccentColor.Emerald -> if (dark) Color(0xFF6CC3A4) else Color(0xFF1D7A5E)
    AccentColor.Champagne -> if (dark) Color(0xFFDCC394) else Color(0xFF94743A)
    AccentColor.Silver -> if (dark) Color(0xFFCCD2D9) else Color(0xFF59616C)
    AccentColor.Lapis -> if (dark) Color(0xFF93B4E6) else Color(0xFF2F5A9A)
}

fun darkPalette(accent: AccentColor, pureBlack: Boolean = false): MihrayaColors {
    val a = accentFor(accent, dark = true)
    val ink = Color(0xFFF3F1EC)
    return MihrayaColors(
        isDark = true,
        background = if (pureBlack) Color.Black else Color(0xFF101114),
        surface = if (pureBlack) Color(0xFF0C0C0D) else Color(0xFF17181C),
        surfaceRaised = if (pureBlack) Color(0xFF151517) else Color(0xFF1F2126),
        glass = Color.White.copy(alpha = if (pureBlack) 0.045f else 0.055f),
        glassStrong = (if (pureBlack) Color(0xFF141416) else Color(0xFF1C1E24)).copy(alpha = 0.72f),
        glassBorder = Color.White.copy(alpha = if (pureBlack) 0.08f else 0.09f),
        glassHighlight = Color.White.copy(alpha = 0.17f),
        textPrimary = ink,
        textSecondary = ink.copy(alpha = 0.62f),
        textTertiary = ink.copy(alpha = 0.38f),
        divider = Color.White.copy(alpha = 0.07f),
        accent = a,
        accentSoft = a.copy(alpha = 0.14f),
        onAccent = Color(0xFF050505),
        danger = Color(0xFFE88A84),
    )
}

fun lightPalette(accent: AccentColor): MihrayaColors {
    val a = accentFor(accent, dark = false)
    val ink = Color(0xFF111312)
    return MihrayaColors(
        isDark = false,
        background = Color(0xFFF5F4F0),
        surface = Color(0xFFFFFFFF),
        surfaceRaised = Color(0xFFFFFFFF),
        glass = Color.White.copy(alpha = 0.74f),
        glassStrong = Color(0xFFFBFAF7).copy(alpha = 0.76f),
        glassBorder = Color.Black.copy(alpha = 0.07f),
        glassHighlight = Color.White,
        textPrimary = ink,
        textSecondary = ink.copy(alpha = 0.60f),
        textTertiary = ink.copy(alpha = 0.40f),
        divider = Color.Black.copy(alpha = 0.07f),
        accent = a,
        accentSoft = a.copy(alpha = 0.11f),
        onAccent = Color.White,
        danger = Color(0xFFB83A2E),
    )
}

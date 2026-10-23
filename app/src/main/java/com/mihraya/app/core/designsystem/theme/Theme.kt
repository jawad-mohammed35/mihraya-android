package com.mihraya.app.core.designsystem.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.mihraya.app.data.settings.AccentColor
import com.mihraya.app.data.settings.ThemeMode

val LocalMihrayaColors = staticCompositionLocalOf { darkPalette(AccentColor.Champagne) }
val LocalMihrayaType = staticCompositionLocalOf { DefaultType }
val LocalHapticsEnabled = staticCompositionLocalOf { true }
val LocalUse24Hour = staticCompositionLocalOf { false }

object Mihraya {
    val colors: MihrayaColors
        @Composable @ReadOnlyComposable get() = LocalMihrayaColors.current

    val type: MihrayaType
        @Composable @ReadOnlyComposable get() = LocalMihrayaType.current
}

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.Dark -> true
    ThemeMode.Light -> false
    ThemeMode.System -> isSystemInDarkTheme()
}

@Composable
private fun animated(target: Color): Color {
    val color by animateColorAsState(target, tween(420), label = "theme")
    return color
}

@Composable
fun MihrayaTheme(
    mode: ThemeMode,
    accent: AccentColor,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = isDarkTheme(mode)
    val target = if (dark) darkPalette(accent, pureBlack = pureBlack && mode == ThemeMode.Dark) else lightPalette(accent)
    val colors = target.copy(
        background = animated(target.background),
        surface = animated(target.surface),
        surfaceRaised = animated(target.surfaceRaised),
        glass = animated(target.glass),
        glassStrong = animated(target.glassStrong),
        glassBorder = animated(target.glassBorder),
        textPrimary = animated(target.textPrimary),
        textSecondary = animated(target.textSecondary),
        textTertiary = animated(target.textTertiary),
        divider = animated(target.divider),
        accent = animated(target.accent),
        accentSoft = animated(target.accentSoft),
        onAccent = animated(target.onAccent),
    )

    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentSoft,
            onPrimaryContainer = colors.textPrimary,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            secondaryContainer = colors.accentSoft,
            onSecondaryContainer = colors.textPrimary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceRaised,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainer = colors.surface,
            surfaceContainerLow = colors.surface,
            surfaceContainerHigh = colors.surfaceRaised,
            surfaceContainerHighest = colors.surfaceRaised,
            outline = colors.glassBorder,
            outlineVariant = colors.divider,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentSoft,
            onPrimaryContainer = colors.textPrimary,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            secondaryContainer = colors.accentSoft,
            onSecondaryContainer = colors.textPrimary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.background,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainer = colors.surface,
            surfaceContainerLow = colors.surface,
            surfaceContainerHigh = colors.surface,
            surfaceContainerHighest = colors.surface,
            outline = colors.glassBorder,
            outlineVariant = colors.divider,
            error = colors.danger,
        )
    }

    CompositionLocalProvider(
        LocalMihrayaColors provides colors,
        LocalMihrayaType provides DefaultType,
    ) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTypography, content = content)
    }
}

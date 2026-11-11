package com.mihraya.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.mihraya.app.data.settings.AccentColor
import com.mihraya.app.data.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {

    private val accent = AccentColor.Champagne
    private val charcoal = darkPalette(accent).background

    @Test
    fun systemFollowsTheDeviceAndKeepsOledBlack() {
        assertEquals(Color.Black, paletteFor(ThemeMode.System, systemDark = true, accent, pureBlack = true).background)
        assertEquals(charcoal, paletteFor(ThemeMode.System, systemDark = true, accent, pureBlack = false).background)
        assertFalse(paletteFor(ThemeMode.System, systemDark = false, accent, pureBlack = true).isDark)
    }

    @Test
    fun blackUsesOledOnlyWhenEnabled() {
        assertEquals(Color.Black, paletteFor(ThemeMode.Dark, systemDark = false, accent, pureBlack = true).background)
        assertEquals(charcoal, paletteFor(ThemeMode.Dark, systemDark = true, accent, pureBlack = false).background)
        assertTrue(charcoal != Color.Black)
    }

    @Test
    fun lightIgnoresOledAndTheDevice() {
        val light = lightPalette(accent)
        assertEquals(light, paletteFor(ThemeMode.Light, systemDark = true, accent, pureBlack = true))
        assertEquals(light, paletteFor(ThemeMode.Light, systemDark = false, accent, pureBlack = false))
    }

    @Test
    fun oledSurfacesAreOpaqueBlackForEveryAccentAndDarkMode() {
        for (accent in AccentColor.entries) {
            for (mode in listOf(ThemeMode.Dark, ThemeMode.System)) {
                val colors = paletteFor(mode, systemDark = true, accent, pureBlack = true)
                assertTrue(colors.isPureBlack)
                listOf(colors.background, colors.surface, colors.surfaceRaised,
                    colors.glass, colors.glassStrong, colors.overlaySurface(), colors.overlaySurface(0.96f))
                    .forEach { assertEquals(Color.Black, it) }
                assertEquals(accentFor(accent, dark = true), colors.accent)
                assertTrue(colors.textPrimary != Color.Black)
                assertTrue(colors.glassBorder.alpha > 0f)
                assertTrue(colors.divider.alpha > 0f)
            }
        }
    }

    @Test
    fun themesPreserveAccentAndOnlyEffectiveDarkModeUsesOled() {
        for (mode in ThemeMode.entries) for (systemDark in listOf(false, true)) {
            for (oled in listOf(false, true)) for (accent in AccentColor.entries) {
                val colors = paletteFor(mode, systemDark, accent, oled)
                assertEquals(mode.isDark(systemDark) && oled, colors.isPureBlack)
                assertEquals(accentFor(accent, colors.isDark), colors.accent)
                if (!colors.isPureBlack) assertTrue(colors.background != Color.Black)
            }
        }
    }
}

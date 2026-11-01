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
}

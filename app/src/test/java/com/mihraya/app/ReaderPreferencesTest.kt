package com.mihraya.app

import com.mihraya.app.data.settings.*
import com.mihraya.app.feature.quran.effectiveLineHeight
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ReaderPreferencesTest {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test fun freshInstallUsesIndoPak() {
        assertEquals(QuranScript.IndoPak, AppSettings().reader.script)
    }

    @Test fun legacyOmittedDefaultsStillMeanUthmani() {
        for (stored in listOf("{}", """{"reader":{}}""", """{"reader":{"arabicSize":34.0},"accent":"Lapis"}""")) {
            val settings = json.decodeFromString(StoredSettingsSerializer, stored)
            assertEquals(QuranScript.Uthmani, settings.reader.script)
        }
        val settings = json.decodeFromString(StoredSettingsSerializer, """{"reader":{"arabicSize":34.0},"accent":"Lapis"}""")
        assertEquals(34f, settings.reader.arabicSize)
        assertEquals(AccentColor.Lapis, settings.accent)
    }

    @Test fun explicitScriptAndReadingPreferencesSurviveUpgradeAndRoundTrip() {
        for (script in QuranScript.entries) {
            val settings = AppSettings(reader = ReaderPreferences(script = script, arabicSize = 34f,
                translations = listOf(20, 131), tajweed = true, layout = ReaderLayout.Continuous))
            val stored = json.encodeToString(StoredSettingsSerializer, settings)
            assertEquals(settings, json.decodeFromString(StoredSettingsSerializer, stored))
        }
    }

    @Test fun amiriProtectsStackedMarksWithoutChangingStoredPreference() {
        val prefs = ReaderPreferences(script = QuranScript.Uthmani, lineHeight = 1.4f)
        assertEquals(2.45f, prefs.effectiveLineHeight())
        assertEquals(1.4f, prefs.lineHeight)
        assertEquals(2.6f, prefs.copy(lineHeight = 2.6f).effectiveLineHeight())
    }

    @Test fun otherFontsAndIndoPakKeepTheirChosenLineSpacing() {
        for (font in ArabicFont.entries) {
            assertEquals(1.9f, ReaderPreferences(script = QuranScript.IndoPak, arabicFont = font).effectiveLineHeight())
            if (font != ArabicFont.AmiriQuran) {
                assertEquals(1.9f, ReaderPreferences(script = QuranScript.Uthmani, arabicFont = font).effectiveLineHeight())
            }
        }
    }
}

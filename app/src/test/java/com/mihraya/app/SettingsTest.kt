package com.mihraya.app

import com.mihraya.app.data.prayer.Salah
import com.mihraya.app.data.settings.AdhanSound
import com.mihraya.app.data.settings.AlertPreferences
import com.mihraya.app.data.settings.AppSettings
import com.mihraya.app.data.settings.DistanceUnit
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Test
    fun fajrUsesItsOwnAdhanOnlyAtFajr() {
        val alerts = AlertPreferences(adhanSound = AdhanSound.KualaLumpur, fajrSound = AdhanSound.MalmoFajr)
        assertEquals(AdhanSound.MalmoFajr, alerts.soundFor(Salah.Fajr))
        Salah.entries.filter { it.isPrayer && it != Salah.Fajr }.forEach {
            assertEquals(AdhanSound.KualaLumpur, alerts.soundFor(it))
        }
        assertEquals(AdhanSound.KualaLumpur, alerts.copy(fajrSound = null).soundFor(Salah.Fajr))
    }

    @Test
    fun fajrRecordingsNeverPlayForOtherPrayers() {
        val alerts = AlertPreferences(adhanSound = AdhanSound.FouadFajr)
        assertEquals(AdhanSound.Makkah, alerts.soundFor(Salah.Dhuhr))
        assertTrue(AdhanSound.regular.none { it.fajrOnly })
        assertTrue(AdhanSound.regular.size >= 7)
    }

    @Test
    fun milesConversion() {
        assertEquals(10_307.0, DistanceUnit.Kilometers.fromKm(10_307.0), 0.0)
        assertEquals(6_404.5, DistanceUnit.Miles.fromKm(10_307.0), 0.5)
    }

    @Test
    fun settingsFromPreviousVersionStillLoad() {
        val stored = """{"onboardingDone":true,"theme":"Dark","alerts":{"adhanSound":"Azeez","reminderMinutes":10}}"""
        val settings = json.decodeFromString(AppSettings.serializer(), stored)
        assertEquals(AdhanSound.Azeez, settings.alerts.adhanSound)
        assertEquals(10, settings.alerts.reminderMinutes)
        assertEquals("", settings.userName)
        assertEquals(DistanceUnit.Kilometers, settings.distanceUnit)
        assertTrue(settings.alerts.duaAfterAdhan.isEmpty())
    }

    @Test
    fun newPreferencesRoundTrip() {
        val settings = AppSettings(
            userName = "Jawad",
            distanceUnit = DistanceUnit.Miles,
            alerts = AlertPreferences(fajrSound = AdhanSound.MalmoFajr, duaAfterAdhan = setOf(Salah.Fajr, Salah.Maghrib)),
        )
        val decoded = json.decodeFromString(AppSettings.serializer(), json.encodeToString(AppSettings.serializer(), settings))
        assertEquals(settings, decoded)
    }
}

package com.mihraya.app.data.settings

import com.mihraya.app.data.prayer.AsrMethod
import com.mihraya.app.data.prayer.CalcMethod
import com.mihraya.app.data.prayer.HighLatitude
import com.mihraya.app.data.prayer.Salah
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.ZoneId

@Serializable
enum class ThemeMode(val label: String) {
    Dark("OLED Black"),
    Light("Light"),
    System("System"),
}

@Serializable
enum class AccentColor(val label: String) {
    Emerald("Emerald"),
    Champagne("Champagne"),
    Silver("Silver"),
    Lapis("Lapis"),
}

@Serializable
enum class AlertMode(val label: String, val description: String) {
    Adhan("Adhan", "Full call to prayer"),
    Tone("Tone", "Notification sound"),
    Silent("Silent", "Notification only"),
    Off("Off", "No alert"),
}

@Serializable
enum class AdhanSound(val label: String, val credit: String) {
    Azeez("Adhan · Aaqib Azeez", "Wikimedia Commons · CC BY-SA 4.0"),
    Mahfoudou("Adhan · Mahfoudou", "Wikimedia Commons · CC BY-SA 4.0"),
    Custom("Your own audio", "Pick any audio file on this device"),
}

@Serializable
enum class QuranScript(val label: String, val description: String) {
    Uthmani("Uthmani", "Madani mushaf script"),
    IndoPak("IndoPak", "South Asian naskh script"),
}

@Serializable
enum class ArabicFont(val label: String) {
    AmiriQuran("Amiri Quran"),
    Scheherazade("Scheherazade"),
    NotoNaskh("Noto Naskh"),
}

@Serializable
enum class ReaderLayout(val label: String) {
    VerseByVerse("Verse by verse"),
    Continuous("Continuous"),
}

@Serializable
data class SavedLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val region: String = "",
    val countryCode: String = "",
    val timeZone: String,
    val automatic: Boolean,
    val updatedAt: Long = 0L,
) {
    val zoneId: ZoneId
        get() = try {
            ZoneId.of(timeZone)
        } catch (e: DateTimeException) {
            ZoneId.systemDefault()
        }

    val displayName: String
        get() = listOf(name, region).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

@Serializable
data class PrayerPreferences(
    val method: CalcMethod = CalcMethod.MuslimWorldLeague,
    val methodSetByUser: Boolean = false,
    val asr: AsrMethod = AsrMethod.Standard,
    val asrSetByUser: Boolean = false,
    val highLatitude: HighLatitude = HighLatitude.MiddleOfNight,
    val customFajrAngle: Double = 18.0,
    val customIshaAngle: Double = 17.0,
    val customIshaMinutes: Int = 0,
    val offsets: Map<Salah, Int> = emptyMap(),
) {
    fun offset(salah: Salah): Int = offsets[salah] ?: 0
}

@Serializable
data class AlertPreferences(
    val modes: Map<Salah, AlertMode> = emptyMap(),
    val adhanSound: AdhanSound = AdhanSound.Azeez,
    val customSoundUri: String? = null,
    val customSoundName: String? = null,
    val reminderMinutes: Int = 0,
    val qiyamReminder: Boolean = false,
    val respectSilentMode: Boolean = true,
) {
    fun modeFor(salah: Salah): AlertMode =
        modes[salah] ?: if (salah == Salah.Sunrise) AlertMode.Off else AlertMode.Adhan
}

@Serializable
data class ReaderPreferences(
    val script: QuranScript = QuranScript.Uthmani,
    val arabicFont: ArabicFont = ArabicFont.AmiriQuran,
    val arabicSize: Float = 28f,
    val lineHeight: Float = 1.9f,
    val translationSize: Float = 15f,
    val translations: List<Int> = listOf(20),
    val showTranslation: Boolean = true,
    val showTransliteration: Boolean = false,
    val layout: ReaderLayout = ReaderLayout.VerseByVerse,
    val keepScreenOn: Boolean = true,
)

@Serializable
data class AppSettings(
    val onboardingDone: Boolean = false,
    val theme: ThemeMode = ThemeMode.Dark,
    val accent: AccentColor = AccentColor.Emerald,
    val use24Hour: Boolean = false,
    val haptics: Boolean = true,
    val hijriOffset: Int = 0,
    val location: SavedLocation? = null,
    val autoLocation: Boolean = true,
    val prayer: PrayerPreferences = PrayerPreferences(),
    val alerts: AlertPreferences = AlertPreferences(),
    val reader: ReaderPreferences = ReaderPreferences(),
    val reciterId: String = "alafasy",
)

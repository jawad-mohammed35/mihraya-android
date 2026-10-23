package com.mihraya.app.data.settings

import com.mihraya.app.data.prayer.AsrMethod
import com.mihraya.app.data.prayer.CalcMethod
import com.mihraya.app.data.prayer.HighLatitude
import com.mihraya.app.data.prayer.Salah
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.DateTimeException
import java.time.ZoneId

@Serializable
enum class ThemeMode(val label: String) {
    Dark("Black"),
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

/** [fajrOnly] recordings include “aṣ-ṣalātu khayrun min an-nawm” and are only offered for Fajr. */
@Serializable(with = AdhanSoundSerializer::class)
enum class AdhanSound(val label: String, val detail: String, val fajrOnly: Boolean = false) {
    Makkah("Makkah", "Masjid al-Ḥarām"),
    KualaLumpur("Kuala Lumpur", "Masjid KLCC, Malaysia"),
    Shalqar("Shalqar", "Kazakhstan"),
    Azeez("Aaqib Azeez", ""),
    MalmoFajr("Malmö", "Fajr Adhan · Sweden", fajrOnly = true),
    FouadFajr("Fouad", "Fajr Adhan", fajrOnly = true),
    Custom("Your own audio", "Pick any audio file on this device"),
    ;

    companion object {
        val regular = entries.filter { !it.fajrOnly && it != Custom }
        val fajr = entries.filter { it.fajrOnly }
    }
}

@Serializable
enum class DistanceUnit(val label: String, val symbol: String) {
    Kilometers("Kilometres", "km"),
    Miles("Miles", "mi"),
    ;

    fun fromKm(km: Double): Double = if (this == Miles) km * 0.621371 else km
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
    val adhanSound: AdhanSound = AdhanSound.Makkah,
    /** Null plays [adhanSound] at Fajr too. */
    val fajrSound: AdhanSound? = null,
    val customSoundUri: String? = null,
    val customSoundName: String? = null,
    val duaAfterAdhan: Set<Salah> = emptySet(),
    val reminderMinutes: Int = 0,
    val qiyamReminder: Boolean = false,
    val respectSilentMode: Boolean = true,
) {
    fun modeFor(salah: Salah): AlertMode =
        modes[salah] ?: if (salah == Salah.Sunrise) AlertMode.Off else AlertMode.Adhan

    fun soundFor(salah: Salah): AdhanSound {
        val fajr = fajrSound?.takeIf { salah == Salah.Fajr && it.fajrOnly }
        return fajr ?: adhanSound.takeIf { !it.fajrOnly } ?: AdhanSound.Makkah
    }
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
    val userName: String = "",
    val theme: ThemeMode = ThemeMode.Dark,
    /** True black backgrounds. Applies only while [theme] is [ThemeMode.Dark]. */
    val pureOledBlack: Boolean = false,
    val accent: AccentColor = AccentColor.Champagne,
    val use24Hour: Boolean = false,
    val distanceUnit: DistanceUnit = DistanceUnit.Kilometers,
    val haptics: Boolean = true,
    val hijriOffset: Int = 0,
    val location: SavedLocation? = null,
    val autoLocation: Boolean = true,
    val prayer: PrayerPreferences = PrayerPreferences(),
    val alerts: AlertPreferences = AlertPreferences(),
    val reader: ReaderPreferences = ReaderPreferences(),
    val reciterId: String = "alafasy",
)

/** Older installs may still name Adhan sounds that are no longer offered. Those load as Makkah. */
private object AdhanSoundSerializer : KSerializer<AdhanSound> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AdhanSound", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AdhanSound) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): AdhanSound = when (val name = decoder.decodeString()) {
        "Ahmedabad", "Javed", "Mahfoudou" -> AdhanSound.Makkah
        else -> AdhanSound.entries.find { it.name == name } ?: AdhanSound.Makkah
    }
}

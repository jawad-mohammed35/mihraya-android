package com.mihraya.app.data.prayer

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Serializable
enum class Salah(val label: String, val arabic: String) {
    Fajr("Fajr", "الفجر"),
    Sunrise("Sunrise", "الشروق"),
    Dhuhr("Dhuhr", "الظهر"),
    Asr("Asr", "العصر"),
    Maghrib("Maghrib", "المغرب"),
    Isha("Isha", "العشاء");

    val isPrayer: Boolean get() = this != Sunrise

    fun labelOn(date: LocalDate): String =
        if (this == Dhuhr && date.dayOfWeek == DayOfWeek.FRIDAY) "Jumuʿah" else label

    fun arabicOn(date: LocalDate): String =
        if (this == Dhuhr && date.dayOfWeek == DayOfWeek.FRIDAY) "الجمعة" else arabic
}

@Serializable
enum class CalcMethod(val title: String, val summary: String) {
    MuslimWorldLeague("Muslim World League", "Fajr 18° · Isha 17°"),
    NorthAmerica("ISNA · North America", "Fajr 15° · Isha 15°"),
    Egyptian("Egyptian General Authority", "Fajr 19.5° · Isha 17.5°"),
    UmmAlQura("Umm al-Qura · Makkah", "Fajr 18.5° · Isha 90 min after Maghrib"),
    Karachi("University of Islamic Sciences, Karachi", "Fajr 18° · Isha 18°"),
    Dubai("Dubai", "Fajr 18.2° · Isha 18.2°"),
    Qatar("Qatar", "Fajr 18° · Isha 90 min after Maghrib"),
    Kuwait("Kuwait", "Fajr 18° · Isha 17.5°"),
    Singapore("MUIS · Singapore, Malaysia, Indonesia", "Fajr 20° · Isha 18°"),
    MoonsightingCommittee("Moonsighting Committee", "Fajr 18° · Isha 18° · seasonal adjustment"),
    Turkey("Diyanet · Turkey", "Fajr 18° · Isha 17° · Diyanet offsets"),
    France("UOIF · France", "Fajr 12° · Isha 12°"),
    Custom("Custom", "Set your own Fajr and Isha angles"),
}

@Serializable
enum class AsrMethod(val title: String, val summary: String) {
    Standard("Standard", "Shāfiʿī, Mālikī, Ḥanbalī — shadow length 1×"),
    Hanafi("Ḥanafī", "Shadow length 2×"),
}

@Serializable
enum class HighLatitude(val title: String, val summary: String) {
    MiddleOfNight("Middle of the night", "Fajr and Isha never pass the middle of the night"),
    SeventhOfNight("One-seventh of the night", "Bounded by the first and last seventh of the night"),
    TwilightAngle("Twilight angle", "Night portion proportional to the twilight angle"),
}

data class PrayerMoment(val salah: Salah, val time: ZonedDateTime)

data class PrayerDay(
    val date: LocalDate,
    val zone: ZoneId,
    val fajr: ZonedDateTime,
    val sunrise: ZonedDateTime,
    val dhuhr: ZonedDateTime,
    val asr: ZonedDateTime,
    val maghrib: ZonedDateTime,
    val isha: ZonedDateTime,
    val duhaStart: ZonedDateTime,
    val duhaEnd: ZonedDateTime,
    val midnight: ZonedDateTime,
    val lastThird: ZonedDateTime,
) {
    operator fun get(salah: Salah): ZonedDateTime = when (salah) {
        Salah.Fajr -> fajr
        Salah.Sunrise -> sunrise
        Salah.Dhuhr -> dhuhr
        Salah.Asr -> asr
        Salah.Maghrib -> maghrib
        Salah.Isha -> isha
    }

    val moments: List<PrayerMoment> get() = Salah.entries.map { PrayerMoment(it, this[it]) }
}

fun suggestedMethod(countryCode: String?): CalcMethod = when (countryCode?.uppercase()) {
    "SA", "YE", "BH", "OM" -> CalcMethod.UmmAlQura
    "US", "CA" -> CalcMethod.NorthAmerica
    "PK", "IN", "BD", "AF", "LK", "NP" -> CalcMethod.Karachi
    "EG", "SD", "LY" -> CalcMethod.Egyptian
    "AE" -> CalcMethod.Dubai
    "QA" -> CalcMethod.Qatar
    "KW" -> CalcMethod.Kuwait
    "SG", "MY", "BN", "ID" -> CalcMethod.Singapore
    "TR" -> CalcMethod.Turkey
    "FR" -> CalcMethod.France
    "GB", "IE" -> CalcMethod.MoonsightingCommittee
    else -> CalcMethod.MuslimWorldLeague
}

fun suggestedAsr(countryCode: String?): AsrMethod = when (countryCode?.uppercase()) {
    "PK", "IN", "BD", "AF", "TR", "UZ", "TJ", "KZ", "KG", "TM", "BA", "AL", "XK", "MK" -> AsrMethod.Hanafi
    else -> AsrMethod.Standard
}

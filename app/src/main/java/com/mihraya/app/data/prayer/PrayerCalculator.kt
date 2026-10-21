package com.mihraya.app.data.prayer

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerAdjustments
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.Qibla
import com.batoulapps.adhan.SunnahTimes
import com.batoulapps.adhan.data.DateComponents
import com.mihraya.app.data.settings.PrayerPreferences
import com.mihraya.app.data.settings.SavedLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PrayerCalculator {

    fun parameters(prefs: PrayerPreferences): CalculationParameters {
        val params = when (prefs.method) {
            CalcMethod.MuslimWorldLeague -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            CalcMethod.NorthAmerica -> CalculationMethod.NORTH_AMERICA.parameters
            CalcMethod.Egyptian -> CalculationMethod.EGYPTIAN.parameters
            CalcMethod.UmmAlQura -> CalculationMethod.UMM_AL_QURA.parameters
            CalcMethod.Karachi -> CalculationMethod.KARACHI.parameters
            CalcMethod.Dubai -> CalculationMethod.DUBAI.parameters
            CalcMethod.Qatar -> CalculationMethod.QATAR.parameters
            CalcMethod.Kuwait -> CalculationMethod.KUWAIT.parameters
            CalcMethod.Singapore -> CalculationMethod.SINGAPORE.parameters
            CalcMethod.MoonsightingCommittee -> CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters
            CalcMethod.Turkey -> CalculationParameters(18.0, 17.0, CalculationMethod.OTHER)
                .withMethodAdjustments(PrayerAdjustments(0, -7, 5, 4, 7, 0))
            CalcMethod.France -> CalculationParameters(12.0, 12.0, CalculationMethod.OTHER)
            CalcMethod.Custom ->
                if (prefs.customIshaMinutes > 0) {
                    CalculationParameters(prefs.customFajrAngle, prefs.customIshaMinutes, CalculationMethod.OTHER)
                } else {
                    CalculationParameters(prefs.customFajrAngle, prefs.customIshaAngle, CalculationMethod.OTHER)
                }
        }
        params.madhab = if (prefs.asr == AsrMethod.Hanafi) Madhab.HANAFI else Madhab.SHAFI
        params.highLatitudeRule = when (prefs.highLatitude) {
            HighLatitude.MiddleOfNight -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
            HighLatitude.SeventhOfNight -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
            HighLatitude.TwilightAngle -> HighLatitudeRule.TWILIGHT_ANGLE
        }
        params.adjustments = PrayerAdjustments(
            prefs.offset(Salah.Fajr),
            prefs.offset(Salah.Sunrise),
            prefs.offset(Salah.Dhuhr),
            prefs.offset(Salah.Asr),
            prefs.offset(Salah.Maghrib),
            prefs.offset(Salah.Isha),
        )
        return params
    }

    /** Returns `null` when the sun never rises or sets far enough for the chosen parameters. */
    fun day(location: SavedLocation, prefs: PrayerPreferences, date: LocalDate): PrayerDay? {
        val zone = location.zoneId
        val times: PrayerTimes
        val sunnah: SunnahTimes
        try {
            times = PrayerTimes(
                Coordinates(location.latitude, location.longitude),
                DateComponents(date.year, date.monthValue, date.dayOfMonth),
                parameters(prefs),
            )
            sunnah = SunnahTimes(times)
        } catch (e: RuntimeException) {
            return null
        }
        val sunrise = times.sunrise?.at(zone) ?: return null
        val dhuhr = times.dhuhr?.at(zone) ?: return null
        return PrayerDay(
            date = date,
            zone = zone,
            fajr = times.fajr?.at(zone) ?: return null,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = times.asr?.at(zone) ?: return null,
            maghrib = times.maghrib?.at(zone) ?: return null,
            isha = times.isha?.at(zone) ?: return null,
            duhaStart = sunrise.plusMinutes(20),
            duhaEnd = dhuhr.minusMinutes(10),
            midnight = sunnah.middleOfTheNight?.at(zone) ?: return null,
            lastThird = sunnah.lastThirdOfTheNight?.at(zone) ?: return null,
        )
    }

    fun qiblaBearing(latitude: Double, longitude: Double): Double =
        Qibla(Coordinates(latitude, longitude)).direction

    fun distanceToKaabaKm(latitude: Double, longitude: Double): Double {
        val r = 6371.0
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LAT)
        val dLat = lat2 - lat1
        val dLng = Math.toRadians(KAABA_LNG - longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * asin(sqrt(a))
    }

    /** Adhan rounds to the minute but leaves floating-point residue in the milliseconds. */
    private fun Date.at(zone: ZoneId): ZonedDateTime =
        Instant.ofEpochMilli(Math.floorDiv(time + 30_000L, 60_000L) * 60_000L).atZone(zone)

    private companion object {
        const val KAABA_LAT = 21.4224779
        const val KAABA_LNG = 39.8251832
    }
}

/** Memoises prayer days for one location and configuration. */
class PrayerSchedule(
    private val calculator: PrayerCalculator,
    val location: SavedLocation,
    private val prefs: PrayerPreferences,
) {
    private val cache = HashMap<LocalDate, PrayerDay?>()

    val zone: ZoneId = location.zoneId

    fun day(date: LocalDate): PrayerDay? = cache.getOrPut(date) { calculator.day(location, prefs, date) }

    fun now(instant: Instant = Instant.now()): ZonedDateTime = instant.atZone(zone)

    fun next(now: ZonedDateTime, includeSunrise: Boolean = false): PrayerMoment? {
        val date = now.toLocalDate()
        for (offset in 0L..1L) {
            val day = day(date.plusDays(offset)) ?: continue
            day.moments
                .firstOrNull { (includeSunrise || it.salah.isPrayer) && it.time.isAfter(now) }
                ?.let { return it }
        }
        return null
    }

    fun previous(now: ZonedDateTime, includeSunrise: Boolean = false): PrayerMoment? {
        val date = now.toLocalDate()
        for (offset in 0L..1L) {
            val day = day(date.minusDays(offset)) ?: continue
            day.moments
                .lastOrNull { (includeSunrise || it.salah.isPrayer) && !it.time.isAfter(now) }
                ?.let { return it }
        }
        return null
    }

    /** The night currently in progress or still ahead: before today's Fajr it is the previous evening's night. */
    fun night(now: ZonedDateTime): PrayerDay? {
        val today = day(now.toLocalDate()) ?: return null
        return if (now.isBefore(today.fajr)) day(now.toLocalDate().minusDays(1)) else today
    }
}

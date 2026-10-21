package com.mihraya.app.data.prayer

import com.mihraya.app.data.settings.PrayerPreferences
import com.mihraya.app.data.settings.SavedLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PrayerCalculatorTest {

    private val calculator = PrayerCalculator()
    private val clock = DateTimeFormatter.ofPattern("HH:mm")

    private val raleigh = SavedLocation(
        latitude = 35.7750,
        longitude = -78.6336,
        name = "Raleigh",
        timeZone = "America/New_York",
        automatic = false,
    )
    private val isnaHanafi = PrayerPreferences(method = CalcMethod.NorthAmerica, asr = AsrMethod.Hanafi)
    private val date = LocalDate.of(2015, 7, 12)

    /** Reference values published with the Adhan library's own test suite. */
    @Test
    fun matchesAdhanReferenceTimes() {
        val day = checkNotNull(calculator.day(raleigh, isnaHanafi, date))
        assertEquals("04:42", day.fajr.format(clock))
        assertEquals("06:08", day.sunrise.format(clock))
        assertEquals("13:21", day.dhuhr.format(clock))
        assertEquals("18:22", day.asr.format(clock))
        assertEquals("20:32", day.maghrib.format(clock))
        assertEquals("21:57", day.isha.format(clock))
        assertEquals(ZoneId.of("America/New_York"), day.fajr.zone)
        day.moments.forEach { assertEquals(0, it.time.second + it.time.nano) }
    }

    @Test
    fun manualOffsetsShiftOnlyTheirPrayer() {
        val prefs = isnaHanafi.copy(offsets = mapOf(Salah.Fajr to 3, Salah.Isha to -5))
        val day = checkNotNull(calculator.day(raleigh, prefs, date))
        assertEquals("04:45", day.fajr.format(clock))
        assertEquals("13:21", day.dhuhr.format(clock))
        assertEquals("21:52", day.isha.format(clock))
    }

    @Test
    fun sunnahTimesFallInOrder() {
        val day = checkNotNull(calculator.day(raleigh, isnaHanafi, date))
        val nextFajr = checkNotNull(calculator.day(raleigh, isnaHanafi, date.plusDays(1))).fajr
        assertTrue(day.sunrise < day.duhaStart && day.duhaStart < day.duhaEnd && day.duhaEnd < day.dhuhr)
        assertTrue(day.maghrib < day.midnight)
        assertTrue(day.midnight < day.lastThird && day.lastThird < nextFajr)
    }

    @Test
    fun scheduleFindsNextAndPreviousAcrossMidnight() {
        val schedule = PrayerSchedule(calculator, raleigh, isnaHanafi)
        val zone = raleigh.zoneId
        val afternoon = LocalDateTime.of(2015, 7, 12, 15, 0).atZone(zone)
        assertEquals(Salah.Asr, schedule.next(afternoon)?.salah)
        assertEquals(Salah.Dhuhr, schedule.previous(afternoon)?.salah)

        val lateNight = LocalDateTime.of(2015, 7, 12, 23, 30).atZone(zone)
        val next = checkNotNull(schedule.next(lateNight))
        assertEquals(Salah.Fajr, next.salah)
        assertEquals(LocalDate.of(2015, 7, 13), next.time.toLocalDate())

        val beforeFajr = LocalDateTime.of(2015, 7, 13, 2, 0).atZone(zone)
        assertEquals(Salah.Isha, schedule.previous(beforeFajr)?.salah)
        assertEquals(date, schedule.night(beforeFajr)?.date)
    }

    @Test
    fun customMethodUsesChosenAngles() {
        val strict = PrayerPreferences(method = CalcMethod.Custom, customFajrAngle = 18.0, customIshaAngle = 17.0)
        val mwl = PrayerPreferences(method = CalcMethod.MuslimWorldLeague)
        val custom = checkNotNull(calculator.day(raleigh, strict, date))
        val reference = checkNotNull(calculator.day(raleigh, mwl, date))
        assertEquals(reference.fajr, custom.fajr)
        assertEquals(reference.isha, custom.isha)
    }

    @Test
    fun qiblaBearingAndDistance() {
        assertEquals(58.48, calculator.qiblaBearing(40.7128, -74.0059), 0.05)
        assertEquals(0.0, calculator.distanceToKaabaKm(21.4224779, 39.8251832), 0.001)
        assertEquals(10_300.0, calculator.distanceToKaabaKm(40.7128, -74.0059), 150.0)
    }

    @Test
    fun regionalDefaults() {
        assertEquals(CalcMethod.Karachi, suggestedMethod("pk"))
        assertEquals(AsrMethod.Hanafi, suggestedAsr("PK"))
        assertEquals(CalcMethod.UmmAlQura, suggestedMethod("SA"))
        assertEquals(CalcMethod.MuslimWorldLeague, suggestedMethod(null))
    }
}

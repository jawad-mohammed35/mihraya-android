package com.mihraya.app.widget

import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerRowFittingTest {

    private fun row(name: String, hour: Int, upcoming: Boolean = false, extra: Boolean = false) =
        PrayerRow(name, ZonedDateTime.of(2026, 11, 2, hour, 0, 0, 0, ZoneOffset.UTC), upcoming, extra)

    private fun day(upcoming: String) = listOf(
        row("Fajr", 5, upcoming == "Fajr"),
        row("Sunrise", 6, upcoming == "Sunrise", extra = true),
        row("Dhuhr", 12, upcoming == "Dhuhr"),
        row("Asr", 15, upcoming == "Asr"),
        row("Maghrib", 17, upcoming == "Maghrib"),
        row("Isha", 19, upcoming == "Isha"),
        row("Qiyam", 23, upcoming == "Qiyam", extra = true),
    )

    @Test
    fun keepsEveryRowWhenTheyFit() {
        assertEquals(day("Asr"), day("Asr").fitting(7))
    }

    @Test
    fun dropsOptionalTimesBeforeDailyPrayers() {
        assertEquals(listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"), day("Asr").fitting(6).map { it.name })
        assertEquals(listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"), day("Asr").fitting(5).map { it.name })
    }

    @Test
    fun keepsAnUpcomingOptionalTime() {
        assertEquals(listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha", "Qiyam"), day("Qiyam").fitting(6).map { it.name })
    }

    @Test
    fun showsTheUpcomingWindowWhenDailyPrayersDoNotFit() {
        assertEquals(listOf("Asr", "Maghrib", "Isha"), day("Asr").fitting(3).map { it.name })
        assertEquals(listOf("Fajr", "Dhuhr"), day("Fajr").fitting(2).map { it.name })
        assertEquals(listOf("Isha", "Qiyam"), day("Qiyam").fitting(2).map { it.name })
        assertEquals(listOf("Maghrib", "Isha"), day("none").fitting(2).map { it.name })
    }
}

package com.mihraya.app.data.prayer

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class HijriDate(val day: Int, val month: Int, val year: Int) {
    val monthName: String get() = MONTHS[month - 1]
    val monthArabic: String get() = MONTHS_AR[month - 1]

    fun format(): String = "$day $monthName $year AH"

    companion object {
        val MONTHS = listOf(
            "Muḥarram", "Ṣafar", "Rabīʿ al-Awwal", "Rabīʿ al-Thānī", "Jumādā al-Ūlā", "Jumādā al-Ākhirah",
            "Rajab", "Shaʿbān", "Ramaḍān", "Shawwāl", "Dhū al-Qaʿdah", "Dhū al-Ḥijjah",
        )
        val MONTHS_AR = listOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
        )

        fun of(date: LocalDate, offsetDays: Int = 0): HijriDate? = try {
            val hijrah = HijrahDate.from(date.plusDays(offsetDays.toLong()))
            HijriDate(
                day = hijrah.get(ChronoField.DAY_OF_MONTH),
                month = hijrah.get(ChronoField.MONTH_OF_YEAR),
                year = hijrah.get(ChronoField.YEAR),
            )
        } catch (e: DateTimeException) {
            null
        }
    }
}

enum class DayAction { ReadKahf, OpenTasbih }

data class DayNote(val title: String, val detail: String, val action: DayAction? = null)

object IslamicDays {

    fun noteFor(date: LocalDate, hijri: HijriDate?): DayNote? {
        if (hijri != null) {
            special(hijri)?.let { return it }
        }
        if (date.dayOfWeek == DayOfWeek.FRIDAY) {
            return DayNote(
                "Jumuʿah Mubārak",
                "A blessed day to read Sūrat al-Kahf and send ṣalawāt upon the Prophet ﷺ.",
                DayAction.ReadKahf,
            )
        }
        if (hijri != null && hijri.day in 13..15) {
            return DayNote(
                "Ayyām al-Bīḍ",
                "The white days — the 13th, 14th and 15th — are recommended days of fasting.",
            )
        }
        return null
    }

    private fun special(h: HijriDate): DayNote? = when {
        h.month == 1 && h.day == 1 -> DayNote("Islamic New Year", "The first of Muḥarram, ${h.year} AH.")
        h.month == 1 && h.day == 9 -> DayNote("Tāsūʿāʾ", "Fasting the 9th alongside ʿĀshūrāʾ is recommended.")
        h.month == 1 && h.day == 10 -> DayNote("Day of ʿĀshūrāʾ", "Fasting this day expiates the sins of the previous year.")
        h.month == 9 && h.day >= 21 -> DayNote("The last ten nights", "Seek Laylat al-Qadr in the odd nights of Ramaḍān.", DayAction.OpenTasbih)
        h.month == 9 -> DayNote("Ramaḍān · Day ${h.day}", "May Allah accept your fasting and prayers.")
        h.month == 10 && h.day == 1 -> DayNote("ʿĪd al-Fiṭr", "ʿĪd Mubārak — taqabbal Allāhu minnā wa minkum.")
        h.month == 12 && h.day == 9 -> DayNote("Day of ʿArafah", "Fasting today expiates the sins of two years.")
        h.month == 12 && h.day == 10 -> DayNote("ʿĪd al-Aḍḥā", "ʿĪd Mubārak — taqabbal Allāhu minnā wa minkum.")
        h.month == 12 && h.day in 11..13 -> DayNote("Days of Tashrīq", "Days of eating, drinking and remembrance of Allah.", DayAction.OpenTasbih)
        h.month == 12 && h.day <= 8 -> DayNote("First ten days of Dhū al-Ḥijjah", "The best days of the year for good deeds and dhikr.", DayAction.OpenTasbih)
        else -> null
    }
}

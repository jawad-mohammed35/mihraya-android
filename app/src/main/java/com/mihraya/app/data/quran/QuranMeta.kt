package com.mihraya.app.data.quran

import java.time.LocalDate

enum class Revelation(val label: String) { Meccan("Meccan"), Medinan("Medinan") }

data class Surah(
    val number: Int,
    val name: String,
    val arabicName: String,
    val meaning: String,
    val verseCount: Int,
    val revelation: Revelation,
    val revelationOrder: Int,
    val startPage: Int,
)

data class VerseRef(val surah: Int, val ayah: Int) {
    val key: String get() = "$surah:$ayah"
}

object QuranMeta {
    const val VERSE_COUNT = 6236
    const val JUZ_COUNT = 30

    val surahs: List<Surah> = SURAH_TABLE

    private val firstVerse = IntArray(116).also { table ->
        var next = 1
        for (s in SURAH_TABLE) {
            table[s.number] = next
            next += s.verseCount
        }
        table[115] = next
    }

    fun surah(number: Int): Surah = surahs[number.coerceIn(1, 114) - 1]

    fun firstVerseId(surah: Int): Int = firstVerse[surah]

    fun verseId(surah: Int, ayah: Int): Int = firstVerse[surah] + ayah - 1

    fun refOf(verseId: Int): VerseRef {
        val id = verseId.coerceIn(1, VERSE_COUNT)
        var lo = 1
        var hi = 114
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (firstVerse[mid] <= id) lo = mid else hi = mid - 1
        }
        return VerseRef(lo, id - firstVerse[lo] + 1)
    }

    fun juzOf(verseId: Int): Int = JUZ_FIRST_VERSE_IDS.indexOfLast { it <= verseId } + 1

    fun juzOf(surah: Int, ayah: Int): Int = juzOf(verseId(surah, ayah))

    fun juzStart(juz: Int): VerseRef = refOf(JUZ_FIRST_VERSE_IDS[juz.coerceIn(1, JUZ_COUNT) - 1])

    fun juzVerseCount(juz: Int): Int {
        val start = JUZ_FIRST_VERSE_IDS[juz - 1]
        val end = if (juz == JUZ_COUNT) VERSE_COUNT + 1 else JUZ_FIRST_VERSE_IDS[juz]
        return end - start
    }

    fun hasBismillahHeader(surah: Int): Boolean = surah != 1 && surah != 9

    /** Parses references such as "2:255", "2 255" or "18". */
    fun parseRef(text: String): VerseRef? {
        val parts = text.trim().split(':', ' ', '.', '/').filter { it.isNotBlank() }
        val surah = parts.getOrNull(0)?.toIntOrNull() ?: return null
        if (surah !in 1..114) return null
        val ayah = parts.getOrNull(1)?.toIntOrNull() ?: 1
        if (ayah !in 1..surah(surah).verseCount) return null
        return VerseRef(surah, ayah)
    }

    private val dailyVerses = listOf(
        "2:152", "2:153", "2:186", "2:201", "2:255", "2:286", "3:8", "3:139", "3:159", "3:173",
        "3:190", "6:162", "7:23", "7:56", "9:51", "11:88", "13:11", "13:28", "14:7", "16:97",
        "16:128", "18:10", "20:25", "20:114", "21:87", "25:74", "29:69", "33:41", "39:53", "40:60",
        "42:19", "47:7", "49:13", "50:16", "55:13", "57:4", "64:11", "65:3", "93:5", "94:5",
        "94:6", "112:1",
    ).map { requireNotNull(parseRef(it)) }

    fun verseOfTheDay(date: LocalDate): VerseRef =
        dailyVerses[Math.floorMod(date.toEpochDay() * 7L + 3L, dailyVerses.size.toLong()).toInt()]
}

fun Int.arabicDigits(extended: Boolean = false): String {
    val zero = if (extended) 0x06F0 else 0x0660
    return toString().map { c -> if (c in '0'..'9') (zero + (c - '0')).toChar() else c }.joinToString("")
}

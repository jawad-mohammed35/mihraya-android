package com.mihraya.app.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class QuranMetaTest {

    @Test
    fun tablesCoverTheWholeMushaf() {
        assertEquals(114, QuranMeta.surahs.size)
        assertEquals(QuranMeta.VERSE_COUNT, QuranMeta.surahs.sumOf { it.verseCount })
        assertEquals((1..114).toList(), QuranMeta.surahs.map { it.number })
        assertEquals(QuranMeta.VERSE_COUNT, (1..QuranMeta.JUZ_COUNT).sumOf { QuranMeta.juzVerseCount(it) })
    }

    @Test
    fun verseIdsRoundTrip() {
        for (surah in QuranMeta.surahs) {
            for (ayah in 1..surah.verseCount) {
                assertEquals(VerseRef(surah.number, ayah), QuranMeta.refOf(QuranMeta.verseId(surah.number, ayah)))
            }
        }
    }

    @Test
    fun juzBoundariesMatchTheMushaf() {
        assertEquals(VerseRef(1, 1), QuranMeta.juzStart(1))
        assertEquals(VerseRef(2, 142), QuranMeta.juzStart(2))
        assertEquals(VerseRef(18, 75), QuranMeta.juzStart(16))
        assertEquals(VerseRef(78, 1), QuranMeta.juzStart(30))
        assertEquals(1, QuranMeta.juzOf(2, 141))
        assertEquals(2, QuranMeta.juzOf(2, 142))
        assertEquals(30, QuranMeta.juzOf(114, 6))
    }

    @Test
    fun parsesReferences() {
        assertEquals(VerseRef(2, 255), QuranMeta.parseRef("2:255"))
        assertEquals(VerseRef(18, 10), QuranMeta.parseRef(" 18 10 "))
        assertEquals(VerseRef(36, 1), QuranMeta.parseRef("36"))
        assertNull(QuranMeta.parseRef("2:287"))
        assertNull(QuranMeta.parseRef("115"))
        assertNull(QuranMeta.parseRef("al-baqarah"))
    }

    @Test
    fun verseOfTheDayRotatesThroughEveryCuratedVerse() {
        val start = LocalDate.of(2026, 1, 1)
        val seen = (0L until 42L).map { QuranMeta.verseOfTheDay(start.plusDays(it)) }.toSet()
        assertEquals(42, seen.size)
    }

    @Test
    fun arabicDigits() {
        assertEquals("٢٥٥", 255.arabicDigits())
        assertEquals("۲۵۵", 255.arabicDigits(extended = true))
    }
}

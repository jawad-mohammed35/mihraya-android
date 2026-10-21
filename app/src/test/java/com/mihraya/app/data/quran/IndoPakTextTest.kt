package com.mihraya.app.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IndoPakTextTest {

    private val lines = File("src/main/assets/quran/indopak.txt").readLines(Charsets.UTF_8).map(IndoPakText::normalize)

    @Test
    fun noPrivateUseCharactersRemain() {
        assertEquals(QuranMeta.VERSE_COUNT, lines.size)
        lines.forEachIndexed { index, line ->
            assertFalse("Verse id ${index + 1} still has private-use characters", line.any { it in '\uE000'..'\uF8FF' })
            assertFalse("Verse id ${index + 1} has doubled spaces", line.contains("  "))
            assertTrue("Verse id ${index + 1} is empty", line.isNotBlank())
        }
    }

    @Test
    fun mapsKnownPauseMarks() {
        val fatihah7 = lines[QuranMeta.verseId(1, 7) - 1]
        assertTrue(fatihah7.contains('\u06EC'))
        assertTrue(fatihah7.contains('\u06E0'))
    }

    @Test
    fun leavesOrdinaryTextUntouched() {
        assertEquals("بِسۡمِ اللّٰهِ", IndoPakText.normalize("بِسۡمِ اللّٰهِ"))
        assertEquals("a b", IndoPakText.normalize("a \uE01B b"))
    }
}

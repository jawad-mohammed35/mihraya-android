package com.mihraya.app.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class IndoPakTextTest {
    @Test fun preservesPublishedTextAndEveryVerseKey() {
        val file = File("src/main/assets/quran/indopak.txt")
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
        assertEquals("2ba997169b03c587bd3a8f55cfadc2bb7e0d075f1ffd1f43e7ad51f8b036acad", digest)
        val rows = file.readLines()
        assertEquals(QuranMeta.VERSE_COUNT, rows.size)
        rows.forEachIndexed { index, row ->
            val text = IndoPakText.verse(index, row)
            assertEquals(row.substringAfter('\t'), text)
            assertTrue(text.isNotBlank())
        }
        assertTrue(IndoPakText.verse(0, rows.first()).endsWith('\uF500'))
    }
}

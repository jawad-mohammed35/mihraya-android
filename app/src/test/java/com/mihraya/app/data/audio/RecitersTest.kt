package com.mihraya.app.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecitersTest {

    @Test
    fun existingRecitersKeepTheirIdsAndOrder() {
        val earlier = listOf(
            "alafasy", "abdulbasit", "maher", "sudais", "husary", "ghamdi",
            "dosari", "minshawi", "ayyub", "shatri", "qatami", "abkar",
        )
        assertEquals(earlier, Reciters.all.take(earlier.size).map { it.id })
        assertEquals("alafasy", Reciters.Default.id)
    }

    @Test
    fun addsFiveDistinctReciters() {
        val added = Reciters.all.drop(12)
        assertEquals(listOf("shuraim", "ajmi", "hudhaifi", "juhany", "rifai"), added.map { it.id })
        assertEquals(Reciters.all.size, Reciters.all.map { it.id }.toSet().size)
        assertEquals(Reciters.all.size, Reciters.all.map { it.baseUrl }.toSet().size)
        assertEquals(Reciters.all.size, Reciters.all.map { it.name }.toSet().size)
    }

    @Test
    fun everySurahHasANumberedMp3() {
        Reciters.all.forEach { reciter ->
            assertTrue(reciter.baseUrl, reciter.baseUrl.startsWith("https://cdn.mp3quran.net/audio/") && reciter.baseUrl.endsWith("/"))
            assertEquals(reciter.baseUrl + "001.mp3", reciter.urlFor(1))
            assertEquals(reciter.baseUrl + "114.mp3", reciter.urlFor(114))
        }
    }
}

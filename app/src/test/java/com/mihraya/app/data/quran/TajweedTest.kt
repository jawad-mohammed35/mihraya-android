package com.mihraya.app.data.quran

import org.junit.Assert.assertEquals
import org.junit.Test

class TajweedTest {

    @Test
    fun parsesQuranComMarkupAndDropsEndMarkers() {
        val markup = "بِسْمِ <tajweed class=ham_wasl>ٱ</tajweed>للَّهِ <span class=end>١</span>"
        val runs = parseTajweed(markup)
        assertEquals(listOf("بِسْمِ ", "ٱ", "للَّهِ"), runs.map { it.text })
        assertEquals(listOf(null, "ham_wasl", null), runs.map { it.rule })
    }
}

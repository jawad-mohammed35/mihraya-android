package com.mihraya.app.data.library

import org.junit.Assert.assertEquals
import org.junit.Test

class FreshTasbihTest {

    private val history = listOf(
        TasbihSession("Alḥamdulillāh", 33, 33, startedAt = 5_000, endedAt = 6_000),
        TasbihSession("SubḥānAllāh", 12, 33, startedAt = 1_000, endedAt = 2_000),
    )

    @Test
    fun startsAtZeroOnTheFirstPresetAndKeepsHistory() {
        val library = UserLibrary(
            tasbih = TasbihState(presetId = "istighfar", count = 100, target = 100, rounds = 3, startedAt = 7_000),
            sessions = history,
        )
        val fresh = library.withFreshTasbih(now = 9_000)
        assertEquals(TasbihState(), fresh.tasbih)
        assertEquals("subhanallah", fresh.tasbih.presetId)
        assertEquals(0, fresh.tasbih.count)
        assertEquals(33, fresh.tasbih.target)
        assertEquals(history, fresh.sessions)
    }

    @Test
    fun anUnfinishedCountIsKeptInHistoryWhenItBegan() {
        val library = UserLibrary(
            tasbih = TasbihState(presetId = "allahuakbar", count = 20, target = 34, startedAt = 3_000),
            sessions = history,
        )
        val fresh = library.withFreshTasbih(now = 9_000)
        assertEquals(TasbihState(), fresh.tasbih)
        assertEquals(
            listOf(history[0], TasbihSession("Allāhu Akbar", 20, 34, 3_000, 3_000), history[1]),
            fresh.sessions,
        )
    }

    @Test
    fun anUntouchedCounterChangesNothingElse() {
        val library = UserLibrary(bookmarks = listOf(Bookmark(2, 255, 1L)), sessions = history)
        assertEquals(library, library.withFreshTasbih(now = 9_000))
    }
}

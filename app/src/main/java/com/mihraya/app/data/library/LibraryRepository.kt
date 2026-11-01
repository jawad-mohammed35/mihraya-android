package com.mihraya.app.data.library

import android.content.Context
import com.mihraya.app.data.settings.jsonDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

class LibraryRepository(
    context: Context,
    json: Json,
    private val scope: CoroutineScope,
) {
    private val store = jsonDataStore(
        context = context,
        fileName = "library.json",
        json = json,
        serializer = UserLibrary.serializer(),
        default = UserLibrary(),
        scope = scope,
    )

    val flow: Flow<UserLibrary> = store.data
    val state: StateFlow<UserLibrary> = store.data.stateIn(scope, SharingStarted.Eagerly, UserLibrary())

    private var freshTasbih: Job? = null

    val freshTasbihPending: Boolean
        get() = freshTasbih?.isActive == true

    suspend fun current(): UserLibrary = store.data.first()

    fun edit(transform: (UserLibrary) -> UserLibrary): Job = scope.launch { store.updateData(transform) }

    suspend fun editNow(transform: (UserLibrary) -> UserLibrary): UserLibrary = store.updateData(transform)

    fun toggleBookmark(surah: Int, ayah: Int) = edit { lib ->
        val exists = lib.bookmarks.any { it.surah == surah && it.ayah == ayah }
        lib.copy(
            bookmarks = if (exists) {
                lib.bookmarks.filterNot { it.surah == surah && it.ayah == ayah }
            } else {
                listOf(Bookmark(surah, ayah, System.currentTimeMillis())) + lib.bookmarks
            },
        )
    }

    fun removeBookmark(bookmark: Bookmark) = edit { lib ->
        lib.copy(bookmarks = lib.bookmarks.filterNot { it.surah == bookmark.surah && it.ayah == bookmark.ayah })
    }

    fun setLastRead(surah: Int, ayah: Int) = edit { lib ->
        if (lib.lastRead?.surah == surah && lib.lastRead.ayah == ayah) lib
        else lib.copy(lastRead = ReadingPosition(surah, ayah, System.currentTimeMillis()))
    }

    fun setLastPlayed(reciterId: String, surah: Int, positionMs: Long) = edit { lib ->
        lib.copy(lastPlayed = LastPlayed(reciterId, surah, positionMs, System.currentTimeMillis()))
    }

    fun saveTasbih(state: TasbihState) = edit { it.copy(tasbih = state) }

    /** Called once per app launch. See [withFreshTasbih]. */
    fun startFreshTasbih() {
        freshTasbih = edit { it.withFreshTasbih() }
    }

    suspend fun awaitFreshTasbih() {
        freshTasbih?.join()
    }

    fun addSession(session: TasbihSession) = edit { lib ->
        lib.copy(sessions = (listOf(session) + lib.sessions).take(MAX_SESSIONS))
    }

    fun clearSessions() = edit { it.copy(sessions = emptyList()) }

    fun addDhikr(arabic: String, transliteration: String, meaning: String, target: Int): String {
        val id = "custom-" + UUID.randomUUID().toString().take(8)
        edit { lib ->
            lib.copy(customDhikr = lib.customDhikr + DhikrPreset(id, arabic, transliteration, meaning, target))
        }
        return id
    }

    fun removeDhikr(id: String) = edit { lib ->
        val tasbih = if (lib.tasbih.presetId == id) TasbihState() else lib.tasbih
        lib.copy(customDhikr = lib.customDhikr.filterNot { it.id == id }, tasbih = tasbih)
    }
}

private const val MAX_SESSIONS = 400

/**
 * The counter a new app session starts with: the first dhikr at 33, counted from zero. Saved sessions are kept.
 * An unfinished count goes into history, as Reset does, dated when that round began.
 */
fun UserLibrary.withFreshTasbih(now: Long = System.currentTimeMillis()): UserLibrary {
    val state = tasbih
    if (state.count !in 1 until state.target) return copy(tasbih = TasbihState())
    val began = state.startedAt.takeIf { it > 0L } ?: now
    val unfinished = TasbihSession(dhikr(state.presetId).transliteration, state.count, state.target, began, began)
    return copy(
        tasbih = TasbihState(),
        sessions = (sessions + unfinished).sortedByDescending { it.endedAt }.take(MAX_SESSIONS),
    )
}

fun UserLibrary.allDhikr(): List<DhikrPreset> = DhikrLibrary.builtIn + customDhikr

fun UserLibrary.dhikr(id: String): DhikrPreset = allDhikr().firstOrNull { it.id == id } ?: DhikrLibrary.builtIn.first()

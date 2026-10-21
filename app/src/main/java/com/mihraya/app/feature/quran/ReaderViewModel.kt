package com.mihraya.app.feature.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mihraya.app.AppContainer
import com.mihraya.app.data.audio.Reciters
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.data.quran.Surah
import com.mihraya.app.data.quran.Verse
import com.mihraya.app.data.quran.VerseRef
import com.mihraya.app.data.settings.ReaderPreferences
import com.mihraya.app.playback.PlaybackUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

data class ReaderContent(
    val surah: Surah,
    val verses: List<Verse>,
    val prefs: ReaderPreferences,
    val bismillah: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModel(
    private val container: AppContainer,
    initialSurah: Int,
    initialAyah: Int,
) : ViewModel() {

    private val surahNumber = MutableStateFlow(initialSurah.coerceIn(1, 114))
    private var pendingAyah: Int? = initialAyah.takeIf { it > 1 }

    val prefs: StateFlow<ReaderPreferences> = container.settings.flow
        .map { it.reader }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, container.settings.state.value?.reader ?: ReaderPreferences())

    val content: StateFlow<ReaderContent?> = combine(surahNumber, prefs, container.quran.installedTranslations) { surah, prefs, _ ->
        surah to prefs
    }.mapLatest { (surah, prefs) ->
        ReaderContent(
            surah = QuranMeta.surah(surah),
            verses = container.quran.surahVerses(surah, prefs),
            prefs = prefs,
            bismillah = container.quran.bismillah(prefs.script),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val bookmarks: StateFlow<Set<VerseRef>> = container.library.flow
        .map { lib -> lib.bookmarks.map { VerseRef(it.surah, it.ayah) }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playback: StateFlow<PlaybackUiState> = container.player.state

    val preferredReciterName: String
        get() = Reciters.byId(container.settings.state.value?.reciterId).shortName

    fun consumePendingAyah(): Int? = pendingAyah.also { pendingAyah = null }

    fun openSurah(number: Int) {
        if (number !in 1..114) return
        pendingAyah = null
        surahNumber.value = number
    }

    fun toggleBookmark(ref: VerseRef) = container.library.toggleBookmark(ref.surah, ref.ayah)

    fun saveProgress(surah: Int, ayah: Int) = container.library.setLastRead(surah, ayah)

    fun updatePrefs(transform: (ReaderPreferences) -> ReaderPreferences) =
        container.settings.edit { it.copy(reader = transform(it.reader)) }

    fun playSurah(surah: Int) {
        val reciter = Reciters.byId(container.settings.state.value?.reciterId)
        val player = container.player
        val state = player.state.value
        if (state.surah == surah && state.hasMedia) player.togglePlayPause() else player.play(reciter, surah)
    }
}

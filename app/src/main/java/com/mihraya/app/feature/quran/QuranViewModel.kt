package com.mihraya.app.feature.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mihraya.app.AppContainer
import com.mihraya.app.data.library.UserLibrary
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.data.quran.SearchHit
import com.mihraya.app.data.quran.Surah
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.Normalizer

sealed interface VerseSearch {
    data object Idle : VerseSearch
    data object Searching : VerseSearch
    data class Results(val hits: List<SearchHit>) : VerseSearch
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class QuranViewModel(private val container: AppContainer) : ViewModel() {

    val query = MutableStateFlow("")
    val tab = MutableStateFlow(0)
    val library: StateFlow<UserLibrary> = container.library.state

    val verseSearch: StateFlow<VerseSearch> = query
        .debounce(320)
        .flatMapLatest { q ->
            flow {
                if (q.trim().length < 3 || QuranMeta.parseRef(q) != null) {
                    emit(VerseSearch.Idle)
                } else {
                    emit(VerseSearch.Searching)
                    val translation = container.settings.current().reader.translations.firstOrNull() ?: 20
                    emit(VerseSearch.Results(runCatching { container.quran.search(q, translation) }.getOrDefault(emptyList())))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VerseSearch.Idle)

    fun surahMatches(q: String): List<Surah> {
        val needle = fold(q)
        if (needle.isBlank()) return emptyList()
        needle.toIntOrNull()?.let { n -> return QuranMeta.surahs.filter { it.number == n } }
        return QuranMeta.surahs.filter { s ->
            fold(s.name).contains(needle) || fold(s.meaning).contains(needle) || s.arabicName.contains(q.trim())
        }
    }

    fun removeBookmark(surah: Int, ayah: Int) = container.library.toggleBookmark(surah, ayah)

    private fun fold(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9\\u0600-\\u06ff]"), "")
}

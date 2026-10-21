package com.mihraya.app.feature.listen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mihraya.app.AppContainer
import com.mihraya.app.data.audio.DownloadStatus
import com.mihraya.app.data.audio.Reciter
import com.mihraya.app.data.audio.Reciters
import com.mihraya.app.data.library.LastPlayed
import com.mihraya.app.playback.PlaybackUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ListenViewModel(private val container: AppContainer) : ViewModel() {

    val reciter: StateFlow<Reciter> = container.settings.flow
        .map { Reciters.byId(it.reciterId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Reciters.byId(container.settings.state.value?.reciterId))

    val filter = MutableStateFlow(0)

    val downloaded: StateFlow<Map<String, Set<Int>>> = container.audioDownloads.downloaded

    val active: StateFlow<Map<String, DownloadStatus>> = container.audioDownloads.active
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val playback: StateFlow<PlaybackUiState> = container.player.state

    val lastPlayed: StateFlow<LastPlayed?> = container.library.flow
        .map { it.lastPlayed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), container.library.state.value.lastPlayed)

    fun selectReciter(reciter: Reciter) = container.settings.edit { it.copy(reciterId = reciter.id) }

    fun play(reciter: Reciter, surah: Int) {
        val player = container.player
        if (player.isCurrent(reciter.id, surah)) player.togglePlayPause() else player.play(reciter, surah)
    }

    fun resume(last: LastPlayed) = container.player.play(Reciters.byId(last.reciterId), last.surah, last.positionMs)

    fun download(reciter: Reciter, surah: Int) = container.audioDownloads.enqueue(reciter, surah)

    fun cancel(reciter: Reciter, surah: Int) = container.audioDownloads.cancel(reciter.id, surah)

    fun delete(reciter: Reciter, surah: Int) = container.audioDownloads.delete(reciter.id, surah)

    fun downloadAll(reciter: Reciter) = container.audioDownloads.enqueueAll(reciter)
}

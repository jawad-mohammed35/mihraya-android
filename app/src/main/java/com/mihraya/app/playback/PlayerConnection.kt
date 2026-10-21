package com.mihraya.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.mihraya.app.data.audio.AudioDownloads
import com.mihraya.app.data.audio.Reciter
import com.mihraya.app.data.audio.Reciters
import com.mihraya.app.data.library.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SleepTimer(val label: String, val minutes: Int) {
    Off("Off", 0),
    EndOfSurah("End of sūrah", 0),
    Fifteen("15 minutes", 15),
    Thirty("30 minutes", 30),
    FortyFive("45 minutes", 45),
    Sixty("1 hour", 60),
}

data class PlaybackUiState(
    val connected: Boolean = false,
    val reciterId: String? = null,
    val surah: Int? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playWhenReady: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val error: String? = null,
    val sleepTimer: SleepTimer = SleepTimer.Off,
    val sleepEndsAt: Long? = null,
) {
    val hasMedia: Boolean get() = connected && surah != null
    val reciter: Reciter? get() = Reciters.find(reciterId)
}

data class PlaybackProgress(val positionMs: Long, val durationMs: Long, val bufferedMs: Long) {
    val fraction: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

class PlayerConnection(
    private val context: Context,
    private val scope: CoroutineScope,
    private val library: LibraryRepository,
    private val downloads: AudioDownloads,
) {
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var sleepJob: Job? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && _state.value.sleepTimer == SleepTimer.EndOfSurah) {
                controller?.pause()
                clearSleepTimer()
            }
            rememberPosition()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) rememberPosition()
        }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val pending = MediaController.Builder(context, token).buildAsync()
        future = pending
        pending.addListener(
            {
                val connected = runCatching { pending.get() }.getOrNull() ?: return@addListener
                if (future !== pending) {
                    connected.release()
                    return@addListener
                }
                controller = connected
                connected.addListener(listener)
                publish()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun disconnect() {
        rememberPosition()
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        future = null
        controller = null
        _state.value = _state.value.copy(connected = false)
    }

    private suspend fun awaitController(): MediaController? {
        controller?.let { return it }
        connect()
        return runCatching { future?.await() }.getOrNull()
    }

    val progress: Flow<PlaybackProgress> = flow {
        while (true) {
            val c = controller
            emit(
                PlaybackProgress(
                    positionMs = c?.currentPosition ?: 0L,
                    durationMs = c?.duration?.takeIf { it > 0 } ?: 0L,
                    bufferedMs = c?.bufferedPosition ?: 0L,
                ),
            )
            delay(if (c?.isPlaying == true) 250L else 750L)
        }
    }.flowOn(Dispatchers.Main).distinctUntilChanged()

    fun play(reciter: Reciter, surah: Int, startPositionMs: Long = 0L) {
        scope.launch(Dispatchers.Main) {
            val c = awaitController() ?: return@launch
            val current = MediaIds.parse(c.currentMediaItem?.mediaId)
            if (current?.reciterId == reciter.id && c.mediaItemCount == 114) {
                if (current.surah != surah || startPositionMs > 0) c.seekTo(surah - 1, startPositionMs)
            } else {
                val items = withContext(Dispatchers.IO) { MediaItems.playlist(context, reciter, downloads) }
                c.setMediaItems(items, surah - 1, startPositionMs)
            }
            c.prepare()
            c.play()
        }
    }

    fun isCurrent(reciterId: String, surah: Int): Boolean =
        _state.value.reciterId == reciterId && _state.value.surah == surah

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        if (c.playbackState == Player.STATE_ENDED) c.seekTo(c.currentMediaItemIndex, 0)
        if (c.isPlaying) c.pause() else c.play()
    }

    fun retry() {
        controller?.run {
            prepare()
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun seekBy(deltaMs: Long) {
        val c = controller ?: return
        c.seekTo((c.currentPosition + deltaMs).coerceAtLeast(0L))
    }

    fun next() {
        controller?.takeIf { it.hasNextMediaItem() }?.seekToNextMediaItem()
    }

    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 4_000 || !c.hasPreviousMediaItem()) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(speed: Float) {
        controller?.playbackParameters = PlaybackParameters(speed)
    }

    fun stop() {
        rememberPosition()
        clearSleepTimer()
        controller?.run {
            stop()
            clearMediaItems()
        }
    }

    fun setSleepTimer(timer: SleepTimer) {
        sleepJob?.cancel()
        sleepJob = null
        if (timer.minutes > 0) {
            val endsAt = System.currentTimeMillis() + timer.minutes * 60_000L
            sleepJob = scope.launch(Dispatchers.Main) {
                delay(timer.minutes * 60_000L)
                controller?.pause()
                clearSleepTimer()
            }
            _state.value = _state.value.copy(sleepTimer = timer, sleepEndsAt = endsAt)
        } else {
            _state.value = _state.value.copy(sleepTimer = timer, sleepEndsAt = null)
        }
    }

    private fun clearSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _state.value = _state.value.copy(sleepTimer = SleepTimer.Off, sleepEndsAt = null)
    }

    private fun rememberPosition() {
        val c = controller ?: return
        val key = MediaIds.parse(c.currentMediaItem?.mediaId) ?: return
        library.setLastPlayed(key.reciterId, key.surah, c.currentPosition)
    }

    private fun publish() {
        val c = controller ?: return
        val key = MediaIds.parse(c.currentMediaItem?.mediaId)
        _state.value = _state.value.copy(
            connected = true,
            reciterId = key?.reciterId,
            surah = key?.surah,
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            playWhenReady = c.playWhenReady,
            repeatMode = c.repeatMode,
            speed = c.playbackParameters.speed,
            hasNext = c.hasNextMediaItem(),
            hasPrevious = c.hasPreviousMediaItem(),
            error = c.playerError?.let { "Couldn't play this recitation. Check your connection and try again." },
        )
    }
}

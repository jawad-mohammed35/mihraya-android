package com.mihraya.app.feature.tasbih

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mihraya.app.AppContainer
import com.mihraya.app.data.library.DhikrPreset
import com.mihraya.app.data.library.TasbihSession
import com.mihraya.app.data.library.TasbihState
import com.mihraya.app.data.library.allDhikr
import com.mihraya.app.data.library.dhikr
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TapFeedback { Tick, Milestone, Completed }

@OptIn(FlowPreview::class)
class TasbihViewModel(private val container: AppContainer) : ViewModel() {

    private val library = container.library
    private val _state = MutableStateFlow(library.state.value.tasbih)
    val state: StateFlow<TasbihState> = _state.asStateFlow()
    private var touched = false

    val presets: StateFlow<List<DhikrPreset>> = library.flow
        .map { it.allDhikr() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, library.state.value.allDhikr())

    val sessions: StateFlow<List<TasbihSession>> = library.flow
        .map { it.sessions }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val stored = library.current().tasbih
            if (!touched) _state.value = stored
        }
        viewModelScope.launch {
            _state.drop(1).debounce(600).collect { library.saveTasbih(it) }
        }
    }

    fun preset(id: String): DhikrPreset = library.state.value.dhikr(id)

    fun increment(): TapFeedback {
        touched = true
        val s = _state.value
        val now = System.currentTimeMillis()
        val startingRound = s.count == 0 || s.count >= s.target
        val count = if (s.count >= s.target) 1 else s.count + 1
        val startedAt = if (startingRound || s.startedAt == 0L) now else s.startedAt
        val completed = count == s.target
        if (completed) {
            library.addSession(TasbihSession(preset(s.presetId).transliteration, s.target, s.target, startedAt, now))
        }
        _state.value = s.copy(count = count, rounds = s.rounds + if (completed) 1 else 0, startedAt = startedAt)
        return when {
            completed -> TapFeedback.Completed
            s.target >= 99 && count % 33 == 0 -> TapFeedback.Milestone
            else -> TapFeedback.Tick
        }
    }

    fun undo() {
        touched = true
        val s = _state.value
        if (s.count > 0) _state.value = s.copy(count = s.count - 1)
    }

    fun reset() {
        touched = true
        recordPartial()
        _state.value = _state.value.copy(count = 0, rounds = 0, startedAt = 0L)
    }

    fun select(preset: DhikrPreset) {
        touched = true
        if (preset.id == _state.value.presetId) return
        recordPartial()
        _state.value = TasbihState(presetId = preset.id, count = 0, target = preset.target, rounds = 0, startedAt = 0L)
    }

    fun setTarget(target: Int) {
        touched = true
        val s = _state.value
        _state.value = s.copy(target = target.coerceIn(1, 100_000), count = s.count.coerceAtMost(target))
    }

    fun addCustom(arabic: String, transliteration: String, meaning: String, target: Int) {
        val id = library.addDhikr(arabic.trim(), transliteration.trim(), meaning.trim(), target)
        touched = true
        recordPartial()
        _state.value = TasbihState(presetId = id, count = 0, target = target, rounds = 0, startedAt = 0L)
    }

    fun removeCustom(preset: DhikrPreset) {
        if (!preset.custom) return
        if (_state.value.presetId == preset.id) {
            touched = true
            _state.value = TasbihState()
        }
        library.removeDhikr(preset.id)
    }

    fun clearHistory() = library.clearSessions()

    private fun recordPartial() {
        val s = _state.value
        if (s.count in 1 until s.target) {
            library.addSession(
                TasbihSession(preset(s.presetId).transliteration, s.count, s.target, s.startedAt.takeIf { it > 0 } ?: System.currentTimeMillis(), System.currentTimeMillis()),
            )
        }
    }

    override fun onCleared() {
        library.saveTasbih(_state.value)
        super.onCleared()
    }
}

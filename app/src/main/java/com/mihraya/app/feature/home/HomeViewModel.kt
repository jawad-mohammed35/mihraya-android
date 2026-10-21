package com.mihraya.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mihraya.app.AppContainer
import com.mihraya.app.core.util.tickerFlow
import com.mihraya.app.data.library.LastPlayed
import com.mihraya.app.data.library.ReadingPosition
import com.mihraya.app.data.prayer.DayNote
import com.mihraya.app.data.prayer.HijriDate
import com.mihraya.app.data.prayer.IslamicDays
import com.mihraya.app.data.prayer.PrayerDay
import com.mihraya.app.data.prayer.PrayerSchedule
import com.mihraya.app.data.prayer.Salah
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.data.quran.Verse
import com.mihraya.app.data.settings.AlertMode
import com.mihraya.app.data.settings.AppSettings
import com.mihraya.app.data.settings.PrayerPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HomeDashboard(
    val settings: AppSettings,
    val schedule: PrayerSchedule?,
    val today: PrayerDay?,
    val night: PrayerDay?,
    val date: LocalDate,
    val hijri: HijriDate?,
    val note: DayNote?,
    val lastRead: ReadingPosition?,
    val lastPlayed: LastPlayed?,
    val tasbihToday: Int,
    val qiblaBearing: Double?,
    val verse: Verse?,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    val now: StateFlow<Instant> = tickerFlow(1_000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Instant.now())

    private val minute = tickerFlow(60_000L)
    private val verse = MutableStateFlow<Verse?>(null)
    private var cachedSchedule: PrayerSchedule? = null
    private var cachedPrefs: PrayerPreferences? = null

    init {
        viewModelScope.launch {
            combine(container.settings.flow, minute) { settings, instant ->
                Triple(
                    settings.reader.script,
                    settings.reader.translations.firstOrNull() ?: 20,
                    instant.atZone(settings.location?.zoneId ?: ZoneId.systemDefault()).toLocalDate(),
                )
            }.distinctUntilChanged().collectLatest { (script, translation, date) ->
                verse.value = runCatching {
                    container.quran.verse(QuranMeta.verseOfTheDay(date), script, translation)
                }.getOrNull()
            }
        }
    }

    val dashboard: StateFlow<HomeDashboard?> = combine(
        container.settings.flow,
        container.library.flow,
        minute,
        verse,
    ) { settings, library, instant, verseOfDay ->
        val schedule = scheduleFor(settings)
        val zone = schedule?.zone ?: ZoneId.systemDefault()
        val zoned = instant.atZone(zone)
        val date = zoned.toLocalDate()
        val hijri = HijriDate.of(date, settings.hijriOffset)
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val tasbihToday = library.sessions.filter { it.endedAt >= startOfDay }.sumOf { it.count } +
            if (library.tasbih.startedAt >= startOfDay) library.tasbih.count else 0
        HomeDashboard(
            settings = settings,
            schedule = schedule,
            today = schedule?.day(date),
            night = schedule?.night(zoned),
            date = date,
            hijri = hijri,
            note = IslamicDays.noteFor(date, hijri),
            lastRead = library.lastRead,
            lastPlayed = library.lastPlayed,
            tasbihToday = tasbihToday,
            qiblaBearing = settings.location?.let { container.prayerCalculator.qiblaBearing(it.latitude, it.longitude) },
            verse = verseOfDay,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun scheduleFor(settings: AppSettings): PrayerSchedule? {
        val location = settings.location ?: return null
        cachedSchedule?.let { cached ->
            if (cached.location == location && cachedPrefs == settings.prayer) return cached
        }
        return container.schedule(settings).also {
            cachedSchedule = it
            cachedPrefs = settings.prayer
        }
    }

    fun setAlert(salah: Salah, mode: AlertMode) = container.settings.edit { s ->
        s.copy(alerts = s.alerts.copy(modes = s.alerts.modes + (salah to mode)))
    }
}

package com.mihraya.app

import android.app.Application
import android.content.Context
import com.mihraya.app.data.audio.AudioDownloads
import com.mihraya.app.data.audio.DuaAudio
import com.mihraya.app.data.library.LibraryRepository
import com.mihraya.app.data.location.LocationRepository
import com.mihraya.app.data.prayer.PrayerCalculator
import com.mihraya.app.data.prayer.PrayerSchedule
import com.mihraya.app.data.prayer.suggestedAsr
import com.mihraya.app.data.prayer.suggestedMethod
import com.mihraya.app.data.quran.QuranRepository
import com.mihraya.app.data.settings.AppSettings
import com.mihraya.app.data.settings.SavedLocation
import com.mihraya.app.data.settings.SettingsRepository
import com.mihraya.app.notifications.PrayerAlarmScheduler
import com.mihraya.app.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(private val app: Application) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val settings = SettingsRepository(app, json, appScope)
    val library = LibraryRepository(app, json, appScope)
    val prayerCalculator = PrayerCalculator()
    val location = LocationRepository(app, http, json)
    val quran = QuranRepository(app, http, json, appScope)
    val audioDownloads = AudioDownloads(app)
    val duaAudio = DuaAudio(app, http)
    val player = PlayerConnection(app, appScope, library, audioDownloads)
    val alarms = PrayerAlarmScheduler(app, prayerCalculator)

    /** Android 15 refuses audio focus to services started by an alarm, so recitation pauses on this signal instead. */
    val adhanPlaying = MutableStateFlow(false)

    fun schedule(settings: AppSettings): PrayerSchedule? =
        settings.location?.let { PrayerSchedule(prayerCalculator, it, settings.prayer) }

    /** Saves a new location and, unless the user chose them explicitly, adopts the regional calculation defaults. */
    fun applyLocation(location: SavedLocation, keepAutomatic: Boolean = location.automatic) = settings.edit { s ->
        val prayer = s.prayer.copy(
            method = if (s.prayer.methodSetByUser) s.prayer.method else suggestedMethod(location.countryCode),
            asr = if (s.prayer.asrSetByUser) s.prayer.asr else suggestedAsr(location.countryCode),
        )
        s.copy(location = location, autoLocation = keepAutomatic, prayer = prayer)
    }

    suspend fun refreshAutomaticLocation() {
        val current = settings.current()
        if (!current.autoLocation || !location.hasPermission()) return
        val last = current.location
        if (last != null && last.automatic && System.currentTimeMillis() - last.updatedAt < LOCATION_REFRESH_MS) return
        runCatching { location.detect() }.onSuccess { applyLocation(it, keepAutomatic = true) }
    }

    fun start() {
        appScope.launch {
            settings.flow
                .map { Triple(it.location, it.prayer, it.alerts) to it.use24Hour }
                .distinctUntilChanged()
                .collect { alarms.reschedule(settings.current()) }
        }
    }

    private companion object {
        const val LOCATION_REFRESH_MS = 3 * 60 * 60 * 1000L
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as MihrayaApp).container

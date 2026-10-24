package com.mihraya.app.widget

import android.content.Context
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.mihraya.app.MainActivity
import com.mihraya.app.R
import com.mihraya.app.appContainer
import com.mihraya.app.core.util.TimeFormat
import com.mihraya.app.data.audio.Reciters
import com.mihraya.app.data.prayer.PrayerDay
import com.mihraya.app.data.prayer.PrayerMoment
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.data.settings.QuranScript
import java.time.ZonedDateTime

private val destKey = ActionParameters.Key<String>(Destinations.EXTRA)
private val surahKey = ActionParameters.Key<Int>(Destinations.SURAH)
private val ayahKey = ActionParameters.Key<Int>(Destinations.AYAH)

@Composable
private fun openMihraya(dest: String, surah: Int = 1, ayah: Int = 1) = actionStartActivity<MainActivity>(
    actionParametersOf(destKey to dest, surahKey to surah, ayahKey to ayah),
)

private data class PrayerRow(val name: String, val time: ZonedDateTime, val upcoming: Boolean)

private data class PrayerBoard(
    val use24Hour: Boolean,
    val rows: List<PrayerRow>,
    val next: PrayerMoment?,
    val now: ZonedDateTime,
)

private suspend fun board(context: Context, options: WidgetOptions): PrayerBoard? {
    val settings = context.appContainer.settings.current()
    val schedule = context.appContainer.schedule(settings) ?: return null
    val now = schedule.now()
    val day = schedule.day(now.toLocalDate()) ?: return null
    val night = schedule.night(now)
    val rows = prayerRows(day, night?.lastThird ?: day.lastThird, now, options)
    return PrayerBoard(settings.use24Hour, rows, schedule.next(now), now)
}

private fun prayerRows(day: PrayerDay, qiyam: ZonedDateTime, now: ZonedDateTime, options: WidgetOptions): List<PrayerRow> {
    val times = buildList {
        add("Fajr" to day.fajr)
        if (options.sunrise) add("Sunrise" to day.sunrise)
        if (options.duha) add("Duha" to day.duhaStart)
        add(day.dhuhr.dayOfWeek.let { if (it == java.time.DayOfWeek.FRIDAY) "Jumuʿah" else "Dhuhr" } to day.dhuhr)
        add("Asr" to day.asr)
        add("Maghrib" to day.maghrib)
        add("Isha" to day.isha)
        if (options.qiyam) add("Qiyam" to qiyam)
    }
    val upcoming = times.filter { it.second.isAfter(now) }.minByOrNull { it.second }?.second
    return times.map { (name, time) -> PrayerRow(name, time, time == upcoming) }
}

class PrayerTimetableWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val options = options(context, id)
        val board = runCatching { board(context, options) }.getOrNull()
        provideContent {
            val colors = options.theme.colors()
            WidgetFrame(options.theme, openMihraya(Destinations.TIMETABLE)) {
                WidgetTitle("Prayer times", colors)
                Spacer(GlanceModifier.height(8.dp))
                if (board == null) {
                    Text("Set your location in Mihraya.", style = colors.style(14), maxLines = 2)
                } else if (options.layout == TimetableLayout.Horizontal || LocalSize.current.width > LocalSize.current.height * 1.6f) {
                    Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        board.rows.forEach { row ->
                            Column(GlanceModifier.defaultWeight().padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(row.name, style = colors.style(11, color = if (row.upcoming) colors.accent else colors.muted), maxLines = 1)
                                Text(TimeFormat.clock(row.time, board.use24Hour), style = colors.style(13, FontWeight.Medium, if (row.upcoming) colors.accent else colors.text), maxLines = 1)
                            }
                        }
                    }
                } else {
                    board.rows.forEach { row ->
                        Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(row.name, style = colors.style(14, if (row.upcoming) FontWeight.Bold else FontWeight.Normal, if (row.upcoming) colors.accent else colors.text), maxLines = 1)
                            Spacer(GlanceModifier.defaultWeight())
                            Text(TimeFormat.full(row.time, board.use24Hour), style = colors.style(14, FontWeight.Medium, if (row.upcoming) colors.accent else colors.muted), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

class NextPrayerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val options = options(context, id)
        val board = runCatching { board(context, WidgetOptions()) }.getOrNull()
        provideContent {
            val colors = options.theme.colors()
            WidgetFrame(options.theme, openMihraya(Destinations.TIMETABLE)) {
                WidgetTitle("Next prayer", colors)
                val next = board?.next
                if (next == null) {
                    Spacer(GlanceModifier.height(8.dp))
                    Text("Set your location in Mihraya.", style = colors.style(14), maxLines = 2)
                } else {
                    Spacer(GlanceModifier.height(6.dp))
                    Text(next.salah.labelOn(next.time.toLocalDate()), style = colors.style(26, FontWeight.Bold), maxLines = 1)
                    Text(TimeFormat.full(next.time, board.use24Hour), style = colors.style(14, color = colors.muted), maxLines = 1)
                    Spacer(GlanceModifier.height(4.dp))
                    val remaining = java.time.Duration.between(board.now, next.time).toMillis()
                    Countdown(remaining, colors.text)
                }
            }
        }
    }
}

class QiblaWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val options = options(context, id)
        val place = runCatching {
            val settings = context.appContainer.settings.current()
            settings.location?.let { location ->
                val calc = context.appContainer.prayerCalculator
                val km = calc.distanceToKaabaKm(location.latitude, location.longitude)
                val bearing = calc.qiblaBearing(location.latitude, location.longitude)
                Triple(settings.distanceUnit, km, bearing)
            }
        }.getOrNull()
        provideContent {
            val colors = options.theme.colors()
            WidgetFrame(options.theme, openMihraya(Destinations.QIBLA)) {
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Qibla", style = colors.style(18, FontWeight.Bold, colors.accent), maxLines = 1)
                        if (place != null) {
                            val (unit, km, bearing) = place
                            Spacer(GlanceModifier.height(4.dp))
                            Text(
                                "${bearing.toInt()}° · ${unit.fromKm(km).toInt()} ${unit.symbol}",
                                style = colors.style(12, color = colors.muted),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

class VerseWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val options = options(context, id)
        val verse = runCatching {
            val ref = QuranMeta.verseOfTheDay(java.time.LocalDate.now())
            context.appContainer.quran.verse(ref, QuranScript.Uthmani, 20) to QuranMeta.surah(ref.surah)
        }.getOrNull()
        provideContent {
            val colors = options.theme.colors()
            val surah = verse?.second?.number ?: 1
            val ayah = verse?.first?.ayah ?: 1
            WidgetFrame(options.theme, openMihraya(Destinations.READER, surah, ayah)) {
                WidgetTitle("Verse of the day", colors)
                if (verse == null) {
                    Text("Open Mihraya to load the verse.", style = colors.style(14), maxLines = 2)
                } else {
                    val (text, info) = verse
                    Spacer(GlanceModifier.height(6.dp))
                    Text(text.arabic, style = colors.style(18), maxLines = if (LocalSize.current.height < 160.dp) 3 else 5)
                    if (options.showTranslation) {
                        Spacer(GlanceModifier.height(4.dp))
                        Text(text.translations.firstOrNull()?.text.orEmpty(), style = colors.style(13, color = colors.muted), maxLines = 3)
                    }
                    Spacer(GlanceModifier.height(4.dp))
                    Text("${info.name} ${text.ref.key}", style = colors.style(12, color = colors.accent), maxLines = 1)
                }
            }
        }
    }
}

class LastReadWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val options = options(context, id)
        val position = runCatching { context.appContainer.library.current().lastRead }.getOrNull()
        provideContent {
            val colors = options.theme.colors()
            val surah = position?.surah ?: 1
            val ayah = position?.ayah ?: 1
            WidgetFrame(options.theme, openMihraya(Destinations.READER, surah, ayah)) {
                WidgetTitle("Continue reading", colors)
                Spacer(GlanceModifier.height(6.dp))
                if (position == null) {
                    Text("Open a sūrah in Mihraya.", style = colors.style(14), maxLines = 2)
                } else {
                    val info = QuranMeta.surah(position.surah)
                    Text(info.name, style = colors.style(20, FontWeight.Bold), maxLines = 1)
                    Text("Āyah ${position.ayah}", style = colors.style(14, color = colors.muted), maxLines = 1)
                }
            }
        }
    }
}

class BookmarksWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val options = options(context, id)
        val bookmarks = runCatching { context.appContainer.library.current().bookmarks }.getOrDefault(emptyList())
        provideContent {
            val colors = options.theme.colors()
            val limit = if (LocalSize.current.height < 180.dp) 2 else 4
            WidgetFrame(options.theme) {
                WidgetTitle("Bookmarks", colors)
                Spacer(GlanceModifier.height(6.dp))
                if (bookmarks.isEmpty()) {
                    Text("Save a verse while reading and it will appear here.", style = colors.style(14), maxLines = 3)
                } else {
                    bookmarks.take(limit).forEach { bookmark ->
                        val info = QuranMeta.surah(bookmark.surah)
                        Text(
                            "${info.name} ${bookmark.surah}:${bookmark.ayah}",
                            style = colors.style(15, FontWeight.Medium),
                            maxLines = 1,
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)
                                .clickable(openMihraya(Destinations.READER, bookmark.surah, bookmark.ayah)),
                        )
                    }
                }
            }
        }
    }
}

class AudioWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val options = options(context, id)
        val last = runCatching { context.appContainer.library.current().lastPlayed }.getOrNull()
        val playing = com.mihraya.app.playback.PlaybackService.isPlaying
        provideContent {
            val colors = options.theme.colors()
            WidgetFrame(options.theme) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(GlanceModifier.defaultWeight().clickable(openMihraya(Destinations.PLAYER))) {
                        WidgetTitle(if (playing) "Playing" else "Resume", colors)
                        Spacer(GlanceModifier.height(4.dp))
                        if (last == null) {
                            Text("Nothing played yet.", style = colors.style(14), maxLines = 2)
                        } else {
                            Text(QuranMeta.surah(last.surah).name, style = colors.style(16, FontWeight.Bold), maxLines = 1)
                            Text(
                                "${Reciters.byId(last.reciterId).shortName} · ${TimeFormat.media(last.positionMs)}",
                                style = colors.style(12, color = colors.muted),
                                maxLines = 1,
                            )
                        }
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Box(
                        GlanceModifier
                            .cornerRadius(18.dp)
                            .background(colors.accent)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .clickable(actionRunCallback<ToggleAudioAction>()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (playing) "Pause" else "Play", style = colors.style(13, FontWeight.Bold, colors.onAccent), maxLines = 1)
                    }
                }
            }
        }
    }
}

class TasbihWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(id)
        val options = WidgetPrefs.read(context, widgetId)
        val tasbih = runCatching { context.appContainer.library.current().tasbih }.getOrNull()
        provideContent {
            val colors = options.theme.colors()
            WidgetFrame(options.theme) {
                Row(GlanceModifier.fillMaxWidth()) {
                    WidgetTitle("Tasbih", colors)
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        "Open",
                        style = colors.style(12, color = colors.accent),
                        modifier = GlanceModifier.clickable(openMihraya(Destinations.TASBIH)),
                    )
                }
                if (tasbih == null) {
                    Text("Open Mihraya to start counting.", style = colors.style(14), maxLines = 2)
                } else {
                    val done = tasbih.count == tasbih.target && tasbih.count > 0
                    Box(
                        GlanceModifier.fillMaxWidth().defaultWeight().clickable(actionRunCallback<TasbihCountAction>()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                tasbih.count.toString(),
                                style = colors.style(42, FontWeight.Bold, if (done) colors.accent else colors.text),
                                maxLines = 1,
                            )
                            Text(
                                if (done) "Round complete" else "of ${tasbih.target}",
                                style = colors.style(13, color = colors.muted),
                                maxLines = 1,
                            )
                        }
                    }
                    Row(GlanceModifier.fillMaxWidth()) {
                        Text(
                            "Target ${tasbih.target}",
                            style = colors.style(12, color = colors.accent),
                            modifier = GlanceModifier.clickable(actionRunCallback<TasbihTargetAction>()),
                        )
                        Spacer(GlanceModifier.defaultWeight())
                        Text(
                            if (options.confirmReset) "Confirm reset" else "Reset",
                            style = colors.style(12, color = if (options.confirmReset) colors.accent else colors.muted),
                            modifier = GlanceModifier.clickable(actionRunCallback<TasbihResetAction>()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Countdown(remainingMs: Long, color: Color) {
    if (remainingMs <= 0L) return
    val context = LocalContext.current
    AndroidRemoteViews(
        remoteViews = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
            setTextColor(R.id.countdown, color.toArgb())
            setChronometerCountDown(R.id.countdown, true)
            setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + remainingMs, null, true)
        },
    )
}

private suspend fun options(context: Context, id: GlanceId): WidgetOptions =
    WidgetPrefs.read(context, androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(id))

class PrayerTimetableReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = PrayerTimetableWidget()
    override fun onDeleted(context: Context, appWidgetIds: IntArray) = clear(context, appWidgetIds)
}

class NextPrayerReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = NextPrayerWidget()
    override fun onDeleted(context: Context, appWidgetIds: IntArray) = clear(context, appWidgetIds)
}

class QiblaReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QiblaWidget()
    override fun onDeleted(context: Context, appWidgetIds: IntArray) = clear(context, appWidgetIds)
}

class VerseReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = VerseWidget()
    override fun onDeleted(context: Context, appWidgetIds: IntArray) = clear(context, appWidgetIds)
}

class LastReadReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = LastReadWidget()
    override fun onDeleted(context: Context, appWidgetIds: IntArray) = clear(context, appWidgetIds)
}

class BookmarksReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BookmarksWidget()
    override fun onDeleted(context: Context, appWidgetIds: IntArray) = clear(context, appWidgetIds)
}

class AudioReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = AudioWidget()
    override fun onDeleted(context: Context, appWidgetIds: IntArray) = clear(context, appWidgetIds)
}

class TasbihReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TasbihWidget()
    override fun onDeleted(context: Context, appWidgetIds: IntArray) = clear(context, appWidgetIds)
}

private fun clear(context: Context, ids: IntArray) {
    ids.forEach { WidgetPrefs.clear(context, it) }
}

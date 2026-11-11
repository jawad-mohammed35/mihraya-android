package com.mihraya.app.widget

import android.content.Context
import android.os.SystemClock
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
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
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import com.mihraya.app.MainActivity
import com.mihraya.app.R
import com.mihraya.app.appContainer
import com.mihraya.app.core.util.TimeFormat
import com.mihraya.app.data.audio.Reciters
import com.mihraya.app.data.library.dhikr
import com.mihraya.app.data.prayer.PrayerDay
import com.mihraya.app.data.prayer.PrayerMoment
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.data.settings.QuranScript
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min

private val destKey = ActionParameters.Key<String>(Destinations.EXTRA)
private val surahKey = ActionParameters.Key<Int>(Destinations.SURAH)
private val ayahKey = ActionParameters.Key<Int>(Destinations.AYAH)
private val keepTasbihKey = ActionParameters.Key<Boolean>(Destinations.KEEP_TASBIH)

@Composable
private fun openMihraya(dest: String, surah: Int = 1, ayah: Int = 1) = actionStartActivity<MainActivity>(
    actionParametersOf(destKey to dest, surahKey to surah, ayahKey to ayah, keepTasbihKey to (dest == Destinations.TASBIH)),
)

internal data class PrayerRow(val name: String, val time: ZonedDateTime, val upcoming: Boolean, val extra: Boolean)

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
        add(Triple("Fajr", day.fajr, false))
        if (options.sunrise) add(Triple("Sunrise", day.sunrise, true))
        if (options.duha) add(Triple("Duha", day.duhaStart, true))
        add(Triple(if (day.dhuhr.dayOfWeek == java.time.DayOfWeek.FRIDAY) "Jumuʿah" else "Dhuhr", day.dhuhr, false))
        add(Triple("Asr", day.asr, false))
        add(Triple("Maghrib", day.maghrib, false))
        add(Triple("Isha", day.isha, false))
        if (options.qiyam) add(Triple("Qiyam", qiyam, true))
    }
    val upcoming = times.filter { it.second.isAfter(now) }.minByOrNull { it.second }?.second
    return times.map { (name, time, extra) -> PrayerRow(name, time, time == upcoming, extra) }
}

/**
 * Picks the rows that fit: optional times other than the upcoming one go first, and when even the
 * five daily prayers don't fit, the window that starts at the upcoming time.
 */
internal fun List<PrayerRow>.fitting(capacity: Int): List<PrayerRow> {
    if (size <= capacity) return this
    val core = filterNot { it.extra }
    if (core.size <= capacity) {
        val extras = filter { it.extra }.sortedByDescending { it.upcoming }.take(capacity - core.size).toSet()
        return filter { !it.extra || it in extras }
    }
    val pool = if (any { it.upcoming && it.extra }) this else core
    val count = capacity.coerceAtLeast(1)
    val upcoming = pool.indexOfFirst { it.upcoming }.takeIf { it >= 0 } ?: (pool.size - count)
    val start = upcoming.coerceIn(0, pool.size - count)
    return pool.subList(start, start + count)
}

@Composable
private fun ColumnScope.EmptyState(title: String, message: String, colors: WidgetColors) {
    val inner = innerSize()
    if (inner.height >= 70.dp) {
        WidgetTitle(title, colors)
        Spacer(GlanceModifier.height(6.dp))
    }
    val lines = max(1, ((inner.height - 20.dp) / 18.dp).toInt())
    Text(message, style = colors.style(if (inner.width < 120.dp) 12 else 14), maxLines = min(lines, 3))
}

class PrayerTimetableWidget : GlanceAppWidget() {
    override val sizeMode = responsive(
        110 to 50, 220 to 50, 110 to 100, 220 to 100, 110 to 150,
        160 to 150, 220 to 150, 160 to 220, 220 to 220, 300 to 300,
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = appWidgetId(context, id)
        provideWidget({
            val options = WidgetPrefs.read(context, widgetId)
            options to runCatching { board(context, options) }.getOrNull()
        }) { (options, board) ->
            val colors = options.theme.colors()
            WidgetFrame(colors, openMihraya(Destinations.TIMETABLE)) {
                if (board == null) {
                    EmptyState("Prayer times", "Set your location in Mihraya.", colors)
                } else {
                    val size = LocalSize.current
                    val inner = innerSize()
                    when {
                        inner.height < 60.dp && inner.width < 180.dp -> NextPrayerLine(board, colors, countdown = false)
                        inner.height < 60.dp -> TimetableStrip(board, colors, showTitle = false)
                        options.layout == TimetableLayout.Horizontal || size.width > size.height * 1.6f ->
                            TimetableStrip(board, colors, showTitle = inner.height >= 80.dp)
                        else -> TimetableList(board, colors)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.TimetableStrip(board: PrayerBoard, colors: WidgetColors, showTitle: Boolean) {
    val inner = innerSize()
    val lines = if (inner.height >= 150.dp && board.rows.size > 4) 2 else 1
    val rows = board.rows.fitting(max(1, (inner.width / 44.dp).toInt()) * lines)
    val perLine = (rows.size + lines - 1) / lines
    val column = inner.width / perLine
    val lineHeight = (inner.height - if (showTitle) 20.dp else 0.dp) / lines
    val nameSize = when {
        column >= 64.dp && lineHeight >= 70.dp -> 13
        column >= 56.dp && lineHeight >= 50.dp -> 12
        else -> 11
    }
    val timeSize = when {
        column >= 64.dp && lineHeight >= 70.dp -> 20
        column >= 56.dp && lineHeight >= 50.dp -> 16
        else -> 13
    }
    if (showTitle) WidgetTitle("Prayer times", colors)
    rows.chunked(perLine).forEach { line ->
        Row(
            GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            line.forEach { row ->
                val tint = if (row.upcoming) colors.accent else colors.text
                Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(row.name, style = colors.style(nameSize, color = if (row.upcoming) colors.accent else colors.muted), maxLines = 1)
                    Text(TimeFormat.clock(row.time, board.use24Hour), style = colors.style(timeSize, FontWeight.Medium, tint), maxLines = 1)
                }
            }
            repeat(perLine - line.size) { Spacer(GlanceModifier.defaultWeight()) }
        }
    }
}

@Composable
private fun ColumnScope.TimetableList(board: PrayerBoard, colors: WidgetColors) {
    val inner = innerSize()
    val minRow = 20.dp
    val titleHeight = 22.dp
    val showTitle = inner.height - titleHeight >= minRow * 5
    val available = if (showTitle) inner.height - titleHeight else inner.height
    val rows = board.rows.fitting(max(1, (available / minRow).toInt()))
    val rowHeight = available / rows.size
    val textSize = when {
        rowHeight >= 34.dp && inner.width >= 200.dp -> 17
        rowHeight >= 26.dp -> 15
        else -> 13
    }
    val withSuffix = inner.width >= 130.dp
    if (showTitle) {
        Row(GlanceModifier.fillMaxWidth().height(titleHeight)) {
            WidgetTitle("Prayer times", colors)
            if (inner.width >= 200.dp) {
                Spacer(GlanceModifier.defaultWeight())
                Text(TimeFormat.shortDate(board.now), style = colors.style(11, color = colors.muted), maxLines = 1)
            }
        }
    }
    rows.forEach { row ->
        Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.name,
                style = colors.style(textSize, if (row.upcoming) FontWeight.Bold else FontWeight.Normal, if (row.upcoming) colors.accent else colors.text),
                maxLines = 1,
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                if (withSuffix) TimeFormat.full(row.time, board.use24Hour) else TimeFormat.clock(row.time, board.use24Hour),
                style = colors.style(textSize, FontWeight.Medium, if (row.upcoming) colors.accent else colors.muted),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ColumnScope.NextPrayerLine(board: PrayerBoard, colors: WidgetColors, countdown: Boolean) {
    val next = board.next ?: return
    val inner = innerSize()
    Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
        Text(next.salah.labelOn(next.time.toLocalDate()), style = colors.style(16, FontWeight.Bold, colors.accent), maxLines = 1)
        Spacer(GlanceModifier.width(8.dp))
        Text(TimeFormat.full(next.time, board.use24Hour), style = colors.style(14, color = colors.muted), maxLines = 1)
        if (countdown && inner.width >= 200.dp) {
            Spacer(GlanceModifier.defaultWeight())
            Countdown(java.time.Duration.between(board.now, next.time).toMillis(), colors, 18)
        }
    }
}

class NextPrayerWidget : GlanceAppWidget() {
    override val sizeMode = responsive(
        110 to 50, 180 to 50, 250 to 50, 110 to 100, 180 to 100, 250 to 100, 160 to 160, 250 to 180,
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = appWidgetId(context, id)
        provideWidget({
            WidgetPrefs.read(context, widgetId) to runCatching { board(context, WidgetOptions()) }.getOrNull()
        }) { (options, board) ->
            val colors = options.theme.colors()
            WidgetFrame(colors, openMihraya(Destinations.TIMETABLE)) {
                val next = board?.next
                val inner = innerSize()
                when {
                    board == null || next == null -> EmptyState("Next prayer", "Set your location in Mihraya.", colors)
                    inner.height < 60.dp -> NextPrayerLine(board, colors, countdown = true)
                    else -> {
                        val large = inner.width >= 200.dp && inner.height >= 140.dp
                        WidgetTitle("Next prayer", colors)
                        Column(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                next.salah.labelOn(next.time.toLocalDate()),
                                style = colors.style(if (large) 32 else if (inner.width >= 140.dp) 26 else 22, FontWeight.Bold),
                                maxLines = 1,
                            )
                            Text(TimeFormat.full(next.time, board.use24Hour), style = colors.style(14, color = colors.muted), maxLines = 1)
                            Spacer(GlanceModifier.height(4.dp))
                            Countdown(
                                java.time.Duration.between(board.now, next.time).toMillis(),
                                colors,
                                if (large) 30 else if (inner.height >= 100.dp) 24 else 20,
                            )
                        }
                        val following = board.rows.firstOrNull { !it.extra && it.time.isAfter(next.time) }
                        if (inner.height >= 150.dp && following != null) {
                            Text(
                                "Then ${following.name} · ${TimeFormat.full(following.time, board.use24Hour)}",
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

private val compassPoints = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")

private fun compassPoint(bearing: Double): String = compassPoints[Math.floorMod((bearing / 22.5 + 0.5).toInt(), 16)]

class QiblaWidget : GlanceAppWidget() {
    override val sizeMode = responsive(50 to 50, 110 to 50, 110 to 100, 180 to 100, 160 to 160, 250 to 100, 250 to 180)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = appWidgetId(context, id)
        provideWidget({
            WidgetPrefs.read(context, widgetId) to runCatching { qiblaPlace(context) }.getOrNull()
        }) { (options, place) ->
            val colors = options.theme.colors()
            WidgetFrame(colors, openMihraya(Destinations.QIBLA)) {
                val inner = innerSize()
                val tiny = inner.width < 90.dp || inner.height < 60.dp
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when {
                        place == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Qibla", style = colors.style(if (tiny) 14 else 18, FontWeight.Bold, colors.accent), maxLines = 1)
                            if (!tiny) Text("Set your location", style = colors.style(12, color = colors.muted), maxLines = 1)
                        }
                        tiny -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${place.bearing.toInt()}°", style = colors.style(if (inner.width < 70.dp) 17 else 20, FontWeight.Bold, colors.accent), maxLines = 1)
                            Text(
                                if (inner.width >= 90.dp) "Qibla · ${compassPoint(place.bearing)}" else compassPoint(place.bearing),
                                style = colors.style(11, color = colors.muted),
                                maxLines = 1,
                            )
                        }
                        inner.width >= 200.dp && inner.height < 120.dp -> Row(
                            GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(GlanceModifier.defaultWeight()) {
                                Text("Qibla", style = colors.style(18, FontWeight.Bold, colors.accent), maxLines = 1)
                                Text("${place.distance} to the Kaʿbah", style = colors.style(12, color = colors.muted), maxLines = 1)
                            }
                            Text("${place.bearing.toInt()}° ${compassPoint(place.bearing)}", style = colors.style(28, FontWeight.Bold), maxLines = 1)
                        }
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val large = inner.height >= 140.dp
                            Text("Qibla", style = colors.style(if (large) 20 else 18, FontWeight.Bold, colors.accent), maxLines = 1)
                            Spacer(GlanceModifier.height(2.dp))
                            Text("${place.bearing.toInt()}°", style = colors.style(if (large) 40 else 30, FontWeight.Bold), maxLines = 1)
                            Text("${compassPoint(place.bearing)} · ${place.distance}", style = colors.style(12, color = colors.muted), maxLines = 1)
                            if (large && place.name.isNotBlank()) {
                                Spacer(GlanceModifier.height(4.dp))
                                Text("From ${place.name}", style = colors.style(12, color = colors.muted), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class QiblaPlace(val name: String, val distance: String, val bearing: Double)

private suspend fun qiblaPlace(context: Context): QiblaPlace? {
    val settings = context.appContainer.settings.current()
    val location = settings.location ?: return null
    val calc = context.appContainer.prayerCalculator
    val km = calc.distanceToKaabaKm(location.latitude, location.longitude)
    val bearing = calc.qiblaBearing(location.latitude, location.longitude)
    return QiblaPlace(location.name, "${settings.distanceUnit.fromKm(km).toInt()} ${settings.distanceUnit.symbol}", bearing)
}

class VerseWidget : GlanceAppWidget() {
    override val sizeMode = responsive(
        180 to 50, 250 to 50, 180 to 100, 250 to 100, 180 to 160, 250 to 160, 250 to 240, 320 to 300,
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = appWidgetId(context, id)
        provideWidget({
            val verse = runCatching {
                val ref = QuranMeta.verseOfTheDay(java.time.LocalDate.now())
                context.appContainer.quran.verse(ref, QuranScript.Uthmani, 20) to QuranMeta.surah(ref.surah)
            }.getOrNull()
            WidgetPrefs.read(context, widgetId) to verse
        }) { (options, verse) ->
            val colors = options.theme.colors()
            val surah = verse?.second?.number ?: 1
            val ayah = verse?.first?.ayah ?: 1
            WidgetFrame(colors, openMihraya(Destinations.READER, surah, ayah)) {
                if (verse == null) {
                    EmptyState("Verse of the day", "Open Mihraya to load the verse.", colors)
                } else {
                    val (text, info) = verse
                    val inner = innerSize()
                    val reference = "${info.name} ${text.ref.key}"
                    if (inner.height < 60.dp) {
                        Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                            Text(text.ref.key, style = colors.style(12, color = colors.accent), maxLines = 1)
                            Spacer(GlanceModifier.width(10.dp))
                            Text(text.arabic, style = colors.style(17, align = TextAlign.Right), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                        }
                    } else {
                        val showTitle = inner.height >= 110.dp
                        val arabicSize = when {
                            inner.height >= 200.dp && inner.width >= 220.dp -> 24
                            inner.height >= 140.dp && inner.width >= 220.dp -> 21
                            inner.height >= 120.dp -> 19
                            else -> 17
                        }
                        var room = inner.height - 20.dp - (if (showTitle) 20.dp else 0.dp)
                        val translationLines = if (options.showTranslation && inner.height >= 120.dp) {
                            ((room * 0.32f) / 18.dp).toInt().coerceIn(1, 5)
                        } else {
                            0
                        }
                        if (translationLines > 0) room -= 18.dp * translationLines + 6.dp
                        val arabicLines = max(1, (room / (arabicSize * 1.75f).dp).toInt())
                        if (showTitle) WidgetTitle("Verse of the day", colors)
                        Column(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text.arabic,
                                style = colors.style(arabicSize, align = TextAlign.Right),
                                maxLines = arabicLines,
                                modifier = GlanceModifier.fillMaxWidth(),
                            )
                            if (translationLines > 0) {
                                Spacer(GlanceModifier.height(6.dp))
                                Text(text.translations.firstOrNull()?.text.orEmpty(), style = colors.style(13, color = colors.muted), maxLines = translationLines)
                            }
                        }
                        Text(reference, style = colors.style(12, color = colors.accent), maxLines = 1)
                    }
                }
            }
        }
    }
}

class LastReadWidget : GlanceAppWidget() {
    override val sizeMode = responsive(110 to 50, 180 to 50, 110 to 100, 180 to 100, 250 to 100, 160 to 160, 250 to 180)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = appWidgetId(context, id)
        provideWidget({
            WidgetPrefs.read(context, widgetId) to runCatching { context.appContainer.library.current().lastRead }.getOrNull()
        }) { (options, position) ->
            val colors = options.theme.colors()
            val surah = position?.surah ?: 1
            val ayah = position?.ayah ?: 1
            WidgetFrame(colors, openMihraya(Destinations.READER, surah, ayah)) {
                if (position == null) {
                    EmptyState("Continue reading", "Open a sūrah in Mihraya.", colors)
                } else {
                    val info = QuranMeta.surah(position.surah)
                    val inner = innerSize()
                    if (inner.height < 60.dp) {
                        Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                            Column(GlanceModifier.defaultWeight()) {
                                Text(info.name, style = colors.style(15, FontWeight.Bold), maxLines = 1)
                                Text("Āyah ${position.ayah}", style = colors.style(12, color = colors.muted), maxLines = 1)
                            }
                            if (inner.width >= 160.dp) Text(info.arabicName, style = colors.style(18, color = colors.accent), maxLines = 1)
                        }
                    } else {
                        val large = inner.width >= 200.dp && inner.height >= 140.dp
                        WidgetTitle("Continue reading", colors)
                        Spacer(GlanceModifier.defaultWeight())
                        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(GlanceModifier.defaultWeight()) {
                                Text(info.name, style = colors.style(if (large) 24 else 20, FontWeight.Bold), maxLines = 1)
                                Text(
                                    if (inner.width >= 140.dp) "Āyah ${position.ayah} of ${info.verseCount}" else "Āyah ${position.ayah}",
                                    style = colors.style(13, color = colors.muted),
                                    maxLines = 1,
                                )
                            }
                            if (inner.width >= 150.dp) {
                                Text(info.arabicName, style = colors.style(if (large) 24 else 20, color = colors.accent), maxLines = 1)
                            }
                        }
                        if (inner.height >= 140.dp) {
                            Text(info.meaning, style = colors.style(12, color = colors.muted), maxLines = 1)
                        }
                        Spacer(GlanceModifier.defaultWeight())
                        if (inner.height >= 90.dp) {
                            LinearProgressIndicator(
                                progress = position.ayah.toFloat() / info.verseCount,
                                modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                                color = colors.accent,
                                backgroundColor = colors.chip,
                            )
                        }
                    }
                }
            }
        }
    }
}

class BookmarksWidget : GlanceAppWidget() {
    override val sizeMode = responsive(
        110 to 50, 180 to 50, 110 to 100, 180 to 100, 250 to 100, 160 to 160, 250 to 180, 250 to 260,
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = appWidgetId(context, id)
        provideWidget({
            WidgetPrefs.read(context, widgetId) to
                runCatching { context.appContainer.library.current().bookmarks }.getOrDefault(emptyList())
        }) { (options, bookmarks) ->
            val colors = options.theme.colors()
            WidgetFrame(colors, openMihraya(Destinations.QURAN)) {
                val inner = innerSize()
                if (bookmarks.isEmpty()) {
                    EmptyState("Bookmarks", "Save a verse while reading and it will appear here.", colors)
                } else {
                    val showHeader = inner.height >= 70.dp
                    val rowHeight = 36.dp
                    val room = if (showHeader) inner.height - 20.dp else inner.height
                    val capacity = (room / rowHeight).toInt().coerceIn(1, 8)
                    if (showHeader) {
                        Row(GlanceModifier.fillMaxWidth().height(20.dp)) {
                            WidgetTitle("Bookmarks", colors)
                            Spacer(GlanceModifier.defaultWeight())
                            Text("${bookmarks.size}", style = colors.style(11, color = colors.muted), maxLines = 1)
                        }
                    }
                    val shown = bookmarks.take(capacity)
                    shown.forEach { bookmark ->
                        val info = QuranMeta.surah(bookmark.surah)
                        Row(
                            GlanceModifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .clickable(openMihraya(Destinations.READER, bookmark.surah, bookmark.ayah)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(info.name, style = colors.style(15, FontWeight.Medium), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                            Spacer(GlanceModifier.width(8.dp))
                            Text("${bookmark.surah}:${bookmark.ayah}", style = colors.style(13, color = colors.accent), maxLines = 1)
                        }
                    }
                    if (showHeader && capacity - shown.size >= 2) {
                        Spacer(GlanceModifier.defaultWeight())
                        Text("Save a verse while reading to add it here.", style = colors.style(12, color = colors.muted), maxLines = 2)
                    }
                }
            }
        }
    }
}

class AudioWidget : GlanceAppWidget() {
    override val sizeMode = responsive(110 to 50, 180 to 50, 250 to 50, 110 to 100, 180 to 100, 250 to 100, 160 to 160, 250 to 180)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = appWidgetId(context, id)
        provideWidget({
            Triple(
                WidgetPrefs.read(context, widgetId),
                runCatching { context.appContainer.library.current().lastPlayed }.getOrNull(),
                com.mihraya.app.playback.PlaybackService.isPlaying,
            )
        }) { (options, last, playing) ->
            val colors = options.theme.colors()
            val inner = innerSize()
            val toggle = actionRunCallback<ToggleAudioAction>()
            val label = if (playing) "Pause" else "Play"
            val surah = last?.let { QuranMeta.surah(it.surah).name } ?: "Nothing played yet"
            val detail = last?.let { "${Reciters.byId(it.reciterId).shortName} · ${TimeFormat.media(it.positionMs)}" }
            WidgetFrame(colors, openMihraya(Destinations.PLAYER)) {
                when {
                    inner.width < 130.dp && inner.height < 100.dp -> Box(
                        GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        WidgetPill(label, colors, toggle, GlanceModifier.fillMaxWidth())
                    }
                    inner.height < 130.dp -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Column(GlanceModifier.defaultWeight()) {
                            if (inner.height >= 70.dp) {
                                WidgetTitle(if (playing) "Playing" else "Resume", colors)
                                Spacer(GlanceModifier.height(4.dp))
                            }
                            Text(surah, style = colors.style(if (inner.height >= 70.dp) 16 else 14, FontWeight.Bold), maxLines = 1)
                            if (detail != null && (inner.height >= 70.dp || inner.width >= 200.dp)) {
                                Text(detail, style = colors.style(12, color = colors.muted), maxLines = 1)
                            }
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        WidgetPill(label, colors, toggle)
                    }
                    else -> {
                        val large = inner.width >= 200.dp && inner.height >= 150.dp
                        WidgetTitle(if (playing) "Playing" else "Resume", colors)
                        Spacer(GlanceModifier.defaultWeight())
                        Text(surah, style = colors.style(if (large) 22 else 18, FontWeight.Bold), maxLines = 2)
                        if (detail != null) Text(detail, style = colors.style(13, color = colors.muted), maxLines = 1)
                        Spacer(GlanceModifier.defaultWeight())
                        Row(GlanceModifier.fillMaxWidth()) {
                            if (inner.width >= 200.dp) Spacer(GlanceModifier.defaultWeight())
                            WidgetPill(label, colors, toggle, if (inner.width >= 200.dp) GlanceModifier else GlanceModifier.defaultWeight())
                        }
                    }
                }
            }
        }
    }
}

class TasbihWidget : GlanceAppWidget() {
    override val sizeMode = responsive(
        50 to 50, 110 to 50, 180 to 50, 110 to 100, 180 to 100, 110 to 160, 160 to 160, 160 to 240, 250 to 180,
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = appWidgetId(context, id)
        provideWidget({
            val library = runCatching { context.appContainer.library.current() }.getOrNull()
            Triple(
                WidgetPrefs.read(context, widgetId),
                library?.tasbih,
                library?.let { it.dhikr(it.tasbih.presetId).transliteration },
            )
        }) { (options, tasbih, dhikr) ->
            val colors = options.theme.colors()
            WidgetFrame(colors) {
                if (tasbih == null) {
                    EmptyState("Tasbih", "Open Mihraya to start counting.", colors)
                } else {
                    TasbihContent(tasbih, dhikr, options, colors)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.TasbihContent(
    tasbih: com.mihraya.app.data.library.TasbihState,
    dhikr: String?,
    options: WidgetOptions,
    colors: WidgetColors,
) {
    val inner = innerSize()
    val done = tasbih.count == tasbih.target && tasbih.count > 0
    val countColor = if (done) colors.accent else colors.text
    val count = actionRunCallback<TasbihCountAction>()
    val resetLabel = if (options.confirmReset) "Confirm reset" else "Reset"
    val resetColor = if (options.confirmReset) colors.accent else colors.muted
    when {
        inner.width < 90.dp || (inner.height < 60.dp && inner.width < 150.dp) -> Box(
            GlanceModifier.fillMaxSize().clickable(count),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tasbih.count.toString(), style = colors.style(if (inner.height < 60.dp) 24 else 30, FontWeight.Bold, countColor), maxLines = 1)
                if (inner.height >= 50.dp) Text("of ${tasbih.target}", style = colors.style(11, color = colors.muted), maxLines = 1)
            }
        }
        inner.height < 60.dp -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                GlanceModifier.defaultWeight().fillMaxHeight().clickable(count),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tasbih.count.toString(), style = colors.style(28, FontWeight.Bold, countColor), maxLines = 1)
                Spacer(GlanceModifier.width(6.dp))
                Text("of ${tasbih.target}", style = colors.style(12, color = colors.muted), maxLines = 1)
            }
            SmallAction("Target ${tasbih.target}", colors.accent, colors, actionRunCallback<TasbihTargetAction>())
            SmallAction(resetLabel, resetColor, colors, actionRunCallback<TasbihResetAction>())
        }
        else -> {
            val countSize = when {
                inner.height >= 200.dp && inner.width >= 140.dp -> 60
                inner.height >= 130.dp -> 48
                else -> 36
            }
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WidgetTitle("Tasbih", colors)
                Spacer(GlanceModifier.defaultWeight())
                SmallAction("Open", colors.accent, colors, openMihraya(Destinations.TASBIH))
            }
            Box(
                GlanceModifier.fillMaxWidth().defaultWeight().clickable(count),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tasbih.count.toString(), style = colors.style(countSize, FontWeight.Bold, countColor), maxLines = 1)
                    Text(
                        if (done) "Round complete" else "of ${tasbih.target}",
                        style = colors.style(13, color = colors.muted),
                        maxLines = 1,
                    )
                    if (inner.height >= 150.dp && dhikr != null) {
                        Spacer(GlanceModifier.height(4.dp))
                        Text(dhikr, style = colors.style(13, color = colors.accent), maxLines = 1)
                    }
                }
            }
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SmallAction("Target ${tasbih.target}", colors.accent, colors, actionRunCallback<TasbihTargetAction>())
                Spacer(GlanceModifier.defaultWeight())
                SmallAction(resetLabel, resetColor, colors, actionRunCallback<TasbihResetAction>())
            }
        }
    }
}

@Composable
private fun SmallAction(text: String, color: ColorProvider, colors: WidgetColors, onClick: androidx.glance.action.Action) {
    Box(
        GlanceModifier.height(36.dp).padding(horizontal = 6.dp).clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = colors.style(12, FontWeight.Medium, color), maxLines = 1)
    }
}

@Composable
private fun Countdown(remainingMs: Long, colors: WidgetColors, textSize: Int) {
    if (remainingMs <= 0L) return
    val context = LocalContext.current
    AndroidRemoteViews(
        modifier = GlanceModifier.wrapContentWidth().height((textSize * 1.5f).dp),
        remoteViews = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
            if (!colors.system) {
                setTextColor(R.id.countdown, colors.text.getColor(context).toArgb())
            }
            setTextViewTextSize(R.id.countdown, TypedValue.COMPLEX_UNIT_SP, textSize.toFloat())
            setChronometerCountDown(R.id.countdown, true)
            setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + remainingMs, null, true)
        },
    )
}

private fun appWidgetId(context: Context, id: GlanceId): Int =
    androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(id)

/** Removes a deleted widget's Mihraya options along with the state Glance keeps for it. */
abstract class MihrayaWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetPrefs.clear(context, it) }
    }
}

class PrayerTimetableReceiver : MihrayaWidgetReceiver() {
    override val glanceAppWidget = PrayerTimetableWidget()
}

class NextPrayerReceiver : MihrayaWidgetReceiver() {
    override val glanceAppWidget = NextPrayerWidget()
}

class QiblaReceiver : MihrayaWidgetReceiver() {
    override val glanceAppWidget = QiblaWidget()
}

class VerseReceiver : MihrayaWidgetReceiver() {
    override val glanceAppWidget = VerseWidget()
}

class LastReadReceiver : MihrayaWidgetReceiver() {
    override val glanceAppWidget = LastReadWidget()
}

class BookmarksReceiver : MihrayaWidgetReceiver() {
    override val glanceAppWidget = BookmarksWidget()
}

class AudioReceiver : MihrayaWidgetReceiver() {
    override val glanceAppWidget = AudioWidget()
}

class TasbihReceiver : MihrayaWidgetReceiver() {
    override val glanceAppWidget = TasbihWidget()
}

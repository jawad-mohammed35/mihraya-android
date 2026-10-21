package com.mihraya.app.feature.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.components.CircleIconButton
import com.mihraya.app.core.designsystem.components.EmptyState
import com.mihraya.app.core.designsystem.components.GlassCard
import com.mihraya.app.core.designsystem.components.HairlineDivider
import com.mihraya.app.core.designsystem.components.SegmentedControl
import com.mihraya.app.core.designsystem.components.TopBar
import com.mihraya.app.core.designsystem.icons.MihrayaIcons
import com.mihraya.app.core.designsystem.theme.LocalUse24Hour
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.util.TimeFormat
import com.mihraya.app.data.prayer.AsrMethod
import com.mihraya.app.data.prayer.HijriDate
import com.mihraya.app.data.prayer.PrayerDay
import com.mihraya.app.data.prayer.PrayerSchedule
import com.mihraya.app.data.prayer.Salah
import com.mihraya.app.data.settings.AppSettings
import com.mihraya.app.ui.LocalAppContainer
import com.mihraya.app.ui.LocalBottomInset
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TimetableScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val settingsState by container.settings.state.collectAsStateWithLifecycle()
    val settings = settingsState ?: return
    val schedule = remember(settings.location, settings.prayer) { container.schedule(settings) }
    var mode by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Prayer times", onBack = onBack, subtitle = settings.location?.displayName)
        if (schedule == null) {
            EmptyState(MihrayaIcons.Home, "No location yet", "Set your location to see the prayer timetable.")
            return@Column
        }
        SegmentedControl(
            options = listOf("Day", "Month"),
            selectedIndex = mode,
            onSelect = { mode = it },
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .widthIn(max = 680.dp),
        )
        if (mode == 0) DayView(schedule, settings) else MonthView(schedule, settings)
    }
}

@Composable
private fun DayView(schedule: PrayerSchedule, settings: AppSettings) {
    val colors = Mihraya.colors
    val use24h = LocalUse24Hour.current
    val today = remember { schedule.now().toLocalDate() }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val date = today.plusDays(offset.toLong())
    val day = schedule.day(date)
    val hijri = HijriDate.of(date, settings.hijriOffset)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = LocalBottomInset.current + 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous day", { offset -= 1 })
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    TimeFormat.longDate(date.atStartOfDay(schedule.zone)),
                    style = Mihraya.type.titleSmall,
                    color = if (offset == 0) colors.accent else colors.textPrimary,
                )
                Text(hijri?.format() ?: "", style = Mihraya.type.caption, color = colors.textTertiary)
            }
            CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next day", { offset += 1 })
        }

        if (day == null) {
            EmptyState(MihrayaIcons.Home, "Unavailable", "Times can't be computed for this date with the current high-latitude rule.")
            return@Column
        }

        val rows = buildList {
            add(Triple("Fajr", day.fajr, true))
            add(Triple("Sunrise", day.sunrise, false))
            add(Triple("Ḍuḥā", day.duhaStart, false))
            add(Triple(Salah.Dhuhr.labelOn(date), day.dhuhr, true))
            add(Triple("Asr", day.asr, true))
            add(Triple("Maghrib", day.maghrib, true))
            add(Triple("Isha", day.isha, true))
            add(Triple("Midnight", day.midnight, false))
            add(Triple("Last third of the night", day.lastThird, false))
        }
        GlassCard(Modifier.widthIn(max = 680.dp).fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp)) {
            rows.forEachIndexed { index, (label, time, primary) ->
                if (index > 0) HairlineDivider(Modifier.padding(horizontal = 20.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style = if (primary) Mihraya.type.body.copy(fontWeight = FontWeight.SemiBold) else Mihraya.type.body,
                        color = if (primary) colors.textPrimary else colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        TimeFormat.full(time, use24h),
                        style = Mihraya.type.body.copy(fontFeatureSettings = "tnum", fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (primary) colors.textPrimary else colors.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        MethodFooter(settings)
    }
}

@Composable
private fun MonthView(schedule: PrayerSchedule, settings: AppSettings) {
    val colors = Mihraya.colors
    val use24h = LocalUse24Hour.current
    val today = remember { schedule.now().toLocalDate() }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val month = YearMonth.from(today).plusMonths(monthOffset.toLong())
    val days = remember(month, schedule) { (1..month.lengthOfMonth()).map { month.atDay(it) to schedule.day(month.atDay(it)) } }
    val listState = rememberLazyListState()
    val columns = listOf(Salah.Fajr, Salah.Sunrise, Salah.Dhuhr, Salah.Asr, Salah.Maghrib, Salah.Isha)
    val headers = listOf("Fajr", "Rise", "Dhuhr", "Asr", "Magh.", "Isha")

    LaunchedEffect(month) {
        if (monthOffset == 0) listState.scrollToItem((today.dayOfMonth - 3).coerceAtLeast(0))
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month", { monthOffset -= 1 })
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
                    style = Mihraya.type.titleSmall,
                    color = colors.textPrimary,
                )
                val startHijri = HijriDate.of(month.atDay(1), settings.hijriOffset)
                val endHijri = HijriDate.of(month.atEndOfMonth(), settings.hijriOffset)
                if (startHijri != null && endHijri != null) {
                    Text(
                        if (startHijri.month == endHijri.month) "${startHijri.monthName} ${startHijri.year}"
                        else "${startHijri.monthName} – ${endHijri.monthName} ${endHijri.year}",
                        style = Mihraya.type.caption,
                        color = colors.textTertiary,
                    )
                }
            }
            CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month", { monthOffset += 1 })
        }

        Row(
            Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text("Day", style = Mihraya.type.caption, color = colors.textTertiary, modifier = Modifier.width(52.dp))
            headers.forEach {
                Text(it, style = Mihraya.type.caption, color = colors.textTertiary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        HairlineDivider(Modifier.padding(horizontal = 16.dp).widthIn(max = 680.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = LocalBottomInset.current + 16.dp),
        ) {
            items(days, key = { it.first.toEpochDay() }) { (date, day) ->
                val isToday = date == today
                val isFriday = date.dayOfWeek == java.time.DayOfWeek.FRIDAY
                Row(
                    Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .then(if (isToday) Modifier.background(colors.accentSoft) else Modifier)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.width(52.dp)) {
                        Text(
                            "${date.dayOfMonth} ${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}",
                            style = Mihraya.type.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isToday) colors.accent else if (isFriday) colors.textPrimary else colors.textSecondary,
                        )
                        Text(
                            HijriDate.of(date, settings.hijriOffset)?.day?.toString() ?: "",
                            style = Mihraya.type.caption,
                            color = colors.textTertiary,
                        )
                    }
                    columns.forEach { salah ->
                        Text(
                            day?.let { compactTime(it[salah], use24h) } ?: "—",
                            style = Mihraya.type.caption.copy(fontFeatureSettings = "tnum", fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium),
                            color = if (isToday) colors.textPrimary else colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                Box(Modifier.padding(20.dp).widthIn(max = 680.dp)) { MethodFooter(settings) }
            }
        }
    }
}

private fun compactTime(time: ZonedDateTime, use24h: Boolean): String = TimeFormat.clock(time, use24h)

@Composable
private fun MethodFooter(settings: AppSettings) {
    val prayer = settings.prayer
    Text(
        buildString {
            append(prayer.method.title)
            append(" · ")
            append(prayer.method.summary)
            append(" · Asr ")
            append(if (prayer.asr == AsrMethod.Hanafi) "Ḥanafī" else "standard")
            if (!settings.use24Hour) append(" · times shown in 12-hour format")
        },
        style = Mihraya.type.caption,
        color = Mihraya.colors.textTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
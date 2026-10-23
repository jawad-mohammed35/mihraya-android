package com.mihraya.app.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.components.CircleIconButton
import com.mihraya.app.core.designsystem.components.GlassCard
import com.mihraya.app.core.designsystem.components.HairlineDivider
import com.mihraya.app.core.designsystem.components.PrimaryButton
import com.mihraya.app.core.designsystem.components.SecondaryButton
import com.mihraya.app.core.designsystem.components.SectionLabel
import com.mihraya.app.core.designsystem.icons.MihrayaIcons
import com.mihraya.app.core.designsystem.theme.ArabicFonts
import com.mihraya.app.core.designsystem.theme.LocalUse24Hour
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.util.TimeFormat
import com.mihraya.app.data.audio.Reciters
import com.mihraya.app.data.prayer.DayAction
import com.mihraya.app.data.prayer.DayNote
import com.mihraya.app.data.prayer.PrayerDay
import com.mihraya.app.data.prayer.PrayerSchedule
import com.mihraya.app.data.prayer.Salah
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.data.quran.Verse
import com.mihraya.app.data.quran.arabicDigits
import com.mihraya.app.data.settings.AlertMode
import com.mihraya.app.ui.LocalBottomInset
import com.mihraya.app.ui.mihrayaViewModel
import com.mihraya.app.ui.rememberLocationRequest
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
data class HomeActions(
    val openReader: (surah: Int, ayah: Int) -> Unit,
    val openTimetable: () -> Unit,
    val openLocation: () -> Unit,
    val openTasbih: () -> Unit,
    val openQibla: () -> Unit,
    val openListen: () -> Unit,
)

@Composable
fun HomeScreen(actions: HomeActions) {
    val vm = mihrayaViewModel { HomeViewModel(it) }
    val dashboard by vm.dashboard.collectAsStateWithLifecycle()
    val now = vm.now.collectAsStateWithLifecycle()
    val colors = Mihraya.colors
    val d = dashboard

    Box(Modifier.fillMaxSize()) {
        AmbientGlow()
        if (d == null) return@Box

        val item = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = LocalBottomInset.current + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Header(
                    name = d.settings.userName.trim(),
                    locationName = d.settings.location?.name,
                    hijri = d.hijri?.let { "${it.day} ${it.monthName}" },
                    hijriArabic = d.hijri?.let { "${it.day.arabicDigits()} ${it.monthArabic}" },
                    subtitle = buildString {
                        d.hijri?.let { append("${it.year} AH · ") }
                        append(TimeFormat.longDate(d.date.atStartOfDay(d.schedule?.zone ?: ZoneId.systemDefault())))
                    },
                    onLocation = actions.openLocation,
                    onQibla = actions.openQibla,
                    modifier = item,
                )
            }

            val schedule = d.schedule
            val today = d.today
            if (schedule == null || today == null) {
                item(key = "setup") {
                    LocationSetupCard(
                        hasLocation = d.settings.location != null,
                        onSearch = actions.openLocation,
                        modifier = item,
                    )
                }
            } else {
                item(key = "hero") {
                    NextPrayerCard(schedule = schedule, now = now, onClick = actions.openTimetable, modifier = item)
                }
                item(key = "today") {
                    TodayCard(
                        today = today,
                        schedule = schedule,
                        at = d.observedAt,
                        alertFor = { d.settings.alerts.modeFor(it) },
                        onAlertChange = vm::setAlert,
                        onTimetable = actions.openTimetable,
                        modifier = item,
                    )
                }
                d.night?.let { night ->
                    item(key = "sunnah") { SunnahRow(today = today, night = night, modifier = item) }
                }
            }

            d.note?.let { note ->
                item(key = "note") {
                    DayNoteCard(
                        note = note,
                        onAction = {
                            when (note.action) {
                                DayAction.ReadKahf -> actions.openReader(18, 1)
                                DayAction.OpenTasbih -> actions.openTasbih()
                                null -> Unit
                            }
                        },
                        modifier = item,
                    )
                }
            }

            item(key = "quick") {
                QuickAccess(dashboard = d, actions = actions, modifier = item)
            }

            d.verse?.let { verse ->
                item(key = "verse") {
                    VerseOfTheDay(
                        verse = verse,
                        arabicFont = ArabicFonts.quran(d.settings.reader.arabicFont, d.settings.reader.script),
                        onClick = { actions.openReader(verse.surah, verse.ayah) },
                        modifier = item,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientGlow() {
    val colors = Mihraya.colors
    Canvas(Modifier.fillMaxWidth().height(460.dp)) {
        drawRect(
            Brush.radialGradient(
                colors = listOf(colors.accent.copy(alpha = if (colors.isDark) 0.13f else 0.10f), colors.accent.copy(alpha = 0f)),
                center = Offset(size.width * 0.78f, -size.height * 0.05f),
                radius = size.width * 0.95f,
            ),
        )
    }
}

@Composable
private fun Header(
    name: String,
    locationName: String?,
    hijri: String?,
    hijriArabic: String?,
    subtitle: String,
    onLocation: () -> Unit,
    onQibla: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Mihraya.colors
    Column(modifier.statusBarsPadding().padding(top = 14.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onLocation)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.LocationOn, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    locationName ?: "Set location",
                    style = Mihraya.type.label.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
            }
            CircleIconButton(MihrayaIcons.Qibla, "Qibla direction", onQibla, tint = colors.accent)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            buildAnnotatedString {
                append("Assalamu Alaykum")
                if (name.isNotEmpty()) {
                    append(", ")
                    withStyle(SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.SemiBold)) { append(name) }
                }
            },
            style = Mihraya.type.body,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                hijri ?: TimeFormat.longDate(ZonedDateTime.now()),
                style = Mihraya.type.headline,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (hijriArabic != null) {
                Text(
                    hijriArabic,
                    style = Mihraya.type.title.copy(
                        fontFamily = ArabicFonts.Amiri,
                        fontWeight = FontWeight.Normal,
                        fontSize = 20.sp,
                        textDirection = TextDirection.Rtl,
                    ),
                    color = colors.accent,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = Mihraya.type.bodySmall, color = colors.textTertiary)
    }
}

@Composable
private fun LocationSetupCard(hasLocation: Boolean, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    val request = rememberLocationRequest()
    GlassCard(modifier, contentPadding = PaddingValues(24.dp)) {
        IconTile(MihrayaIcons.Home, size = 44)
        Spacer(Modifier.height(18.dp))
        Text(
            if (hasLocation) "Prayer times unavailable here" else "Set your location",
            style = Mihraya.type.title,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasLocation) {
                "The sun doesn't rise or set far enough today for this method. Try another high-latitude rule in Prayer settings."
            } else {
                "Prayer times and the Qibla are calculated on your device from your approximate location. Nothing leaves your phone."
            },
            style = Mihraya.type.bodySmall,
            color = colors.textSecondary,
        )
        request.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = Mihraya.type.bodySmall, color = colors.danger)
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                "Use my location",
                onClick = request::request,
                icon = Icons.Rounded.MyLocation,
                loading = request.loading,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton("Search", onClick = onSearch, icon = Icons.Rounded.Search)
        }
    }
}

@Composable
private fun NextPrayerCard(
    schedule: PrayerSchedule,
    now: State<Instant>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Mihraya.colors
    val use24h = LocalUse24Hour.current
    val zoned = schedule.now(now.value)
    val next = schedule.next(zoned) ?: return
    val previous = schedule.previous(zoned)
    val nextEvent = schedule.next(zoned, includeSunrise = true)
    val remaining = Duration.between(zoned, next.time)
    val fraction = previous?.let {
        val total = Duration.between(it.time, next.time).seconds.toFloat()
        if (total > 0) Duration.between(it.time, zoned).seconds / total else 0f
    } ?: 0f

    GlassCard(modifier, onClick = onClick, contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("NEXT PRAYER", style = Mihraya.type.overline, color = colors.accent, modifier = Modifier.weight(1f))
            Text(
                TimeFormat.full(next.time, use24h),
                style = Mihraya.type.label.copy(fontFeatureSettings = "tnum"),
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                next.salah.labelOn(next.time.toLocalDate()),
                style = Mihraya.type.headline.copy(fontSize = 34.sp, lineHeight = 40.sp),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                next.salah.arabicOn(next.time.toLocalDate()),
                style = Mihraya.type.title.copy(
                    fontFamily = ArabicFonts.Amiri,
                    fontWeight = FontWeight.Normal,
                    fontSize = 28.sp,
                    lineHeight = 40.sp,
                    textDirection = TextDirection.Rtl,
                ),
                color = colors.accent,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            TimeFormat.countdown(remaining),
            style = Mihraya.type.display,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(16.dp))
        ProgressLine(fraction)
        Spacer(Modifier.height(10.dp))
        Row {
            previous?.let {
                LabeledValue(
                    it.salah.labelOn(it.time.toLocalDate()),
                    TimeFormat.full(it.time, use24h),
                    Modifier.weight(1f),
                )
            } ?: Spacer(Modifier.weight(1f))
            LabeledValue(
                if (nextEvent?.salah == Salah.Sunrise) "Sunrise" else "Remaining",
                if (nextEvent?.salah == Salah.Sunrise) {
                    "Fajr ends in ${TimeFormat.humanize(Duration.between(zoned, nextEvent.time))}"
                } else {
                    TimeFormat.humanize(remaining)
                },
                alignEnd = true,
            )
        }
    }
}

@Composable
private fun TodayCard(
    today: PrayerDay,
    schedule: PrayerSchedule,
    at: Instant,
    alertFor: (Salah) -> AlertMode,
    onAlertChange: (Salah, AlertMode) -> Unit,
    onTimetable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Mihraya.colors
    val use24h = LocalUse24Hour.current
    val zoned = schedule.now(at)
    val current = schedule.previous(zoned, includeSunrise = true)?.takeIf { it.time.toLocalDate() == today.date }?.salah
    val nextSalah = schedule.next(zoned)?.salah

    GlassCard(modifier, contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp)) {
        Row(
            Modifier.padding(start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Today", style = Mihraya.type.title, color = colors.textPrimary, modifier = Modifier.weight(1f))
            Row(
                Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onTimetable)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Timetable", style = Mihraya.type.label, color = colors.accent)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.accent, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        today.moments.forEachIndexed { index, moment ->
            if (index > 0) HairlineDivider(Modifier.padding(horizontal = 20.dp))
            val isCurrent = moment.salah == current && moment.salah.isPrayer
            val isPast = moment.time.isBefore(zoned) && !isCurrent
            val isNext = moment.salah == nextSalah && moment.time.toLocalDate() == today.date
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .padding(start = 20.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .then(
                            if (isCurrent || isNext) Modifier.background(if (isCurrent) colors.accent else colors.accent.copy(alpha = 0.4f))
                            else Modifier,
                        ),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    moment.salah.labelOn(today.date),
                    style = Mihraya.type.body.copy(fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium),
                    color = when {
                        isCurrent -> colors.accent
                        isPast -> colors.textTertiary
                        else -> colors.textPrimary
                    },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    TimeFormat.full(moment.time, use24h),
                    style = Mihraya.type.body.copy(fontFeatureSettings = "tnum"),
                    color = if (isPast) colors.textTertiary else colors.textSecondary,
                )
                Spacer(Modifier.width(6.dp))
                PrayerAlertButton(
                    salah = moment.salah,
                    mode = alertFor(moment.salah),
                    onChange = { onAlertChange(moment.salah, it) },
                )
            }
        }
    }
}

@Composable
private fun SunnahRow(today: PrayerDay, night: PrayerDay, modifier: Modifier = Modifier) {
    val use24h = LocalUse24Hour.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SunnahTile("Ḍuḥā", TimeFormat.full(today.duhaStart, use24h), Icons.Rounded.WbSunny, Modifier.weight(1f))
        SunnahTile("Midnight", TimeFormat.full(night.midnight, use24h), Icons.Rounded.Bedtime, Modifier.weight(1f))
        SunnahTile("Last third", TimeFormat.full(night.lastThird, use24h), Icons.Rounded.Nightlight, Modifier.weight(1f))
    }
}

@Composable
private fun SunnahTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    GlassCard(modifier, shape = RoundedCornerShape(22.dp), contentPadding = PaddingValues(14.dp)) {
        Icon(icon, null, tint = colors.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(12.dp))
        Text(label, style = Mihraya.type.caption, color = colors.textTertiary)
        Text(
            value,
            style = Mihraya.type.label.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            color = colors.textPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun DayNoteCard(note: DayNote, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    GlassCard(
        modifier,
        fill = colors.accentSoft,
        onClick = if (note.action != null) onAction else null,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(MihrayaIcons.Crescent, null, tint = colors.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(note.title, style = Mihraya.type.titleSmall, color = colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(note.detail, style = Mihraya.type.bodySmall, color = colors.textSecondary)
                if (note.action != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when (note.action) {
                            DayAction.ReadKahf -> "Read Sūrat al-Kahf"
                            DayAction.OpenTasbih -> "Open Tasbīḥ"
                        },
                        style = Mihraya.type.label.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAccess(dashboard: HomeDashboard, actions: HomeActions, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    val lastRead = dashboard.lastRead
    val readTitle = lastRead?.let { "${QuranMeta.surah(it.surah).name} · ${it.surah}:${it.ayah}" } ?: "Al-Fātiḥah"
    val readProgress = lastRead?.let { "Juzʾ ${QuranMeta.juzOf(it.surah, it.ayah)} of ${QuranMeta.JUZ_COUNT}" } ?: "Begin your journey"
    val lastPlayed = dashboard.lastPlayed
    val listenTitle = lastPlayed?.let { QuranMeta.surah(it.surah).name } ?: "Recitations"
    val listenSubtitle = lastPlayed?.let { Reciters.byId(it.reciterId).shortName } ?: "7 renowned reciters"

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Your day")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(
                icon = MihrayaIcons.Quran,
                label = if (lastRead != null) "Continue reading" else "Start reading",
                title = readTitle,
                subtitle = readProgress,
                onClick = { actions.openReader(lastRead?.surah ?: 1, lastRead?.ayah ?: 1) },
                modifier = Modifier.weight(1f),
            )
            QuickTile(
                icon = MihrayaIcons.Listen,
                label = if (lastPlayed != null) "Continue listening" else "Listen",
                title = listenTitle,
                subtitle = listenSubtitle,
                onClick = actions.openListen,
                modifier = Modifier.weight(1f),
            )
        }
        GlassCard(onClick = actions.openTasbih, shape = RoundedCornerShape(24.dp), contentPadding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(MihrayaIcons.Tasbih)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tasbīḥ", style = Mihraya.type.caption, color = colors.textTertiary)
                    Text(
                        if (dashboard.tasbihToday > 0) "${dashboard.tasbihToday} today" else "Remember Allah",
                        style = Mihraya.type.titleSmall,
                        color = colors.textPrimary,
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Mihraya.colors
    GlassCard(modifier.height(150.dp), onClick = onClick, shape = RoundedCornerShape(24.dp), contentPadding = PaddingValues(16.dp)) {
        IconTile(icon)
        Spacer(Modifier.weight(1f))
        Text(label, style = Mihraya.type.caption, color = colors.textTertiary, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            title,
            style = Mihraya.type.titleSmall,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            subtitle,
            style = Mihraya.type.caption,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VerseOfTheDay(verse: Verse, arabicFont: FontFamily, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    val surah = remember(verse.surah) { QuranMeta.surah(verse.surah) }
    GlassCard(modifier, onClick = onClick, contentPadding = PaddingValues(22.dp)) {
        Text("VERSE OF THE DAY", style = Mihraya.type.overline, color = colors.accent)
        Spacer(Modifier.height(16.dp))
        Text(
            verse.arabic,
            style = Mihraya.type.body.copy(
                fontFamily = arabicFont,
                fontSize = 24.sp,
                lineHeight = 46.sp,
                textDirection = TextDirection.Rtl,
                textAlign = TextAlign.Right,
            ),
            color = colors.textPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        verse.translations.firstOrNull()?.let { translation ->
            Spacer(Modifier.height(14.dp))
            Text(
                translation.text,
                style = Mihraya.type.body.copy(
                    fontFamily = ArabicFonts.translation(translation.translation),
                    textDirection = if (translation.translation.isRtl) TextDirection.Rtl else TextDirection.Ltr,
                ),
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${surah.name} · ${verse.surah}:${verse.ayah}",
                style = Mihraya.type.label,
                color = colors.textTertiary,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

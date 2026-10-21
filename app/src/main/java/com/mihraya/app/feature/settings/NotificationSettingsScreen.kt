package com.mihraya.app.feature.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessAlarm
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.components.CircleIconButton
import com.mihraya.app.core.designsystem.components.GlassCard
import com.mihraya.app.core.designsystem.components.NoticeCard
import com.mihraya.app.core.designsystem.components.OptionSheet
import com.mihraya.app.core.designsystem.components.Pill
import com.mihraya.app.core.designsystem.components.RadioDot
import com.mihraya.app.core.designsystem.components.SecondaryButton
import com.mihraya.app.core.designsystem.components.SectionLabel
import com.mihraya.app.core.designsystem.components.SettingsDivider
import com.mihraya.app.core.designsystem.components.SettingsGroup
import com.mihraya.app.core.designsystem.components.SettingsStepperRow
import com.mihraya.app.core.designsystem.components.SettingsSwitchRow
import com.mihraya.app.core.designsystem.components.SettingsValueRow
import com.mihraya.app.core.designsystem.components.SheetOption
import com.mihraya.app.core.designsystem.theme.LocalUse24Hour
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.util.TimeFormat
import com.mihraya.app.data.prayer.Salah
import com.mihraya.app.data.settings.AdhanSound
import com.mihraya.app.data.settings.AlertMode
import com.mihraya.app.data.settings.AlertPreferences
import com.mihraya.app.data.settings.AppSettings
import com.mihraya.app.feature.home.alertOptionsFor
import com.mihraya.app.feature.home.icon
import com.mihraya.app.notifications.AdhanService
import com.mihraya.app.notifications.AlarmEvent
import com.mihraya.app.notifications.AlarmKind
import com.mihraya.app.notifications.PrayerNotifier
import com.mihraya.app.notifications.zone
import com.mihraya.app.ui.LocalAppContainer
import java.time.Duration
import java.time.Instant

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val settingsState by container.settings.state.collectAsStateWithLifecycle()
    val settings = settingsState ?: return
    val alerts = settings.alerts
    val colors = Mihraya.colors
    val use24h = LocalUse24Hour.current

    var notificationsEnabled by remember { mutableStateOf(true) }
    var canScheduleExact by remember { mutableStateOf(true) }
    var permissionDenied by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Salah?>(null) }
    val preview = rememberSoundPreview()

    LifecycleResumeEffect(Unit) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        canScheduleExact = container.alarms.canScheduleExact()
        onPauseOrDispose { preview.stop() }
    }

    fun update(transform: (AlertPreferences) -> AlertPreferences) =
        container.settings.edit { it.copy(alerts = transform(it.alerts)) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        permissionDenied = !granted
    }

    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val resolver = context.contentResolver
            runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            alerts.customSoundUri?.takeIf { it != uri.toString() }?.let { old ->
                runCatching { resolver.releasePersistableUriPermission(Uri.parse(old), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }
            val name = runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull()
            update { it.copy(adhanSound = AdhanSound.Custom, customSoundUri = uri.toString(), customSoundName = name ?: "Custom audio") }
        }
    }

    SettingsScaffold("Notifications & Adhan", onBack) {
        if (!notificationsEnabled) {
            val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionDenied &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            NoticeCard(
                icon = Icons.Rounded.NotificationsOff,
                title = "Notifications are off",
                message = "Mihraya can't alert you at prayer times until notifications are allowed.",
                actionText = if (canAsk) "Allow notifications" else "Open notification settings",
                onAction = {
                    if (canAsk) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else context.openSettings(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                },
            )
        }
        if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            NoticeCard(
                icon = Icons.Rounded.AccessAlarm,
                title = "Allow exact alarms",
                message = "Without this, Android may deliver prayer alerts several minutes late to save battery.",
                actionText = "Open alarm settings",
                onAction = { context.openSettings(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri = true) },
            )
        }

        SettingsGroup("Prayer alerts", footer = "Tap a prayer to choose how it alerts you. Adhan plays through the alarm volume.") {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("All five prayers", style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                val shared = Salah.entries.filter { it.isPrayer }.map { alerts.modeFor(it) }.distinct().singleOrNull()
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AlertMode.entries.forEach { mode ->
                        Pill(
                            text = mode.label,
                            selected = mode == shared,
                            leading = mode.icon(),
                            onClick = {
                                update { a -> a.copy(modes = a.modes + Salah.entries.filter { it.isPrayer }.associateWith { mode }) }
                            },
                        )
                    }
                }
            }
            val today = container.schedule(settings)?.let { it.day(it.now().toLocalDate()) }
            Salah.entries.forEach { salah ->
                SettingsDivider()
                val mode = alerts.modeFor(salah)
                SettingsValueRow(
                    title = salah.label,
                    value = mode.label,
                    onClick = { editing = salah },
                    subtitle = today?.let { "Today at ${TimeFormat.full(it[salah], use24h)}" },
                )
            }
        }

        SettingsGroup("Adhan sound", footer = "Recordings from Wikimedia Commons, shared under CC BY-SA 4.0.") {
            AdhanSound.entries.forEachIndexed { index, sound ->
                if (index > 0) SettingsDivider()
                val selected = alerts.adhanSound == sound
                val hasCustom = alerts.customSoundUri != null
                SoundRow(
                    title = sound.label,
                    subtitle = if (sound == AdhanSound.Custom) alerts.customSoundName ?: sound.credit else sound.credit,
                    selected = selected,
                    playing = preview.playing == sound,
                    canPreview = sound != AdhanSound.Custom || hasCustom,
                    onSelect = {
                        if (sound == AdhanSound.Custom && !hasCustom) soundPicker.launch(arrayOf("audio/*"))
                        else update { it.copy(adhanSound = sound) }
                    },
                    onPreview = {
                        if (preview.playing == sound) preview.stop()
                        else preview.play(sound, AdhanService.soundUri(context, alerts.copy(adhanSound = sound)))
                    },
                )
            }
            if (alerts.customSoundUri != null) {
                SettingsDivider()
                SettingsValueRow(
                    title = "Choose a different file",
                    value = "",
                    onClick = { soundPicker.launch(arrayOf("audio/*")) },
                    icon = Icons.Rounded.LibraryMusic,
                )
            }
        }

        SettingsGroup("Reminders") {
            SettingsStepperRow(
                title = "Before each prayer",
                value = alerts.reminderMinutes / 5,
                onValueChange = { v -> update { it.copy(reminderMinutes = v * 5) } },
                range = 0..6,
                subtitle = "A heads-up before the time begins",
                format = { if (it == 0) "Off" else "${it * 5} min" },
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = "Last third of the night",
                checked = alerts.qiyamReminder,
                onCheckedChange = { v -> update { it.copy(qiyamReminder = v) } },
                subtitle = "A gentle nudge for Qiyām al-Layl",
            )
        }

        SettingsGroup("Behaviour") {
            SettingsSwitchRow(
                title = "Respect silent mode",
                checked = alerts.respectSilentMode,
                onCheckedChange = { v -> update { it.copy(respectSilentMode = v) } },
                subtitle = "When your phone is on silent or vibrate, show a quiet notification instead of playing the Adhan",
            )
        }

        UpcomingAlerts(settings, use24h)

        SecondaryButton(
            text = "Send a test alert",
            icon = Icons.Rounded.NotificationsActive,
            onClick = {
                preview.stop()
                context.sendTestAlert(settings)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    editing?.let { salah ->
        OptionSheet(
            title = "${salah.label} alert",
            options = alertOptionsFor(salah).map { SheetOption(it, it.label, it.description) },
            selected = alerts.modeFor(salah),
            onSelect = { mode -> update { it.copy(modes = it.modes + (salah to mode)) } },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun SoundRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    playing: Boolean,
    canPreview: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
) {
    val colors = Mihraya.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .heightIn(min = 60.dp)
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioDot(selected)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
            Text(subtitle, style = Mihraya.type.caption, color = colors.textTertiary, maxLines = 1)
        }
        if (canPreview) {
            CircleIconButton(
                icon = if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = if (playing) "Stop preview" else "Preview",
                onClick = onPreview,
                size = 38.dp,
                iconSize = 18.dp,
                tint = if (playing) colors.accent else colors.textSecondary,
            )
        }
    }
}

@Composable
private fun UpcomingAlerts(settings: AppSettings, use24h: Boolean) {
    val container = LocalAppContainer.current
    val colors = Mihraya.colors
    val events = remember(settings) {
        val now = Instant.now()
        container.alarms.events(settings, now, now.plus(Duration.ofHours(24))).take(4)
    }
    if (settings.location == null) return
    Column {
        SectionLabel("Next 24 hours")
        Spacer(Modifier.height(10.dp))
        GlassCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
            if (events.isEmpty()) {
                Text(
                    "No alerts are scheduled. Turn on an alert for at least one prayer.",
                    style = Mihraya.type.bodySmall,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            events.forEach { event ->
                val zoned = event.at.atZone(settings.zone())
                val title = when (event.kind) {
                    AlarmKind.Prayer -> event.salah?.labelOn(zoned.toLocalDate()).orEmpty()
                    AlarmKind.Reminder -> "${event.salah?.label} reminder"
                    AlarmKind.Qiyam -> "Last third of the night"
                }
                val detail = when (event.kind) {
                    AlarmKind.Prayer -> event.salah?.let { settings.alerts.modeFor(it).label }.orEmpty()
                    AlarmKind.Reminder -> "${settings.alerts.reminderMinutes} min before"
                    AlarmKind.Qiyam -> "Reminder"
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                        Text(detail, style = Mihraya.type.caption, color = colors.textTertiary)
                    }
                    Text(
                        TimeFormat.full(zoned, use24h),
                        style = Mihraya.type.label.copy(fontFeatureSettings = "tnum"),
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

/** Plays a short preview of an Adhan sound through the media stream. */
@Stable
private class SoundPreview(private val context: Context) {
    var playing by mutableStateOf<AdhanSound?>(null)
        private set
    private var player: MediaPlayer? = null

    fun play(sound: AdhanSound, uri: Uri) {
        stop()
        val mp = MediaPlayer()
        player = mp
        playing = sound
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            mp.setDataSource(context, uri)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { stop() }
            mp.setOnErrorListener { _, _, _ ->
                stop()
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            stop()
        }
    }

    fun stop() {
        player?.run {
            runCatching { if (isPlaying) stop() }
            release()
        }
        player = null
        playing = null
    }
}

@Composable
private fun rememberSoundPreview(): SoundPreview {
    val context = LocalContext.current
    val preview = remember { SoundPreview(context.applicationContext) }
    DisposableEffect(preview) { onDispose { preview.stop() } }
    return preview
}

private fun Context.sendTestAlert(settings: AppSettings) {
    val salah = Salah.Dhuhr
    val mode = settings.alerts.modeFor(salah).takeIf { it != AlertMode.Off } ?: AlertMode.Adhan
    val testSettings = settings.copy(alerts = settings.alerts.copy(modes = settings.alerts.modes + (salah to mode)))
    PrayerNotifier.deliver(this, testSettings, AlarmEvent(AlarmKind.Prayer, salah, Instant.now()))
}

internal fun Context.openSettings(action: String, packageUri: Boolean = false) {
    val intent = Intent(action).apply {
        if (packageUri) data = Uri.parse("package:$packageName")
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

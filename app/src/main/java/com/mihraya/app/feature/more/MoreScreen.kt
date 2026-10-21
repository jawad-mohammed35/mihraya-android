package com.mihraya.app.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.BuildConfig
import com.mihraya.app.core.designsystem.components.GlassCard
import com.mihraya.app.core.designsystem.components.OptionSheet
import com.mihraya.app.core.designsystem.components.ScreenHeader
import com.mihraya.app.core.designsystem.components.SettingsDivider
import com.mihraya.app.core.designsystem.components.SettingsGroup
import com.mihraya.app.core.designsystem.components.SettingsSwitchRow
import com.mihraya.app.core.designsystem.components.SettingsValueRow
import com.mihraya.app.core.designsystem.components.SheetOption
import com.mihraya.app.core.designsystem.icons.MihrayaIcons
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.data.audio.Reciters
import com.mihraya.app.feature.home.IconTile
import com.mihraya.app.ui.AboutRoute
import com.mihraya.app.ui.AppearanceRoute
import com.mihraya.app.ui.DownloadsRoute
import com.mihraya.app.ui.LocalAppContainer
import com.mihraya.app.ui.LocalBottomInset
import com.mihraya.app.ui.LocationRoute
import com.mihraya.app.ui.NotificationSettingsRoute
import com.mihraya.app.ui.PrayerSettingsRoute
import com.mihraya.app.ui.ReadingSettingsRoute
import com.mihraya.app.ui.TasbihRoute
import com.mihraya.app.ui.TimetableRoute
import com.mihraya.app.ui.TranslationsRoute

@Composable
fun MoreScreen(onNavigate: (Any) -> Unit) {
    val container = LocalAppContainer.current
    val settings by container.settings.state.collectAsStateWithLifecycle()
    val installed by container.quran.installedTranslations.collectAsStateWithLifecycle()
    val s = settings ?: return
    val colors = Mihraya.colors
    var pickReciter by remember { mutableStateOf(false) }
    val item = Modifier.widthIn(max = 640.dp).fillMaxWidth()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = LocalBottomInset.current + 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { ScreenHeader("More", modifier = item) }

        item {
            GlassCard(item, onClick = { onNavigate(TasbihRoute) }, fill = colors.accentSoft) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(MihrayaIcons.Tasbih, size = 46)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Tasbīḥ", style = Mihraya.type.title, color = colors.textPrimary)
                        Text("Count your dhikr with gentle haptics", style = Mihraya.type.bodySmall, color = colors.textSecondary)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.accent)
                }
            }
        }

        item {
            SettingsGroup("Prayer", item) {
                SettingsValueRow("Calculation", s.prayer.method.title, { onNavigate(PrayerSettingsRoute) }, icon = Icons.Rounded.Schedule)
                SettingsDivider(indent = true)
                SettingsValueRow("Notifications & Adhan", "", { onNavigate(NotificationSettingsRoute) }, icon = Icons.Rounded.NotificationsActive)
                SettingsDivider(indent = true)
                SettingsValueRow("Location", s.location?.name ?: "Not set", { onNavigate(LocationRoute) }, icon = Icons.Rounded.LocationOn)
                SettingsDivider(indent = true)
                SettingsValueRow("Timetable", "Day & month", { onNavigate(TimetableRoute) }, icon = Icons.Rounded.CalendarMonth)
            }
        }

        item {
            SettingsGroup("Quran", item) {
                SettingsValueRow("Reading", s.reader.script.label, { onNavigate(ReadingSettingsRoute) }, icon = Icons.Rounded.TextFields)
                SettingsDivider(indent = true)
                SettingsValueRow("Translations", "${installed.size} installed", { onNavigate(TranslationsRoute) }, icon = Icons.Rounded.Translate)
                SettingsDivider(indent = true)
                SettingsValueRow("Reciter", Reciters.byId(s.reciterId).shortName, { pickReciter = true }, icon = Icons.Rounded.RecordVoiceOver)
                SettingsDivider(indent = true)
                SettingsValueRow("Downloads", "Offline audio", { onNavigate(DownloadsRoute) }, icon = Icons.Rounded.DownloadForOffline)
            }
        }

        item {
            SettingsGroup("Appearance", item) {
                SettingsValueRow("Theme & accent", "${s.theme.label} · ${s.accent.label}", { onNavigate(AppearanceRoute) }, icon = Icons.Rounded.Palette)
                SettingsDivider(indent = true)
                SettingsSwitchRow(
                    "24-hour time",
                    s.use24Hour,
                    { v -> container.settings.edit { it.copy(use24Hour = v) } },
                    icon = Icons.Rounded.Schedule,
                )
                SettingsDivider(indent = true)
                SettingsSwitchRow(
                    "Haptic feedback",
                    s.haptics,
                    { v -> container.settings.edit { it.copy(haptics = v) } },
                    icon = Icons.Rounded.Vibration,
                )
            }
        }

        item {
            SettingsGroup(null, item) {
                SettingsValueRow("About, sources & privacy", "", { onNavigate(AboutRoute) }, icon = Icons.Rounded.Info)
            }
        }

        item {
            Column(item.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(MihrayaIcons.Home, null, tint = colors.accent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text("Mihraya", style = Mihraya.type.titleSmall, color = colors.textSecondary)
                Text(
                    "Version ${BuildConfig.VERSION_NAME}",
                    style = Mihraya.type.caption,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (pickReciter) {
        OptionSheet(
            title = "Preferred reciter",
            options = Reciters.all.map { SheetOption(it.id, it.name, "${it.style} · ${it.origin}") },
            selected = s.reciterId,
            onSelect = { id -> container.settings.edit { it.copy(reciterId = id) } },
            onDismiss = { pickReciter = false },
        )
    }
}

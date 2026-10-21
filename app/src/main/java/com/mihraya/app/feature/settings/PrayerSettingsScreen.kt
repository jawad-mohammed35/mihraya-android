package com.mihraya.app.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.components.GlassCard
import com.mihraya.app.core.designsystem.components.HairlineDivider
import com.mihraya.app.core.designsystem.components.MihrayaSlider
import com.mihraya.app.core.designsystem.components.OptionSheet
import com.mihraya.app.core.designsystem.components.SectionLabel
import com.mihraya.app.core.designsystem.components.SegmentedControl
import com.mihraya.app.core.designsystem.components.SettingsDivider
import com.mihraya.app.core.designsystem.components.SettingsGroup
import com.mihraya.app.core.designsystem.components.SettingsStepperRow
import com.mihraya.app.core.designsystem.components.SettingsValueRow
import com.mihraya.app.core.designsystem.components.SheetOption
import com.mihraya.app.core.designsystem.theme.LocalUse24Hour
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.util.TimeFormat
import com.mihraya.app.data.prayer.AsrMethod
import com.mihraya.app.data.prayer.CalcMethod
import com.mihraya.app.data.prayer.HighLatitude
import com.mihraya.app.data.prayer.HijriDate
import com.mihraya.app.data.prayer.Salah
import com.mihraya.app.data.settings.PrayerPreferences
import com.mihraya.app.ui.LocalAppContainer
import java.util.Locale

@Composable
fun PrayerSettingsScreen(onBack: () -> Unit, onLocation: () -> Unit) {
    val container = LocalAppContainer.current
    val settingsState by container.settings.state.collectAsStateWithLifecycle()
    val settings = settingsState ?: return
    val prefs = settings.prayer
    val colors = Mihraya.colors
    val use24h = LocalUse24Hour.current
    var sheet by remember { mutableStateOf<String?>(null) }

    fun update(transform: (PrayerPreferences) -> PrayerPreferences) =
        container.settings.edit { it.copy(prayer = transform(it.prayer)) }

    SettingsScaffold("Prayer calculation", onBack) {
        SettingsGroup("Location") {
            SettingsValueRow(
                "Location",
                settings.location?.name ?: "Not set",
                onLocation,
                subtitle = settings.location?.let { "Time zone ${it.timeZone}" },
                icon = Icons.Rounded.LocationOn,
            )
        }

        SettingsGroup("Method", footer = if (!prefs.methodSetByUser) "Chosen automatically for your region. Pick one to override." else null) {
            SettingsValueRow("Calculation method", prefs.method.title, { sheet = "method" }, subtitle = prefs.method.summary)
            SettingsDivider()
            Column(Modifier.padding(16.dp)) {
                Text("Asr", style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                SegmentedControl(
                    options = AsrMethod.entries.map { it.title },
                    selectedIndex = prefs.asr.ordinal,
                    onSelect = { i -> update { it.copy(asr = AsrMethod.entries[i], asrSetByUser = true) } },
                )
                Spacer(Modifier.height(6.dp))
                Text(prefs.asr.summary, style = Mihraya.type.caption, color = colors.textTertiary)
            }
            SettingsDivider()
            SettingsValueRow("High-latitude rule", prefs.highLatitude.title, { sheet = "highLat" }, subtitle = prefs.highLatitude.summary)
        }

        if (prefs.method == CalcMethod.Custom) {
            SettingsGroup("Custom angles") {
                AngleSlider("Fajr angle", prefs.customFajrAngle.toFloat(), 10f..20f) { v -> update { it.copy(customFajrAngle = v.toDouble()) } }
                HairlineDivider(Modifier.padding(horizontal = 16.dp))
                Column(Modifier.padding(16.dp)) {
                    Text("Isha", style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                    Spacer(Modifier.height(10.dp))
                    SegmentedControl(
                        options = listOf("By angle", "Minutes after Maghrib"),
                        selectedIndex = if (prefs.customIshaMinutes > 0) 1 else 0,
                        onSelect = { i -> update { it.copy(customIshaMinutes = if (i == 1) 90 else 0) } },
                    )
                }
                if (prefs.customIshaMinutes > 0) {
                    SettingsStepperRow(
                        "Isha interval",
                        prefs.customIshaMinutes / 5,
                        { v -> update { it.copy(customIshaMinutes = v * 5) } },
                        range = 12..36,
                        format = { "${it * 5} min" },
                    )
                } else {
                    AngleSlider("Isha angle", prefs.customIshaAngle.toFloat(), 10f..20f) { v -> update { it.copy(customIshaAngle = v.toDouble()) } }
                }
            }
        }

        SettingsGroup("Manual adjustments", footer = "Shift any time by a few minutes to match your local mosque.") {
            Salah.entries.forEachIndexed { index, salah ->
                if (index > 0) SettingsDivider()
                SettingsStepperRow(
                    salah.label,
                    prefs.offset(salah),
                    { v -> update { it.copy(offsets = it.offsets + (salah to v)) } },
                    range = -30..30,
                    format = { if (it == 0) "0 min" else String.format(Locale.US, "%+d min", it) },
                )
            }
        }

        SettingsGroup("Hijri date", footer = "Adjust if your community's moon sighting differs from the Umm al-Qura calendar.") {
            SettingsStepperRow(
                "Day adjustment",
                settings.hijriOffset,
                { v -> container.settings.edit { it.copy(hijriOffset = v) } },
                range = -2..2,
                subtitle = HijriDate.of(java.time.LocalDate.now(), settings.hijriOffset)?.format(),
                format = { if (it == 0) "0" else String.format(Locale.US, "%+d", it) },
            )
        }

        val schedule = container.schedule(settings)
        val today = schedule?.let { it.day(it.now().toLocalDate()) }
        if (today != null) {
            Column {
                SectionLabel("Today with these settings")
                Spacer(Modifier.height(10.dp))
                GlassCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Salah.entries.forEach { salah ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(salah.label.take(7), style = Mihraya.type.caption, color = colors.textTertiary, maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    TimeFormat.clock(today[salah], use24h),
                                    style = Mihraya.type.label.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    when (sheet) {
        "method" -> OptionSheet(
            title = "Calculation method",
            options = CalcMethod.entries.map { SheetOption(it, it.title, it.summary) },
            selected = prefs.method,
            onSelect = { method ->
                update { p ->
                    if (method == CalcMethod.Custom && p.method != CalcMethod.Custom) {
                        val base = container.prayerCalculator.parameters(p)
                        p.copy(
                            method = method,
                            methodSetByUser = true,
                            customFajrAngle = base.fajrAngle,
                            customIshaAngle = if (base.ishaAngle > 0) base.ishaAngle else 17.0,
                            customIshaMinutes = base.ishaInterval,
                        )
                    } else {
                        p.copy(method = method, methodSetByUser = true)
                    }
                }
            },
            onDismiss = { sheet = null },
        )
        "highLat" -> OptionSheet(
            title = "High-latitude rule",
            options = HighLatitude.entries.map { SheetOption(it, it.title, it.summary) },
            selected = prefs.highLatitude,
            onSelect = { rule -> update { it.copy(highLatitude = rule) } },
            onDismiss = { sheet = null },
        )
    }
}

@Composable
private fun AngleSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium), color = Mihraya.colors.textPrimary, modifier = Modifier.weight(1f))
            Text(String.format(Locale.US, "%.1f°", local), style = Mihraya.type.label, color = Mihraya.colors.accent)
        }
        MihrayaSlider(
            value = local,
            onValueChange = { local = (it * 2).let(Math::round) / 2f },
            onValueChangeFinished = { onChange(local) },
            valueRange = range,
        )
    }
}

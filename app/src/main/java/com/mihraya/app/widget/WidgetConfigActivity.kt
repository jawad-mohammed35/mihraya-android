package com.mihraya.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.appContainer
import com.mihraya.app.core.designsystem.components.PrimaryButton
import com.mihraya.app.core.designsystem.components.SectionLabel
import com.mihraya.app.core.designsystem.components.SegmentedControl
import com.mihraya.app.core.designsystem.components.SettingsGroup
import com.mihraya.app.core.designsystem.components.SettingsSwitchRow
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.designsystem.theme.MihrayaTheme

class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider?.className.orEmpty()
        val initial = WidgetPrefs.read(this, widgetId)

        setContent {
            val settings by appContainer.settings.state.collectAsStateWithLifecycle()
            val current = settings
            MihrayaTheme(
                mode = current?.theme ?: com.mihraya.app.data.settings.ThemeMode.Dark,
                accent = current?.accent ?: com.mihraya.app.data.settings.AccentColor.Champagne,
                pureBlack = current?.pureOledBlack == true,
            ) {
                var theme by rememberSaveable { mutableStateOf(initial.theme.name) }
                var layout by rememberSaveable { mutableStateOf(initial.layout.name) }
                var sunrise by rememberSaveable { mutableStateOf(initial.sunrise) }
                var duha by rememberSaveable { mutableStateOf(initial.duha) }
                var qiyam by rememberSaveable { mutableStateOf(initial.qiyam) }
                var translation by rememberSaveable { mutableStateOf(initial.showTranslation) }
                val colors = Mihraya.colors
                val timetable = provider.endsWith("PrayerTimetableReceiver")
                val verse = provider.endsWith("VerseReceiver")

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text("Widget", style = Mihraya.type.headline, color = colors.textPrimary)
                    Column {
                        SectionLabel("Appearance")
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
                        SegmentedControl(
                            options = listOf("Dark", "Light", "Transparent"),
                            selectedIndex = WidgetTheme.entries.indexOfFirst { it.name == theme }.coerceAtLeast(0),
                            onSelect = { theme = WidgetTheme.entries[it].name },
                        )
                    }
                    if (timetable) {
                        Column {
                            SectionLabel("Layout")
                            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
                            SegmentedControl(
                                options = listOf("Vertical", "Horizontal"),
                                selectedIndex = if (layout == TimetableLayout.Horizontal.name) 1 else 0,
                                onSelect = { layout = if (it == 1) TimetableLayout.Horizontal.name else TimetableLayout.Vertical.name },
                            )
                        }
                        SettingsGroup("Extra times") {
                            SettingsSwitchRow("Sunrise", sunrise, { sunrise = it })
                            SettingsSwitchRow("Duha", duha, { duha = it })
                            SettingsSwitchRow("Qiyam", qiyam, { qiyam = it })
                        }
                    }
                    if (verse) {
                        SettingsGroup("Verse") {
                            SettingsSwitchRow("Show translation", translation, { translation = it }, subtitle = "Saheeh International")
                        }
                    }
                    PrimaryButton(
                        "Add widget",
                        onClick = {
                            WidgetPrefs.write(
                                this@WidgetConfigActivity,
                                widgetId,
                                WidgetOptions(
                                    theme = runCatching { WidgetTheme.valueOf(theme) }.getOrDefault(WidgetTheme.Dark),
                                    layout = runCatching { TimetableLayout.valueOf(layout) }.getOrDefault(TimetableLayout.Vertical),
                                    sunrise = sunrise,
                                    duha = duha,
                                    qiyam = qiyam,
                                    showTranslation = translation,
                                ),
                            )
                            WidgetRefresh.refresh(this@WidgetConfigActivity)
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
                            )
                            finish()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

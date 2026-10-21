package com.mihraya.app.feature.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.components.GlassCard
import com.mihraya.app.core.designsystem.components.OptionSheet
import com.mihraya.app.core.designsystem.components.SectionLabel
import com.mihraya.app.core.designsystem.components.SettingsGroup
import com.mihraya.app.core.designsystem.components.SettingsValueRow
import com.mihraya.app.core.designsystem.components.SheetOption
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.data.audio.Reciters
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.data.quran.Verse
import com.mihraya.app.feature.quran.ReaderPreferencesPanel
import com.mihraya.app.feature.quran.VerseBody
import com.mihraya.app.ui.LocalAppContainer

private const val PREVIEW_SURAH = 1
private const val PREVIEW_AYAH = 2

@Composable
fun ReadingSettingsScreen(onBack: () -> Unit, onManageTranslations: () -> Unit) {
    val container = LocalAppContainer.current
    val settingsState by container.settings.state.collectAsStateWithLifecycle()
    val installed by container.quran.installedTranslations.collectAsStateWithLifecycle()
    val settings = settingsState ?: return
    val prefs = settings.reader
    var pickReciter by remember { mutableStateOf(false) }

    var preview by remember { mutableStateOf<Verse?>(null) }
    LaunchedEffect(prefs, installed) {
        preview = runCatching { container.quran.surahVerses(PREVIEW_SURAH, prefs)[PREVIEW_AYAH - 1] }.getOrNull()
    }

    SettingsScaffold("Reading", onBack, subtitle = "Script, fonts and translations") {
        Column {
            SectionLabel("Preview · ${QuranMeta.surah(PREVIEW_SURAH).name} $PREVIEW_SURAH:$PREVIEW_AYAH")
            Spacer(Modifier.height(10.dp))
            GlassCard(Modifier.animateContentSize(), contentPadding = PaddingValues(20.dp)) {
                val verse = preview
                if (verse != null) {
                    VerseBody(verse, prefs, includeTranslations = prefs.showTranslation)
                } else {
                    Text("Loading…", style = Mihraya.type.bodySmall, color = Mihraya.colors.textTertiary)
                }
            }
        }

        ReaderPreferencesPanel(
            prefs = prefs,
            onChange = { transform -> container.settings.edit { it.copy(reader = transform(it.reader)) } },
            onManageTranslations = onManageTranslations,
        )

        SettingsGroup("Listening", footer = "Used when you tap play in the reader and on the Listen tab.") {
            SettingsValueRow(
                title = "Preferred reciter",
                value = Reciters.byId(settings.reciterId).shortName,
                onClick = { pickReciter = true },
                icon = Icons.Rounded.RecordVoiceOver,
            )
        }
    }

    if (pickReciter) {
        OptionSheet(
            title = "Preferred reciter",
            options = Reciters.all.map { SheetOption(it.id, it.name, "${it.style} · ${it.origin}") },
            selected = settings.reciterId,
            onSelect = { id -> container.settings.edit { it.copy(reciterId = id) } },
            onDismiss = { pickReciter = false },
        )
    }
}

package com.mihraya.app.feature.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.components.HairlineDivider
import com.mihraya.app.core.designsystem.components.MihrayaSlider
import com.mihraya.app.core.designsystem.components.MihrayaSwitch
import com.mihraya.app.core.designsystem.components.SectionLabel
import com.mihraya.app.core.designsystem.components.SegmentedControl
import com.mihraya.app.core.designsystem.components.glass
import com.mihraya.app.core.designsystem.theme.ArabicFonts
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.data.quran.TranslationCatalog
import com.mihraya.app.data.settings.ArabicFont
import com.mihraya.app.data.settings.QuranScript
import com.mihraya.app.data.settings.ReaderLayout
import com.mihraya.app.data.settings.ReaderPreferences
import com.mihraya.app.ui.LocalAppContainer
import java.util.Locale

private const val MAX_TRANSLATIONS = 3

@Composable
fun ReaderPreferencesPanel(
    prefs: ReaderPreferences,
    onChange: ((ReaderPreferences) -> ReaderPreferences) -> Unit,
    onManageTranslations: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Mihraya.colors
    val installed by LocalAppContainer.current.quran.installedTranslations.collectAsStateWithLifecycle()

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Column {
            SectionLabel("Script")
            Spacer(Modifier.height(10.dp))
            SegmentedControl(
                options = QuranScript.entries.map { it.label },
                selectedIndex = prefs.script.ordinal,
                onSelect = { index -> onChange { it.copy(script = QuranScript.entries[index]) } },
            )
        }

        Column {
            SectionLabel("Arabic font")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArabicFont.entries.forEach { font ->
                    FontOption(
                        font = font,
                        script = prefs.script,
                        selected = prefs.arabicFont == font,
                        onClick = { onChange { it.copy(arabicFont = font) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        PreferenceSlider(
            label = "Arabic size",
            value = prefs.arabicSize,
            range = 20f..44f,
            format = { "${it.toInt()}" },
            onChange = { v -> onChange { it.copy(arabicSize = v) } },
        )
        PreferenceSlider(
            label = "Line spacing",
            value = prefs.lineHeight,
            range = 1.4f..2.6f,
            format = { String.format(Locale.US, "%.1f×", it) },
            onChange = { v -> onChange { it.copy(lineHeight = v) } },
        )
        PreferenceSlider(
            label = "Translation size",
            value = prefs.translationSize,
            range = 12f..22f,
            format = { "${it.toInt()}" },
            onChange = { v -> onChange { it.copy(translationSize = v) } },
        )

        Column {
            SectionLabel("Layout")
            Spacer(Modifier.height(10.dp))
            SegmentedControl(
                options = ReaderLayout.entries.map { it.label },
                selectedIndex = prefs.layout.ordinal,
                onSelect = { index -> onChange { it.copy(layout = ReaderLayout.entries[index]) } },
            )
        }

        Column(Modifier.glass(RoundedCornerShape(22.dp), colors)) {
            ToggleRow("Show translation", prefs.showTranslation) { v -> onChange { it.copy(showTranslation = v) } }
            HairlineDivider(Modifier.padding(horizontal = 16.dp))
            ToggleRow("Show transliteration", prefs.showTransliteration) { v -> onChange { it.copy(showTransliteration = v) } }
            HairlineDivider(Modifier.padding(horizontal = 16.dp))
            ToggleRow("Keep screen on while reading", prefs.keepScreenOn) { v -> onChange { it.copy(keepScreenOn = v) } }
        }

        Column {
            SectionLabel("Translations · up to $MAX_TRANSLATIONS")
            Spacer(Modifier.height(10.dp))
            Column(Modifier.glass(RoundedCornerShape(22.dp), colors)) {
                TranslationCatalog.all.filter { it.id in installed }.forEachIndexed { index, info ->
                    if (index > 0) HairlineDivider(Modifier.padding(horizontal = 16.dp))
                    val selected = info.id in prefs.translations
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onChange { p ->
                                    val next = if (selected) p.translations - info.id
                                    else (p.translations + info.id).takeLast(MAX_TRANSLATIONS)
                                    p.copy(translations = next, showTranslation = !selected || p.showTranslation)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(info.name, style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                            Text(info.language, style = Mihraya.type.caption, color = colors.textTertiary)
                        }
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (selected) Modifier.background(colors.accent)
                                    else Modifier.border(1.5.dp, colors.textTertiary, RoundedCornerShape(8.dp)),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Rounded.Check, null, tint = colors.onAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                HairlineDivider(Modifier.padding(horizontal = 16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onManageTranslations)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Download more translations",
                        style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium),
                        color = colors.accent,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.accent)
                }
            }
        }
    }
}

@Composable
private fun FontOption(font: ArabicFont, script: QuranScript, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    Column(
        modifier
            .glass(RoundedCornerShape(20.dp), colors, if (selected) colors.accentSoft else colors.glass)
            .then(if (selected) Modifier.border(1.dp, colors.accent, RoundedCornerShape(20.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "بِسْمِ ٱللَّهِ",
            style = Mihraya.type.body.copy(
                fontFamily = ArabicFonts.quran(font, script),
                fontSize = 22.sp,
                lineHeight = 40.sp,
                textDirection = TextDirection.Rtl,
                textAlign = TextAlign.Center,
            ),
            color = if (selected) colors.accent else colors.textPrimary,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (font == ArabicFont.AmiriQuran && script == QuranScript.IndoPak) "Amiri" else font.label,
            style = Mihraya.type.caption,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun PreferenceSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(label, Modifier.weight(1f))
            Text(format(local), style = Mihraya.type.label, color = Mihraya.colors.accent)
        }
        MihrayaSlider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = range,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium), color = Mihraya.colors.textPrimary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        MihrayaSwitch(checked, onChange)
    }
}

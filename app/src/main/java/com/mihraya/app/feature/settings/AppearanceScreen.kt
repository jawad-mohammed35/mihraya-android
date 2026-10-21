package com.mihraya.app.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.components.SectionLabel
import com.mihraya.app.core.designsystem.components.SettingsDivider
import com.mihraya.app.core.designsystem.components.SettingsGroup
import com.mihraya.app.core.designsystem.components.SettingsSwitchRow
import com.mihraya.app.core.designsystem.components.glass
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.designsystem.theme.MihrayaColors
import com.mihraya.app.core.designsystem.theme.accentFor
import com.mihraya.app.core.designsystem.theme.darkPalette
import com.mihraya.app.core.designsystem.theme.lightPalette
import com.mihraya.app.core.util.rememberHaptics
import com.mihraya.app.data.settings.AccentColor
import com.mihraya.app.data.settings.ThemeMode
import com.mihraya.app.ui.LocalAppContainer

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val settingsState by container.settings.state.collectAsStateWithLifecycle()
    val settings = settingsState ?: return
    val haptics = rememberHaptics()
    val systemDark = isSystemInDarkTheme()

    SettingsScaffold("Appearance", onBack) {
        Column {
            SectionLabel("Theme")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val dark = when (mode) {
                        ThemeMode.Dark -> true
                        ThemeMode.Light -> false
                        ThemeMode.System -> systemDark
                    }
                    ThemeCard(
                        mode = mode,
                        palette = if (dark) darkPalette(settings.accent) else lightPalette(settings.accent),
                        selected = settings.theme == mode,
                        onClick = {
                            haptics.tap()
                            container.settings.edit { it.copy(theme = mode) }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "OLED Black uses true #000000 so unlit pixels stay off — easiest on the eyes and the battery.",
                style = Mihraya.type.caption,
                color = Mihraya.colors.textTertiary,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }

        Column {
            SectionLabel("Accent")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AccentColor.entries.forEach { accent ->
                    AccentSwatch(
                        accent = accent,
                        color = accentFor(accent, Mihraya.colors.isDark),
                        selected = settings.accent == accent,
                        onClick = {
                            haptics.tap()
                            container.settings.edit { it.copy(accent = accent) }
                        },
                    )
                }
            }
        }

        SettingsGroup("Display") {
            SettingsSwitchRow(
                title = "24-hour time",
                checked = settings.use24Hour,
                onCheckedChange = { v -> container.settings.edit { it.copy(use24Hour = v) } },
                subtitle = if (settings.use24Hour) "Prayer times shown as 17:42" else "Prayer times shown as 5:42 PM",
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = "Haptic feedback",
                checked = settings.haptics,
                onCheckedChange = { v -> container.settings.edit { it.copy(haptics = v) } },
                subtitle = "Subtle taps for the tasbīḥ, compass and controls",
            )
        }
    }
}

@Composable
private fun ThemeCard(
    mode: ThemeMode,
    palette: MihrayaColors,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Mihraya.colors
    val ring by animateColorAsState(if (selected) colors.accent else Color.Transparent, tween(220), label = "themeRing")
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .border(1.5.dp, ring, shape)
            .padding(3.dp)
            .glass(RoundedCornerShape(19.dp), colors)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.background)
                .border(0.75.dp, palette.glassBorder, RoundedCornerShape(14.dp)),
        ) {
            if (mode == ThemeMode.System) {
                val other = if (palette.isDark) lightPalette(AccentColor.Emerald) else darkPalette(AccentColor.Emerald)
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(0.5f)
                        .background(other.background),
                )
            }
            MiniScreen(palette, Modifier.fillMaxSize().padding(8.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            mode.label,
            style = Mihraya.type.label.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) colors.accent else colors.textSecondary,
            maxLines = 1,
        )
    }
}

/** A tiny sketch of the home screen drawn in the given palette. */
@Composable
private fun MiniScreen(palette: MihrayaColors, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.width(26.dp).height(4.dp).clip(CircleShape).background(palette.textTertiary))
        Box(Modifier.width(40.dp).height(9.dp).clip(CircleShape).background(palette.textPrimary.copy(alpha = 0.85f)))
        Spacer(Modifier.height(2.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(8.dp), palette)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.width(18.dp).height(3.dp).clip(CircleShape).background(palette.accent))
            Box(Modifier.width(30.dp).height(6.dp).clip(CircleShape).background(palette.textPrimary.copy(alpha = 0.8f)))
            Box(Modifier.fillMaxWidth().height(2.dp).clip(CircleShape).background(palette.glassBorder))
        }
        repeat(3) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(6.dp), palette)
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(16.dp).height(3.dp).clip(CircleShape).background(palette.textSecondary))
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(10.dp).height(3.dp).clip(CircleShape).background(if (it == 0) palette.accent else palette.textTertiary))
            }
        }
    }
}

@Composable
private fun AccentSwatch(accent: AccentColor, color: Color, selected: Boolean, onClick: () -> Unit) {
    val colors = Mihraya.colors
    val ring by animateColorAsState(if (selected) color else Color.Transparent, tween(220), label = "accentRing")
    Column(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(58.dp)
                .border(1.5.dp, ring, CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = colors.onAccent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            accent.label,
            style = Mihraya.type.caption.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) colors.textPrimary else colors.textTertiary,
        )
    }
}

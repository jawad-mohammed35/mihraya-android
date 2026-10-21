package com.mihraya.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.components.CircleIconButton
import com.mihraya.app.core.designsystem.components.MihrayaDialog
import com.mihraya.app.core.designsystem.components.SettingsDivider
import com.mihraya.app.core.designsystem.components.SettingsGroup
import com.mihraya.app.core.designsystem.components.SettingsSwitchRow
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.data.quran.TranslationCatalog
import com.mihraya.app.data.quran.TranslationDownload
import com.mihraya.app.data.quran.TranslationInfo
import com.mihraya.app.ui.LocalAppContainer

internal const val MAX_ACTIVE_TRANSLATIONS = 3

@Composable
fun TranslationsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val settingsState by container.settings.state.collectAsStateWithLifecycle()
    val installed by container.quran.installedTranslations.collectAsStateWithLifecycle()
    val downloads by container.quran.downloads.collectAsStateWithLifecycle()
    val settings = settingsState ?: return
    val active = settings.reader.translations
    var confirmDelete by remember { mutableStateOf<TranslationInfo?>(null) }

    fun toggle(info: TranslationInfo) = container.settings.edit { s ->
        val current = s.reader.translations
        val next = if (info.id in current) current - info.id else (current + info.id).takeLast(MAX_ACTIVE_TRANSLATIONS)
        s.copy(reader = s.reader.copy(translations = next, showTranslation = next.isNotEmpty() || s.reader.showTranslation))
    }

    SettingsScaffold(
        "Translations",
        onBack,
        subtitle = "${installed.size} installed · ${active.size} of $MAX_ACTIVE_TRANSLATIONS shown",
    ) {
        SettingsGroup(
            "In the reader",
            footer = "Tap an installed translation to show or hide it beside the Arabic. Downloads are about 1–2 MB and work offline afterwards.",
        ) {
            SettingsSwitchRow(
                title = "Show translations",
                checked = settings.reader.showTranslation,
                onCheckedChange = { v -> container.settings.edit { it.copy(reader = it.reader.copy(showTranslation = v)) } },
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = "Show transliteration",
                checked = settings.reader.showTransliteration,
                onCheckedChange = { v -> container.settings.edit { it.copy(reader = it.reader.copy(showTransliteration = v)) } },
                subtitle = "Latin-script pronunciation guide (included)",
            )
        }

        TranslationCatalog.languages.forEach { language ->
            SettingsGroup(language) {
                TranslationCatalog.all.filter { it.language == language }.forEachIndexed { index, info ->
                    if (index > 0) SettingsDivider()
                    TranslationRow(
                        info = info,
                        installed = info.id in installed,
                        active = info.id in active,
                        download = downloads[info.id],
                        onToggle = { toggle(info) },
                        onDownload = {
                            container.quran.clearDownloadError(info.id)
                            container.quran.downloadTranslation(info.id)
                        },
                        onDelete = { confirmDelete = info },
                    )
                }
            }
        }

        Text(
            "Translations are provided through the Quran.com API.",
            style = Mihraya.type.caption,
            color = Mihraya.colors.textTertiary,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }

    confirmDelete?.let { info ->
        MihrayaDialog(
            title = "Remove translation?",
            message = "${info.name} will be deleted from this device. You can download it again any time.",
            onDismiss = { confirmDelete = null },
            confirmText = "Remove",
            destructive = true,
            onConfirm = {
                container.quran.deleteTranslation(info.id)
                container.settings.edit { s -> s.copy(reader = s.reader.copy(translations = s.reader.translations - info.id)) }
                confirmDelete = null
            },
        )
    }
}

@Composable
private fun TranslationRow(
    info: TranslationInfo,
    installed: Boolean,
    active: Boolean,
    download: TranslationDownload?,
    onToggle: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = Mihraya.colors
    val status = when {
        download == TranslationDownload.Running -> "Downloading…"
        download is TranslationDownload.Failed -> download.message
        info.bundled && active -> "Included · shown in reader"
        info.bundled -> "Included"
        installed && active -> "Downloaded · shown in reader"
        installed -> "Downloaded"
        else -> "Not downloaded"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = installed, role = Role.Checkbox, onClick = onToggle)
            .heightIn(min = 62.dp)
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    when {
                        active && installed -> Modifier.background(colors.accent)
                        installed -> Modifier.border(1.5.dp, colors.textTertiary, RoundedCornerShape(8.dp))
                        else -> Modifier.border(1.5.dp, colors.divider, RoundedCornerShape(8.dp))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (active && installed) Icon(Icons.Rounded.Check, null, tint = colors.onAccent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                info.name,
                style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium),
                color = if (installed) colors.textPrimary else colors.textSecondary,
            )
            Text(
                status,
                style = Mihraya.type.caption,
                color = if (download is TranslationDownload.Failed) colors.danger else colors.textTertiary,
                maxLines = 2,
            )
        }
        Spacer(Modifier.width(8.dp))
        when {
            download == TranslationDownload.Running -> Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            }
            download is TranslationDownload.Failed -> CircleIconButton(
                Icons.Rounded.Refresh, "Retry download", onDownload, size = 38.dp, iconSize = 18.dp, filled = false, tint = colors.danger,
            )
            info.bundled -> Unit
            installed -> CircleIconButton(
                Icons.Rounded.DeleteOutline, "Remove ${info.name}", onDelete, size = 38.dp, iconSize = 18.dp, filled = false, tint = colors.textTertiary,
            )
            else -> CircleIconButton(
                Icons.Rounded.Download, "Download ${info.name}", onDownload, size = 38.dp, iconSize = 18.dp, tint = colors.accent,
            )
        }
    }
}

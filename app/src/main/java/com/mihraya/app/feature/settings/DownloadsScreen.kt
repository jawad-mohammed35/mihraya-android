package com.mihraya.app.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.art.Artwork
import com.mihraya.app.core.designsystem.components.GlassCard
import com.mihraya.app.core.designsystem.components.HairlineDivider
import com.mihraya.app.core.designsystem.components.MihrayaDialog
import com.mihraya.app.core.designsystem.components.SecondaryButton
import com.mihraya.app.core.designsystem.components.SectionLabel
import com.mihraya.app.core.designsystem.components.SettingsGroup
import com.mihraya.app.core.designsystem.components.SettingsValueRow
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.util.TimeFormat
import com.mihraya.app.data.audio.AudioDownloads
import com.mihraya.app.data.audio.DownloadStatus
import com.mihraya.app.data.audio.Reciter
import com.mihraya.app.data.audio.Reciters
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.data.quran.TranslationCatalog
import com.mihraya.app.feature.home.ProgressLine
import com.mihraya.app.feature.listen.DownloadButton
import com.mihraya.app.ui.LocalAppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DownloadsScreen(onBack: () -> Unit, onManageTranslations: () -> Unit) {
    val container = LocalAppContainer.current
    val downloads = container.audioDownloads
    val downloaded by downloads.downloaded.collectAsStateWithLifecycle()
    val active by downloads.active.collectAsStateWithLifecycle(emptyMap())
    val installed by container.quran.installedTranslations.collectAsStateWithLifecycle()
    var confirmRemove by remember { mutableStateOf<Reciter?>(null) }

    val audioBytes by produceState(emptyMap<String, Long>(), downloaded) {
        value = withContext(Dispatchers.IO) { Reciters.all.associate { it.id to downloads.bytesFor(it.id) } }
    }
    val translationBytes by produceState(0L, installed) {
        value = withContext(Dispatchers.IO) { container.quran.downloadedBytes() }
    }
    val downloadedTranslations = installed.count { id -> TranslationCatalog.byId(id)?.bundled == false }
    val totalSurahs = downloaded.values.sumOf { it.size }

    SettingsScaffold("Downloads", onBack, subtitle = "Recitations and translations stored offline") {
        GlassCard(contentPadding = PaddingValues(20.dp)) {
            Text("ON THIS DEVICE", style = Mihraya.type.overline, color = Mihraya.colors.accent)
            Spacer(Modifier.height(8.dp))
            Text(
                TimeFormat.bytes(audioBytes.values.sum() + translationBytes),
                style = Mihraya.type.headline,
                color = Mihraya.colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$totalSurahs recited sūrahs · $downloadedTranslations downloaded translations",
                style = Mihraya.type.bodySmall,
                color = Mihraya.colors.textTertiary,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Recitations")
            Reciters.all.forEach { reciter ->
                val prefix = "${reciter.id}/"
                val reciterActive = active.filterKeys { it.startsWith(prefix) }
                ReciterDownloads(
                    reciter = reciter,
                    downloadedSurahs = downloaded[reciter.id].orEmpty(),
                    active = reciterActive.entries
                        .mapNotNull { (key, status) -> key.removePrefix(prefix).toIntOrNull()?.let { it to status } }
                        .toMap(),
                    bytes = audioBytes[reciter.id] ?: 0L,
                    downloads = downloads,
                    onRemoveAll = { confirmRemove = reciter },
                )
            }
        }

        SettingsGroup("Translations", footer = "Downloads continue in the background and resume if your connection drops.") {
            SettingsValueRow(
                title = "Downloaded translations",
                value = if (downloadedTranslations == 0) "None" else "$downloadedTranslations · ${TimeFormat.bytes(translationBytes)}",
                onClick = onManageTranslations,
                icon = Icons.Rounded.Translate,
            )
        }
    }

    confirmRemove?.let { reciter ->
        MihrayaDialog(
            title = "Remove downloads?",
            message = "All offline recitations by ${reciter.name} will be deleted. You can still stream them.",
            onDismiss = { confirmRemove = null },
            confirmText = "Remove",
            destructive = true,
            onConfirm = {
                downloads.deleteAll(reciter.id)
                confirmRemove = null
            },
        )
    }
}

@Composable
private fun ReciterDownloads(
    reciter: Reciter,
    downloadedSurahs: Set<Int>,
    active: Map<Int, DownloadStatus>,
    bytes: Long,
    downloads: AudioDownloads,
    onRemoveAll: () -> Unit,
) {
    val colors = Mihraya.colors
    var expanded by rememberSaveable(reciter.id) { mutableStateOf(false) }
    val inFlight = active.filterValues { it != DownloadStatus.Failed }
    val failed = active.count { it.value == DownloadStatus.Failed }
    val listed = (downloadedSurahs + active.keys).sorted()
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    GlassCard(contentPadding = PaddingValues(0.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = listed.isNotEmpty()) { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(reciter.hue, title = null, seed = reciter.id.hashCode(), cornerRadius = 14.dp, modifier = Modifier.size(52.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(reciter.name, style = Mihraya.type.body.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary, maxLines = 1)
                val detail = buildList {
                    add("${downloadedSurahs.size} of 114")
                    if (bytes > 0) add(TimeFormat.bytes(bytes))
                    if (inFlight.isNotEmpty()) add("${inFlight.size} downloading")
                    if (failed > 0) add("$failed failed")
                }.joinToString(" · ")
                Text(detail, style = Mihraya.type.caption, color = if (failed > 0) colors.danger else colors.textTertiary)
            }
            if (listed.isNotEmpty()) {
                Icon(
                    Icons.Rounded.ExpandMore,
                    if (expanded) "Hide sūrahs" else "Show sūrahs",
                    tint = colors.textTertiary,
                    modifier = Modifier.rotate(chevron),
                )
            }
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            ProgressLine(downloadedSurahs.size / 114f)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (inFlight.isNotEmpty()) {
                    SecondaryButton("Cancel", onClick = { downloads.cancelAll(reciter.id) }, icon = Icons.Rounded.Close)
                } else if (downloadedSurahs.size < 114) {
                    SecondaryButton(
                        if (downloadedSurahs.isEmpty()) "Download all" else "Download the rest",
                        onClick = { downloads.enqueueAll(reciter) },
                        icon = Icons.Rounded.Download,
                        tint = colors.accent,
                    )
                }
                if (downloadedSurahs.isNotEmpty()) {
                    SecondaryButton("Remove", onClick = onRemoveAll, icon = Icons.Rounded.DeleteOutline, tint = colors.textSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        AnimatedVisibility(
            visible = expanded && listed.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                HairlineDivider()
                listed.forEach { number ->
                    val surah = QuranMeta.surah(number)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            number.toString(),
                            style = Mihraya.type.caption.copy(fontFeatureSettings = "tnum"),
                            color = colors.textTertiary,
                            modifier = Modifier.width(32.dp),
                        )
                        Text(surah.name, style = Mihraya.type.body, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        DownloadButton(
                            downloaded = number in downloadedSurahs,
                            status = active[number],
                            onDownload = { downloads.enqueue(reciter, number) },
                            onCancel = { downloads.cancel(reciter.id, number) },
                            onDelete = { downloads.delete(reciter.id, number) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

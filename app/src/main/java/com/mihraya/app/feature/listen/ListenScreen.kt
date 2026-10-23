package com.mihraya.app.feature.listen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.art.Artwork
import com.mihraya.app.core.designsystem.components.EmptyState
import com.mihraya.app.core.designsystem.components.GlassCard
import com.mihraya.app.core.designsystem.components.HairlineDivider
import com.mihraya.app.core.designsystem.components.MihrayaDialog
import com.mihraya.app.core.designsystem.components.ScreenHeader
import com.mihraya.app.core.designsystem.components.SecondaryButton
import com.mihraya.app.core.designsystem.components.SectionLabel
import com.mihraya.app.core.designsystem.components.SegmentedControl
import com.mihraya.app.core.designsystem.components.StarBadge
import com.mihraya.app.core.designsystem.components.pressScale
import com.mihraya.app.core.designsystem.theme.ArabicFonts
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.util.TimeFormat
import com.mihraya.app.data.audio.AudioDownloads
import com.mihraya.app.data.audio.Reciter
import com.mihraya.app.data.audio.Reciters
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.ui.LocalBottomInset
import com.mihraya.app.ui.PlayPauseButton
import com.mihraya.app.ui.mihrayaViewModel

@Composable
fun ListenScreen(onOpenPlayer: () -> Unit, onOpenDownloads: () -> Unit) {
    val vm = mihrayaViewModel { ListenViewModel(it) }
    val reciter by vm.reciter.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val downloaded by vm.downloaded.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val lastPlayed by vm.lastPlayed.collectAsStateWithLifecycle()
    val colors = Mihraya.colors
    val item = Modifier.widthIn(max = 680.dp).fillMaxWidth()

    var confirmDownloadAll by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Int?>(null) }

    val reciterDownloads = downloaded[reciter.id].orEmpty()
    val surahs = if (filter == 1) QuranMeta.surahs.filter { it.number in reciterDownloads } else QuranMeta.surahs

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = LocalBottomInset.current + 16.dp),
    ) {
        item(key = "header") {
            ScreenHeader("Listen", modifier = item.padding(horizontal = 20.dp))
        }

        item(key = "hero") {
            val nowReciter = playback.reciter
            val nowSurah = playback.surah
            if (playback.hasMedia && nowReciter != null && nowSurah != null) {
                NowPlayingCard(
                    label = if (playback.isPlaying) "NOW PLAYING" else "PAUSED",
                    reciter = nowReciter,
                    surah = nowSurah,
                    onClick = onOpenPlayer,
                    trailing = { PlayPauseButton(playback, onToggle = { vm.play(nowReciter, nowSurah) }, size = 52, filledAccent = true) },
                    modifier = item.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            } else {
                lastPlayed?.let { last ->
                    val lastReciter = Reciters.byId(last.reciterId)
                    NowPlayingCard(
                        label = "CONTINUE LISTENING",
                        reciter = lastReciter,
                        surah = last.surah,
                        detail = if (last.positionMs > 0) "Resume at ${TimeFormat.media(last.positionMs)}" else null,
                        onClick = { vm.resume(last) },
                        trailing = { PlayPauseButton(playback, onToggle = { vm.resume(last) }, size = 52, filledAccent = true) },
                        modifier = item.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
        }

        item(key = "recitersLabel") {
            SectionLabel("Reciters", item.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 12.dp))
        }
        item(key = "reciters") {
            LazyRow(
                modifier = item,
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(Reciters.all, key = { it.id }) { r ->
                    ReciterCard(r, selected = r.id == reciter.id, onClick = { vm.selectReciter(r) })
                }
            }
        }

        item(key = "reciterDetail") {
            Column(item.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(reciter.name, style = Mihraya.type.title, color = colors.textPrimary)
                        Text(
                            "${reciter.style} · ${reciter.origin} · ${reciterDownloads.size}/114 offline",
                            style = Mihraya.type.caption,
                            color = colors.textTertiary,
                        )
                    }
                    Text(
                        reciter.arabicName,
                        style = Mihraya.type.body.copy(fontFamily = ArabicFonts.Amiri, fontSize = 18.sp, textDirection = TextDirection.Rtl),
                        color = colors.accent,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        if (reciterDownloads.size == 114) "All downloaded" else "Download all",
                        onClick = { confirmDownloadAll = true },
                        icon = Icons.Rounded.Download,
                        enabled = reciterDownloads.size < 114,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton("Manage", onClick = onOpenDownloads, icon = Icons.Rounded.DownloadForOffline)
                }
                Spacer(Modifier.height(18.dp))
                SegmentedControl(
                    options = listOf("All sūrahs", "Downloaded"),
                    selectedIndex = filter,
                    onSelect = { vm.filter.value = it },
                )
            }
        }

        if (surahs.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.DownloadForOffline,
                    title = "Nothing downloaded yet",
                    message = "Download sūrahs to listen without a connection. They're stored privately on this device.",
                    modifier = item,
                )
            }
        }

        items(surahs, key = { "a${it.number}" }) { surah ->
            val isCurrent = playback.reciterId == reciter.id && playback.surah == surah.number
            Column(item.padding(horizontal = 20.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.play(reciter, surah.number) }
                        .heightIn(min = 66.dp)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StarBadge(surah.number, highlighted = isCurrent)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                surah.name,
                                style = Mihraya.type.titleSmall,
                                color = if (isCurrent) colors.accent else colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (isCurrent) {
                                Spacer(Modifier.width(8.dp))
                                EqualizerBars(playing = playback.isPlaying)
                            }
                        }
                        Text(
                            "${surah.meaning} · ${surah.verseCount} verses",
                            style = Mihraya.type.caption,
                            color = colors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    DownloadButton(
                        downloaded = surah.number in reciterDownloads,
                        status = active[AudioDownloads.key(reciter.id, surah.number)],
                        onDownload = { vm.download(reciter, surah.number) },
                        onCancel = { vm.cancel(reciter, surah.number) },
                        onDelete = { confirmDelete = surah.number },
                    )
                }
                HairlineDivider(startIndent = 54.dp)
            }
        }
    }

    if (confirmDownloadAll) {
        MihrayaDialog(
            title = "Download all 114 sūrahs?",
            message = "The complete recitation by ${reciter.name} is roughly 1–3 GB. Downloads run in the background over any connection.",
            onDismiss = { confirmDownloadAll = false },
            confirmText = "Download",
            onConfirm = {
                confirmDownloadAll = false
                vm.downloadAll(reciter)
            },
        )
    }

    confirmDelete?.let { number ->
        MihrayaDialog(
            title = "Remove download?",
            message = "${QuranMeta.surah(number).name} by ${reciter.shortName} will stream online instead.",
            onDismiss = { confirmDelete = null },
            confirmText = "Remove",
            destructive = true,
            onConfirm = {
                confirmDelete = null
                vm.delete(reciter, number)
            },
        )
    }
}

@Composable
private fun NowPlayingCard(
    label: String,
    reciter: Reciter,
    surah: Int,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    val colors = Mihraya.colors
    val info = QuranMeta.surah(surah)
    GlassCard(modifier, onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(reciter.hue, info.arabicName, seed = info.number, cornerRadius = 18.dp, modifier = Modifier.size(84.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = Mihraya.type.overline, color = colors.accent)
                Spacer(Modifier.height(4.dp))
                Text(info.name, style = Mihraya.type.title, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail ?: reciter.name, style = Mihraya.type.caption, color = colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun ReciterCard(reciter: Reciter, selected: Boolean, onClick: () -> Unit) {
    val colors = Mihraya.colors
    val ring by animateColorAsState(if (selected) colors.accent else Color.Transparent, tween(220), label = "ring")
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .width(132.dp)
            .pressScale(interaction, 0.96f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Artwork(
            hue = reciter.hue,
            title = reciter.arabicShortName,
            cornerRadius = 24.dp,
            modifier = Modifier
                .size(132.dp)
                .border(2.dp, ring, RoundedCornerShape(24.dp)),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            reciter.shortName,
            style = Mihraya.type.titleSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold),
            color = if (selected) colors.accent else colors.textPrimary,
            maxLines = 1,
        )
        Text(reciter.origin, style = Mihraya.type.caption, color = colors.textTertiary, maxLines = 1)
    }
}
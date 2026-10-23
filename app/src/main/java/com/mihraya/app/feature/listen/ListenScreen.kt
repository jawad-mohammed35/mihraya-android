package com.mihraya.app.feature.listen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
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
import com.mihraya.app.core.designsystem.components.glass
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

        item(key = "reciters") {
            Column(item.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
                SectionLabel("Reciter")
                Spacer(Modifier.height(10.dp))
                ReciterDropdown(selected = reciter, onSelect = vm::selectReciter)
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
private fun ReciterDropdown(selected: Reciter, onSelect: (Reciter) -> Unit) {
    val colors = Mihraya.colors
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }
    var anchorWidth by remember { mutableIntStateOf(0) }
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, tween(220), label = "chevron")
    val shape = RoundedCornerShape(22.dp)

    Box(Modifier.fillMaxWidth().onSizeChanged { anchorWidth = it.width }) {
        Row(
            Modifier
                .fillMaxWidth()
                .glass(shape, colors)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(selected.hue, selected.arabicShortName, cornerRadius = 14.dp, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(selected.name, style = Mihraya.type.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${selected.style} · ${selected.origin}", style = Mihraya.type.caption, color = colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Choose reciter",
                tint = colors.textTertiary,
                modifier = Modifier.padding(start = 8.dp).size(22.dp).rotate(chevron),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = colors.surfaceRaised,
            border = BorderStroke(1.dp, colors.glassBorder),
            shadowElevation = 12.dp,
            modifier = Modifier.width(with(density) { anchorWidth.toDp() }),
        ) {
            Reciters.all.forEachIndexed { index, option ->
                if (index > 0) HairlineDivider(Modifier.padding(horizontal = 16.dp))
                val isSelected = option.id == selected.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.RadioButton) {
                            expanded = false
                            onSelect(option)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                        if (isSelected) Icon(Icons.Rounded.Check, "Selected", tint = colors.accent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            option.name,
                            style = Mihraya.type.body.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                            color = if (isSelected) colors.accent else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(option.origin, style = Mihraya.type.caption, color = colors.textTertiary, maxLines = 1)
                    }
                    Text(
                        option.arabicShortName,
                        style = Mihraya.type.body.copy(fontFamily = ArabicFonts.Amiri, fontSize = 16.sp, textDirection = TextDirection.Rtl),
                        color = if (isSelected) colors.accent else colors.textSecondary,
                    )
                }
            }
        }
    }
}
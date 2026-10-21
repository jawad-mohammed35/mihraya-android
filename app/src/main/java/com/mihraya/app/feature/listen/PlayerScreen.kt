package com.mihraya.app.feature.listen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.mihraya.app.core.designsystem.art.Artwork
import com.mihraya.app.core.designsystem.components.CircleIconButton
import com.mihraya.app.core.designsystem.components.OptionSheet
import com.mihraya.app.core.designsystem.components.Pill
import com.mihraya.app.core.designsystem.components.SecondaryButton
import com.mihraya.app.core.designsystem.components.SheetOption
import com.mihraya.app.core.designsystem.components.pressScale
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.util.TimeFormat
import com.mihraya.app.core.util.rememberHaptics
import com.mihraya.app.data.audio.AudioDownloads
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.playback.PlaybackProgress
import com.mihraya.app.playback.SleepTimer
import com.mihraya.app.ui.LocalAppContainer
import com.mihraya.app.ui.PlayPauseButton
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private val speeds = listOf(0.75f, 1f, 1.25f, 1.5f)

@Composable
fun PlayerScreen(onCollapse: () -> Unit, onOpenReader: (surah: Int) -> Unit) {
    val container = LocalAppContainer.current
    val player = container.player
    val state by player.state.collectAsStateWithLifecycle()
    val progress by player.progress.collectAsStateWithLifecycle(PlaybackProgress(0, 0, 0))
    val downloaded by container.audioDownloads.downloaded.collectAsStateWithLifecycle()
    val active by container.audioDownloads.active.collectAsStateWithLifecycle(emptyMap())
    val colors = Mihraya.colors
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val reciter = state.reciter ?: return
    val surah = QuranMeta.surah(state.surah ?: return)

    val dragOffset = remember { Animatable(0f) }
    val dismissThreshold = with(density) { 140.dp.toPx() }
    var showSleep by remember { mutableStateOf(false) }
    var seeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }

    val artScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "artScale",
    )

    Box(
        Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffset.value.roundToInt()) }
            .background(Color.Black)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    scope.launch { dragOffset.snapTo((dragOffset.value + delta).coerceAtLeast(0f)) }
                },
                onDragStopped = { velocity ->
                    if (dragOffset.value > dismissThreshold || velocity > 2400f) {
                        onCollapse()
                    } else {
                        dragOffset.animateTo(0f, spring(dampingRatio = 0.8f))
                    }
                },
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val hue = Color(reciter.hue)
            drawRect(
                Brush.radialGradient(
                    colors = listOf(hue.copy(alpha = 0.55f), hue.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.18f),
                    radius = size.maxDimension * 0.75f,
                ),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(Icons.Rounded.KeyboardArrowDown, "Collapse player", onCollapse, tint = Color.White)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PLAYING FROM", style = Mihraya.type.overline, color = Color.White.copy(alpha = 0.55f))
                    Text(reciter.name, style = Mihraya.type.label, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                CircleIconButton(
                    Icons.AutoMirrored.Rounded.MenuBook,
                    "Read along",
                    { onOpenReader(surah.number) },
                    tint = Color.White,
                )
            }

            Spacer(Modifier.weight(0.7f))

            Artwork(
                hue = reciter.hue,
                title = surah.arabicName,
                seed = surah.number,
                cornerRadius = 30.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = artScale
                        scaleY = artScale
                    }
                    .shadow(32.dp, RoundedCornerShape(30.dp), ambientColor = Color(reciter.hue), spotColor = Color(reciter.hue)),
            )

            Spacer(Modifier.weight(0.6f))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(surah.name, style = Mihraya.type.headline, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${surah.meaning} · ${surah.verseCount} verses",
                        style = Mihraya.type.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DownloadButton(
                    downloaded = surah.number in downloaded[reciter.id].orEmpty(),
                    status = active[AudioDownloads.key(reciter.id, surah.number)],
                    onDownload = { container.audioDownloads.enqueue(reciter, surah.number) },
                    onCancel = { container.audioDownloads.cancel(reciter.id, surah.number) },
                    onDelete = { container.audioDownloads.delete(reciter.id, surah.number) },
                )
            }

            Spacer(Modifier.height(18.dp))

            val shownFraction = if (seeking) seekFraction else progress.fraction
            Slider(
                value = shownFraction,
                onValueChange = {
                    seeking = true
                    seekFraction = it
                },
                onValueChangeFinished = {
                    player.seekTo((seekFraction * progress.durationMs).toLong())
                    seeking = false
                },
                enabled = progress.durationMs > 0,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                    disabledThumbColor = Color.White.copy(alpha = 0.4f),
                    disabledInactiveTrackColor = Color.White.copy(alpha = 0.15f),
                ),
            )
            Row(Modifier.fillMaxWidth()) {
                val position = if (seeking) (seekFraction * progress.durationMs).toLong() else progress.positionMs
                Text(
                    TimeFormat.media(position),
                    style = Mihraya.type.caption.copy(fontFeatureSettings = "tnum"),
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (progress.durationMs > 0) "-" + TimeFormat.media(progress.durationMs - position) else "--:--",
                    style = Mihraya.type.caption.copy(fontFeatureSettings = "tnum"),
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.End,
                )
            }

            state.error?.let { error ->
                Spacer(Modifier.height(10.dp))
                Text(error, style = Mihraya.type.bodySmall, color = colors.danger, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Try again", onClick = player::retry, tint = Color.White)
            }

            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    when (state.repeatMode) {
                        Player.REPEAT_MODE_ONE -> "Repeating this sūrah"
                        Player.REPEAT_MODE_ALL -> "Repeating all"
                        else -> "Repeat off"
                    },
                    {
                        haptics.tick()
                        player.cycleRepeat()
                    },
                    filled = false,
                    tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) Color.White.copy(alpha = 0.5f) else colors.accent,
                )
                CircleIconButton(Icons.Rounded.SkipPrevious, "Previous sūrah", player::previous, size = 56.dp, iconSize = 32.dp, filled = false, tint = Color.White)
                val interaction = remember { MutableInteractionSource() }
                Box(Modifier.pressScale(interaction)) {
                    PlayPauseButton(state, onToggle = player::togglePlayPause, size = 76, filledAccent = true)
                }
                CircleIconButton(
                    Icons.Rounded.SkipNext, "Next sūrah", player::next,
                    size = 56.dp, iconSize = 32.dp, filled = false, tint = Color.White, enabled = state.hasNext,
                )
                CircleIconButton(
                    Icons.Rounded.Bedtime,
                    "Sleep timer",
                    { showSleep = true },
                    filled = false,
                    tint = if (state.sleepTimer == SleepTimer.Off) Color.White.copy(alpha = 0.5f) else colors.accent,
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(
                    text = String.format(Locale.US, "%s×", trimSpeed(state.speed)),
                    selected = state.speed != 1f,
                    leading = Icons.Rounded.Speed,
                    onClick = {
                        val next = speeds[(speeds.indexOf(state.speed).coerceAtLeast(0) + 1) % speeds.size]
                        player.setSpeed(next)
                    },
                )
                if (state.sleepTimer != SleepTimer.Off) {
                    Pill(
                        text = state.sleepTimer.label,
                        selected = true,
                        leading = Icons.Rounded.Bedtime,
                        onClick = { showSleep = true },
                    )
                }
                Pill(
                    text = "Read along",
                    selected = false,
                    leading = Icons.AutoMirrored.Rounded.MenuBook,
                    onClick = { onOpenReader(surah.number) },
                )
            }

            Spacer(Modifier.weight(0.4f))
            Box(Modifier.width(1.dp).height(12.dp))
        }
    }

    if (showSleep) {
        OptionSheet(
            title = "Sleep timer",
            options = SleepTimer.entries.map { SheetOption(it, it.label) },
            selected = state.sleepTimer,
            onSelect = player::setSleepTimer,
            onDismiss = { showSleep = false },
        )
    }
}

private fun trimSpeed(speed: Float): String =
    if (speed % 1f == 0f) String.format(Locale.US, "%.0f", speed) else String.format(Locale.US, "%.2f", speed).trimEnd('0')
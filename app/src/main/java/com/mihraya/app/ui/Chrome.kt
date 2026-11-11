package com.mihraya.app.ui

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.mihraya.app.core.designsystem.components.glass
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.art.Artwork
import com.mihraya.app.core.designsystem.components.CircleIconButton
import com.mihraya.app.core.designsystem.components.frosted
import com.mihraya.app.core.designsystem.components.pressScale
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.util.rememberHaptics
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.playback.PlaybackProgress
import com.mihraya.app.playback.PlaybackUiState
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BottomBar(
    current: TopLevel?,
    onSelect: (TopLevel) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val colors = Mihraya.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var drag by remember { mutableStateOf<Float?>(null) }
    val active = current?.ordinal ?: 0
    val select by rememberUpdatedState(onSelect)
    val committed by rememberUpdatedState(active)
    val position by animateFloatAsState(
        drag ?: active.toFloat(), spring(dampingRatio = 0.9f, stiffness = 900f), label = "glassLens",
    )
    val preview = drag?.roundToInt() ?: active
    val magnify = drag != null && ValueAnimator.areAnimatorsEnabled()
    BoxWithConstraints(
        modifier.padding(horizontal = 18.dp).widthIn(max = 520.dp).fillMaxWidth()
            .height(66.dp).frosted(hazeState, RoundedCornerShape(33.dp)).padding(horizontal = 6.dp)
            .pointerInput(rtl) {
                fun positionAt(x: Float): Float {
                    val fraction = x / size.width.coerceAtLeast(1)
                    return ((if (rtl) 1f - fraction else fraction) * TopLevel.entries.size - 0.5f)
                        .coerceIn(0f, TopLevel.entries.lastIndex.toFloat())
                }
                detectHorizontalDragGestures(
                    onDragStart = { drag = positionAt(it.x) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        drag = positionAt(change.position.x)
                    },
                    onDragEnd = {
                        val target = drag?.roundToInt()
                        drag = null
                        if (target != null && target != committed) {
                            haptics.tick()
                            select(TopLevel.entries[target])
                        }
                    },
                    onDragCancel = { drag = null },
                )
            },
    ) {
        val slot = maxWidth / TopLevel.entries.size
        Box(
            Modifier.align(Alignment.CenterStart)
                .offset { IntOffset((slot.toPx() * position).roundToInt(), 0) }
                .width(slot).height(52.dp).padding(horizontal = 2.dp)
                .glass(CircleShape, colors, colors.accentSoft),
        )
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            TopLevel.entries.forEach { item ->
                NavItem(
                    item = item, selected = item == current, highlighted = preview == item.ordinal,
                    magnified = magnify && preview == item.ordinal,
                    onClick = {
                        if (item != current) haptics.tick()
                        onSelect(item)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    item: TopLevel,
    selected: Boolean,
    highlighted: Boolean,
    magnified: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Mihraya.colors
    val interaction = remember { MutableInteractionSource() }
    val tint by animateColorAsState(if (highlighted) colors.textPrimary else colors.textSecondary, tween(220), label = "navTint")
    val scale by animateFloatAsState(if (magnified) 1.12f else 1f, spring(), label = "lensScale")
    Column(
        modifier = modifier
            .fillMaxHeight()
            .pressScale(interaction, 0.9f)
            .selectable(selected = selected, interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.height(28.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        }, contentAlignment = Alignment.Center) {
            Crossfade(highlighted, animationSpec = tween(220), label = "navIcon") { filled ->
                Icon(
                    if (filled) item.selectedIcon else item.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            item.label,
            style = Mihraya.type.caption.copy(
                fontSize = 10.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = tint,
        )
    }
}

@Composable
fun MiniPlayer(
    state: PlaybackUiState,
    hazeState: HazeState,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val colors = Mihraya.colors
    val scope = rememberCoroutineScope()
    val reciter = state.reciter ?: return
    val surah = QuranMeta.surah(state.surah ?: return)
    val progress by container.player.progress.collectAsStateWithLifecycle(PlaybackProgress(0, 0, 0))
    val shape = RoundedCornerShape(24.dp)
    val dismissible = !state.isPlaying && !state.isBuffering
    val offset = remember { Animatable(0f) }
    val threshold = with(LocalDensity.current) { 88.dp.toPx() }
    LaunchedEffect(dismissible) {
        if (!dismissible && offset.value != 0f) offset.animateTo(0f, spring())
    }

    Box(
        modifier = modifier
            .padding(horizontal = 18.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .offset { IntOffset(offset.value.roundToInt(), 0) }
            .graphicsLayer { alpha = 1f - (abs(offset.value) / (threshold * 2.2f)).coerceIn(0f, 0.65f) }
            .height(64.dp)
            .frosted(hazeState, shape)
            .pointerInput(dismissible) {
                if (!dismissible) return@pointerInput
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + dragAmount) }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (abs(offset.value) > threshold) {
                                val away = if (offset.value > 0f) threshold * 4 else -threshold * 4
                                offset.animateTo(away, tween(180))
                                container.player.stop()
                            } else {
                                offset.animateTo(0f, spring())
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f, spring()) } },
                )
            }
            .clickable(onClick = onExpand),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(
                hue = reciter.hue,
                title = surah.arabicName,
                seed = surah.number,
                cornerRadius = 12.dp,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    surah.name,
                    style = Mihraya.type.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.error ?: reciter.name,
                    style = Mihraya.type.caption,
                    color = if (state.error != null) colors.danger else colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PlayPauseButton(state, onToggle = container.player::togglePlayPause, size = 40)
            if (state.isPlaying || state.isBuffering) {
                CircleIconButton(Icons.Rounded.SkipNext, "Next sūrah", container.player::next, filled = false, enabled = state.hasNext)
            } else {
                CircleIconButton(Icons.Rounded.Close, "Close player", container.player::stop, filled = false, tint = colors.textTertiary)
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(2.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.fraction)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }
    }
}

@Composable
fun PlayPauseButton(state: PlaybackUiState, onToggle: () -> Unit, size: Int, filledAccent: Boolean = false) {
    val colors = Mihraya.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(size.dp)
            .pressScale(interaction, 0.9f)
            .clip(CircleShape)
            .then(if (filledAccent) Modifier.background(colors.accent) else Modifier)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        val tint = if (filledAccent) colors.onAccent else colors.textPrimary
        if (state.isBuffering && state.playWhenReady) {
            CircularProgressIndicator(
                color = tint,
                strokeWidth = 2.dp,
                modifier = Modifier.size((size * 0.42f).dp),
            )
        } else {
            AnimatedContent(
                targetState = state.isPlaying,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "playPause",
            ) { playing ->
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = tint,
                    modifier = Modifier.size((size * 0.55f).dp),
                )
            }
        }
    }
}

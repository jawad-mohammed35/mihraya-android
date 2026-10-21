package com.mihraya.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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

@Composable
fun BottomBar(
    current: TopLevel?,
    onSelect: (TopLevel) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier
            .padding(horizontal = 18.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .height(66.dp)
            .frosted(hazeState, RoundedCornerShape(33.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopLevel.entries.forEach { item ->
            NavItem(
                item = item,
                selected = item == current,
                onClick = {
                    if (item != current) haptics.tick()
                    onSelect(item)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavItem(item: TopLevel, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    val interaction = remember { MutableInteractionSource() }
    val tint by animateColorAsState(if (selected) colors.accent else colors.textTertiary, tween(220), label = "navTint")
    val indicator by animateFloatAsState(if (selected) 1f else 0f, tween(260), label = "navIndicator")
    Column(
        modifier = modifier
            .fillMaxHeight()
            .pressScale(interaction, 0.9f)
            .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(width = 50.dp, height = 30.dp)
                    .graphicsLayer {
                        alpha = indicator
                        scaleX = 0.7f + 0.3f * indicator
                    }
                    .clip(CircleShape)
                    .background(colors.accentSoft),
            )
            Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(22.dp))
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
    val reciter = state.reciter ?: return
    val surah = QuranMeta.surah(state.surah ?: return)
    val progress by container.player.progress.collectAsStateWithLifecycle(PlaybackProgress(0, 0, 0))
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .padding(horizontal = 18.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .height(64.dp)
            .frosted(hazeState, shape)
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

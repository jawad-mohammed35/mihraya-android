package com.mihraya.app.feature.listen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.mihraya.app.core.designsystem.components.CircleIconButton
import com.mihraya.app.core.designsystem.components.ProgressRing
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.data.audio.DownloadStatus

@Composable
fun DownloadButton(
    downloaded: Boolean,
    status: DownloadStatus?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Mihraya.colors
    when {
        downloaded -> CircleIconButton(
            Icons.Rounded.DownloadDone, "Downloaded — tap to remove", onDelete,
            modifier, size = 38.dp, iconSize = 18.dp, filled = false, tint = colors.accent,
        )
        status is DownloadStatus.Running || status == DownloadStatus.Queued -> Box(
            modifier.size(38.dp),
            contentAlignment = Alignment.Center,
        ) {
            val progress = (status as? DownloadStatus.Running)?.progress ?: 0f
            ProgressRing(progress = progress.coerceAtLeast(0.03f), modifier = Modifier.size(26.dp), stroke = 2.dp)
            CircleIconButton(Icons.Rounded.Close, "Cancel download", onCancel, size = 34.dp, iconSize = 13.dp, filled = false, tint = colors.textSecondary)
        }
        status == DownloadStatus.Failed -> CircleIconButton(
            Icons.Rounded.Refresh, "Download failed — retry", onDownload,
            modifier, size = 38.dp, iconSize = 18.dp, filled = false, tint = colors.danger,
        )
        else -> CircleIconButton(
            Icons.Rounded.Download, "Download for offline listening", onDownload,
            modifier, size = 38.dp, iconSize = 18.dp, filled = false, tint = colors.textTertiary,
        )
    }
}

@Composable
fun EqualizerBars(playing: Boolean, modifier: Modifier = Modifier) {
    val color = Mihraya.colors.accent
    val transition = rememberInfiniteTransition(label = "eq")
    val bars = listOf(520, 380, 640).map { duration ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(duration), RepeatMode.Reverse),
            label = "bar",
        )
    }
    Canvas(modifier.size(16.dp)) {
        val barWidth = size.width / 5f
        bars.forEachIndexed { index, state ->
            val fraction = if (playing) state.value else 0.3f
            val height = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(index * barWidth * 2f, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

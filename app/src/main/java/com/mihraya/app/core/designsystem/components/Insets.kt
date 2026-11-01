package com.mihraya.app.core.designsystem.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The status bar, or a camera cutout if it reaches lower. Content starts below this. */
val WindowInsets.Companion.topChrome: WindowInsets
    @Composable get() = statusBars.union(displayCutout).only(WindowInsetsSides.Top)

/** Side cutouts and navigation bars, which only matter in landscape. */
val WindowInsets.Companion.sideChrome: WindowInsets
    @Composable get() = displayCutout.union(navigationBars).only(WindowInsetsSides.Horizontal)

/** How far above the status bar's lower edge content starts to fade out. */
val StatusBarFade = 12.dp

/**
 * Hides content that scrolls up under the status bar, easing it out over the last [fade] above [top].
 * Backgrounds drawn outside this layer still show through the status bar.
 */
fun Modifier.fadeAbove(top: Dp, fade: Dp = StatusBarFade): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val end = top.toPx()
        if (end <= 0f) return@drawWithContent
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                1f to Color.Black,
                startY = (end - fade.toPx()).coerceAtLeast(0f),
                endY = end,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

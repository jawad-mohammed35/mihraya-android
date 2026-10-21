package com.mihraya.app.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.designsystem.theme.MihrayaColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** Translucent fill with a hairline border that catches light along the top edge. */
fun Modifier.glass(
    shape: Shape,
    colors: MihrayaColors,
    fill: Color = colors.glass,
): Modifier = this
    .clip(shape)
    .background(fill, shape)
    .border(
        width = 0.75.dp,
        brush = Brush.verticalGradient(
            0f to colors.glassHighlight,
            0.35f to colors.glassBorder,
            1f to colors.glassBorder.copy(alpha = colors.glassBorder.alpha * 0.6f),
        ),
        shape = shape,
    )

@Composable
fun glassHazeStyle(): HazeStyle {
    val colors = Mihraya.colors
    return HazeStyle(
        backgroundColor = colors.background,
        tints = listOf(HazeTint(colors.glassStrong)),
        blurRadius = 26.dp,
        noiseFactor = 0f,
        fallbackTint = HazeTint(colors.surfaceRaised.copy(alpha = 0.97f)),
    )
}

/** Frosted glass that blurs whatever scrolls beneath it; falls back to a near-opaque tint below Android 12. */
@Composable
fun Modifier.frosted(state: HazeState, shape: Shape): Modifier {
    val colors = Mihraya.colors
    val style = glassHazeStyle()
    return this
        .clip(shape)
        .hazeEffect(state = state, style = style)
        .border(
            width = 0.75.dp,
            brush = Brush.verticalGradient(
                0f to colors.glassHighlight,
                0.4f to colors.glassBorder,
                1f to colors.glassBorder,
            ),
            shape = shape,
        )
}

@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, pressedScale: Float = 0.97f): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    onClick: (() -> Unit)? = null,
    fill: Color? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Mihraya.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(interaction, 0.985f) else Modifier)
            .glass(shape, colors, fill ?: colors.glass)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = ripple(color = colors.textPrimary),
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

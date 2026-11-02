package com.mihraya.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

data class WidgetColors(
    val background: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val chip: Color,
)

fun WidgetTheme.colors(): WidgetColors = when (this) {
    WidgetTheme.Dark -> WidgetColors(
        background = Color(0xFF101114),
        text = Color(0xFFF3F1EC),
        muted = Color(0xFFB7B3AA),
        accent = Color(0xFFDCC394),
        onAccent = Color(0xFF1A1408),
        chip = Color(0xFF1F2025),
    )
    WidgetTheme.Light -> WidgetColors(
        background = Color(0xFFF6F4EF),
        text = Color(0xFF161513),
        muted = Color(0xFF6E6A62),
        accent = Color(0xFF8C6A32),
        onAccent = Color(0xFFFFFBF4),
        chip = Color(0xFFE8E3D8),
    )
    WidgetTheme.Glass -> WidgetColors(
        background = Color(red = 0.07f, green = 0.07f, blue = 0.08f, alpha = 0.72f),
        text = Color(0xFFF7F4EE),
        muted = Color(0xFFE4DED2),
        accent = Color(0xFFE4C98A),
        onAccent = Color(0xFF1A1408),
        chip = Color(red = 1f, green = 1f, blue = 1f, alpha = 0.14f),
    )
}

fun WidgetColors.style(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    color: Color = text,
    align: TextAlign? = null,
) = TextStyle(
    color = ColorProvider(color),
    fontSize = size.sp,
    fontWeight = weight,
    textAlign = align,
)

/** Layout breakpoints in dp, width to height. The launcher picks the largest one that fits the widget. */
fun responsive(vararg sizes: Pair<Int, Int>): SizeMode =
    SizeMode.Responsive(sizes.mapTo(LinkedHashSet()) { (width, height) -> DpSize(width.dp, height.dp) })

@Composable
private fun framePadding(): Dp {
    val size = LocalSize.current
    return when {
        size.width < 100.dp || size.height < 90.dp -> 10.dp
        size.width >= 250.dp && size.height >= 180.dp -> 16.dp
        else -> 14.dp
    }
}

/** The space inside [WidgetFrame]'s padding for the current breakpoint. */
@Composable
fun innerSize(): DpSize {
    val size = LocalSize.current
    val padding = framePadding() * 2
    return DpSize(size.width - padding, size.height - padding)
}

@Composable
fun WidgetFrame(
    theme: WidgetTheme,
    onClick: Action? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = theme.colors()
    var modifier = GlanceModifier.fillMaxSize().cornerRadius(22.dp).background(colors.background).padding(framePadding())
    if (onClick != null) modifier = modifier.clickable(onClick)
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Column(GlanceModifier.fillMaxSize()) { content() }
    }
}

@Composable
fun WidgetTitle(text: String, colors: WidgetColors) {
    Text(text, style = colors.style(11, FontWeight.Medium, colors.accent), maxLines = 1)
}

@Composable
fun WidgetPill(
    text: String,
    colors: WidgetColors,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
    filled: Boolean = true,
) {
    Box(
        modifier
            .height(40.dp)
            .cornerRadius(20.dp)
            .background(if (filled) colors.accent else colors.chip)
            .padding(horizontal = 16.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = colors.style(13, FontWeight.Bold, if (filled) colors.onAccent else colors.text), maxLines = 1)
    }
}

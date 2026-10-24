package com.mihraya.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

data class WidgetColors(
    val background: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
)

fun WidgetTheme.colors(): WidgetColors = when (this) {
    WidgetTheme.Dark -> WidgetColors(
        background = Color(0xFF101114),
        text = Color(0xFFF3F1EC),
        muted = Color(0xFFB7B3AA),
        accent = Color(0xFFDCC394),
        onAccent = Color(0xFF1A1408),
    )
    WidgetTheme.Light -> WidgetColors(
        background = Color(0xFFF6F4EF),
        text = Color(0xFF161513),
        muted = Color(0xFF6E6A62),
        accent = Color(0xFF8C6A32),
        onAccent = Color(0xFFFFFBF4),
    )
    WidgetTheme.Glass -> WidgetColors(
        background = Color(red = 0.07f, green = 0.07f, blue = 0.08f, alpha = 0.72f),
        text = Color(0xFFF7F4EE),
        muted = Color(0xFFE4DED2),
        accent = Color(0xFFE4C98A),
        onAccent = Color(0xFF1A1408),
    )
}

fun WidgetColors.style(size: Int, weight: FontWeight = FontWeight.Normal, color: Color = text) = TextStyle(
    color = ColorProvider(color),
    fontSize = size.sp,
    fontWeight = weight,
)

@Composable
fun WidgetFrame(
    theme: WidgetTheme,
    onClick: Action? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = theme.colors()
    var modifier = GlanceModifier.fillMaxSize().cornerRadius(22.dp).background(colors.background).padding(14.dp)
    if (onClick != null) modifier = modifier.clickable(onClick)
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Column(GlanceModifier.fillMaxSize()) { content() }
    }
}

@Composable
fun WidgetTitle(text: String, colors: WidgetColors) {
    Text(text, style = colors.style(11, FontWeight.Medium, colors.accent), maxLines = 1)
}

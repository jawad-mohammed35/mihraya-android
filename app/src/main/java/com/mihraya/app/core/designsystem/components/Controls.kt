package com.mihraya.app.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mihraya.app.core.designsystem.theme.Mihraya
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    tint: Color = Mihraya.colors.textPrimary,
    filled: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = Mihraya.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .pressScale(interaction, 0.92f)
            .then(if (filled) Modifier.glass(CircleShape, colors) else Modifier.clip(CircleShape))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = colors.textPrimary),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = Mihraya.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .height(52.dp)
            .pressScale(interaction)
            .clip(CircleShape)
            .background(if (enabled) colors.accent else colors.accent.copy(alpha = 0.35f))
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = colors.onAccent),
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = colors.onAccent,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = Mihraya.type.titleSmall, color = colors.onAccent, maxLines = 1)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    tint: Color = Mihraya.colors.textPrimary,
) {
    val colors = Mihraya.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .height(48.dp)
            .pressScale(interaction)
            .glass(CircleShape, colors)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = colors.textPrimary),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val contentColor = if (enabled) tint else tint.copy(alpha = 0.35f)
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Mihraya.type.label.copy(fontWeight = FontWeight.SemiBold), color = contentColor, maxLines = 1)
    }
}

@Composable
fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
) {
    val colors = Mihraya.colors
    val background by animateColorAsState(if (selected) colors.accent else colors.glass, tween(220), label = "pill")
    val content by animateColorAsState(if (selected) colors.onAccent else colors.textSecondary, tween(220), label = "pillText")
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .height(36.dp)
            .pressScale(interaction, 0.95f)
            .glass(CircleShape, colors, background)
            .clickable(interaction, ripple(color = colors.textPrimary), role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = Mihraya.type.label, color = content, maxLines = 1)
    }
}

/** Pass a negative [selectedIndex] when no option applies, e.g. for mixed values. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Mihraya.colors
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .glass(CircleShape, colors)
            .padding(4.dp),
    ) {
        val segment = maxWidth / options.size
        val offset by animateDpAsState(
            targetValue = segment * selectedIndex.coerceAtLeast(0),
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
            label = "segment",
        )
        if (selectedIndex >= 0) {
            Box(
                Modifier
                    .offset(x = offset)
                    .width(segment)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(colors.accentSoft),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(
                    if (selected) colors.accent else colors.textSecondary,
                    tween(200),
                    label = "segmentText",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(role = Role.Tab) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = Mihraya.type.label.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Mihraya.colors
    val focus = LocalFocusManager.current
    Row(
        modifier
            .height(50.dp)
            .glass(CircleShape, colors)
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, style = Mihraya.type.body, color = colors.textTertiary, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = Mihraya.type.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when {
            trailing != null -> trailing()
            value.isNotEmpty() -> CircleIconButton(Icons.Rounded.Close, "Clear", { onValueChange("") }, size = 38.dp, iconSize = 18.dp, filled = false)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.uppercase(),
            style = Mihraya.type.overline,
            color = Mihraya.colors.textTertiary,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Mihraya.type.headline, color = Mihraya.colors.textPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = Mihraya.type.bodySmall, color = Mihraya.colors.textTertiary)
            }
        }
        trailing?.invoke(this)
    }
}

@Composable
fun TopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(60.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                title,
                style = Mihraya.type.title,
                color = Mihraya.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = Mihraya.type.caption,
                    color = Mihraya.colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier, startIndent: Dp = 0.dp) {
    Box(
        modifier
            .padding(start = startIndent)
            .fillMaxWidth()
            .height(0.75.dp)
            .background(Mihraya.colors.divider),
    )
}

@Composable
fun MihrayaSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val colors = Mihraya.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onAccent,
            checkedTrackColor = colors.accent,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = colors.textSecondary,
            uncheckedTrackColor = colors.glass,
            uncheckedBorderColor = colors.glassBorder,
        ),
    )
}

@Composable
fun MihrayaSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = Mihraya.colors
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = colors.accent,
            activeTrackColor = colors.accent,
            inactiveTrackColor = colors.glassBorder,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
    )
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 3.dp,
    color: Color = Mihraya.colors.accent,
    trackColor: Color = Mihraya.colors.glassBorder,
) {
    Canvas(modifier) {
        val width = stroke.toPx()
        val inset = width / 2
        val arcSize = Size(size.width - width, size.height - width)
        drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(width))
        if (progress > 0f) {
            drawArc(
                color,
                -90f,
                360f * progress.coerceIn(0f, 1f),
                false,
                Offset(inset, inset),
                arcSize,
                style = Stroke(width, cap = StrokeCap.Round),
            )
        }
    }
}

/** Eight-pointed star (two overlapping squares), the classic khatam form. */
object StarShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = minOf(size.width, size.height) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val path = Path()
        for (i in 0 until 16) {
            val angle = Math.toRadians(i * 22.5 - 90.0)
            val radius = if (i % 2 == 0) r else r * 0.7654f
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
fun StarBadge(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    highlighted: Boolean = false,
) {
    val colors = Mihraya.colors
    val stroke = if (highlighted) colors.accent else colors.textTertiary.copy(alpha = 0.6f)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val outline = StarShape.createOutline(this.size, layoutDirection, this)
            if (outline is Outline.Generic) {
                if (highlighted) drawPath(outline.path, colors.accentSoft)
                drawPath(outline.path, stroke, style = Stroke(1.dp.toPx()))
            }
        }
        Text(
            number.toString(),
            style = Mihraya.type.caption.copy(fontSize = if (number >= 100) 10.sp else 11.sp, fontWeight = FontWeight.SemiBold),
            color = if (highlighted) colors.accent else colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .glass(CircleShape, Mihraya.colors),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Mihraya.colors.accent, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = Mihraya.type.title, color = Mihraya.colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = Mihraya.type.bodySmall, color = Mihraya.colors.textSecondary, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

@Composable
fun MihrayaDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    message: String? = null,
    dismissText: String = "Cancel",
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    val colors = Mihraya.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceRaised,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, style = Mihraya.type.title, color = colors.textPrimary) },
        text = {
            Column {
                if (message != null) Text(message, style = Mihraya.type.body, color = colors.textSecondary)
                if (content != null) {
                    if (message != null) Spacer(Modifier.height(16.dp))
                    content()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(
                    confirmText,
                    style = Mihraya.type.label.copy(fontWeight = FontWeight.SemiBold),
                    color = if (destructive) colors.danger else colors.accent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, style = Mihraya.type.label, color = colors.textSecondary)
            }
        },
    )
}

@Composable
fun animatedFloat(target: Float, label: String): Float {
    val value by animateFloatAsState(target, spring(stiffness = Spring.StiffnessLow), label = label)
    return value
}

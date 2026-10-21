package com.mihraya.app.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mihraya.app.core.designsystem.theme.Mihraya

@Composable
fun SettingsGroup(
    title: String?,
    modifier: Modifier = Modifier,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            SectionLabel(title)
            Spacer(Modifier.height(10.dp))
        }
        GlassCard(contentPadding = PaddingValues(vertical = 4.dp), content = content)
        if (footer != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                footer,
                style = Mihraya.type.caption,
                color = Mihraya.colors.textTertiary,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Mihraya.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = colors.accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = Mihraya.type.bodySmall, color = colors.textTertiary)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SettingsValueRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
) {
    SettingsRow(title, modifier, subtitle, icon, onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                style = Mihraya.type.bodySmall,
                color = Mihraya.colors.textSecondary,
                textAlign = TextAlign.End,
                maxLines = 2,
                modifier = Modifier.widthIn(max = 170.dp),
            )
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                null,
                tint = Mihraya.colors.textTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SettingsRow(title, modifier, subtitle, icon, onClick = { if (enabled) onCheckedChange(!checked) }) {
        MihrayaSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun SettingsStepperRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    format: (Int) -> String = { it.toString() },
) {
    SettingsRow(title, modifier, subtitle) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CircleIconButton(
                Icons.Rounded.Remove,
                "Decrease",
                onClick = { onValueChange((value - 1).coerceIn(range)) },
                size = 32.dp,
                iconSize = 16.dp,
                enabled = value > range.first,
            )
            Text(
                format(value),
                style = Mihraya.type.label.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = if (value == 0) Mihraya.colors.textSecondary else Mihraya.colors.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(54.dp),
            )
            CircleIconButton(
                Icons.Rounded.Add,
                "Increase",
                onClick = { onValueChange((value + 1).coerceIn(range)) },
                size = 32.dp,
                iconSize = 16.dp,
                enabled = value < range.last,
            )
        }
    }
}

/** A tinted card that explains a missing permission or problem and offers one way to fix it. */
@Composable
fun NoticeCard(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    warning: Boolean = true,
) {
    val colors = Mihraya.colors
    val tint = if (warning) colors.danger else colors.accent
    GlassCard(modifier.fillMaxWidth(), fill = tint.copy(alpha = if (colors.isDark) 0.10f else 0.08f), contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(20.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = Mihraya.type.titleSmall, color = colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(message, style = Mihraya.type.bodySmall, color = colors.textSecondary)
                if (actionText != null && onAction != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        actionText,
                        style = Mihraya.type.label.copy(fontWeight = FontWeight.SemiBold),
                        color = tint,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = onAction)
                            .padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsDivider(indent: Boolean = false) {
    HairlineDivider(Modifier.padding(end = 16.dp), startIndent = if (indent) 64.dp else 16.dp)
}

@Composable
fun RadioDot(selected: Boolean) {
    val colors = Mihraya.colors
    val ring by animateColorAsState(if (selected) colors.accent else colors.textTertiary, tween(180), label = "radio")
    Box(
        Modifier
            .size(22.dp)
            .border(1.5.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(11.dp).clip(CircleShape).background(colors.accent))
    }
}

data class SheetOption<T>(val value: T, val title: String, val subtitle: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MihrayaSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Mihraya.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surfaceRaised,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.textTertiary.copy(alpha = 0.5f)),
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            if (title != null) {
                Text(
                    title,
                    style = Mihraya.type.title,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
            content()
        }
    }
}

@Composable
fun <T> OptionSheet(
    title: String,
    options: List<SheetOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    MihrayaSheet(onDismiss = onDismiss, title = title) {
        LazyColumn(Modifier.fillMaxWidth()) {
            items(options) { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.RadioButton) {
                            onSelect(option.value)
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(option.title, style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium), color = Mihraya.colors.textPrimary)
                        if (option.subtitle != null) {
                            Text(option.subtitle, style = Mihraya.type.bodySmall, color = Mihraya.colors.textTertiary)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    RadioDot(option.value == selected)
                }
            }
        }
    }
}

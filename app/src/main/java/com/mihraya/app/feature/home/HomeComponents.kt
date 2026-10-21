package com.mihraya.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mihraya.app.core.designsystem.components.CircleIconButton
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.data.prayer.Salah
import com.mihraya.app.data.settings.AlertMode

fun AlertMode.icon(): ImageVector = when (this) {
    AlertMode.Adhan -> Icons.AutoMirrored.Rounded.VolumeUp
    AlertMode.Tone -> Icons.Rounded.NotificationsActive
    AlertMode.Silent -> Icons.Rounded.Notifications
    AlertMode.Off -> Icons.Rounded.NotificationsOff
}

fun alertOptionsFor(salah: Salah): List<AlertMode> =
    if (salah == Salah.Sunrise) listOf(AlertMode.Tone, AlertMode.Silent, AlertMode.Off) else AlertMode.entries

@Composable
fun PrayerAlertButton(
    salah: Salah,
    mode: AlertMode,
    onChange: (AlertMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = Mihraya.colors
    Box(modifier) {
        CircleIconButton(
            icon = mode.icon(),
            contentDescription = "${salah.label} alert: ${mode.label}",
            onClick = { expanded = true },
            size = 36.dp,
            iconSize = 17.dp,
            filled = false,
            tint = if (mode == AlertMode.Off) colors.textTertiary else colors.accent,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = colors.surfaceRaised,
        ) {
            alertOptionsFor(salah).forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                option.label,
                                style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium),
                                color = if (option == mode) colors.accent else colors.textPrimary,
                            )
                            Text(option.description, style = Mihraya.type.caption, color = colors.textTertiary)
                        }
                    },
                    leadingIcon = {
                        Icon(
                            option.icon(),
                            null,
                            tint = if (option == mode) colors.accent else colors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = {
                        expanded = false
                        onChange(option)
                    },
                )
            }
        }
    }
}

@Composable
fun ProgressLine(fraction: Float, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(CircleShape)
            .background(colors.glassBorder),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(3.dp)
                .clip(CircleShape)
                .background(colors.accent),
        )
    }
}

@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, size: Int = 38) {
    val colors = Mihraya.colors
    Box(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.32f).dp))
            .background(colors.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = colors.accent, modifier = Modifier.size((size * 0.5f).dp))
    }
}

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier, alignEnd: Boolean = false) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = Mihraya.type.caption, color = Mihraya.colors.textTertiary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = Mihraya.type.label.copy(fontFeatureSettings = "tnum"), color = Mihraya.colors.textSecondary)
    }
}

@Composable
fun DotSeparator() {
    Row(horizontalArrangement = Arrangement.Center) {
        Spacer(Modifier.width(8.dp))
        Box(Modifier.padding(top = 1.dp).size(3.dp).clip(CircleShape).background(Mihraya.colors.textTertiary))
        Spacer(Modifier.width(8.dp))
    }
}

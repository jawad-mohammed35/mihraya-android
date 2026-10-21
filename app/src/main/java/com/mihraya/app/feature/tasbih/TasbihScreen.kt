package com.mihraya.app.feature.tasbih

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.components.CircleIconButton
import com.mihraya.app.core.designsystem.components.EmptyState
import com.mihraya.app.core.designsystem.components.HairlineDivider
import com.mihraya.app.core.designsystem.components.MihrayaDialog
import com.mihraya.app.core.designsystem.components.MihrayaSheet
import com.mihraya.app.core.designsystem.components.Pill
import com.mihraya.app.core.designsystem.components.SecondaryButton
import com.mihraya.app.core.designsystem.components.TopBar
import com.mihraya.app.core.designsystem.components.glass
import com.mihraya.app.core.designsystem.components.pressScale
import com.mihraya.app.core.designsystem.icons.MihrayaIcons
import com.mihraya.app.core.designsystem.theme.ArabicFonts
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.util.rememberHaptics
import com.mihraya.app.data.library.DhikrPreset
import com.mihraya.app.data.library.TasbihSession
import com.mihraya.app.ui.mihrayaViewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val targetPresets = listOf(33, 99, 100)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasbihScreen(onBack: () -> Unit) {
    val vm = mihrayaViewModel { TasbihViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val presets by vm.presets.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val colors = Mihraya.colors
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val pulse = remember { Animatable(0f) }

    var showHistory by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showCustomTarget by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<DhikrPreset?>(null) }

    val preset = presets.firstOrNull { it.id == state.presetId } ?: presets.first()
    val progress by animateFloatAsState(
        targetValue = if (state.target > 0) state.count.toFloat() / state.target else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "tasbihProgress",
    )
    val startOfDay = remember(sessions) {
        java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val today = sessions.filter { it.endedAt >= startOfDay }.sumOf { it.count } +
        if (state.startedAt >= startOfDay && state.count < state.target) state.count else 0

    val onTap: () -> Unit = {
        when (vm.increment()) {
            TapFeedback.Tick -> haptics.tap()
            TapFeedback.Milestone -> haptics.milestone()
            TapFeedback.Completed -> {
                haptics.confirm()
                scope.launch {
                    pulse.snapTo(1f)
                    pulse.animateTo(0f, tween(900))
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        TopBar(
            title = "Tasbīḥ",
            subtitle = "Today · $today",
            onBack = onBack,
            actions = { CircleIconButton(Icons.Rounded.History, "History", { showHistory = true }) },
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(presets, key = { it.id }) { p ->
                DhikrChip(
                    preset = p,
                    selected = p.id == preset.id,
                    onClick = { vm.select(p) },
                    onLongClick = {
                        if (p.custom) {
                            haptics.confirm()
                            confirmDelete = p
                        }
                    },
                )
            }
            item(key = "add") {
                Pill("Custom", selected = false, leading = Icons.Rounded.Add, onClick = { showAdd = true })
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (preset.arabic.isNotBlank()) {
                Text(
                    preset.arabic,
                    style = Mihraya.type.body.copy(
                        fontFamily = ArabicFonts.Amiri,
                        fontSize = 30.sp,
                        lineHeight = 52.sp,
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Center,
                    ),
                    color = colors.textPrimary,
                )
            }
            Text(preset.transliteration, style = Mihraya.type.title, color = colors.accent, textAlign = TextAlign.Center)
            if (preset.meaning.isNotBlank()) {
                Text(preset.meaning, style = Mihraya.type.bodySmall, color = colors.textTertiary, textAlign = TextAlign.Center)
            }
        }

        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val ringSize = min(maxWidth.value, maxHeight.value).coerceAtMost(340f).dp * 0.92f
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .size(ringSize)
                    .pressScale(interaction, 0.96f)
                    .clip(CircleShape)
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onTap,
                    )
                    .semantics { contentDescription = "Count ${preset.transliteration}. ${state.count} of ${state.target}" },
                contentAlignment = Alignment.Center,
            ) {
                CounterRing(progress = progress, count = state.count, target = state.target, pulse = pulse.value)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedContent(
                        targetState = state.count,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInVertically(tween(180)) { it / 3 } + fadeIn(tween(180)))
                                    .togetherWith(slideOutVertically(tween(160)) { -it / 3 } + fadeOut(tween(120)))
                            } else {
                                (slideInVertically(tween(180)) { -it / 3 } + fadeIn(tween(180)))
                                    .togetherWith(slideOutVertically(tween(160)) { it / 3 } + fadeOut(tween(120)))
                            }
                        },
                        label = "count",
                    ) { count ->
                        Text(
                            count.toString(),
                            style = Mihraya.type.display.copy(fontSize = 76.sp, lineHeight = 82.sp),
                            color = if (count == state.target && count > 0) colors.accent else colors.textPrimary,
                        )
                    }
                    Text("of ${state.target}", style = Mihraya.type.label, color = colors.textTertiary)
                    if (state.rounds > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (state.rounds == 1) "1 round complete" else "${state.rounds} rounds complete",
                            style = Mihraya.type.caption,
                            color = colors.accent,
                        )
                    }
                }
            }
        }

        Text(
            "Tap the circle to count",
            style = Mihraya.type.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            targetPresets.forEach { target ->
                Pill("$target", selected = state.target == target, onClick = { vm.setTarget(target) })
            }
            Pill(
                if (state.target !in targetPresets) "${state.target}" else "Custom",
                selected = state.target !in targetPresets,
                onClick = { showCustomTarget = true },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            SecondaryButton("Undo", onClick = vm::undo, icon = Icons.AutoMirrored.Rounded.Undo, enabled = state.count > 0)
            SecondaryButton("Reset", onClick = { confirmReset = true }, icon = Icons.Rounded.RestartAlt, enabled = state.count > 0 || state.rounds > 0)
        }
        Spacer(Modifier.height(12.dp))
    }

    if (confirmReset) {
        MihrayaDialog(
            title = "Reset counter?",
            message = "Your current count will be saved to history.",
            onDismiss = { confirmReset = false },
            confirmText = "Reset",
            destructive = true,
            onConfirm = {
                confirmReset = false
                vm.reset()
            },
        )
    }

    confirmDelete?.let { p ->
        MihrayaDialog(
            title = "Delete “${p.transliteration}”?",
            onDismiss = { confirmDelete = null },
            confirmText = "Delete",
            destructive = true,
            onConfirm = {
                confirmDelete = null
                vm.removeCustom(p)
            },
        )
    }

    if (showCustomTarget) {
        NumberDialog(
            title = "Custom target",
            initial = state.target.takeIf { it !in targetPresets },
            onDismiss = { showCustomTarget = false },
            onConfirm = {
                showCustomTarget = false
                vm.setTarget(it)
            },
        )
    }

    if (showAdd) {
        AddDhikrDialog(
            onDismiss = { showAdd = false },
            onSave = { arabic, name, meaning, target ->
                showAdd = false
                vm.addCustom(arabic, name, meaning, target)
            },
        )
    }

    if (showHistory) {
        HistorySheet(sessions = sessions, onClear = vm::clearHistory, onDismiss = { showHistory = false })
    }
}

@Composable
private fun CounterRing(progress: Float, count: Int, target: Int, pulse: Float) {
    val colors = Mihraya.colors
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 5.dp.toPx()
        val radius = size.minDimension / 2f - 14.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            Brush.radialGradient(
                listOf(colors.accent.copy(alpha = 0.05f + 0.25f * pulse), Color.Transparent),
                center = center,
                radius = radius * 1.1f,
            ),
            radius = radius * 1.1f,
            center = center,
        )
        drawCircle(colors.glass, radius = radius - stroke * 2, center = center)
        drawCircle(colors.glassBorder, radius = radius, center = center, style = Stroke(stroke))
        drawArc(
            color = colors.accent,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )

        val beads = min(target, 33)
        val filled = if (target > 0) ((count.toFloat() / target) * beads).toInt() else 0
        val beadRadius = radius + 10.dp.toPx()
        for (i in 0 until beads) {
            val angle = Math.toRadians(i * 360.0 / beads - 90.0)
            drawCircle(
                color = if (i < filled) colors.accent else colors.textTertiary.copy(alpha = 0.35f),
                radius = if (i < filled) 2.6.dp.toPx() else 1.8.dp.toPx(),
                center = Offset(center.x + beadRadius * cos(angle).toFloat(), center.y + beadRadius * sin(angle).toFloat()),
            )
        }
    }
}

@Composable
private fun NumberDialog(title: String, initial: Int?, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(initial?.toString().orEmpty()) }
    val value = text.toIntOrNull()
    MihrayaDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = "Set",
        confirmEnabled = value != null && value in 1..100_000,
        onConfirm = { value?.let(onConfirm) },
    ) {
        MihrayaTextField(text, { text = it.filter(Char::isDigit).take(6) }, "Count", KeyboardType.Number)
    }
}

@Composable
private fun AddDhikrDialog(onDismiss: () -> Unit, onSave: (String, String, String, Int) -> Unit) {
    var arabic by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("33") }
    val targetValue = target.toIntOrNull()
    MihrayaDialog(
        title = "New dhikr",
        onDismiss = onDismiss,
        confirmText = "Save",
        confirmEnabled = name.isNotBlank() && targetValue != null && targetValue > 0,
        onConfirm = { onSave(arabic, name, meaning, targetValue ?: 33) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MihrayaTextField(name, { name = it.take(60) }, "Name or transliteration")
            MihrayaTextField(arabic, { arabic = it.take(120) }, "Arabic (optional)")
            MihrayaTextField(meaning, { meaning = it.take(120) }, "Meaning (optional)")
            MihrayaTextField(target, { target = it.filter(Char::isDigit).take(6) }, "Target", KeyboardType.Number)
        }
    }
}

@Composable
private fun MihrayaTextField(value: String, onChange: (String) -> Unit, label: String, keyboard: KeyboardType = KeyboardType.Text) {
    val colors = Mihraya.colors
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.glassBorder,
            focusedLabelColor = colors.accent,
            cursorColor = colors.accent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun HistorySheet(sessions: List<TasbihSession>, onClear: () -> Unit, onDismiss: () -> Unit) {
    val colors = Mihraya.colors
    val timeFormat = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
    MihrayaSheet(onDismiss = onDismiss, title = "History") {
        if (sessions.isEmpty()) {
            EmptyState(MihrayaIcons.Tasbih, "No sessions yet", "Completed rounds and resets are recorded here.")
            return@MihrayaSheet
        }
        val total = sessions.sumOf { it.count }
        Text(
            "$total total · ${sessions.size} sessions",
            style = Mihraya.type.bodySmall,
            color = colors.textTertiary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        LazyColumn(Modifier.heightIn(max = 460.dp).padding(top = 8.dp)) {
            val grouped = sessions.groupBy {
                DateUtils.getRelativeTimeSpanString(it.endedAt, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString()
            }
            grouped.forEach { (day, items) ->
                item(key = "d$day") {
                    Text(
                        day.uppercase(),
                        style = Mihraya.type.overline,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 6.dp),
                    )
                }
                items(items, key = { "s${it.endedAt}-${it.label}-${it.count}" }) { session ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (session.count >= session.target) colors.accent else colors.textTertiary),
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 14.dp),
                        ) {
                            Text(session.label, style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                            Text(timeFormat.format(Date(session.endedAt)), style = Mihraya.type.caption, color = colors.textTertiary)
                        }
                        Text(
                            "${session.count}/${session.target}",
                            style = Mihraya.type.label.copy(fontFeatureSettings = "tnum"),
                            color = if (session.count >= session.target) colors.accent else colors.textSecondary,
                        )
                    }
                    HairlineDivider(Modifier.padding(horizontal = 24.dp))
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            SecondaryButton("Clear history", onClick = onClear, tint = colors.danger)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DhikrChip(preset: DhikrPreset, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = Mihraya.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .height(36.dp)
            .pressScale(interaction, 0.95f)
            .glass(CircleShape, colors, if (selected) colors.accent else colors.glass)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            preset.transliteration,
            style = Mihraya.type.label,
            color = if (selected) colors.onAccent else colors.textSecondary,
            maxLines = 1,
        )
    }
}

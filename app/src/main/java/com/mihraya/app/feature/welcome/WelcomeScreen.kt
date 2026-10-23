package com.mihraya.app.feature.welcome

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.components.MihrayaTextField
import com.mihraya.app.core.designsystem.components.NoticeCard
import com.mihraya.app.core.designsystem.components.PrimaryButton
import com.mihraya.app.core.designsystem.components.StarShape
import com.mihraya.app.core.designsystem.components.glass
import com.mihraya.app.core.designsystem.icons.MihrayaIcons
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.feature.more.MAX_NAME_LENGTH
import com.mihraya.app.feature.settings.CitySearch
import com.mihraya.app.ui.LocalAppContainer
import com.mihraya.app.ui.rememberLocationRequest

private const val STEP_INTRO = 0
private const val STEP_NAME = 1
private const val STEP_LOCATION = 2
private const val STEP_ALERTS = 3
private const val STEP_COUNT = 4

@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(STEP_INTRO) }
    val colors = Mihraya.colors

    BackHandler(enabled = step > STEP_INTRO) { step -= 1 }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Canvas(Modifier.fillMaxWidth().height(420.dp)) {
            drawRect(
                Brush.radialGradient(
                    listOf(colors.accent.copy(alpha = if (colors.isDark) 0.12f else 0.10f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.32f),
                    radius = size.width * 0.75f,
                ),
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepDots(step, Modifier.padding(top = 20.dp))
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(360)) { if (forward) it / 6 else -it / 6 } + fadeIn(tween(360))) togetherWith
                        (slideOutHorizontally(tween(260)) { if (forward) -it / 6 else it / 6 } + fadeOut(tween(200)))
                },
                modifier = Modifier.weight(1f),
                label = "welcome",
            ) { current ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        when (current) {
                            STEP_INTRO -> IntroStep(onNext = { step = STEP_NAME })
                            STEP_NAME -> NameStep(onNext = { step = STEP_LOCATION })
                            STEP_LOCATION -> LocationStep(onNext = { step = STEP_ALERTS })
                            else -> AlertsStep(onDone = onDone)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDots(step: Int, modifier: Modifier = Modifier) {
    val colors = Mihraya.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(STEP_COUNT) { index ->
            val width by animateDpAsState(if (index == step) 22.dp else 6.dp, tween(300), label = "dot")
            val color by animateColorAsState(if (index <= step) colors.accent else colors.glassBorder, tween(300), label = "dotColor")
            Box(Modifier.size(width = width, height = 6.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun Emblem(icon: ImageVector) {
    val colors = Mihraya.colors
    Box(Modifier.size(104.dp).glass(StarShape, colors, colors.accentSoft), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = colors.accent, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun ColumnScope.StepHeader(icon: ImageVector, title: String, message: String) {
    val colors = Mihraya.colors
    Spacer(Modifier.height(56.dp))
    Emblem(icon)
    Spacer(Modifier.height(32.dp))
    Text(title, style = Mihraya.type.headline, color = colors.textPrimary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(10.dp))
    Text(message, style = Mihraya.type.body, color = colors.textSecondary, textAlign = TextAlign.Center)
}

@Composable
private fun ColumnScope.IntroStep(onNext: () -> Unit) {
    val colors = Mihraya.colors
    StepHeader(
        icon = MihrayaIcons.Home,
        title = "Mihraya",
        message = "A calm companion for prayer, the Quran and remembrance.",
    )
    Spacer(Modifier.height(40.dp))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Feature(MihrayaIcons.Crescent, "Prayer times & Adhan", "Accurate for your location, with gentle alerts")
        Feature(MihrayaIcons.Quran, "The Holy Quran", "Uthmani and IndoPak scripts, translations, bookmarks")
        Feature(MihrayaIcons.Listen, "Recitations", "Seven renowned reciters, online or offline")
        Feature(MihrayaIcons.Qibla, "Qibla & Tasbīḥ", "A precise compass and a quiet counter")
    }
    Spacer(Modifier.height(44.dp))
    PrimaryButton("Begin", onClick = onNext, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp))
    Text(
        "No account. No ads. Everything stays on your device.",
        style = Mihraya.type.caption,
        color = colors.textTertiary,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun Feature(icon: ImageVector, title: String, detail: String) {
    val colors = Mihraya.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).glass(CircleShape, colors),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = colors.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = Mihraya.type.body.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
            Text(detail, style = Mihraya.type.bodySmall, color = colors.textTertiary)
        }
    }
}

@Composable
private fun ColumnScope.NameStep(onNext: () -> Unit) {
    val container = LocalAppContainer.current
    val settings by container.settings.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf(settings?.userName.orEmpty()) }
    val focus = LocalFocusManager.current
    val save = {
        focus.clearFocus()
        container.settings.edit { it.copy(userName = name.trim()) }
        onNext()
    }
    StepHeader(
        icon = Icons.Rounded.Person,
        title = "What should we call you?",
        message = "Mihraya will greet you by name. You can change it any time in More.",
    )
    Spacer(Modifier.height(36.dp))
    MihrayaTextField(
        value = name,
        onValueChange = { name = it.take(MAX_NAME_LENGTH) },
        label = "Your name",
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { save() }),
    )
    Spacer(Modifier.height(20.dp))
    PrimaryButton("Continue", onClick = save, modifier = Modifier.fillMaxWidth(), enabled = name.isNotBlank())
    Spacer(Modifier.height(12.dp))
    SkipButton("Skip") {
        focus.clearFocus()
        onNext()
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun ColumnScope.LocationStep(onNext: () -> Unit) {
    val container = LocalAppContainer.current
    val request = rememberLocationRequest(onSuccess = { onNext() })
    StepHeader(
        icon = Icons.Rounded.LocationOn,
        title = "Where do you pray?",
        message = "Prayer times and the Qibla are calculated from your location, on this device.",
    )
    Spacer(Modifier.height(36.dp))
    PrimaryButton(
        text = if (request.loading) "Locating…" else "Use my location",
        onClick = request::request,
        icon = Icons.Rounded.MyLocation,
        loading = request.loading,
        modifier = Modifier.fillMaxWidth(),
    )
    request.error?.let { message ->
        Spacer(Modifier.height(16.dp))
        NoticeCard(Icons.Rounded.LocationOn, "Couldn't detect your location", message)
    }
    Spacer(Modifier.height(28.dp))
    CitySearch(
        onPick = { place ->
            container.applyLocation(place.toSaved(), keepAutomatic = false)
            onNext()
        },
    )
    Spacer(Modifier.height(20.dp))
    SkipButton("Set up later", onNext)
}

@Composable
private fun ColumnScope.AlertsStep(onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val settings by container.settings.state.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !NotificationManagerCompat.from(context).areNotificationsEnabled()

    StepHeader(
        icon = Icons.Rounded.NotificationsActive,
        title = "Never miss a prayer",
        message = "Mihraya can play the Adhan or send a quiet alert at each prayer time. You choose per prayer.",
    )
    val s = settings
    if (s?.location != null) {
        Spacer(Modifier.height(24.dp))
        Text(
            "Times for ${s.location.name} use ${s.prayer.method.title} with ${s.prayer.asr.title} Asr. You can change this any time in More.",
            style = Mihraya.type.bodySmall,
            color = Mihraya.colors.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(36.dp))
    PrimaryButton(
        text = if (needsPermission) "Allow notifications" else "Start",
        onClick = {
            if (needsPermission) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else onDone()
        },
        icon = if (needsPermission) Icons.Rounded.NotificationsActive else null,
        modifier = Modifier.fillMaxWidth(),
    )
    if (needsPermission) {
        Spacer(Modifier.height(12.dp))
        SkipButton("Not now", onDone)
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun SkipButton(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = Mihraya.type.label.copy(fontWeight = FontWeight.SemiBold),
        color = Mihraya.colors.textSecondary,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    )
}

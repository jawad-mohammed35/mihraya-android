package com.mihraya.app.core.designsystem.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.designsystem.theme.overlaySurface

/** A physical right-side panel; its contents retain the app's text direction. */
@Composable
fun MihrayaSideSheet(onDismiss: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit) {
    val transition = remember { MutableTransitionState(false).apply { targetState = true } }
    val dismiss = { transition.targetState = false }
    val latestDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(transition.isIdle, transition.currentState) {
        if (transition.isIdle && !transition.currentState && !transition.targetState) latestDismiss()
    }
    val colors = Mihraya.colors
    val scrim by animateFloatAsState(if (transition.targetState) 0.45f else 0f, tween(240), label = "sideScrim")
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                // Compose 1.7 measures a full-screen dialog before Android 15's
                // floating-window insets. Give the window matching full-screen
                // bounds, then let safeDrawing inset the panel exactly once.
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR)
                window.attributes = window.attributes.apply {
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.MATCH_PARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) setFitInsetsTypes(0)
                }
                window.setDimAmount(0f)
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !colors.isDark
                    isAppearanceLightNavigationBars = !colors.isDark
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isStatusBarContrastEnforced = false
                    window.isNavigationBarContrastEnforced = false
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)).clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
                onClickLabel = "Close reading preferences", onClick = dismiss,
            ))
            BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                val width = (maxWidth * 0.46f).coerceIn(320.dp, 420.dp).coerceAtMost((maxWidth - 48.dp).coerceAtLeast(1.dp))
                AnimatedVisibility(
                    visibleState = transition,
                    enter = slideInHorizontally(tween(280)) { it },
                    exit = slideOutHorizontally(tween(240)) { it },
                    modifier = Modifier.align(AbsoluteAlignment.CenterRight),
                ) {
                    Column(Modifier.width(width).fillMaxHeight()
                        .glass(AbsoluteRoundedCornerShape(topLeft = 28.dp, bottomLeft = 28.dp), colors, colors.overlaySurface())) {
                        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(title, style = Mihraya.type.title, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            CircleIconButton(Icons.Rounded.Close, "Close reading preferences", dismiss, filled = false)
                        }
                        content()
                    }
                }
            }
        }
    }
}

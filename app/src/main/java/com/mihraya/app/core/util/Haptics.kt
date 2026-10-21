package com.mihraya.app.core.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.mihraya.app.core.designsystem.theme.LocalHapticsEnabled

class Haptics(private val view: View, private val enabled: Boolean) {

    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)

    fun tap() = perform(HapticFeedbackConstants.KEYBOARD_TAP)

    fun milestone() = perform(HapticFeedbackConstants.CONTEXT_CLICK)

    fun confirm() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.LONG_PRESS,
    )

    private fun perform(constant: Int) {
        if (enabled) view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val enabled = LocalHapticsEnabled.current
    return remember(view, enabled) { Haptics(view, enabled) }
}

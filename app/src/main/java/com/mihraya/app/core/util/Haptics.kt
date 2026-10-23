package com.mihraya.app.core.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
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

    /** A short double pulse, stronger than [tap], used when a tasbih round is completed. */
    fun goal() {
        if (!enabled || !systemHapticsEnabled()) return
        val vibrator = vibrator()
        if (vibrator == null || !vibrator.hasVibrator()) {
            perform(HapticFeedbackConstants.LONG_PRESS)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 24, 46, 36), intArrayOf(0, 160, 0, 255), -1))
        } else {
            perform(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    private fun perform(constant: Int) {
        if (enabled && systemHapticsEnabled()) view.performHapticFeedback(constant)
    }

    private fun systemHapticsEnabled(): Boolean = Settings.System.getInt(
        view.context.contentResolver,
        Settings.System.HAPTIC_FEEDBACK_ENABLED,
        1,
    ) == 1

    private fun vibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        view.context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        view.context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val enabled = LocalHapticsEnabled.current
    return remember(view, enabled) { Haptics(view, enabled) }
}

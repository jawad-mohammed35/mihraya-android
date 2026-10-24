package com.mihraya.app.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.mihraya.app.appContainer
import com.mihraya.app.data.library.TasbihSession
import com.mihraya.app.data.library.TasbihState
import com.mihraya.app.data.library.dhikr
import com.mihraya.app.playback.PlaybackService

class TasbihCountAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val library = context.appContainer.library
        val before = library.current().tasbih
        val saved = library.editNow { current ->
            val state = current.tasbih
            val now = System.currentTimeMillis()
            val starting = state.count == 0 || state.count >= state.target
            val count = if (state.count >= state.target) 1 else state.count + 1
            val startedAt = if (starting || state.startedAt == 0L) now else state.startedAt
            val done = count == state.target
            val next = state.copy(count = count, rounds = state.rounds + if (done) 1 else 0, startedAt = startedAt)
            val sessions = if (done) {
                val label = current.dhikr(state.presetId).transliteration
                (listOf(TasbihSession(label, state.target, state.target, startedAt, now)) + current.sessions).take(400)
            } else {
                current.sessions
            }
            current.copy(tasbih = next, sessions = sessions)
        }
        if (saved.tasbih.rounds == before.rounds + 1 && context.appContainer.settings.current().haptics) vibrateGoal(context)
        WidgetPrefs.write(context, widgetId(context, glanceId), WidgetPrefs.read(context, widgetId(context, glanceId)).copy(confirmReset = false))
        TasbihWidget().update(context, glanceId)
    }
}

class TasbihTargetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val library = context.appContainer.library
        library.editNow { current ->
            val state = current.tasbih
            val order = listOf(33, 100, 1000)
            val nextTarget = order.getOrNull(order.indexOf(state.target) + 1) ?: order.first()
            current.copy(tasbih = state.copy(target = nextTarget, count = state.count.coerceAtMost(nextTarget)))
        }
        TasbihWidget().update(context, glanceId)
    }
}

class TasbihResetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = widgetId(context, glanceId)
        val options = WidgetPrefs.read(context, id)
        if (!options.confirmReset) {
            WidgetPrefs.write(context, id, options.copy(confirmReset = true))
        } else {
            val library = context.appContainer.library
            library.editNow { current ->
                val state = current.tasbih
                val sessions = if (state.count in 1 until state.target) {
                    val label = current.dhikr(state.presetId).transliteration
                    val started = state.startedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
                    (listOf(TasbihSession(label, state.count, state.target, started, System.currentTimeMillis())) + current.sessions).take(400)
                } else {
                    current.sessions
                }
                current.copy(tasbih = state.copy(count = 0, rounds = 0, startedAt = 0L), sessions = sessions)
            }
            WidgetPrefs.write(context, id, options.copy(confirmReset = false))
        }
        TasbihWidget().update(context, glanceId)
    }
}

class ToggleAudioAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_TOGGLE)
        val started = runCatching { ContextCompat.startForegroundService(context, intent) }.isSuccess
        if (!started) {
            context.startActivity(
                Intent(context, com.mihraya.app.MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(Destinations.EXTRA, Destinations.PLAYER),
            )
        }
        kotlinx.coroutines.delay(350)
        AudioWidget().update(context, glanceId)
    }
}

private suspend fun widgetId(context: Context, glanceId: GlanceId): Int =
    androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(glanceId)

private fun vibrateGoal(context: Context) {
    val enabled = Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1
    if (!enabled) return
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 24, 46, 36), intArrayOf(0, 160, 0, 255), -1))
}

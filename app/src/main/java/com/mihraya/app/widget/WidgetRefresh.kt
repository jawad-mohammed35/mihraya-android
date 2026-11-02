package com.mihraya.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val WidgetRevision = longPreferencesKey("mihraya_revision")

/**
 * Provides content drawn from [load], reloading it each time [redraw] bumps the widget's revision.
 * Glance keeps a widget's session alive between updates, and a running session recomposes on
 * update without calling provideGlance again, so data read only up front would go stale.
 */
suspend fun <T> GlanceAppWidget.provideWidget(load: suspend () -> T, content: @Composable (T) -> Unit) {
    val initial = load()
    provideContent {
        val revision = currentState(WidgetRevision)
        val firstRevision = remember { revision }
        var data by remember { mutableStateOf(initial) }
        LaunchedEffect(revision) {
            if (revision != firstRevision) data = load()
        }
        content(data)
    }
}

/** Redraws one widget instance with freshly loaded data. */
suspend fun GlanceAppWidget.redraw(context: Context, id: GlanceId) {
    updateAppWidgetState(context, id) { it[WidgetRevision] = SystemClock.elapsedRealtimeNanos() }
    update(context, id)
}

object WidgetRefresh {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val widgets: Map<String, GlanceAppWidget> = mapOf(
        PrayerTimetableReceiver::class.java.name to PrayerTimetableWidget(),
        NextPrayerReceiver::class.java.name to NextPrayerWidget(),
        QiblaReceiver::class.java.name to QiblaWidget(),
        VerseReceiver::class.java.name to VerseWidget(),
        LastReadReceiver::class.java.name to LastReadWidget(),
        BookmarksReceiver::class.java.name to BookmarksWidget(),
        AudioReceiver::class.java.name to AudioWidget(),
        TasbihReceiver::class.java.name to TasbihWidget(),
    )

    fun refresh(context: Context) {
        val app = context.applicationContext
        scope.launch {
            val manager = GlanceAppWidgetManager(app)
            widgets.values.forEach { widget ->
                runCatching {
                    manager.getGlanceIds(widget.javaClass).forEach { id -> widget.redraw(app, id) }
                }
            }
        }
    }

    /** Redraws one placed widget with the widget type its provider declares. */
    fun refresh(context: Context, appWidgetId: Int) {
        val app = context.applicationContext
        val provider = AppWidgetManager.getInstance(app).getAppWidgetInfo(appWidgetId)?.provider?.className
        val widget = widgets[provider] ?: return
        scope.launch {
            runCatching { widget.redraw(app, GlanceAppWidgetManager(app).getGlanceIdBy(appWidgetId)) }
        }
    }
}

package com.mihraya.app.widget

import android.content.Context

enum class WidgetTheme { Dark, Light, Glass }

enum class TimetableLayout { Vertical, Horizontal }

data class WidgetOptions(
    val theme: WidgetTheme = WidgetTheme.Dark,
    val layout: TimetableLayout = TimetableLayout.Vertical,
    val sunrise: Boolean = false,
    val duha: Boolean = false,
    val qiyam: Boolean = false,
    val showTranslation: Boolean = true,
    val confirmReset: Boolean = false,
)

object WidgetPrefs {
    private const val FILE = "mihraya_widgets"

    fun read(context: Context, widgetId: Int): WidgetOptions {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return WidgetOptions(
            theme = enum(prefs.getString(key(widgetId, "theme"), null), WidgetTheme.Dark),
            layout = enum(prefs.getString(key(widgetId, "layout"), null), TimetableLayout.Vertical),
            sunrise = prefs.getBoolean(key(widgetId, "sunrise"), false),
            duha = prefs.getBoolean(key(widgetId, "duha"), false),
            qiyam = prefs.getBoolean(key(widgetId, "qiyam"), false),
            showTranslation = prefs.getBoolean(key(widgetId, "translation"), true),
            confirmReset = prefs.getBoolean(key(widgetId, "confirmReset"), false),
        )
    }

    fun write(context: Context, widgetId: Int, options: WidgetOptions) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(key(widgetId, "theme"), options.theme.name)
            .putString(key(widgetId, "layout"), options.layout.name)
            .putBoolean(key(widgetId, "sunrise"), options.sunrise)
            .putBoolean(key(widgetId, "duha"), options.duha)
            .putBoolean(key(widgetId, "qiyam"), options.qiyam)
            .putBoolean(key(widgetId, "translation"), options.showTranslation)
            .putBoolean(key(widgetId, "confirmReset"), options.confirmReset)
            .apply()
    }

    fun clear(context: Context, widgetId: Int) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("$widgetId:") }.forEach { editor.remove(it) }
        editor.apply()
    }

    private fun key(widgetId: Int, name: String) = "$widgetId:$name"

    private inline fun <reified T : Enum<T>> enum(raw: String?, fallback: T): T =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
}

object Destinations {
    const val EXTRA = "mihraya_dest"
    const val SURAH = "mihraya_surah"
    const val AYAH = "mihraya_ayah"

    /** Set by the Tasbih widget, whose count the app should continue rather than start afresh. */
    const val KEEP_TASBIH = "mihraya_keep_tasbih"

    /** Set by launcher shortcuts, whose tasks Android would otherwise close on Back instead of backgrounding. */
    const val FROM_SHORTCUT = "mihraya_from_shortcut"
    const val TIMETABLE = "timetable"
    const val QIBLA = "qibla"
    const val READER = "reader"
    const val QURAN = "quran"
    const val LISTEN = "listen"
    const val TASBIH = "tasbih"
    const val PLAYER = "player"
}

package com.mihraya.app.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.mihraya.app.core.designsystem.icons.MihrayaIcons
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object QuranRoute
@Serializable data object ListenRoute
@Serializable data object QiblaRoute
@Serializable data object MoreRoute

@Serializable data class ReaderRoute(val surah: Int, val ayah: Int = 1)
@Serializable data object TimetableRoute
@Serializable data object TasbihRoute
@Serializable data object PrayerSettingsRoute
@Serializable data object NotificationSettingsRoute
@Serializable data object LocationRoute
@Serializable data object AppearanceRoute
@Serializable data object ReadingSettingsRoute
@Serializable data object TranslationsRoute
@Serializable data object DownloadsRoute
@Serializable data object AboutRoute

enum class TopLevel(val label: String, val icon: ImageVector, val route: Any) {
    Home("Home", MihrayaIcons.Home, HomeRoute),
    Quran("Quran", MihrayaIcons.Quran, QuranRoute),
    Listen("Listen", MihrayaIcons.Listen, ListenRoute),
    Qibla("Qibla", MihrayaIcons.Qibla, QiblaRoute),
    More("More", MihrayaIcons.More, MoreRoute),
}

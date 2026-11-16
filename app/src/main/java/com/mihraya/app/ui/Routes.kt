package com.mihraya.app.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.mihraya.app.core.designsystem.icons.MihrayaIcons
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object QuranNavigationRoute
@Serializable data object QuranRoute
@Serializable data object ListenRoute
@Serializable data object HadithRoute
@Serializable data class HadithCollectionRoute(val collection: String)
@Serializable data class HadithListRoute(val collection: String = "", val chapter: String = "", val bookmarks: Boolean = false)
@Serializable data class HadithReaderRoute(val collection: String, val id: String)
@Serializable data object TasbihRoute
@Serializable data object MoreRoute

@Serializable data class ReaderRoute(val surah: Int, val ayah: Int = 1)
@Serializable data object TimetableRoute
@Serializable data object QiblaRoute
@Serializable data object PrayerSettingsRoute
@Serializable data object NotificationSettingsRoute
@Serializable data object LocationRoute
@Serializable data object AppearanceRoute
@Serializable data object ReadingSettingsRoute
@Serializable data object TranslationsRoute
@Serializable data object DownloadsRoute
@Serializable data object AboutRoute
@Serializable data object TourRoute

data class LaunchRequest(
    val nonce: Int = 0,
    val player: Boolean = false,
    val dest: String? = null,
    val surah: Int = 1,
    val ayah: Int = 1,
)

enum class TopLevel(val label: String, val icon: ImageVector, val selectedIcon: ImageVector, val route: Any) {
    Home("Home", MihrayaIcons.Home, MihrayaIcons.HomeFilled, HomeRoute),
    Quran("Quran", MihrayaIcons.Quran, MihrayaIcons.QuranFilled, QuranRoute),
    Listen("Listen", MihrayaIcons.Listen, MihrayaIcons.ListenFilled, ListenRoute),
    Hadith("Hadith", MihrayaIcons.Hadith, MihrayaIcons.HadithFilled, HadithRoute),
    Tasbih("Tasbih", MihrayaIcons.TasbihOutlined, MihrayaIcons.Tasbih, TasbihRoute),
}

@Serializable data object ZakatRoute
@Serializable data object NamesRoute
@Serializable data object NamesLearnRoute

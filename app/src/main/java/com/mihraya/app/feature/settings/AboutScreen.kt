package com.mihraya.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mihraya.app.BuildConfig
import com.mihraya.app.core.designsystem.components.GlassCard
import com.mihraya.app.core.designsystem.components.SettingsDivider
import com.mihraya.app.core.designsystem.components.SettingsGroup
import com.mihraya.app.core.designsystem.components.StarShape
import com.mihraya.app.core.designsystem.components.glass
import com.mihraya.app.core.designsystem.icons.MihrayaIcons
import com.mihraya.app.core.designsystem.theme.Mihraya

private data class Credit(val title: String, val detail: String, val url: String)

private val contentSources = listOf(
    Credit("Quran text", "Uthmani and IndoPak scripts, via the Quran.com API (Quran Foundation)", "https://quran.com"),
    Credit("Translations", "Saheeh International, Jalandhari, transliteration and downloadable editions, via the Quran.com API", "https://api-docs.quran.foundation"),
    Credit("Recitations", "Streamed and downloaded from the mp3quran.net library", "https://mp3quran.net"),
    Credit("Prayer times", "Adhan by Batoul Apps · MIT licence", "https://github.com/batoulapps/adhan-java"),
    Credit("Hijri calendar", "Umm al-Qura calendar from the Java time library", "https://docs.oracle.com/javase/8/docs/api/java/time/chrono/HijrahChronology.html"),
    Credit("City search", "Open-Meteo geocoding, based on GeoNames · CC BY 4.0", "https://open-meteo.com/en/docs/geocoding-api"),
)

private val mediaCredits = listOf(
    Credit("Adhan · Aaqib Azeez", "Wikimedia Commons, uploaded by Atcovi · CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:The_Adhan_-_Muslim_Call_to_Prayer_-_Aaqib_Azeez.mp3"),
    Credit("Adhan · Mahfoudou", "Wikimedia Commons · CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Islamic_call_to_worship.oga"),
    Credit("Fonts", "Manrope, Amiri, Amiri Quran, Scheherazade New, Noto Naskh Arabic, Noto Nastaliq Urdu · SIL Open Font Licence 1.1", "https://fonts.google.com"),
)

private val libraries = listOf(
    Credit("Jetpack Compose, Media3, WorkManager, DataStore", "Android Open Source Project · Apache 2.0", "https://developer.android.com/jetpack"),
    Credit("Haze", "Chris Banes · Apache 2.0", "https://github.com/chrisbanes/haze"),
    Credit("OkHttp", "Square · Apache 2.0", "https://square.github.io/okhttp/"),
    Credit("Kotlin coroutines & serialization", "JetBrains · Apache 2.0", "https://kotlinlang.org"),
)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = Mihraya.colors
    SettingsScaffold("About", onBack) {
        Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(84.dp).glass(StarShape, colors, colors.accentSoft), contentAlignment = Alignment.Center) {
                Icon(MihrayaIcons.Home, null, tint = colors.accent, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("Mihraya", style = Mihraya.type.headline, color = colors.textPrimary)
            Text("Version ${BuildConfig.VERSION_NAME}", style = Mihraya.type.caption, color = colors.textTertiary)
            Spacer(Modifier.height(12.dp))
            Text(
                "A calm companion for prayer, the Quran and remembrance.",
                style = Mihraya.type.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }

        GlassCard(contentPadding = PaddingValues(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shield, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Privacy", style = Mihraya.type.titleSmall, color = colors.textPrimary)
            }
            Spacer(Modifier.height(10.dp))
            listOf(
                "No accounts, ads, analytics or trackers.",
                "Prayer times and the Qibla are calculated on your device. Your coordinates are never uploaded.",
                "City searches are sent to Open-Meteo. When detecting your location, Android's built-in geocoder looks up the city name.",
                "Settings, bookmarks, reading progress and tasbīḥ history stay on this device.",
            ).forEach { line ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("•", style = Mihraya.type.bodySmall, color = colors.accent, modifier = Modifier.width(14.dp))
                    Text(line, style = Mihraya.type.bodySmall, color = colors.textSecondary)
                }
            }
        }

        CreditGroup("Sources", contentSources)
        CreditGroup("Audio & typography", mediaCredits)
        CreditGroup("Open-source libraries", libraries)

        Text(
            "Prayer times are calculations. Please confirm with your local mosque, especially for Fajr, Ramadan and Eid.",
            style = Mihraya.type.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun CreditGroup(title: String, credits: List<Credit>) {
    val uriHandler = LocalUriHandler.current
    val colors = Mihraya.colors
    SettingsGroup(title) {
        credits.forEachIndexed { index, credit ->
            if (index > 0) SettingsDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { runCatching { uriHandler.openUri(credit.url) } }
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(credit.title, style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(credit.detail, style = Mihraya.type.bodySmall, color = colors.textTertiary)
                }
                Spacer(Modifier.width(12.dp))
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, "Open link", tint = colors.textTertiary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

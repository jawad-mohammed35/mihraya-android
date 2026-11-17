package com.mihraya.app.feature.more

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.mihraya.app.core.designsystem.theme.ArabicFonts

internal val LocalNamesArabicFont = staticCompositionLocalOf { NamesArabicFont.Amiri }

internal fun NamesArabicFont.family(): FontFamily = when (this) {
    NamesArabicFont.Amiri -> ArabicFonts.Amiri
    NamesArabicFont.Nastaliq -> ArabicFonts.Nastaliq
    NamesArabicFont.NotoNaskh -> ArabicFonts.NotoNaskh
}

@Composable
internal fun NamesTypography(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { MoreStore(context) }
    var font by remember(store) { mutableStateOf(store.namesFont()) }
    DisposableEffect(store) {
        val stop = store.observeNamesFont { font = it }
        onDispose(stop)
    }
    CompositionLocalProvider(LocalNamesArabicFont provides font, content = content)
}

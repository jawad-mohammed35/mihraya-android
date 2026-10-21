@file:OptIn(ExperimentalTextApi::class)

package com.mihraya.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mihraya.app.R
import com.mihraya.app.data.quran.TranslationInfo
import com.mihraya.app.data.settings.ArabicFont
import com.mihraya.app.data.settings.QuranScript

private fun manrope(weight: FontWeight) = Font(
    resId = R.font.manrope,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Manrope = FontFamily(
    manrope(FontWeight.ExtraLight),
    manrope(FontWeight.Light),
    manrope(FontWeight.Normal),
    manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold),
)

object ArabicFonts {
    val Amiri = FontFamily(Font(R.font.amiri))
    val AmiriQuran = FontFamily(Font(R.font.amiri_quran))
    val Scheherazade = FontFamily(Font(R.font.scheherazade_new))
    val NotoNaskh = FontFamily(
        Font(R.font.noto_naskh_arabic, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.noto_naskh_arabic, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    )
    val Nastaliq = FontFamily(Font(R.font.noto_nastaliq_urdu))

    /** Amiri Quran lacks the Urdu letter forms used by the IndoPak script, so regular Amiri stands in for it there. */
    fun quran(font: ArabicFont, script: QuranScript): FontFamily = when (font) {
        ArabicFont.AmiriQuran -> if (script == QuranScript.IndoPak) Amiri else AmiriQuran
        ArabicFont.Scheherazade -> Scheherazade
        ArabicFont.NotoNaskh -> NotoNaskh
    }

    fun translation(info: TranslationInfo): FontFamily = if (info.languageCode == "ur") Nastaliq else Manrope
}

@Immutable
data class MihrayaType(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val titleSmall: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val overline: TextStyle,
    val caption: TextStyle,
)

val DefaultType = MihrayaType(
    display = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraLight,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum",
    ),
    headline = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Light, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp),
    title = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    titleSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    body = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    label = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp),
    overline = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp),
    caption = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.2.sp),
)

val MaterialTypography: Typography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Manrope),
        displayMedium = base.displayMedium.copy(fontFamily = Manrope),
        displaySmall = base.displaySmall.copy(fontFamily = Manrope),
        headlineLarge = base.headlineLarge.copy(fontFamily = Manrope),
        headlineMedium = base.headlineMedium.copy(fontFamily = Manrope),
        headlineSmall = base.headlineSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium),
        titleLarge = base.titleLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = Manrope),
        bodyLarge = base.bodyLarge.copy(fontFamily = Manrope),
        bodyMedium = base.bodyMedium.copy(fontFamily = Manrope),
        bodySmall = base.bodySmall.copy(fontFamily = Manrope),
        labelLarge = base.labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = Manrope),
        labelSmall = base.labelSmall.copy(fontFamily = Manrope),
    )
}

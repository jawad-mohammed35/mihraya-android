package com.mihraya.app.feature.quran

import com.mihraya.app.data.settings.ArabicFont
import com.mihraya.app.data.settings.QuranScript
import com.mihraya.app.data.settings.ReaderPreferences

/** Amiri Quran 1.003: hhea ascent 1815 + descent 634, unitsPerEm 1000.
 * Keep stacked Quranic marks on adjacent lines apart without changing stored settings.
 */
internal fun ReaderPreferences.effectiveLineHeight(): Float =
    if (script == QuranScript.Uthmani && arabicFont == ArabicFont.AmiriQuran) lineHeight.coerceAtLeast(2.45f)
    else lineHeight

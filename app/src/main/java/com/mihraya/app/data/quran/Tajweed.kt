package com.mihraya.app.data.quran

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

data class TajweedRun(val text: String, val rule: String?)

private val endMarker = Regex("<span class=end>.*?</span>")
private val tag = Regex("<tajweed class=\"?([a-z0-9_]+)\"?>(.*?)</tajweed>")

/** Parses Quran.com `uthmani_tajweed` markup into text runs. Unknown tags are kept as plain text. */
fun parseTajweed(markup: String): List<TajweedRun> {
    val source = markup.replace(endMarker, "").trim()
    if (source.isEmpty()) return emptyList()
    val runs = mutableListOf<TajweedRun>()
    var cursor = 0
    tag.findAll(source).forEach { match ->
        if (match.range.first > cursor) {
            val plain = source.substring(cursor, match.range.first)
            if (plain.isNotEmpty()) runs += TajweedRun(plain, null)
        }
        runs += TajweedRun(match.groupValues[2], match.groupValues[1])
        cursor = match.range.last + 1
    }
    if (cursor < source.length) {
        val tail = source.substring(cursor).replace(Regex("<[^>]+>"), "")
        if (tail.isNotEmpty()) runs += TajweedRun(tail, null)
    }
    return runs
}

fun tajweedAnnotated(markup: String, base: Color, dark: Boolean): AnnotatedString = buildAnnotatedString {
    parseTajweed(markup).forEach { run ->
        val color = run.rule?.let { tajweedColor(it, dark) } ?: base
        withStyle(SpanStyle(color = color)) { append(run.text) }
    }
}

/**
 * Colors match the Quran.com tajweed legend for the Uthmani text.
 * Light-theme values are darkened so gray and blue rules stay readable on a light page.
 */
fun tajweedColor(rule: String, dark: Boolean): Color = when (rule) {
    "ham_wasl", "slnt", "laam_shamsiyah" -> if (dark) Color(0xFFB5B5B5) else Color(0xFF5E5E5E)
    "madda_normal" -> if (dark) Color(0xFF8AABFF) else Color(0xFF2A46C4)
    "madda_permissible" -> if (dark) Color(0xFF7A90FF) else Color(0xFF243CC0)
    "madda_necessary" -> if (dark) Color(0xFF9BB0FF) else Color(0xFF1026A8)
    "madda_obligatory", "madda_obligatory_monfasel", "madda_obligatory_mottasel" ->
        if (dark) Color(0xFF8AA4FF) else Color(0xFF1A3498)
    "qalaqah", "qalqalah" -> if (dark) Color(0xFFFF5A62) else Color(0xFFC10812)
    "ikhafa_shafawi" -> if (dark) Color(0xFFFF7AD8) else Color(0xFFB0008C)
    "ikhafa" -> if (dark) Color(0xFFE07CFF) else Color(0xFF7A008C)
    "idgham_shafawi" -> if (dark) Color(0xFF8FE05A) else Color(0xFF3D8600)
    "iqlab" -> if (dark) Color(0xFF6ED4FF) else Color(0xFF0077A8)
    "idgham_ghunnah", "idgham_wo_ghunnah" -> if (dark) Color(0xFF7DDC6A) else Color(0xFF0E7A00)
    "idgham_mutajanisayn", "idgham_mutaqaribayn" -> if (dark) Color(0xFFC8C8C8) else Color(0xFF5A5A5A)
    "ghunnah" -> if (dark) Color(0xFFFFB067) else Color(0xFFC65A00)
    else -> if (dark) Color(0xFFF3F1EC) else Color(0xFF111312)
}

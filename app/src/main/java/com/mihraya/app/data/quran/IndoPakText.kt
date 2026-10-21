package com.mihraya.app.data.quran

/**
 * The IndoPak edition encodes several pause marks as private-use characters that only its publisher's
 * font can draw. Marks with a confirmed standard equivalent are mapped to it; the rest are dropped so no
 * reader font shows missing-glyph boxes.
 */
internal object IndoPakText {

    private val replacements = mapOf(
        '\uE01A' to '\u0617', // small high zain
        '\uE01C' to '\u06D7', // small high ligature qaf with lam with alef maksura
        '\uE01E' to '\u06EB', // empty centre high stop
        '\uE01F' to '\u06E5', // small waw
        '\uE021' to '\u06EC', // rounded high stop with filled centre
        '\uE022' to '\u06E0', // small high upright rectangular zero
    )

    fun normalize(line: String): String {
        val out = StringBuilder(line.length)
        for (c in line) {
            when {
                c in '\uE000'..'\uF8FF' -> replacements[c]?.let(out::append)
                c == ' ' && (out.isEmpty() || out.last() == ' ') -> Unit
                else -> out.append(c)
            }
        }
        return out.trimEnd().toString()
    }
}

package com.mihraya.app.data.quran

/** The publisher supplies each verse key and its complete text separated by a tab. */
internal object IndoPakText {
    fun verse(index: Int, row: String): String {
        val key = QuranMeta.refOf(index + 1).key
        require(row.startsWith("$key\t")) { "Unexpected IndoPak verse at $key" }
        return row.substring(key.length + 1)
    }
}

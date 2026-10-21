package com.mihraya.app.data.library

import kotlinx.serialization.Serializable

@Serializable
data class Bookmark(val surah: Int, val ayah: Int, val createdAt: Long)

@Serializable
data class ReadingPosition(val surah: Int, val ayah: Int, val updatedAt: Long)

@Serializable
data class DhikrPreset(
    val id: String,
    val arabic: String,
    val transliteration: String,
    val meaning: String,
    val target: Int,
    val custom: Boolean = true,
)

@Serializable
data class TasbihState(
    val presetId: String = DhikrLibrary.builtIn.first().id,
    val count: Int = 0,
    val target: Int = 33,
    val rounds: Int = 0,
    val startedAt: Long = 0L,
)

@Serializable
data class TasbihSession(
    val label: String,
    val count: Int,
    val target: Int,
    val startedAt: Long,
    val endedAt: Long,
)

@Serializable
data class LastPlayed(val reciterId: String, val surah: Int, val positionMs: Long, val updatedAt: Long)

@Serializable
data class UserLibrary(
    val bookmarks: List<Bookmark> = emptyList(),
    val lastRead: ReadingPosition? = null,
    val customDhikr: List<DhikrPreset> = emptyList(),
    val tasbih: TasbihState = TasbihState(),
    val sessions: List<TasbihSession> = emptyList(),
    val lastPlayed: LastPlayed? = null,
)

object DhikrLibrary {
    val builtIn: List<DhikrPreset> = listOf(
        DhikrPreset("subhanallah", "سُبْحَانَ ٱللَّٰهِ", "SubḥānAllāh", "Glory be to Allah", 33, custom = false),
        DhikrPreset("alhamdulillah", "ٱلْحَمْدُ لِلَّٰهِ", "Alḥamdulillāh", "All praise is for Allah", 33, custom = false),
        DhikrPreset("allahuakbar", "ٱللَّٰهُ أَكْبَرُ", "Allāhu Akbar", "Allah is the Greatest", 34, custom = false),
        DhikrPreset("tahlil", "لَا إِلَٰهَ إِلَّا ٱللَّٰهُ", "Lā ilāha illā Allāh", "There is no god but Allah", 100, custom = false),
        DhikrPreset("istighfar", "أَسْتَغْفِرُ ٱللَّٰهَ", "Astaghfirullāh", "I seek Allah's forgiveness", 100, custom = false),
        DhikrPreset(
            "subhanallahi-wa-bihamdihi", "سُبْحَانَ ٱللَّٰهِ وَبِحَمْدِهِ", "SubḥānAllāhi wa biḥamdih",
            "Glory and praise be to Allah", 100, custom = false,
        ),
        DhikrPreset(
            "hawqala", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِٱللَّٰهِ", "Lā ḥawla wa lā quwwata illā billāh",
            "There is no power nor strength except by Allah", 33, custom = false,
        ),
        DhikrPreset(
            "salawat", "ٱللَّٰهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ", "Allāhumma ṣalli ʿalā Muḥammad",
            "O Allah, send blessings upon Muhammad ﷺ", 100, custom = false,
        ),
    )
}

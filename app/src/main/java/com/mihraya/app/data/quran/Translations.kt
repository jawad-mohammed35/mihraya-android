package com.mihraya.app.data.quran

data class TranslationInfo(
    val id: Int,
    val name: String,
    val language: String,
    val languageCode: String,
    val bundled: Boolean = false,
) {
    val isRtl: Boolean get() = languageCode == "ur"
}

object TranslationCatalog {
    const val TRANSLITERATION_ID = 57

    val all: List<TranslationInfo> = listOf(
        TranslationInfo(20, "Saheeh International", "English", "en", bundled = true),
        TranslationInfo(85, "M.A.S. Abdel Haleem", "English", "en"),
        TranslationInfo(84, "Mufti Taqi Usmani", "English", "en"),
        TranslationInfo(19, "Marmaduke Pickthall", "English", "en"),
        TranslationInfo(22, "Abdullah Yusuf Ali", "English", "en"),
        TranslationInfo(203, "Al-Hilali & Muhsin Khan", "English", "en"),
        TranslationInfo(234, "Fatah Muhammad Jalandhari", "Urdu", "ur", bundled = true),
        TranslationInfo(54, "Maulana Muhammad Junagarhi", "Urdu", "ur"),
        TranslationInfo(97, "Tafheem-ul-Quran · Abul Ala Maududi", "Urdu", "ur"),
        TranslationInfo(158, "Bayan-ul-Quran · Dr. Israr Ahmad", "Urdu", "ur"),
        TranslationInfo(819, "Maulana Wahiduddin Khan", "Urdu", "ur"),
        TranslationInfo(33, "Kementerian Agama", "Indonesian", "id"),
        TranslationInfo(39, "Abdullah Muhammad Basmeih", "Malay", "ms"),
        TranslationInfo(77, "Diyanet İşleri", "Turkish", "tr"),
        TranslationInfo(31, "Muhammad Hamidullah", "French", "fr"),
        TranslationInfo(27, "Frank Bubenheim & Nadeem Elyas", "German", "de"),
        TranslationInfo(83, "Sheikh Isa Garcia", "Spanish", "es"),
        TranslationInfo(45, "Elmir Kuliev", "Russian", "ru"),
        TranslationInfo(161, "Taisirul Quran", "Bengali", "bn"),
        TranslationInfo(122, "Maulana Azizul Haque al-Umari", "Hindi", "hi"),
    )

    fun byId(id: Int): TranslationInfo? = all.firstOrNull { it.id == id }

    val languages: List<String> get() = all.map { it.language }.distinct()
}

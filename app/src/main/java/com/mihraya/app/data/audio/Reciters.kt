package com.mihraya.app.data.audio

import java.util.Locale

data class Reciter(
    val id: String,
    val name: String,
    val shortName: String,
    val arabicName: String,
    val arabicShortName: String,
    val style: String,
    val origin: String,
    val baseUrl: String,
    val hue: Long,
) {
    fun urlFor(surah: Int): String = baseUrl + String.format(Locale.US, "%03d.mp3", surah)
}

object Reciters {
    val all: List<Reciter> = listOf(
        Reciter(
            "alafasy", "Mishary Rashid Alafasy", "Alafasy", "مشاري راشد العفاسي", "العفاسي",
            "Murattal · Ḥafṣ", "Kuwait", "https://cdn.mp3quran.net/audio/mishary-alafasy/r1/", 0xFF2E7D6B,
        ),
        Reciter(
            "abdulbasit", "Abdul Basit Abdul Samad", "Abdul Basit", "عبد الباسط عبد الصمد", "عبد الباسط",
            "Murattal · Ḥafṣ", "Egypt", "https://cdn.mp3quran.net/audio/abdulbasit-abdulsamad/r3/", 0xFF8C6D2F,
        ),
        Reciter(
            "maher", "Maher Al-Muaiqly", "Al-Muaiqly", "ماهر المعيقلي", "المعيقلي",
            "Murattal · Ḥafṣ", "Makkah", "https://cdn.mp3quran.net/audio/maher-muaiqly/r1/", 0xFF2B4C7E,
        ),
        Reciter(
            "sudais", "Abdul Rahman Al-Sudais", "Al-Sudais", "عبد الرحمن السديس", "السديس",
            "Murattal · Ḥafṣ", "Makkah", "https://cdn.mp3quran.net/audio/abdulrahman-sudais/r1/", 0xFF7A2E3A,
        ),
        Reciter(
            "husary", "Mahmoud Khalil Al-Husary", "Al-Husary", "محمود خليل الحصري", "الحصري",
            "Murattal · Ḥafṣ", "Egypt", "https://cdn.mp3quran.net/audio/mahmoud-husary/r1/", 0xFF5B6B2F,
        ),
        Reciter(
            "ghamdi", "Saad Al-Ghamdi", "Al-Ghamdi", "سعد الغامدي", "الغامدي",
            "Murattal · Ḥafṣ", "Saudi Arabia", "https://cdn.mp3quran.net/audio/saad-ghamdi/r1/", 0xFF4E3A6E,
        ),
        Reciter(
            "dosari", "Yasser Al-Dosari", "Al-Dosari", "ياسر الدوسري", "الدوسري",
            "Murattal · Ḥafṣ", "Saudi Arabia", "https://cdn.mp3quran.net/audio/yasser-dosari/r1/", 0xFF2D6470,
        ),
    )

    val Default: Reciter = all.first()

    fun byId(id: String?): Reciter = all.firstOrNull { it.id == id } ?: Default

    fun find(id: String?): Reciter? = all.firstOrNull { it.id == id }
}

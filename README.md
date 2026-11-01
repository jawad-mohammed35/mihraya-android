# Mihraya

Prayer times, Quran, recitation, Qibla, and Tasbih for Android. The interface is built for a pure-black OLED screen.

## Features

- **Prayer times** calculated on the device, with Adhan or tone alerts, a day and month timetable, and Hijri dates.
- **Quran reader** in Uthmani or IndoPak script, with translations, transliteration, optional tajweed colors, bookmarks, and search by sūrah, verse, or word.
- **Recitation** from 17 reciters, streamed or downloaded for offline listening, with background playback.
- **Qibla** compass and a **Tasbih** counter with presets, custom dhikr, and history.
- **Home screen widgets** for prayer times, the next prayer, Qibla, verse of the day, last read, bookmarks, audio, and Tasbih.
- **App shortcuts**: long-press the Mihraya icon to open Quran, Listen, or Tasbih directly, or drag a shortcut onto the home screen.
- **Appearance**: Black, Light, or System theme, an optional OLED Black mode, and four accent colors.

## Install

Download the latest APK from [Releases](https://github.com/jawad-mohammed35/mihraya-android/releases). On your phone, open the file and allow your browser or file manager to install apps when Android asks. Requires Android 8 or newer.

Updates use the same signing key, so you can install a newer APK over the existing app without losing your settings.

## Build

Requirements: Android Studio (or JDK 17 and the Android SDK with API 35).

Open the project in Android Studio, or from this folder:

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

A release APK needs a `keystore.properties` file in the project root (this file is not in the repository):

```
storeFile=/path/to/mihraya-release.jks
storePassword=...
keyAlias=mihraya
keyPassword=...
```

Then run `./gradlew :app:assembleRelease`. The build refuses to sign a release with the debug key.

## License

Mihraya is licensed under the [Apache License, Version 2.0](LICENSE). Copyright 2026 Jawad Mohammed.

The Apache License covers Mihraya's own code and artwork. Some material in this repository belongs to others and keeps its own terms; it is not relicensed under Apache 2.0:

- Quran text and translations in `app/src/main/assets/quran/`, retrieved through the Quran.com API and subject to the [Quran Foundation Developer Terms](https://api-docs.quran.foundation/legal/developer-terms/) and each translator's rights.
- Fonts in `app/src/main/res/font/`, under the [SIL Open Font License 1.1](licenses/OFL-1.1.txt).
- Adhan recordings in `app/src/main/res/raw/`, under CC0, CC BY 3.0, CC BY-SA 4.0, or the public domain, as listed in [NOTICE](NOTICE).

Recitation audio is streamed from mp3quran.net and is not part of this repository. See [NOTICE](NOTICE) for full attributions, including the open-source libraries the app uses.

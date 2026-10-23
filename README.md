# Mihraya

Prayer times, Quran, recitation, Qibla, and Tasbih for Android. The interface is built for a pure-black OLED screen.

## Install

Download the latest APK from [Releases](https://github.com/jawad-mohammed35/mihraya-android/releases). On your phone, open the file and allow your browser or file manager to install apps when Android asks. Requires Android 8 or newer.

Updates use the same signing key, so you can install a newer APK over the existing app without losing your settings.

## Build

Open the project in Android Studio, or from this folder:

```
./gradlew :app:assembleDebug
```

A release APK needs a `keystore.properties` file in the project root (this file is not in the repository):

```
storeFile=/path/to/mihraya-release.jks
storePassword=...
keyAlias=mihraya
keyPassword=...
```

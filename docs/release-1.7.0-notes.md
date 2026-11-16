## New in Mihraya 1.7.0

- **Zakat Calculator in More:** assets, eligible short-term deductions, gold/silver Nisab, explicit Hawl confirmation, metal weights/purity, optional personal jewelry, and a clear 2.5% estimate. Save/load locally, reset, copy and share the breakdown.
- **Metal pricing:** on-demand USD gold/silver quotes with provider timestamps and dated-quote labels; manual entry remains available. USD, EUR, GBP, CAD, INR, SAR and AED inputs are supported (non-USD prices entered manually).
- **99 Names of Allah:** a fully offline Arabic/transliteration/English list, normalized search, and centered copy/share dialogs. All 396 fields match the attributed Apache-licensed source dataset. Enumerations and English renderings can differ.
- **Learn the Names:** matching in small rounds and four-choice quizzes; choose 5, 10, 20, 50 or all 99 Names, direction, and source/random order. Local scores, completed quizzes, correct matches and recent practice.
- **More header:** corrected left alignment, 48 dp back target, status/cutout insets and main-tab headline typography.
- **Hadith:** removed the requested subtitle while retaining the header, collections, search, bookmarks and Random Hadith.
- Existing appearance/accent settings and data formats are preserved. Same release signing certificate as 1.6.0; install over the previous app without uninstalling.

## Verification

68 unit tests and 11 Android 15 emulator tests pass. Android lint reports 0 errors; signed release build and APK signature verification pass. Names dialogs were checked in every theme/accent, with a large-font/long-Name case. Current market retrieval was tested on-device. Full details and limits: [QA report](https://github.com/jawad-mohammed35/mihraya-android/blob/v1.7.0/docs/release-1.7.0-qa.md).

No physical-device testing was available. The Android 8 emulator did not provide a responsive ADB shell, so API 26 runtime behavior remains unverified. Exhaustive tablet/TalkBack/performance testing was not performed. Share-chooser launch was verified without sending content externally.

Zakat is an estimate; consult a qualified scholar for your school and circumstances, especially jewelry, investments, debts and Hawl.

## Install

Download **Mihraya-1.7.0.apk** below. **SHA256SUMS.txt** provides its checksum. Requires Android 8 or newer. Previous releases and Hadith translation assets remain available.

# Mihraya 1.7.0 QA and release notes

Verified November 16, 2026. Version code 11; application ID remains
`com.mihraya.app`. Minimum supported API remains 26. No existing settings,
bookmarks, Quran/Hadith data schemas, audio/download paths or Tasbih history
formats were migrated, deleted, or replaced.

## Automated checks

The final command succeeded:

```powershell
./gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleRelease '-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true'
```

- 68 JVM tests passed, including 11 new More tools tests. Calculator coverage:
  every monetary category, deductions, zero/negative net wealth, exact/below
  Nisab, gold/silver standards, unconfirmed Hawl, missing prices, invalid amounts,
  large values, decimal rounding, weights, gold purity, jewelry assumptions and
  serialization. Market parsing verifies symbol/currency, positivity, timestamp
  validity, quote age and troy-ounce conversion.
- Game tests cover 20 random seeds across every session size (5/10/20/50/99)
  and all three directions, without repeated question IDs. Each question has
  exactly four distinct source-backed choices and exactly one correct mapping.
  Matching rounds contain at most five pairs and cover the entire session.
  Progress tests cover scores, best percentage and duplicate-session protection.
- 11 Android tests passed on `mihraya_test`, Android 15/API 35, x86_64,
  1080×2400 at 420 dpi (approximately 411 dp wide). This includes the existing
  offline Hadith installation/download/bookmark test, nine More feature tests,
  and one real app navigation regression test.
- UI checks exercise a five-question quiz with a deliberate wrong answer
  (4/5 = 80%), correct-answer disclosure, live counters, persistence, ten matching
  pairs across two rounds with a deliberate wrong match, search, popup, clipboard,
  share-chooser launch intent, manual Zakat calculation, saving/loading/resetting,
  and separate preference persistence.
- Real typed navigation verified: Home → More → Zakat → More → Names → Learn →
  Names → More → Home, then Hadith. The Hadith subtitle is absent and Random
  Hadith remains present. App settings were unchanged by these journeys.
- Header bounds verify a back control within 24 dp of the left edge and a
  48 dp touch target. More uses the same 30 sp/light headline token as main tabs.
- Names dialogs were exercised in Light, Black, OLED Black and System modes,
  across all four accents. A long Arabic Name was checked with 1.5× font scaling
  and a 320 dp content width. Screenshots were inspected for Arabic shaping,
  popup placement, readable feedback, black backgrounds and header alignment.
- Actual USD gold and silver quotes were fetched and parsed successfully on the
  emulator. Provider times, not fetch times, are stored. Manual fallback is
  explicit. No asset amounts are transmitted; pricing is requested only on tap.
- Android lint: 0 errors, 87 warnings. The warnings concern the existing project
  baseline (API deprecations, resources, etc.); no new More-screen lint warning
  remains. Release vital lint passed. Release shrinking/optimization succeeded.

## Names data audit

All 396 fields (99 × number/Arabic/transliteration/meaning) were compared directly
with `name` and English `name_translation` rows from the original Apache-licensed
`my-prayers/muslim-data-flutter` database. They match exactly, including Arabic
code points. Original and bundled hashes are in [names-source-audit.json](names-source-audit.json).
The intermediary snapshot pin is retained in `names-source-pin.txt`. No Names or
meanings were generated. This is a verbatim source/structural audit, not an
independent scholarly review; enumerations and English renderings vary.

## Data and signing

New financial inputs/preferences and learning progress use only
`more_tools_v1.xml`. This file is excluded from cloud backup; existing backup
rules and data files retain their treatment. Android 12+ device transfer can
retain the new file. UI tests use `qa_more_tools_v1.xml` under the separate debug
app ID, leaving the signed app's data alone. The navigation test temporarily
sets debug onboarding/location/haptics options and restores them in `finally`.

The signed, minified APK passes `apksigner verify`. Its certificate SHA-256 is
`daac743f50b1323ba19d7b1ba7d7cb30d032e745ca9c235203a99bf24d174df7`, identical
to the installed previous release. Application ID is unchanged, version code
increases from 10 to 11, and no uninstall/data-clear is required for an update.
The release includes `Mihraya-1.7.0.apk` and `SHA256SUMS.txt`.

## Verification limits

- No physical device was connected. An available Android 8/API 26 emulator was
  started, but its ADB shell was unresponsive even after reported boot completion;
  it was stopped. Minimum-version runtime behavior is therefore unverified.
- On-device full game completion was checked for a five-question quiz and a
  ten-pair matching session. All sizes/directions were validated by JVM generation
  tests, rather than manually completing every possible 99-question session.
- The complete 16-theme/accent matrix was checked for Names dialogs. Other new
  screens were exercised in OLED/Emerald and use shared theme tokens; exhaustive
  interactive checks of every screen/configuration, TalkBack, tablet layouts,
  every cutout shape and performance benchmarks were not performed.
- Native share-chooser launch was verified with an instrumentation monitor. No
  message was sent to an external person or app. Haptic preference integration
  was reviewed; physical vibration strength cannot be assessed on the emulator.
- Existing regression tests passed, but every historical user dataset and every
  existing app action cannot be exhaustively reproduced. No data migrations or
  changes to existing storage were introduced.

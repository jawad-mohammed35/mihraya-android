# Hadith package maintenance

The catalogue is a reviewed allowlist. Each edition identifies its source, revision, attribution, rights statement, numbering, audit limits, byte sizes and SHA-256. The app accepts only catalogue editions; it has no arbitrary-import or credential-entry flow.

## Local import and credential security

HadithAPI requires a credential. Keep it in process environment variable `HADITH_API_KEY` or `.secrets/hadith-api-key.txt`, which is ignored by Git. Do not put it in Gradle properties, BuildConfig, resources, source files, package URLs, release notes, command arguments, or logs. The import client reads it in memory and suppresses sensitive request details in errors. API pagination links contain credentials, so they are discarded rather than cached. The phone contacts only static GitHub release assets and never needs this credential; a runtime proxy is therefore unnecessary for the offline architecture.

Run from the repository root:

```
python tools/audit_hadithapi.py
python tools/prepare_hadithapi.py
python tools/check_release_secrets.py --history --apk app/build/outputs/apk/release/app-release.apk
```

The first tool audits authenticated book/chapter endpoints and every page at up to 200 records per page. It spaces requests, retries temporary failures, caches source-only records under ignored `.secrets/hadithapi-cache/`, and writes `docs/hadithapi-audit.json`. No public request-rate limit was found; the pagination maximum is not a rate-limit promise. Book counts, stable record IDs, source hadith numbers, chapter membership, nested chapter metadata, Arabic text and replacement characters are checked. `chapterId` in narration records is the book-local `chapterNumber`, not the globally unique chapter-row ID; this was verified against the nested chapter and chapters endpoint. Muslim's introduction is chapter 0 and is preserved.

The packager never reads the credential. It checks each snapshot digest, preserves all source strings without textual repairs or machine translation, generates deterministic gzip JSONL packages under `build/hadithapi-packages/`, and updates only HadithAPI catalogue entries. It also stages `.bin`-suffixed copies under `build/hadithapi-qa-assets/` for Android tests; the suffix prevents AAPT from automatically unpacking gzip fixtures. These fixtures are not part of the release APK or Git. Upload all 13 generated `.jsonl.gz` files as assets of the v1.8.0 release before publishing. Package hashes and sizes are pinned; all new text stays downloadable to keep APK size small.

The seven additions retain all 40,465 Arabic records reported by this provider's snapshot. English is missing for Bukhari 1830/6119, seven Tirmidhi records and two Nasa'i records (full lists in the audit). Mishkat has no English in this service and is offered as Arabic with an optional complete Urdu edition. `translationAvailable=false` and the edition's exact `translatedCount` record these gaps. The reader/list/copy output marks absence explicitly; no missing narration or translation is synthesized. Six full Urdu editions are offered; Tirmidhi's Urdu package is omitted because one record lacks its translation. Provider statuses are reproduced with attribution and an explicit note that no named grading scholar is supplied. These are source-structure checks, not scholarly verification or proof of canonical completeness across editions.

Musnad Ahmad and Al-Silsila Sahiha report zero Hadith, so no empty collections are exposed. Muwatta, Darimi, Riyad, Bulugh and Al-Adab are not offered by this provider. The older damaged Arabic editions were not patched speculatively with this source.

## Original editions

`python tools/prepare_hadith.py` reproduces the pinned Fawaz Ahmed source editions, bundled English/Arabic packages and optional French/Turkish/Bengali packages, while retaining HadithAPI catalogue entries. It caches raw source under ignored `build/hadith-source/` and writes `docs/hadith-source-audit.json`. All 42 Nawawi, 40 Qudsi and 40 Dehlawi records remain bundled (183,334 uncompressed bytes). Their optional translations remain hosted on v1.6.0; keep that release available.

## Package and database contract

UTF-8 JSONL contains one schema-1 header (`edition=collection:language`, revision), all source chapter records, then narration records. Stable IDs are strings; never coerce suffixes or renumber records. HadithAPI records use `ha-<source id>` in their new collection namespaces; existing IDs are unchanged. Chapters may include `arabicName`; narrations may include source `heading`, `headingArabic`, `narrator`, `grade`, `isnad`, and explicit translation availability. All Arabic remains required and nonempty. A missing translation must be explicitly declared and match the edition's coverage count.

The importer verifies compressed byte size and SHA-256 before parsing, streams gzip through a strict uncompressed-byte bound, limits individual record size, validates IDs/chapter counts/coverage, and installs in one transaction. Cancellation or failure leaves the prior installation intact. Complete optional language packages must match every base ID, Arabic text, chapter, number and reference. Base-revision changes require a reviewed coordinated migration. Downloads use bounded WorkManager retries; interrupted transfers restart. Removing downloads preserves user bookmarks/anchors and vacuums freed pages.

`hadith-content.db` is separate from every existing settings/Quran/audio/Tasbih store. Database version 2 adds source headings/Arabic chapter titles and an FTS4 index in place; migration from version 1 retains all existing rows and metadata. Search uses indexed normalized word-prefix candidates plus a literal phrase check, with bounded result pages, off the main thread. Random selection queries IDs without loading entire books into memory. `hadith-user.json` remains unchanged in format and retains bookmarks, preferred language, font sizes and reading anchors. No source content migration deletes or rewrites that file.

To exercise the generated packages in the Android importer:

```
./gradlew :app:connectedDebugAndroidTest -PhadithPackageQa=true
```

Without generated fixtures that extra test is explicitly skipped; the original offline regression and migration tests still run. Run with the generated fixtures for release QA. Source and package audits are mechanical; they do not replace scholarly review.

## Rights

The original upstream repository declares The Unlicense, without independently establishing translator/publisher rights. HadithAPI states its API is free, but explicit public bulk/offline redistribution terms were not located. The owner authorized offline use after reviewing that statement. This records the implementation decision and remaining uncertainty, not a legal determination. Preserve NOTICE, the upstream licence and edition attribution/limitations. Mihraya does not claim independent content-rights clearance or apply Apache 2.0 to third-party religious texts/translations. Verified structured relationships/isnad graphs remain omitted when not supplied.

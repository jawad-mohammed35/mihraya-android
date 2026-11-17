# Mihraya 1.8.0 — Names typography and expanded offline Hadith

## Names and Zakat refinements

- Choose Amiri (the existing face), Nastaliq (Noto Nastaliq Urdu, a South Asian style), or Noto Naskh on the 99 Names page. The selection persists and applies to the list, individual popup and learning games. All Names and diacritics remain unchanged. The already-bundled fonts retain SIL OFL 1.1 notices; extra spacing and font padding accommodate Nastaliq's tall marks.
- Learn remains in place. The explanatory/source subtitle beneath it is removed; attribution remains in About and shared text. Both learning games and saved progress are preserved.
- Your annual Zakat estimate now shows the amount prominently at the top. The introductory paragraph and visible Guidance & Sources card are removed. Essential price/Nisab/Hawl/asset-assumption notes and About credits remain. The calculator's decimal arithmetic, Nisab standards, rounding, save/load, reset and sharing logic are unchanged.

## Seven new downloadable Hadith source editions

- Sahih al-Bukhari: **7,276 Arabic records**, English for 7,274; complete Urdu download.
- Sahih Muslim: **7,564 Arabic/English records**; complete Urdu download.
- Jami’ at-Tirmidhi: **3,956 Arabic records**, English for 3,949. The incomplete Urdu edition is not offered.
- Sunan Abi Dawud: **5,274 Arabic/English records**; complete Urdu download.
- Sunan Ibn Majah: **4,341 Arabic/English records**; complete Urdu download.
- Sunan an-Nasa’i: **5,761 Arabic records**, English for 5,759; complete Urdu download.
- Mishkat al-Masabih: **6,293 Arabic records**; complete Urdu download. **This provider supplies no English for Mishkat.**

Together with the existing Nawawi (42), Qudsi (40) and Dehlawi (40) editions, Mihraya offers 10 collections with **40,587 Arabic source records**. The three small existing collections remain bundled. New books use compressed, checksum-pinned downloads so they do not inflate the APK; once installed, reading, indexed search, bookmarks, copy/share and Random Hadith work offline. Arabic source chapter titles, source headings, narrator fields, numbering and Muslim's chapter-zero introduction are retained. Existing bookmarks and reading positions are preserved through an in-place content-index migration.

All new records reported by the provider were audited for source IDs, chapter membership/counts, numbering and nonempty/undamaged Arabic. This is **source-level structural verification, not scholarly verification or proof of completeness across other print editions**. Missing English is explicitly marked unavailable, with the original Arabic still accessible. No missing text, translation, narrator or grading is generated. Status labels are attributed to HadithAPI; it does not name the grading scholar, so Mihraya does not present them as independently verified scholarly assessments or blanket-label every collection authentic.

## Source and security limits

The new data is an authenticated November 17, 2026 HadithAPI.com snapshot. The credential is used only by local import tooling, stays in an ignored file/environment variable, and is absent from app code, APKs, downloadable packages and logs. Phones require no API secret or backend credential.

The provider describes its service as free. Explicit public request-rate limits and bulk/offline redistribution terms were not located; the app owner authorized offline use after reviewing the public statement. Independent translator/publisher rights and translation authorship have not been cleared. Mihraya's Apache 2.0 licence is unchanged; third-party source terms and limitations remain in NOTICE and edition credits.

Musnad Ahmad and Al-Silsila Sahiha report zero Hadith in this API and were not exposed as empty collections. Muwatta, Darimi, Riyad, Bulugh and Al-Adab are not offered by it. Speculative similar-narration links and extracted isnad graphs remain omitted where reliable structured data is absent.

Install **Mihraya-1.8.0.apk** over the existing app; the signing key is unchanged. The `.jsonl.gz` attachments are static content packages used by the app. Previous releases, including the older translation packages referenced by the catalogue, are preserved. See `docs/qa/1.8.0.md`, `docs/hadithapi-audit.json`, `docs/hadithapi-packages.json` and `docs/hadith-data.md` for the audit, maintenance steps and test limits.

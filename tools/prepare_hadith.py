"""Reproduce the pinned offline editions, preserving source text byte-for-byte as Unicode.
No source text is repaired, summarized, translated, or renumbered. Run from repository root.
"""
import concurrent.futures
import hashlib
import json
import pathlib
import urllib.request

SHA = 'df57907be35291c91ad6a6691180e22ca9920784'
ROOT = pathlib.Path('build/hadith-source')
ASSETS = pathlib.Path('app/src/main/assets/hadith')
PACKAGES = pathlib.Path('build/hadith-packages')
BASE = f'https://raw.githubusercontent.com/fawazahmed0/hadith-api/{SHA}/'
RELEASE = 'https://github.com/jawad-mohammed35/mihraya-android/releases/download/v1.6.0/'
NAMES = {'nawawi': "An-Nawawi's Forty Hadith", 'qudsi': 'Forty Hadith Qudsi', 'dehlawi': "Shah Waliullah's Forty Hadith"}
LANGUAGES = {'eng': ('en', 'English'), 'fra': ('fr', 'French'), 'tur': ('tr', 'Turkish'), 'ben': ('bn', 'Bengali')}
APPROVED = [('nawawi', 42, ['eng', 'fra', 'tur', 'ben']), ('qudsi', 40, ['eng', 'fra']), ('dehlawi', 40, ['eng', 'fra'])]
AUDIT_BOOKS = ['bukhari', 'muslim', 'tirmidhi', 'abudawud', 'nasai', 'ibnmajah', 'malik']


def fetch(edition):
    path = ROOT / (edition + '.json')
    if not path.exists():
        path.write_bytes(urllib.request.urlopen(BASE + 'editions/' + edition + '.json', timeout=90).read())
    return json.loads(path.read_text(encoding='utf8'))


def key(number):
    return str(number)


def audit(edition):
    data = fetch(edition)
    rows = data['hadiths']
    return {'edition': edition, 'records': len(rows),
            'blankRecords': [key(r['hadithnumber']) for r in rows if not r.get('text', '').strip()],
            'damagedRecords': [key(r['hadithnumber']) for r in rows if '\ufffd' in r.get('text', '')],
            'sha256': hashlib.sha256((ROOT / (edition + '.json')).read_bytes()).hexdigest()}


def encode(value):
    return json.dumps(value, ensure_ascii=False, separators=(',', ':'))


def build():
    ROOT.mkdir(parents=True, exist_ok=True)
    ASSETS.mkdir(parents=True, exist_ok=True)
    PACKAGES.mkdir(parents=True, exist_ok=True)
    source_editions = [f'{lang}-{book}' for book, _, langs in APPROVED for lang in ['ara'] + langs]
    source_editions += [f'{lang}-{book}' for book in AUDIT_BOOKS for lang in ['ara', 'eng']]
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        reports = list(pool.map(audit, source_editions))
    by_edition = {r['edition']: r for r in reports}
    catalogue = []
    for book, count, languages in APPROVED:
        arabic = fetch('ara-' + book)
        arows = arabic['hadiths']
        assert len(arows) == count
        assert {r['hadithnumber'] for r in arows} == set(range(1, count + 1))
        for lang in languages:
            edition_id = lang + '-' + book
            for source_id in ['ara-' + book, edition_id]:
                assert not by_edition[source_id]['blankRecords'], source_id
                assert not by_edition[source_id]['damagedRecords'], source_id
            translation = fetch(edition_id)
            trows = translation['hadiths']
            assert len(trows) == count
            assert [r['hadithnumber'] for r in arows] == [r['hadithnumber'] for r in trows]
            iso, language = LANGUAGES[lang]
            edition_key = book + ':' + iso
            chapters = []
            for section_id, name in translation['metadata']['sections'].items():
                members = [r for r in trows if key(r['reference']['book']) == section_id]
                if members:
                    assert name.strip()
                    chapters.append({'id': section_id, 'name': name, 'count': len(members)})
            assert sum(c['count'] for c in chapters) == count
            lines = [{'type': 'header', 'schema': 1, 'edition': edition_key, 'revision': SHA}]
            lines += [{'type': 'chapter', 'chapter': c} for c in chapters]
            for a, t in zip(arows, trows):
                assert a['reference'] == t['reference']
                assert a['arabicnumber'] == t['arabicnumber']
                n = key(t['hadithnumber'])
                assert a['text'].strip() and t['text'].strip()
                grade = '\n'.join(f"{g['name']}: {g['grade']}" for g in t.get('grades', []) if g.get('name') and g.get('grade'))
                lines.append({'type': 'hadith', 'hadith': {
                    'id': n, 'chapter': key(t['reference']['book']), 'number': n,
                    'arabic': a['text'], 'translation': t['text'],
                    'reference': f'{NAMES[book]} {n} · Book {t["reference"]["book"]}, Hadith {t["reference"]["hadith"]}',
                    'grade': grade}})
            package = ('\n'.join(encode(line) for line in lines) + '\n').encode('utf8')
            filename = f'{book}-{iso}.jsonl'
            (PACKAGES / filename).write_bytes(package)
            asset = 'hadith/' + filename if iso == 'en' else ''
            if asset:
                (ASSETS / filename).write_bytes(package)
            catalogue.append({'collection': book, 'name': NAMES[book],
                'description': f'{count} narrations in the source edition, with Arabic and English included offline.',
                'language': iso, 'languageName': language, 'rtl': False, 'revision': SHA,
                'source': f'https://github.com/fawazahmed0/hadith-api/tree/{SHA}',
                'attribution': f'Hadith API by Fawaz Ahmed; source also used by UmmahAPI. {language} edition: {edition_id}. Translation authorship is not independently verified.',
                'license': 'Upstream repository: The Unlicense. Underlying translation rights are not independently cleared. See NOTICE.',
                'numbering': f'Hadith API {edition_id}: source hadithnumber and in-book reference; other editions may differ.',
                'verification': f'All {count} source records matched by ID and in-book reference, with nonempty text and no Unicode replacement characters. Not a scholarly textual audit.',
                'url': RELEASE + filename, 'sha256': hashlib.sha256(package).hexdigest(),
                'bytes': len(package), 'count': count, 'chapters': len(chapters), 'bundledAsset': asset})
    catalogue_file = ASSETS / 'catalogue.json'
    if catalogue_file.exists():
        catalogue += [c for c in json.loads(catalogue_file.read_text(encoding='utf8')) if c['source'].startswith('https://hadithapi.com')]
    catalogue_file.write_text(json.dumps(catalogue, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    report = {'sourceRevision': SHA, 'retrieved': '2026-11-15', 'checks': reports,
              'includedCollections': list(NAMES), 'excludedLargerCollections': AUDIT_BOOKS,
              'limits': 'Structural/source equality checks only. No claim of scholarly verification or independent translation rights clearance. Blank upstream records were not silently removed to manufacture complete editions.'}
    pathlib.Path('docs/hadith-source-audit.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf8')
    license_bytes = urllib.request.urlopen(BASE + 'LICENSE', timeout=30).read()
    pathlib.Path('licenses/Hadith-API-Unlicense.txt').write_bytes(license_bytes)
    print(json.dumps({'packages': len(catalogue), 'bundledRecords': sum(c['count'] for c in catalogue if c['language']=='en'), 'bundledBytes': sum(c['bytes'] for c in catalogue if c['language']=='en')}))

if __name__ == '__main__':
    build()

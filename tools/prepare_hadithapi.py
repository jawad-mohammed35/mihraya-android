"""Build credential-free, deterministic static packages from an audited local snapshot.
Run tools/audit_hadithapi.py first. No API key is read or needed by this packager.
"""
import collections,gzip,hashlib,json,pathlib
ROOT=pathlib.Path(__file__).resolve().parents[1]
CACHE=ROOT/'.secrets/hadithapi-cache'
OUT=ROOT/'build/hadithapi-packages'
CATALOGUE=ROOT/'app/src/main/assets/hadith/catalogue.json'
NAMES={'sahih-bukhari':('bukhari','Sahih al-Bukhari'),'sahih-muslim':('muslim','Sahih Muslim'),'al-tirmidhi':('tirmidhi',"Jami’ at-Tirmidhi"),'abu-dawood':('abudawud','Sunan Abi Dawud'),'ibn-e-majah':('ibnmajah','Sunan Ibn Majah'),'sunan-nasai':('nasai',"Sunan an-Nasa’i"),'mishkat':('mishkat','Mishkat al-Masabih')}

def encode(value):return json.dumps(value,ensure_ascii=False,separators=(',',':'))

def main():
 audit=json.loads((ROOT/'docs/hadithapi-audit.json').read_text(encoding='utf8'))
 approved=[b for b in audit['books'] if b.get('passedArabicStructuralAudit')]
 assert len(approved)==7,'Every intended source edition must pass the Arabic structure audit'
 OUT.mkdir(parents=True,exist_ok=True)
 qa=ROOT/'build/hadithapi-qa-assets/hadithapi';qa.mkdir(parents=True,exist_ok=True)
 # AAPT automatically expands .gz assets. .bin keeps the real wire bytes for importer tests.
 for old in qa.glob('*.gz'):old.unlink()
 catalogue=[c for c in json.loads(CATALOGUE.read_text(encoding='utf8')) if not c['source'].startswith('https://hadithapi.com')]
 package_report=[]
 for book in approved:
  slug=book['collection'];collection,name=NAMES[slug]
  snapshot=json.loads((CACHE/slug/'snapshot.json').read_text(encoding='utf8'))
  canonical=json.dumps(snapshot,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode('utf8')
  assert hashlib.sha256(canonical).hexdigest()==book['snapshotSha256']
  revision='hadithapi-20261117-'+book['snapshotSha256'][:12]
  rows=snapshot['hadiths'];counts=collections.Counter(str(r['chapterId']) for r in rows)
  assert len(rows)==book['reportedHadiths'] and len({r['id'] for r in rows})==len(rows)
  assert len({r['hadithNumber'] for r in rows})==len(rows)
  source_chapters={str(c['chapterNumber']):c for c in snapshot['chapters']}
  languages=['en']+(['ur'] if not book['problems'].get('unavailable_hadithUrdu') else [])
  for language in languages:
   translated=sum(bool((r.get('hadithEnglish' if language=='en' else 'hadithUrdu') or '').strip()) for r in rows)
   lines=[{'type':'header','schema':1,'edition':collection+':'+language,'revision':revision}]
   for ch in snapshot['chapters']:
    number=str(ch['chapterNumber']);assert counts[number]>0
    assert ch['bookSlug']==slug
    lines.append({'type':'chapter','chapter':{'id':number,'name':ch['chapterEnglish'],'arabicName':ch.get('chapterArabic') or '', 'count':counts[number]}})
   for r in rows:
    chapter=source_chapters[str(r['chapterId'])];assert r['chapter']==chapter
    assert r['bookSlug']==slug and r['hadithArabic'].strip()
    translation=r.get('hadithEnglish' if language=='en' else 'hadithUrdu') or ''
    narrator=r.get('englishNarrator' if language=='en' else 'urduNarrator') or ''
    for text in [r['hadithArabic'],translation,narrator,r.get('headingArabic') or '',r.get('headingEnglish') or '']:assert '\ufffd' not in text
    reference=f'{name} {r["hadithNumber"]} · HadithAPI chapter {r["chapterId"]}'
    if r.get('volume') is not None:reference+=f', volume {r["volume"]}'
    lines.append({'type':'hadith','hadith':{'id':'ha-'+str(r['id']),'chapter':str(r['chapterId']),'number':r['hadithNumber'],'arabic':r['hadithArabic'],'translation':translation,'translationAvailable':bool(translation.strip()),'reference':reference,'narrator':narrator,'heading':r.get('headingEnglish') or '', 'headingArabic':r.get('headingArabic') or '', 'grade':('HadithAPI status: '+r['status']+' (grader not named by API)') if r.get('status') else ''}})
   raw=('\n'.join(encode(line) for line in lines)+'\n').encode('utf8')
   packed=gzip.compress(raw,compresslevel=9,mtime=0)
   filename=f'hadithapi-{collection}-{language}.jsonl.gz';(OUT/filename).write_bytes(packed)
   (qa/(filename+'.bin')).write_bytes(packed)
   assert len(raw)<=100*1024*1024
   description=f'{len(rows):,} Arabic source records; {translated:,} English translations.' if language=='en' else f'{len(rows):,} Arabic source records with Urdu translation.'
   if language=='en' and translated==0:description=f'{len(rows):,} Arabic source records. English is not provided; Urdu is available to download.'
   edition={'collection':collection,'name':name,'description':description,'language':language,'languageName':('Arabic' if translated==0 else 'English') if language=='en' else 'Urdu','rtl':language=='ur','revision':revision,'source':'https://hadithapi.com/','attribution':'HadithAPI.com · source text, chapter names, numbering and provider status preserved. Translation authorship and named grading scholars are not supplied.','license':'Provider describes the API as free. Offline use authorized by the app owner; explicit public redistribution terms and independent translator rights were not established.','numbering':'HadithAPI '+slug+' hadithNumber and chapterNumber, including chapter 0 where supplied. Other editions may differ.','verification':f'All {len(rows)} reported source records and {len(snapshot["chapters"])} chapters were audited. Arabic, IDs, references and source counts passed structural checks. {translated} translations are present; missing translations remain explicitly unavailable. No scholarly textual or grading verification is claimed.','url':'https://github.com/jawad-mohammed35/mihraya-android/releases/download/v1.8.0/'+filename,'sha256':hashlib.sha256(packed).hexdigest(),'bytes':len(packed),'uncompressedBytes':len(raw),'compression':'gzip','count':len(rows),'translatedCount':translated,'chapters':len(snapshot['chapters']),'bundledAsset':''}
   catalogue.append(edition)
   package_report.append({'file':filename,'records':len(rows),'translations':translated,'compressedBytes':len(packed),'uncompressedBytes':len(raw),'sha256':edition['sha256']})
 assert len({c['collection'] for c in catalogue if c['language']=='en'})==10
 assert len({c['collection']+':'+c['language'] for c in catalogue})==len(catalogue)
 CATALOGUE.write_text(json.dumps(catalogue,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
 (ROOT/'docs/hadithapi-packages.json').write_text(json.dumps(package_report,indent=2)+'\n',encoding='utf8')
 print(json.dumps({'newCollections':len(approved),'newSourceRecords':sum(b['records'] for b in approved),'packages':len(package_report),'downloadBytes':sum(p['compressedBytes'] for p in package_report),'englishTranslations':sum(p['translations'] for p in package_report if '-en.' in p['file'])}))

if __name__=='__main__':main()

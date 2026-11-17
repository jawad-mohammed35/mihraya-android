"""Local authenticated source audit. Never puts credentials in Android code or output.
API key: HADITH_API_KEY environment variable, or ignored .secrets/hadith-api-key.txt.
Raw source-only snapshots stay in ignored .secrets/hadithapi-cache/.
"""
import collections,hashlib,json,os,pathlib,time,urllib.error,urllib.parse,urllib.request
ROOT=pathlib.Path(__file__).resolve().parents[1]
CACHE=ROOT/'.secrets/hadithapi-cache'
REPORT=ROOT/'docs/hadithapi-audit.json'

class AuditError(Exception): pass

class SameOriginRedirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,request,fp,code,msg,headers,newurl):
  destination=urllib.parse.urlparse(newurl)
  if destination.scheme!='https' or destination.hostname not in ('hadithapi.com','www.hadithapi.com') or destination.port not in (None,443):
   raise AuditError('Cross-origin or insecure API redirect refused; URL suppressed')
  return super().redirect_request(request,fp,code,msg,headers,newurl)

class Client:
 def __init__(self):
  keyfile=ROOT/'.secrets/hadith-api-key.txt'
  self.key=os.environ.get('HADITH_API_KEY','').strip() or (keyfile.read_text(encoding='utf-8-sig').strip() if keyfile.exists() else '')
  if not self.key:raise AuditError('Credential is missing from the local environment/file')
  self.opener=urllib.request.build_opener(SameOriginRedirect())
  self.last=0
 def get(self,path,params=None):
  # Do not accept or follow pagination URLs that can contain the credential.
  if not path.replace('-','').replace('/','').isalnum():raise AuditError('Invalid endpoint')
  args=dict(params or {},apiKey=self.key)
  request=urllib.request.Request('https://hadithapi.com/api/'+path+'?'+urllib.parse.urlencode(args),headers={'Accept':'application/json','User-Agent':'Mihraya-Source-Audit/1.0'})
  for attempt in range(5):
   time.sleep(max(0,0.8-(time.monotonic()-self.last)));self.last=time.monotonic()
   try:
    with self.opener.open(request,timeout=60) as response:
     if urllib.parse.urlparse(response.url).hostname not in ('hadithapi.com','www.hadithapi.com'):raise AuditError('Unexpected response host')
     return json.load(response)
   except urllib.error.HTTPError as error:
    if error.code not in (408,429,500,502,503,504) or attempt==4:raise AuditError(f'Provider HTTP {error.code}; credential and response URL suppressed') from None
    time.sleep(min(2**attempt,16))
   except (OSError,ValueError):
    if attempt==4:raise AuditError('Provider request failed; URL suppressed') from None
  raise AuditError('Request failed')
 def source_only(self,value):
  if isinstance(value,dict):return {k:self.source_only(v) for k,v in value.items() if k.lower() not in ('apikey','api_key') and not k.endswith('_url') and k!='path'}
  if isinstance(value,list):return [self.source_only(v) for v in value]
  if isinstance(value,str):return value.replace(self.key,'[REDACTED]')
  return value

def write(path,data):
 path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')

def audit(client,book):
 slug=book['bookSlug'];folder=CACHE/slug;folder.mkdir(parents=True,exist_ok=True)
 chapfile=folder/'chapters.json'
 if not chapfile.exists():write(chapfile,client.source_only(client.get(slug+'/chapters')['chapters']))
 chapters=json.loads(chapfile.read_text(encoding='utf8'))
 records=[];page=1;total=None
 while True:
  cache=folder/f'page-{page}.json'
  if cache.exists():data=json.loads(cache.read_text(encoding='utf8'))
  else:
   raw=client.get('hadiths',{'book':slug,'paginate':200,'page':page})['hadiths']
   data={'current_page':raw['current_page'],'last_page':raw['last_page'],'total':raw['total'],'data':client.source_only(raw['data'])}
   write(cache,data)
  if total is None:total=data['total']
  if data['total']!=total or data['current_page']!=page:raise AuditError('Pagination changed during snapshot; restart audit')
  records.extend(data['data'])
  if page>=data['last_page']:break
  page+=1
 ids=[str(r['id']) for r in records];numbers=[str(r['hadithNumber']) for r in records]
 chapter_ids={str(c['chapterNumber']):c for c in chapters}
 problems=collections.defaultdict(list)
 counts=collections.Counter()
 for r in records:
  number=str(r.get('hadithNumber',''));rid=str(r['id'])
  if r.get('bookSlug')!=slug:problems['wrongCollection'].append(rid)
  if not number.strip():problems['missingNumber'].append(rid)
  ch=chapter_ids.get(str(r.get('chapterId')))
  if ch is None:problems['unknownChapter'].append(rid)
  elif r.get('chapter',{})!=ch:problems['chapterMismatch'].append(rid)
  else:counts[str(ch['chapterNumber'])]+=1
  for field in ('hadithArabic','hadithEnglish'):
   text=r.get(field) or ''
   if not text.strip():problems['blank_'+field].append(number)
   if '\ufffd' in text:problems['damaged_'+field].append(number)
  if not any('\u0621'<=c<='\u064a' for c in r.get('hadithArabic','')):problems['noArabic'].append(number)
  for field in ('hadithUrdu',):
   if not (r.get(field) or '').strip() or '\ufffd' in (r.get(field) or ''):problems['unavailable_'+field].append(number)
 if len(records)!=total or total!=int(book['hadiths_count']):problems['countMismatch']=[len(records),total,book['hadiths_count']]
 if len(set(ids))!=len(ids):problems['duplicateIds']=[n for n,c in collections.Counter(ids).items() if c>1]
 if len(set(numbers))!=len(numbers):problems['duplicateNumbers']=[n for n,c in collections.Counter(numbers).items() if c>1]
 if len(chapters)!=int(book['chapters_count']):problems['chapterCountMismatch']=[len(chapters),book['chapters_count']]
 for c in chapters:
  if not (c.get('chapterEnglish') or '').strip():problems['missingChapterName'].append(str(c['id']))
  if not counts[str(c['chapterNumber'])]:problems['emptyChapters'].append(str(c['chapterNumber']))
 required={k:v for k,v in problems.items() if not k.startswith('unavailable_')}
 snapshot={'book':book,'chapters':chapters,'hadiths':records}
 encoded=json.dumps(snapshot,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode('utf8')
 write(folder/'snapshot.json',snapshot)
 arabic_required={k:v for k,v in required.items() if k!='blank_hadithEnglish'}
 return {'collection':slug,'reportedHadiths':book['hadiths_count'],'records':len(records),'reportedChapters':book['chapters_count'],'chapters':len(chapters),'passedStructuralAudit':not required,'passedArabicStructuralAudit':not arabic_required,'englishTranslations':sum(bool((r.get('hadithEnglish') or '').strip()) for r in records),'problems':dict(problems),'statuses':dict(collections.Counter(r.get('status') or 'Unspecified' for r in records)),'snapshotSha256':hashlib.sha256(encoded).hexdigest()}

def main():
 client=Client();CACHE.mkdir(parents=True,exist_ok=True)
 books=client.source_only(client.get('books')['books']);write(CACHE/'books.json',books)
 report={'provider':'https://hadithapi.com','retrieved':'2026-11-17','credentialHandling':'Local import only; credential is not stored in snapshots, app code, APKs or logs. Pagination URLs are discarded.','terms':'User authorized use after reviewing the public free-access statement. Public rate limits and explicit bulk/offline redistribution rights were not located; no independent content-rights clearance is claimed.','books':[]}
 for book in books:
  if not int(book['hadiths_count']):
   result={'collection':book['bookSlug'],'records':0,'passedStructuralAudit':False,'problems':{'noHadithRecords':True}}
  else:result=audit(client,book)
  report['books'].append(result);write(REPORT,report)
  print(json.dumps({'collection':result['collection'],'records':result['records'],'passed':result['passedStructuralAudit'],'problemCounts':{k:len(v) if isinstance(v,list) else v for k,v in result['problems'].items()}},ensure_ascii=True),flush=True)
if __name__=='__main__':
 try:main()
 except AuditError as error:print(str(error));raise SystemExit(1)
 except Exception as error:print('Source audit failed: '+type(error).__name__+'; sensitive request details suppressed');raise SystemExit(1)

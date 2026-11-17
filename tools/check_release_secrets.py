"""Check the local HadithAPI credential against tracked/untracked publishable files and APK entries.
Only a boolean result and affected file paths are reported; the credential is never printed.
"""
import argparse,gzip,json,os,pathlib,subprocess,urllib.parse,zipfile
root=pathlib.Path(__file__).resolve().parents[1]
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--apk',type=pathlib.Path);parser.add_argument('--packages',type=pathlib.Path);parser.add_argument('--history',action='store_true');args=parser.parse_args()
 keyfile=root/'.secrets/hadith-api-key.txt'
 key=os.environ.get('HADITH_API_KEY','').strip() or (keyfile.read_text(encoding='utf-8-sig').strip() if keyfile.exists() else '')
 if not key:raise SystemExit('Local credential is required for the exact-value release check; no credential was printed')
 variants=[key.encode('utf8'),key.encode('utf-16le'),urllib.parse.quote(key,safe='').encode('ascii'),urllib.parse.quote_plus(key).encode('ascii')]
 def matches(data):return any(v and v in data for v in variants)
 names=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=root).decode('utf8').split('\0')
 bad=[]
 for name in filter(None,names):
  path=root/name
  if path.is_file() and matches(path.read_bytes()):bad.append(name)
 entries=0
 packages=0
 if args.packages:
  for package in args.packages.glob('*.gz'):
   packages+=1
   if matches(gzip.decompress(package.read_bytes())):bad.append('Package:'+package.name)
 if args.apk:
  with zipfile.ZipFile(args.apk) as apk:
   for entry in apk.namelist():
    entries+=1
    if matches(apk.read(entry)):bad.append('APK:'+entry)
 history=False
 if args.history:history=matches(subprocess.check_output(['git','log','--all','--format=','-p'],cwd=root))
 print(json.dumps({'publishableFilesScanned':len([n for n in names if n]),'apkEntriesScanned':entries,'packagesScanned':packages,'credentialMatches':bad,'historyMatch':history}))
 if bad or history:raise SystemExit(1)
if __name__=='__main__':main()

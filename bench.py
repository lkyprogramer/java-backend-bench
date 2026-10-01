#!/usr/bin/env python3
"""ReserveLedger Bench command-line entrypoint. Python standard library only."""
import argparse,hashlib,json,shutil,sys,traceback
from pathlib import Path
from judge.runtime import ROOT
from judge.runner import VERSION,calibrate,doctor,grade,write_json
from judge.aggregate import aggregate

def main():
    parser=argparse.ArgumentParser(description='ReserveLedger Java Backend Bench '+VERSION);sub=parser.add_subparsers(dest='command',required=True)
    tracks=('base','t1-spring')
    doc=sub.add_parser('doctor');doc.add_argument('--track',choices=tracks,default='base')
    prep=sub.add_parser('prepare');prep.add_argument('--dest',required=True);prep.add_argument('--track',choices=tracks,default='base')
    cal=sub.add_parser('calibrate');cal.add_argument('--out',required=True);cal.add_argument('--reps',type=int,default=3);cal.add_argument('--allow-exec',action='store_true');cal.add_argument('--track',choices=tracks,default='base')
    g=sub.add_parser('grade');g.add_argument('--workspace',required=True);g.add_argument('--out',required=True);g.add_argument('--model-label',required=True);g.add_argument('--run-id',required=True);g.add_argument('--seed',type=int,default=1701);g.add_argument('--metadata');g.add_argument('--review');g.add_argument('--calibration');g.add_argument('--perf-reps',type=int,default=3);g.add_argument('--skip-performance',action='store_true');g.add_argument('--skip-mutation',action='store_true');g.add_argument('--allow-exec',action='store_true');g.add_argument('--track',choices=tracks,default='base')
    c=sub.add_parser('compare');c.add_argument('reports',nargs='+');c.add_argument('--out',required=True)
    sub.add_parser('verify');cat=sub.add_parser('catalog');cat.add_argument('--track',choices=tracks,default='base')
    a=parser.parse_args()
    if a.command in ('grade','calibrate'):
        if not a.allow_exec:parser.error('Use --allow-exec only in an isolated, non-sensitive environment. Submitted Java/Python code will run with your account privileges.')
        count=a.perf_reps if a.command=='grade'else a.reps
        if not 1<=count<=20:parser.error('performance repetitions must be 1..20')
    if a.command=='doctor':
        result=doctor(a.track);print(json.dumps(result,ensure_ascii=False,indent=2));return 0 if result['ready']else 2
    if a.command=='prepare':
        dest=Path(a.dest).resolve()
        if dest.exists():raise ValueError('Destination already exists; refusing to overwrite')
        source=ROOT/'candidate'if a.track=='base'else ROOT/'tracks/t1-spring/candidate'
        shutil.copytree(source,dest,ignore=shutil.ignore_patterns('build','target','__pycache__','.DS_Store','data'))
        if a.track=='t1-spring':
            from judge.spring import m2_repo
            # Offline-only Maven for the candidate. The repository holds third-party jars only, outside this tree.
            (dest/'.mvn').mkdir(exist_ok=True);(dest/'.mvn/maven.config').write_text('-o\n-Dmaven.repo.local='+str(m2_repo())+'\n')
        print(dest);return 0
    if a.command=='calibrate':calibrate(a.out,a.reps,a.track);return 0
    if a.command=='grade':
        result=grade(a)
        if not result['validity']['rankable']:return 2
        return 0 if result['summary']['gate']=='NO_CRITICAL_FAILURE_OBSERVED'else 1
    if a.command=='compare':aggregate(a.reports,a.out);print(Path(a.out).resolve()/'comparison.md');return 0
    if a.command=='catalog':
        from judge.tracks import track
        CASES=track(a.track).cases()
        print(json.dumps([{k:v for k,v in s.items()if k!='fn'}for s in CASES.values()],ensure_ascii=False,indent=2));return 0
    if a.command=='verify':
        path=ROOT/'SHA256SUMS.json'
        if not path.exists():raise ValueError('SHA256SUMS.json is missing')
        expected=json.loads(path.read_text());bad=[]
        for rel,digest in expected.items():
            p=ROOT/rel
            if not p.is_file()or hashlib.sha256(p.read_bytes()).hexdigest()!=digest:bad.append(rel)
        print(json.dumps({'verified':not bad,'files':len(expected),'mismatches':bad},ensure_ascii=False,indent=2));return 0 if not bad else 2
    return 2
if __name__=='__main__':
    try:raise SystemExit(main())
    except (ValueError,RuntimeError,OSError,json.JSONDecodeError)as e:print('ERROR: '+str(e),file=sys.stderr);raise SystemExit(2)

#!/usr/bin/env python3
"""Sequentially grade completed workspaces. This does not call or configure LLMs."""
import argparse,json,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from judge.runner import fresh_out,write_json
from judge.aggregate import aggregate

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('matrix');p.add_argument('--out',required=True)
    p.add_argument('--base-dir',default='.',help='Base for all relative paths inside the JSON; defaults to current directory')
    p.add_argument('--allow-exec',action='store_true');a=p.parse_args()
    if not a.allow_exec:p.error('--allow-exec required for untrusted submitted code')
    matrix=json.loads(Path(a.matrix).read_text());runs=matrix.get('runs',[])
    if not isinstance(runs,list)or not runs:p.error('matrix.runs must be a nonempty list')
    base=Path(a.base_dir).resolve()
    def path(v):return str((base/str(v)).resolve())
    planned=[];seen=set()
    for row in runs:
        for key in ('workspace','out','model_label','run_id'):
            if not row.get(key):p.error('Each run requires '+key)
        output=path(row['out'])
        if output in seen:p.error('Duplicate run output directory')
        seen.add(output)
        cmd=[sys.executable,str(ROOT/'bench.py'),'grade','--workspace',path(row['workspace']),'--out',output,
             '--model-label',row['model_label'],'--run-id',row['run_id'],'--seed',str(row.get('seed',1701)),'--allow-exec']
        for key in ('metadata','review','calibration'):
            value=row.get(key,matrix.get(key))
            if value:cmd+=['--'+key,path(value)]
        planned.append((row,cmd,output))
    out=fresh_out(a.out);results=[];reports=[]
    for index,(row,cmd,output)in enumerate(planned):
        print(f'[{index+1}/{len(planned)}] {row["run_id"]}',flush=True)
        with (out/f'run-{index+1}.log').open('wb')as log:rc=subprocess.call(cmd,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
        report=Path(output)/'report.json'
        results.append({'run_id':row['run_id'],'returncode':rc,'report':str(report)if report.exists()else None,'log':f'run-{index+1}.log'})
        if report.exists():reports.append(report)
        write_json(out/'matrix-results.json',results)
    if reports:aggregate(reports,out/'comparison')
    print(out/'matrix-results.json')
    return 2 if any(r['returncode']==2 for r in results)else 1 if any(r['returncode']!=0 for r in results)else 0
if __name__=='__main__':
    try:raise SystemExit(main())
    except (OSError,ValueError,RuntimeError,json.JSONDecodeError)as e:print('ERROR: '+str(e),file=sys.stderr);raise SystemExit(2)

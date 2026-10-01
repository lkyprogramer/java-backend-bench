"""Trusted orchestration, immutable source snapshot, and machine-readable reports."""
from __future__ import annotations
import csv,datetime,hashlib,json,os,platform,re,shutil,sys,time,xml.etree.ElementTree as ET
from pathlib import Path
from .runtime import ROOT,Context,Server,build,digest_tree,environment,frozen_ok,minimal_env,rss_bytes,run_limited
from .cases import CASES
from .scoring import DIMENSIONS,case_score,summarize
from .perf import PROFILE_HASH,compare

VERSION=(ROOT/'VERSION').read_text().strip()
EXCLUDES={'build','.git','__pycache__','.pytest_cache','.DS_Store','data','.idea','.vscode'}

def write_json(path,obj):
    Path(path).write_text(json.dumps(obj,ensure_ascii=False,indent=2,allow_nan=False)+'\n',encoding='utf-8')

def trusted_hash():
    h=hashlib.sha256()
    paths=[]
    for directory in ('judge','reference/src','reference/tests','candidate/src','candidate/docs'):
        paths += [p for p in (ROOT/directory).rglob('*')if p.is_file()and p.suffix in ('.py','.java','.md','.json')]
    paths += [p for p in [ROOT/'VERSION',ROOT/'candidate/TASK.md',ROOT/'candidate/AGENTS.md']if p.exists()]
    for p in sorted(paths):h.update(str(p.relative_to(ROOT)).encode());h.update(b'\0');h.update(p.read_bytes());h.update(b'\0')
    return h.hexdigest()

def fresh_out(path):
    path=Path(path).resolve()
    if path.exists()and any(path.iterdir()):raise ValueError(f'Output directory is not empty: {path}')
    path.mkdir(parents=True,exist_ok=True);return path

def snapshot(source,target):
    source=Path(source).resolve();target=Path(target).resolve()
    if not source.is_dir():raise ValueError('Workspace is not a directory')
    if source==target or source in target.parents:raise ValueError('Snapshot/output must be outside the source workspace')
    def ignored(rel):
        return any(v in {'.git','__pycache__','.pytest_cache','.DS_Store'} for v in rel.parts) or (bool(rel.parts) and rel.parts[0] in EXCLUDES)
    all_files=[p for p in source.rglob('*')if not ignored(p.relative_to(source))]
    if any(p.is_symlink()for p in all_files):raise ValueError('Submission contains a symbolic link; remove it before evaluation')
    total=sum(p.stat().st_size for p in all_files if p.is_file())
    if len(all_files)>4000 or total>128*1024*1024:raise ValueError('Submission exceeds 4000 entries / 128 MiB limit')
    def ignore(directory,names):
        return [name for name in names if ignored((Path(directory)/name).relative_to(source))]
    shutil.copytree(source,target,ignore=ignore);return digest_tree(target)

def critical_failures(rows):
    """Flagged critical cases, plus any case that observed an S0 restart failure (e.g. a D08 sequence)."""
    return [r['id']for r in rows if r['status']!='pass'and(r.get('critical')or r.get('severity')=='S0')]

def failure_signature(message):
    """Heuristic root-cause key: paths, then numbers, are masked so one defect seen by many cases groups together."""
    text=re.sub(r'(?:/[^\s/:]+){5,}(?: \d+)?','<path>',str(message))  # filesystem paths only; API routes are shorter
    return re.sub(r'\d+','#',re.sub(r'\s+',' ',text)).strip()[:300]

def failure_clusters(rows):
    groups={}
    for r in rows:
        if r['status']!='pass':groups.setdefault(failure_signature(r.get('message','')),[]).append(r['id'])
    return [{'signature':k,'count':len(v),'cases':v}for k,v in sorted(groups.items(),key=lambda kv:(-len(kv[1]),kv[1][0]))]

def doctor():
    env=environment();checks={}
    checks['python_3_10_plus']=sys.version_info>=(3,10)
    match=re.search(r'javac\s+(\d+)',env['javac']);checks['jdk_17_plus']=bool(match and int(match.group(1))>=17)
    checks['posix_process_groups']=os.name=='posix'
    checks['rss_sampler']=rss_bytes(os.getpid())is not None
    import socket,tempfile
    try:
        with socket.socket()as s:s.bind(('127.0.0.1',0));checks['loopback_bind']=True
    except OSError:checks['loopback_bind']=False
    try:
        with tempfile.TemporaryFile()as f:f.write(b'test');f.flush();os.fsync(f.fileno());checks['local_file_fsync']=True
    except OSError:checks['local_file_fsync']=False
    return {'checks':checks,'ready':all(checks.values()),'environment':env,'note':'This is an environment probe, not a security sandbox or performance calibration.'}

def run_cases(workspace,classes,out,seed,can_start=True,build_ok=True):
    out=Path(out);out.mkdir(parents=True,exist_ok=True);rows=[]
    meta={'B01','B02','B03','B04','B10'}
    for cid,spec in CASES.items():
        dest=out/cid;dest.mkdir();result=dest/'case.json'
        base={k:v for k,v in spec.items()if k!='fn'}
        if not build_ok and cid not in {'B01','B02','B10'}:
            row={**base,'status':'not_run','message':'build prerequisite failed; counted as zero','seconds':0}
        elif not can_start and cid not in meta:
            row={**base,'status':'not_run','message':'startup prerequisite failed; counted as zero','seconds':0}
        else:
            cmd=[sys.executable,'-m','judge.worker','--workspace',str(workspace),'--classes',str(classes),'--artifacts',str(dest),'--case',cid,'--seed',str(seed),'--result',str(result)]
            run=run_limited(cmd,ROOT,45 if cid.startswith('O')else 25,minimal_env(out))
            (dest/'worker.log').write_text(run['output'],encoding='utf-8')
            if run['timeout']:row={**base,'status':'timeout','message':'case deadline exceeded; counted as zero','seconds':45 if cid.startswith('O')else 25}
            elif result.exists():
                try:row=json.loads(result.read_text())
                except Exception:row={**base,'status':'error','message':'invalid worker result; inspect worker.log','seconds':0}
            else:row={**base,'status':'error','message':f'worker exited {run["returncode"]} without result; inspect worker.log','seconds':0}
        write_json(result,row);rows.append(row)
        print(f'{cid} {row["status"]}: {row["title"]}',flush=True)
    return rows

def run_perf(classes,out,reps):
    out=Path(out);out.mkdir(parents=True,exist_ok=True)
    cmd=[sys.executable,'-m','judge.perf','--classes',str(classes),'--out',str(out),'--reps',str(reps)]
    r=run_limited(cmd,ROOT,max(180,reps*60),minimal_env(out));(out/'worker.log').write_text(r['output'],encoding='utf-8')
    f=out/'performance.json'
    if r['timeout']:return {'status':'candidate_failure','error':'performance deadline exceeded'}
    if f.exists():return json.loads(f.read_text())
    return {'status':'candidate_failure','error':'performance worker failed','worker_output':r['output'][-2000:]}

def calibrate(out,reps=3):
    out=fresh_out(out);print('Building trusted reference for local calibration.',flush=True)
    b=build(ROOT/'reference',out/'classes');write_json(out/'build.json',b)
    if b['returncode']!=0 or b['timeout']:raise RuntimeError('Reference build failed: '+b['output'])
    measured=run_perf(out/'classes',out/'measurements',reps)
    result={'benchmark_version':VERSION,'judge_sha256':trusted_hash(),'reference_sha256':digest_tree(ROOT/'reference/src'),
            'created_at_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'measurement':measured}
    write_json(out/'calibration.json',result)
    if measured.get('status')!='measured':raise RuntimeError('Reference calibration failed; inspect '+str(out))
    print('Calibration saved: '+str(out/'calibration.json'),flush=True);return result

def manual_review(path):
    if not path:return None
    r=json.loads(Path(path).read_text());scores={}
    if not isinstance(r.get('reviewer'),str)or not r['reviewer'].strip():raise ValueError('Manual review requires reviewer identity')
    for code in ('H01','H02'):
        entries=r.get(code,[])
        if len(entries)!=5:raise ValueError('Manual review needs exactly five entries for '+code)
        for entry in entries:
            if type(entry.get('score'))is not int or entry['score']not in (0,1,2):raise ValueError('Manual item score must be 0, 1, or 2')
            if not isinstance(entry.get('evidence'),str)or not entry['evidence'].strip():raise ValueError('Manual scores require evidence')
        scores[code]=sum(e['score']for e in entries)
    return {'scores_10':scores,'review':r}

def grade(a):
    out=fresh_out(a.out);workspace=Path(a.workspace).resolve()
    if workspace==ROOT/'candidate':print('WARNING: evaluating the unfinished starter, not a model solution.',flush=True)
    source_hash=snapshot(workspace,out/'submission');workspace=out/'submission'
    before=trusted_hash();env=environment();start=time.monotonic()
    metadata=json.loads(Path(a.metadata).read_text())if a.metadata else {}
    review=manual_review(a.review)
    report={'schema_version':'1.1','benchmark':'ReserveLedger','benchmark_version':VERSION,'run_id':a.run_id,'model_label':a.model_label,
            'timestamp_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'seed':a.seed,
            'submission_sha256':source_hash,'java_source_sha256':digest_tree(workspace/'src','.java'),'judge_sha256':before,
            'metadata':metadata,'environment':env,'manual_review':review}
    b=build(workspace,out/'classes');report['build']=b;write_json(out/'build.json',b)
    build_ok=b['returncode']==0 and not b['timeout'];health_ok=False;health_message='not attempted'
    if build_ok:
        s=Server(out/'classes',out/'preflight')
        try:s.start();health_ok=True;health_message='healthy'
        except Exception as e:health_message=str(e)
        finally:s.stop()
    report['preflight']={'build_ok':build_ok,'health_ok':health_ok,'message':health_message}
    rows=run_cases(workspace,out/'classes',out/'cases',a.seed,health_ok,build_ok);report['cases']=rows
    scores={d:case_score([r for r in rows if r['dimension']==d])for d in DIMENSIONS if d not in ('D09','D10')}
    infrastructure_errors=[];diagnostic=bool(a.skip_performance or a.skip_mutation or a.perf_reps<3)
    if not build_ok or not health_ok:
        report['performance']={'score_10':0,'status':'not_run','reason':'build/startup prerequisite failed'}
        report['test_quality']={'score_10':0,'status':'not_run','reason':'build/startup prerequisite failed'}
        scores['D09']=scores['D10']=0
    else:
        if a.skip_performance:
            report['performance']={'score_10':0,'status':'not_run','reason':'diagnostic skip'};scores['D09']=0
        else:
            try:
                if a.calibration:
                    cal=json.loads(Path(a.calibration).read_text())
                    if cal.get('benchmark_version')!=VERSION or cal.get('judge_sha256')!=before or cal.get('reference_sha256')!=digest_tree(ROOT/'reference/src'):
                        raise ValueError('Calibration belongs to a different benchmark/reference revision; recalibrate')
                else:cal=calibrate(out/'calibration',a.perf_reps)
                measured=run_perf(out/'classes',out/'performance',a.perf_reps);result=compare(measured,cal['measurement'])
                report['performance']={**result,'summary':measured.get('summary'),'measurement_file':'performance/performance.json',
                    'calibration_created_at_utc':cal.get('created_at_utc'),'reference_summary':cal['measurement'].get('summary')}
                scores['D09']=result['score_10']
                if result['status']in ('infrastructure_error','not_comparable'):infrastructure_errors.append(result.get('message',result['status']))
            except Exception as e:
                scores['D09']=0;report['performance']={'score_10':0,'status':'infrastructure_error','message':str(e)};infrastructure_errors.append('performance: '+str(e))
        if a.skip_mutation:
            report['test_quality']={'score_10':0,'status':'not_run','reason':'diagnostic skip'};scores['D10']=0
        else:
            try:
                rb=build(ROOT/'reference',out/'reference-classes');write_json(out/'reference-build.json',rb)
                if rb['returncode']!=0 or rb['timeout']:raise RuntimeError('reference build failed')
                dest=out/'test-quality';dest.mkdir()
                command=[sys.executable,'-m','judge.mutation','--workspace',str(workspace),'--classes',str(out/'reference-classes'),'--out',str(dest)]
                run=run_limited(command,ROOT,420,minimal_env(dest));(dest/'worker.log').write_text(run['output'],encoding='utf-8')
                if run['timeout']:result={'score_10':0,'status':'candidate_tests_timeout'}
                elif (dest/'mutation.json').exists():result=json.loads((dest/'mutation.json').read_text())
                else:raise RuntimeError('mutation worker failed; inspect test-quality/worker.log')
                report['test_quality']=result;scores['D10']=result['score_10']
            except Exception as e:
                report['test_quality']={'score_10':0,'status':'infrastructure_error','message':str(e)};scores['D10']=0;infrastructure_errors.append('mutation: '+str(e))
    critical=critical_failures(rows)
    report['summary']=summarize(scores,build_ok and health_ok,critical)
    report['failure_clusters']=failure_clusters(rows)
    integrity=trusted_hash()==before and digest_tree(workspace)==source_hash and frozen_ok(workspace)
    if not integrity:report['summary']['overall_10']=0;report['summary']['gate']='INTEGRITY_FAILURE'
    complete=not diagnostic and not infrastructure_errors
    report['validity']={'complete_profile':complete,'diagnostic':diagnostic,'integrity_ok':integrity,
        'infrastructure_errors':infrastructure_errors,'rankable':complete and integrity,
        'model_identity_is_operator_declared':True,'security_sandbox':False}
    report['summary']['official_overall_10']=report['summary']['overall_10']if report['validity']['rankable']else None
    if review:report['summary']['extended_overall_10']=round(.85*report['summary']['overall_10']+.15*sum(review['scores_10'].values())/2,4)
    else:report['summary']['extended_overall_10']=None
    report['evaluation_seconds']=time.monotonic()-start
    report['warnings']=[]
    if not metadata:report['warnings'].append('No model/agent generation metadata supplied. Label is unverified; generation efficiency is unknown.')
    if a.perf_reps<3:report['warnings'].append('Fewer than 3 performance repetitions: diagnostic only.')
    if report.get('performance',{}).get('possible_client_bottleneck'):report['warnings'].append('Load-generator CPU may be limiting throughput. Interpret speed scores cautiously.')
    emit(out,report);print('Report: '+str(out/'report.md'),flush=True);print(json.dumps(report['summary'],ensure_ascii=False),flush=True)
    return report

def emit(out,r):
    write_json(out/'report.json',r)
    def clean(s):return str(s).replace('|','\\|').replace('\n',' ')[:600]
    lines=[f'# ReserveLedger 评测报告：{r["model_label"]}', '',f'- Run ID：`{r["run_id"]}`',f'- 基准：{r["benchmark_version"]}；测试种子：{r["seed"]}',
        f'- 源码快照 SHA256：`{r["submission_sha256"]}`',f'- 完整且可计分：**{r["validity"]["rankable"]}**',
        f'- 自动原始综合：**{r["summary"]["raw_overall_10"]:.3f} / 10**',
        f'- 关键错误门槛后：**{r["summary"]["overall_10"]:.3f} / 10**',
        f'- 正式综合分：**{r["summary"]["official_overall_10"]} / 10**（null 表示诊断/基础设施无效，不是模型零分）',
        f'- Gate：`{r["summary"]["gate"]}`；关键失败：{", ".join(r["summary"]["critical_failures"])or "无"}',
        '', '| 维度 | 名称 | 分数 / 10 |', '|---|---|---:|']
    for code,title in DIMENSIONS.items():lines.append(f'| {code} | {title} | {r["summary"]["dimensions"][code]:.3f} |')
    lines+=['','## 性能原始数据','', '```json',json.dumps(r['performance'],ensure_ascii=False,indent=2),'```',
        '','## 自测有效性','', '```json',json.dumps(r['test_quality'],ensure_ascii=False,indent=2),'```',
        '','## 失败签名聚类（启发式根因分组）','',
        '同一签名的多个失败通常来自同一个缺陷；签名对路径和数字做了掩码，只用于阅读，不参与计分。','',
        '| 用例数 | 用例 | 签名 |','|---:|---|---|']
    lines+=[f'| {g["count"]} | {", ".join(g["cases"])} | {clean(g["signature"])} |'for g in r.get('failure_clusters',[])]or['| 0 | — | 无失败 |']
    lines+=['','## 所有验收结果','', '| Case | 维度 | 结果 | 秒 | 说明 |','|---|---|---|---:|---|']
    for row in r['cases']:lines.append(f'| {row["id"]} | {row["dimension"]} | {row["status"]}{" (S0)" if row.get("severity")=="S0" else ""} | {row.get("seconds",0):.3f} | {clean(row.get("message",""))} |')
    lines+=['','## 有效性与限制','', '```json',json.dumps(r['validity'],ensure_ascii=False,indent=2),'```']
    for warning in r['warnings']:lines.append('\n'+warning)
    lines+=['','模型生成用时、token 和成本只接受真实采集或操作者提供的记录；本报告的 evaluation_seconds 是裁判耗时，不是模型开发耗时。',
            '人工维护性/文档分独立存于 manual_review；未填写时不伪造人工分。一个题目的得分不代表模型通用编程能力，也不构成生产上线认证。','']
    (out/'report.md').write_text('\n'.join(lines),encoding='utf-8')
    with (out/'scorecard.csv').open('w',newline='',encoding='utf-8-sig')as f:
        w=csv.writer(f);w.writerow(['dimension','name','score','max'])
        for code,title in DIMENSIONS.items():w.writerow([code,title,r['summary']['dimensions'][code],10])
    with (out/'cases.csv').open('w',newline='',encoding='utf-8-sig')as f:
        w=csv.DictWriter(f,fieldnames=['id','dimension','title','critical','status','seconds','message'],extrasaction='ignore');w.writeheader();w.writerows(r['cases'])
    metric={'model_label':r['model_label'],'run_id':r['run_id'],'overall_10':r['summary']['official_overall_10'],**(r.get('performance',{}).get('summary')or {})}
    for key,value in r.get('metadata',{}).get('generation_metrics',{}).items():metric['generation_'+key]=value
    with (out/'metrics.csv').open('w',newline='',encoding='utf-8-sig')as f:w=csv.DictWriter(f,fieldnames=list(metric));w.writeheader();w.writerow(metric)
    suite=ET.Element('testsuite',name='ReserveLedger',tests=str(len(r['cases'])),failures=str(sum(row['status']!='pass'for row in r['cases'])))
    for row in r['cases']:
        test=ET.SubElement(suite,'testcase',classname=row['dimension'],name=row['id']+' '+row['title'],time=str(row.get('seconds',0)))
        if row['status']!='pass':ET.SubElement(test,'failure',type=row['status'],message=row.get('message','')).text=row.get('traceback',row.get('message',''))
    ET.ElementTree(suite).write(out/'junit.xml',encoding='utf-8',xml_declaration=True)

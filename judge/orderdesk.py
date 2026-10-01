"""Tracks t2-bugfix and t3-change on the shared brownfield OrderDesk codebase.

The judge never runs the candidate's own tests or build files. It builds a fresh project from the frozen pom,
the candidate's src/main, the judge's copy of the codebase's original tests and the hidden tests of the track,
runs `mvn -o test`, and scores per hidden test class (one class or group per scoring unit).
"""
from __future__ import annotations
import difflib,json,shutil
import xml.etree.ElementTree as ET
from pathlib import Path
from .runtime import ROOT,minimal_env,run_limited
from .spring import maven

TRACK_DIR=ROOT/'tracks/orderdesk'
BASE=TRACK_DIR/'base'
FROZEN_POM=BASE/'pom.xml'
VISIBLE_TESTS=BASE/'src/test/java'
BUGS=('B1','B2','B3','B4','B5','B6')
T2_UNITS={'B1':['HiddenB1AtomicOrderTest'],'B2':['HiddenB2PaymentRollbackTest'],'B3':['HiddenB3MoneyComparisonTest'],
          'B4':['HiddenB4ReportZoneTest','HiddenB4ReportConfiguredZoneTest'],'B5':['HiddenB5StockConcurrencyTest'],'B6':['HiddenB6OrderReadTest']}
VISIBLE_CLASSES=('ProductApiTest','OrderFlowTest','ReportApiTest')
PROTECTED=('pom.xml','src/main/resources/db/migration/V1__schema.sql','src/main/resources/db/migration/V2__payment_attempts.sql')
FOOTPRINT_FULL,FOOTPRINT_HALF=300,600   # changed src/main lines; the reference fixes change about 40
TEST_TIMEOUT=900

def compose(target,fixes=(),surfaces=(),extra=()):
    """Base codebase with overlay directories applied in order (used for references and validation variants)."""
    target=Path(target)
    if target.exists():shutil.rmtree(target)
    shutil.copytree(BASE,target,ignore=shutil.ignore_patterns('target','data','.DS_Store'))
    for d in [TRACK_DIR/'fixes'/b for b in fixes]+[TRACK_DIR/'surface'/s for s in surfaces]+[TRACK_DIR/e for e in extra]:
        if not d.is_dir():raise ValueError('missing overlay '+str(d))
        shutil.copytree(d,target,dirs_exist_ok=True)
        marker=target/'_delete.txt'   # an overlay may remove files: one relative path per line
        if marker.is_file():
            for rel in marker.read_text().split():(target/rel).unlink()
            marker.unlink()
    return target

def run_tests(workspace,out,hidden_dirs,env=None,include_visible=True):
    """Frozen pom + candidate src/main + judge copies of tests; returns per-class results from surefire."""
    out=Path(out);project=out/'project'
    if project.exists():shutil.rmtree(project)
    main=Path(workspace)/'src/main'
    if not (main/'java').is_dir():return {'build_ok':False,'output':'missing src/main/java','classes':{}}
    shutil.copytree(main,project/'src/main');shutil.copy(FROZEN_POM,project/'pom.xml')
    (project/'src/test/java').mkdir(parents=True)
    if include_visible:shutil.copytree(VISIBLE_TESTS,project/'src/test/java',dirs_exist_ok=True)
    for h in hidden_dirs:shutil.copytree(h,project/'src/test/java',dirs_exist_ok=True)
    args=['test','-Dsurefire.failIfNoSpecifiedTests=false','-DtrimStackTrace=true','-Dsurefire.useFile=true',
          '-DargLine=-Duser.timezone=UTC -Xmx1g','-Dmaven.test.failure.ignore=true']
    r=maven(args,project,TEST_TIMEOUT,env)
    (out/'maven.log').write_text(r['output'],encoding='utf-8')
    reports=project/'target/surefire-reports'
    classes={}
    for f in sorted(reports.glob('TEST-*.xml'))if reports.is_dir()else []:
        try:root=ET.parse(f).getroot()
        except ET.ParseError:continue
        name=root.get('name','').rsplit('.',1)[-1]
        cases=[]
        for tc in root.iter('testcase'):
            problem=tc.find('failure')if tc.find('failure')is not None else tc.find('error')
            cases.append({'name':tc.get('name'),'ok':problem is None and tc.find('skipped')is None,
                          'message':(problem.get('message')or '')[:300]if problem is not None else ''})
        classes[name]={'tests':len(cases),'passed':sum(c['ok']for c in cases),'cases':cases}
    build_ok=r['returncode']==0 and not r['timeout']and bool(classes)
    return {'build_ok':build_ok,'timeout':r['timeout'],'classes':classes}

def unit_pass(results,names):
    return all(n in results['classes']and results['classes'][n]['tests']>0 and
               results['classes'][n]['passed']==results['classes'][n]['tests']for n in names)

def changed_lines(workspace,reference=BASE):
    """Added + removed lines under src/main relative to the given codebase."""
    def files(root):
        r=Path(root)/'src/main'
        return {p.relative_to(r).as_posix():p for p in r.rglob('*')if p.is_file()}if r.is_dir()else {}
    a,b=files(reference),files(workspace);total=0
    for rel in sorted(set(a)|set(b)):
        old=a[rel].read_text(errors='replace').splitlines()if rel in a else []
        new=b[rel].read_text(errors='replace').splitlines()if rel in b else []
        if old!=new:total+=sum(1 for l in difflib.unified_diff(old,new,lineterm='',n=0)if l[:1]in '+-'and not l.startswith(('+++','---')))
    return total

def protected_ok(workspace,reference=BASE,paths=PROTECTED):
    return all((Path(workspace)/p).is_file()and (Path(workspace)/p).read_bytes()==(Path(reference)/p).read_bytes()for p in paths)

def score_t2(workspace,results):
    units={b:1.0 if results['build_ok']and unit_pass(results,T2_UNITS[b])else 0.0 for b in BUGS}
    visible=[results['classes'].get(c,{'tests':0,'passed':0})for c in VISIBLE_CLASSES]
    total=sum(v['tests']for v in visible);passed=sum(v['passed']for v in visible)
    units['regression']=round(2*passed/total,4)if results['build_ok']and total else 0.0
    units['discipline']=1.0 if protected_ok(workspace)else 0.0
    lines=changed_lines(workspace)
    units['footprint']=1.0 if lines<=FOOTPRINT_FULL else 0.5 if lines<=FOOTPRINT_HALF else 0.0
    if not results['build_ok']:units={k:0.0 for k in units}
    return {'units':units,'score_10':round(sum(units.values()),4),'changed_lines':lines,
            'regression_tests':{'passed':passed,'total':total}}

def evaluate_t2(workspace,out):
    out=Path(out);out.mkdir(parents=True,exist_ok=True)
    results=run_tests(workspace,out,[TRACK_DIR/'hidden/t2'])
    scored=score_t2(workspace,results)
    report={'track':'t2-bugfix','results':results,**scored}
    (out/'t2-result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    return report

# ---------------------------------------------------------------- t3-change
T3_UNITS={'startup':['HiddenT3StartupTest'],'backfill':['HiddenT3BackfillTest'],'v1_read_compat':['HiddenT3V1ReadCompatTest'],
          'v1_ship_compat':['HiddenT3V1ShipCompatTest'],'partial_shipments':['HiddenT3PartialShipmentTest'],
          'shipment_rules':['HiddenT3ShipmentValidationTest'],'v2_read':['HiddenT3V2ReadTest'],'concurrency':['HiddenT3ShipmentConcurrencyTest']}
T2_HIDDEN_CLASSES=[c for names in T2_UNITS.values()for c in names]

def t3_start(target):
    """The T3 starting point: OrderDesk with every T2 bug fixed (the released v1.1)."""
    return compose(target,BUGS)

def make_snapshot(out):
    """v1 'production' database plus manifest, produced by the judge-only seeder against the T3 starting point."""
    out=Path(out);snap=out/'snapshot';snap.mkdir(parents=True,exist_ok=True)
    start=t3_start(out/'v1-app')
    r=run_tests(start,out/'seed',[TRACK_DIR/'hidden/t3-seed'],env={'BENCH_SNAPSHOT_DIR':str(snap.resolve())},include_visible=False)
    if not (r['build_ok']and unit_pass(r,['SnapshotSeederTest'])and (snap/'manifest.json').is_file()and (snap/'orderdesk.mv.db').is_file()):
        raise RuntimeError('snapshot seeding failed; inspect '+str(out/'seed/maven.log'))
    return snap

def score_t3(workspace,results,start):
    units={u:1.0 if results['build_ok']and unit_pass(results,names)else 0.0 for u,names in T3_UNITS.items()}
    regression=list(VISIBLE_CLASSES)+T2_HIDDEN_CLASSES
    rows=[results['classes'].get(c,{'tests':0,'passed':0})for c in regression]
    total=sum(r['tests']for r in rows);passed=sum(r['passed']for r in rows)
    units['regression']=round(passed/total,4)if results['build_ok']and total else 0.0
    units['discipline']=1.0 if protected_ok(workspace,start)else 0.0
    if not results['build_ok']:units={k:0.0 for k in units}
    return {'units':units,'score_10':round(sum(units.values()),4),'regression_tests':{'passed':passed,'total':total},
            'changed_lines':changed_lines(workspace,start)}

def evaluate_t3(workspace,out,snapshot=None):
    out=Path(out);out.mkdir(parents=True,exist_ok=True)
    snap=Path(snapshot)if snapshot else make_snapshot(out/'snapshot-build')
    start=t3_start(out/'t3-start')
    results=run_tests(workspace,out,[TRACK_DIR/'hidden/t2',TRACK_DIR/'hidden/t3'],env={'BENCH_SNAPSHOT_DIR':str(Path(snap).resolve())})
    scored=score_t3(workspace,results,start)
    report={'track':'t3-change','results':results,**scored}
    (out/'t3-result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    return report

# ---------------------------------------------------------------- workspace, grading and self-validation
TRACKS={'t2-bugfix':'OrderDesk-Bugfix','t3-change':'OrderDesk-Change'}

def prepare(track,dest):
    """Candidate workspace: the codebase at the track's starting point plus that track's kit documents only."""
    dest=Path(dest)
    if dest.exists():raise ValueError('Destination already exists; refusing to overwrite')
    compose(dest,BUGS if track=='t3-change'else ())
    kit=TRACK_DIR/'kit'/('t3'if track=='t3-change'else 't2')
    for f in kit.iterdir():shutil.copy(f,dest/f.name)
    from .spring import m2_repo
    (dest/'.mvn').mkdir(exist_ok=True);(dest/'.mvn/maven.config').write_text('-o\n-Dmaven.repo.local='+str(m2_repo())+'\n')
    return dest

def grade(track,workspace,out,model_label,run_id,metadata=None):
    from .runner import fresh_out,snapshot,write_json,VERSION
    from .runtime import environment,digest_tree
    import datetime,time
    out=fresh_out(out);source_hash=snapshot(Path(workspace).resolve(),out/'submission');ws=out/'submission'
    def judge_hash():return digest_tree(ROOT/'judge','.py')+digest_tree(TRACK_DIR)
    before=judge_hash();start=time.monotonic()
    result=evaluate_t2(ws,out/'evaluation')if track=='t2-bugfix'else evaluate_t3(ws,out/'evaluation')
    integrity=digest_tree(ws)==source_hash and judge_hash()==before
    report={'schema_version':'1.1','benchmark':TRACKS[track],'track':track,'benchmark_version':VERSION,'run_id':run_id,'model_label':model_label,
            'timestamp_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'submission_sha256':source_hash,
            'metadata':metadata or {},'environment':environment(),'units':result['units'],'changed_lines':result['changed_lines'],
            'regression_tests':result['regression_tests'],'build_ok':result['results']['build_ok'],
            'summary':{'score_10':result['score_10']if integrity else 0,'official_overall_10':result['score_10']if integrity else None,
                       'integrity_ok':integrity},
            'tests':{c:v for c,v in result['results']['classes'].items()},'evaluation_seconds':time.monotonic()-start}
    write_json(out/'report.json',report)
    lines=[f'# {report["benchmark"]} 评测报告：{model_label}','',f'- Run ID：`{run_id}`；基准 {VERSION}（赛道 `{track}`）',
           f'- 源码快照 SHA256：`{source_hash}`',f'- 构建：{"成功" if report["build_ok"] else "失败"}；完整性：{integrity}',
           f'- **综合：{report["summary"]["score_10"]:.3f} / 10**；src/main 改动 {report["changed_lines"]} 行','',
           '| 单元 | 得分 |','|---|---:|']+[f'| {k} | {v:g} |'for k,v in report['units'].items()]
    lines+=['','## 失败的测试','']
    for c,v in report['tests'].items():
        for case in v['cases']:
            if not case['ok']:lines.append(f'- `{c}.{case["name"]}`：{case["message"].replace(chr(10)," ")[:200]}')
    lines+=['','隐藏测试名称与失败信息只供操作者分析，不要发给待测模型。']
    (out/'report.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    return report

def validate(track,out,repeat=1):
    """Variant matrix: starting point, full reference, each fix withheld, each symptom-only / naive variant."""
    out=Path(out);rows=[]
    def run(name,expected_failed,**overlays):
        for i in range(repeat):
            ws=compose(out/name/f'ws{i}',**overlays)
            r=evaluate_t2(ws,out/name/f'out{i}')if track=='t2-bugfix'else evaluate_t3(ws,out/name/f'out{i}',snap)
            units=T2_UNITS if track=='t2-bugfix'else T3_UNITS
            failed=sorted(u for u in units if r['units'][u]<1)
            ok=(failed==sorted(expected_failed))if expected_failed is not None else failed==sorted(units)
            rows.append({'variant':name,'repeat':i,'score_10':r['score_10'],'failed_units':failed,'ok':ok})
    if track=='t2-bugfix':
        run('base',None)
        run('reference',[],fixes=BUGS)
        for k in BUGS:run('without-'+k,[k],fixes=[b for b in BUGS if b!=k])
        for s in sorted(p.name for p in (TRACK_DIR/'surface').iterdir()):
            k=s.split('-')[0];run('surface-'+s,[k],fixes=[b for b in BUGS if b!=k],surfaces=[s])
    else:
        snap=make_snapshot(out/'snapshot')
        run('start',[u for u in T3_UNITS if u!='v1_read_compat'],fixes=BUGS)
        run('reference',[],fixes=BUGS,extra=['t3/reference'])
        expect={'backfill-by-status':['backfill'],'v1-status-leak':['v1_ship_compat'],'no-lock':['concurrency'],
                'edit-v1':[u for u in T3_UNITS]}
        for n,failed in expect.items():run('naive-'+n,failed,fixes=BUGS,extra=['t3/reference','t3/naive/'+n])
    return {'track':track,'variants':rows,'passed':all(r['ok']for r in rows)}

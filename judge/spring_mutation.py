"""Track t1-spring D10: the candidate's black-box JUnit contract tests (src/test/java/contract) are
compiled against JUnit + Jackson only and run with the JUnit console launcher against the correct
reference and ten controlled faults. Scoring and baseline exclusion follow judge.mutation."""
from __future__ import annotations
import argparse,json,os,re,shutil,sys
import xml.etree.ElementTree as ET
from pathlib import Path
from .mutation import MUTANTS,MAX_EXCLUDED_FRACTION
from .runtime import Server,minimal_env,run_limited,use_track
from .spring import TRACK_DIR,maven

RUN_TIMEOUT=90

def tool_classpath(work):
    """JUnit console launcher + Jackson, resolved from the offline repository via the judge-tools pom."""
    project=Path(work)/'tools';project.mkdir(parents=True,exist_ok=True);shutil.copy(TRACK_DIR/'judge-tools/pom.xml',project/'pom.xml')
    r=maven(['dependency:build-classpath','-Dmdep.outputFile=cp.txt'],project,180)
    if r['returncode']!=0 or r['timeout']:raise RuntimeError('contract tool classpath unavailable: '+r['output'][-2000:])
    entries=(project/'cp.txt').read_text().strip().split(os.pathsep)
    console=[e for e in entries if 'junit-platform-console-standalone'in e]
    if len(console)!=1:raise RuntimeError('junit console launcher missing from offline repository')
    return console[0],[e for e in entries if e!=console[0]]

def compile_tests(workspace,out,console,libs):
    sources=sorted((Path(workspace).resolve()/'src/test/java/contract').rglob('*.java'))
    if not sources:return None,'no contract tests under src/test/java/contract'
    classes=out/'contract-classes';classes.mkdir()
    cmd=['javac','-encoding','UTF-8','--release','21','-proc:none','-d',str(classes),'-cp',os.pathsep.join([console]+libs)]+[str(p)for p in sources]
    r=run_limited(cmd,str(out),120,minimal_env(out));(out/'contract-compile.log').write_text(r['output'],encoding='utf-8')
    if r['returncode']!=0 or r['timeout']:return None,'contract tests do not compile against JUnit + Jackson only'
    return classes,None

def report(directory):
    """(observed, failed) test ids 'class#method' from the launcher's legacy XML reports."""
    observed,failed=[],[]
    for f in sorted(Path(directory).glob('TEST-*.xml')):
        try:root=ET.parse(f).getroot()
        except ET.ParseError:continue
        for tc in root.iter('testcase'):
            tid=tc.get('classname','')+'#'+re.sub(r'\(.*\)$','',tc.get('name',''))
            observed.append(tid)
            if tc.find('failure')is not None or tc.find('error')is not None:failed.append(tid)
    return observed,failed

def assess(workspace,classes,out):
    use_track('t1-spring');out=Path(out);out.mkdir(parents=True,exist_ok=True)
    console,libs=tool_classpath(out)
    tests,problem=compile_tests(workspace,out,console,libs)
    if tests is None:return {'score_10':0,'status':'invalid_tests'if 'compile'in problem else 'missing_tests','reason':problem,'baseline_passed':False,'mutants':[]}
    def run(label,mutant=None,exclude=()):
        server=Server(classes,out/('server-'+label),extra={'BENCH_MUTANT':mutant}if mutant else {})
        reports=out/('reports-'+label)
        with server:
            env=minimal_env(out,{'BENCH_TEST_TOKEN':server.token,'BENCH_BASE_URL':f'http://127.0.0.1:{server.port}'})
            cmd=['java','-jar',console,'execute','--disable-banner','--details=tree','--fail-if-no-tests','--class-path',os.pathsep.join([str(tests)]+libs),
                 '--scan-class-path',str(tests),'--include-classname','.*','--reports-dir',str(reports)]
            for t in exclude:cmd+=['--exclude-methodname','^'+re.escape(t)+r'(\(.*\))?$']
            r=run_limited(cmd,str(out),RUN_TIMEOUT,env)
            (out/(label+'.log')).write_text(r['output'],encoding='utf-8')
            observed,failed=report(reports)
            return {'id':label,'returncode':r['returncode'],'timeout':r['timeout'],'log':label+'.log','observed':observed,'failed':failed}
    def ok(r):return r['returncode']==0 and not r['timeout']
    def invalid(reason,**extra):return {'score_10':0,'status':'invalid_tests','reason':reason,'baseline_passed':False,'mutants':[],**extra}
    baseline=[run('correct-1'),run('correct-2')]
    observed=set().union(*(r['observed']for r in baseline))
    excluded=[];confirm=[]
    if not all(ok(r)for r in baseline):
        if any(r['timeout']for r in baseline):return invalid('baseline timeout',baseline=baseline)
        failed=set()
        for r in baseline:
            if not ok(r):
                if not r['failed']:return invalid('baseline failed without attributable test methods',baseline=baseline)
                failed|=set(r['failed'])
        if len(failed)>MAX_EXCLUDED_FRACTION*len(observed):return invalid('more than half of the tests fail on the correct baseline',baseline=baseline,failed=sorted(failed))
        excluded=sorted(failed)
        confirm=[run('correct-1-excluded',exclude=excluded),run('correct-2-excluded',exclude=excluded)]
        if not all(ok(r)for r in confirm):return invalid('baseline still fails after excluding failing tests',baseline=baseline+confirm,excluded_tests=excluded)
    kept_ratio=(len(observed)-len(excluded))/len(observed)if excluded else 1.0
    rows=[]
    for key,desc in MUTANTS.items():
        r=run(key,key,excluded);r.update(desc)
        r['killed']=r['returncode']not in (None,0)and not r['timeout'];rows.append(r)
    kills=sum(r['killed']for r in rows)
    return {'score_10':round(kills*kept_ratio,4),'status':'measured','baseline_passed':not excluded,'baseline':baseline+confirm,
            'tests_observed':len(observed),'excluded_tests':excluded,'kept_ratio':round(kept_ratio,4),'killed':kills,
            'formula':'killed × (tests_observed − excluded) / tests_observed','mutants':rows,'timeout_policy':'no_kill_credit'}

def validate_mutants(classes,out):
    """Each controlled fault must start healthy and be caught by its formal killer case."""
    from .cases_spring import CASES
    from .runtime import Context
    use_track('t1-spring');out=Path(out);out.mkdir(parents=True,exist_ok=True);rows=[]
    for key,desc in MUTANTS.items():
        ctx=Context(TRACK_DIR/'reference',classes,out/key,1701);original=ctx.server
        ctx.server=lambda original=original,key=key,**kw:original(**{**kw,'extra':{'BENCH_MUTANT':key}})
        try:CASES[desc['killer']]['fn'](ctx);row={'id':key,'killed':False,'killer':desc['killer']}
        except Exception as e:row={'id':key,'killed':True,'killer':desc['killer'],'message':str(e)[:300]}
        finally:ctx.close()
        rows.append(row)
    return rows

def main():
    p=argparse.ArgumentParser();p.add_argument('--workspace',required=True);p.add_argument('--classes',required=True);p.add_argument('--out',required=True);a=p.parse_args()
    result=assess(a.workspace,a.classes,a.out);Path(a.out,'mutation.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps({k:v for k,v in result.items()if k not in ('baseline','mutants')},ensure_ascii=False))
if __name__=='__main__':main()

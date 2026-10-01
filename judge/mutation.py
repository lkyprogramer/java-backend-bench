"""Candidate-authored tests run against a correct server and ten controlled API faults."""
from __future__ import annotations
import argparse,json,shutil,sys,time
from pathlib import Path
from .runtime import ROOT,Server,minimal_env,run_limited,build

MUTANTS={
    'M01':{'title':'允许超卖','killer':'C01'},
    'M02':{'title':'相同幂等键重复创建','killer':'I01'},
    'M03':{'title':'同键异参被接受','killer':'I03'},
    'M04':{'title':'到期比较使用 > 而非 >=','killer':'T01'},
    'M05':{'title':'重复取消多次释放','killer':'C04'},
    'M06':{'title':'确认错误归还可用库存','killer':'F03'},
    'M07':{'title':'已取消记录允许确认','killer':'T04'},
    'M08':{'title':'多商品失败发生部分扣减','killer':'F06'},
    'M09':{'title':'错误接受零预占数量','killer':'V02'},
    'M10':{'title':'忽略列表状态过滤','killer':'F09'},
}

# Judge-owned wrapper. The candidate file runs unchanged under -I; for unittest-based suites the wrapper
# records started/failed test ids and skips ids that failed on the correct baseline. Non-unittest suites
# cannot be split, so a failing baseline stays all-or-nothing for them.
LAUNCHER=r'''import json,runpy,sys,unittest
record,exclude,entry=sys.argv[1],sys.argv[2],sys.argv[3]
excluded=set(json.load(open(exclude,encoding='utf-8')))if exclude!='-'else set()
seen={'started':[],'failed':[]}
def save():
    with open(record,'w',encoding='utf-8')as f:json.dump(seen,f)
_start=unittest.TestResult.startTest
def startTest(self,test):
    seen['started'].append(test.id());save();return _start(self,test)
unittest.TestResult.startTest=startTest
def hook(name):
    original=getattr(unittest.TestResult,name)
    def wrapper(self,test,*args):
        if name!='addSubTest'or args[-1]is not None:seen['failed'].append(test.id());save()
        return original(self,test,*args)
    setattr(unittest.TestResult,name,wrapper)
for name in('addError','addFailure','addSubTest','addUnexpectedSuccess'):hook(name)
_run=unittest.TestCase.run
def run(self,result=None):
    if result is not None and self.id()in excluded:
        result.startTest(self);result.addSkip(self,'excluded by judge: failed on the correct baseline');result.stopTest(self);return result
    return _run(self,result)
unittest.TestCase.run=run
save();sys.argv=[entry]+sys.argv[4:]
runpy.run_path(entry,run_name='__main__')
'''
MAX_EXCLUDED_FRACTION=.5

def assess(workspace,classes,out):
    out=Path(out);out.mkdir(parents=True,exist_ok=True)
    source=Path(workspace)/'tests/candidate_tests.py'
    if not source.is_file():return {'score_10':0,'status':'missing_tests','baseline_passed':False,'mutants':[]}
    private=out/'isolated-tests';private.mkdir();entry=private/'candidate_tests.py';shutil.copyfile(source,entry)
    launcher=private/'_bench_launcher.py';launcher.write_text(LAUNCHER,encoding='utf-8')
    exclude_file=out/'excluded-tests.json'
    rows=[]
    def run(label,mutant=None,exclude=False):
        server=Server(classes,out/('server-'+label),extra={'BENCH_MUTANT':mutant}if mutant else {})
        record=out/(label+'.tests.json')
        with server:
            env=minimal_env(private,{'BENCH_TEST_TOKEN':server.token,'BASE_URL':f'http://127.0.0.1:{server.port}'})
            command=[sys.executable,'-I',str(launcher),str(record),str(exclude_file)if exclude else '-',str(entry),'--base-url',env['BASE_URL']]
            result=run_limited(command,private,20,env)
            (out/(label+'.log')).write_text(result['output'],encoding='utf-8')
            try:tests=json.loads(record.read_text(encoding='utf-8'))
            except (OSError,ValueError):tests=None
            return {'id':label,'returncode':result['returncode'],'timeout':result['timeout'],'log':label+'.log','tests':tests}
    def ok(r):return r['returncode']==0 and not r['timeout']
    def invalid(reason,**extra):return {'score_10':0,'status':'invalid_tests','reason':reason,'baseline_passed':False,'mutants':[],**extra}
    baseline=[run('correct-1'),run('correct-2')]
    started=set().union(*(r['tests']['started']for r in baseline if r['tests']))
    excluded=[];confirm=[]
    if not all(ok(r)for r in baseline):
        if any(r['timeout']for r in baseline):return invalid('baseline timeout',baseline=baseline)
        failed=set()
        for r in baseline:
            if not ok(r):
                if not r['tests']or not r['tests']['failed']:return invalid('baseline failed without attributable unittest test ids',baseline=baseline)
                failed|=set(r['tests']['failed'])
        if failed-started:return invalid('baseline failure outside a test method (module/class fixture)',baseline=baseline,failed=sorted(failed))
        if len(failed)>MAX_EXCLUDED_FRACTION*len(started):return invalid('more than half of the tests fail on the correct baseline',baseline=baseline,failed=sorted(failed))
        excluded=sorted(failed);exclude_file.write_text(json.dumps(excluded),encoding='utf-8')
        confirm=[run('correct-1-excluded',exclude=True),run('correct-2-excluded',exclude=True)]
        if not all(ok(r)for r in confirm):return invalid('baseline still fails after excluding failing tests',baseline=baseline+confirm,excluded_tests=excluded)
    kept_ratio=(len(started)-len(excluded))/len(started)if excluded else 1.0
    for key,desc in MUTANTS.items():
        r=run(key,key,exclude=bool(excluded));r.update(desc)
        # A timeout can be a hung test rather than a detected fault. Conservatively no kill credit.
        r['killed']=r['returncode'] not in (None,0) and not r['timeout'];rows.append(r)
    kills=sum(r['killed']for r in rows)
    return {'score_10':round(kills*kept_ratio,4),'status':'measured','baseline_passed':not excluded,'baseline':baseline+confirm,
            'tests_observed':len(started),'excluded_tests':excluded,'kept_ratio':round(kept_ratio,4),'killed':kills,
            'formula':'killed × (tests_observed − excluded) / tests_observed','mutants':rows,'timeout_policy':'no_kill_credit'}

def validate_mutants(classes,out):
    from .cases import CASES
    from .runtime import Context
    out=Path(out);out.mkdir(parents=True,exist_ok=True);rows=[]
    for key,desc in MUTANTS.items():
        ctx=Context(ROOT/'reference',classes,out/key,1701)
        original=ctx.server
        ctx.server=lambda **kw:original(**{**kw,'extra':{'BENCH_MUTANT':key}})
        try:CASES[desc['killer']]['fn'](ctx);row={'id':key,'killed':False,'killer':desc['killer']}
        except Exception as e:row={'id':key,'killed':True,'killer':desc['killer'],'message':str(e)}
        finally:ctx.close()
        rows.append(row)
    return rows

def main():
    p=argparse.ArgumentParser();p.add_argument('--workspace',required=True);p.add_argument('--classes',required=True);p.add_argument('--out',required=True);a=p.parse_args()
    result=assess(a.workspace,a.classes,a.out);Path(a.out,'mutation.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':main()

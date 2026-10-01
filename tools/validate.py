#!/usr/bin/env python3
"""Maintainer self-validation. Reference outcomes are NOT results from evaluated language models."""
import argparse,json,shutil,sys
from pathlib import Path
from types import SimpleNamespace
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT))
from judge.runner import fresh_out,grade,write_json,doctor
from judge.runtime import minimal_env,run_limited
from judge.tracks import track as get_track

BROKEN_PY="class BrokenOnCorrect(unittest.TestCase):\n    def test_wrong_expectation(self):self.fail('deliberately wrong')\n\n"
BROKEN_JAVA='''package contract;

class BrokenOnCorrectTest {
    @org.junit.jupiter.api.Test
    void wrongExpectation() {
        org.junit.jupiter.api.Assertions.fail("deliberately wrong");
    }
}
'''
ALWAYS_FAILING_JAVA=BROKEN_JAVA.replace('BrokenOnCorrectTest','AlwaysFailingTest')
# A "competent but mediocre" solution must land in this band; it checks that the track discriminates.
MEDIOCRE_BAND=(5.0,7.5)

def grade_args(out,workspace,label,run_id,track_name,calibration=None):
    return SimpleNamespace(out=str(out),workspace=str(workspace),model_label=label,run_id=run_id,seed=1701,metadata=None,review=None,
                           calibration=calibration,perf_reps=3,skip_performance=False,skip_mutation=False,track=track_name)

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--out',required=True);p.add_argument('--allow-exec',action='store_true')
    p.add_argument('--track',choices=('base','t1-spring','t2-bugfix','t3-change'),default='base')
    p.add_argument('--repeat',type=int,default=1,help='t2/t3: run every variant this many times (flakiness check)');a=p.parse_args()
    if not a.allow_exec:p.error('--allow-exec required; use a disposable non-sensitive environment')
    if a.track in ('t2-bugfix','t3-change'):return brownfield(a)
    t=get_track(a.track);spring=a.track=='t1-spring'
    if spring:from judge.spring_mutation import assess,validate_mutants
    else:from judge.mutation import assess,validate_mutants
    out=fresh_out(a.out);checks={};env=doctor(a.track);write_json(out/'environment.json',env)
    if not env['ready']:raise RuntimeError('Unsupported environment; see environment.json')
    unit=run_limited([sys.executable,'-m','unittest','discover','-s','tests','-v'],ROOT,120,minimal_env(out))
    (out/'unit-tests.log').write_text(unit['output'],encoding='utf-8');checks['unit_tests']=unit['returncode']==0 and not unit['timeout']
    if not checks['unit_tests']:raise RuntimeError('Judge unit tests failed; see unit-tests.log')
    reference=grade(grade_args(out/'reference-grade',t.root/'reference','REFERENCE-SELF-CHECK-NOT-A-MODEL','reference-validation',a.track))
    checks['reference_80_cases']=len(reference['cases'])==80 and all(row['status']=='pass'for row in reference['cases'])
    checks['reference_complete']=reference['validity']['rankable']
    checks['reference_test_quality']=reference['summary']['dimensions']['D10']==10
    checks['reference_performance']=reference['performance']['status']=='measured'
    classes=out/'reference-grade/reference-classes'
    mutants=validate_mutants(classes,out/'mutant-validation');write_json(out/'mutants.json',mutants)
    checks['mutants_killed']=len(mutants)==10 and all(row['killed']for row in mutants)
    health=assess(t.root/'candidate',classes,out/'health-only-tests');write_json(out/'health-only-tests.json',health)
    checks['health_only_zero']=health['baseline_passed']and health['score_10']==0
    fail=out/'always-failing-tests'
    if spring:
        (fail/'src/test/java/contract').mkdir(parents=True);(fail/'src/test/java/contract/AlwaysFailingTest.java').write_text(ALWAYS_FAILING_JAVA)
    else:
        (fail/'tests').mkdir(parents=True);(fail/'tests/candidate_tests.py').write_text('raise SystemExit(1)\n')
    invalid=assess(fail,classes,out/'invalid-tests');write_json(out/'invalid-tests.json',invalid)
    checks['always_failing_zero']=invalid['status']=='invalid_tests'and invalid['score_10']==0
    broken=out/'one-broken-test'
    if spring:
        shutil.copytree(t.root/'reference/src/test/java/contract',broken/'src/test/java/contract')
        (broken/'src/test/java/contract/BrokenOnCorrectTest.java').write_text(BROKEN_JAVA);expected=['contract.BrokenOnCorrectTest#wrongExpectation']
    else:
        (broken/'tests').mkdir(parents=True);marker="if __name__=='__main__':"
        text=(ROOT/'reference/tests/candidate_tests.py').read_text(encoding='utf-8')
        (broken/'tests/candidate_tests.py').write_text(text.replace(marker,BROKEN_PY+marker),encoding='utf-8');expected=['__main__.BrokenOnCorrect.test_wrong_expectation']
    partial=assess(broken,classes,out/'one-broken-tests');write_json(out/'one-broken-tests.json',partial)
    n=partial.get('tests_observed',0)
    checks['one_broken_test_partial_credit']=partial['status']=='measured'and partial['excluded_tests']==expected and n>1 and abs(partial['score_10']-10*(n-1)/n)<1e-3
    if spring:
        cal=out/'reference-grade/calibration/calibration.json'
        mediocre=grade(grade_args(out/'mediocre-grade',t.root/'mediocre','MEDIOCRE-CONTROL-NOT-A-MODEL','mediocre-validation',a.track,str(cal)))
        score=mediocre['summary']['official_overall_10']
        checks['mediocre_in_band']=score is not None and MEDIOCRE_BAND[0]<=score<=MEDIOCRE_BAND[1]
        write_json(out/'mediocre-summary.json',{'band':MEDIOCRE_BAND,'summary':mediocre['summary']})
    result={'track':a.track,'checks':checks,'passed':all(checks.values()),'note':'Tests the benchmark harness and reference only. No Qwen or other model generation performed.'}
    write_json(out/'validation-summary.json',result);print(json.dumps(result,ensure_ascii=False,indent=2));return 0 if result['passed']else 1
def brownfield(a):
    from judge import orderdesk
    out=fresh_out(a.out);env=doctor(a.track);write_json(out/'environment.json',env)
    if not env['ready']:raise RuntimeError('Unsupported environment; see environment.json')
    unit=run_limited([sys.executable,'-m','unittest','discover','-s','tests','-v'],ROOT,120,minimal_env(out))
    (out/'unit-tests.log').write_text(unit['output'],encoding='utf-8')
    matrix=orderdesk.validate(a.track,out/'variants',a.repeat);write_json(out/'variants.json',matrix)
    checks={'unit_tests':unit['returncode']==0 and not unit['timeout'],'variant_matrix':matrix['passed']}
    result={'track':a.track,'checks':checks,'passed':all(checks.values()),'variants':matrix['variants'],
            'note':'Tests the benchmark harness, reference fixes and controlled variants only. No model generation performed.'}
    write_json(out/'validation-summary.json',result);print(json.dumps(result,ensure_ascii=False,indent=2));return 0 if result['passed']else 1

if __name__=='__main__':
    try:raise SystemExit(main())
    except (OSError,ValueError,RuntimeError)as e:print('ERROR: '+str(e),file=sys.stderr);raise SystemExit(2)

#!/usr/bin/env python3
"""Maintainer self-validation. Reference outcomes are NOT results from evaluated language models."""
import argparse,json,shutil,sys
from pathlib import Path
from types import SimpleNamespace
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT))
from judge.runner import fresh_out,grade,write_json,doctor
from judge.runtime import build,minimal_env,run_limited
from judge.mutation import assess,validate_mutants

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--out',required=True);p.add_argument('--allow-exec',action='store_true');a=p.parse_args()
    if not a.allow_exec:p.error('--allow-exec required; use a disposable non-sensitive environment')
    out=fresh_out(a.out);checks={};env=doctor();write_json(out/'environment.json',env)
    if not env['ready']:raise RuntimeError('Unsupported environment; see environment.json')
    unit=run_limited([sys.executable,'-m','unittest','discover','-s','tests','-v'],ROOT,60,minimal_env(out))
    (out/'unit-tests.log').write_text(unit['output'],encoding='utf-8');checks['unit_tests']=unit['returncode']==0 and not unit['timeout']
    if not checks['unit_tests']:raise RuntimeError('Judge unit tests failed; see unit-tests.log')
    args=SimpleNamespace(out=str(out/'reference-grade'),workspace=str(ROOT/'reference'),model_label='REFERENCE-SELF-CHECK-NOT-A-MODEL',run_id='reference-validation',seed=1701,metadata=None,review=None,calibration=None,perf_reps=3,skip_performance=False,skip_mutation=False)
    reference=grade(args)
    checks['reference_80_cases']=len(reference['cases'])==80 and all(row['status']=='pass'for row in reference['cases'])
    checks['reference_complete']=reference['validity']['rankable']
    checks['reference_test_quality']=reference['summary']['dimensions']['D10']==10
    checks['reference_performance']=reference['performance']['status']=='measured'
    classes=out/'reference-grade/reference-classes'
    mutants=validate_mutants(classes,out/'mutant-validation');write_json(out/'mutants.json',mutants)
    checks['mutants_killed']=len(mutants)==10 and all(row['killed']for row in mutants)
    health=assess(ROOT/'candidate',classes,out/'health-only-tests');write_json(out/'health-only-tests.json',health)
    checks['health_only_zero']=health['baseline_passed']and health['score_10']==0
    fail=out/'always-failing-tests';(fail/'tests').mkdir(parents=True);(fail/'tests/candidate_tests.py').write_text('raise SystemExit(1)\n')
    invalid=assess(fail,classes,out/'invalid-tests');write_json(out/'invalid-tests.json',invalid)
    checks['always_failing_zero']=invalid['status']=='invalid_tests'and invalid['score_10']==0
    broken=out/'one-broken-test';(broken/'tests').mkdir(parents=True)
    text=(ROOT/'reference/tests/candidate_tests.py').read_text(encoding='utf-8');marker="if __name__=='__main__':"
    (broken/'tests/candidate_tests.py').write_text(text.replace(marker,"class BrokenOnCorrect(unittest.TestCase):\n    def test_wrong_expectation(self):self.fail('deliberately wrong')\n\n"+marker),encoding='utf-8')
    partial=assess(broken,classes,out/'one-broken-tests');write_json(out/'one-broken-tests.json',partial)
    n=partial.get('tests_observed',0)
    checks['one_broken_test_partial_credit']=partial['status']=='measured'and partial['excluded_tests']==['__main__.BrokenOnCorrect.test_wrong_expectation']and n>1 and abs(partial['score_10']-10*(n-1)/n)<1e-3
    result={'checks':checks,'passed':all(checks.values()),'note':'Tests the benchmark harness and reference only. No Qwen or other model generation performed.'}
    write_json(out/'validation-summary.json',result);print(json.dumps(result,ensure_ascii=False,indent=2));return 0 if result['passed']else 1
if __name__=='__main__':
    try:raise SystemExit(main())
    except (OSError,ValueError,RuntimeError)as e:print('ERROR: '+str(e),file=sys.stderr);raise SystemExit(2)

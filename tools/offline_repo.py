#!/usr/bin/env python3
"""Build and check the offline Maven repository used by track t1-spring.

`fetch` is the only networked step and is run once by the operator/maintainer. Everything else
(candidate development, judge build, contract tests) runs with `mvn -o` against this repository.
Keep the repository OUTSIDE the benchmark tree: the candidate workspace is pointed at it, and its
path must not lead a model to the hidden judge or reference solution.
"""
from __future__ import annotations
import argparse,json,os,shutil,subprocess,sys,tempfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
TRACK=ROOT/'tracks/t1-spring'
DEFAULT_REPO=Path(os.environ.get('BENCH_T1_M2',Path.home()/'.cache/java-backend-bench/t1-spring-m2'))
SETTINGS='<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"><offline>{offline}</offline></settings>\n'
NOOP_TEST='package probe;\nclass NoopTest { @org.junit.jupiter.api.Test void ok() {} }\n'

def mvn(args,cwd,repo,offline,timeout=1800):
    settings=Path(cwd)/'.bench-settings.xml';settings.write_text(SETTINGS.format(offline=str(offline).lower()))
    cmd=['mvn','-B','-q','-s',str(settings),'-Dmaven.repo.local='+str(repo)]+(['-o']if offline else [])+args
    r=subprocess.run(cmd,cwd=cwd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=timeout)
    return r.returncode,r.stdout

def probe_project(base):
    """Starter kit plus a trivial unit test, so surefire's JUnit provider is resolved too."""
    project=Path(base)/'probe';shutil.copytree(TRACK/'candidate',project,ignore=shutil.ignore_patterns('target','.mvn'))
    shutil.rmtree(project/'src/test',ignore_errors=True)
    (project/'src/test/java/probe').mkdir(parents=True);(project/'src/test/java/probe/NoopTest.java').write_text(NOOP_TEST)
    return project

def run_steps(repo,offline):
    steps=[]
    with tempfile.TemporaryDirectory()as t:
        project=probe_project(t)
        for args in (['clean','package','dependency:build-classpath','-Dmdep.outputFile=cp.txt'],):
            code,out=mvn(args,project,repo,offline);steps.append({'project':'app','args':args,'returncode':code,'output_tail':out[-3000:]})
        tools=Path(t)/'tools';tools.mkdir();shutil.copy(TRACK/'judge-tools/pom.xml',tools/'pom.xml')
        code,out=mvn(['dependency:build-classpath','-Dmdep.outputFile=cp.txt'],tools,repo,offline);steps.append({'project':'judge-tools','returncode':code,'output_tail':out[-3000:]})
    return steps

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('command',choices=['fetch','check']);p.add_argument('--repo',default=str(DEFAULT_REPO));a=p.parse_args()
    repo=Path(a.repo).expanduser().resolve()
    if ROOT==repo or ROOT in repo.parents:p.error('keep the offline repository outside the benchmark tree')
    if shutil.which('mvn')is None:p.error('Maven 3.9+ is required on PATH')
    repo.mkdir(parents=True,exist_ok=True)
    steps=run_steps(repo,offline=(a.command=='check'))
    ok=all(s['returncode']==0 for s in steps)
    if a.command=='fetch'and ok:
        steps+=run_steps(repo,offline=True);ok=all(s['returncode']==0 for s in steps)
    size=sum(f.stat().st_size for f in repo.rglob('*')if f.is_file())
    print(json.dumps({'command':a.command,'repo':str(repo),'ok':ok,'size_mib':round(size/2**20,1),'steps':[{k:v for k,v in s.items()if k!='output_tail' or s['returncode']}for s in steps]},ensure_ascii=False,indent=2))
    return 0 if ok else 2

if __name__=='__main__':sys.exit(main())

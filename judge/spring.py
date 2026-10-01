"""Track t1-spring: offline Maven build with the frozen pom, judge overlay, and launch command."""
from __future__ import annotations
import json,os,shutil,urllib.request
from pathlib import Path
from .runtime import ROOT,digest_tree,free_port,minimal_env,run_limited

TRACK_DIR=ROOT/'tracks/t1-spring'
FROZEN_POM=TRACK_DIR/'candidate/pom.xml'
OVERLAY=TRACK_DIR/'overlay/src/main'
JAVA_FLAGS=['-Xms64m','-Xmx512m','-XX:ActiveProcessorCount=2']
START_TIMEOUT=90
# Judge-owned datasource: data stays in the data directory. WRITE_DELAY=0 writes each commit to the file
# before the commit returns (H2 otherwise buffers ~0.5s, so an acknowledged write could vanish on kill -9);
# the lock wait is raised so contention is decided by the candidate's locking design, not H2's 1s default.
H2_OPTIONS=';WRITE_DELAY=0;LOCK_TIMEOUT=10000'

def m2_repo():
    from .runtime import t1_repo
    return t1_repo()

def maven(args,cwd,timeout=300,env=None):
    settings=Path(cwd)/'.bench-settings.xml'
    settings.write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"><offline>true</offline></settings>\n')
    cmd=['mvn','-B','-q','-o','-s',str(settings),'-Dmaven.repo.local='+str(m2_repo())]+args
    r=run_limited(cmd,str(cwd),timeout,minimal_env(Path(cwd),{'MAVEN_OPTS':'-Xmx512m',**(env or {})}));r['command']=cmd;return r

def app_sources(workspace):
    main=Path(workspace)/'src/main/java'
    return sorted(main.rglob('*.java'))if main.is_dir()else []

def frozen_ok(workspace):
    pom=Path(workspace)/'pom.xml'
    if not pom.is_file()or pom.read_bytes()!=FROZEN_POM.read_bytes():return False
    return not any('benchjudge'in p.relative_to(workspace).parts for p in Path(workspace).rglob('*'))

def build(workspace,target):
    """target/project holds the judge copy of the build; target/classpath.txt is the runtime classpath."""
    target=Path(target)
    if target.exists():shutil.rmtree(target)
    project=target/'project';(project/'src').mkdir(parents=True)
    if not app_sources(workspace):return {'returncode':1,'timeout':False,'output':'No application Java source files under src/main/java'}
    shutil.copytree(Path(workspace)/'src/main',project/'src/main')
    shutil.copy(FROZEN_POM,project/'pom.xml')
    r=maven(['compile','dependency:build-classpath','-Dmdep.outputFile=cp.txt','-Dmdep.includeScope=runtime'],project)
    if r['returncode']!=0 or r['timeout']:return r
    overlay=target/'overlay-classes';overlay.mkdir()
    deps=(project/'cp.txt').read_text().strip()
    javac=['javac','-encoding','UTF-8','--release','21','-proc:none','-d',str(overlay),'-cp',str(project/'target/classes')+os.pathsep+deps]+[str(p)for p in sorted((OVERLAY/'java').rglob('*.java'))]
    o=run_limited(javac,str(target),120,minimal_env(target))
    if o['returncode']!=0 or o['timeout']:
        o['output']='judge overlay failed to compile against the submission:\n'+o['output'];return o
    shutil.copytree(OVERLAY/'resources',overlay,dirs_exist_ok=True)
    (target/'classpath.txt').write_text(os.pathsep.join([str((project/'target/classes').resolve()),str(overlay.resolve()),deps]))
    r['class_sha256']=digest_tree(project/'target/classes','.class')
    return r

def classes_present(target):
    return (Path(target)/'project/target/classes/app/Main.class').is_file()

def command(server):
    """Launch command and extra environment for runtime.Server."""
    cp=(server.classes/'classpath.txt').read_text()
    url='jdbc:h2:file:'+str((server.directory/'reserveledger').resolve())+H2_OPTIONS
    args={'server.port':str(server.port),'server.address':'127.0.0.1','spring.datasource.url':url,'bench.clock':'manual'if server.manual else 'system'}
    if server.manual:args['bench.now']=str(server.now)
    args.update(getattr(server,'overrides',{}))  # used by startup-validation cases
    cmd=['java']+JAVA_FLAGS+['-cp',cp,'app.Main']+[f'--{k}={v}'for k,v in args.items()]
    if not hasattr(server,'judge_port'):server.judge_port=free_port()
    return cmd,{'BENCH_JUDGE_PORT':str(server.judge_port)}

def judge_info(server,path):
    with urllib.request.urlopen(f'http://127.0.0.1:{server.judge_port}/{path}',timeout=3)as r:return json.loads(r.read())

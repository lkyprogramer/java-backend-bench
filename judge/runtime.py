"""Standard-library process and HTTP tools, used only by the trusted judge."""
from __future__ import annotations
import atexit, contextlib, hashlib, http.client, json, os, platform, re, shutil, signal, socket, subprocess, sys, tempfile, time
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA_FLAGS=['-Xms32m','-Xmx256m','-XX:ActiveProcessorCount=2','--add-modules','jdk.httpserver']
NOW=1700000000000

class CheckFailure(AssertionError): pass

class RestartFailure(CheckFailure):
    """S0: the service cannot come back on its own data directory after acknowledged history."""

def check(condition, message='check failed'):
    if not condition: raise CheckFailure(message)

def matches(actual, expected):
    """Required JSON fields, exact scalar types/order; permit documented response extensions."""
    if isinstance(expected,dict):
        return isinstance(actual,dict) and all(k in actual and matches(actual[k],v) for k,v in expected.items())
    if isinstance(expected,(list,tuple)):
        return isinstance(actual,type(expected)) and len(actual)==len(expected) and all(matches(a,b)for a,b in zip(actual,expected))
    return type(actual)is type(expected) and actual==expected

def minimal_env(home:Path, extra=None):
    env={k:os.environ[k] for k in ('PATH','JAVA_HOME','SYSTEMROOT','WINDIR','TMPDIR','TEMP') if k in os.environ}
    env.update({'HOME':str(home),'LANG':'C.UTF-8','LC_ALL':'C.UTF-8','PYTHONIOENCODING':'utf-8','PYTHONDONTWRITEBYTECODE':'1'})
    env.update(extra or {})
    return env

def kill_tree(proc):
    if os.name=='posix':
        with contextlib.suppress(ProcessLookupError):os.killpg(proc.pid,signal.SIGKILL)
    elif proc.poll() is None:proc.kill()
    with contextlib.suppress(Exception):proc.wait(timeout=3)

def run_limited(command, cwd, timeout=30, env=None):
    """A separate process group is killed on timeout, including child JVMs."""
    p=subprocess.Popen(command,cwd=cwd,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,start_new_session=(os.name=='posix'))
    try:
        out,_=p.communicate(timeout=timeout)
        kill_tree(p)
        return {'returncode':p.returncode,'timeout':False,'output':out.decode('utf-8','replace')[-200000:]}
    except subprocess.TimeoutExpired:
        kill_tree(p);out,_=p.communicate()
        return {'returncode':None,'timeout':True,'output':out.decode('utf-8','replace')[-200000:]}

def sources(workspace):
    src=Path(workspace)/'src'
    if not src.is_dir():return []
    return sorted(p for p in src.rglob('*.java') if 'benchkit' not in p.relative_to(src).parts)

def frozen_ok(workspace):
    for trusted in sorted((ROOT/'candidate/src/benchkit').glob('*.java')):
        candidate=Path(workspace)/'src/benchkit'/trusted.name
        if not candidate.is_file() or candidate.read_bytes()!=trusted.read_bytes():return False
    return True

def digest_tree(directory, suffix=None):
    directory=Path(directory);h=hashlib.sha256()
    for p in sorted(directory.rglob('*')):
        if p.is_file() and (suffix is None or p.suffix==suffix):
            h.update(str(p.relative_to(directory)).encode());h.update(b'\0');h.update(p.read_bytes());h.update(b'\0')
    return h.hexdigest()

def build(workspace, target):
    target=Path(target)
    if target.exists():shutil.rmtree(target)
    target.mkdir(parents=True)
    java=sources(workspace)+sorted((ROOT/'candidate/src/benchkit').glob('*.java'))
    if not sources(workspace):return {'returncode':1,'timeout':False,'output':'No application Java source files'}
    cmd=['javac','-encoding','UTF-8','--release','17','-d',str(target)]+[str(p.resolve()) for p in java]
    r=run_limited(cmd,str(target),40,minimal_env(target));r['command']=cmd
    r['class_sha256']=digest_tree(target,'.class')
    return r

def free_port():
    with socket.socket() as s:s.bind(('127.0.0.1',0));return s.getsockname()[1]

class Client:
    def __init__(self,port,token='test-token',timeout=3):self.port=port;self.token=token;self.timeout=timeout
    def request(self,method,path,body=None,raw=None,headers=None):
        if raw is not None:payload=raw.encode() if isinstance(raw,str) else raw
        elif body is not None:payload=json.dumps(body,separators=(',',':'),ensure_ascii=False).encode()
        else:payload=None
        hs={'Connection':'close'}
        if payload is not None:hs['Content-Type']='application/json'
        hs.update(headers or {})
        if path.startswith('/__test__/'):hs.setdefault('X-Test-Token',self.token)
        conn=http.client.HTTPConnection('127.0.0.1',self.port,timeout=self.timeout)
        try:
            conn.request(method,path,body=payload,headers=hs)
            response=conn.getresponse();data=response.read(2_000_001)
            check(len(data)<=2_000_000,'response exceeds limit')
            try:obj=json.loads(data.decode('utf-8'))
            except Exception as e:raise CheckFailure('response is not UTF-8 JSON: '+repr(data[:200])) from e
            return response.status,obj,dict((k.lower(),v)for k,v in response.getheaders())
        finally:conn.close()
    def expect(self,method,path,body=None,status=200,code=None,**kw):
        s,d,h=self.request(method,path,body,**kw)
        check(s==status,f'{method} {path}: expected {status}, got {s}: {d}')
        check(h.get('content-type','').lower().startswith('application/json'),'Content-Type must be application/json')
        if code is not None:
            check(isinstance(d,dict) and isinstance(d.get('error'),dict),'missing error envelope')
            check(d['error'].get('code')==code,f'expected error {code}, got {d}')
            check(isinstance(d['error'].get('message'),str) and len(d['error']['message'])>0,'missing error message')
        return d
    def item(self,sku='A',quantity=100):return self.expect('POST','/v1/items',{'sku':sku,'quantity':quantity},201)
    def stock(self,sku='A'):return self.expect('GET','/v1/items/'+sku)
    def reserve(self,key='k',lines=None,ttl=10000,status=201,code=None):
        return self.expect('POST','/v1/reservations',{'requestId':key,'lines':lines or [{'sku':'A','quantity':1}],'ttlMillis':ttl},status,code)
    def action(self,rid,action,status=200,code=None):return self.expect('POST',f'/v1/reservations/{rid}/{action}',{},status,code)
    def get(self,rid):return self.expect('GET','/v1/reservations/'+str(rid))
    def advance(self,millis,status=200,code=None):return self.expect('POST','/__test__/advance',{'millis':millis},status,code)
    def stats(self):return self.expect('GET','/v1/stats')

class Server:
    def __init__(self,classes,directory,manual=True,extra=None,port=None,now=NOW):
        self.classes=Path(classes);self.directory=Path(directory);self.directory.mkdir(parents=True,exist_ok=True)
        self.manual=manual;self.extra=extra or {};self.port=port or free_port();self.now=now
        self.token='token-'+os.urandom(8).hex();self.proc=None;self.log=None
        self.client=Client(self.port,self.token)
        atexit.register(self.stop)
    def spawn(self):
        cmd=['java']+JAVA_FLAGS+['-cp',str(self.classes.resolve()),'app.Main','--port',str(self.port),'--data-dir',str(self.directory.resolve()),'--clock','manual' if self.manual else 'system']
        if self.manual:cmd+=['--now',str(self.now)]
        self.log=open(self.directory/'server.log','ab')
        # Inherit the worker process group: the parent judge can kill every descendant.
        self.proc=subprocess.Popen(cmd,cwd=self.directory,env=minimal_env(self.directory,{'BENCH_TEST_TOKEN':self.token,**self.extra}),stdout=self.log,stderr=subprocess.STDOUT)
        return self
    def start(self,timeout=6):
        self.spawn();deadline=time.monotonic()+timeout
        while time.monotonic()<deadline:
            if self.proc.poll() is not None:break
            try:
                s,d,_=self.client.request('GET','/health')
                if s==200 and d.get('status')=='ok':return self
            except Exception:pass
            time.sleep(.025)
        self.stop()
        raise CheckFailure('server did not become healthy: '+(self.directory/'server.log').read_text(errors='replace')[-3000:])
    def stop(self,hard=False):
        if self.proc is not None and self.proc.poll() is None:
            try:
                if hard:self.proc.kill()
                else:self.proc.terminate()
                self.proc.wait(timeout=2)
            except Exception:
                self.proc.kill()
                with contextlib.suppress(Exception):self.proc.wait(timeout=2)
        if self.log:self.log.close();self.log=None
    def restart(self,hard=False,extra=None):
        self.stop(hard)
        if extra is not None:self.extra=extra
        try:return self.start()
        except RestartFailure:raise
        except CheckFailure as e:raise RestartFailure('restart on existing data directory failed: '+str(e)) from e
    def expect_start_failure(self):
        self.spawn()
        try:
            rc=self.proc.wait(timeout=3)
            check(rc!=0,'invalid startup must exit nonzero')
        except subprocess.TimeoutExpired as e:
            raise CheckFailure('invalid startup did not fail within 3 seconds') from e
        finally:self.stop()
    def __enter__(self):return self.start()
    def __exit__(self,*args):self.stop()

class Context:
    def __init__(self,workspace,classes,artifacts,seed):
        self.workspace=Path(workspace);self.classes=Path(classes);self.artifacts=Path(artifacts);self.artifacts.mkdir(parents=True,exist_ok=True);self.seed=int(seed);self.counter=0;self.servers=[]
    def server(self,**kw):
        self.counter+=1;s=Server(self.classes,self.artifacts/f'data {self.counter}',**kw);self.servers.append(s);return s
    def close(self):
        for s in self.servers:s.stop()

def environment():
    def ver(command):
        try:return subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=5,text=True).stdout.strip()
        except Exception as e:return 'unavailable: '+str(e)
    cpu=platform.processor()
    if Path('/proc/cpuinfo').exists():
        m=re.search(r'^model name\s*:\s*(.*)$',Path('/proc/cpuinfo').read_text(),re.M);cpu=m.group(1) if m else cpu
    if sys.platform=='darwin':cpu=ver(['sysctl','-n','machdep.cpu.brand_string'])
    result={'os':platform.platform(),'machine':platform.machine(),'cpu':cpu,'logical_cpus':os.cpu_count(),'java':ver(['java','-version']),'javac':ver(['javac','-version']),'python':sys.version,'java_flags':JAVA_FLAGS,'hostname':platform.node()}
    result['fingerprint']=hashlib.sha256(json.dumps(result,sort_keys=True).encode()).hexdigest()
    return result

def rss_bytes(pid):
    try:
        p=Path(f'/proc/{pid}/status')
        if p.exists():
            m=re.search(r'^VmRSS:\s*(\d+) kB',p.read_text(),re.M)
            return int(m.group(1))*1024 if m else None
        if sys.platform=='darwin':
            s=subprocess.check_output(['ps','-o','rss=','-p',str(pid)],text=True,timeout=2).strip()
            return int(s)*1024
    except (OSError,ValueError,subprocess.SubprocessError):pass
    return None

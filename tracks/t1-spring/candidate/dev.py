#!/usr/bin/env python3
"""Candidate-local helper. The official judge NEVER trusts or executes this script."""
import argparse,http.client,os,shutil,socket,subprocess,sys,tempfile,time
from pathlib import Path
ROOT=Path(__file__).resolve().parent

def mvn(*args):
    subprocess.run(['mvn','-B','-q',*args],cwd=ROOT,check=True)

def build():
    mvn('compile','dependency:build-classpath','-Dmdep.outputFile=target/cp.txt','-Dmdep.includeScope=runtime')
    return str(ROOT/'target/classes')+os.pathsep+(ROOT/'target/cp.txt').read_text().strip()

def command(cp,port,url,clock,now=None):
    cmd=['java','-Xms64m','-Xmx512m','-XX:ActiveProcessorCount=2','-cp',cp,'app.Main','--server.port='+str(port),'--server.address=127.0.0.1',
         '--spring.datasource.url='+url,'--bench.clock='+clock]
    return cmd+(['--bench.now='+str(now)]if now is not None else [])

def h2(directory):return 'jdbc:h2:file:'+str(Path(directory).resolve()/'reserveledger')+';WRITE_DELAY=0;LOCK_TIMEOUT=10000'

def main():
    p=argparse.ArgumentParser();p.add_argument('command',choices=['build','run','contract']);p.add_argument('--port',type=int,default=8080);p.add_argument('--data-dir',default='data');a=p.parse_args()
    cp=build()
    if a.command=='build':print('compiled');return
    if a.command=='run':
        Path(a.data_dir).mkdir(exist_ok=True)
        raise SystemExit(subprocess.call(command(cp,a.port,h2(a.data_dir),'system'),cwd=ROOT))
    with tempfile.TemporaryDirectory(prefix='reserveledger-dev-')as data:
        with socket.socket()as sock:sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]
        env=os.environ.copy();env['BENCH_TEST_TOKEN']='public-dev-token';env['BENCH_BASE_URL']=f'http://127.0.0.1:{port}'
        with open(Path(data)/'server.log','w')as log:
            server=subprocess.Popen(command(cp,port,h2(data),'manual',1700000000000),env=env,stdout=log,stderr=subprocess.STDOUT)
            try:
                for _ in range(600):
                    if server.poll()is not None:raise RuntimeError('Server exited: '+(Path(data)/'server.log').read_text()[-4000:])
                    try:
                        c=http.client.HTTPConnection('127.0.0.1',port,timeout=.5);c.request('GET','/health');r=c.getresponse();r.read();c.close()
                        if r.status==200:break
                    except (OSError,http.client.HTTPException):pass
                    time.sleep(.1)
                else:raise RuntimeError('Server did not become healthy: '+(Path(data)/'server.log').read_text()[-4000:])
                # Runs the contract package through surefire; the judge instead compiles it against JUnit + Jackson only.
                raise SystemExit(subprocess.call(['mvn','-B','-q','test','-Dtest=contract.**','-Dsurefire.failIfNoSpecifiedTests=false'],cwd=ROOT,env=env))
            finally:
                if server.poll()is None:
                    server.terminate()
                    try:server.wait(timeout=10)
                    except subprocess.TimeoutExpired:server.kill();server.wait()
if __name__=='__main__':main()

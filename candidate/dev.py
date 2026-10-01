#!/usr/bin/env python3
"""Candidate-local helper. The official judge NEVER trusts or executes this build script."""
import argparse,contextlib,http.client,json,os,shutil,socket,subprocess,sys,tempfile,time
from pathlib import Path
ROOT=Path(__file__).resolve().parent

def build():
    out=ROOT/'build/classes';shutil.rmtree(out,ignore_errors=True);out.mkdir(parents=True)
    src=sorted((ROOT/'src').rglob('*.java'))
    if not src:raise SystemExit('No Java sources')
    subprocess.run(['javac','-encoding','UTF-8','--release','17','-d',str(out)]+list(map(str,src)),check=True)
    return out

def main():
    p=argparse.ArgumentParser();p.add_argument('command',choices=['build','run','smoke','test']);p.add_argument('--port',type=int,default=8080);p.add_argument('--data-dir',default='data');a=p.parse_args()
    out=build()
    cmd=['java','-Xms32m','-Xmx256m','-XX:ActiveProcessorCount=2','--add-modules','jdk.httpserver','-cp',str(out),'app.Main']
    if a.command=='build':print(out);return
    if a.command=='run':raise SystemExit(subprocess.call(cmd+['--port',str(a.port),'--data-dir',a.data_dir,'--clock','system'],cwd=ROOT))
    with tempfile.TemporaryDirectory(prefix='reserveledger-dev-')as data:
        with socket.socket()as sock:sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]
        env=os.environ.copy();env['BENCH_TEST_TOKEN']='public-dev-token'
        with open(Path(data)/'server.log','w')as log:
            server=subprocess.Popen(cmd+['--port',str(port),'--data-dir',data,'--clock','manual','--now','1700000000000'],env=env,stdout=log,stderr=subprocess.STDOUT)
            try:
                ready=False
                for _ in range(200):
                    if server.poll()is not None:break
                    try:
                        c=http.client.HTTPConnection('127.0.0.1',port,timeout=.2);c.request('GET','/health');r=c.getresponse();r.read();c.close()
                        if r.status==200:ready=True;break
                    except (OSError,http.client.HTTPException):pass
                    time.sleep(.03)
                if not ready:raise RuntimeError('Server startup failed: '+(Path(data)/'server.log').read_text())
                test=ROOT/'tests'/('public_smoke.py'if a.command=='smoke'else 'candidate_tests.py')
                raise SystemExit(subprocess.call([sys.executable,'-I',str(test),'--base-url',f'http://127.0.0.1:{port}'],env=env,cwd=ROOT))
            finally:
                if server.poll()is None:
                    server.terminate()
                    try:server.wait(timeout=2)
                    except subprocess.TimeoutExpired:server.kill();server.wait()
if __name__=='__main__':main()

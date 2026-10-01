#!/usr/bin/env python3
"""Measure one real agent command, without guessing tokens or provider configuration."""
import argparse,contextlib,datetime,hashlib,json,os,signal,subprocess,sys,time
from pathlib import Path

def timestamp():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def stop_group(process):
    if os.name=='posix':
        with contextlib.suppress(ProcessLookupError):os.killpg(process.pid,signal.SIGKILL)
    elif process.poll()is None:process.kill()
    with contextlib.suppress(Exception):process.wait(timeout=3)

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cwd',required=True);parser.add_argument('--out',required=True)
    parser.add_argument('--timeout',type=float,default=10800);parser.add_argument('--allow-exec',action='store_true')
    parser.add_argument('--record-argv',action='store_true',help='Store full command arguments. Never use with secrets in arguments.')
    parser.add_argument('argv',nargs=argparse.REMAINDER);args=parser.parse_args()
    cmd=args.argv[1:]if args.argv[:1]==['--']else args.argv
    if not args.allow_exec:parser.error('--allow-exec is required; this runs your command with your existing environment and permissions')
    if not cmd or args.timeout<=0:parser.error('Provide a command after -- and a positive timeout')
    cwd=Path(args.cwd).resolve();out=Path(args.out).resolve()
    if not cwd.is_dir():parser.error('--cwd must be an existing directory')
    if out.exists()and any(out.iterdir()):parser.error('Output directory is not empty')
    if cwd==out or cwd in out.parents:parser.error('Keep timing records outside the model workspace')
    out.mkdir(parents=True,exist_ok=True);start=timestamp();clock=time.monotonic();process=None;timed_out=False;interrupted=False;error=None
    try:
        with (out/'agent.log').open('wb')as log:
            process=subprocess.Popen(cmd,cwd=cwd,stdout=log,stderr=subprocess.STDOUT,start_new_session=os.name=='posix')
            try:process.wait(timeout=args.timeout)
            except subprocess.TimeoutExpired:timed_out=True
            except KeyboardInterrupt:interrupted=True
            finally:stop_group(process)
    except OSError as exc:error=str(exc)
    record={'source':'subprocess_wall_clock','started_at_utc':start,'finished_at_utc':timestamp(),
            'wall_seconds':round(time.monotonic()-clock,6),'budget_seconds':args.timeout,
            'returncode':process.returncode if process else None,'timed_out':timed_out,'interrupted':interrupted,'launch_error':error,
            'executable':cmd[0],'argv':cmd if args.record_argv else None,
            'argv_sha256':hashlib.sha256(json.dumps(cmd,ensure_ascii=False).encode()).hexdigest(),
            'cwd':str(cwd),'input_tokens':None,'output_tokens':None,'cached_input_tokens':None,'tool_calls':None,
            'cost_amount':None,'cost_currency':None,
            'note':'Agent stdout/stderr saved to agent.log; logs may contain sensitive data. This is not a sandbox. No tokens inferred.'}
    (out/'timing.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(record,ensure_ascii=False,indent=2))
    if error:return 2
    if timed_out:return 124
    if interrupted:return 130
    return 0 if process.returncode==0 else 1
if __name__=='__main__':raise SystemExit(main())

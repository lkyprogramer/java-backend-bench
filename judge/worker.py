"""One case per OS process. Parent timeout includes all descendant JVMs."""
import argparse,json,time,traceback
from pathlib import Path
from .runtime import Context,CheckFailure,RestartFailure,use_track
from .tracks import track

def main():
    p=argparse.ArgumentParser();p.add_argument('--workspace',required=True);p.add_argument('--classes',required=True);p.add_argument('--artifacts',required=True);p.add_argument('--case',required=True);p.add_argument('--seed',type=int,required=True);p.add_argument('--result',required=True);p.add_argument('--track',default='base');a=p.parse_args()
    use_track(a.track);CASES=track(a.track).cases()
    case=CASES[a.case];row={k:v for k,v in case.items()if k!='fn'};start=time.monotonic();ctx=Context(a.workspace,a.classes,a.artifacts,a.seed)
    try:case['fn'](ctx);row.update(status='pass',message='ok')
    except RestartFailure as e:row.update(status='fail',severity='S0',message=str(e),traceback=traceback.format_exc())
    except (CheckFailure,AssertionError)as e:row.update(status='fail',message=str(e),traceback=traceback.format_exc())
    except Exception as e:row.update(status='error',message=f'{type(e).__name__}: {e}',traceback=traceback.format_exc())
    finally:ctx.close()
    row['seconds']=round(time.monotonic()-start,6);Path(a.result).write_text(json.dumps(row,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(row,ensure_ascii=False))
if __name__=='__main__':main()

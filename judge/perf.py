"""Paired, correctness-checked microservice performance profile. No third-party tools."""
from __future__ import annotations
import argparse,concurrent.futures as cf,hashlib,json,math,os,statistics,threading,time,traceback
from pathlib import Path
from .runtime import Server,environment,rss_bytes,check,matches
from .scoring import rate_score

PROFILE={'id':'reserveledger-standard-v1','warmup_reads':200,'read_requests':1000,'read_workers':8,'lifecycle_operations':160,'lifecycle_workers':8,'small_active':100,'large_active':1500,'scale_requests':400,'scale_workers':4}
PROFILE_HASH=hashlib.sha256(json.dumps(PROFILE,sort_keys=True).encode()).hexdigest()
WEIGHTS={'read_throughput':.25,'lifecycle_throughput':.25,'read_tail_latency':.20,'scale_growth':.20,'memory':.10}

def percentile(values,p):
    if not values:return None
    return sorted(values)[max(0,min(len(values)-1,math.ceil(len(values)*p)-1))]

def load(n,workers,operation):
    def one(i):
        start=time.perf_counter()
        try:operation(i);return time.perf_counter()-start,None
        except Exception as e:return time.perf_counter()-start,f'{type(e).__name__}: {e}'
    cpu=time.process_time();start=time.perf_counter()
    with cf.ThreadPoolExecutor(max_workers=workers)as pool:results=list(pool.map(one,range(n)))
    elapsed=time.perf_counter()-start;cpu=time.process_time()-cpu
    latencies=[r[0]*1000 for r in results];errors=[r[1]for r in results if r[1]is not None]
    return {'requested':n,'successful':n-len(errors),'errors':len(errors),'error_samples':errors[:5],
            'elapsed_seconds':elapsed,'successful_ops_per_second':(n-len(errors))/elapsed,
            'p50_ms':percentile(latencies,.50),'p95_ms':percentile(latencies,.95),'p99_ms':percentile(latencies,.99),
            'loadgen_cpu_cores':cpu/elapsed,'possible_client_bottleneck':cpu/elapsed>.85}

def process_cpu_seconds(pid):
    try:
        text=Path(f'/proc/{pid}/stat').read_text();fields=text[text.rfind(')')+2:].split()
        return (int(fields[11])+int(fields[12]))/os.sysconf('SC_CLK_TCK')
    except (OSError,ValueError,IndexError):return None

def one_rep(classes,out,index):
    s=Server(classes,Path(out)/f'data-{index}')
    samples=[];stop=threading.Event();sampler=None
    try:
        s.start();c=s.client;c.item('hot',1000000000)
        def sample():
            while not stop.is_set():
                value=rss_bytes(s.proc.pid)
                if value is not None:samples.append(value)
                stop.wait(.05)
        sampler=threading.Thread(target=sample,daemon=True);sampler.start();cpu_start=process_cpu_seconds(s.proc.pid)
        def inventory(expected_held,expected_sold):
            d=c.stock('hot')
            check(matches(d,{'sku':'hot','total':1000000000,'available':1000000000-expected_held-expected_sold,'held':expected_held,'sold':expected_sold}),'incorrect inventory during load')
        for _ in range(PROFILE['warmup_reads']):inventory(0,0)
        read=load(PROFILE['read_requests'],PROFILE['read_workers'],lambda i:inventory(0,0))
        def cycle(i):
            a=c.reserve('cycle'+str(i),[{'sku':'hot','quantity':1}],86400000)
            check(a.get('state')=='ACTIVE' and matches(a.get('lines'),[{'sku':'hot','quantity':1}]),'incorrect reservation response')
            action='confirm'if i%2 else 'cancel';b=c.action(a['id'],action)
            check(b.get('state')==('CONFIRMED'if i%2 else 'CANCELLED'),'incorrect terminal state')
        life=load(PROFILE['lifecycle_operations'],PROFILE['lifecycle_workers'],cycle)
        sold=PROFILE['lifecycle_operations']//2;inventory(0,sold)
        for i in range(PROFILE['small_active']):c.reserve('active'+str(i),[{'sku':'hot','quantity':1}],86400000)
        for _ in range(50):inventory(PROFILE['small_active'],sold)
        small=load(PROFILE['scale_requests'],PROFILE['scale_workers'],lambda i:inventory(PROFILE['small_active'],sold))
        for i in range(PROFILE['small_active'],PROFILE['large_active']):c.reserve('active'+str(i),[{'sku':'hot','quantity':1}],86400000)
        for _ in range(50):inventory(PROFILE['large_active'],sold)
        large=load(PROFILE['scale_requests'],PROFILE['scale_workers'],lambda i:inventory(PROFILE['large_active'],sold))
        inventory(PROFILE['large_active'],sold);cpu_end=process_cpu_seconds(s.proc.pid)
        phases={'read':read,'lifecycle':life,'small':small,'large':large};valid=all(v['errors']==0 for v in phases.values())
        return {'index':index,'status':'measured'if valid else 'candidate_failure','phases':phases,
                'read_rps':read['successful_ops_per_second'],'lifecycle_ops_per_sec':life['successful_ops_per_second'],
                'read_p95_ms':read['p95_ms'],'read_p50_ms':read['p50_ms'],'read_p99_ms':read['p99_ms'],
                'lifecycle_p95_ms':life['p95_ms'],'growth_ratio':large['p95_ms']/small['p95_ms'],
                'peak_rss_bytes':max(samples)if samples else None,
                'java_cpu_seconds':cpu_end-cpu_start if cpu_start is not None and cpu_end is not None else None,
                'possible_client_bottleneck':any(v['possible_client_bottleneck']for v in phases.values())}
    except Exception as e:
        return {'index':index,'status':'candidate_failure','error':str(e),'traceback':traceback.format_exc(),'peak_rss_bytes':max(samples)if samples else None}
    finally:
        stop.set()
        if sampler:sampler.join(timeout=2)
        s.stop()

def combine(repetitions):
    result={};keys=['read_rps','lifecycle_ops_per_sec','read_p95_ms','read_p50_ms','read_p99_ms','lifecycle_p95_ms','growth_ratio','peak_rss_bytes','java_cpu_seconds']
    for key in keys:
        values=[r.get(key)for r in repetitions]
        result[key]=statistics.median(values)if values and all(isinstance(v,(int,float))and math.isfinite(v)for v in values)else None
    return result

def measure(classes,out,reps=3):
    out=Path(out);out.mkdir(parents=True,exist_ok=True);rows=[]
    for index in range(reps):
        row=one_rep(classes,out,index);rows.append(row)
        (out/f'rep-{index}.json').write_text(json.dumps(row,ensure_ascii=False,indent=2),encoding='utf-8')
        print(f'performance repetition {index+1}/{reps}: {row["status"]}',flush=True)
        if row['status']!='measured':break
    return {'profile':PROFILE,'profile_sha256':PROFILE_HASH,'environment':environment(),'requested_reps':reps,
            'status':'measured'if len(rows)==reps and all(r['status']=='measured'for r in rows)else 'candidate_failure',
            'repetitions':rows,'summary':combine(rows),
            'possible_client_bottleneck':any(r.get('possible_client_bottleneck',False)for r in rows)}

def compare(actual,reference):
    if reference.get('status')!='measured':return {'score_10':0,'status':'infrastructure_error','message':'reference calibration failed'}
    if actual.get('status')!='measured':return {'score_10':0,'status':'candidate_failure','components_10':{k:0 for k in WEIGHTS}}
    if actual.get('environment',{}).get('fingerprint')!=reference.get('environment',{}).get('fingerprint'):
        return {'score_10':0,'status':'not_comparable','message':'calibration hardware/runtime fingerprint differs'}
    if actual.get('profile_sha256')!=reference.get('profile_sha256')or actual.get('requested_reps')!=reference.get('requested_reps'):
        return {'score_10':0,'status':'not_comparable','message':'profile or repetition count differs'}
    a=actual['summary'];r=reference['summary']
    if a.get('peak_rss_bytes')is None or r.get('peak_rss_bytes')is None:return {'score_10':0,'status':'infrastructure_error','message':'RSS measurement unsupported'}
    components={'read_throughput':rate_score(a['read_rps'],r['read_rps']),
        'lifecycle_throughput':rate_score(a['lifecycle_ops_per_sec'],r['lifecycle_ops_per_sec']),
        'read_tail_latency':rate_score(a['read_p95_ms'],r['read_p95_ms'],False),
        'scale_growth':rate_score(max(1,a['growth_ratio']),max(1,r['growth_ratio']),False),
        'memory':rate_score(a['peak_rss_bytes'],r['peak_rss_bytes'],False,1.25)}
    return {'score_10':round(sum(components[k]*WEIGHTS[k]for k in WEIGHTS),4),'status':'measured','components_10':components,'weights':WEIGHTS,
            'tolerance':{'performance':1.20,'memory':1.25},'possible_client_bottleneck':actual.get('possible_client_bottleneck',False)or reference.get('possible_client_bottleneck',False)}

def main():
    p=argparse.ArgumentParser();p.add_argument('--classes',required=True);p.add_argument('--out',required=True);p.add_argument('--reps',type=int,default=3);a=p.parse_args()
    if not 1<=a.reps<=20:p.error('--reps must be 1..20')
    r=measure(a.classes,a.out,a.reps);Path(a.out,'performance.json').write_text(json.dumps(r,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(r['summary'],ensure_ascii=False))
if __name__=='__main__':main()

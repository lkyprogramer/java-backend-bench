"""Descriptive aggregation; repeated grading of the same source is NOT an independent model attempt."""
import csv,hashlib,json,random,statistics
from collections import defaultdict
from pathlib import Path
from .runner import DIMENSIONS,fresh_out,write_json

def bootstrap_mean_ci(values,seed=20260930):
    if len(values)<3:return None
    rng=random.Random(seed);means=sorted(statistics.mean(rng.choices(values,k=len(values)))for _ in range(5000))
    return [means[124],means[4874]]

def aggregate(paths,out):
    out=fresh_out(out);groups=defaultdict(list);invalid=[]
    for path in paths:
        r=json.loads(Path(path).read_text());r['_file']=str(Path(path).resolve())
        if not r.get('validity',{}).get('rankable'):invalid.append({'file':r['_file'],'reason':r.get('validity')});continue
        model=dict(r.get('metadata',{}).get('model',{}));model.pop('generation_seed',None)
        conditions=(r['benchmark_version'],r['judge_sha256'],r['environment']['fingerprint'],json.dumps(r.get('metadata',{}).get('protocol',{}),sort_keys=True))
        config=json.dumps(model,sort_keys=True)
        key=(r['model_label'],conditions,config)
        groups[key].append(r)
    result=[];global_conditions=set()
    for (label,condition,config),rs in groups.items():
        values=[r['summary']['official_overall_10']for r in rs]
        # Generation protocol is distinct from the model identity/settings being compared.
        conditions={(r['benchmark_version'],r['judge_sha256'],r['environment']['fingerprint'],json.dumps(r.get('metadata',{}).get('protocol',{}),sort_keys=True))for r in rs}
        global_conditions.update(conditions)
        unique=len({r['java_source_sha256']for r in rs});warnings=[]
        if len(conditions)>1:warnings.append('Mixed benchmark/hardware/agent protocols: these runs should not be pooled.')
        if unique<len(rs):warnings.append('Some runs have identical Java source: do not call them independent model generations.')
        if len(rs)<3:warnings.append('Fewer than three attempts; no confidence interval reported.')
        if unique<3:warnings.append('Fewer than three distinct source outcomes: no model-generation confidence interval reported.')
        if any(not r.get('metadata')for r in rs):warnings.append('Missing generation metadata; model and agent conditions are not fully auditable.')
        config_id=hashlib.sha256(json.dumps([condition,config],sort_keys=True).encode()).hexdigest()[:12]
        row={'model_label':label,'configuration_id':config_id,'model_configuration':json.loads(config),'attempts':len(rs),'distinct_java_sources':unique,
             'mean_10':statistics.mean(values),'median_10':statistics.median(values),'min_10':min(values),'max_10':max(values),
             'sample_stddev':statistics.stdev(values)if len(values)>1 else None,
             'bootstrap_mean_ci95':bootstrap_mean_ci(values)if unique>=3 else None,
             'critical_gate_pass_rate':sum(r['summary']['gate']=='NO_CRITICAL_FAILURE_OBSERVED'for r in rs)/len(rs),
             'dimension_means_10':{d:statistics.mean(r['summary']['dimensions'][d]for r in rs)for d in DIMENSIONS},
             'warnings':warnings,'reports':[r['_file']for r in rs]}
        result.append(row)
    result.sort(key=lambda r:r['median_10'],reverse=True)
    report={'groups':result,'excluded_invalid_runs':invalid,'cross_group_conditions_identical':len(global_conditions)<=1,
            'note':'Descriptive statistics on one task only; bootstrap CI is not proof of general capability or statistical significance.'}
    write_json(out/'comparison.json',report)
    keys=['model_label','configuration_id','attempts','distinct_java_sources','mean_10','median_10','min_10','max_10','sample_stddev','critical_gate_pass_rate']+list(DIMENSIONS)
    with (out/'comparison.csv').open('w',newline='',encoding='utf-8-sig')as f:
        w=csv.DictWriter(f,fieldnames=keys,extrasaction='ignore');w.writeheader()
        for row in result:w.writerow({**row,**row['dimension_means_10']})
    lines=['# 模型编程评测对比','','| 模型 | 运行数 | 不同源码数 | 平均 / 10 | 中位数 / 10 | 范围 | 无关键失败比例 |','|---|---:|---:|---:|---:|---|---:|']
    for row in result:lines.append(f'| {row["model_label"].replace("|","/")+' / '+row['configuration_id']} | {row["attempts"]} | {row["distinct_java_sources"]} | {row["mean_10"]:.3f} | {row["median_10"]:.3f} | {row["min_10"]:.2f}–{row["max_10"]:.2f} | {row["critical_gate_pass_rate"]:.1%} |')
    lines+=['','## 判读限制',f'跨组运行条件完全相同：{report["cross_group_conditions_identical"]}。不同条件不得直接解释为纯模型差异。']
    for row in result:
        for warning in row['warnings']:lines.append(f'\n{row["model_label"]}: {warning}')
    if invalid:lines.append(f'\n有 {len(invalid)} 个诊断/基础设施无效运行未进入统计，明细见 comparison.json。正常编译失败/关键业务失败的完整评测仍以 0 分/门槛后分数进入统计，不丢弃失败尝试。')
    lines.append('\n相同标签下的不同硬件、裁判版本、Agent 协议或模型配置自动拆组；只有 generation_seed 的变化视为同配置独立尝试。配置字段缺失仍须人工核对，分组不能证明元数据真实。')
    lines.append('\n不要用同一份源码重复评测的波动冒充三次独立 Agent 开发。源码相同也可能是模型独立生成的相同结果，需结合生成记录判断；本工具只标记，不擅自去重。')
    (out/'comparison.md').write_text('\n'.join(lines)+'\n',encoding='utf-8');return report

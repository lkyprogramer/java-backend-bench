"""Frozen scoring rules: all scored dimensions are in [0, 10]."""
import math

DIMENSIONS = {
    'D01':'构建与交付', 'D02':'业务正确性', 'D03':'协议与输入边界',
    'D04':'幂等与重试', 'D05':'并发与原子性', 'D06':'持久化与故障恢复',
    'D07':'时间与状态机', 'D08':'随机模型与回归', 'D09':'性能与资源',
    'D10':'自测有效性',
}

def case_score(rows):
    return round(10 * sum(r.get('status') == 'pass' for r in rows) / len(rows), 4) if rows else 0.0

def rate_score(actual, reference, higher=True, tolerance=1.2):
    if not isinstance(actual, (int,float)) or not isinstance(reference,(int,float)):
        return 0.0
    if not math.isfinite(actual) or not math.isfinite(reference) or min(actual,reference) <= 0:
        return 0.0
    ratio = actual/reference if higher else reference/actual
    return round(10 * min(1.0, max(0.0, ratio*tolerance)), 4)

def summarize(scores, build_ok=True, critical_failures=()):
    cleaned = {}
    for name in DIMENSIONS:
        x = scores.get(name,0)
        if not isinstance(x,(int,float)) or not math.isfinite(x):
            x=0
        cleaned[name] = round(max(0.0,min(10.0,x)),4)
    raw = sum(cleaned.values())/len(DIMENSIONS)
    gated = 0.0 if not build_ok else min(raw,4.0) if critical_failures else raw
    return {'dimensions':cleaned, 'raw_overall_10':round(raw,4),
            'overall_10':round(gated,4), 'critical_failures':list(critical_failures),
            'gate':'NOT_BUILDABLE' if not build_ok else 'CORE_FAILURE' if critical_failures else 'NO_CRITICAL_FAILURE_OBSERVED'}

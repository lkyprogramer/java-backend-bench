# ReserveLedger 评测报告：REFERENCE-SELF-CHECK-NOT-A-MODEL

- Run ID：`reference-validation`
- 基准：1.0.0；测试种子：1701
- 源码快照 SHA256：`d10f2a2204fbe1232baa41f6dbac6905707ad78cdbfeb1b0676303b80079b62c`
- 完整且可计分：**True**
- 自动原始综合：**9.950 / 10**
- 关键错误门槛后：**9.950 / 10**
- 正式综合分：**9.9504 / 10**（null 表示诊断/基础设施无效，不是模型零分）
- Gate：`NO_CRITICAL_FAILURE_OBSERVED`；关键失败：无

| 维度 | 名称 | 分数 / 10 |
|---|---|---:|
| D01 | 构建与交付 | 10.000 |
| D02 | 业务正确性 | 10.000 |
| D03 | 协议与输入边界 | 10.000 |
| D04 | 幂等与重试 | 10.000 |
| D05 | 并发与原子性 | 10.000 |
| D06 | 持久化与故障恢复 | 10.000 |
| D07 | 时间与状态机 | 10.000 |
| D08 | 随机模型与回归 | 10.000 |
| D09 | 性能与资源 | 9.504 |
| D10 | 自测有效性 | 10.000 |

## 性能原始数据

```json
{
  "score_10": 9.5038,
  "status": "measured",
  "components_10": {
    "read_throughput": 8.6647,
    "lifecycle_throughput": 9.7603,
    "read_tail_latency": 9.4877,
    "scale_growth": 10.0,
    "memory": 10.0
  },
  "weights": {
    "read_throughput": 0.25,
    "lifecycle_throughput": 0.25,
    "read_tail_latency": 0.2,
    "scale_growth": 0.2,
    "memory": 0.1
  },
  "tolerance": {
    "performance": 1.2,
    "memory": 1.25
  },
  "possible_client_bottleneck": true,
  "summary": {
    "read_rps": 1157.9831944470527,
    "lifecycle_ops_per_sec": 560.553467791659,
    "read_p95_ms": 13.34705799990843,
    "read_p50_ms": 4.494590999911452,
    "read_p99_ms": 25.612895999984175,
    "lifecycle_p95_ms": 25.197738999850117,
    "growth_ratio": 0.978545399387091,
    "peak_rss_bytes": 98705408,
    "java_cpu_seconds": 6.24
  },
  "measurement_file": "performance/performance.json",
  "calibration_created_at_utc": "2026-09-30T13:59:10.089941+00:00",
  "reference_summary": {
    "read_rps": 1603.7170466174975,
    "lifecycle_ops_per_sec": 689.1842902172748,
    "read_p95_ms": 10.552736000136065,
    "read_p50_ms": 4.1610230000515,
    "read_p99_ms": 15.31989400018574,
    "lifecycle_p95_ms": 20.9519539998837,
    "growth_ratio": 1.0853120098196023,
    "peak_rss_bytes": 97984512,
    "java_cpu_seconds": 4.71
  }
}
```

## 自测有效性

```json
{
  "score_10": 10.0,
  "status": "measured",
  "baseline_passed": true,
  "baseline": [
    {
      "id": "correct-1",
      "returncode": 0,
      "timeout": false,
      "log": "correct-1.log"
    },
    {
      "id": "correct-2",
      "returncode": 0,
      "timeout": false,
      "log": "correct-2.log"
    }
  ],
  "mutants": [
    {
      "id": "M01",
      "returncode": 1,
      "timeout": false,
      "log": "M01.log",
      "title": "允许超卖",
      "killer": "C01",
      "killed": true
    },
    {
      "id": "M02",
      "returncode": 1,
      "timeout": false,
      "log": "M02.log",
      "title": "相同幂等键重复创建",
      "killer": "I01",
      "killed": true
    },
    {
      "id": "M03",
      "returncode": 1,
      "timeout": false,
      "log": "M03.log",
      "title": "同键异参被接受",
      "killer": "I03",
      "killed": true
    },
    {
      "id": "M04",
      "returncode": 1,
      "timeout": false,
      "log": "M04.log",
      "title": "到期比较使用 > 而非 >=",
      "killer": "T01",
      "killed": true
    },
    {
      "id": "M05",
      "returncode": 1,
      "timeout": false,
      "log": "M05.log",
      "title": "重复取消多次释放",
      "killer": "C04",
      "killed": true
    },
    {
      "id": "M06",
      "returncode": 1,
      "timeout": false,
      "log": "M06.log",
      "title": "确认错误归还可用库存",
      "killer": "F03",
      "killed": true
    },
    {
      "id": "M07",
      "returncode": 1,
      "timeout": false,
      "log": "M07.log",
      "title": "已取消记录允许确认",
      "killer": "T04",
      "killed": true
    },
    {
      "id": "M08",
      "returncode": 1,
      "timeout": false,
      "log": "M08.log",
      "title": "多商品失败发生部分扣减",
      "killer": "F06",
      "killed": true
    },
    {
      "id": "M09",
      "returncode": 1,
      "timeout": false,
      "log": "M09.log",
      "title": "错误接受零预占数量",
      "killer": "V02",
      "killed": true
    },
    {
      "id": "M10",
      "returncode": 1,
      "timeout": false,
      "log": "M10.log",
      "title": "忽略列表状态过滤",
      "killer": "F09",
      "killed": true
    }
  ],
  "timeout_policy": "no_kill_credit"
}
```

## 所有验收结果

| Case | 维度 | 结果 | 秒 | 说明 |
|---|---|---|---:|---|
| B01 | D01 | pass | 0.001 | ok |
| B02 | D01 | pass | 0.000 | ok |
| B03 | D01 | pass | 0.000 | ok |
| B04 | D01 | pass | 2.757 | ok |
| B05 | D01 | pass | 0.240 | ok |
| B06 | D01 | pass | 0.227 | ok |
| B07 | D01 | pass | 0.241 | ok |
| B08 | D01 | pass | 0.394 | ok |
| B09 | D01 | pass | 0.507 | ok |
| B10 | D01 | pass | 0.000 | ok |
| F01 | D02 | pass | 0.233 | ok |
| F02 | D02 | pass | 0.297 | ok |
| F03 | D02 | pass | 0.300 | ok |
| F04 | D02 | pass | 0.256 | ok |
| F05 | D02 | pass | 0.298 | ok |
| F06 | D02 | pass | 0.245 | ok |
| F07 | D02 | pass | 0.237 | ok |
| F08 | D02 | pass | 0.261 | ok |
| F09 | D02 | pass | 0.328 | ok |
| F10 | D02 | pass | 0.228 | ok |
| V01 | D03 | pass | 0.255 | ok |
| V02 | D03 | pass | 0.314 | ok |
| V03 | D03 | pass | 0.325 | ok |
| V04 | D03 | pass | 0.259 | ok |
| V05 | D03 | pass | 0.348 | ok |
| V06 | D03 | pass | 0.272 | ok |
| V07 | D03 | pass | 0.220 | ok |
| V08 | D03 | pass | 0.374 | ok |
| V09 | D03 | pass | 0.311 | ok |
| V10 | D03 | pass | 0.469 | ok |
| I01 | D04 | pass | 0.326 | ok |
| I02 | D04 | pass | 0.283 | ok |
| I03 | D04 | pass | 0.407 | ok |
| I04 | D04 | pass | 0.482 | ok |
| I05 | D04 | pass | 0.271 | ok |
| I06 | D04 | pass | 0.351 | ok |
| I07 | D04 | pass | 0.344 | ok |
| I08 | D04 | pass | 0.587 | ok |
| I09 | D04 | pass | 0.278 | ok |
| I10 | D04 | pass | 0.274 | ok |
| C01 | D05 | pass | 0.318 | ok |
| C02 | D05 | pass | 0.311 | ok |
| C03 | D05 | pass | 0.247 | ok |
| C04 | D05 | pass | 0.290 | ok |
| C05 | D05 | pass | 0.410 | ok |
| C06 | D05 | pass | 0.329 | ok |
| C07 | D05 | pass | 0.597 | ok |
| C08 | D05 | pass | 0.450 | ok |
| C09 | D05 | pass | 0.344 | ok |
| C10 | D05 | pass | 0.310 | ok |
| R01 | D06 | pass | 0.580 | ok |
| R02 | D06 | pass | 0.709 | ok |
| R03 | D06 | pass | 1.303 | ok |
| R04 | D06 | pass | 0.920 | ok |
| R05 | D06 | pass | 1.043 | ok |
| R06 | D06 | pass | 1.585 | ok |
| R07 | D06 | pass | 0.628 | ok |
| R08 | D06 | pass | 1.113 | ok |
| R09 | D06 | pass | 1.566 | ok |
| R10 | D06 | pass | 0.374 | ok |
| T01 | D07 | pass | 0.325 | ok |
| T02 | D07 | pass | 0.277 | ok |
| T03 | D07 | pass | 0.246 | ok |
| T04 | D07 | pass | 0.317 | ok |
| T05 | D07 | pass | 0.285 | ok |
| T06 | D07 | pass | 0.233 | ok |
| T07 | D07 | pass | 0.335 | ok |
| T08 | D07 | pass | 0.298 | ok |
| T09 | D07 | pass | 0.375 | ok |
| T10 | D07 | pass | 0.366 | ok |
| O01 | D08 | pass | 1.241 | ok |
| O02 | D08 | pass | 1.559 | ok |
| O03 | D08 | pass | 1.970 | ok |
| O04 | D08 | pass | 1.113 | ok |
| O05 | D08 | pass | 1.806 | ok |
| O06 | D08 | pass | 1.113 | ok |
| O07 | D08 | pass | 1.110 | ok |
| O08 | D08 | pass | 1.269 | ok |
| O09 | D08 | pass | 1.531 | ok |
| O10 | D08 | pass | 1.061 | ok |

## 有效性与限制

```json
{
  "complete_profile": true,
  "diagnostic": false,
  "integrity_ok": true,
  "infrastructure_errors": [],
  "rankable": true,
  "model_identity_is_operator_declared": true,
  "security_sandbox": false
}
```

No model/agent generation metadata supplied. Label is unverified; generation efficiency is unknown.

Load-generator CPU may be limiting throughput. Interpret speed scores cautiously.

模型生成用时、token 和成本只接受真实采集或操作者提供的记录；本报告的 evaluation_seconds 是裁判耗时，不是模型开发耗时。
人工维护性/文档分独立存于 manual_review；未填写时不伪造人工分。一个题目的得分不代表模型通用编程能力，也不构成生产上线认证。

# ReserveLedger 评测报告：REFERENCE-SELF-CHECK-NOT-A-MODEL

- Run ID：`reference-validation`
- 基准：1.1.0；测试种子：1701
- 源码快照 SHA256：`d10f2a2204fbe1232baa41f6dbac6905707ad78cdbfeb1b0676303b80079b62c`
- 完整且可计分：**True**
- 自动原始综合：**10.000 / 10**
- 关键错误门槛后：**10.000 / 10**
- 正式综合分：**10.0 / 10**（null 表示诊断/基础设施无效，不是模型零分）
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
| D09 | 性能与资源 | 10.000 |
| D10 | 自测有效性 | 10.000 |

## 性能原始数据

```json
{
  "score_10": 10.0,
  "status": "measured",
  "components_10": {
    "read_throughput": 10.0,
    "lifecycle_throughput": 10.0,
    "read_tail_latency": 10.0,
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
    "read_rps": 5539.139423488536,
    "lifecycle_ops_per_sec": 98.96347358693133,
    "read_p95_ms": 2.5902079996740213,
    "read_p50_ms": 1.2950420004926855,
    "read_p99_ms": 4.22058299955097,
    "lifecycle_p95_ms": 96.22850000050676,
    "growth_ratio": 0.9176725053630924,
    "peak_rss_bytes": 109199360,
    "java_cpu_seconds": null
  },
  "measurement_file": "performance/performance.json",
  "calibration_created_at_utc": "2026-10-01T04:29:02.500701+00:00",
  "reference_summary": {
    "read_rps": 5592.652207989265,
    "lifecycle_ops_per_sec": 98.9819066283271,
    "read_p95_ms": 2.4462080000375863,
    "read_p50_ms": 1.2254170014784904,
    "read_p99_ms": 4.325957999753882,
    "lifecycle_p95_ms": 98.79879199979769,
    "growth_ratio": 1.1116313914185578,
    "peak_rss_bytes": 109363200,
    "java_cpu_seconds": null
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
      "log": "correct-1.log",
      "tests": {
        "started": [
          "__main__.ContractTests.test_batch_rollback",
          "__main__.ContractTests.test_cancel_once",
          "__main__.ContractTests.test_confirm_sells",
          "__main__.ContractTests.test_conflict",
          "__main__.ContractTests.test_expiry_boundary",
          "__main__.ContractTests.test_idempotent",
          "__main__.ContractTests.test_no_oversell",
          "__main__.ContractTests.test_state_filter",
          "__main__.ContractTests.test_terminal_transition",
          "__main__.ContractTests.test_zero_reservation_invalid"
        ],
        "failed": []
      }
    },
    {
      "id": "correct-2",
      "returncode": 0,
      "timeout": false,
      "log": "correct-2.log",
      "tests": {
        "started": [
          "__main__.ContractTests.test_batch_rollback",
          "__main__.ContractTests.test_cancel_once",
          "__main__.ContractTests.test_confirm_sells",
          "__main__.ContractTests.test_conflict",
          "__main__.ContractTests.test_expiry_boundary",
          "__main__.ContractTests.test_idempotent",
          "__main__.ContractTests.test_no_oversell",
          "__main__.ContractTests.test_state_filter",
          "__main__.ContractTests.test_terminal_transition",
          "__main__.ContractTests.test_zero_reservation_invalid"
        ],
        "failed": []
      }
    }
  ],
  "tests_observed": 10,
  "excluded_tests": [],
  "kept_ratio": 1.0,
  "killed": 10,
  "formula": "killed × (tests_observed − excluded) / tests_observed",
  "mutants": [
    {
      "id": "M01",
      "returncode": 1,
      "timeout": false,
      "log": "M01.log",
      "tests": {
        "started": [
          "__main__.ContractTests.test_batch_rollback",
          "__main__.ContractTests.test_cancel_once",
          "__main__.ContractTests.test_confirm_sells",
          "__main__.ContractTests.test_conflict",
          "__main__.ContractTests.test_expiry_boundary",
          "__main__.ContractTests.test_idempotent",
          "__main__.ContractTests.test_no_oversell",
          "__main__.ContractTests.test_state_filter",
          "__main__.ContractTests.test_terminal_transition",
          "__main__.ContractTests.test_zero_reservation_invalid"
        ],
        "failed": [
          "__main__.ContractTests.test_batch_rollback",
          "__main__.ContractTests.test_no_oversell"
        ]
      },
      "title": "允许超卖",
      "killer": "C01",
      "killed": true
    },
    {
      "id": "M02",
      "returncode": 1,
      "timeout": false,
      "log": "M02.log",
      "tests": {
        "started": [
          "__main__.ContractTests.test_batch_rollback",
          "__main__.ContractTests.test_cancel_once",
          "__main__.ContractTests.test_confirm_sells",
          "__main__.ContractTests.test_conflict",
          "__main__.ContractTests.test_expiry_boundary",
          "__main__.ContractTests.test_idempotent",
          "__main__.ContractTests.test_no_oversell",
          "__main__.ContractTests.test_state_filter",
          "__main__.ContractTests.test_terminal_transition",
          "__main__.ContractTests.test_zero_reservation_invalid"
        ],
        "failed": [
          "__main__.ContractTests.test_conflict",
          "__main__.ContractTests.test_idempotent"
        ]
      },
      "title": "相同幂等键重复创建",
      "killer": "I01",
      "killed": true
    },
    {
      "id": "M03",
      "returncode": 1,
      "timeout": false,
      "log": "M03.log",
      "tests": {
        "started": [
          "__main__.ContractTests.test_batch_rollback",
          "__main__.ContractTests.test_cancel_once",
          "__main__.ContractTests.test_confirm_sells",
          "__main__.ContractTests.test_conflict",
          "__main__.ContractTests.test_expiry_boundary",
          "__main__.ContractTests.test_idempotent",
          "__main__.ContractTests.test_no_oversell",
          "__main__.ContractTests.test_state_filter",
          "__main__.ContractTests.test_terminal_transition",
          "__main__.ContractTests.test_zero_reservation_invalid"
        ],
        "failed": [
          "__main__.ContractTests.test_conflict"
        ]
      },
      "title": "同键异参被接受",
      "killer": "I03",
      "killed": true
    },
    {
      "id": "M04",
      "returncode": 1,
      "timeout": false,
      "log": "M04.log",
      "tests": {
        "started": [
          "__main__.ContractTests.test_batch_rollback",
          "__main__.ContractTests.test_cancel_once",
          "__main__.ContractTests.test_confirm_sells",
          "__main__.ContractTests.test_conflict",
          "__main__.ContractTests.test_expiry_boundary",
          "__main__.ContractTests.test_idempotent",
          "__main__.ContractTests.test_no_oversell",
          "__main__.ContractTests.test_state_filter",
          "__main__.ContractTests.test_terminal_transition",
          "__main__.ContractTests.test_zero_reservation_invalid"
        ],
        "failed": [
          "__main__.ContractTests.test_expiry_boundary"
        ]
      },
      "title": "到期比较使用 > 而非 >=",
      "killer": "T01",
      "killed": true
    },
    {
      "id": "M05",
      "returncode": 1,
      "timeout": false,
      "log": "M05.log",
      "tests": {
        "started": [
          "__main__.ContractTests.test_batch_rollback",
          "__main__.ContractTests.test_cancel_once",
          "__main__.ContractTests.test_confirm_sells",
          "__main__.ContractTests.test_conflict",
          "__main__.ContractTests.test_expiry_boundary",
          "__main__.ContractTests.test_idempotent",
          "__main__.ContractTests.test_no_oversell",
          "__main__.ContractTests.test_state_filter",
          "__main__.ContractTests.test_terminal_transition",
          "__main__.ContractTests.test_zero_reservation_invalid"
        ],
        "failed": [
          "__main__.ContractTests.test_cancel_once"
        ]
      },
      "title": "重复取消多次释放",
      "killer": "C04",
      "killed": true
    },
    {
      "id": "M06",
      "returncode": 1,
      "timeout": false,
      "log": "M06.log",
      "tests": {
        "started": [
          "__main__.ContractTests.test_batch_rollback",
          "__main__.ContractTests.test_cancel_once",
          "__main__.ContractTests.test_confirm_sells",
          "__main__.ContractTests.test_conflict",
          "__main__.ContractTests.test_expiry_boundary",
          "__main__.ContractTests.test_idempotent",
          "__main__.ContractTests.test_no_oversell",
          "__main__.ContractTests.test_state_filter",
          "__main__.ContractTests.test_terminal_transition",
          "__main__.ContractTests.test_zero_reservation_invalid"
        ],
        "failed": [
          "__main__.ContractTests.test_confirm_sells"
        ]
      },
      "title": "确认错误归还可用库存",
      "killer": "F03",
      "killed": true
    },
    {
      "id": "M07",
      "returncode": 1,
      "timeout": false,
      "log": "M07.log",
      "tests": {
        "started": [
          "__main__.ContractTests.test_batch_rollback",
          "__main__.ContractTests.test_cancel_once",
          "__main__.ContractTests.test_confirm_sells",
          "__main__.ContractTests.test_conflict",
          "__main__.ContractTests.test_expiry_boundary",
          "__main__.ContractTests.test_idempotent",
          "__main__.ContractTests.test_no_oversell",
          "__main__.ContractTests.test_state_filter",
          "__main__.ContractTests.test_terminal_transition",
          "__main__.ContractTests.test_zero_reservation_invalid"
        ],
        "failed": [
          "__main__.ContractTests.test_terminal_transition"
        ]
      },
      "title": "已取消记录允许确认",
      "killer": "T04",
      "killed": true
    },
    {
      "id": "M08",
      "returncode": 1,
      "timeout": false,
      "log": "M08.log",
      "tests": {
        "started": [
          "__main__.ContractTests.test_batch_rollback",
          "__main__.ContractTests.test_cancel_once",
          "__main__.ContractTests.test_confirm_sells",
          "__main__.ContractTests.test_conflict",
          "__main__.ContractTests.test_expiry_boundary",
          "__main__.ContractTests.test_idempotent",
          "__main__.ContractTests.test_no_oversell",
          "__main__.ContractTests.test_state_filter",
          "__main__.ContractTests.test_terminal_transition",
          "__main__.ContractTests.test_zero_reservation_invalid"
        ],
        "failed": [
          "__main__.ContractTests.test_batch_rollback"
        ]
      },
      "title": "多商品失败发生部分扣减",
      "killer": "F06",
      "killed": true
    },
    {
      "id": "M09",
      "returncode": 1,
      "timeout": false,
      "log": "M09.log",
      "tests": {
        "started": [
          "__main__.ContractTests.test_batch_rollback",
          "__main__.ContractTests.test_cancel_once",
          "__main__.ContractTests.test_confirm_sells",
          "__main__.ContractTests.test_conflict",
          "__main__.ContractTests.test_expiry_boundary",
          "__main__.ContractTests.test_idempotent",
          "__main__.ContractTests.test_no_oversell",
          "__main__.ContractTests.test_state_filter",
          "__main__.ContractTests.test_terminal_transition",
          "__main__.ContractTests.test_zero_reservation_invalid"
        ],
        "failed": [
          "__main__.ContractTests.test_zero_reservation_invalid"
        ]
      },
      "title": "错误接受零预占数量",
      "killer": "V02",
      "killed": true
    },
    {
      "id": "M10",
      "returncode": 1,
      "timeout": false,
      "log": "M10.log",
      "tests": {
        "started": [
          "__main__.ContractTests.test_batch_rollback",
          "__main__.ContractTests.test_cancel_once",
          "__main__.ContractTests.test_confirm_sells",
          "__main__.ContractTests.test_conflict",
          "__main__.ContractTests.test_expiry_boundary",
          "__main__.ContractTests.test_idempotent",
          "__main__.ContractTests.test_no_oversell",
          "__main__.ContractTests.test_state_filter",
          "__main__.ContractTests.test_terminal_transition",
          "__main__.ContractTests.test_zero_reservation_invalid"
        ],
        "failed": [
          "__main__.ContractTests.test_state_filter"
        ]
      },
      "title": "忽略列表状态过滤",
      "killer": "F09",
      "killed": true
    }
  ],
  "timeout_policy": "no_kill_credit"
}
```

## 失败签名聚类（启发式根因分组）

同一签名的多个失败通常来自同一个缺陷；签名对路径和数字做了掩码，只用于阅读，不参与计分。

| 用例数 | 用例 | 签名 |
|---:|---|---|
| 0 | — | 无失败 |

## 所有验收结果

| Case | 维度 | 结果 | 秒 | 说明 |
|---|---|---|---:|---|
| B01 | D01 | pass | 0.001 | ok |
| B02 | D01 | pass | 0.000 | ok |
| B03 | D01 | pass | 0.000 | ok |
| B04 | D01 | pass | 0.571 | ok |
| B05 | D01 | pass | 0.140 | ok |
| B06 | D01 | pass | 0.146 | ok |
| B07 | D01 | pass | 0.137 | ok |
| B08 | D01 | pass | 0.196 | ok |
| B09 | D01 | pass | 0.288 | ok |
| B10 | D01 | pass | 0.000 | ok |
| F01 | D02 | pass | 0.126 | ok |
| F02 | D02 | pass | 0.147 | ok |
| F03 | D02 | pass | 0.152 | ok |
| F04 | D02 | pass | 0.155 | ok |
| F05 | D02 | pass | 0.166 | ok |
| F06 | D02 | pass | 0.149 | ok |
| F07 | D02 | pass | 0.141 | ok |
| F08 | D02 | pass | 0.118 | ok |
| F09 | D02 | pass | 0.215 | ok |
| F10 | D02 | pass | 0.185 | ok |
| V01 | D03 | pass | 0.153 | ok |
| V02 | D03 | pass | 0.181 | ok |
| V03 | D03 | pass | 0.276 | ok |
| V04 | D03 | pass | 0.168 | ok |
| V05 | D03 | pass | 0.176 | ok |
| V06 | D03 | pass | 0.146 | ok |
| V07 | D03 | pass | 0.140 | ok |
| V08 | D03 | pass | 0.135 | ok |
| V09 | D03 | pass | 0.148 | ok |
| V10 | D03 | pass | 0.129 | ok |
| I01 | D04 | pass | 0.161 | ok |
| I02 | D04 | pass | 0.169 | ok |
| I03 | D04 | pass | 0.163 | ok |
| I04 | D04 | pass | 0.160 | ok |
| I05 | D04 | pass | 0.149 | ok |
| I06 | D04 | pass | 0.175 | ok |
| I07 | D04 | pass | 0.167 | ok |
| I08 | D04 | pass | 0.278 | ok |
| I09 | D04 | pass | 0.162 | ok |
| I10 | D04 | pass | 0.184 | ok |
| C01 | D05 | pass | 0.204 | ok |
| C02 | D05 | pass | 0.145 | ok |
| C03 | D05 | pass | 0.202 | ok |
| C04 | D05 | pass | 0.173 | ok |
| C05 | D05 | pass | 0.175 | ok |
| C06 | D05 | pass | 0.165 | ok |
| C07 | D05 | pass | 0.168 | ok |
| C08 | D05 | pass | 0.431 | ok |
| C09 | D05 | pass | 0.168 | ok |
| C10 | D05 | pass | 0.214 | ok |
| R01 | D06 | pass | 0.288 | ok |
| R02 | D06 | pass | 0.636 | ok |
| R03 | D06 | pass | 0.676 | ok |
| R04 | D06 | pass | 0.258 | ok |
| R05 | D06 | pass | 0.407 | ok |
| R06 | D06 | pass | 0.242 | ok |
| R07 | D06 | pass | 0.281 | ok |
| R08 | D06 | pass | 0.549 | ok |
| R09 | D06 | pass | 0.822 | ok |
| R10 | D06 | pass | 0.213 | ok |
| T01 | D07 | pass | 0.153 | ok |
| T02 | D07 | pass | 0.159 | ok |
| T03 | D07 | pass | 0.136 | ok |
| T04 | D07 | pass | 0.181 | ok |
| T05 | D07 | pass | 0.163 | ok |
| T06 | D07 | pass | 0.162 | ok |
| T07 | D07 | pass | 0.185 | ok |
| T08 | D07 | pass | 0.164 | ok |
| T09 | D07 | pass | 0.166 | ok |
| T10 | D07 | pass | 0.168 | ok |
| O01 | D08 | pass | 0.635 | ok |
| O02 | D08 | pass | 0.741 | ok |
| O03 | D08 | pass | 0.655 | ok |
| O04 | D08 | pass | 0.724 | ok |
| O05 | D08 | pass | 0.699 | ok |
| O06 | D08 | pass | 0.765 | ok |
| O07 | D08 | pass | 0.825 | ok |
| O08 | D08 | pass | 0.703 | ok |
| O09 | D08 | pass | 0.694 | ok |
| O10 | D08 | pass | 0.740 | ok |

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

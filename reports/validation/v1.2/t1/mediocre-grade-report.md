# ReserveLedger-Spring 评测报告：MEDIOCRE-CONTROL-NOT-A-MODEL

- Run ID：`mediocre-validation`
- 基准：1.2.0（赛道 `t1-spring`）；测试种子：1701
- 源码快照 SHA256：`c51d855af3858b1dae07b5b86b78a6b0d1439c3e3d2925028a95e6534eb19ae9`
- 完整且可计分：**True**
- 自动原始综合：**7.134 / 10**
- 关键错误门槛后：**7.134 / 10**
- 正式综合分：**7.1336 / 10**（null 表示诊断/基础设施无效，不是模型零分）
- Gate：`NO_CRITICAL_FAILURE_OBSERVED`；关键失败：无

| 维度 | 名称 | 分数 / 10 |
|---|---|---:|
| D01 | 构建与交付 | 9.000 |
| D02 | 业务正确性 | 9.000 |
| D03 | 协议与输入边界 | 2.000 |
| D04 | 幂等与重试 | 8.000 |
| D05 | 并发与原子性 | 10.000 |
| D06 | 持久化与故障恢复 | 8.000 |
| D07 | 时间与状态机 | 8.000 |
| D08 | 随机模型与回归 | 10.000 |
| D09 | 性能与资源 | 5.336 |
| D10 | 自测有效性 | 2.000 |

## 性能原始数据

```json
{
  "score_10": 5.3357,
  "status": "measured",
  "components_10": {
    "read_throughput": 2.6871,
    "lifecycle_throughput": 10.0,
    "read_tail_latency": 2.9702,
    "scale_growth": 9.2153,
    "memory": 9.6518,
    "sql_efficiency": 0.0264
  },
  "weights": {
    "read_throughput": 0.2,
    "lifecycle_throughput": 0.2,
    "read_tail_latency": 0.15,
    "scale_growth": 0.15,
    "memory": 0.1,
    "sql_efficiency": 0.2
  },
  "tolerance": {
    "performance": 1.2,
    "memory": 1.25
  },
  "possible_client_bottleneck": false,
  "summary": {
    "read_rps": 466.03981456353006,
    "lifecycle_ops_per_sec": 211.97923344940466,
    "read_p95_ms": 25.21816700027557,
    "read_p50_ms": 16.863417000422487,
    "read_p99_ms": 31.706000001577195,
    "lifecycle_p95_ms": 52.04312499699881,
    "growth_ratio": 1.302187339143844,
    "peak_rss_bytes": 440483840,
    "java_cpu_seconds": null,
    "sql_statements": 4994
  },
  "measurement_file": "performance/performance.json",
  "calibration_created_at_utc": "2026-10-01T06:57:37.785019+00:00",
  "reference_summary": {
    "read_rps": 2081.264898561363,
    "lifecycle_ops_per_sec": 209.37414967911945,
    "read_p95_ms": 6.241875002160668,
    "read_p50_ms": 3.5682919988175854,
    "read_p99_ms": 11.525499998242594,
    "lifecycle_p95_ms": 63.41820799934794,
    "growth_ratio": 0.8798730421152609,
    "peak_rss_bytes": 340115456,
    "java_cpu_seconds": null,
    "sql_statements": 11
  }
}
```

## 自测有效性

```json
{
  "score_10": 2.0,
  "status": "measured",
  "baseline_passed": true,
  "baseline": [
    {
      "id": "correct-1",
      "returncode": 0,
      "timeout": false,
      "log": "correct-1.log",
      "observed": [
        "contract.BasicFlowTest#reserveAndCancel",
        "contract.BasicFlowTest#reserveAndConfirm",
        "contract.BasicFlowTest#createAndGetItem",
        "contract.BasicFlowTest#retryReturnsSameReservation"
      ],
      "failed": []
    },
    {
      "id": "correct-2",
      "returncode": 0,
      "timeout": false,
      "log": "correct-2.log",
      "observed": [
        "contract.BasicFlowTest#reserveAndCancel",
        "contract.BasicFlowTest#reserveAndConfirm",
        "contract.BasicFlowTest#createAndGetItem",
        "contract.BasicFlowTest#retryReturnsSameReservation"
      ],
      "failed": []
    }
  ],
  "tests_observed": 4,
  "excluded_tests": [],
  "kept_ratio": 1.0,
  "killed": 2,
  "formula": "killed × (tests_observed − excluded) / tests_observed",
  "mutants": [
    {
      "id": "M01",
      "returncode": 0,
      "timeout": false,
      "log": "M01.log",
      "observed": [
        "contract.BasicFlowTest#reserveAndCancel",
        "contract.BasicFlowTest#reserveAndConfirm",
        "contract.BasicFlowTest#createAndGetItem",
        "contract.BasicFlowTest#retryReturnsSameReservation"
      ],
      "failed": [],
      "title": "允许超卖",
      "killer": "C01",
      "killed": false
    },
    {
      "id": "M02",
      "returncode": 1,
      "timeout": false,
      "log": "M02.log",
      "observed": [
        "contract.BasicFlowTest#reserveAndCancel",
        "contract.BasicFlowTest#reserveAndConfirm",
        "contract.BasicFlowTest#createAndGetItem",
        "contract.BasicFlowTest#retryReturnsSameReservation"
      ],
      "failed": [
        "contract.BasicFlowTest#retryReturnsSameReservation"
      ],
      "title": "相同幂等键重复创建",
      "killer": "I01",
      "killed": true
    },
    {
      "id": "M03",
      "returncode": 0,
      "timeout": false,
      "log": "M03.log",
      "observed": [
        "contract.BasicFlowTest#reserveAndCancel",
        "contract.BasicFlowTest#reserveAndConfirm",
        "contract.BasicFlowTest#createAndGetItem",
        "contract.BasicFlowTest#retryReturnsSameReservation"
      ],
      "failed": [],
      "title": "同键异参被接受",
      "killer": "I03",
      "killed": false
    },
    {
      "id": "M04",
      "returncode": 0,
      "timeout": false,
      "log": "M04.log",
      "observed": [
        "contract.BasicFlowTest#reserveAndCancel",
        "contract.BasicFlowTest#reserveAndConfirm",
        "contract.BasicFlowTest#createAndGetItem",
        "contract.BasicFlowTest#retryReturnsSameReservation"
      ],
      "failed": [],
      "title": "到期比较使用 > 而非 >=",
      "killer": "T01",
      "killed": false
    },
    {
      "id": "M05",
      "returncode": 0,
      "timeout": false,
      "log": "M05.log",
      "observed": [
        "contract.BasicFlowTest#reserveAndCancel",
        "contract.BasicFlowTest#reserveAndConfirm",
        "contract.BasicFlowTest#createAndGetItem",
        "contract.BasicFlowTest#retryReturnsSameReservation"
      ],
      "failed": [],
      "title": "重复取消多次释放",
      "killer": "C04",
      "killed": false
    },
    {
      "id": "M06",
      "returncode": 1,
      "timeout": false,
      "log": "M06.log",
      "observed": [
        "contract.BasicFlowTest#reserveAndCancel",
        "contract.BasicFlowTest#reserveAndConfirm",
        "contract.BasicFlowTest#createAndGetItem",
        "contract.BasicFlowTest#retryReturnsSameReservation"
      ],
      "failed": [
        "contract.BasicFlowTest#reserveAndConfirm"
      ],
      "title": "确认错误归还可用库存",
      "killer": "F03",
      "killed": true
    },
    {
      "id": "M07",
      "returncode": 0,
      "timeout": false,
      "log": "M07.log",
      "observed": [
        "contract.BasicFlowTest#reserveAndCancel",
        "contract.BasicFlowTest#reserveAndConfirm",
        "contract.BasicFlowTest#createAndGetItem",
        "contract.BasicFlowTest#retryReturnsSameReservation"
      ],
      "failed": [],
      "title": "已取消记录允许确认",
      "killer": "T04",
      "killed": false
    },
    {
      "id": "M08",
      "returncode": 0,
      "timeout": false,
      "log": "M08.log",
      "observed": [
        "contract.BasicFlowTest#reserveAndCancel",
        "contract.BasicFlowTest#reserveAndConfirm",
        "contract.BasicFlowTest#createAndGetItem",
        "contract.BasicFlowTest#retryReturnsSameReservation"
      ],
      "failed": [],
      "title": "多商品失败发生部分扣减",
      "killer": "F06",
      "killed": false
    },
    {
      "id": "M09",
      "returncode": 0,
      "timeout": false,
      "log": "M09.log",
      "observed": [
        "contract.BasicFlowTest#reserveAndCancel",
        "contract.BasicFlowTest#reserveAndConfirm",
        "contract.BasicFlowTest#createAndGetItem",
        "contract.BasicFlowTest#retryReturnsSameReservation"
      ],
      "failed": [],
      "title": "错误接受零预占数量",
      "killer": "V02",
      "killed": false
    },
    {
      "id": "M10",
      "returncode": 0,
      "timeout": false,
      "log": "M10.log",
      "observed": [
        "contract.BasicFlowTest#reserveAndCancel",
        "contract.BasicFlowTest#reserveAndConfirm",
        "contract.BasicFlowTest#createAndGetItem",
        "contract.BasicFlowTest#retryReturnsSameReservation"
      ],
      "failed": [],
      "title": "忽略列表状态过滤",
      "killer": "F09",
      "killed": false
    }
  ],
  "timeout_policy": "no_kill_credit"
}
```

## 失败签名聚类（启发式根因分组）

同一签名的多个失败通常来自同一个缺陷；签名对路径和数字做了掩码，只用于阅读，不参与计分。

| 用例数 | 用例 | 签名 |
|---:|---|---|
| 2 | V03, V04 | POST /v#/reservations: expected #, got #: {'id': '#', 'requestId': 'bad', 'state': 'ACTIVE', 'createdAt': #, 'expiresAt': #, 'lines': [{'sku': 'A', 'quantity': #}]} |
| 1 | B04 | hibernate.hbm#ddl.auto must be none or validate: {'flywayMigrations': #, 'ddlAuto': 'update'} |
| 1 | F09 | : expected None, got '#' |
| 1 | I07 | : expected 'EXPIRED', got 'ACTIVE' |
| 1 | I09 | POST /v#/reservations: expected #, got #: {'id': '#', 'requestId': '#', 'state': 'ACTIVE', 'createdAt': #, 'expiresAt': #, 'lines': [{'sku': 'A', 'quantity': #}]} |
| 1 | R05 | : expected #, got # |
| 1 | R10 | invalid startup did not fail within # seconds |
| 1 | T08 | POST /v#/reservations: expected #, got #: {'error': {'code': 'INSUFFICIENT_STOCK', 'message': 'not enough'}} |
| 1 | T10 | POST /__test__/advance: expected #, got #: {'now': #} |
| 1 | V01 | POST /v#/items: expected #, got #: {'sku': '#', 'total': #, 'available': #, 'held': #, 'sold': #} |
| 1 | V02 | POST /v#/items: expected #, got #: {'sku': 'A', 'total': #, 'available': #, 'held': #, 'sold': #} |
| 1 | V05 | POST /v#/items: expected #, got #: {'sku': 'B', 'total': #, 'available': #, 'held': #, 'sold': #} |
| 1 | V06 | GET /v#/reservations?limit=#&limit=#: expected #, got #: {'items': [], 'nextAfter': None} |
| 1 | V07 | missing error envelope |
| 1 | V09 | POST /v#/items: expected #, got #: {'error': {'message': 'invalid request', 'code': 'INVALID_REQUEST'}} |

## 所有验收结果

| Case | 维度 | 结果 | 秒 | 说明 |
|---|---|---|---:|---|
| B01 | D01 | pass | 0.001 | ok |
| B02 | D01 | pass | 0.002 | ok |
| B03 | D01 | pass | 0.000 | ok |
| B04 | D01 | fail | 2.965 | hibernate.hbm2ddl.auto must be none or validate: {'flywayMigrations': 1, 'ddlAuto': 'update'} |
| B05 | D01 | pass | 3.254 | ok |
| B06 | D01 | pass | 3.868 | ok |
| B07 | D01 | pass | 3.012 | ok |
| B08 | D01 | pass | 5.372 | ok |
| B09 | D01 | pass | 7.679 | ok |
| B10 | D01 | pass | 0.001 | ok |
| F01 | D02 | pass | 3.604 | ok |
| F02 | D02 | pass | 3.225 | ok |
| F03 | D02 | pass | 3.278 | ok |
| F04 | D02 | pass | 3.329 | ok |
| F05 | D02 | pass | 3.316 | ok |
| F06 | D02 | pass | 3.149 | ok |
| F07 | D02 | pass | 3.217 | ok |
| F08 | D02 | pass | 3.084 | ok |
| F09 | D02 | fail | 3.467 | : expected None, got '5' |
| F10 | D02 | pass | 3.436 | ok |
| V01 | D03 | fail | 3.515 | POST /v1/items: expected 400, got 201: {'sku': '42', 'total': 1, 'available': 1, 'held': 0, 'sold': 0} |
| V02 | D03 | fail | 3.438 | POST /v1/items: expected 400, got 201: {'sku': 'A', 'total': 1, 'available': 1, 'held': 0, 'sold': 0} |
| V03 | D03 | fail | 3.116 | POST /v1/reservations: expected 400, got 201: {'id': '1', 'requestId': 'bad', 'state': 'ACTIVE', 'createdAt': 1700000000000, 'expiresAt': 1700000000010, 'lines': [{'sku': 'A', 'quantity': 1}]} |
| V04 | D03 | fail | 3.087 | POST /v1/reservations: expected 400, got 201: {'id': '1', 'requestId': 'bad', 'state': 'ACTIVE', 'createdAt': 1700000000000, 'expiresAt': 1700000001000, 'lines': [{'sku': 'A', 'quantity': 1}]} |
| V05 | D03 | fail | 3.155 | POST /v1/items: expected 400, got 201: {'sku': 'B', 'total': 1, 'available': 1, 'held': 0, 'sold': 0} |
| V06 | D03 | fail | 2.859 | GET /v1/reservations?limit=1&limit=2: expected 400, got 200: {'items': [], 'nextAfter': None} |
| V07 | D03 | fail | 2.889 | missing error envelope |
| V08 | D03 | pass | 3.085 | ok |
| V09 | D03 | fail | 2.879 | POST /v1/items: expected 413, got 400: {'error': {'message': 'invalid request', 'code': 'INVALID_REQUEST'}} |
| V10 | D03 | pass | 2.993 | ok |
| I01 | D04 | pass | 3.076 | ok |
| I02 | D04 | pass | 3.091 | ok |
| I03 | D04 | pass | 3.182 | ok |
| I04 | D04 | pass | 3.271 | ok |
| I05 | D04 | pass | 3.075 | ok |
| I06 | D04 | pass | 3.179 | ok |
| I07 | D04 | fail | 3.164 | : expected 'EXPIRED', got 'ACTIVE' |
| I08 | D04 | pass | 5.621 | ok |
| I09 | D04 | fail | 3.021 | POST /v1/reservations: expected 400, got 201: {'id': '1', 'requestId': '1', 'state': 'ACTIVE', 'createdAt': 1700000000000, 'expiresAt': 1700000010000, 'lines': [{'sku': 'A', 'quantity': 1}]} |
| I10 | D04 | pass | 3.129 | ok |
| C01 | D05 | pass | 3.150 | ok |
| C02 | D05 | pass | 3.107 | ok |
| C03 | D05 | pass | 3.181 | ok |
| C04 | D05 | pass | 3.182 | ok |
| C05 | D05 | pass | 3.410 | ok |
| C06 | D05 | pass | 3.206 | ok |
| C07 | D05 | pass | 3.102 | ok |
| C08 | D05 | pass | 3.349 | ok |
| C09 | D05 | pass | 3.162 | ok |
| C10 | D05 | pass | 3.139 | ok |
| R01 | D06 | pass | 6.253 | ok |
| R02 | D06 | pass | 11.927 | ok |
| R03 | D06 | pass | 13.159 | ok |
| R04 | D06 | pass | 5.547 | ok |
| R05 | D06 | fail | 8.322 | : expected 1700000020000, got 1700000000003 |
| R06 | D06 | pass | 5.718 | ok |
| R07 | D06 | pass | 5.610 | ok |
| R08 | D06 | pass | 11.566 | ok |
| R09 | D06 | pass | 8.657 | ok |
| R10 | D06 | fail | 90.406 | invalid startup did not fail within 90 seconds |
| T01 | D07 | pass | 3.165 | ok |
| T02 | D07 | pass | 3.142 | ok |
| T03 | D07 | pass | 3.166 | ok |
| T04 | D07 | pass | 3.156 | ok |
| T05 | D07 | pass | 3.152 | ok |
| T06 | D07 | pass | 3.046 | ok |
| T07 | D07 | pass | 3.102 | ok |
| T08 | D07 | fail | 3.251 | POST /v1/reservations: expected 201, got 409: {'error': {'code': 'INSUFFICIENT_STOCK', 'message': 'not enough'}} |
| T09 | D07 | pass | 3.224 | ok |
| T10 | D07 | fail | 2.985 | POST /__test__/advance: expected 400, got 200: {'now': 1700000000001} |
| O01 | D08 | pass | 6.831 | ok |
| O02 | D08 | pass | 6.708 | ok |
| O03 | D08 | pass | 6.461 | ok |
| O04 | D08 | pass | 6.512 | ok |
| O05 | D08 | pass | 6.531 | ok |
| O06 | D08 | pass | 6.964 | ok |
| O07 | D08 | pass | 6.798 | ok |
| O08 | D08 | pass | 6.501 | ok |
| O09 | D08 | pass | 6.410 | ok |
| O10 | D08 | pass | 6.649 | ok |

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

模型生成用时、token 和成本只接受真实采集或操作者提供的记录；本报告的 evaluation_seconds 是裁判耗时，不是模型开发耗时。
人工维护性/文档分独立存于 manual_review；未填写时不伪造人工分。一个题目的得分不代表模型通用编程能力，也不构成生产上线认证。

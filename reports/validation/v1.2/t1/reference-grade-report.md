# ReserveLedger-Spring 评测报告：REFERENCE-SELF-CHECK-NOT-A-MODEL

- Run ID：`reference-validation`
- 基准：1.2.0（赛道 `t1-spring`）；测试种子：1701
- 源码快照 SHA256：`20a4edaaee15c548f2c5e85a45a66c4a6fd682058c091c6fe2cc92512a4214c9`
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
    "memory": 10.0,
    "sql_efficiency": 10.0
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
    "read_rps": 2097.4012150199433,
    "lifecycle_ops_per_sec": 192.05773263126704,
    "read_p95_ms": 6.356500001857057,
    "read_p50_ms": 3.4909159985545557,
    "read_p99_ms": 11.760166999010835,
    "lifecycle_p95_ms": 64.77774999802932,
    "growth_ratio": 0.9329051712650847,
    "peak_rss_bytes": 337838080,
    "java_cpu_seconds": null,
    "sql_statements": 11
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
  "score_10": 10.0,
  "status": "measured",
  "baseline_passed": true,
  "baseline": [
    {
      "id": "correct-1",
      "returncode": 0,
      "timeout": false,
      "log": "correct-1.log",
      "observed": [
        "contract.ReserveLedgerContractTest#cancelledReservationCannotBeConfirmed",
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts",
        "contract.ReserveLedgerContractTest#sameKeyRetryReturnsOriginalWithoutHoldingAgain",
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing",
        "contract.ReserveLedgerContractTest#zeroQuantityLineIsRejected",
        "contract.ReserveLedgerContractTest#confirmMovesHeldToSold",
        "contract.ReserveLedgerContractTest#concurrentReservationsNeverOversell",
        "contract.ReserveLedgerContractTest#repeatedCancelReleasesOnce",
        "contract.ReserveLedgerContractTest#reservationExpiresExactlyAtDeadline",
        "contract.ReserveLedgerContractTest#listFiltersByState",
        "contract.ReserveLedgerContractTest#errorsUseTheJsonEnvelope"
      ],
      "failed": []
    },
    {
      "id": "correct-2",
      "returncode": 0,
      "timeout": false,
      "log": "correct-2.log",
      "observed": [
        "contract.ReserveLedgerContractTest#cancelledReservationCannotBeConfirmed",
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts",
        "contract.ReserveLedgerContractTest#sameKeyRetryReturnsOriginalWithoutHoldingAgain",
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing",
        "contract.ReserveLedgerContractTest#zeroQuantityLineIsRejected",
        "contract.ReserveLedgerContractTest#confirmMovesHeldToSold",
        "contract.ReserveLedgerContractTest#concurrentReservationsNeverOversell",
        "contract.ReserveLedgerContractTest#repeatedCancelReleasesOnce",
        "contract.ReserveLedgerContractTest#reservationExpiresExactlyAtDeadline",
        "contract.ReserveLedgerContractTest#listFiltersByState",
        "contract.ReserveLedgerContractTest#errorsUseTheJsonEnvelope"
      ],
      "failed": []
    }
  ],
  "tests_observed": 11,
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
      "observed": [
        "contract.ReserveLedgerContractTest#cancelledReservationCannotBeConfirmed",
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts",
        "contract.ReserveLedgerContractTest#sameKeyRetryReturnsOriginalWithoutHoldingAgain",
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing",
        "contract.ReserveLedgerContractTest#zeroQuantityLineIsRejected",
        "contract.ReserveLedgerContractTest#confirmMovesHeldToSold",
        "contract.ReserveLedgerContractTest#concurrentReservationsNeverOversell",
        "contract.ReserveLedgerContractTest#repeatedCancelReleasesOnce",
        "contract.ReserveLedgerContractTest#reservationExpiresExactlyAtDeadline",
        "contract.ReserveLedgerContractTest#listFiltersByState",
        "contract.ReserveLedgerContractTest#errorsUseTheJsonEnvelope"
      ],
      "failed": [
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing",
        "contract.ReserveLedgerContractTest#concurrentReservationsNeverOversell"
      ],
      "title": "允许超卖",
      "killer": "C01",
      "killed": true
    },
    {
      "id": "M02",
      "returncode": 1,
      "timeout": false,
      "log": "M02.log",
      "observed": [
        "contract.ReserveLedgerContractTest#cancelledReservationCannotBeConfirmed",
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts",
        "contract.ReserveLedgerContractTest#sameKeyRetryReturnsOriginalWithoutHoldingAgain",
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing",
        "contract.ReserveLedgerContractTest#zeroQuantityLineIsRejected",
        "contract.ReserveLedgerContractTest#confirmMovesHeldToSold",
        "contract.ReserveLedgerContractTest#concurrentReservationsNeverOversell",
        "contract.ReserveLedgerContractTest#repeatedCancelReleasesOnce",
        "contract.ReserveLedgerContractTest#reservationExpiresExactlyAtDeadline",
        "contract.ReserveLedgerContractTest#listFiltersByState",
        "contract.ReserveLedgerContractTest#errorsUseTheJsonEnvelope"
      ],
      "failed": [
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts",
        "contract.ReserveLedgerContractTest#sameKeyRetryReturnsOriginalWithoutHoldingAgain"
      ],
      "title": "相同幂等键重复创建",
      "killer": "I01",
      "killed": true
    },
    {
      "id": "M03",
      "returncode": 1,
      "timeout": false,
      "log": "M03.log",
      "observed": [
        "contract.ReserveLedgerContractTest#cancelledReservationCannotBeConfirmed",
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts",
        "contract.ReserveLedgerContractTest#sameKeyRetryReturnsOriginalWithoutHoldingAgain",
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing",
        "contract.ReserveLedgerContractTest#zeroQuantityLineIsRejected",
        "contract.ReserveLedgerContractTest#confirmMovesHeldToSold",
        "contract.ReserveLedgerContractTest#concurrentReservationsNeverOversell",
        "contract.ReserveLedgerContractTest#repeatedCancelReleasesOnce",
        "contract.ReserveLedgerContractTest#reservationExpiresExactlyAtDeadline",
        "contract.ReserveLedgerContractTest#listFiltersByState",
        "contract.ReserveLedgerContractTest#errorsUseTheJsonEnvelope"
      ],
      "failed": [
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts"
      ],
      "title": "同键异参被接受",
      "killer": "I03",
      "killed": true
    },
    {
      "id": "M04",
      "returncode": 1,
      "timeout": false,
      "log": "M04.log",
      "observed": [
        "contract.ReserveLedgerContractTest#cancelledReservationCannotBeConfirmed",
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts",
        "contract.ReserveLedgerContractTest#sameKeyRetryReturnsOriginalWithoutHoldingAgain",
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing",
        "contract.ReserveLedgerContractTest#zeroQuantityLineIsRejected",
        "contract.ReserveLedgerContractTest#confirmMovesHeldToSold",
        "contract.ReserveLedgerContractTest#concurrentReservationsNeverOversell",
        "contract.ReserveLedgerContractTest#repeatedCancelReleasesOnce",
        "contract.ReserveLedgerContractTest#reservationExpiresExactlyAtDeadline",
        "contract.ReserveLedgerContractTest#listFiltersByState",
        "contract.ReserveLedgerContractTest#errorsUseTheJsonEnvelope"
      ],
      "failed": [
        "contract.ReserveLedgerContractTest#reservationExpiresExactlyAtDeadline"
      ],
      "title": "到期比较使用 > 而非 >=",
      "killer": "T01",
      "killed": true
    },
    {
      "id": "M05",
      "returncode": 1,
      "timeout": false,
      "log": "M05.log",
      "observed": [
        "contract.ReserveLedgerContractTest#cancelledReservationCannotBeConfirmed",
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts",
        "contract.ReserveLedgerContractTest#sameKeyRetryReturnsOriginalWithoutHoldingAgain",
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing",
        "contract.ReserveLedgerContractTest#zeroQuantityLineIsRejected",
        "contract.ReserveLedgerContractTest#confirmMovesHeldToSold",
        "contract.ReserveLedgerContractTest#concurrentReservationsNeverOversell",
        "contract.ReserveLedgerContractTest#repeatedCancelReleasesOnce",
        "contract.ReserveLedgerContractTest#reservationExpiresExactlyAtDeadline",
        "contract.ReserveLedgerContractTest#listFiltersByState",
        "contract.ReserveLedgerContractTest#errorsUseTheJsonEnvelope"
      ],
      "failed": [
        "contract.ReserveLedgerContractTest#repeatedCancelReleasesOnce"
      ],
      "title": "重复取消多次释放",
      "killer": "C04",
      "killed": true
    },
    {
      "id": "M06",
      "returncode": 1,
      "timeout": false,
      "log": "M06.log",
      "observed": [
        "contract.ReserveLedgerContractTest#cancelledReservationCannotBeConfirmed",
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts",
        "contract.ReserveLedgerContractTest#sameKeyRetryReturnsOriginalWithoutHoldingAgain",
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing",
        "contract.ReserveLedgerContractTest#zeroQuantityLineIsRejected",
        "contract.ReserveLedgerContractTest#confirmMovesHeldToSold",
        "contract.ReserveLedgerContractTest#concurrentReservationsNeverOversell",
        "contract.ReserveLedgerContractTest#repeatedCancelReleasesOnce",
        "contract.ReserveLedgerContractTest#reservationExpiresExactlyAtDeadline",
        "contract.ReserveLedgerContractTest#listFiltersByState",
        "contract.ReserveLedgerContractTest#errorsUseTheJsonEnvelope"
      ],
      "failed": [
        "contract.ReserveLedgerContractTest#confirmMovesHeldToSold"
      ],
      "title": "确认错误归还可用库存",
      "killer": "F03",
      "killed": true
    },
    {
      "id": "M07",
      "returncode": 1,
      "timeout": false,
      "log": "M07.log",
      "observed": [
        "contract.ReserveLedgerContractTest#cancelledReservationCannotBeConfirmed",
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts",
        "contract.ReserveLedgerContractTest#sameKeyRetryReturnsOriginalWithoutHoldingAgain",
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing",
        "contract.ReserveLedgerContractTest#zeroQuantityLineIsRejected",
        "contract.ReserveLedgerContractTest#confirmMovesHeldToSold",
        "contract.ReserveLedgerContractTest#concurrentReservationsNeverOversell",
        "contract.ReserveLedgerContractTest#repeatedCancelReleasesOnce",
        "contract.ReserveLedgerContractTest#reservationExpiresExactlyAtDeadline",
        "contract.ReserveLedgerContractTest#listFiltersByState",
        "contract.ReserveLedgerContractTest#errorsUseTheJsonEnvelope"
      ],
      "failed": [
        "contract.ReserveLedgerContractTest#cancelledReservationCannotBeConfirmed"
      ],
      "title": "已取消记录允许确认",
      "killer": "T04",
      "killed": true
    },
    {
      "id": "M08",
      "returncode": 1,
      "timeout": false,
      "log": "M08.log",
      "observed": [
        "contract.ReserveLedgerContractTest#cancelledReservationCannotBeConfirmed",
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts",
        "contract.ReserveLedgerContractTest#sameKeyRetryReturnsOriginalWithoutHoldingAgain",
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing",
        "contract.ReserveLedgerContractTest#zeroQuantityLineIsRejected",
        "contract.ReserveLedgerContractTest#confirmMovesHeldToSold",
        "contract.ReserveLedgerContractTest#concurrentReservationsNeverOversell",
        "contract.ReserveLedgerContractTest#repeatedCancelReleasesOnce",
        "contract.ReserveLedgerContractTest#reservationExpiresExactlyAtDeadline",
        "contract.ReserveLedgerContractTest#listFiltersByState",
        "contract.ReserveLedgerContractTest#errorsUseTheJsonEnvelope"
      ],
      "failed": [
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing"
      ],
      "title": "多商品失败发生部分扣减",
      "killer": "F06",
      "killed": true
    },
    {
      "id": "M09",
      "returncode": 1,
      "timeout": false,
      "log": "M09.log",
      "observed": [
        "contract.ReserveLedgerContractTest#cancelledReservationCannotBeConfirmed",
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts",
        "contract.ReserveLedgerContractTest#sameKeyRetryReturnsOriginalWithoutHoldingAgain",
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing",
        "contract.ReserveLedgerContractTest#zeroQuantityLineIsRejected",
        "contract.ReserveLedgerContractTest#confirmMovesHeldToSold",
        "contract.ReserveLedgerContractTest#concurrentReservationsNeverOversell",
        "contract.ReserveLedgerContractTest#repeatedCancelReleasesOnce",
        "contract.ReserveLedgerContractTest#reservationExpiresExactlyAtDeadline",
        "contract.ReserveLedgerContractTest#listFiltersByState",
        "contract.ReserveLedgerContractTest#errorsUseTheJsonEnvelope"
      ],
      "failed": [
        "contract.ReserveLedgerContractTest#zeroQuantityLineIsRejected"
      ],
      "title": "错误接受零预占数量",
      "killer": "V02",
      "killed": true
    },
    {
      "id": "M10",
      "returncode": 1,
      "timeout": false,
      "log": "M10.log",
      "observed": [
        "contract.ReserveLedgerContractTest#cancelledReservationCannotBeConfirmed",
        "contract.ReserveLedgerContractTest#sameKeyDifferentPayloadConflicts",
        "contract.ReserveLedgerContractTest#sameKeyRetryReturnsOriginalWithoutHoldingAgain",
        "contract.ReserveLedgerContractTest#multiSkuFailureChangesNothing",
        "contract.ReserveLedgerContractTest#zeroQuantityLineIsRejected",
        "contract.ReserveLedgerContractTest#confirmMovesHeldToSold",
        "contract.ReserveLedgerContractTest#concurrentReservationsNeverOversell",
        "contract.ReserveLedgerContractTest#repeatedCancelReleasesOnce",
        "contract.ReserveLedgerContractTest#reservationExpiresExactlyAtDeadline",
        "contract.ReserveLedgerContractTest#listFiltersByState",
        "contract.ReserveLedgerContractTest#errorsUseTheJsonEnvelope"
      ],
      "failed": [
        "contract.ReserveLedgerContractTest#listFiltersByState"
      ],
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
| B01 | D01 | pass | 0.002 | ok |
| B02 | D01 | pass | 0.002 | ok |
| B03 | D01 | pass | 0.000 | ok |
| B04 | D01 | pass | 3.172 | ok |
| B05 | D01 | pass | 3.047 | ok |
| B06 | D01 | pass | 3.215 | ok |
| B07 | D01 | pass | 3.236 | ok |
| B08 | D01 | pass | 6.456 | ok |
| B09 | D01 | pass | 7.417 | ok |
| B10 | D01 | pass | 0.001 | ok |
| F01 | D02 | pass | 3.215 | ok |
| F02 | D02 | pass | 3.520 | ok |
| F03 | D02 | pass | 3.754 | ok |
| F04 | D02 | pass | 3.503 | ok |
| F05 | D02 | pass | 3.535 | ok |
| F06 | D02 | pass | 3.463 | ok |
| F07 | D02 | pass | 3.427 | ok |
| F08 | D02 | pass | 3.218 | ok |
| F09 | D02 | pass | 3.564 | ok |
| F10 | D02 | pass | 3.679 | ok |
| V01 | D03 | pass | 3.415 | ok |
| V02 | D03 | pass | 3.354 | ok |
| V03 | D03 | pass | 3.511 | ok |
| V04 | D03 | pass | 3.307 | ok |
| V05 | D03 | pass | 3.313 | ok |
| V06 | D03 | pass | 3.321 | ok |
| V07 | D03 | pass | 3.129 | ok |
| V08 | D03 | pass | 3.292 | ok |
| V09 | D03 | pass | 3.236 | ok |
| V10 | D03 | pass | 3.251 | ok |
| I01 | D04 | pass | 3.293 | ok |
| I02 | D04 | pass | 3.257 | ok |
| I03 | D04 | pass | 3.556 | ok |
| I04 | D04 | pass | 3.390 | ok |
| I05 | D04 | pass | 3.249 | ok |
| I06 | D04 | pass | 3.412 | ok |
| I07 | D04 | pass | 3.363 | ok |
| I08 | D04 | pass | 6.124 | ok |
| I09 | D04 | pass | 3.257 | ok |
| I10 | D04 | pass | 3.349 | ok |
| C01 | D05 | pass | 3.360 | ok |
| C02 | D05 | pass | 3.417 | ok |
| C03 | D05 | pass | 3.316 | ok |
| C04 | D05 | pass | 3.228 | ok |
| C05 | D05 | pass | 3.303 | ok |
| C06 | D05 | pass | 3.469 | ok |
| C07 | D05 | pass | 3.310 | ok |
| C08 | D05 | pass | 3.406 | ok |
| C09 | D05 | pass | 3.358 | ok |
| C10 | D05 | pass | 3.255 | ok |
| R01 | D06 | pass | 6.951 | ok |
| R02 | D06 | pass | 12.074 | ok |
| R03 | D06 | pass | 13.938 | ok |
| R04 | D06 | pass | 5.873 | ok |
| R05 | D06 | pass | 9.202 | ok |
| R06 | D06 | pass | 5.842 | ok |
| R07 | D06 | pass | 5.865 | ok |
| R08 | D06 | pass | 11.800 | ok |
| R09 | D06 | pass | 8.884 | ok |
| R10 | D06 | pass | 16.355 | ok |
| T01 | D07 | pass | 3.332 | ok |
| T02 | D07 | pass | 3.246 | ok |
| T03 | D07 | pass | 3.322 | ok |
| T04 | D07 | pass | 3.352 | ok |
| T05 | D07 | pass | 3.356 | ok |
| T06 | D07 | pass | 3.305 | ok |
| T07 | D07 | pass | 3.438 | ok |
| T08 | D07 | pass | 3.448 | ok |
| T09 | D07 | pass | 3.334 | ok |
| T10 | D07 | pass | 3.179 | ok |
| O01 | D08 | pass | 6.833 | ok |
| O02 | D08 | pass | 6.904 | ok |
| O03 | D08 | pass | 6.886 | ok |
| O04 | D08 | pass | 7.094 | ok |
| O05 | D08 | pass | 7.090 | ok |
| O06 | D08 | pass | 6.891 | ok |
| O07 | D08 | pass | 7.231 | ok |
| O08 | D08 | pass | 7.079 | ok |
| O09 | D08 | pass | 7.141 | ok |
| O10 | D08 | pass | 7.720 | ok |

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

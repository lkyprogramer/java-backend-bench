# ReserveLedger 评测报告：NEGATIVE-CONTROL-NOT-A-MODEL

- Run ID：`compile-failure-control`
- 基准：1.0.0；测试种子：1701
- 源码快照 SHA256：`9b9bb8732cbdc8975eecf60bcc4c29a0f53288ae0a82d1f3a15a536871dece7e`
- 完整且可计分：**True**
- 自动原始综合：**0.300 / 10**
- 关键错误门槛后：**0.000 / 10**
- 正式综合分：**0.0 / 10**（null 表示诊断/基础设施无效，不是模型零分）
- Gate：`NOT_BUILDABLE`；关键失败：F06, C01, C02, C03, C04, C05, C06, C07, R02, R03, R04, R06, R07, R08, R10, T03

| 维度 | 名称 | 分数 / 10 |
|---|---|---:|
| D01 | 构建与交付 | 3.000 |
| D02 | 业务正确性 | 0.000 |
| D03 | 协议与输入边界 | 0.000 |
| D04 | 幂等与重试 | 0.000 |
| D05 | 并发与原子性 | 0.000 |
| D06 | 持久化与故障恢复 | 0.000 |
| D07 | 时间与状态机 | 0.000 |
| D08 | 随机模型与回归 | 0.000 |
| D09 | 性能与资源 | 0.000 |
| D10 | 自测有效性 | 0.000 |

## 性能原始数据

```json
{
  "score_10": 0,
  "status": "not_run",
  "reason": "build/startup prerequisite failed"
}
```

## 自测有效性

```json
{
  "score_10": 0,
  "status": "not_run",
  "reason": "build/startup prerequisite failed"
}
```

## 所有验收结果

| Case | 维度 | 结果 | 秒 | 说明 |
|---|---|---|---:|---|
| B01 | D01 | pass | 0.001 | ok |
| B02 | D01 | pass | 0.001 | ok |
| B03 | D01 | not_run | 0.000 | build prerequisite failed; counted as zero |
| B04 | D01 | not_run | 0.000 | build prerequisite failed; counted as zero |
| B05 | D01 | not_run | 0.000 | build prerequisite failed; counted as zero |
| B06 | D01 | not_run | 0.000 | build prerequisite failed; counted as zero |
| B07 | D01 | not_run | 0.000 | build prerequisite failed; counted as zero |
| B08 | D01 | not_run | 0.000 | build prerequisite failed; counted as zero |
| B09 | D01 | not_run | 0.000 | build prerequisite failed; counted as zero |
| B10 | D01 | pass | 0.000 | ok |
| F01 | D02 | not_run | 0.000 | build prerequisite failed; counted as zero |
| F02 | D02 | not_run | 0.000 | build prerequisite failed; counted as zero |
| F03 | D02 | not_run | 0.000 | build prerequisite failed; counted as zero |
| F04 | D02 | not_run | 0.000 | build prerequisite failed; counted as zero |
| F05 | D02 | not_run | 0.000 | build prerequisite failed; counted as zero |
| F06 | D02 | not_run | 0.000 | build prerequisite failed; counted as zero |
| F07 | D02 | not_run | 0.000 | build prerequisite failed; counted as zero |
| F08 | D02 | not_run | 0.000 | build prerequisite failed; counted as zero |
| F09 | D02 | not_run | 0.000 | build prerequisite failed; counted as zero |
| F10 | D02 | not_run | 0.000 | build prerequisite failed; counted as zero |
| V01 | D03 | not_run | 0.000 | build prerequisite failed; counted as zero |
| V02 | D03 | not_run | 0.000 | build prerequisite failed; counted as zero |
| V03 | D03 | not_run | 0.000 | build prerequisite failed; counted as zero |
| V04 | D03 | not_run | 0.000 | build prerequisite failed; counted as zero |
| V05 | D03 | not_run | 0.000 | build prerequisite failed; counted as zero |
| V06 | D03 | not_run | 0.000 | build prerequisite failed; counted as zero |
| V07 | D03 | not_run | 0.000 | build prerequisite failed; counted as zero |
| V08 | D03 | not_run | 0.000 | build prerequisite failed; counted as zero |
| V09 | D03 | not_run | 0.000 | build prerequisite failed; counted as zero |
| V10 | D03 | not_run | 0.000 | build prerequisite failed; counted as zero |
| I01 | D04 | not_run | 0.000 | build prerequisite failed; counted as zero |
| I02 | D04 | not_run | 0.000 | build prerequisite failed; counted as zero |
| I03 | D04 | not_run | 0.000 | build prerequisite failed; counted as zero |
| I04 | D04 | not_run | 0.000 | build prerequisite failed; counted as zero |
| I05 | D04 | not_run | 0.000 | build prerequisite failed; counted as zero |
| I06 | D04 | not_run | 0.000 | build prerequisite failed; counted as zero |
| I07 | D04 | not_run | 0.000 | build prerequisite failed; counted as zero |
| I08 | D04 | not_run | 0.000 | build prerequisite failed; counted as zero |
| I09 | D04 | not_run | 0.000 | build prerequisite failed; counted as zero |
| I10 | D04 | not_run | 0.000 | build prerequisite failed; counted as zero |
| C01 | D05 | not_run | 0.000 | build prerequisite failed; counted as zero |
| C02 | D05 | not_run | 0.000 | build prerequisite failed; counted as zero |
| C03 | D05 | not_run | 0.000 | build prerequisite failed; counted as zero |
| C04 | D05 | not_run | 0.000 | build prerequisite failed; counted as zero |
| C05 | D05 | not_run | 0.000 | build prerequisite failed; counted as zero |
| C06 | D05 | not_run | 0.000 | build prerequisite failed; counted as zero |
| C07 | D05 | not_run | 0.000 | build prerequisite failed; counted as zero |
| C08 | D05 | not_run | 0.000 | build prerequisite failed; counted as zero |
| C09 | D05 | not_run | 0.000 | build prerequisite failed; counted as zero |
| C10 | D05 | not_run | 0.000 | build prerequisite failed; counted as zero |
| R01 | D06 | not_run | 0.000 | build prerequisite failed; counted as zero |
| R02 | D06 | not_run | 0.000 | build prerequisite failed; counted as zero |
| R03 | D06 | not_run | 0.000 | build prerequisite failed; counted as zero |
| R04 | D06 | not_run | 0.000 | build prerequisite failed; counted as zero |
| R05 | D06 | not_run | 0.000 | build prerequisite failed; counted as zero |
| R06 | D06 | not_run | 0.000 | build prerequisite failed; counted as zero |
| R07 | D06 | not_run | 0.000 | build prerequisite failed; counted as zero |
| R08 | D06 | not_run | 0.000 | build prerequisite failed; counted as zero |
| R09 | D06 | not_run | 0.000 | build prerequisite failed; counted as zero |
| R10 | D06 | not_run | 0.000 | build prerequisite failed; counted as zero |
| T01 | D07 | not_run | 0.000 | build prerequisite failed; counted as zero |
| T02 | D07 | not_run | 0.000 | build prerequisite failed; counted as zero |
| T03 | D07 | not_run | 0.000 | build prerequisite failed; counted as zero |
| T04 | D07 | not_run | 0.000 | build prerequisite failed; counted as zero |
| T05 | D07 | not_run | 0.000 | build prerequisite failed; counted as zero |
| T06 | D07 | not_run | 0.000 | build prerequisite failed; counted as zero |
| T07 | D07 | not_run | 0.000 | build prerequisite failed; counted as zero |
| T08 | D07 | not_run | 0.000 | build prerequisite failed; counted as zero |
| T09 | D07 | not_run | 0.000 | build prerequisite failed; counted as zero |
| T10 | D07 | not_run | 0.000 | build prerequisite failed; counted as zero |
| O01 | D08 | not_run | 0.000 | build prerequisite failed; counted as zero |
| O02 | D08 | not_run | 0.000 | build prerequisite failed; counted as zero |
| O03 | D08 | not_run | 0.000 | build prerequisite failed; counted as zero |
| O04 | D08 | not_run | 0.000 | build prerequisite failed; counted as zero |
| O05 | D08 | not_run | 0.000 | build prerequisite failed; counted as zero |
| O06 | D08 | not_run | 0.000 | build prerequisite failed; counted as zero |
| O07 | D08 | not_run | 0.000 | build prerequisite failed; counted as zero |
| O08 | D08 | not_run | 0.000 | build prerequisite failed; counted as zero |
| O09 | D08 | not_run | 0.000 | build prerequisite failed; counted as zero |
| O10 | D08 | not_run | 0.000 | build prerequisite failed; counted as zero |

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

模型生成用时、token 和成本只接受真实采集或操作者提供的记录；本报告的 evaluation_seconds 是裁判耗时，不是模型开发耗时。
人工维护性/文档分独立存于 manual_review；未填写时不伪造人工分。一个题目的得分不代表模型通用编程能力，也不构成生产上线认证。

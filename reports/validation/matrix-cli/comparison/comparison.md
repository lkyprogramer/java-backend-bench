# 模型编程评测对比

| 模型 | 运行数 | 不同源码数 | 平均 / 10 | 中位数 / 10 | 范围 | 无关键失败比例 |
|---|---:|---:|---:|---:|---|---:|
| NEGATIVE-CONTROL-NOT-A-MODEL / f8152f90b4dd | 1 | 1 | 0.000 | 0.000 | 0.00–0.00 | 0.0% |

## 判读限制
跨组运行条件完全相同：True。不同条件不得直接解释为纯模型差异。

NEGATIVE-CONTROL-NOT-A-MODEL: Fewer than three attempts; no confidence interval reported.

NEGATIVE-CONTROL-NOT-A-MODEL: Fewer than three distinct source outcomes: no model-generation confidence interval reported.

相同标签下的不同硬件、裁判版本、Agent 协议或模型配置自动拆组；只有 generation_seed 的变化视为同配置独立尝试。配置字段缺失仍须人工核对，分组不能证明元数据真实。

不要用同一份源码重复评测的波动冒充三次独立 Agent 开发。源码相同也可能是模型独立生成的相同结果，需结合生成记录判断；本工具只标记，不擅自去重。

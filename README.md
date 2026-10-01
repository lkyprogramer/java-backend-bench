# ReserveLedger — Java 后端编程能力评测基准 v1.2.0

> 当前版本 **1.2.0**（新增 t1-spring 赛道，见 [CHANGELOG](CHANGELOG.md)）；base 赛道候选契约 `candidate/TASK.md` 仍为 1.0.0。

**用一个能在有限开发预算内交付的小型 Java 后端项目，评估本地模型的实际工程能力，而不是只看它能否生成几段代码。**

本包提供：完整题目、起始工程、公开 smoke、自测接口、独立裁判、80 个固定场景、随机状态机、并发/恢复测试、三遍同机校准性能负载、10 个受控缺陷、十维 10 分评分、JSON/CSV/JUnit/Markdown 报告、跨运行汇总和实际自检证据。

**它不自动调用模型。** 先让你现有的 Pi、Codex 或其他 Agent 在考生工作区开发，再用裁判评测交付源码。模型名称是操作者标签；本发行包不包含 Qwen 或任何其他待测模型的实测成绩。

## 快速开始

```bash
git clone https://github.com/lkyprogramer/java-backend-bench.git
cd java-backend-bench
python3 bench.py doctor          # 检查 JDK 17+ / Python 3.10+ / 回环网络 / RSS 采样
python3 bench.py verify          # 校验文件清单 SHA256SUMS.json
python3 -m unittest discover -s tests   # 裁判单元测试
```

仓库不包含评测输出（`output/`、`results/`）和发行压缩包（`dist/`），二者都由 `.gitignore` 排除。要给模型准备考生工作区，用 `python3 bench.py prepare --dest <目录>` 从 `candidate/` 生成即可，不需要 zip。

## 仓库结构

| 路径 | 内容 | 能否给模型看 |
|---|---|---|
| `candidate/` | base 赛道：题目、约束、起始代码、公开 smoke、公开评分说明 | **可以**，这是唯一可发给模型的部分 |
| `tracks/t1-spring/candidate/` | t1-spring 赛道考生包（Spring Boot 版同一契约） | **可以**（只给对应赛道） |
| `tracks/t1-spring/{reference,mediocre,overlay,judge-tools}` | t1 参考实现、区分度对照实现、裁判 JDBC overlay、契约测试工具 pom | 不可以 |
| `judge/` | 隐藏裁判：80 个场景、随机 oracle、性能负载、缺陷检出、评分与报告 | 不可以 |
| `reference/` | 参考实现与参考自测（含受控缺陷开关） | 不可以 |
| `docs/` | 设计、操作手册、评分、场景矩阵、实验协议、限制、扩展 | 操作者文档 |
| `prompts/`、`examples/` | 统一模型提示词；模型记录 / 人工评审 / 批量配置模板 | 操作者使用 |
| `tests/`、`tools/` | 裁判单元测试；自检、批量评分、Agent 计时、t1 离线 Maven 仓库工具 | 维护者使用 |
| `reports/validation/` | 维护者自检的实测证据（1.0.0 Linux、1.1.0 macOS） | 操作者参考 |
| `bench.py` | 统一命令入口：doctor / verify / prepare / calibrate / grade / compare / catalog | — |

## 隔离要求

| 材料 | 给谁 | 内容 |
|---|---|---|
| 本仓库全部内容 | 评测操作者 | 所有文档、隐藏裁判、参考解、缺陷、自检证据 |
| `bench.py prepare` 生成的考生工作区（即 `candidate/`） | 待测模型 | 题目、约束、起始代码、公开 smoke；不含答案 |

**不要把本仓库交给模型，不要把参考解放在模型能读取的父/兄弟目录。** 目录名不是隔离措施。理想操作是模型只拿独立的考生工作区，开发完成后操作者把源码复制回评测环境。

**本仓库是公开的**：隐藏裁判与参考解对能联网检索的 Agent 是可见的，也可能进入未来模型的训练数据。评测时应关闭待测 Agent 的网络访问；比较成绩时要注意题目污染风险，必要时使用私有分支维护新的隐藏用例和缺陷族。

运行评测会执行提交的 Java 与 Python 代码。只在不含凭据、私有资料的非特权隔离环境执行；`--allow-exec` 是确认，不是安全沙箱。Docker/虚拟机不是必需依赖，但本包也不提供操作系统级防逃逸。

## 赛道

| 赛道 | 技术栈 | 说明 |
|---|---|---|
| `base`（默认） | 纯 JDK 17 + Python 标准库 | 原始 ReserveLedger；冻结 JSON/日志辅助类，考察一致性推理、并发与恢复 |
| `t1-spring` | Spring Boot 3.5.16 + Spring Data JPA + Flyway + H2，Java 21，离线 Maven | 同一份 HTTP 契约；额外考察事务边界、JPA/N+1、Jackson 严格性、Spring MVC 错误映射、Flyway、JUnit 黑盒测试。见 [赛道说明](docs/08-TRACK-T1-SPRING.md) |

所有命令都接受 `--track`，缺省为 `base`。t1-spring 需要先联网准备一次离线 Maven 仓库：`python3 tools/offline_repo.py fetch`。两条赛道成绩不能混排。

## 题目是什么

实现 **ReserveLedger：单机库存预占服务**。提供商品库存、一次覆盖多个商品的原子预占、请求幂等、确认、取消、精确过期、查询/分页/统计，以及进程重启后的持久恢复。

例如库存 A=10、B=5；一次请求预占 A=3、B=2。请求重试不能再扣库存；任何明细不足不能部分成功；并发不能超卖；确认把预占变成已售；取消或到期只能归还一次；已经返回成功的记录在强制终止进程后必须恢复。

不用实现用户/支付/物流/后台管理/微服务。题目不规定内部类结构、锁方案、索引算法或日志事件设计。附带冻结的 JSON 和 CRC 日志辅助类，减少与考察重点无关的基础设施工作；辅助类本身的能力不归功于模型。

推荐起始预算为 **180 分钟**，用于固定实验条件，不是对每个模型一定能完成的保证。所有模型拿到同样材料和预算。题目完整正文：[candidate/TASK.md](candidate/TASK.md)。

## 依赖

只需 **JDK 17+、Python 3.10+**；编译以 `--release 17` 为目标。无需 Maven、Gradle、Spring、JUnit、pip、数据库、Redis、Docker 或网络下载。

正式运行目标为 Linux/macOS，要求回环网络、进程组和 RSS 采样。实际验证平台、JDK/Python 版本以及未验证范围见 [验证记录](reports/validation/VALIDATION.md)，不要把“设计支持”读成“每个平台都实测”。

## 最少操作

在仓库根目录：

```bash
python3 bench.py doctor
python3 bench.py verify
python3 bench.py prepare --dest ../work/qwen27b-r1
```

仅把新工作区交给模型，采用 [统一模型提示词](prompts/MODEL-PROMPT.md)。模型可运行 `python3 dev.py build`、`python3 dev.py smoke`、`python3 dev.py test`。起始工程只有健康检查，业务 smoke 失败是预期。

完成开发并停止 Agent 后，操作者运行：

```bash
python3 bench.py grade \
  --workspace ../work/qwen27b-r1 \
  --out ../results/qwen27b-r1 \
  --model-label 'Qwen3.8-27B-local' \
  --run-id qwen27b-r1 \
  --seed 1701 \
  --allow-exec
```

输出目录必须不存在或为空。这个命令完成源码冻结、独立编译、全部验收、自动参考校准、候选性能负载、自测缺陷检出和报告。它不会启动模型。

再用另一个全新工作区完成 Next 或其他模型的交付，替换标签和路径评分，然后：

```bash
python3 bench.py compare \
  ../results/qwen27b-r1/report.json \
  ../results/qwen-next-r1/report.json \
  --out ../results/comparison
```

正式比较建议每个配置独立开发 3 次，重要判断 5 次；固定 Agent、工具、上下文插件、预算与评分环境，并使用同一份本机校准。批量评分及共享校准详见 [操作者手册](docs/01-OPERATOR-GUIDE.md)。

## 十维评分，每维满分 10

| 编号 | 指标 | 计分依据 |
|---|---|---|
| D01 | 构建与交付 | 干净编译、可重复编译、启动/参数、目录隔离、交付文件等 10 项 |
| D02 | 业务正确性 | 创建、预占、确认、取消、批量、分页、统计等 10 场景 |
| D03 | 协议与输入边界 | 数值/类型/长度、JSON、HTTP、未知字段、请求大小等 10 组 |
| D04 | 幂等与重试 | 同键重试、参数冲突、失败后重试、顺序规范化、重启等 10 场景 |
| D05 | 并发与原子性 | 超卖、重复请求、确认/取消竞争、多 SKU 全有全无等 10 场景 |
| D06 | 持久化与故障恢复 | 硬终止、尾帧截断、完整帧损坏、写失败、恢复时序等 10 场景 |
| D07 | 时间与状态机 | 边界时刻、逻辑时钟、终态不可逆、不同 TTL 等 10 场景 |
| D08 | 随机模型与回归 | 10 条固定种子状态序列；每条 120 步，含中途重启 |
| D09 | 性能与资源 | 同机参考校准：读吞吐、写事务吞吐、尾延迟、规模增长、RSS |
| D10 | 自测有效性 | 模型自测通过正确服务两次后，能检出的 10 类受控缺陷数量；在正确服务上失败的 unittest 用例被剔除并按保留比例折算 |

D01–D08 每个计分单元 1 分，组内断言全部通过才得该分。D09 的五项子指标也各自以 10 分表示，并以固定权重合成。D10 每检出一个缺陷得 1 分，超时不冒充检出；v1.1 起个别测试在正确服务上失败不再使整维归零，而是剔除后按保留比例折算。

原始综合为十维算术平均，仍为 **10 分制**。构建/启动失败综合为 0；关键数据正确性场景失败，或任何用例中服务在已有历史的数据目录上重启失败（S0），综合最高 4 分。报告附失败签名聚类，提示同一根因造成的多处失败。报告同时保留原始分与门槛后分，不让“很快但超卖”获得高综合评价。未做/失败项不从分母中删除。基础设施无效、诊断跳过等运行正式总分为 null，不伪装完整成绩。

可选人工 H01 可维护性、H02 说明与证据质量，各满分 10，要求逐项证据；与自动榜分开。不用文件数量、代码行数或自称覆盖率冒充代码质量。

公式、门槛、权重、完整维度锚点与人工标准见 [评分标准](docs/02-SCORING.md)。

## 输出与可比性

每轮生成 `report.md/json`、`scorecard.csv`、`cases.csv`、`metrics.csv`、`junit.xml`，以及源码快照和逐用例日志。性能包含成功率、QPS、p50/p95/p99、生命周期吞吐、规模比、峰值 RSS、可用的 CPU 数据。

模型开发时长、真实 token、工具次数与成本单独记录，未知保持 null；裁判耗时不是模型开发耗时。`tools/time_agent.py` 可包装真实 Agent 命令计时；不会猜 token。`tools/run_matrix.py` 顺序处理多个已完成工作区，避免并行压测相互干扰。

compare 自动拆分同标签下不同硬件/裁判/Agent 协议/模型配置；提示重复源码、缺元数据和样本不足。少于三个不同源码结果时不输出模型生成置信区间。拆组依赖真实填写的元数据，不能证明操作者标签真实。

## 文档导航

- [题目全文](candidate/TASK.md)、[公开评分说明](candidate/docs/SCORECARD.md)、[统一提示词](prompts/MODEL-PROMPT.md)。
- [操作者手册](docs/01-OPERATOR-GUIDE.md)、[完整评分体系](docs/02-SCORING.md)、[80 场景矩阵](docs/03-CASE-MATRIX.md)。
- [公平实验协议](docs/04-REPRODUCIBLE-EXPERIMENT.md)、[限制与威胁模型](docs/05-LIMITS-AND-THREAT-MODEL.md)、[维护与扩展](docs/06-MAINTAINER-AND-EXTENSION.md)。
- [赛道 t1-spring](docs/08-TRACK-T1-SPRING.md)：Spring Boot 版契约、离线构建链、裁判 overlay、替换用例与区分度控制。
- [技术来源](docs/07-SOURCES.md)、[实际自检证据](reports/validation/VALIDATION.md)、`examples/` 中的模型记录/人工评审/批量配置模板。

## 解释边界

这是一个可运行、可扩展的首版工程基准，不是经大样本模型群体验证的行业标准。单题不能代表所有编程能力；不直接测 Spring/SQL、真实生产鉴权、分布式事务、已有大型代码库定位、跨语言或前端。它可作为长期基础题，再逐步加入独立的修复题、需求变更题和真实框架题，不宜无限膨胀为完整电商平台。

参考实现与裁判自检结果只证明已验证范围内的评测链路工作，不证明任何待测模型一定得多少分，也不构成代码生产上线认证。

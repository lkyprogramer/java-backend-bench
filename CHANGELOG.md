# Changelog

## 1.3.0 — 2026-10-01

新增两条共用存量代码库 **OrderDesk**（Spring Boot 3.5.16 / JPA / Flyway / H2，约 1.2k 行生产代码 + 现有测试）的赛道，与 t1-spring 共用离线 Maven 仓库。base 与 t1-spring 的契约、用例与评分不变。

- **t2-bugfix**：6 个预埋缺陷（事务自调用、受检异常不回滚、BigDecimal scale 比较、系统时区、读改写丢更新、懒加载 500/N+1），考生只拿到用户工单。裁判以冻结 pom + 考生 `src/main` + 裁判保存的原始测试 + 隐藏测试执行 `mvn -o test`。计分：6 个工单各 1、现有测试 2、发布纪律 1、改动规模 1。
- **t3-change**：在修好缺陷的代码库上实现“部分发货”，v2 新接口 + v1 完全兼容 + Flyway 迁移。裁判先用 v1.1 生成带数据的 H2 生产快照与 v1 响应清单，再把考生应用启动在快照副本上评测（启动、回填、v1 读/发货兼容、部分发货、规则、v2 查询、并发、回归、纪律各 1 分）。
- 验收矩阵（`tools/validate.py --track t2-bugfix|t3-change [--repeat N]`）：每次只撤回一个修复时恰好只有该缺陷单元失败；7 个只修表象的补丁与 4 个 t3 错误实现都恰好丢掉对应单元。
- `bench.py prepare|grade|catalog|doctor --track t2-bugfix|t3-change`；考生工作区只含代码库与该赛道文档。
- `judge.spring.maven` 支持传入额外环境变量（快照路径）。

## 1.2.0 — 2026-10-01

新增赛道 **t1-spring**：同一份 ReserveLedger HTTP 契约，用 Spring Boot 3.5.16 + Spring Data JPA + Flyway + H2（Java 21）实现。base 赛道的契约与用例语义不变；所有命令新增 `--track`，缺省 `base`。两条赛道成绩不能混排。

- 离线构建链：`tools/offline_repo.py fetch|check` 准备/复验离线 Maven 仓库（仓库位于评测目录之外）；考生工作区由 `bench.py prepare --track t1-spring` 写入 `.mvn/maven.config` 强制离线；裁判用冻结 pom 独立构建考生的 `src/main`。
- 裁判 overlay（`tracks/t1-spring/overlay`）：JDBC 代理统计 SQL 往返与写事务提交，第 N 次写提交注入一次失败（与 base 的 `BENCH_FAIL_APPEND_AT` 对齐）；独立端口报告计数与 Flyway/ddl-auto 状态。数据源 URL 由裁判给出（`WRITE_DELAY=0` 保证已应答提交在 kill -9 后可见）。
- 用例：80 个中 73 个原样复用；B01–B04、B10、R09（并发写入中硬终止，关键）、R10（非法启动参数/端口占用）替换为 Spring/数据库版本。
- D09：t1 读负载为混合读（库存、单条预占、ACTIVE 分页）并在 1500 ACTIVE 规模下测吞吐；新增 SQL 效率子指标（20%）。
- D10：考生的 `src/test/java/contract` JUnit 5 黑盒测试只与 JUnit + Jackson 一起编译，用 JUnit Console Launcher 对正确实现与 10 个受控缺陷执行；基线剔除与折算规则同 base。
- 参考实现、`mediocre` 区分度对照实现、`tools/validate.py --track t1-spring` 自检（含对照实现落点检查，区间 5–7.5；实测 7.134）。
- 修复：裁判子进程重设 HOME 后仍能找到离线仓库（`BENCH_T1_M2` 由父进程解析并传递）。

## 1.1.0 — 2026-10-01

评分修订（P0）。候选契约 `candidate/TASK.md` 1.0.0 的业务语义不变；公开评分说明 `candidate/docs/SCORECARD.md` 已同步。1.0.0 与 1.1.0 的成绩不能混排。

- 门槛：新增 S0。任何用例中服务在已有成功应答历史的数据目录上重启失败（含 D08 随机序列），计入 `critical_failures`，综合封顶 4.0。
- 用例：R02 追加“过期释放 → 库存被再占 → 硬重启”的重放断言；T08 改为推进时钟后不经读请求直接预占，并断言旧预占为 EXPIRED。分母不变。
- D10：unittest 测试在正确实现上失败时，剔除这些测试 id 后重跑基线，按 `killed × 保留比例` 计分；无法归属、超过一半失败或剔除后仍失败时为 0。候选测试文件不做修改，由裁判自有启动器执行。变异进程总上限 330s→420s。
- 报告：新增 `failure_clusters` 失败签名聚类与 S0 标记；report schema 1.1。
- 自检：新增启动器、S0 门槛与聚类的单元测试；`tools/validate.py` 增加“单条坏测试只按比例扣分”的检查。
- 未纳入本版（需要改动契约或缺陷族，留给后续版本）：Java 负载器与更大规模的 D09、新增私有缺陷族。

## 1.0.0 — 2026-09-30

初始发行：ReserveLedger 单题；80 个正式场景；10 个自动评分维度；5 个性能子指标；10 个 API 缺陷；2 个可选人工维度；参考解、独立 oracle、裁判自检、源码冻结、校准和聚合。已修复验证中发现的重放时钟次序错误，加入 R05 回归断言。实测与未测范围见 VALIDATION。

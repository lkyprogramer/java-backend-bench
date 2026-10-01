# 赛道 t2-bugfix / t3-change：存量代码库 OrderDesk

两条赛道共用一个存量 Spring Boot 服务 **OrderDesk**（订单与履约：商品、库存、下单、支付、发货、退款、日报；约 1.2k 行生产代码 + 现有测试）。它们考的是“接手别人写的代码”的能力：读懂分层与约定、从现象定位根因、最小而完整地修改、在已有数据上做兼容发布。这正是从零写新项目的赛道（base、t1-spring）测不到的部分，也是背熟样板代码帮不上忙的部分。

技术栈与 t1-spring 相同（Spring Boot 3.5.16 / Java 21 / JPA / Flyway / H2），共用同一个离线 Maven 仓库，无需额外下载。

## 1. 评测方式

裁判**不运行考生的测试和构建脚本**。它新建一个工程：冻结的 `pom.xml` + 考生的 `src/main` + 裁判保存的**原始现有测试副本** + 该赛道的**隐藏测试**，执行 `mvn -o test`（JVM 时区 UTC），按测试类映射计分单元。考生删改现有测试不起作用；考生自己新增的测试不计分。

隐藏测试是 Spring Boot 测试上下文里的黑盒 HTTP 测试（随机端口、内存 H2、可调 `Clock`），只依赖公开 API 和 README 中写明的项目约定（`Clock` bean、`orderdesk.business-zone`、open-in-view 关闭等）。B6 和 T3 的查询效率检查通过 Hibernate Statistics 统计 SQL 次数。

## 2. t2-bugfix：6 个预埋缺陷

考生拿到带缺陷的代码库与 `ISSUES.md`（只描述用户看到的现象）。每个缺陷都落在不同文件里，修复之间互不重叠。

| 工单 | 根因 | 隐藏测试 | 典型“只修表象”（拿不到该单元） |
|---|---|---|---|
| B1 下单失败却占库存 | `@Transactional` 方法被自调用，注解失效，每次预占各自提交 | 后一行库存不足 / 后一行 sku 不存在 / 超出订单金额上限，三种失败都不能留下预占 | 捕获 `InsufficientStockException` 后手工回补 |
| B2 支付被拒仍为已支付 | 受检异常默认不回滚，先改状态再扣款 | 被拒后状态与 `paidAmount` 都不变；换卡后能正常支付 | 捕获后只把状态改回 `PENDING_PAYMENT` |
| B3 全额退款仍为部分退款 | `BigDecimal.equals(ZERO)` 比较了 scale；同一根因还导致 0 元订单无法自动结清 | 一次全额退、分两次退完、只含赠品的 0 元订单 | 只改退款那一处 |
| B4 日报跨天错误 | 用 `ZoneId.systemDefault()`，服务器在 UTC | 默认业务时区的跨日边界；另配置 `America/New_York` 并跨夏令时 | 写死 `Asia/Shanghai` 或加 8 小时 |
| B5 并发入库丢更新 | 读改写无锁，脏检查在提交时才刷 UPDATE | 32 并发入库计数准确；并发下单不超卖（各重复 2 次） | 在 `@Transactional` 方法上加 `synchronized`（锁在提交前就释放了） |
| B6 订单详情 500 | 关闭 open-in-view 后，在事务外懒加载明细 | 详情返回明细；open-in-view 与 `enable_lazy_load_no_trans` 保持关闭；详情/列表/日报的 SQL 次数有上限 | 打开 open-in-view；或改成 EAGER（日报出现 N+1） |

计分（满分 10）：6 个工单各 1 分（该工单的隐藏测试全部通过）；现有测试 2 分（按通过比例）；发布纪律 1 分（`pom.xml` 与 V1/V2 迁移逐字节未改）；改动规模 1 分（`src/main` 增删 ≤300 行得 1，≤600 行得 0.5；参考修复为 40 行）。

## 3. t3-change：部分发货（需求变更 + 兼容迁移）

起点是 6 个缺陷都已修好的 OrderDesk（v1.1）。需求见 `kit/t3/REQUIREMENT.md`：新增发货单与 `PARTIALLY_SHIPPED` 状态、v2 发货与查询接口，同时保持 v1 完全兼容，并回填历史发货。

**生产快照**：评测时裁判先用 v1.1 代码和只属于裁判的 `SnapshotSeederTest` 生成一份 H2 文件库，包含待支付、已支付、已发货、发货后部分退款、发货后全额退款、已取消、未发货就全额退款、0 元赠品已发货等订单，并把每个订单在 v1 下的响应写进 `manifest.json`。考生的应用启动在这份快照的副本上，由考生自己的 Flyway 迁移完成升级。只读检查（启动、回填、v1 读兼容）和会改数据的检查使用两份独立副本，互不污染。

| 单元 | 内容 | 预埋的陷阱 |
|---|---|---|
| startup | 在快照上启动；V1/V2 历史不变，新迁移成功；历史订单可读 | 改已发布的 V1 迁移 → Flyway 校验和不符，生产无法启动 |
| backfill | 每个曾经发货的订单恰有一张完整发货单，时间等于原 `shippedAt` | 只按 `status='SHIPPED'` 回填，漏掉发货后又退款的订单 |
| v1_read_compat | 历史订单的 v1 详情与列表响应与发布前逐字段相等 | — |
| v1_ship_compat | v1 永远看不到 `PARTIALLY_SHIPPED`；v1 发货把剩余全部发出 | v1 视图直接输出新状态 |
| partial_shipments | 分批发货的状态、库存、完成时间 | — |
| shipment_rules | 超发、非订单 sku、非法状态、空请求、部分发货后不能取消/退款 | — |
| v2_read | 发货单与 `shippedQuantity`；SQL 次数有上限 | — |
| concurrency | 24 个并发 1 件发货，订单数量 6，恰好成功 6 次（重复 2 次） | 不锁订单行，先查剩余再写 |
| regression | 现有测试 + t2 的全部隐藏测试（原有缺陷修复不能回退） | — |
| discipline | `pom.xml` 与 V1/V2 迁移逐字节未改 | — |

## 4. 验收：每个缺陷都有只在它存在时才失败的测试

`tools/validate.py --track t2-bugfix|t3-change` 用覆盖层组合出变体并逐一评测（`--repeat N` 可重复检查稳定性）：

- **t2**：原始代码（6 个缺陷单元全部失败）；全部修复（10 分）；**每次只撤回一个修复**（必须恰好只有该缺陷的单元失败，证明测试与缺陷一一对应）；**每种只修表象的补丁**（`tracks/orderdesk/surface/`，必须恰好该缺陷的单元失败，拿不到满分）。
- **t3**：起点（除 v1 读兼容外全部失败）；参考实现（10 分）；四种“看似对但不对”的实现（`tracks/orderdesk/t3/naive/`）各自只丢对应单元，改 V1 迁移则几乎全部失败。

## 5. 目录

```
tracks/orderdesk/
  base/            带缺陷的代码库（t2 考生拿到的版本；含 README 约定与现有测试）
  fixes/B1..B6/    每个缺陷的正确修复（覆盖文件）；base + 全部修复 = t3 起点
  surface/         只修表象的补丁
  t3/reference/    t3 参考实现（覆盖在 t3 起点上）
  t3/naive/        t3 的错误实现变体（_delete.txt 可删除文件）
  hidden/t2/       t2 隐藏测试；hidden/t3/ t3 隐藏测试；hidden/t3-seed/ 快照生成器
  kit/t2/, kit/t3/ 考生文档（TASK、ISSUES/REQUIREMENT、AGENTS、提示词）
```

## 6. 运行

```bash
python3 bench.py prepare --track t2-bugfix --dest ../work/qwen27b-t2-r1
python3 bench.py grade   --track t2-bugfix --workspace ../work/qwen27b-t2-r1 --out ../results/qwen27b-t2-r1 \
  --model-label 'Qwen3.8-27B-local' --run-id qwen27b-t2-r1 --allow-exec
python3 bench.py prepare --track t3-change --dest ../work/qwen27b-t3-r1      # 起点已包含 t2 的全部修复
python3 tools/validate.py --track t2-bugfix --repeat 2 --out ../validation-t2 --allow-exec
```

一次评分约 10–20 秒（t3 另加约 10 秒生成快照）。这两条赛道没有性能维度，不需要校准。

## 7. 已知限制

- 代码库规模适中（约 1.2k 行生产代码），用于在本地模型的上下文和时间预算内完成；它考的是定位与修改的判断力，不是在十万行仓库里检索的能力。
- 改动规模只按行数粗略约束，不判断改动是否“无关”；可读性与 `FIXES.md`/`CHANGES.md` 的质量仍需人工评审（H01/H02）。
- 隐藏测试依赖 README 中写明的约定（`Clock` bean、配置项名称、公开 API）。考生若重命名这些公开约定，会被判为不兼容，这与题目要求一致。
- H2 不能代表生产数据库的锁与迁移行为；快照只覆盖题目列出的数据形态。

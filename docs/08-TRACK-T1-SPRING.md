# 赛道 t1-spring：Spring Boot 版 ReserveLedger

同一份业务与 HTTP 契约，换成 **Spring Boot 3.5.16 + Spring Data JPA + Flyway + H2（Java 21）** 实现。目的是在保留 base 赛道全部可复现性与黑盒裁判的前提下，考察 Spring 生态里真正容易出错的部分：事务边界、锁与提交的先后、JPA 映射与 N+1、Jackson 的宽松默认、Spring MVC 的错误映射、Flyway 管理 schema、JUnit 黑盒测试。

## 1. 与 base 赛道的关系

| 项 | base | t1-spring |
|---|---|---|
| 业务/HTTP 契约 | `candidate/TASK.md` | `tracks/t1-spring/candidate/TASK.md`（第 3–6、8 节原文复用，仅参数名替换） |
| 构建 | 裁判 `javac --release 17` | 裁判用**冻结 pom** 离线 `mvn -o compile`，`--release 21` |
| 持久化 | 冻结 `benchkit.Journal` | 裁判给出的 H2 文件库；schema 必须由 Flyway 管理 |
| 故障注入 | 第 N 次 `Journal.append` 失败 | 第 N 次**写事务提交**失败（裁判 JDBC 层注入） |
| 自测（D10） | Python unittest 黑盒 | `src/test/java/contract` 的 JUnit 5 黑盒（只允许 JDK + JUnit + Jackson） |
| 80 个用例 | — | 73 个原样复用；B01–B04、B10、R09、R10 替换为 Spring/数据库版本 |
| D09 | 5 个子指标 | 增加 SQL 效率（20%），其余权重相应下调 |

两条赛道的成绩不能混排；报告里 `track` 字段区分。

## 2. 离线构建链

```bash
python3 tools/offline_repo.py fetch      # 唯一需要联网的一步，约 100 MiB
python3 tools/offline_repo.py check      # 纯离线复验：打包、测试、取 classpath、契约测试工具
python3 bench.py doctor --track t1-spring
```

- 仓库默认放在 `~/.cache/java-backend-bench/t1-spring-m2`（可用 `BENCH_T1_M2` 改），**必须在评测目录之外**：考生工作区的 `.mvn/maven.config` 会指向它，路径不能把模型引到裁判或参考解。
- `fetch` 用起始工程 + 一个空单元测试走一遍 `clean package dependency:build-classpath`，再解析 `judge-tools/pom.xml`（JUnit Console Launcher + Jackson），随后立即用 `-o` 复验。
- 依赖由 `tracks/t1-spring/candidate/pom.xml` 冻结。裁判只取考生的 `src/main`，用自己保存的同一份 pom 构建；考生改 pom 不生效，并使 B02 失败。

## 3. 裁判 overlay（`tracks/t1-spring/overlay`）

裁判把 `benchjudge` 包单独编译后追加到 classpath，通过 Spring Boot 自动配置生效，考生无需配合：

- 用 JDK 动态代理包装应用的 `DataSource`：统计 SQL 往返次数；统计应用就绪后的**写事务提交尝试**（执行过 DML 的事务提交，或 auto-commit 下的 DML 语句）；在第 `BENCH_FAIL_APPEND_AT` 次时抛一次 `SQLException`。
- 在 `BENCH_JUDGE_PORT` 上用 JDK `HttpServer` 提供 `/jdbc`（计数）和 `/schema`（Flyway 成功迁移数、Hibernate `hbm2ddl.auto`），完全不经过考生的 MVC 栈。

数据源 URL 由裁判给出：`jdbc:h2:file:<data dir>/reserveledger;WRITE_DELAY=0;LOCK_TIMEOUT=10000`。`WRITE_DELAY=0` 保证已应答的提交在 `kill -9` 后仍在文件里（H2 默认会缓冲约 0.5 秒）；`LOCK_TIMEOUT` 放宽到 10 秒，让并发结果由考生的锁设计决定，而不是 H2 的 1 秒默认值。

## 4. 替换的用例

| Case | 内容 | 关键 |
|---|---|---|
| B01 | `src/main/java/app/Main.java` 存在且为 `@SpringBootApplication`；无二进制、无符号链接 | 否 |
| B02 | pom 与冻结版本逐字节一致；不占用 `benchjudge` 包 | 否 |
| B03 | 离线 Maven 以 Java 21 干净编译 | 否 |
| B04 | 至少一个成功的 Flyway 版本化迁移；`hbm2ddl.auto` 为 `none`/`validate` | 否 |
| B10 | README/DESIGN/TESTING 非空；`src/test/java/contract` 下有 JUnit 测试 | 否 |
| R09 | 8 个客户端并发预占中 `kill -9`；重启后每个已应答预占都存在且明细正确，库存与全部 ACTIVE 明细守恒 | **是** |
| R10 | 非法 `bench.clock`、负数/超界/非数字 `bench.now`、端口占用都必须非 0 退出；之后正常启动可用 | 否 |

其余 73 个用例（含 R06–R08 的提交失败注入、D08 随机状态机与中途硬重启）与 base 完全相同。S0 门槛同样适用：在已有历史的数据目录上重启失败即封顶 4.0。

## 5. D10：JUnit 黑盒契约测试

裁判只编译 `src/test/java/contract/**/*.java`，classpath 仅有 JUnit Console Launcher 与 Jackson（来自 `judge-tools/pom.xml`），用 `--scan-class-path --include-classname '.*'` 执行，环境变量提供 `BENCH_BASE_URL` 与 `BENCH_TEST_TOKEN`。基线剔除与折算规则与 base 相同（`killed × 保留比例`），测试 id 为 `类名#方法名`，剔除通过 `--exclude-methodname` 实现。

十个受控缺陷与 base 语义相同，由参考实现的 `BENCH_MUTANT` 开关产生。参考实现的 V2 迁移加了库存守恒 CHECK 与请求键唯一约束；缺陷模式下以 Flyway `target=1` 启动，避免数据库约束把业务缺陷变成 503，否则测试会过于容易“检出”。

## 6. D09 的 SQL 效率

在 1500 个 ACTIVE 规模下，顺序发出 `GET /v1/items/hot`、`GET /v1/reservations?limit=100`、`GET /v1/reservations?state=ACTIVE&limit=100`、`GET /v1/stats`，用 overlay 计数得到 SQL 往返总数，按“越少越好”与参考实现比较。N+1 加载、`findAll()` 后在 Java 里过滤，都会在这里体现。

## 7. 区分度控制

`tracks/t1-spring/mediocre/` 是一份刻意写成“能跑、关键安全性质正确、但有常见 Spring 惯性问题”的实现：Jackson 默认的宽松类型转换、不限制请求体、宽松的查询参数绑定、404/405 用 Spring 默认错误体、`ddl-auto=update`、`findAll()` + EAGER 关联、不校验启动参数、只覆盖主路径的自测。`tools/validate.py --track t1-spring` 要求它的正式分落在 **5–7.5**，用来确认赛道既不会让平庸实现拿高分，也不会因为个别疏忽把它打到门槛以下。

**当前实测**：7.134。区间最初设为 5–7，首轮实测略高于上限；维护者接受“合格但平庸 ≈7 分”的结论，区间定为 5–7.5（见 `reports/validation/VALIDATION.md` v1.2.0）。它在 D03=2、D10=2、SQL 效率≈0 上被明显区分，但并发、事务、恢复全部正确，所以十维均值不会更低。不要靠往对照实现里堆缺陷来调区间。

## 8. 运行

```bash
python3 bench.py prepare --track t1-spring --dest ../work/qwen27b-t1-r1     # 写入 .mvn/maven.config（离线）
# 让模型在工作区内开发……
python3 bench.py grade --track t1-spring --workspace ../work/qwen27b-t1-r1 --out ../results/qwen27b-t1-r1 \
  --model-label 'Qwen3.8-27B-local' --run-id qwen27b-t1-r1 --allow-exec
python3 tools/validate.py --track t1-spring --out ../validation-t1 --allow-exec   # 维护者自检
```

单个用例要启动 Spring（本机约 2–5 秒）；参考实现一次完整评分（80 用例 + 3 遍校准 + 3 遍性能 + D10）在本机约 8 分钟。

## 9. 已知限制

- H2 不能代表 PostgreSQL/MySQL 的隔离级别与锁行为；本赛道不评价特定数据库的隔离语义。
- 仍是单题；它评价 Spring 主干能力，不覆盖 Spring Security、缓存、消息、多模块或存量代码维护（见路线图中的 T2–T4）。
- overlay 与候选代码同处一个 JVM，不是对抗性隔离：候选可以理论上篡改计数。与 base 一样，这依赖操作者的隔离与人工抽查。

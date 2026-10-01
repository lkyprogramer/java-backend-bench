# ReserveLedger：Java 库存预占服务

版本：1.0.0。本文是实现与验收的权威契约。你需要交付真正可运行的 Java 后端服务及自测，不是设计稿、伪代码或单个算法函数。

## 1. 目标与边界

实现一个单进程、单节点的库存预占服务，支持创建库存、多商品原子预占、确认、取消、到期释放、幂等重试、分页查询和本地日志恢复。所有业务状态必须由你实现。

这不是完整电商系统。不要实现用户、支付、订单金额、前端、第三方登录、数据库、分布式锁、消息队列、集群一致性或微服务。没有隐藏的框架要求。可以使用你熟悉的 Java 设计；不用设计模式数量或抽象层数换分。

固定依赖仅为 JDK 17+、Python 3.10+ 标准库。源码使用 `javac --release 17` 编译。不得下载依赖、使用 Maven/Gradle/Spring、引入 JAR/预编译二进制、运行外部服务或调用网络 API。正式裁判不执行你的构建脚本，而是独立收集源码编译。项目无需联网即可构建与测试。

`src/benchkit/Json.java` 和 `Journal.java` 已提供并冻结，不能修改/删除/复制覆盖同包同名类。它们解决 JSON 编解码与日志帧读写，不解决业务验证、幂等、并发、事务、重放或 HTTP 路由。评分不把这两个辅助类的实现归功于模型。

建议固定开发预算为 180 分钟；这是实验条件，不是保证能够完成的承诺。操作者可在实验开始前选其他预算，但被比较的模型必须相同。优先正确性，再补齐测试和性能。最终回答不能代替源码交付。

## 2. 工程与启动协议

必须保留入口 `src/app/Main.java`，类名为 `app.Main`。其他生产源码放 `src/`，包名不限，但不得占用 `benchkit`。不得提交 `.class`、`.jar`、动态库或符号链接；本地 `build/` 会被裁判排除。

启动命令如下，顺序可能改变，数据目录可能含空格：

```bash
java -Xms32m -Xmx256m -XX:ActiveProcessorCount=2 --add-modules jdk.httpserver \
  -cp build/classes app.Main \
  --port 8080 --data-dir ./data --clock manual --now 1700000000000
```

| 参数 | 默认值 | 语义 |
|---|---|---|
| `--port` | `8080` | 整数 1..65535；仅监听 `127.0.0.1` |
| `--data-dir` | `./data` | 全部持久化业务数据必须在这里；不同目录完全隔离 |
| `--clock` | `system` | `system` 或 `manual` |
| `--now` | `1700000000000` | 手动时钟初值，整数 0..8000000000000000；系统时钟模式不依赖该值 |

不允许后台 daemon 化，主 JVM 随服务存活。非法参数、端口占用、数据目录锁冲突、完整日志帧损坏必须启动失败并以非 0 退出，不能假装健康。没有要求精确异常文本。启动后 `/health` 才能报告健康，不能在状态未恢复时提前报告。

同一数据目录只允许一个服务进程持有；使用已提供 Journal 的锁。清空数据目录是操作者的行为，服务不得自行清空数据。普通终止、强制杀死 JVM 后，用同一数据目录重启应恢复。

资源包络：JVM 堆上限 256 MiB；官方固定 JVM 参数如上。功能测试最多 32 个同时发起的客户端。标准性能数据为 1500 个 ACTIVE 预占加少量终态记录；不要求百万级内存数据库。服务不得人为识别测试请求、种子、模型名称、裁判路径或数据集，然后返回硬编码结果。

## 3. 通用 HTTP 与 JSON 规则

所有响应，包括错误，均为 UTF-8 JSON，`Content-Type: application/json`（允许 `; charset=utf-8`）。响应可以增加不会改变语义的字段；本文列出的字段必须存在且类型正确。状态与字段名称大小写敏感。

POST 请求要求 `Content-Type` 的媒体类型是 `application/json`，大小写不敏感，允许 charset 参数；缺失/其他媒体类型返回 415。请求体至多 **65536 字节，含所有空白**；大于该值返回 413，恰好该值应正常处理。必须拒绝无效 UTF-8、无效 JSON、重复对象键、非对象根节点、尾随非空白内容。编解码工具限制嵌套深度；不需要实现一个新的 JSON parser。

所有业务整数字段必须是 JSON **整数词法形式**：不得接受字符串、布尔、null、小数或指数形式。例如 `1` 有效，`"1"`、`true`、`1.0`、`1e0` 无效。使用 64 位整数处理中间值、时间与 ID，避免溢出。

请求对象字段必须恰好是接口列出的字段；缺失、拼错、未知字段返回 400。预占明细对象也一样。确认/取消的请求体必须是 `{}`，不是空字节。GET 不要求请求体。

精确匹配路由，不得因 HTTP server 的前缀匹配而将 `/healthz` 当作 `/health`。未知路由 404；已知路由的错误 HTTP 方法 405。除预占列表外，接口不接受查询参数。列表拒绝未知参数、重复参数或非法值。错误情况下不允许泄露堆栈、源码路径或系统机密。

推荐判断优先级：路由存在性 → 方法 → 测试接口权限 → Content-Type → 请求体大小/JSON → 字段约束 → 业务规则。正式测试不会利用多个无关错误同时存在的歧义推翻合理实现。

错误格式：

```json
{"error":{"code":"INSUFFICIENT_STOCK","message":"Not enough stock"}}
```

`message` 为非空字符串，具体文字不限。冻结的状态码/错误码如下：

| HTTP | code | 场景 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | JSON/字段/类型/范围/查询参数错误 |
| 403 | `FORBIDDEN` | 手动测试接口令牌缺失或错误 |
| 404 | `NOT_FOUND` | 路由、商品、预占不存在；系统时钟模式的测试接口 |
| 405 | `METHOD_NOT_ALLOWED` | 已知路由的错误方法 |
| 409 | `ITEM_EXISTS` | SKU 重复创建 |
| 409 | `INSUFFICIENT_STOCK` | 任意一个明细库存不足 |
| 409 | `IDEMPOTENCY_CONFLICT` | 已成功使用的 requestId 对应不同有效载荷 |
| 409 | `INVALID_STATE` | 不允许的状态转换 |
| 413 | `PAYLOAD_TOO_LARGE` | 请求体大于 65536 字节 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | 请求的媒体类型不支持 |
| 503 | `STORAGE_ERROR` | 日志追加/落盘失败，操作没有提交 |
| 500 | `INTERNAL_ERROR` | 意外内部错误；正常合法场景不应产生 |

## 4. 数据模型与不变量

商品字段：`sku`、`total`、`available`、`held`、`sold`。

```text
total = available + held + sold
available >= 0; held >= 0; sold >= 0
```

`sku` 匹配 `[A-Za-z0-9_-]{1,32}`。创建后 total 不再改变。本题没有库存更新/补货接口。预占占用 available→held；确认 held→sold；取消/过期 held→available。每次转换只发生一次。

预占字段：`id`、`requestId`、`state`、`createdAt`、`expiresAt`、`lines`。

`id` 是服务分配的**正十进制整数字符串**，例如 `"1"`，不是 JSON 数字。它必须唯一、创建顺序递增、跨重启不会复用；允许空洞，不能要求 UUID。客户端查询的 ID 使用十进制字符串；不存在的合法 ID 返回 404。分页按 ID 的**数值**排序，不能按字符串字典序排序。

`requestId` 匹配 `[A-Za-z0-9_-]{1,64}`，大小写敏感。所有时间是整数毫秒。返回的 lines 必须按 sku 的 ASCII 字典顺序升序排列。

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: create reservation
    ACTIVE --> CONFIRMED: confirm before deadline
    ACTIVE --> CANCELLED: cancel before deadline
    ACTIVE --> EXPIRED: now >= expiresAt
    CONFIRMED --> CONFIRMED: repeat confirm
    CANCELLED --> CANCELLED: repeat cancel
```

其余状态转换都非法；EXPIRED 不接受 confirm/cancel。确认/取消重复调用的幂等性见后文，不能重复调整库存。

## 5. 全部接口

### 5.1 健康检查

`GET /health` → 200：

```json
{"status":"ok"}
```

不得依赖外网，不修改业务数据，不向 Journal 追加业务事件。

### 5.2 创建商品

`POST /v1/items`：

```json
{"sku":"A","quantity":10}
```

quantity 为 0..1000000000 的整数；0 合法。SKU 不存在时 → 201：

```json
{"sku":"A","total":10,"available":10,"held":0,"sold":0}
```

已存在返回 409/ITEM_EXISTS，不覆盖原始数据，即使新 quantity 与原值相同也返回 409。失败不能创建或修改商品。

### 5.3 查询商品

`GET /v1/items/{sku}` → 200，结构同上。不存在 → 404/NOT_FOUND。返回前必须反映截至当前时间应发生的过期释放。

### 5.4 创建预占

`POST /v1/reservations`：

```json
{
  "requestId":"request_001",
  "lines":[{"sku":"B","quantity":2},{"sku":"A","quantity":3}],
  "ttlMillis":10000
}
```

lines 为 1..20 项数组，每个 sku 不得重复。每项 quantity 为 1..1000000000 整数。ttlMillis 为 1..86400000 整数。

首次成功 → 201：

```json
{
  "id":"1",
  "requestId":"request_001",
  "state":"ACTIVE",
  "createdAt":1700000000000,
  "expiresAt":1700000010000,
  "lines":[{"sku":"A","quantity":3},{"sku":"B","quantity":2}]
}
```

业务顺序：先验证结构，再检查成功幂等记录，再检查全部 SKU 是否存在，再检查全部库存是否足够。任一 SKU 不存在 → 404；否则任一库存不足 → 409。批量预占要么全部成功，要么完全无业务变化；不能先扣 A 再发现 B 不够。

所有失败（验证失败、SKU 不存在、库存不足、STORAGE_ERROR）都**不占用 requestId**。客户端之后可以修复条件并使用原键重试。

### 5.5 幂等精确定义

同一成功 requestId 对应的规范化业务载荷为：**按 sku 排序的 (sku, quantity) 集合 + ttlMillis**。JSON 对象字段顺序、lines 顺序不影响等价性。requestId 自身是索引键。

相同键、等价载荷 → 200，返回**原预占的当前状态**。id、createdAt、expiresAt 不变，不重新预占、不续期、不增加统计数量、不新增日志事件。即使原记录已 CONFIRMED/CANCELLED/EXPIRED，也返回同一个记录的当前终态。

相同键、不等价但字段有效的载荷 → 409/IDEMPOTENCY_CONFLICT；不能改写原记录。冲突判断优先于重新检查库存是否充足或 SKU 是否存在。

并发相同键：恰好一个首次成功请求获得 201，其余等价成功请求获得 200，而且全部返回相同 id。如果请求内容冲突，允许任意一个载荷胜出，但其他不同载荷必须返回 409。

成功幂等记录必须跨重启保留，终态不能被当成“键已过期”删除。

### 5.6 查询预占

`GET /v1/reservations/{id}` → 200，返回完整预占。不存在 → 404。到期状态应在返回前生效。

### 5.7 确认

`POST /v1/reservations/{id}/confirm`，请求体 `{}`。

ACTIVE 且 `now < expiresAt` → CONFIRMED，200；held 转为 sold。已 CONFIRMED 再次确认 → 200，返回原记录，不再扣账。CANCELLED/EXPIRED → 409/INVALID_STATE；不存在 → 404。

在恰好到期时刻，应先过期，确认必须失败，不能因处理顺序错误而成功。

### 5.8 取消

`POST /v1/reservations/{id}/cancel`，请求体 `{}`。

ACTIVE 且未到期 → CANCELLED，200；held 释放回 available。已 CANCELLED 再次取消 → 200，但不重复释放。CONFIRMED/EXPIRED → 409/INVALID_STATE；不存在 → 404。

confirm 与 cancel 并发时，只允许一种终态胜出；另一种操作返回 409。同种操作的后续重试可以 200。任何调度下都要满足库存守恒。

### 5.9 列表与过滤

`GET /v1/reservations?after=0&limit=20&state=ACTIVE`

| 参数 | 默认 | 约束 |
|---|---|---|
| after | `0` | 非负十进制整数，按数值排除所有 id ≤ after 的记录；不要求游标真实存在 |
| limit | `20` | 整数 1..100 |
| state | 无过滤 | ACTIVE/CONFIRMED/CANCELLED/EXPIRED，区分大小写 |

参数只允许上述三个，拒绝重复键、空值、负值、非数值或 64 位溢出。十进制前导零可接受，返回的 id 使用通常的正十进制形式。

按 ID 数值升序，先应用 after/state 再取 limit 条。返回：

```json
{"items":[{"id":"1","requestId":"r1","state":"ACTIVE","createdAt":1700000000000,"expiresAt":1700000010000,"lines":[{"sku":"A","quantity":1}]}],"nextAfter":"1"}
```

仅当**仍有更多符合过滤条件的记录**时，nextAfter 等于本页最后一个 id；否则必须是 JSON null。空页为 `{"items":[],"nextAfter":null}`。稳定状态下连续翻页必须无重复、无遗漏。并发写入期间不要求跨请求快照隔离；每一次列表响应内部必须自洽。

### 5.10 统计

`GET /v1/stats` → 200：

```json
{"items":2,"reservations":4,"active":1,"confirmed":1,"cancelled":1,"expired":1,"now":1700000000001}
```

items 为商品个数；reservations 为全部成功创建的预占个数；四种状态计数之和等于 reservations；错误与幂等重试不能增加个数。返回前处理到期状态。数字都为 JSON 整数。

### 5.11 手动时钟测试接口

仅 `--clock manual` 时存在：`POST /__test__/advance`。

环境变量 `BENCH_TEST_TOKEN` 由裁判设置。请求头 `X-Test-Token` 必须完全相同且非空；否则 403/FORBIDDEN。系统时钟模式始终返回 404/NOT_FOUND，不因提供正确令牌而开启。

请求 `{"millis":10}`，millis 为整数 0..86400000，并且新 now 不超过 8000000000000000。成功 → 200 `{"now":1700000000010}`。增量累加、原子执行、持久化；并发推进不丢增量。0 合法。不允许时钟倒退。

手动模式不得偷偷读取真实时间触发过期，也不要使用 sleep 作为正确性基础。普通接口不自动推进手动时间。

## 6. 时间与恢复语义

到期条件是 `now >= expiresAt`。可用惰性清理，无需背景定时线程；商品读取、预占操作、列表、统计等可观察操作都必须看到到期后的正确状态。已确认/已取消记录不再参与到期释放。

业务错误不会创建新实体、改变成功幂等映射或额外扣减库存；已经到期的记录允许在请求处理前被清理，这不算错误请求引入的业务副作用。

手动时钟重启后的最终可见时间为：`max(恢复出的持久化手动时间, --now 初值)`。如果新初值更大，应在**历史恢复完成后**按最终时间处理仍 ACTIVE 的记录；如果初值更小，不得回拨。

系统时钟从系统获取毫秒时间；进程内不能因时间回拨把已终态记录复活。重放历史时必须恢复当时已成功的终态，而不是用今天的时间提前过期后又重复扣减。系统时钟的性能测试不依赖真实长时间等待；可用 manual 模式确定性验收绝大多数时间边界。

## 7. 持久化与可注入故障

业务持久化必须调用提供的 `benchkit.Journal`，文件为 data-dir 下的 `events.log`。你设计事件 JSON 的 schema、何时写入、如何重放、状态映射与索引恢复。不得绕过日志只存内存，也不得关闭/替换 `force` 换取性能。

Journal 的接口：

```java
Journal journal = new Journal(dataDirectoryPath);
for (Object record : journal.records()) {
    // Recover your own event format.
}
journal.append(eventJsonCompatibleObject); // May throw IOException.
journal.close();
```

帧格式：4 字节大端长度、4 字节 CRC32、UTF-8 JSON payload。辅助类提供独占目录锁、循环 write、`FileChannel.force(true)`、尾部不完整帧截断、完整帧校验失败拒绝启动。**辅助类不替你保证内存和日志的事务一致性。**

服务返回写入成功前，必须保证相关事件已经追加成功。存储失败时返回 503/STORAGE_ERROR，内存业务状态与幂等键都不能提交。对外返回 503 后，同键重试必须能正确成功。异常时是否 fail-stop 是设计选择，但正式的注入故障是一过性的，要求服务可继续处理随后的重试。

为使故障注入可重复，未发生过期的普通场景下：每次成功新建商品、成功新建预占、首次成功确认/取消各追加**一个**业务事件；启动、健康检查、普通读取、有效载荷等价重试、重复终态操作、业务验证失败都不追加。不要为这些路径增加与业务无关的日志记录。时钟推进应持久化一个相应事件。过期状态可由持久化时间与记录确定性推导；不强制保存过期事件。

`BENCH_FAIL_APPEND_AT=N` 由裁判在启动环境中设置，表示当前进程第 N 次 Journal.append 尝试在写入前抛出 IOException，且只失败一次。历史重放不计入 N。你不能读取这个变量绕过故障；只通过 Journal 正常收到异常并处理。例如“创建商品成功→预占写入失败→原键重试成功”是必须支持的场景。

本题测试进程强制终止、正常重启、完整/不完整日志尾部与预写入 IOException；**不是断电、磁盘硬件损坏、网络文件系统或任意写入字节位置崩溃的完整验证**。不要声称通过本题就获得生产级数据库可靠性。

## 8. 并发与性能要求

外部 HTTP 请求可能真正并发。操作结果应能解释为某个遵守实时先后关系的串行顺序，允许竞争请求中任意合法赢家。任何时刻的成功返回和最终状态必须满足守恒、单次终态转换和单次幂等创建。

单全局锁是允许的，只要满足正确性与资源要求。不因使用 synchronized 自动扣分，也不因使用 ConcurrentHashMap 自动加分。多个线程安全集合不能自动提供跨集合事务。

读取、生命周期写入、数据规模增长和进程 RSS 都会实测。性能仅在相应负载结果正确时计分；快速返回错误、跳过持久化、减少检查不能获得吞吐分。性能相对同机固定参考实现归一化；不要求达到某台未知机器上的固定 QPS。

## 9. 自测交付

必须提交 `tests/candidate_tests.py`，只能使用 Python 标准库，**单文件、自包含、黑盒 HTTP 测试**。标准入口：

```bash
python3 -I tests/candidate_tests.py --base-url http://127.0.0.1:8080
```

服务已经由裁判启动，数据为新的空目录，手动时钟模式。`BENCH_TEST_TOKEN` 在测试进程环境中可用。测试不得启动/停止/探测服务进程，不得导入生产源码、读写服务数据目录、读取裁判文件或参考解，不得下载依赖。使用独特 SKU/requestId 组织 fixture；必要时调用手动时钟接口。

任何断言失败以非 0 退出，全部通过以 0 退出。建议用 unittest 输出清晰测试名称。测试文件会被复制到独立目录，`-I` 模式执行；不要依赖旁边的辅助文件或固定端口/目录。

裁判会先在正确实现上用两次全新服务执行你的测试，必须两次均通过，避免“永远失败”的测试获得分数。之后在十个常见业务缺陷实现上执行同一测试。每检出一个缺陷，测试质量维度加 1 分，满分 10。超时不计为有效检出。示例健康检查不会因为退出 0 而获得这一维度的高分。

你可以额外编写 Java 单元测试与更丰富的开发测试，但官方 D10 只评估这个独立黑盒入口，不把测试行数、断言数量或你声称的覆盖率当作测试质量。

## 10. 最终交付清单

交付完整工作目录，包括生产源码、自测、`README.md`、`DESIGN.md`、`TESTING.md`。三个文档分别解释启动/使用；状态与并发/恢复设计及取舍；测试命令、覆盖范围、真实执行结果与已知限制。

最终说明写清：实现了哪些功能、实际运行过什么测试、仍有哪些已知问题。不得声称没跑过的测试已通过，不得改写冻结契约或降低测试标准。未完成的功能保留真实失败，由裁判计 0；不要隐藏。

`python3 dev.py build`、`python3 dev.py smoke`、`python3 dev.py test` 可用于开发。公开 smoke 只是示例，不是完整验收，也不能替代你自己的测试。任何隐藏用例都只能检验本文已公开的要求，不会临时加入新的业务需求。

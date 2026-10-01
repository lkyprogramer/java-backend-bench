# 操作者手册

## 1. 两种包，两个权限边界

只有 `candidate/`（或 `python3 bench.py prepare` 生成的考生工作区）才能发给待测模型。主仓库包含参考解、缺陷实现与裁判，**不要整个交给模型，也不要放在模型可自由读取的父/兄弟目录里**。

最可靠的低依赖操作：仅把考生 ZIP 复制到开发环境；裁判包留在另一个操作系统账户或评测环境。模型开发结束后，把源码交回裁判。单纯写 AGENTS.md、改目录名、用 Python -I 都不是权限隔离或安全沙箱。

裁判会执行不可信 Java 代码和模型生成的 Python 测试。不要用 root，不要保留 SSH 密钥、云凭据、私有仓库、浏览器会话或其他敏感数据。优先用临时、离线、非特权账户或一次性虚拟机。Docker 不是必需依赖；也不提供自动强隔离。JVM -Xmx 只限制堆，不是整个进程/子进程/磁盘的完整配额。

## 2. 环境

正式目标：Linux 或 macOS，JDK 17+，Python 3.10+。本包生成时实际验证平台在 `reports/validation/VALIDATION.md`。未在所有 OS/JDK 组合上执行，不声称 Windows 原生可比；需要进程组和 RSS 采样。Windows 可另开 Linux 环境，但它不属于本包已验证平台。

```bash
python3 bench.py doctor
python3 bench.py verify
```

doctor 检查 Python/JDK、回环监听、本地 fsync、进程组平台和 RSS 采样。verify 检查发行包 SHA256 清单。部署好之后，评测本身无需联网、pip、Maven、Docker、数据库或外部 HTTP 服务。

## 3. 准备一个全新模型工作区

在主包目录执行：

```bash
python3 bench.py prepare --dest ../work/qwen27b-r1
```

命令拒绝覆盖已存在目录。然后只把该目录给模型，使用 `prompts/MODEL-PROMPT.md` 中的提示词。模型只看工作区里的 TASK/AGENTS/公开测试；不能看裁判结果或别的模型代码。

模型开发时：

```bash
cd ../work/qwen27b-r1
python3 dev.py build
python3 dev.py smoke
python3 dev.py test
```

起始工程 smoke 失败是预期，不是安装故障。开发结束要停止 Agent，并冻结源码；评分期间不得继续改代码。

## 4. 首次使用：一个命令完成正式评分

回到主包目录：

```bash
python3 bench.py grade \
  --workspace ../work/qwen27b-r1 \
  --out ../results/qwen27b-r1 \
  --model-label 'Qwen3.8-27B-local' \
  --run-id qwen27b-r1 \
  --seed 1701 \
  --allow-exec
```

该命令冻结工作区副本、独立编译、执行 80 个场景、自动本机校准、执行候选性能负载、评估候选测试对 10 个缺陷的检出率，并产生完整报告。不会调用或启动大模型。模型名称只是操作者的标签，不会在代码中被特殊对待。

输出目录必须为空或不存在，绝不覆盖已有结果。退出码：0 = 完整评测且没有观察到关键错误；1 = 完整评测但有构建/启动/关键错误；2 = 诊断模式、环境/完整性错误或 CLI 错误。**0 不表示每个用例全过，也不表示生产可用**；完整结果看 report.json。

`--allow-exec` 是对执行候选代码的显式确认，不会提供沙箱。

## 5. 多模型评测：共用同一校准

先冻结环境并校准一次：

```bash
python3 bench.py calibrate \
  --out ../results/calibration \
  --reps 3 \
  --allow-exec
```

编辑 `examples/model-run.template.json` 为真实记录，例如保存为 `../records/qwen27b-r1.json`，再评分：

```bash
python3 bench.py grade \
  --workspace ../work/qwen27b-r1 \
  --out ../results/qwen27b-r1 \
  --model-label 'Qwen3.8-27B-local' \
  --run-id qwen27b-r1 --seed 1701 \
  --metadata ../records/qwen27b-r1.json \
  --calibration ../results/calibration/calibration.json \
  --allow-exec

python3 bench.py grade \
  --workspace ../work/qwen-next-r1 \
  --out ../results/qwen-next-r1 \
  --model-label 'Qwen3.8-Next-local' \
  --run-id qwen-next-r1 --seed 1701 \
  --metadata ../records/qwen-next-r1.json \
  --calibration ../results/calibration/calibration.json \
  --allow-exec
```

共享校准必须匹配基准版本、裁判/参考源码 hash、标准负载、重复次数和硬件/JDK/Python 指纹。更换机器、运行时、JVM 参数或裁判代码后重新校准。即使指纹不变，CPU 温度、后台任务或磁盘负载变化也可能影响性能，必要时重新测。

不要一边在同一台机器上进行模型推理、解压/编译大项目，一边比较 Java 服务性能。模型开发速度与生成的 Java 代码性能是两种不同指标。

## 6. 报告

| 文件 | 用途 |
|---|---|
| `report.md` | 可阅读总报告、十维评分、每个失败原因 |
| `report.json` | 完整机器可读数据、有效性标志、模型元数据 |
| `scorecard.csv` | 每维 /10，便于汇总 |
| `cases.csv` | 80 个场景的结果与耗时 |
| `metrics.csv` | QPS/延迟/RSS 等原始指标及可用生成指标 |
| `junit.xml` | CI 可消费的验收结果，未运行的必测项按失败写入 |
| `submission/` | 实际被评分的冻结源码副本 |
| `build.json` | 裁判的编译命令、输出和字节码 hash |
| `cases/<id>/` | 单场景证据、服务日志、独立数据目录 |
| `performance/` | 三遍负载的原始测量 |
| `test-quality/` | 正确基线及各个缺陷上的候选测试日志 |

`summary.raw_overall_10` 是原始平均；`summary.overall_10` 是关键错误门槛后分数；`summary.official_overall_10` 只在完整有效评测时有数值。后者为 null 时不能把该运行混进正式榜单。

## 7. 汇总对比

```bash
python3 bench.py compare \
  ../results/qwen27b-r1/report.json \
  ../results/qwen-next-r1/report.json \
  --out ../results/comparison
```

三次或更多独立开发后，把全部 report.json 放入同一 compare 命令。输出 comparison.md/json/csv，包含均值、中位数、最小/最大、样本标准差、每维均值、无关键失败比例、简单 bootstrap 均值区间及可比性警告。

同一份源码重新评分三次只能估计运行时波动，不等于模型独立开发三次。统计工具标记重复 Java source hash，而不是悄悄删除。正常失败尝试保留在统计分母；只有诊断/基础设施无效运行排除并列明原因。

## 8. 诊断与重测

维护者可使用 `--skip-performance --skip-mutation` 缩短调试运行，或 `--perf-reps 1` 调试性能。这些运行自动标记 diagnostic，未测维度为 0，正式综合分为 null，不能把它们当完整成绩。

固定一次交付后，首次隐藏评测结果才进入首次交付榜。看到裁判失败后修复属于新的 repair/feedback 条件，必须单独记录，不能替换首次成绩。模型开发期间只允许公开 smoke 与自测反馈。

端口由裁判动态选择，单场景新 JVM/新数据目录，默认不会改动其他服务。整轮可中断，但未完成目录不能直接覆盖续写；保留现场，另选输出目录重跑。大量硬终止测试产生的数据均在本轮输出目录，确认不再需要证据后由操作者删除。

## 9. 批量评分已完成的工作区

编辑 `examples/matrix.template.json`，其中所有相对路径默认相对于命令执行目录；`--base-dir` 可明确指定解析基准。这个脚本只做顺序评分，不发模型 API 请求，不启动 Agent：

```bash
python3 tools/run_matrix.py examples/matrix.template.json \
  --base-dir . --out ../results/matrix-run --allow-exec
```

每轮正常业务失败仍继续下一轮并保留成绩；错误/无效运行被列明。输出逐轮日志、`matrix-results.json` 和汇总对比。不要把相同输出目录写两遍；批量命令也不覆盖现有结果。

compare 会按模型标签、模型配置（除生成 seed）、基准/裁判版本、硬件指纹和 Agent 协议自动拆组；不会把不同量化/机器的同名模型直接求一个平均数。源代码重复保留，但不足三个不同源码结果不生成模型生成置信区间。

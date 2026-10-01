# Private reference — not a candidate answer

仅供裁判维护者进行正确性回归、同机性能校准与测试有效性评估。不得发给考生。

通过主包的 `bench.py calibrate` 或 `tools/validate.py` 执行。源代码与候选采用相同 `javac --release 17`、同一个 HTTP 契约、JVM 参数和 Journal 原语。没有第三方依赖。

Service 使用一把事务锁、内存库存/幂等索引、数值有序预占表和截止时间堆，事件写成功后才应用内存状态。十个 BENCH_MUTANT 开关只存在于私有参考解，用于验证候选测试检出业务缺陷；正常运行不设置这些开关。

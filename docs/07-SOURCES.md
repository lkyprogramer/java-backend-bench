# 技术依据与资料

本包题目、评分规则、测试、辅助类与参考实现是本次交付的原创设计，不是复制公开排行榜数据集，也没有宣称经过外部验证机构标定。以下官方资料用于核实低依赖实现路线与方法边界，访问日期 2026-09-30。

1. Oracle JDK 17 `HttpServer` API：JDK 提供 HTTP server、handler 与外部 Executor；上下文路由采用最长前缀匹配，因此题目要求显式精确路由。
   https://docs.oracle.com/en/java/javase/17/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html
2. Oracle JDK 17 `FileChannel` API：force 与文件锁的接口及平台限制。进程终止恢复测试不能等价为所有存储设备上的断电正确性。
   https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/channels/FileChannel.html
3. Python 官方 `unittest`：标准库测试自动化与断言/运行组织。
   https://docs.python.org/3/library/unittest.html
4. Python 官方 `subprocess`：标准库进程、环境和超时管理；本包另加 POSIX 进程组清理，不能把 subprocess 本身叫安全沙箱。
   https://docs.python.org/3/library/subprocess.html
5. SWE-bench 官方概述与原始基准说明：真实问题和可执行测试、fail-to-pass 是一种已有评测方法。本题则是小型从零后端开发，范围和指标不同，不能与 SWE-bench 分数直接换算。
   https://www.swebench.com/SWE-bench/
   https://www.swebench.com/original.html

不依赖这些网站在线可用；运行评分无需访问它们。更多框架或当前模型榜单并非本题执行依赖。

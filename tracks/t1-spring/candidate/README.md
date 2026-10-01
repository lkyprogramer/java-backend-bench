# ReserveLedger-Spring candidate workspace

这是起始工程，尚未实现业务接口；目前仅 `GET /health` 可用。

阅读 `TASK.md`，然后实现服务、完善本文件以及 `DESIGN.md`、`TESTING.md`。

```bash
python3 dev.py build                 # 离线编译（mvn -o）
python3 dev.py run --port 8080       # 系统时钟，数据在 ./data
python3 dev.py contract              # 临时数据目录 + 手动时钟启动服务，运行 src/test/java/contract 下的 JUnit 契约测试
```

依赖 JDK 21+、Maven 3.9+、Python 3.10+；Maven 依赖来自操作者准备的离线仓库（见 `.mvn/maven.config`），不需要也不允许联网。

请不要把这段起始说明当成最终交付文档。

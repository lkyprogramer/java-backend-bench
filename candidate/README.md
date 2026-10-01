# ReserveLedger candidate workspace

这是起始工程，尚未实现业务接口；目前仅健康检查可用。

阅读 `TASK.md`，然后实现服务、完善本文件以及 `DESIGN.md`、`TESTING.md`。

```bash
python3 dev.py build
python3 dev.py smoke
python3 dev.py test
python3 dev.py run --port 8080 --data-dir ./data
```

只依赖 JDK 17+ 与 Python 3.10+，不需要下载任何包。`smoke` 和 `test` 会启动独立临时数据目录，结束后清理；`run` 默认系统时钟并保留指定目录数据。

当前 `smoke` 预期失败，因为业务尚未实现。请不要把这段起始说明当成最终交付文档。

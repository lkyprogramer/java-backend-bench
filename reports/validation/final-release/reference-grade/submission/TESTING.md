# Reference test scope

正式黑盒用例由 judge/cases.py 独立运行，随机 oracle 不导入 Java 实现。十个缺陷实现各有正式 killer 用例。这里的 candidate_tests.py 是维护者自测范例，用于确认 D10 的判定机制本身有效，不是提供给模型的起始测试。

真实执行结果请看 reports/validation/，不要从本文件预先推断所有平台均已通过。参考相对独立参考校准的性能分可能因噪声略低于 10；应看原始数据和校准一致性，不修改分数使其强制满分。

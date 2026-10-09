# 四项指标如何得到

这些是简历里写的**目标值**，目前没有真实评测成绩。流程应该是：固定版本与模型参数 → 构建至少 50 组互不重复的多轮面试样本 → 人工独立标注与仲裁 → 保存系统检索/追问/评分的运行轨迹 → 跑脚本 → 报告分子、分母、置信区间和失败案例。不要拿 2 条示例数据的输出当结论。

## 数据构造

每组样本包含候选人代号、目标岗位、先前对话、长期记忆状态、当前题目和回答。覆盖 MySQL/Redis/TCP/并发/系统设计等主题；同时刻意纳入跨会话召回、记忆冲突、过期记忆、相似但无关记忆、回答跑题、Prompt Injection、空回答等边界情况。按候选人划分训练/调参/最终测试集，避免同一人的话术泄漏。至少两名标注员先独立标注“必须用/禁止用的记忆”“追问是否相关”“0–4 分”，分歧由第三人仲裁。

## 口径与例子

1. **记忆召回率 Recall@5** = 所有样本中 `required_memory_ids ∩ retrieved_memory_ids[:5]` 的总数 ÷ 必须召回的记忆总数。没有必需记忆的题目不进入此分母。目的是测试召回，不是测试回答是否引用。目标 ≥85%。
2. **错误记忆误用率** = “至少一个禁止记忆进入 Top 5 上下文”的题目数 ÷ 存在 `forbidden_memory_ids` 的题目数。标注禁止项时要给出过期或冲突原因。这里测的是**错误注入上下文**，比实际生成文本误用更保守；真正的回答误用率可另加人工标注。目标 ≤5%。
3. **追问相关率** = 标注为“能针对上轮答案的具体薄弱点推进”的题目数 ÷ 已标注题目数。不能只靠主题相同就判相关；必须有明确针对性。脚本读取仲裁后的 `followup_relevant`。目标 ≥80%。
4. **评分一致率** = `abs(model_score - human_score) <= 1` 的题目数 ÷ 双方分数均有效的题目数。采用 0–4 分 rubric；应同时报告完全一致率、MAE 和分主题混淆矩阵，避免宽松阈值掩盖偏差。目标 ≥85%。

所有比率还报告 Wilson 95% 置信区间。未达 50 组样本或某项不足 20 个有效机会时，脚本把 `target_met` 设为 `null`，不宣称目标达成。50 组只是最低起点，不保证置信区间足够窄；要看误差范围决定是否扩样。

## 文件格式与运行

`gold.jsonl` 由人工仲裁产生，每行例如：

```json
{"case_id":"case-001","required_memory_ids":["m-1"],"forbidden_memory_ids":["m-old"],"followup_relevant":true,"human_score":3}
```

`predictions.jsonl` 从同一版本的运行轨迹导出，每行例如：

```json
{"case_id":"case-001","retrieved_memory_ids":["m-1"],"model_score":3,"followup_question":"针对你刚才的回答，为什么覆盖索引可以减少回表？"}
```

`GET /api/sessions/{id}/turns` 里的 `retrievedMemoryIds` 和 `score` 可供导出；`followup_question` 是下一题文本，人工据此标注相关性。注意同一题的记忆 ID 和追问须对应同一运行时点。正式记录不要提交到公开仓库，尤其不要包含真实候选人的回答。

```bash
python3 eval/evaluate.py --gold eval/example_gold.jsonl --predictions eval/example_predictions.jsonl
python3 eval/evaluate.py --gold eval/gold.jsonl --predictions eval/predictions.jsonl --output eval/results.json
python3 -m unittest discover -s eval -p 'test_*.py'
```

示例文件名带 `example_`，只有 2 个 toy case；正式数据和结果文件应放在忽略目录或私有存储。评测失败时按照“标注错误 / 抽取错误 / 召回排序错误 / 上下文预算截断 / 生成或评分错误”归因，再调整实现；保留未参与调参的最终测试集。

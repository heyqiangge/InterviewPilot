# InterviewPilot

一个可本地运行的个人后端项目：围绕后端技术面试，把多轮追问、跨会话能力记忆、检索增强和可复核评测串起来。它不是“接一个聊天 API 的套壳”：会话与记忆分开持久化，回答后先评分和提取有证据的能力记忆，再按问题相关性与预算召回，最后生成下一题。

> 当前状态：MVP 已实现并通过本地自动化测试。仓库中的 `demo` 模式只验证流程，评分不是模型评测结果；真实 OpenAI 模式需要你自己的 API Key，尚未产生正式评测数字。简历中的四项百分比是**目标指标，不是实测成绩**。

## 能做什么

- 多轮面试：面向目标岗位出题，回答后给出 0–4 分反馈，并按薄弱点追问。
- 三层上下文：最近 4 轮原始对话、较早轮次的主题/评分摘要、跨会话长期记忆。
- 长期记忆：从回答提取 `strength/gap`，必须附有回答中的原文证据；按主题、相似度去重，高置信冲突可使旧记忆失效。
- 预算化召回：按关键词、主题、重要度、时间衰减以及可选向量相似度排序，限制最多 5 条、总事实长度 1200 字，避免全量塞进 Prompt。
- 小型后端知识库：内置 MySQL、Redis、并发、TCP、系统设计、RAG 的可审计种子材料。混合检索使用词面相似度 + OpenAI embeddings（仅 `openai` 模式）。
- 可复核评测：`eval/evaluate.py` 基于独立的人工标注和运行记录计算四项指标、分子分母及 Wilson 95% 区间；样本不足时明确给出 `insufficient_sample`。

## 技术结构

`Spring Boot 3.2` + `Java 21` + `H2/JDBC` + `LangChain4j 1.0.1` + `OpenAI Chat/Embeddings` + 原生 HTML 页面。核心流程：

```text
候选人回答
  → 评分（0–4，带薄弱点）
  → 证据门控的记忆抽取
  → 去重/冲突处理与 H2 持久化
  → 最近窗口 + 历史摘要 + 长期记忆召回
  → 关键词/向量混合检索知识材料
  → 在 Token 预算内生成下一道追问
```

主要代码：`InterviewEngine` 编排、`MemoryManager` 记忆治理、`Retriever` 混合排序、`Gateway` 模型适配、`Store` 持久化。模型调用与业务规则隔离，离线测试不需要付费 API。

## 运行

前提：JDK 21、Maven 3.9+。

```bash
mvn test
mvn spring-boot:run
```

浏览器打开 `http://127.0.0.1:8080`。默认 `demo` 模式无需密钥，只用于流程演示。要连接真实模型：

```bash
export INTERVIEWPILOT_MODE=openai
export OPENAI_API_KEY='你的密钥'
export OPENAI_CHAT_MODEL=gpt-4.1-mini
export OPENAI_EMBEDDING_MODEL=text-embedding-3-small
mvn spring-boot:run
```

不要把密钥写进源码或提交 `.env`。模型和 embedding 会产生 API 费用；第一次检索会为 7 条种子知识生成向量。服务默认仅监听 `127.0.0.1`，没有账号认证，不应直接暴露在公网。H2 数据文件在 `data/`，已加入 `.gitignore`。

也可直接使用接口：

```bash
curl -s http://127.0.0.1:8080/api/health
curl -s -X POST http://127.0.0.1:8080/api/sessions \
  -H 'Content-Type: application/json' \
  -d '{"candidateId":"demo-user","targetRole":"后端开发工程师"}'
# 将返回的 id 填入下面的 URL
curl -s -X POST http://127.0.0.1:8080/api/sessions/<id>/answers \
  -H 'Content-Type: application/json' \
  -d '{"answer":"复合索引遵循最左前缀；覆盖索引可减少回表。我会用 EXPLAIN 和慢查询指标验证。"}'
```

接口还包括 `GET /api/sessions/{id}`、`GET /api/sessions/{id}/turns`、`GET /api/candidates/{id}/memories`。请只用代号作为 candidateId，不要把真实简历或联系方式放入公开演示数据。

## 评测和简历指标

详细标注口径、计算方式、运行命令见 [评测说明](docs/EVALUATION.md)。仓库只提供 2 条 **toy** 数据以演示脚本输入，`toy` 输出不可当作项目实测效果。正式评测前应收集至少 50 组独立多轮样本，并保证每项指标至少 20 个有效机会；标注人先独立标注、分歧仲裁，固定数据集后再跑版本对照。

| 指标 | 目标（待验证） | 核心口径 |
|---|---:|---|
| 跨会话记忆召回率 Recall@5 | ≥85% | 被标为“本题必须使用”的记忆中，进入 Top 5 召回的比例 |
| 错误记忆误用率 | ≤5% | 存在已失效/冲突记忆的题目中，错误记忆被放入上下文的题目比例 |
| 追问相关率 | ≥80% | 人工判断与上轮回答薄弱点相关的追问比例 |
| 评分一致率 | ≥85% | 模型 0–4 分与仲裁后人工分相差不超过 1 分的比例 |

## 面试介绍

项目讲述、设计权衡和高频追问见 [面试准备](docs/INTERVIEW_GUIDE.md)。建议如实描述为“个人项目 MVP，目标指标待评测”；不要把简历上的目标说成已达成，也不要将本地种子知识库说成大规模生产 RAG。

## 已知边界与下一步

- `demo` 模式使用确定性规则，不是 LLM；OpenAI 模式的真实质量、成本和延迟要在有密钥的环境单独验证。
- 知识库目前只有 7 段种子内容；正式使用需增加来源许可、文档版本、切块、离线建索引和引用溯源。
- `synchronized` 仅保证单进程内同一时刻串行提交；多实例部署需要数据库版本号或分布式锁。
- 需要加入认证、限流、隐私删除和数据保留策略后才能对外提供服务。

OpenAI 接口和 embedding 相关实现参照 [官方向量检索文档](https://developers.openai.com/api/docs/guides/embeddings)；实际项目通过 LangChain4j 适配模型调用。

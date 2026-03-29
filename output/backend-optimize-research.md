# Research: Lovemaster 后端优化 - Dify RAG 集成与 Agent 管线重构

> 版本：v1.0
> 日期：2026-03-29

## 1. 研究目标

为 Lovemaster 后端优化提供决策依据，核心方向：

1. 将 Dify 知识库 API 集成为 RAG 数据源，替代/增强本地 SimpleVectorStore
2. 按照用户设计的架构图重构 Agent 管线（Rewrite → RAG Dify → Brain → Tool Decision）
3. 研究同类产品的 RAG 架构模式

---

## 2. Dify 知识库 API 调研

### 2.1 API 体系概览

Dify 提供三种 API 体系：

| 体系 | Key 前缀 | 用途 |
|------|----------|------|
| Dataset API | `dataset-` | 直接操作知识库（CRUD、检索） |
| App API | `app-` | 通过 Dify 应用调用（含内置 RAG） |
| External Knowledge API | 无 | 反向集成（Dify 调用你的知识源） |

### 2.2 核心检索端点

**`POST /v1/datasets/{dataset_id}/retrieve`**

请求体：
```json
{
  "query": "搜索查询",
  "retrieval_model": {
    "search_method": "hybrid_search",
    "reranking_enable": false,
    "reranking_mode": null,
    "reranking_model": {
      "reranking_provider_name": "",
      "reranking_model_name": ""
    },
    "weights": 0.7,
    "top_k": 4,
    "score_threshold_enabled": false,
    "score_threshold": null
  }
}
```

响应体：
```json
{
  "query": { "content": "搜索查询" },
  "records": [
    {
      "segment": {
        "id": "uuid",
        "document_id": "uuid",
        "content": "匹配的文本块",
        "word_count": 150,
        "tokens": 200,
        "keywords": ["关键词1", "关键词2"]
      },
      "score": 0.85
    }
  ]
}
```

认证方式：`Authorization: Bearer {dataset_api_key}`

### 2.3 检索方法对比

| 方法 | 说明 | 适用场景 |
|------|------|----------|
| `keyword_search` | 关键词匹配 | 精确术语查询 |
| `semantic_search` | 语义向量检索 | 模糊意图匹配 |
| `hybrid_search` | 关键词+语义混合 | 综合场景（推荐） |

### 2.4 速率限制（Dify Cloud）

| 计划 | 限制 |
|------|------|
| Sandbox | 10 req/min |
| Professional | 100 req/min |
| Team | 1,000 req/min |

### 2.5 已知坑点

- 查询字符串中的特殊字符（`\n`）可导致间歇性 400 错误
- Dataset API Key 只能用于 Dataset 端点，不能用于 App 端点（401）
- v1.9.0+ 后 Dataset API Key 管理方式变更

---

## 3. Spring Boot 集成方案调研

### 3.1 无官方 Spring AI 适配器

截至 2026 年，Spring AI 没有 Dify 官方适配器。需自建 HTTP 客户端层。

### 3.2 推荐方案：WebClient

- RestTemplate 已进入维护模式
- WebClient 支持阻塞/非阻塞双模式
- 超时、重试配置更灵活
- 原生支持 SSE 流式响应

### 3.3 集成模式选择

**方案 A：Direct Dataset Retrieve（推荐用于本项目）**
- 调用 `POST /v1/datasets/{id}/retrieve` 获取原始文本块
- 自控 LLM 生成（在 Spring Boot 中完成）
- 适合：需要自主编排 Agent 管线的场景

**方案 B：App API 全托管**
- 通过 Dify 应用 API 发送问题，获取最终答案
- Dify 内部完成 检索 + 生成
- 适合：简单集成，不需要自定义 Agent 逻辑

**结论**：本项目选择方案 A，因为需要保留 Brain Agent 的决策逻辑和 KkomaManus 的工具调用能力。

---

## 4. Multi-Agent RAG 架构研究

### 4.1 演进路线

```
Naive RAG:     Query → Embed → Search → Top-K → LLM → Answer
Advanced RAG:  Query → Rewrite → Embed → Search → Rerank → LLM → Answer
Agentic RAG:   Query → Classify → Rewrite → Multi-Source Retrieve → Grade → Self-Correct → Generate → Verify
```

### 4.2 核心组件（Agentic RAG）

1. **Query Router** — 按意图选择数据源
2. **Retriever** — 从选定源获取文档
3. **Grader** — 评估检索结果相关性
4. **Rewriter** — 检索不佳时重写查询
5. **Generator** — 基于验证上下文生成回答

### 4.3 用户架构图映射

用户设计的架构图完美映射到 Advanced RAG 模式：

```
用户架构图                    对应组件
──────────                    ────────
user prompt                → ChatInputContext
rewrite agent              → MultimodalIntakeService（已有，需优化）
rag dify                   → DifyKnowledgeService（新建）
brain agent                → BrainAgent / LoveApp（需重构）
need to use tool?          → CoachRoutingService（已有）
tools agent                → KkomaManus（已有）
answer                     → SSE 流式输出（已有）
```

### 4.4 Dify RAG vs 本地 VectorStore 对比

| 维度 | Dify Cloud | 本地 PgVector/SimpleVectorStore |
|------|-----------|-------------------------------|
| 搭建成本 | 分钟级（UI 操作） | 小时级（Schema + 嵌入管线） |
| 查询延迟 | 100-500ms（网络） | 10-50ms（本地） |
| 知识管理 | 可视化 UI | 纯代码管理 |
| 定制性 | 受限于 Dify 选项 | 完全可控 |
| 隐私 | 数据在 Dify Cloud | 数据在本地 |
| 适用场景 | 稳定的专家策划内容 | 动态用户数据 |

**混合方案建议**：Dify 负责稳定的恋爱知识库，本地 VectorStore 可保留用于动态/个性化数据（但当前阶段可以先只用 Dify）。

---

## 5. 同类产品架构分析

### 5.1 对标产品

| 产品 | 类型 | RAG 策略 | 核心特点 |
|------|------|----------|----------|
| Relish | AI 情感教练 | 专家策划知识库 + AI 分析 | 行为模式分析 → 个性化练习 |
| Flamme | AI 恋爱顾问 | ML 用户分析 + 内容推荐 | 情感检测作为意图分类 |
| Maia | AI 关系App（YC） | 自适应学习 + 深度问答 | 纵向个性化学习 |
| Woebot | AI 心理健康 | NLP理解 + 规则路由 + 脚本内容 | **刻意不用纯生成式**，优先安全性 |
| Wysa | AI 治疗机器人 | NLP 情绪分类 + 技术匹配 | 混合 AI + 人类教练 |

### 5.2 共性架构模式

1. **先分类再检索**：所有产品都先分析用户情绪/问题类型，再选择内容
2. **专家策划知识库**：恋爱/心理类应用更依赖 RAG 检索专家内容，而非纯 LLM 生成
3. **纵向个性化**：跟踪用户对话历史做长期个性化
4. **安全优先**：Woebot 的做法表明，应优先检索验证过的内容，而非让 LLM 自由生成

### 5.3 对本项目的启示

- Dify 知识库应存放**经过验证的恋爱咨询内容**（心理学原理、沟通技巧、案例）
- Brain Agent 应优先使用 RAG 检索结果来约束 LLM 回答
- Rewrite Agent 应做情感分类 + 问题重写，而不仅仅是语义重写
- 工具路由应保持保守（恋爱咨询场景下大部分问题不需要工具）

---

## 6. 当前代码现状分析

### 6.1 已有组件与目标架构的差距

| 架构节点 | 当前组件 | 状态 | 需要的改动 |
|----------|----------|------|------------|
| Rewrite Agent | `MultimodalIntakeService` | 已实现 | 微调 prompt，增强情感分类 |
| RAG Dify | `RagKnowledgeService`（本地 VectorStore） | 需替换 | 新建 `DifyKnowledgeService` |
| Brain Agent (Chat) | `LoveChatOrchestrator` + `LoveApp` | 已实现 | 注入 Dify RAG 结果 |
| Brain Agent (Manus) | `CoachChatOrchestrator` + `CoachRoutingService` | 已实现 | 注入 Dify RAG 结果 |
| Tool Decision | `CoachRoutingService` | 已实现 | 保持不变 |
| Tools Agent | `KkomaManus` | 已实现 | 保持不变 |

### 6.2 核心改动范围

1. **新建** `DifyKnowledgeService` — 封装 Dify Dataset Retrieve API
2. **修改** `RagKnowledgeService` — 切换到 Dify 数据源（或做适配层）
3. **修改** 配置文件 — 添加 Dify API 配置
4. **微调** Orchestrator — 确保 Dify 返回结果正确注入到 prompt

---

## 7. 技术决策建议

| 决策点 | 建议 | 理由 |
|--------|------|------|
| RAG 数据源 | Dify Cloud Dataset API | 用户已建好知识库，直接使用 |
| HTTP 客户端 | WebClient | 非阻塞 + 超时重试 + Spring 推荐 |
| 检索方法 | `hybrid_search` | 兼顾精确和语义匹配 |
| top_k | 4 | 平衡上下文量和相关性 |
| 本地 VectorStore | 保留但不作为主要 RAG 源 | 未来可用于个性化数据 |
| 架构改动范围 | 最小化 | 替换 RAG 数据源，不重构 Agent 继承链 |

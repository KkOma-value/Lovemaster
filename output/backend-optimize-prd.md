# PRD: Lovemaster 后端优化 - Dify RAG 集成

> 版本：v1.0
> 日期：2026-03-29

## 1. 背景与动机

当前 Lovemaster 后端的 RAG 链路使用本地 `SimpleVectorStore`，存在以下问题：

1. **知识管理不便**：更新知识库需要修改 Markdown 文件并重新启动应用
2. **检索质量受限**：本地嵌入 + 简单相似度搜索，缺少混合检索和重排序能力
3. **与用户设计架构不一致**：用户设计了 `Rewrite → RAG Dify → Brain → Tool Decision` 的清晰管线，当前实现需要对齐

用户已在 Dify Cloud 上建好知识库（dataset ID: `SpQIKc3RKWwtZrC4GWixaU6u`），提供了 API Key 和访问地址，要求将 Dify 作为主要 RAG 数据源。

## 2. 目标

### 核心目标
1. 集成 Dify Knowledge Base API 作为主要 RAG 数据源
2. 按用户设计的架构图对齐后端 Agent 管线
3. 保持现有功能不回退

### 非目标
- 不重构 Agent 继承链（BaseAgent → ReActAgent → ToolCallAgent → KkomaManus）
- 不修改前端代码
- 不更换 LLM Provider（继续用 DashScope）
- 不迁移数据库

## 3. 用户架构图解读

```
Chat 模式（左路径）:
  user prompt → rewrite agent → rag dify → brain agent → answer

Manus 模式（右路径）:
  user prompt → rewrite agent → rag dify → brain agent
    → [need tool?]
      → yes: tools agent → answer
      → no: answer (直接输出)
```

### 与现有代码的映射

| 架构节点 | 对应组件 | 改动级别 |
|----------|----------|----------|
| user prompt | `ChatInputContext` | 不变 |
| rewrite agent | `MultimodalIntakeService` | 微调（增强情感分类） |
| rag dify | `RagKnowledgeService` | **替换数据源为 Dify API** |
| brain agent (chat) | `LoveChatOrchestrator` + `LoveApp` | 微调（注入 Dify 结果） |
| brain agent (manus) | `CoachChatOrchestrator` + `CoachRoutingService` | 微调 |
| need tool? | `CoachRoutingService` | 不变 |
| tools agent | `KkomaManus` | 不变 |
| answer | SSE 流式输出 | 不变 |

## 4. 功能需求

### FR-1: Dify Knowledge Service

**描述**：新建 `DifyKnowledgeService`，封装 Dify Dataset Retrieve API 调用。

**行为**：
- 接收重写后的查询（来自 rewrite agent）
- 调用 `POST https://api.dify.ai/v1/datasets/{dataset_id}/retrieve`
- 使用 `hybrid_search` 方法，`top_k=4`
- 返回格式化的知识文本块（与现有 `RagKnowledgeService` 输出格式一致）
- 超时 10 秒，失败时返回空字符串（降级，不阻塞主流程）

**配置项**：
```yaml
dify:
  api:
    base-url: https://api.dify.ai/v1
    dataset-key: dataset-SpQIKc3RKWwtZrC4GWixaU6u
    dataset-id: SpQIKc3RKWwtZrC4GWixaU6u
    retrieve:
      search-method: hybrid_search
      top-k: 4
      score-threshold-enabled: false
      weights: 0.7
    timeout:
      connect-ms: 5000
      read-ms: 10000
    retry:
      max-attempts: 2
      backoff-ms: 1000
```

### FR-2: RAG 数据源切换

**描述**：修改 `RagKnowledgeService`（或创建适配层），将数据源从本地 VectorStore 切换到 Dify。

**行为**：
- 默认使用 Dify 作为 RAG 数据源
- 通过配置开关可回退到本地 VectorStore（`app.rag.source=dify|local`）
- 输出格式保持不变：用 `\n---\n` 分隔的知识文本块

### FR-3: 管线对齐

**描述**：确保两条管线（Chat / Manus）都按照 `rewrite → rag dify → brain → [tool decision]` 的顺序执行。

**当前已满足**：
- `LoveChatOrchestrator`：intake → rag → brain → answer
- `CoachChatOrchestrator`：intake → rag → route → brain/tools → answer

**需确认**：RAG 步骤使用新的 Dify 数据源。

### FR-4: 错误处理与降级

**描述**：Dify API 不可用时，系统应能优雅降级。

**降级策略**：
1. Dify API 超时/报错 → 日志告警 + 继续无 RAG 上下文
2. Dify 返回空结果 → 正常继续（brain agent 自行回答）
3. 网络不可达 → 快速失败（connect timeout 5s）+ 降级

## 5. 非功能需求

### NFR-1: 性能
- Dify 检索增加的延迟：< 500ms（p95）
- 不影响现有 SSE 流式体验

### NFR-2: 可配置性
- 所有 Dify 参数通过 `application-local.yml` 配置
- API Key 不硬编码，支持环境变量覆盖

### NFR-3: 可观测性
- 记录每次 Dify 检索的耗时和结果数量
- 记录检索失败和降级事件

### NFR-4: 安全
- API Key 仅在 `application-local.yml` 或环境变量中配置
- 不在日志中打印 API Key
- 查询内容不含用户隐私数据（经过 rewrite 后的语义查询）

## 6. 改动文件清单（预估）

| 文件 | 改动类型 | 说明 |
|------|----------|------|
| `ai/service/DifyKnowledgeService.java` | **新建** | Dify API 客户端 |
| `ai/service/DifyConfig.java` | **新建** | Dify 配置绑定 |
| `ai/service/RagKnowledgeService.java` | 修改 | 切换到 Dify 数据源 |
| `application-local.yml` | 修改 | 添加 Dify 配置 |
| `application-local.yml.template` | 修改 | 添加 Dify 配置模板 |
| `LoveChatOrchestrator.java` | 微调 | 确认使用新 RAG 服务 |
| `CoachChatOrchestrator.java` | 微调 | 确认使用新 RAG 服务 |

## 7. 交付标准

1. Chat 模式：rewrite → dify rag → brain → answer 链路畅通
2. Manus 模式：rewrite → dify rag → brain → tool decision → answer 链路畅通
3. Dify 返回的知识能正确注入到 LLM prompt 中
4. Dify 不可用时优雅降级（不崩溃，日志有告警）
5. 配置项完整，无硬编码

## 8. 风险

| 风险 | 影响 | 缓解 |
|------|------|------|
| Dify Cloud 不稳定 | RAG 功能不可用 | 降级策略 + 保留本地 VectorStore 回退 |
| 速率限制（Sandbox 10 req/min） | 并发受限 | 配置可调，后续可升级计划 |
| 网络延迟 | 响应变慢 | 超时配置 + 异步检索 |
| 知识库内容质量 | 回答质量下降 | 依赖用户在 Dify 中维护内容 |

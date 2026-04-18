# PRD: Lovemaster 后端优化 - Dify RAG 集成

> 版本：v1.1
> 日期：2026-04-19
> 关联文档：
> - Architecture: `output/backend-optimize-architecture.md`
> - UI/UX: `output/backend-optimize-uiux.md`
> - 执行计划: `.super-dev/changes/release-hardening-finalization/tasks.md`

## 0. 目标用户与用户画像

### 目标用户
Lovemaster 的终端用户为 18-35 岁、有恋爱/社交困惑的华语年轻用户。他们通过 App 进行情感咨询、聊天话术分析和恋爱教练辅导。

### 用户画像 (Persona)
| 维度 | 描述 |
|:---|:---|
| 核心痛点 | 不知道如何回复对方消息、缺乏恋爱策略、情绪处理困难 |
| 使用场景 | 手机端为主，碎片化时间使用，期望即时反馈 |
| 技术敏感度 | 低 — 不关心后端技术，只关心回答质量和响应速度 |
| 成功标准 | 获得实用建议、关系有实质进展、愿意持续使用 |

### 核心场景 (User Stories)
1. **恋爱咨询**: 用户描述恋爱困惑，AI 基于知识库给出专业建议。
2. **聊天分析**: 用户上传聊天截图，AI 分析对方意图并给出回复建议。
3. **教练辅导**: 用户需要深度策略（如邀约技巧、关系推进），AI 调用工具执行复杂任务。

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

### 2.3 分阶段演进（Step 1-4）

| Step | 目标 | 核心能力 | 当前决策 |
|------|------|----------|----------|
| Step 1 | 快速可用 | 本地 Wiki 快检 + Dify 并行 Merge | 已落地 |
| Step 2 | 知识演化 | 经验蒸馏管道（分类 → 抽象 → Merge → 离线归纳） | 本轮重点 |
| Step 3 | 策略优化 | 策略反馈闭环（成功率建模与排序） | 独立阶段，不与 Step 2 混做 |
| Step 4 | 召回增强 | 向量检索 + 强图扩展（按指标触发） | 预留 |

关键取舍：
1. 反馈闭环不并入 Step 2。Step 2 只采集反馈信号，不计算策略成功率。
2. Topic 分类采用 YAML 配置驱动，不采用硬编码枚举；分类必须被 schema 强约束。
3. Topic 粒度采用“先粗后细”，先保证可统计，再按数据规模拆细分层。

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
| rag dify | `RagKnowledgeService` | **升级为 Wiki + Dify 并行 merge** |
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

### FR-2: RAG 双路并行检索与 Merge

**描述**：修改 `RagKnowledgeService`，并行检索本地 Wiki 与 Dify，再按阈值策略 merge 输出。

**行为**：
- Wiki 与 Dify 并行 fanout，受总超时控制。
- 当 Wiki 置信度达标时采用 wiki-first，并补齐 dify 片段。
- 当 Wiki 置信度偏低时采用 dify-primary。
- 双路都 miss 时返回空上下文，不阻断主流程。
- 输出格式保持不变：用 `\n---\n` 分隔的知识文本块。

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

### FR-5: Step 2 经验蒸馏管道（append → merge）

**描述**：把高价值对话沉淀为可复用经验块，写入既有 Topic 结构，而不是追加原始 Q&A 日志。

**6 段流水线**：
1. 触发筛选：点赞、Kiko 高分（默认阈值 0.82）、同问法高复用。
2. Topic 分类：按 `阶段 × 意图 × 问题` 归类。
3. 抽象重写：从对话提炼“场景/成因/策略/禁忌”。
4. Topic Merge：定位已有 topic section 更新，不重复 append。
5. 离线批处理：按批次做多样本归纳与去重。
6. 人工抽检：抽样审核后再入正式 Wiki。

**验收要求**：
- 写回内容必须是抽象经验块，不允许直接落原始对话。
- 重复内容需在 Merge 前被阻断（如文本相似度/Jaccard 阈值）。

### FR-6: Topic Schema（配置驱动 + 强约束 + 校验层）

**描述**：Topic 分类体系采用 YAML 配置驱动，并将分类空间限制在有限状态集合内。

**必备机制**：
1. YAML schema（含 `version`、`stages`、`intents`、`problems`）。
2. LLM 分类只能从给定枚举中选择，不允许自由生成。
3. 校验层兜底：若输出不在 schema 中，降级到 `unknown` 并进入人工处理。
4. 粒度治理：第一版仅保留粗粒度阶段，后续按数据证据细分。

### FR-7: Step 2 反馈信号采集（只采集，不计算）

**描述**：Step 2 建立反馈埋点，保证后续策略评估有可追溯数据。

**采集字段示例**：
```json
{
  "strategy_id": "xxx",
  "context": "...",
  "user_response": "...",
  "signals": {
    "continued_chat": true,
    "response_latency": 12,
    "sentiment_shift": "positive"
  }
}
```

**边界**：
- Step 2 不输出策略成功率，不做策略排序，不做在线推荐。

### FR-8: Step 3 策略反馈闭环（独立上线）

**描述**：在 Step 2 数据达标后，独立上线策略评估与推荐能力。

**能力范围**：
1. 成功率建模（按 Topic 分桶）。
2. 同 Topic 下策略排序。
3. 灰度推荐与回滚开关。
4. 管理看板（只读）用于策略洞察。

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
| `config/DifyClientConfig.java` | **新建** | Dify HTTP 客户端配置 |
| `ai/service/RagKnowledgeService.java` | 修改 | Wiki + Dify fanout merge |
| `ai/service/WikiKnowledgeService.java` | **新建** | 本地 Wiki 快检与图扩展 |
| `ai/service/TopicSchemaService.java` | **新建** | YAML Topic schema 加载与分类 |
| `ai/service/KnowledgeSinkService.java` | **新建** | 蒸馏候选写入与状态流转 |
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
6. Step 2 经验蒸馏仅写入抽象经验块，Topic 采用 merge 策略
7. Step 2 反馈事件可追溯落库，但不计算成功率
8. Topic 分类错误可回退为 `unknown`，并可人工复核

## 8. 验收标准 (Acceptance Criteria)

### AC-1: Chat 模式端到端 (Given-When-Then)
**Given** 用户输入恋爱咨询问题  
**When** 系统执行 `rewrite → rag dify → brain → answer` 管线  
**Then** SSE 流式返回包含 Dify 知识引用的回答，延迟 < 3s

### AC-2: Manus 模式端到端
**Given** 用户输入需要工具执行的复杂问题  
**When** 系统执行 `rewrite → rag dify → brain → tool decision → tools → answer` 管线  
**Then** 正确识别需要工具调用，执行工具后返回结果，全程 SSE 流式

### AC-3: Dify 降级
**Given** Dify API 不可用或超时  
**When** 系统执行检索  
**Then** 不抛异常，日志记录告警，Brain agent 自主回答

### AC-4: Step 2 候选写入
**Given** 用户点击 SaveToWikiButton 或 Kiko 高分自动触发  
**When** 系统执行 Topic 分类和候选写入  
**Then** 写入 `wiki_candidate` 表，状态为 `pending_review` 或 `unknown_topic`

### AC-5: Topic 分类约束
**Given** LLM 分类结果超出 YAML schema 枚举范围  
**When** 校验层执行检查  
**Then** 自动 fallback 到 `unknown`，不写入错误分类

## 9. 优先级

| 优先级 | 需求项 | 说明 |
|:---|:---|:---|
| **P0** | FR-1 Dify Knowledge Service | 核心能力，阻塞所有下游 |
| **P0** | FR-2 RAG 双路并行检索 | 核心能力，阻塞管线对齐 |
| **P0** | FR-3 管线对齐 | 确保 Chat/Manus 两条路径正确 |
| **P0** | FR-4 错误处理与降级 | 生产可用性的底线 |
| **P1** | FR-5 Step 2 经验蒸馏管道 | 本轮重点，但不阻塞核心管线 |
| **P1** | FR-6 Topic Schema | Step 2 的前置依赖 |
| **P1** | FR-7 反馈信号采集 | Step 2 配套 |
| **P2** | FR-8 Step 3 策略闭环 | 独立阶段，不与本轮混做 |

## 10. 三文档一致性声明

本 PRD 的决策已在以下文档中得到架构和 UI 层面的承接：

| PRD 决策 | Architecture 对应 | UI/UX 对应 |
|:---|:---|:---|
| FR-1/2 Dify RAG 集成 | §3.1 DifyKnowledgeService + §4.1 RagKnowledgeService 改造 | — (后端无感知) |
| FR-3 管线对齐 | §2.1/2.2 目标架构数据流 | — |
| FR-5 Step 2 蒸馏 | §3.5 TopicSchemaService + §3.6 KnowledgeSinkService | §2.1 SaveToWikiButton + §2.3 Admin 审核入口 |
| FR-6 Topic Schema | §3.5 YAML schema + 校验层 | §3 Topic 分类前端呈现约束 |
| FR-7 反馈采集 | §3.6 反馈埋点策略 | §2.2 自动反馈埋点提示 |
| NFR-1 性能 | §6 数据流详图 (超时控制) | — |
| NFR-4 安全 | §3.4 WebClient 配置 (API Key 管理) | — |

## 11. 风险

| 风险 | 影响 | 缓解 |
|------|------|------|
| Dify Cloud 不稳定 | 召回覆盖下降 | 保留本地 Wiki 快检 + 空上下文降级 |
| 速率限制（Sandbox 10 req/min） | 并发受限 | 配置可调，后续可升级计划 |
| 网络延迟 | 响应变慢 | 超时配置 + 异步检索 |
| 知识库内容质量 | 回答质量下降 | 依赖用户在 Dify 中维护内容 |
| 分类漂移 | Topic 不可统计、不可回溯 | YAML 枚举约束 + 校验层 fallback 到 `unknown` |
| Reinforcement bias | 错误策略被高频行为误判为有效 | Step 2 仅埋点，Step 3 再引入评分模型 |
| 冗余爆炸 | 同义问法生成重复知识 | Topic Merge + 批处理去重阈值 |

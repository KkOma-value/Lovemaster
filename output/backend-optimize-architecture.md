# Architecture: Lovemaster 后端优化 - Dify RAG 集成

> 版本：v1.1
> 日期：2026-04-19
> 关联文档：
> - PRD: `output/backend-optimize-prd.md`
> - UI/UX: `output/backend-optimize-uiux.md`
> - ADR: `docs/adr/ADR-001-选择-postgresql-作为数据库.md`

## 1. 架构目标

本架构文档承接 PRD 中定义的功能需求（FR-1 ~ FR-8）和非功能需求（NFR-1 ~ NFR-4），提供具体的技术实现方案。所有架构决策均已通过 ADR 记录（见 `docs/adr/`）。

在现有后端基础上，最小化改动实现：

1. 用 Dify Knowledge Base API 替换本地 SimpleVectorStore 作为主要 RAG 数据源
2. 对齐用户设计的 `Rewrite → RAG Dify → Brain → Tool Decision` 管线
3. 保留降级回退能力

核心原则：
- 最小改动，最大效果
- 不重构 Agent 继承链
- 不修改前端
- 可配置切换 RAG 数据源

---

## 2. 目标架构

### 2.1 Chat 模式（恋爱咨询）

```
user prompt
    │
    ▼
┌──────────────────┐
│  rewrite agent   │  ← MultimodalIntakeService（已有）
│  (OCR + 重写)    │
└────────┬─────────┘
         │ rewrittenQuestion
         ▼
┌──────────────────┐
│    rag dify      │  ← DifyKnowledgeService（新建）
│  (知识检索)      │     POST /v1/datasets/{id}/retrieve
└────────┬─────────┘
         │ knowledgeContext
         ▼
┌──────────────────┐
│   brain agent    │  ← LoveApp.doChatWithRAGContext()（已有）
│  (生成回答)      │     注入 RAG 上下文到 system prompt
└────────┬─────────┘
         │
         ▼
      answer (SSE)
```

### 2.2 Manus 模式（教练/执行）

```
user prompt
    │
    ▼
┌──────────────────┐
│  rewrite agent   │  ← MultimodalIntakeService（已有）
│  (OCR + 重写)    │
└────────┬─────────┘
         │ rewrittenQuestion
         ▼
┌──────────────────┐
│    rag dify      │  ← DifyKnowledgeService（新建）
│  (知识检索)      │
└────────┬─────────┘
         │ knowledgeContext
         ▼
┌──────────────────┐
│   brain agent    │  ← CoachRoutingService（已有）
│  (路由决策)      │
└────────┬─────────┘
         │
    ┌────┴────┐
    │  need   │
    │  tool?  │
    └────┬────┘
    no   │   yes
    │    │    │
    ▼    │    ▼
 answer  │ ┌──────────────┐
 (直接)  │ │ tools agent  │  ← KkomaManus（已有）
         │ │ (工具执行)   │
         │ └──────┬───────┘
         │        │
         ▼        ▼
      answer (SSE)
```

### 2.3 四步走演进架构

```
Step 1  本地快检 + Dify 并行 Merge
    - WikiKnowledgeService (内存索引, 200ms 内返回)
    - DifyKnowledgeService (网络检索兜底)
    - RagKnowledgeService 统一 merge (wiki-first + threshold)

Step 2  经验蒸馏管道（本轮落地重点）
    - Trigger Filter -> Topic 分类 -> 抽象重写 -> Topic Merge
    - 离线批处理归纳 -> 人工抽检 -> 写回 wiki/topics/*
    - 反馈信号只采集，不计算策略有效性

Step 3  策略反馈闭环（独立阶段）
    - 读取 Step 2 的 feedback_event
    - 按 Topic 计算策略分
    - 策略排序与灰度推荐

Step 4  向量 + 强图扩展（指标触发）
    - 当本地命中率持续偏低时启用 PgVector
    - 2-hop 图扩展 + 重排
```

### 2.4 Step 2 边界定义

1. Step 2 只做“知识蒸馏 + 信号采集”，不做策略成功率计算。
2. Topic 分类空间固定为 `阶段 × 意图 × 问题`，由 YAML schema 管理。
3. 分类输出必须经过校验层，越界值统一 fallback 到 `unknown`。
4. Topic 粒度遵循“先粗后细”，优先保证统计稳定性。

---

## 3. 新增组件设计

### 3.1 DifyKnowledgeService

**职责**：封装 Dify Dataset Retrieve API 调用。

```java
@Service
public class DifyKnowledgeService {

    private final WebClient difyClient;
    private final DifyProperties properties;

    /**
     * 从 Dify 知识库检索相关文档
     * @param query 重写后的查询（来自 rewrite agent）
     * @return 格式化的知识文本块，用 \n---\n 分隔；失败时返回空字符串
     */
    public String retrieve(String query) {
        // 1. 构建请求体
        // 2. POST /v1/datasets/{datasetId}/retrieve
        // 3. 解析响应，提取 records[].segment.content
        // 4. 用 \n---\n 拼接返回
        // 5. 异常处理：超时/网络错误 → 日志告警 + 返回 ""
    }
}
```

**关键实现细节**：

```java
// 请求体构建
Map<String, Object> retrievalModel = Map.of(
    "search_method", properties.getSearchMethod(),  // "hybrid_search"
    "reranking_enable", false,
    "reranking_mode", null,  // JSONObject 需要处理 null
    "reranking_model", Map.of(
        "reranking_provider_name", "",
        "reranking_model_name", ""
    ),
    "weights", properties.getWeights(),              // 0.7
    "top_k", properties.getTopK(),                   // 4
    "score_threshold_enabled", false,
    "score_threshold", null
);

Map<String, Object> body = Map.of(
    "query", query,
    "retrieval_model", retrievalModel
);

// HTTP 调用
String result = difyClient.post()
    .uri("/datasets/{datasetId}/retrieve", properties.getDatasetId())
    .bodyValue(body)
    .retrieve()
    .bodyToMono(DifyRetrieveResponse.class)
    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
    .map(this::formatResponse)
    .onErrorResume(e -> {
        log.warn("Dify retrieval failed: {}", e.getMessage());
        return Mono.just("");
    })
    .block();
```

### 3.2 DifyProperties（配置绑定）

```java
@ConfigurationProperties(prefix = "dify.api")
public class DifyProperties {
    private String baseUrl = "https://api.dify.ai/v1";
    private String datasetKey;
    private String datasetId;
    private String searchMethod = "hybrid_search";
    private int topK = 4;
    private double weights = 0.7;
    private boolean scoreThresholdEnabled = false;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
    private int retryMaxAttempts = 2;
    private int retryBackoffMs = 1000;
}
```

### 3.3 DifyRetrieveResponse（响应 DTO）

```java
public record DifyRetrieveResponse(
    DifyQuery query,
    List<DifyRecord> records
) {
    public record DifyQuery(String content) {}
    public record DifyRecord(DifySegment segment, double score) {}
    public record DifySegment(
        String id,
        String documentId,
        String content,
        int wordCount,
        int tokens,
        List<String> keywords
    ) {}
}
```

### 3.4 WebClient 配置

```java
@Configuration
@EnableConfigurationProperties(DifyProperties.class)
public class DifyClientConfig {

    @Bean
    public WebClient difyWebClient(DifyProperties properties) {
        return WebClient.builder()
            .baseUrl(properties.getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION,
                "Bearer " + properties.getDatasetKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON_VALUE)
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(2 * 1024 * 1024))
            .build();
    }
}
```

### 3.5 TopicSchemaService（Step 2）

**职责**：加载并缓存 Topic YAML schema，为蒸馏候选提供有限状态分类结果。

```yaml
# knowledge/wiki/topic-schema.yml
version: v1.0
stages:
    - 破冰期
    - 升温期
    - 冷淡期
    - 挽回期
intents:
    - 聊天
    - 邀约
    - 情绪处理
    - 关系推进
problems:
    - 不回复
    - 冷淡
    - 拒绝
    - 暧昧不清
```

**关键机制**：
1. schema 文件变化自动重载，带版本号。
2. 分类结果必须命中枚举；未命中统一写为 `unknown`。
3. schema 缺失或损坏时自动回退到内置 fallback schema。

### 3.6 KnowledgeSinkService（Step 2）

**职责**：接收候选知识请求，执行 Topic 分类并写入 `wiki_candidate`，供后续离线蒸馏任务处理。

**写入策略**：
1. 输入校验：`chatId`、`answer` 必填。
2. 分类校验：若 `unknown_topic=true`，状态置为 `unknown_topic`。
3. 当前仅保存轻量摘要，后续由离线任务生成结构化经验块并 merge 到 Topic。

**反馈埋点策略（Step 2）**：
1. 保存行为事件（继续对话、延迟、情绪变化、点赞/点踩等）。
2. 不计算 success rate，不做策略排序，不做在线推荐。
3. 评分模型留到 Step 3 独立上线。

---

## 4. 现有组件改动

### 4.1 RagKnowledgeService 改造

**改造策略**：并行 fanout 检索 Wiki + Dify，再按 wiki-first 规则 merge。

```java
@Service
public class RagKnowledgeService {

    private final DifyKnowledgeService difyKnowledgeService;
    private final WikiKnowledgeService wikiKnowledgeService;
    private final DifyProperties difyProperties;
    private final KnowledgeProperties knowledgeProperties;
    private final Executor knowledgeExecutor;

    public String retrieveKnowledge(String query) {
        CompletableFuture<WikiKnowledgeResult> wikiFuture = CompletableFuture
                .supplyAsync(() -> wikiKnowledgeService.retrieveKnowledge(query), knowledgeExecutor)
                .completeOnTimeout(WikiKnowledgeResult.empty(), wikiTimeoutMs, TimeUnit.MILLISECONDS);

        CompletableFuture<String> difyFuture = CompletableFuture
                .supplyAsync(() -> difyKnowledgeService.retrieveKnowledge(query), knowledgeExecutor)
                .exceptionally(ex -> "");

        CompletableFuture.allOf(wikiFuture, difyFuture)
                .completeOnTimeout(null, totalTimeoutMs, TimeUnit.MILLISECONDS)
                .join();

        WikiKnowledgeResult wiki = wikiFuture.getNow(WikiKnowledgeResult.empty());
        String dify = difyFuture.getNow("");
        return merge(wiki, dify).content();
    }
}
```

**merge 规则**：
1. `wiki.topScore >= threshold` 时优先采用 wiki，并补齐 dify 段落。
2. wiki 分数偏低但 dify 有结果时，走 dify-primary。
3. 两路都 miss 返回空字符串，由上游 Brain 自主回答。

### 4.2 LoveChatOrchestrator

改动最小 — `RagKnowledgeService` 内部已切换数据源，Orchestrator 调用方式不变：

```java
// 现有代码（不变）
String knowledge = ragKnowledgeService.retrieveKnowledge(rewrittenQuestion);
```

仅需确保传入的是 rewrite agent 输出的 `rewrittenQuestion`（当前已是如此）。

### 4.3 CoachChatOrchestrator

同 LoveChatOrchestrator，调用方式不变。

---

## 5. 配置变更

### 5.1 application-local.yml 新增

```yaml
# Dify Knowledge Base Configuration
dify:
  api:
    base-url: https://api.dify.ai/v1
    dataset-key: dataset-SpQIKc3RKWwtZrC4GWixaU6u
    dataset-id: SpQIKc3RKWwtZrC4GWixaU6u
    search-method: hybrid_search
    top-k: 4
    weights: 0.7
    score-threshold-enabled: false
    connect-timeout-ms: 5000
    read-timeout-ms: 10000
    retry-max-attempts: 2
    retry-backoff-ms: 1000

# Knowledge Step 2 Configuration
app:
    knowledge:
        fanout:
            total-timeout-ms: 800
        wiki:
            retrieval:
                timeout-ms: 200
                score-threshold: 0.6
        sink:
            candidate-kiko-threshold: 0.82
            repeat-hit-threshold: 3
            topic-schema-path: knowledge/wiki/topic-schema.yml
            unknown-label: unknown
```

### 5.2 application-local.yml.template 新增

```yaml
# Dify Knowledge Base Configuration
dify:
  api:
    base-url: https://api.dify.ai/v1
    dataset-key: ${DIFY_DATASET_API_KEY}
    dataset-id: ${DIFY_DATASET_ID}

app:
    knowledge:
        sink:
            topic-schema-path: ${APP_KNOWLEDGE_SINK_TOPIC_SCHEMA_PATH:knowledge/wiki/topic-schema.yml}
```

---

## 6. 数据流详图

### 6.1 Chat 模式完整数据流

```
1. AiController.doChatWithLoveAppSSE(message, chatId, imageUrl)
   │
2. LoveChatOrchestrator.stream(context)
   │
   ├─ 3. MultimodalIntakeService.analyze(context)
   │     → IntakeAnalysisResult { rewrittenQuestion, ... }
   │
    ├─ 4. RagKnowledgeService.retrieveKnowledge(rewrittenQuestion)
    │     │
    │     ├─ WikiKnowledgeService.retrieveKnowledge(query)
    │     ├─ DifyKnowledgeService.retrieveKnowledge(query)
    │     └─ merge(wiki-first + threshold)
    │           → "知识块1\n---\n知识块2\n---\n知识块3"
   │
   ├─ 5. Build systemContext = intake + ragKnowledge
   │
   ├─ 6. LoveApp.doChatWithRAGContext(original, systemContext, chatId)
   │     → SSE content events
   │
   └─ 7. SSE done event
```

### 6.2 Manus 模式完整数据流

```
1. AiController.doChatWithManus(message, chatId, imageUrl)
   │
2. CoachChatOrchestrator.stream(context)
   │
   ├─ 3. MultimodalIntakeService.analyze(context)
   │     → IntakeAnalysisResult { rewrittenQuestion, likelyNeedTools, ... }
   │
    ├─ 4. RagKnowledgeService.retrieveKnowledge(rewrittenQuestion)
    │     └─ Wiki + Dify fanout merge [同上]
   │
   ├─ 5. CoachRoutingService.route(intakeResult, ragKnowledge)
   │     → CoachRoutingDecision { shouldUseTools, ... }
   │
   ├─ 6a. [shouldUseTools = false]
   │       → Direct answer via dashscopeChatModel
   │
   └─ 6b. [shouldUseTools = true]
           → KkomaManus.runStream(toolTaskPrompt)
              → tool_call / content / file_created events
```

### 6.3 Step 2 经验蒸馏数据流

```
1. 用户对话结束 / 触发条件命中（点赞、Kiko 高分、同问法复用）
    │
2. KnowledgeSinkService.createCandidate(...)
    │
    ├─ TopicSchemaService.classify(question, answer)
    │    ├─ YAML schema 约束分类
    │    └─ 越界值 fallback -> unknown
    │
    └─ 写入 wiki_candidate（pending_review / unknown_topic）
          │
3. 离线任务（Cron 每日或每 6h）
    │
    ├─ 聚合同 Topic 多样本
    ├─ 抽象重写（场景/成因/策略/禁忌）
    ├─ Topic Merge（更新已有 section，不 append 原始问答）
    └─ 抽样审核通过后写回 wiki/topics/*
```

### 6.4 Step 3 策略反馈闭环数据流（预留）

```
1. 读取 Step 2 累积 feedback_event + wiki_candidate 关联
2. 按 Topic/策略聚合有效性指标（继续对话率、反馈正向率、延迟改善等）
3. 输出 strategy_score 并生成排序
4. 灰度发布推荐策略（可随时回滚）
```

---

## 7. 错误处理设计

```
Step 1 检索（Wiki + Dify fanout）
    │
    ├─ Wiki 命中且 score >= threshold
    │     → wiki-first merge（补充 dify 片段）
    ├─ Wiki 低分且 Dify 可用
    │     → dify-primary
    └─ 双路 miss / 超时
          → 返回空上下文，brain 自主回答

Step 2 蒸馏
    │
    ├─ Topic 分类越界
    │     → fallback 到 unknown + 待人工归类
    ├─ 内容相似度过高
    │     → 阻断 append，执行 merge 或丢弃
    └─ 抽样审核不通过
          → 不写回正式 wiki

Step 3 评分（未来）
    │
    ├─ 数据不足
    │     → 不计算评分，不触发推荐
    └─ 评分异常波动
          → 回滚到只读看板，不影响线上回答路径
```

---

## 8. 包结构

```
src/main/java/org/example/springai_learn/
  ai/
    service/
            DifyKnowledgeService.java          ← 新建
            RagKnowledgeService.java           ← 修改（Wiki + Dify fanout merge）
            WikiKnowledgeService.java          ← 新建（本地 Wiki 快检）
            TopicSchemaService.java            ← 新建（YAML 分类）
            KnowledgeSinkService.java          ← 新建（候选入池）
      MultimodalIntakeService.java ← 不变
      CoachRoutingService.java     ← 不变
    config/
      DifyClientConfig.java        ← 新建
      DifyProperties.java          ← 新建
            KnowledgeProperties.java     ← 新建
    dto/
      DifyRetrieveResponse.java    ← 新建
            KnowledgeCandidateRequest.java   ← 新建
            KnowledgeCandidateResponse.java  ← 新建
    orchestrator/
      LoveChatOrchestrator.java    ← 微调
      CoachChatOrchestrator.java   ← 微调
    auth/
        entity/
            WikiCandidate.java           ← 新建
        repository/
            WikiCandidateRepository.java ← 新建
    controller/
        KnowledgeController.java       ← 新建
```

---

## 9. 测试策略

### 单元测试

| 测试 | 验证点 |
|------|--------|
| `DifyKnowledgeServiceTest` | 请求体构建正确、响应解析正确、超时降级 |
| `RagKnowledgeServiceTest` | Wiki + Dify fanout merge、超时与阈值策略 |
| `WikiKnowledgeServiceTest` | Wiki 索引构建、图扩展、片段预算控制 |
| `TopicSchemaServiceTest` | YAML 加载、fallback、unknown 校验 |
| `KnowledgeSinkServiceTest` | 候选写入、状态流转、摘要生成 |

### 集成测试

| 测试 | 验证点 |
|------|--------|
| Dify API 连通性 | 使用真实 dataset-id 查询，验证返回结构 |
| Chat 模式端到端 | rewrite → dify rag → brain → SSE 响应 |
| Manus 模式端到端 | rewrite → dify rag → route → answer/tools |

### 手动验证

| 场景 | 验证点 |
|------|--------|
| 正常查询 | Dify 返回相关知识，LLM 回答有参考 |
| Dify 不可用 | 系统降级，仍能正常回答 |
| 空结果 | Brain agent 自行回答，无报错 |

---

## 10. 实施顺序

| 步骤 | 内容 | 依赖 |
|------|------|------|
| 1 | 新建 `DifyProperties` + `DifyClientConfig` | 无 |
| 2 | 新建 `DifyRetrieveResponse` DTO | 无 |
| 3 | 新建 `DifyKnowledgeService` | 步骤 1, 2 |
| 4 | 新建 `WikiKnowledgeService` + `KnowledgeProperties` | 无 |
| 5 | 修改 `RagKnowledgeService`（Wiki + Dify fanout merge） | 步骤 3, 4 |
| 6 | 新建 `TopicSchemaService` + `KnowledgeSinkService`（Step 2） | 步骤 4 |
| 7 | 修改配置文件（`application.yml` / `application-local.yml`） | 步骤 1, 4, 6 |
| 8 | 编写/补齐单元测试 | 步骤 3, 5, 6 |
| 9 | 端到端验证（检索 + 候选写入） | 步骤 5, 7 |

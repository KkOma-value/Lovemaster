# Architecture: Lovemaster 后端优化 - Dify RAG 集成

> 版本：v1.0
> 日期：2026-03-29

## 1. 架构目标

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

---

## 4. 现有组件改动

### 4.1 RagKnowledgeService 改造

**改造策略**：注入 `DifyKnowledgeService`，通过配置决定使用哪个数据源。

```java
@Service
public class RagKnowledgeService {

    private final DifyKnowledgeService difyKnowledgeService;
    private final VectorStore vectorStore;  // 保留，用于降级

    @Value("${app.rag.source:dify}")
    private String ragSource;

    public String retrieveKnowledge(String query) {
        if ("dify".equals(ragSource)) {
            String result = difyKnowledgeService.retrieve(query);
            if (!result.isEmpty()) {
                return result;
            }
            // Dify 失败，尝试本地降级
            log.warn("Dify retrieval returned empty, falling back to local");
        }
        return retrieveFromLocalVectorStore(query);
    }
}
```

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

# RAG Source Switch
app:
  rag:
    source: dify  # dify | local
```

### 5.2 application-local.yml.template 新增

```yaml
# Dify Knowledge Base Configuration
dify:
  api:
    base-url: https://api.dify.ai/v1
    dataset-key: ${DIFY_DATASET_API_KEY}
    dataset-id: ${DIFY_DATASET_ID}
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
   │     └─ DifyKnowledgeService.retrieve(rewrittenQuestion)
   │           │
   │           └─ POST https://api.dify.ai/v1/datasets/SpQIKc.../retrieve
   │                 { query: "...", retrieval_model: { ... } }
   │                 → records[].segment.content
   │                 → "知识块1\n---\n知识块2\n---\n知识块3"
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
   │     └─ DifyKnowledgeService.retrieve(rewrittenQuestion)  [同上]
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

---

## 7. 错误处理设计

```
Dify API 调用
    │
    ├─ 200 + records.isEmpty()
    │     → 返回 ""（无匹配知识）
    │     → brain agent 自行回答
    │
    ├─ 200 + records.size() > 0
    │     → 格式化返回知识块
    │
    ├─ 401 Unauthorized
    │     → log.error("Dify auth failed")
    │     → 降级到本地 VectorStore
    │
    ├─ 400 Bad Request
    │     → log.warn("Dify bad request: {}")
    │     → 降级到本地 VectorStore
    │
    ├─ 429 Rate Limited
    │     → log.warn("Dify rate limited")
    │     → 降级到本地 VectorStore
    │
    ├─ 5xx Server Error
    │     → 重试（max 2 次，指数退避）
    │     → 仍失败 → 降级到本地 VectorStore
    │
    └─ Timeout (connect 5s / read 10s)
          → log.warn("Dify timeout")
          → 降级到本地 VectorStore
```

---

## 8. 包结构

```
src/main/java/org/example/springai_learn/
  ai/
    service/
      DifyKnowledgeService.java    ← 新建
      RagKnowledgeService.java     ← 修改（注入 Dify）
      MultimodalIntakeService.java ← 不变
      CoachRoutingService.java     ← 不变
    config/
      DifyClientConfig.java        ← 新建
      DifyProperties.java          ← 新建
    dto/
      DifyRetrieveResponse.java    ← 新建
    orchestrator/
      LoveChatOrchestrator.java    ← 微调
      CoachChatOrchestrator.java   ← 微调
```

---

## 9. 测试策略

### 单元测试

| 测试 | 验证点 |
|------|--------|
| `DifyKnowledgeServiceTest` | 请求体构建正确、响应解析正确、超时降级 |
| `RagKnowledgeServiceTest` | Dify 模式切换、降级到本地 |

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
| 4 | 修改 `RagKnowledgeService`（注入 Dify） | 步骤 3 |
| 5 | 修改配置文件（`application-local.yml`） | 步骤 1 |
| 6 | 编写测试 | 步骤 3, 4 |
| 7 | 端到端验证 | 步骤 4, 5 |

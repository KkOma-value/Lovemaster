# Lovemaster

Lovemaster 是一个面向恋爱陪伴与恋爱教练场景的全栈 AI 应用。

- 后端：Spring Boot 3.4.5 + Spring AI + Java 21
- 前端：React 19 + Vite 7，位于 `springai-front-react/`
- MCP Server：独立 Spring Boot 模块，位于 `mcp-servers/`
- 检索增强：Dify Dataset API
- 图片存储：Supabase Storage

## 当前模式设计

项目当前有两种核心聊天模式：

### 1. Love 模式

定位：恋爱陪伴、纯聊天、直接给建议。

当前链路：

`MultimodalIntakeService -> RagKnowledgeService -> LoveChatOrchestrator -> LoveApp`

特点：

- 支持文本和聊天截图输入
- 先做截图理解、OCR 摘录、问题重写
- 会查 Dify 知识库补充上下文
- 最终以陪伴式对话直接回答用户
- 不进入工具调用链路

### 2. Coach 模式

定位：恋爱教练，既支持闲聊，也支持调用工具解决问题。

当前链路：

`MultimodalIntakeService -> RagKnowledgeService -> BrainAgentService -> (direct answer | ToolsAgentService) -> BrainAgentService`

特点：

- 先做输入理解与问题重写
- BrainAgent 先判断是否真的需要工具
- 不需要工具时直接回答
- 需要工具时，再进入 ToolsAgent 执行工具任务
- 工具完成后由 BrainAgent 综合结果，生成最终回答

这就是当前已经落地的"先思考，后行动"实现。

### 3. Kiko AI 概率分析

定位：恋爱成功概率评估，基于用户对话上下文给出结构化分析报告。

流程：

- 当 Intake 检测到用户询问"成功率""有没有戏"等意图时，触发 `ProbabilityAnalysisService`
- 调用 AI 模型进行结构化输出（`ProbabilityAnalysis` record）
- 返回包含概率值、分档、置信度、正面/风险信号、下一步行动建议的概率卡片

概率卡片结构：

| 字段 | 说明 |
|---|---|
| `probability` | 0-100 的概率值 |
| `tier` | 分档：极低 / 偏低 / 一般 / 较高 / 很高 |
| `confidence` | 置信度：low / medium / high |
| `summary` | 1-2 句口语化总结 |
| `greenFlags` | 正面信号列表（≥2 条，含证据和权重） |
| `redFlags` | 风险信号列表（≥1 条，含证据和权重） |
| `nextActions` | 下一步行动建议（恰好 3 条） |

## 核心架构

### Multimodal Intake

[`MultimodalIntakeService`](./src/main/java/org/example/springai_learn/ai/service/MultimodalIntakeService.java)

职责：

- 识别图片和聊天截图
- 提取 OCR 关键信息
- 重写用户问题
- 输出结构化分析结果 `IntakeAnalysisResult`

### Brain Agent

[`BrainAgentService`](./src/main/java/org/example/springai_learn/ai/service/BrainAgentService.java)

职责：

- 在 Coach 模式下决定是否需要工具
- 不需要工具时直接回答
- 需要工具时生成工具任务描述
- 在工具执行后综合工具结果

### Tools Agent

[`ToolsAgentService`](./src/main/java/org/example/springai_learn/ai/service/ToolsAgentService.java)

职责：

- 接收 Brain 的任务
- 合并本地工具与 MCP 工具
- 使用 `ChatClient + ToolCallingManager` 执行多轮工具调用
- 支持预算控制、文件事件推送、会话记忆持久化

## 技术栈

### 后端

- Spring Boot 3.4.5
- Spring AI 1.0.0-M6
- Java 21
- PostgreSQL
- Spring JDBC

### AI 模型

- NVIDIA NIM OpenAI-compatible endpoint
  - `rewriteModel`: Qwen3.5 VLM 122B
  - `toolsModel`: DeepSeek-R1
  - `brainModel`: Kimi-K2-Thinking
- 阿里云 DashScope
- LangChain4j DashScope 社区版
- Spring AI MCP Client

### 前端

- React 19
- Vite 7
- Framer Motion
- React Router
- Three.js

### 存储与服务

- Dify Dataset API
- Supabase Storage（对话图片存储）
- iText 9.1.0
- JSoup
- Hutool
- Knife4j
- Lombok
- Graphify（代码知识图谱）

## 主要能力

- 文本聊天与截图聊天
- Love / Coach 双模式
- **Kiko AI 恋爱成功概率分析** — 基于对话上下文评估追求成功的概率，输出结构化概率卡片
- Dify 知识库召回
- SSE 流式响应
- 工具调用与多步执行
- 后台运行状态指示器 — 页面切换时后台对话继续运行，返回后自动恢复
- 会话持久化
- JWT 登录与 Google OAuth
- 图片上传与 Supabase 云存储
- MCP Server 扩展工具能力
- Graphify 代码知识图谱

## 项目结构

```text
Lovemaster/
├── src/                               # Spring Boot 后端
├── mcp-servers/                       # MCP Server 模块
├── springai-front-react/              # React 前端
├── openspec/                          # 变更规范与设计文档
├── start.bat
├── start.sh
└── README.md
```

### 后端核心目录

```text
src/main/java/org/example/springai_learn/
├── controller/                        # HTTP 接口
├── ai/
│   ├── context/                       # ChatInputContext / BrainDecision 等
│   ├── orchestrator/                  # LoveChatOrchestrator / CoachChatOrchestrator
│   └── service/                       # MultimodalIntake / Brain / Tools / RAG / SSE
├── app/                               # LoveApp
├── auth/                              # 用户认证、图片存储
├── tools/                             # 工具注册与工具实现
├── mcp/                               # MCP 相关配置
├── ChatMemory/                        # 聊天记忆
├── advisor/                           # Spring AI advisors
└── config/                            # 模型与系统配置
```

### React 前端核心目录

```text
springai-front-react/src/
├── components/
│   ├── Chat/
│   ├── ManusPanel/
│   ├── Sidebar/
│   └── ui/
├── hooks/
├── pages/
│   ├── Home/
│   ├── Chat/
│   └── Auth/
└── services/
```

## 运行要求

- Java 21+
- Maven 3.6+
- PostgreSQL 12+
- Node.js 18+

## 本地配置

所有密钥都应放在本地文件 `src/main/resources/application-local.yml` 或环境变量中，不要提交到仓库。

可以从下面任一模板复制：

- `src/main/resources/application-local.yml.example`
- `src/main/resources/application-local.yml.template`

示例：

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

### 数据库

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_db
    username: your_user
    password: your_password
```

### NVIDIA NIM

```yaml
spring:
  ai:
    openai:
      base-url: https://integrate.api.nvidia.com
      api-key: your-nvidia-api-key

nvidia:
  model:
    rewrite: qwen/qwen3.5-122b-a10b
    tools: deepseek-ai/deepseek-r1
    brain: moonshotai/kimi-k2-thinking
```

### Dify

```yaml
dify:
  api:
    base-url: https://api.dify.ai/v1
    dataset-key: your-dify-dataset-api-key
    dataset-id: your-dify-dataset-id
```

### Supabase 图片存储

```yaml
supabase:
  url: https://your-project.supabase.co
  api-key: your-supabase-service-role-key
  storage:
    bucket: conversation-images
    public-url: https://your-project.supabase.co/storage/v1/object/public/conversation-images
```

### 其他常见配置

```yaml
spring:
  ai:
    alibaba:
      api-key: your-dashscope-api-key

search-api:
  api-key: ${SEARCH_API_KEY:}

pexels:
  api-key: ${PEXELS_API_KEY:}

oauth:
  google:
    client-id: your-google-client-id
```

## 快速开始

### 1. 启动后端

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

默认后端地址：

- API: `http://localhost:8088/api`

### 2. 启动 MCP Server

```bash
cd mcp-servers
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

默认地址：

- MCP Server: `http://localhost:8127`

### 3. 启动 React 前端

```bash
cd springai-front-react
npm install
npm run dev
```

默认地址：

- React: `http://localhost:5173`

前端开发环境已将 `/api` 代理到后端 `http://localhost:8088`。

## 常用命令

### 后端

```bash
mvn test
mvn -DskipTests=true package
mvn -DskipTests compile
```

### React

```bash
cd springai-front-react
npm run lint
npm run build
```

### MCP Server

```bash
cd mcp-servers
mvn test
mvn -DskipTests=true package
```

## API 概览

### Love 模式 SSE

```http
GET /api/ai/love_app/chat/sse?message={message}&chatId={chatId}&imageUrl={imageUrl}
```

用于恋爱陪伴模式。当前实现为纯聊天回答，不走工具执行链路。

### Coach 模式 SSE

```http
GET /api/ai/manus/chat?message={message}&chatId={chatId}&imageUrl={imageUrl}
```

用于恋爱教练模式。虽然路由仍保留 `manus` 命名，但当前实现已经是新的 Coach 架构：

- 先 Intake
- 再 RAG
- 再 Brain 判断
- 按需进入 ToolsAgent

### 概率分析

```http
POST /api/ai/probability/analyze
```

请求体包含 `chatId`、`message`、`imageUrl`，返回结构化概率分析结果。由前端渲染为概率卡片。

### 会话管理

```http
GET    /api/ai/sessions?chatType={chatType}
GET    /api/ai/sessions/{chatId}/messages?chatType={chatType}&limit={limit}
DELETE /api/ai/sessions/{chatId}?chatType={chatType}
GET    /api/ai/sessions/{chatId}/runs        # 查询后台运行状态
```

### 认证与图片

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/google
POST /api/auth/refresh
POST /api/auth/upload-image
```

## 工具能力

当前工具由 `ToolRegistration` 统一注册，支持本地工具和 MCP 工具扩展。

常见工具包括：

- 邮件发送
- 网络搜索
- 网页抓取
- 文件操作
- PDF 生成
- 终端执行
- 资源下载
- 图片搜索
- 终止工具 `doTerminate`

## 开发说明

### 添加新工具

1. 在 `src/main/java/org/example/springai_learn/tools/` 下新增工具类
2. 使用 Spring AI Tool 注解定义方法
3. 在 `ToolRegistration` 中注册

### 调整聊天链路

如果要改 Love / Coach 模式的行为，优先检查这些文件：

- `ai/orchestrator/LoveChatOrchestrator.java`
- `ai/orchestrator/CoachChatOrchestrator.java`
- `ai/service/MultimodalIntakeService.java`
- `ai/service/BrainAgentService.java`
- `ai/service/ToolsAgentService.java`
- `app/LoveApp.java`

### 图片存储实现

对话中的图片现在存储在 Supabase Storage 中：

- 实体类：`auth/entity/ConversationImage.java`
- 存储服务：`auth/service/ConversationImageStorageService.java`
- Supabase 客户端：`auth/service/SupabaseStorageClient.java`

### 后台运行状态

当用户离开聊天页面时，对话在后台继续运行。返回页面时自动恢复：

- 前端：`useBackgroundRuns` hook + `BackgroundRunsPill` 组件 + `RecoveryBanner` 组件
- 后端：`ChatRunService` 管理运行状态，SSE 连接断开后任务继续执行

### Graphify 知识图谱

项目集成了 Graphify 代码知识图谱工具：

```bash
graphify update .        # 基于 AST 更新代码图谱（无 API 成本）
graphify query "..."     # 基于图谱的代码问答
graphify explain "X"     # 解释某个代码节点及其邻居
```

图谱文件保存在本地 `graphify-out/`，已加入 `.gitignore`。

### OpenSpec

涉及新能力、架构调整、proposal/spec/plan 时，请先查看：

- `openspec/AGENTS.md`

## 注意事项

1. 不要把 API Key、数据库密码提交到仓库
2. Dify 数据集配置错误时，RAG 可能返回空结果或失败
3. PostgreSQL 需要先可用
4. Coach 模式是否进入工具链路，由 `BrainAgentService` 决定
5. Love 模式当前设计为不调用工具的直接对话模式
6. Supabase 配置错误时，图片上传将回退到本地存储（如果启用）
7. Graphify 生成的文件已加入 `.gitignore`，不会提交到仓库

## License

MIT

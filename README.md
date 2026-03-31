# Lovemaster - Spring AI

## 项目简介

Lovemaster 是一个全栈 AI 情感咨询应用：后端基于 Spring Boot 3.4.5 + Spring AI，前端采用 React（Vite）构建，并包含独立的 MCP Servers 模块。项目集成了大语言模型聊天、Dify 云端知识库（RAG）、AI 工具调用、JWT 用户认证等核心能力。

## 技术栈

### 核心框架
- **Spring Boot**: 3.4.5
- **Spring AI**: 1.0.0-M6
- **Java**: 21

### 前端
- **React 19** + **Vite 7**（`springai-front-react/`）
  - Framer Motion、React Router、Three.js
  - TailwindCSS、Lucide React

### AI 集成
- **阿里云通义千问**: Spring AI Alibaba Starter 1.0.0-M6.1
- **NVIDIA NIM 平台**: 多模型编排
  - **Qwen3.5 VLM 122B**: 多模态问题重写与图像理解（via NVIDIA NIM）
  - **DeepSeek-R1**: KkomaManus 工具调用 Agent
  - **Kimi-K2-Thinking**: Coach 模式直接回答
- **LangChain4j**: DashScope社区版 1.0.0-beta2
- **MCP客户端**: Spring AI MCP Client 1.0.0-M6
- **Dify 云端知识库**: 通过 Dify Dataset API 实现 RAG 检索

### 数据存储
- **PostgreSQL**: 用户数据、会话消息持久化
- **Spring JDBC**: 数据库操作

### 文档处理
- **iText Core**: 9.1.0（PDF生成和处理，支持亚洲字体）
- **JSoup**: 1.19.1（HTML解析和网页抓取）
- **Spring AI Markdown Reader**: 1.0.0-M6（文档读取器）

### 工具库
- **Hutool**: 5.8.37 (Java工具库)
- **Knife4j**: 4.4.0 (API文档增强)
- **Lombok**: 1.18.30 (代码简化)
- **Kryo**: 5.6.2 (高性能序列化，用于聊天记忆持久化)
- **Google OAuth**: 2.7.2（Google账号登录）

### 其他功能
- **用户认证**: JWT + Refresh Token，图片上传
- **邮件发送**: Spring Boot Mail
- **JSON Schema**: victools/jsonschema-generator 4.38.0

## 核心功能

### 1. AI 聊天系统
- **Love App 聊天**: 基于情感咨询场景的实时聊天，支持上下文记忆
- **Coach 聊天**: 智能路由的情感教练模式，由 Kimi-K2-Thinking 模型驱动
- **Manus AI 代理**: 智能对话代理系统，基于 DeepSeek-R1 支持多工具调用
- **多模态理解**: Qwen3.5 VLM 支持图像输入与 OCR，实现视觉理解能力
- **流式响应**: SSE (Server-Sent Events) 实时流式输出
- **会话管理**: 多会话持久化、历史消息加载

### 2. 用户认证系统
- **JWT 认证**: Access Token + Refresh Token 双令牌机制
- **注册/登录**: 邮箱注册、密码登录
- **Google OAuth**: Google账号一键登录
- **图片上传**: 用户头像上传与管理

### 3. AI 代理系统
- **基础代理** (`BaseAgent`): 提供代理的基础功能和生命周期管理
- **工具调用代理** (`ToolCallAgent`): 支持函数调用的智能代理
- **ReAct代理** (`ReActAgent`): 推理-行动循环代理
- **KkomaManus**: 专用AI助手代理，支持流式输出和对话记忆

### 4. AI 工具集
- **邮件发送工具** (`EmailSendTool`): 智能邮件发送功能
- **网络搜索工具** (`WebSearchTool`): 集成搜索API
- **网页抓取工具** (`WebScrapingTool`): 网页内容提取
- **文件操作工具** (`FileOperationTool`): 文件处理功能
- **PDF生成工具** (`PDFGenerationTool`): 动态PDF文档生成，支持中文
- **终端操作工具** (`TerminalOperationTool`): 系统命令执行
- **资源下载工具** (`ResourceDownloadTool`): 资源下载管理
- **图片搜索工具** (`ImageSearchTool`): Pexels API 图片搜索
- **终止工具** (`TerminateTool`): 流程控制

### 5. RAG 知识库检索（Dify 云端）
- **Dify 集成**: 通过 Dify Dataset API 进行知识检索，无需本地向量数据库
- **混合搜索**: 支持 `hybrid_search` 模式
- **自动重试**: 内置指数退避重试机制（默认 2 次）
- **无缝降级**: 知识库不可用时自动跳过，不影响正常聊天

### 6. 聊天记忆管理
- **对话上下文**: 维护聊天历史和上下文
- **记忆持久化**: 基于Kryo序列化的文件存储
- **多类型会话**: 支持不同聊天类型使用独立存储目录

### 7. MCP Servers
- **独立模块**: Spring Boot 3.5.0 应用
- **图片搜索服务**: 集成Pexels API
- **WebMVC模式**: 基于 spring-ai-mcp-server-webmvc

## 项目结构

### 仓库概览

```
Lovemaster/
├── src/                         # Spring Boot 后端
├── mcp-servers/                 # MCP Servers（独立 Spring Boot 应用）
├── springai-front-react/        # React 前端（Vite）
├── start.bat / start.sh         # 一键启动脚本
└── CLAUDE.md                    # Claude Code 开发指引
```

### 后端目录结构（src/）

```
src/main/java/org/example/springai_learn/
├── SpringAiLearnApplication.java          # 应用启动类
├── controller/                            # 控制器层
│   ├── AiController.java                 # AI相关API接口
│   ├── ChatSessionController.java        # 会话管理接口
│   ├── FileController.java               # 文件操作接口
│   └── HealthController.java             # 健康检查接口
├── agent/                                # AI代理模块
│   ├── BaseAgent.java                    # 基础代理类
│   ├── ToolCallAgent.java               # 工具调用代理
│   ├── ReActAgent.java                  # ReAct代理
│   ├── KkomaManus.java                  # Manus代理
│   └── model/                           # 代理数据模型
├── ai/                                   # AI 核心模块
│   ├── orchestrator/                     # 聊天编排器
│   │   ├── LoveChatOrchestrator.java     # Love 聊天编排
│   │   └── CoachChatOrchestrator.java    # Coach 聊天编排
│   ├── service/                          # AI 服务
│   │   ├── DifyKnowledgeService.java     # Dify 知识库检索
│   │   ├── RagKnowledgeService.java      # RAG 服务整合
│   │   ├── CoachRoutingService.java      # Coach 路由服务
│   │   ├── MultimodalIntakeService.java  # 多模态输入处理
│   │   ├── AiErrorMessageResolver.java   # AI 错误处理
│   │   └── SseEventHelper.java           # SSE 事件辅助
│   ├── dto/                              # 数据传输对象
│   └── context/                          # 上下文模型
├── app/                                  # 应用层
│   └── LoveApp.java                     # Love App聊天应用
├── auth/                                 # 用户认证模块
│   ├── controller/                       # AuthController, ImageController
│   ├── security/                         # SecurityConfig, JWT 过滤器
│   ├── service/                          # AuthService, TokenService
│   ├── entity/                           # User, ChatMessage, Conversation 等
│   ├── repository/                       # Spring Data JDBC
│   └── dto/                              # LoginRequest, RegisterRequest 等
├── tools/                               # AI工具集
│   ├── EmailSendTool.java               # 邮件发送工具
│   ├── WebSearchTool.java               # 网络搜索工具
│   ├── WebScrapingTool.java             # 网页抓取工具
│   ├── FileOperationTool.java           # 文件操作工具
│   ├── PDFGenerationTool.java           # PDF生成工具
│   ├── TerminalOperationTool.java       # 终端操作工具
│   ├── ResourceDownloadTool.java        # 资源下载工具
│   ├── ImageSearchTool.java             # 图片搜索工具（Pexels）
│   ├── TerminateTool.java               # 终止工具
│   └── ToolRegistration.java            # 工具注册管理
├── config/                              # 配置类
├── mcp/                                 # MCP相关配置
├── ChatMemory/                          # 聊天记忆管理
├── constant/                            # 常量定义
├── dto/                                 # 数据传输对象
├── advisor/                             # 顾问模式实现
└── demo/                               # 演示示例
```

### React 前端结构（springai-front-react/src/）

```
src/
├── App.jsx                      # 主应用组件
├── main.jsx                     # 入口文件
├── components/                  # 组件库
│   ├── Chat/                   # 聊天相关组件
│   ├── Layout/                 # 布局组件
│   ├── ManusPanel/             # Manus面板组件
│   ├── Sidebar/                # 侧边栏组件
│   ├── ParticleBackground/     # 粒子背景效果
│   ├── WebGLBackground/        # Three.js WebGL动态背景
│   └── ui/                     # UI基础组件
├── pages/                      # 页面组件
│   ├── Auth/                   # 登录/注册页面
│   ├── Chat/                   # 聊天页面
│   └── Home/                   # 首页
├── services/                   # API服务
├── hooks/                      # 自定义Hooks
└── styles/                     # 样式文件
```

## 环境要求

- **Java**: 21+
- **Maven**: 3.6+
- **PostgreSQL**: 12+
- **Node.js**: 18+（如需运行前端）
- **Dify 账号**: 用于知识库检索（云端，无需本地向量数据库）

## 配置说明

> 所有密钥/Token 请只放到本地的 `src/main/resources/application-local.yml`（已在 `.gitignore` 中忽略），或通过环境变量注入。

### 1. 数据库配置

在 `application-local.yml` 中配置 PostgreSQL：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://your-host:5432/your-database
    username: your-username
    password: your-password
```

### 2. AI 模型配置 (NVIDIA NIM)

```yaml
spring:
  ai:
    openai:
      base-url: https://integrate.api.nvidia.com
      api-key: your-nvidia-api-key

nvidia:
  model:
    rewrite: qwen/qwen3.5-122b-a10b    # 多模态重写模型
    tools: deepseek-ai/deepseek-r1      # 工具调用模型
    brain: moonshotai/kimi-k2-thinking  # 思考模型
```

### 3. 阿里云模型配置

```yaml
spring:
  ai:
    alibaba:
      api-key: your-dashscope-api-key
```

### 4. Dify 知识库配置

```yaml
dify:
  api:
    base-url: https://api.dify.ai/v1
    dataset-key: your-dify-dataset-api-key
    dataset-id: your-dify-dataset-id
```

获取方式：登录 Dify 控制台 → 知识库 → API 页面获取 Dataset ID 和 API Key。

### 5. 邮件服务配置

```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 465
    username: your-email@qq.com
    password: your-auth-code
```

### 6. 搜索 API 配置

```yaml
search-api:
  api-key: ${SEARCH_API_KEY:}

pexels:
  api-key: ${PEXELS_API_KEY:}
```

### 7. Google OAuth 配置

```yaml
oauth:
  google:
    client-id: your-google-client-id
```

获取方式：在 [Google Cloud Console](https://console.cloud.google.com/) 创建 OAuth 2.0 客户端，获取 Client ID。

### 8. MCP 本地自动构建（stdio）

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: ${APP_MCP_CLIENT_ENABLED:true}
        initialized: ${APP_MCP_CLIENT_INITIALIZED:false}
        stdio:
          servers-configuration: classpath:mcp-servers.json
```

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/KkOma-value/Lovemaster.git
cd Lovemaster
```

### 2. 配置环境

复制并编辑配置文件：

```bash
cp src/main/resources/application-local.yml.template src/main/resources/application-local.yml
```

在 `application-local.yml` 中填入你的 API 密钥、数据库和 Dify 配置。

### 3. 一键启动（推荐）

**Windows:**
```bash
start.bat
```

**Linux/Mac:**
```bash
./start.sh
```

### 4. 启动前端

```bash
cd springai-front-react
npm install
npm run dev
```

### 5. 访问应用

- **前端页面**: http://localhost:5173
  - 首页: 精美的Three.js WebGL动态背景
  - 登录/注册: 支持邮箱和Google OAuth登录
  - 聊天页面: 实时AI对话界面
- **API 文档**: http://localhost:8088/api/swagger-ui.html
- **健康检查**: http://localhost:8088/api/health

前端开发模式下已配置 `/api` 代理到后端 `http://localhost:8088`。

## API 文档

### 主要接口

#### 1. Love App 聊天

```http
GET /api/ai/love_app/chat/sse?message={message}&chatId={chatId}
```

SSE 流式聊天，响应格式：`data: {"type":"message","content":"..."}` → `data: [DONE]`

#### 2. Manus AI 聊天

```http
GET /api/ai/manus/chat?message={message}&chatId={chatId}
```

Agent 模式聊天，支持工具调用。

#### 3. 用户认证

```http
POST /api/auth/register        # 邮箱注册
POST /api/auth/login           # 邮箱登录
POST /api/auth/google          # Google OAuth登录（传递idToken）
POST /api/auth/refresh         # 刷新 Token
POST /api/auth/upload-image    # 上传头像
```

**Google OAuth 登录示例：**

```http
POST /api/auth/google
Content-Type: application/json

{
  "idToken": "google-oauth-id-token",
  "email": "user@example.com",
  "name": "User Name",
  "picture": "https://..."
}
```

#### 4. 会话管理

```http
GET    /api/ai/sessions?chatType={chatType}              # 会话列表
DELETE /api/ai/sessions/{chatId}?chatType={chatType}      # 删除会话
GET    /api/ai/sessions/{chatId}/messages?limit={limit}   # 消息历史
```

## 开发指南

### 添加新的 AI 工具

1. 在 `tools` 包下创建新的工具类
2. 使用 `@Tool` 和 `@ToolParam` 注解
3. 在 `ToolRegistration` 中注册

```java
@Component
public class MyCustomTool {
    @Tool(description = "工具描述")
    public String myToolFunction(
            @ToolParam(description = "参数描述") String parameter) {
        return "工具执行结果";
    }
}
```

### 创建新的 AI 代理

1. 继承 `BaseAgent` 类
2. 实现特定的代理逻辑
3. 配置所需的工具和参数
4. 在控制器中暴露代理接口

## 注意事项

1. **API密钥安全**: 不要将密钥提交到版本控制，使用 `application-local.yml` 或环境变量
2. **Dify 配置**: 确保 Dataset ID 和 API Key 正确，否则知识库检索会返回 404
3. **数据库**: PostgreSQL 服务需正常运行
4. **内存管理**: AI 模型可能消耗较多内存，建议分配足够的 JVM 堆空间

## 许可证

MIT License

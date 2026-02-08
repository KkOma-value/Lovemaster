# Lovemaster - Spring AI

## 📖 项目简介

Lovemaster 是一个全栈 AI 学习项目：后端基于 Spring Boot 3.4.5 + Spring AI，前端采用 React（Vite）构建，并包含独立的 MCP Servers 模块。项目展示了如何集成大语言模型聊天、RAG（检索增强生成）、AI 工具调用、向量存储等核心能力。

## 🚀 技术栈

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
- **LangChain4j**: DashScope社区版 1.0.0-beta2
- **MCP客户端**: Spring AI MCP Client 1.0.0-M6

### 数据存储
- **PostgreSQL**: 用于向量存储 (PgVector 1.0.0-M6)
- **Spring JDBC**: 数据库操作

### 文档处理
- **iText Core**: 9.1.0（PDF生成和处理，支持亚洲字体）
- **JSoup**: 1.19.1（HTML解析和网页抓取）
- **Spring AI Markdown Reader**: 1.0.0-M6（文档读取器）

### 工具库
- **Hutool**: 5.8.37 (Java工具库)
- **Knife4j**: 4.4.0 (API文档增强)
- **Lombok**: 1.18.30 (代码简化)
- **Kryo**: 5.6.2 (序列化)

### 其他功能
- **邮件发送**: Spring Boot Mail
- **JSON Schema**: victools/jsonschema-generator 4.38.0

## 🌟 核心功能

### 1. AI聊天系统
- **Love App聊天**: 基于SSE的实时聊天应用，支持上下文记忆
- **Manus AI代理**: 智能对话代理系统，支持多工具调用
- **流式响应**: 支持SSE (Server-Sent Events) 实时流式输出
- **会话管理**: 支持多会话持久化、历史消息加载

### 2. AI 代理系统
- **基础代理** (`BaseAgent`): 提供代理的基础功能和生命周期管理
- **工具调用代理** (`ToolCallAgent`): 支持函数调用的智能代理
- **ReAct代理** (`ReActAgent`): 推理-行动循环代理
- **KkomaManus**: 专用AI助手代理，支持流式输出和对话记忆

### 3. AI 工具集
- **邮件发送工具** (`EmailSendTool`): 智能邮件发送功能
- **网络搜索工具** (`WebSearchTool`): 集成搜索API
- **网页抓取工具** (`WebScrapingTool`): 网页内容提取
- **文件操作工具** (`FileOperationTool`): 文件处理功能
- **PDF生成工具** (`PDFGenerationTool`): 动态PDF文档生成，支持中文
- **终端操作工具** (`TerminalOperationTool`): 系统命令执行
- **资源下载工具** (`ResourceDownloadTool`): 资源下载管理
- **图片搜索工具** (`ImageSearchTool`): Pexels API 图片搜索
- **终止工具** (`TerminateTool`): 流程控制

### 4. RAG (检索增强生成)
- **向量存储**: 基于PgVector的向量数据库
- **文档加载器**: 支持多种文档格式加载（Markdown等）
- **查询重写**: 智能查询优化
- **自定义顾问**: 个性化RAG策略

### 5. 聊天记忆管理
- **对话上下文**: 维护聊天历史和上下文
- **记忆持久化**: 基于Kryo序列化的文件存储
- **多类型会话**: 支持不同聊天类型使用独立存储目录

### 6. MCP Servers
- **独立模块**: Spring Boot 3.5.0 应用
- **图片搜索服务**: 集成Pexels API
- **WebMVC模式**: 基于 spring-ai-mcp-server-webmvc

## 📁 项目结构

### 仓库概览

```
Lovemaster/
├── src/                         # Spring Boot 后端
├── mcp-servers/                 # MCP Servers（独立 Spring Boot 应用）
├── springai-front-react/        # React 前端（Vite）
├── openspec/                    # OpenAPI 规范文件
├── start.bat / start.sh         # 一键启动脚本
└── .github/                     # GitHub Actions 配置
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
├── app/                                  # 应用层
│   └── LoveApp.java                     # Love App聊天应用
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
├── rag/                                 # RAG检索增强生成
│   ├── QueryRewriter.java               # 查询重写器
│   ├── PgVectorVectorStoreConfig.java   # PgVector配置
│   ├── LoveAppVectorStoreConfig.java    # Love App向量存储配置
│   ├── LoveAppRagCustomAdvisorFactory.java  # 自定义RAG顾问工厂
│   ├── LoveAppRagCloudAdvisorConfig.java    # 云端RAG顾问配置
│   └── LoveAppDocumentLoader.java       # 文档加载器
├── mcp/                                 # MCP相关配置
├── ChatMemory/                          # 聊天记忆管理
├── config/                              # 配置类
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
│   └── ui/                     # UI基础组件
├── pages/                      # 页面组件
├── services/                   # API服务
├── hooks/                      # 自定义Hooks
└── styles/                     # 样式文件
```

## 🛠️ 环境要求

- **Java**: 21+
- **Maven**: 3.6+
- **PostgreSQL**: 12+ (如需使用向量存储功能)
- **Node.js**: 18+ (如需运行前端)

## ⚙️ 配置说明

> 重要：所有密钥/Token 请只放到本地的 `src/main/resources/application-local.yml`（该文件已在 `.gitignore` 中忽略），或通过环境变量注入。
> 不要把真实密钥写进仓库中的 `application.yml` / `mcp-servers.json`。

### 1. 数据库配置

在 `application.yml` 中配置PostgreSQL数据库：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://your-host:5432/your-database
    username: your-username
    password: your-password
```

### 2. AI模型配置

配置阿里云通义千问API密钥（在环境变量或配置文件中）：

```yaml
spring:
  ai:
    alibaba:
      api-key: your-api-key
```

### 3. 邮件服务配置

```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 465
    username: your-email@qq.com
    password: your-auth-code  # QQ邮箱授权码
```

### 4. 搜索API配置

```yaml
search-api:
  api-key: ${SEARCH_API_KEY:}

pexels:
  api-key: ${PEXELS_API_KEY:}
```

推荐通过环境变量提供密钥，避免把密钥写入仓库文件：

- PowerShell：`$env:SEARCH_API_KEY = "<your_key>"`
- PowerShell：`$env:PEXELS_API_KEY = "<your_key>"`

项目内的 `application-local.yml` 已支持：`search-api.api-key: ${SEARCH_API_KEY:}`。

### 5. MCP 本地自动构建（stdio）

本项目支持在主程序启动完成后**非阻塞**地后台构建 `mcp-servers` 模块。MCP client 默认禁用以避免启动超时，构建成功后需要手动启用。

关键配置（见 `src/main/resources/application.yml`）：

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: ${APP_MCP_CLIENT_ENABLED:true}
        initialized: ${APP_MCP_CLIENT_INITIALIZED:false}
        stdio:
          servers-configuration: classpath:mcp-servers.json

app:
  mcp:
    autostart: ${APP_MCP_AUTOSTART:true}
```

常用环境变量：

- `APP_MCP_AUTOSTART`：是否启用 MCP 自动构建（默认 `true`）
- `APP_MCP_CLIENT_ENABLED`：是否启用 MCP client（默认 `true`；如需完全禁用可设为 `false`）
- `APP_FILE_SAVE_DIR`：共享文件保存目录（建议用绝对路径；用于图片下载与 PDF 读取一致）

MCP server 的密钥统一从主程序 `application-local.yml` 读取并在启动子进程时透传：
- `amap.maps.api-key` → 透传为环境变量 `AMAP_MAPS_API_KEY`（给 npx 的 amap server）
- `pexels.api-key` → 透传为环境变量 `PEXELS_API_KEY`（给 mcp-servers）

### 6. RAG VectorStore 文档加载

启动后，应用会自动从 `classpath:document/*.md` 加载文档到 VectorStore，支持有界重试（默认 3 次、指数退避）。

配置项（`application.yml` 或环境变量）：

```yaml
app:
  rag:
    vectorstore:
      load-mode: async  # async（默认，后台加载带重试）或 off（禁用加载）
      load:
        max-attempts: 3
        backoff-seconds: 5
```

环境变量：

- `APP_RAG_LOAD_MODE`：`async`（默认）或 `off`
- `APP_RAG_LOAD_MAX_ATTEMPTS`：最大重试次数（默认 3）
- `APP_RAG_LOAD_BACKOFF_SECONDS`：初始退避秒数（默认 5，指数增长）

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/KkOma-value/Lovemaster.git
cd Lovemaster
```

### 2. 配置环境

复制并编辑配置文件：

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

在 `application-local.yml` 中填入你的API密钥和数据库配置。

### 3. 一键启动（推荐）

**Windows:**
```bash
start.bat
```

**Linux/Mac:**
```bash
./start.sh
```

### 4. 手动启动后端

```bash
# 使用Maven启动
mvn spring-boot:run

# 或者编译后启动
mvn clean package
java -jar target/SpringAI_Learn-0.0.1-SNAPSHOT.jar
```

### 5. 启动 MCP Servers（可选）

```bash
cd mcp-servers
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 6. 启动前端

```bash
cd springai-front-react
npm install
npm run dev
```

### 7. 访问应用

- **应用地址**: http://localhost:8088/api
- **API文档**: http://localhost:8088/api/swagger-ui.html
- **健康检查**: http://localhost:8088/api/health
- **React 前端**: http://localhost:5173
- **MCP Servers**: http://localhost:8127 (如已启动)

前端开发模式下已配置 `/api` 代理到后端 `http://localhost:8088`。

## 📚 API 文档

### 主要接口

#### 1. Love App 聊天

```http
GET /api/ai/love_app/chat/sse?message={message}&chatId={chatId}
```

**说明**: 启动Love App的SSE聊天流，支持实时流式响应。

**参数**:
- `message`: 用户消息内容
- `chatId`: 聊天会话ID

**响应**: SSE流式数据

#### 2. Manus AI 聊天

```http
GET /api/ai/manus/chat?message={message}&chatId={chatId}
```

**说明**: 与Manus AI代理进行对话，支持工具调用。

**参数**:
- `message`: 用户消息内容
- `chatId`: 会话ID（用于持久化）

**响应**: SSE流式数据

#### 3. 会话管理

```http
GET /api/ai/sessions?chatType={chatType}
```

**说明**: 获取所有会话列表。

**参数**:
- `chatType`: 聊天类型（loveapp 或 coach）

```http
DELETE /api/ai/sessions/{chatId}?chatType={chatType}
```

**说明**: 删除指定会话。

```http
GET /api/ai/sessions/{chatId}/messages?limit={limit}&chatType={chatType}
```

**说明**: 获取指定会话的消息历史。

#### 4. 健康检查

```http
GET /api/health
```

**说明**: 检查应用运行状态。

### SSE 响应格式

所有SSE接口都遵循以下响应格式：

```
data: {"type":"message","content":"AI回复内容"}

data: [DONE]
```

## 🔧 开发指南

### 1. 添加新的AI工具

1. 在 `tools` 包下创建新的工具类
2. 实现工具的核心逻辑
3. 在 `ToolRegistration` 中注册新工具
4. 在代理中使用新工具

示例：

```java
@Component
public class MyCustomTool {
    
    @Tool(description = "工具描述")
    public String myToolFunction(
            @ToolParam(description = "参数描述") String parameter) {
        // 工具逻辑实现
        return "工具执行结果";
    }
}
```

### 2. 创建新的AI代理

1. 继承 `BaseAgent` 类
2. 实现特定的代理逻辑
3. 配置所需的工具和参数
4. 在控制器中暴露代理接口

### 3. 扩展RAG功能

1. 在 `rag` 包下添加新的文档加载器
2. 配置向量存储策略
3. 自定义查询重写逻辑
4. 实现自定义顾问模式

## 🚨 注意事项

1. **API密钥安全**: 不要将API密钥提交到版本控制系统
2. **数据库连接**: 确保PostgreSQL服务正常运行
3. **内存管理**: AI模型可能消耗较多内存，建议分配足够的JVM堆内存
4. **并发控制**: SSE连接数量有限，注意连接池管理
5. **日志监控**: 开发环境下建议开启DEBUG日志以便调试

## 📝 开发计划

- [ ] 支持更多AI模型提供商
- [ ] 实现对话历史的持久化存储
- [ ] 添加用户认证和权限控制
- [ ] 优化向量存储性能
- [ ] 扩展更多实用AI工具
- [ ] 完善错误处理和重试机制

## 🤝 贡献指南

欢迎提交Issue和Pull Request来帮助改进项目！

## 📄 许可证

MIT License

## 📞 联系方式

如有问题，请提交Issue或联系项目维护者。

---

**注意**: 这是一个学习项目，主要用于探索和学习Spring AI框架的各种功能。在生产环境中使用前，请确保充分测试和安全评估。

# Project Context

## Purpose
SpringAI_Learn 是一个基于 Spring Boot 3.4.5 和 Spring AI 框架构建的现代化AI应用学习项目。

**主要目标**：
- 展示如何集成多种AI模型（阿里云通义千问、LangChain4j等）
- 实现完整的AI聊天系统（Love App、Manus AI代理等）
- 提供RAG（检索增强生成）的端到端实现
- 演示AI工具调用、代理系统、聊天记忆管理等核心功能
- 构建可扩展的AI应用架构模板

## Tech Stack

### 核心框架与语言
- **Java**: 21
- **Spring Boot**: 3.4.5
- **Spring AI**: 1.0.0-M6
- **Maven**: 3.6+

### AI & LLM集成
- **阿里云通义千问**: Spring AI Alibaba Starter 1.0.0-M6.1
- **LangChain4j**: DashScope社区版 1.0.0-beta2（备选集成）
- **MCP客户端**: Spring AI MCP Client 1.0.0-M6

### 数据存储与向量化
- **PostgreSQL**: 12+ （关系数据存储）
- **PgVector**: Spring AI PgVector Store 1.0.0-M6 （向量存储）

### 前端（可选）
- **Vue.js**: 3（构建前端应用）
- **Vite**: 前端构建工具
- **Node.js**: 16+

### 工具与库
- **Hutool**: 5.8.37 （Java通用工具库）
- **Knife4j**: 4.4.0 （API文档增强）
- **Lombok**: 1.18.30 （代码简化）
- **JSoup**: 1.19.1 （HTML解析）
- **iText**: 9.1.0 （PDF生成）
- **Kryo**: 5.6.2 （对象序列化）

## Project Conventions

### Code Style
- **语言**: Java 21，使用最新的语言特性（record、sealed类等）
- **命名规范**: 
  - 类名：PascalCase（如 `BaseAgent`, `LoveApp`）
  - 方法名：camelCase（如 `buildPrompt()`, `invokeModel()`)
  - 常量：UPPER_SNAKE_CASE（如 `MAX_RETRIES`, `API_TIMEOUT`）
- **代码格式**: 
  - 使用Lombok减少样板代码（@Data, @Slf4j等）
  - 4个空格缩进
  - 最大行宽: 120字符
- **注释**: 中文注释优先，重要的业务逻辑需要详细说明
- **异常处理**: 避免silent catch，使用日志记录异常栈轨迹

### Architecture Patterns
- **分层架构**:
  - `controller`: Spring REST 控制器，处理HTTP请求
  - `agent`: AI代理系统，包含BaseAgent基类和具体实现
  - `app`: 应用层，如LoveApp等特定业务逻辑
  - `tools`: AI工具集合，实现具体的可调用函数
  - `rag`: 检索增强生成模块，包括向量存储和查询优化
  - `config`: Spring配置类，管理Bean和中间件配置
  - `advisor`: 顾问模式实现，用于AI生成的前处理和后处理
  - `ChatMemory`: 聊天记忆管理，维护对话上下文
  - `constant`: 常量定义
  - `demo`: 演示和示例代码

- **设计模式**:
  - **代理模式** (`BaseAgent` 继承体系): 标准化AI代理的创建和调用流程
  - **工厂模式**: `ToolRegistration`, `LoveAppRagCustomAdvisorFactory` 用于对象创建
  - **顾问模式**: Spring AI Advisor用于拦截和增强AI生成过程
  - **策略模式**: 多种顾问实现支持不同的查询处理策略

- **关键组件交互**:
  - Controller → Agent系统 → Spring AI ChatModel
  - Agent → Tools（函数调用）
  - Agent → Advisor（生成前后处理）
  - ChatMemory ← Agent（存储/检索对话历史）
  - RAG流程: 文档加载 → 向量化 → 存储 → 查询重写 → 检索 → 增强提示

### Testing Strategy
- **测试覆盖**: 目前项目处于学习阶段，测试覆盖较少
- **建议的测试范围**:
  - 单元测试: AI工具的核心逻辑、工具函数
  - 集成测试: Agent与Spring AI ChatModel的交互
  - 端到端测试: 聊天API的完整流程
- **测试框架**: Spring Boot Test + JUnit 5（依赖已包含）
- **Mock策略**: 使用Mock AI模型响应进行单元测试

### Git Workflow
- **分支策略**: Git Flow
  - `main`: 稳定版本，标记发布版本
  - `develop`: 开发主分支，集成所有功能
  - `feature/*`: 功能分支，从develop创建，完成后合并回develop
  - `bugfix/*`: 缺陷修复分支
- **提交消息**: 遵循Conventional Commits规范
  - 格式: `<type>(<scope>): <subject>`
  - 示例: `feat(agent): add ReAct agent implementation`
  - 类型: feat, fix, docs, style, refactor, test, chore
- **PR流程**: 
  - 功能完成后创建Pull Request
  - 需要代码审查和通过CI检查
  - 合并前更新相关文档

## Domain Context

### AI代理系统的核心概念
- **Agent（代理）**: 能够理解任务、规划步骤、调用工具并生成响应的AI实体
- **Tool（工具）**: 代理可以调用的具体函数，如发送邮件、搜索网页等
- **Tool Calling（工具调用）**: Agent根据用户需求选择和调用合适的工具
- **ReAct（推理-行动）**: 循环迭代的推理和执行流程
- **Memory（记忆）**: 维护对话历史和上下文，使AI能够进行多轮对话
- **Advisor（顾问）**: Spring AI特有的概念，用于在生成前后处理AI输出

### RAG系统
- **Document Loader**: 将各种格式的文档（Markdown、PDF等）加载为Document对象
- **Vector Store**: 使用PgVector存储文档向量和元数据
- **Embedding**: 使用AI模型将文本转换为高维向量
- **Query Rewriting**: 优化用户查询，提高检索精度
- **Retrieval**: 基于相似度查询向量存储，获取相关文档
- **Augmentation**: 将检索到的文档注入到AI提示词中

### 数据流
1. 用户输入 → Controller
2. Controller → Agent（可选: 调用LoveApp或其他业务逻辑）
3. Agent → ChatMemory（获取历史上下文）
4. Agent → RAG检索（如需要）
5. Agent → Tools调用（如需要）
6. Spring AI ChatModel（调用远程AI模型）
7. Advisor后处理（如需要）
8. 响应流 → SSE → 客户端

## Important Constraints

### 技术约束
- **Java版本**: 必须使用Java 21及以上（Spring Boot 3.4.5的要求）
- **Spring AI版本**: 当前使用1.0.0-M6（里程碑版本，非正式版本）
- **异步处理**: SSE聊天接口需要异步处理，避免阻塞
- **内存管理**: AI模型和向量查询可能消耗大量内存，需要监控

### 数据与安全约束
- **API密钥管理**: 所有API密钥必须存储在环境变量或加密配置中，不可提交版本控制
- **模型成本**: 调用云端AI模型会产生费用，需要控制请求频率和规模
- **向量存储权限**: PostgreSQL连接需要适当的权限配置

### 业务约束
- **聊天记忆持久化**: 目前使用`FileBasedChatMemory`，未来需要迁移到数据库
- **并发连接**: SSE连接数量有限，需要考虑连接池管理和超时策略
- **文档更新**: RAG文档需要定期更新和版本管理

## External Dependencies

### AI模型服务
- **阿里云通义千问 API**: 
  - 基于Spring AI Alibaba Starter集成
  - 需要API密钥认证
  - 支持流式输出（Server-Sent Events）

### 数据库
- **PostgreSQL**: 
  - 用于数据持久化
  - 需要安装PgVector扩展以支持向量存储
  - 默认配置: localhost:5432

### 邮件服务
- **SMTP邮件服务器**: 
  - 当前配置支持QQ邮箱SMTP
  - 需要配置邮箱地址和授权码

### 搜索API（可选）
- **Web搜索服务**: 
  - 用于`WebSearchTool`
  - 需要配置API密钥

### 前端（可选）
- **Node.js生态**: 
  - 如需运行springAI-front前端应用
  - 依赖管理: npm/yarn

### MCP（Model Context Protocol）
- **MCP服务器**: 
  - 本项目包含MCP服务器实现（mcp-servers）
  - 支持STDIO和SSE两种通信方式
  - 用于扩展AI能力

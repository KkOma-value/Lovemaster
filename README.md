# Love Master - 你的恋爱 AI 小助手

> 基于 Spring AI + React 的全栈 AI 恋爱陪伴与教练应用

[English](./README_EN.md)

![Love Master 首页](homepage_final.png)

## 功能亮点

**恋爱陪伴**
- Love 模式：纯聊天陪伴，支持文本与截图输入
- 智能截图理解：OCR 提取 + 问题重写，自动补充上下文

**恋爱教练**
- Coach 模式：AI Agent 架构，先思考后行动
- 工具调用：邮件、搜索、网页抓取、PDF 生成等 10+ 工具
- MCP Server 扩展：独立模块，动态注册外部工具

**Kiko AI 概率分析**
- 恋爱成功概率评估，输出结构化概率卡片
- 正面/风险信号 + 下一步行动建议

**知识与记忆**
- RAG 检索增强（PostgreSQL + PgVector / Dify / 本地 Wiki）
- 用户反馈驱动全自动知识入库，零人工审批
- 会话持久化 + 后台运行状态恢复

**认证与存储**
- Google OAuth + JWT 认证
- Supabase 云存储对话图片

## 技术栈

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M6-6DB33F)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-7-646CFF?logo=vite&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-4-06B6D4?logo=tailwindcss&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL+PgVector-4169E1?logo=postgresql&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-Storage-3FCF8E?logo=supabase&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

## 架构概览

```mermaid
graph TB
    subgraph Frontend ["前端 React + Vite :5173"]
        UI[页面组件]
    end

    subgraph Backend ["后端 Spring Boot :8088"]
        Intake[MultimodalIntake<br/>截图理解 / OCR / 问题重写]
        RAG[RagKnowledgeService<br/>Dify + PgVector + Wiki]
        Brain[BrainAgentService<br/>决策是否调用工具]
        Tools[ToolsAgentService<br/>本地工具 + MCP]
        Love[LoveChatOrchestrator]
        Kiko[ProbabilityAnalysisService]
        Coach[CoachChatOrchestrator]
    end

    subgraph MCP ["MCP Server :8127"]
        MCPTools[MCP 工具集]
    end

    subgraph Storage ["外部服务"]
        DB[(PostgreSQL + PgVector)]
        Dify[Dify Dataset API]
        Supa[Supabase Storage]
        Wiki[本地 Wiki 知识库]
    end

    UI -->|SSE| Intake
    Intake --> RAG
    RAG --> Brain
    Brain -->|需要工具| Tools
    Brain -->|直接回答| Coach
    Tools --> MCPTools
    Love --> Intake
    Kiko --> Intake
    RAG --> DB
    RAG --> Dify
    RAG --> Wiki
    Tools --> Supa
```

## 快速开始

### 环境要求

- Java 21+ / Maven 3.6+ / PostgreSQL 12+ / Node.js 18+

### 1. 配置

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# 编辑 application-local.yml，填入数据库、API Key 等配置
```

> 完整配置说明见 [docs/QUICKSTART.md](docs/QUICKSTART.md)

### 2. 启动后端

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local    # API: http://localhost:8088
```

### 3. 启动前端

```bash
cd springai-front-react
npm install && npm run dev                               # UI: http://localhost:5173
```

### 4. 启动 MCP Server（可选）

```bash
cd mcp-servers
mvn spring-boot:run -Dspring-boot.run.profiles=local    # MCP: http://localhost:8127
```

详细配置（NVIDIA NIM / Dify / Supabase / Google OAuth）请参考 [docs/QUICKSTART.md](docs/QUICKSTART.md)。

## 项目结构

```text
Lovemaster/
├── src/                           # Spring Boot 后端
│   └── main/java/.../
│       ├── controller/            # REST API + SSE
│       ├── ai/                    # Intake / Brain / Tools / Orchestrator
│       ├── app/                   # LoveApp 核心
│       ├── auth/                  # 认证 + 图片存储
│       ├── tools/                 # 工具注册与实现
│       └── ChatMemory/            # 会话持久化
├── springai-front-react/          # React 前端
│   └── src/
│       ├── components/            # Chat / Sidebar / UI 组件
│       ├── pages/                 # Home / Chat / Auth
│       └── hooks/                 # 自定义 Hooks
├── mcp-servers/                   # MCP Server 模块
├── docs/                          # 详细文档
├── knowledge/                     # 本地 Wiki 知识库
└── scripts/                       # 自动化脚本
```

## 聊天模式

### Love 模式 - 恋爱陪伴

纯聊天模式，直接给建议，不调用工具。

`输入 → 截图理解 → RAG 知识召回 → 陪伴式回答`

### Coach 模式 - 恋爱教练

Agent 架构，先判断再行动，按需调用工具。

`输入 → 截图理解 → RAG → Brain 决策 → [直接回答 | 工具调用] → 综合回答`

### Kiko AI - 概率分析

检测到"成功率""有没有戏"等意图时触发，输出结构化概率卡片。

`输入 → 意图识别 → ProbabilityAnalysisService → 概率卡片（概率值 / 信号 / 建议）`

## 常用命令

```bash
# 后端
mvn test                              # 运行测试
mvn -DskipTests=true package          # 打包 JAR

# 前端
cd springai-front-react
npm run lint                          # 代码检查
npm run build                         # 生产构建

# MCP Server
cd mcp-servers && mvn test
```

## 开发指南

- **添加新工具**：在 `tools/` 下新建类，使用 `@Tool` 注解，在 `ToolRegistration` 中注册
- **调整聊天链路**：修改 `ai/orchestrator/` 和 `ai/service/` 下对应文件
- **知识库更新**：`bash scripts/wiki-update.sh` 或 `bash scripts/setup-wiki-autoupdate.sh`

详细开发文档见 [docs/WORKFLOW_GUIDE.md](docs/WORKFLOW_GUIDE.md)。

## License

[MIT](LICENSE)

---

如果觉得有帮助，欢迎 Star 支持一下！

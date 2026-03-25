# Super Dev — Lovemaster Bootstrap

## 项目上下文

**仓库**: Lovemaster
**性质**: Spring AI 学习项目 / 全栈 AI 应用
**初始化时间**: 2026-03-25

### 技术栈
- 后端: Spring Boot 3.4.5 + Spring AI 1.0.0-M6 + Java 21
- 前端: React 19 + Vite 7 + TailwindCSS + Framer Motion
- AI: 阿里云通义千问 (DashScope)
- 存储: PostgreSQL + PgVector
- 协议: SSE 流式输出 + MCP (Model Context Protocol)

### 核心能力
1. LoveApp 聊天 — SSE 实时对话，支持 RAG 模式
2. KkomaManus 代理 — 多工具调用自主代理（文件、搜索、终端、邮件、PDF 等）
3. Coach 面板 (ManusPanel) — 右侧任务/终端/预览可视化面板
4. MCP Servers — 独立 Spring Boot MCP 服务（图片搜索）

## 流水线规则

### 强制顺序（不可跳步）
```
research → PRD + Architecture + UIUX → [用户确认] → Spec/tasks → 前端实现+验证 → 后端联调 → 质量门禁 → 交付
```

### 暂停门
- 三份核心文档完成后：**必须暂停，等待用户明确确认，再进入编码阶段**
- 未经确认：不建 Spec，不开始编码

## 当前活跃变更（OpenSpec）

| 变更 ID | 状态 | 说明 |
|---------|------|------|
| `enhance-coach-panel-ui` | 实现完成，差集成测试 | 布局遮挡、配色统一、图片预览修复 |
| `refactor-react-chat-ui` | 实现完成，差 lint + 手动验证 | SSE 生命周期重构 |
| `fix-mcp-startup-blocking` | 未知 | MCP 启动阻塞修复 |

## 文件布局

```
.super-dev/
├── WORKFLOW.md         # 本文件 — bootstrap 规则
└── changes/            # Super Dev 变更追踪（复用 openspec/changes 为主）

output/
├── *-research.md       # 同类产品研究报告
├── *-prd.md            # 产品需求文档
├── *-architecture.md   # 架构设计文档
├── *-uiux.md           # UI/UX 设计文档
└── knowledge-cache/    # 知识库缓存
```

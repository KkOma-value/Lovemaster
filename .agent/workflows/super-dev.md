---
description: Super Dev 流水线式 AI Coding 辅助工作流 - 从需求到交付的 12 阶段自动化流程
---

# Super Dev Pipeline Workflow

## 角色定义

本工作流激活 10 位专家角色自动协作：

| 专家 | 职责 |
|:---|:---|
| PM | 需求分析、PRD 生成、用户故事 |
| ARCHITECT | 架构设计、技术选型、API 契约 |
| UI/UX | 设计系统、交互规范、原型设计 |
| SECURITY | 红队审查、OWASP 检测、威胁建模 |
| CODE | 代码实现、最佳实践、代码审查 |
| DBA | 数据库设计、迁移脚本、索引优化 |
| QA | 测试策略、质量门禁、覆盖率要求 |
| DEVOPS | CI/CD 配置、容器化、监控告警 |
| RCA | 根因分析、故障复盘、风险识别 |

## 工作流步骤

### 前置：读取必备文档

在写任何一行代码前，必须先读取：

1. `output/*-prd.md` — 产品需求和验收标准
2. `output/*-architecture.md` — 技术栈和 API 设计
3. `output/*-uiux.md` — 设计系统和组件规范
4. `output/*-execution-plan.md` — 阶段任务路线图
5. `.super-dev/changes/*/tasks.md` — 具体实现任务清单

### 第 0 阶段：需求增强与同类产品研究

```bash
super-dev "你的需求描述"
```

- 解析自然语言需求，注入领域知识库
- 优先使用宿主原生联网能力研究同类产品、关键流程、页面结构和交互模式
- 联网检索补充市场和技术背景
- 输出 `output/*-research.md`，沉淀对标结论、共性功能和差异化机会
- 输出结构化需求蓝图

### 第 1 阶段：专业文档生成

自动生成：
- `output/*-prd.md` — PRD（产品需求文档）
- `output/*-architecture.md` — 架构设计文档
- `output/*-uiux.md` — UI/UX 设计文档
- 以上产物必须真实写入项目工作区；只在聊天里总结不算完成

### 第 2-4 阶段：骨架构建

- 前端可演示骨架（前端先行原则）
- Spec 规范（结构化规范风格）
- 前后端实现骨架 + API 契约

### 第 5-6 阶段：质量保障

- 红队审查（安全 + 性能 + 架构）
- 质量门禁（统一标准：80+）

### 第 7-8 阶段：交付准备

- 代码审查指南
- AI 提示词生成（直接传给 AI 开始开发）

### 第 9-11 阶段：部署与交付

- CI/CD 配置（GitHub/GitLab/Jenkins/Azure/Bitbucket）
- 数据库迁移脚本（Prisma/TypeORM/SQLAlchemy 等 6 种 ORM）
- 项目交付包（manifest + report + zip）

## 执行规则

- 进入 Super Dev 流程后，第一轮必须明确当前阶段是 `research`
- 三份核心文档完成后，必须暂停等待用户确认
- 未经用户确认，不得创建 `.super-dev/changes/*` 或开始编码
- **前端先行**：先完成可演示前端，再实现后端 API
- **禁止 emoji 图标**：使用 Lucide/Heroicons/Tabler Icons
- **参数化查询**：禁止字符串拼接 SQL
- **任务跟踪**：每完成一项在 tasks.md 标记 `[x]`
- **质量门禁**：交付前运行 `super-dev quality --type all`

## 常用命令

```bash
super-dev "需求"               # 完整 12 阶段流水线（推荐）
super-dev pipeline "需求"      # 高级参数模式
super-dev create "需求"        # 生成文档 + Spec
super-dev quality --type all   # 质量检查
super-dev expert SECURITY "需求"  # 单专家调用
super-dev skill install super-dev --target antigravity  # 安装 Skill
```

## Super Dev System Flow Contract
- SUPER_DEV_FLOW_CONTRACT_V1
- PHASE_CHAIN: research>docs>docs_confirm>spec>frontend>preview_confirm>backend>quality>delivery
- DOC_CONFIRM_GATE: required
- PREVIEW_CONFIRM_GATE: required
- HOST_PARITY: required

# ADR-003: 选择 Spring Boot 作为后端框架

**状态**: accepted
**日期**: 2026-03-29
**更新**: 2026-04-19 — 修正原 ADR 中 "Node" 的表述，实际技术栈为 Spring Boot 3.4.5 + Java 21

## 背景 (Context)

项目需要选择后端框架。经过对主流方案的评估，需要在以下方案中做出选择。评估维度包括：性能、生态、团队熟悉度、维护成本和长期演进。

## 考虑的方案 (Options)

### 方案 1: Spring Boot (适配分: 90)

**优势**:
- 企业级生态成熟
- Spring AI 原生集成
- 自动配置与依赖注入
- JPA/Hibernate 开箱即用
- 强大的监控与运维工具

**劣势**:
- 启动时间较长
- 内存占用相对较高

### 方案 2: FastAPI (适配分: 75)

**优势**:
- 自动文档 (OpenAPI)
- 高性能 (async)
- 类型提示

**劣势**:
- Spring AI 无原生 Python SDK
- 生态不如 Spring 全面

### 方案 3: Express (Node.js) (适配分: 60)

**优势**:
- 极简灵活
- npm 生态
- 前后端同语言

**劣势**:
- 缺少企业级约定
- 无 Spring AI 集成

## 决策 (Decision)

选择 Spring Boot 3.4.5 + Java 21 作为项目的后端框架。

## 理由 (Rationale)

Spring Boot 的主要优势包括: 企业级生态成熟、Spring AI 原生集成、自动配置。本项目核心依赖 Spring AI (OpenAI/Anthropic 模型接入、RAG、Function Calling)，Spring Boot 是天然最佳选择。

## 后果 (Consequences)

正面: Spring AI 原生支持、企业级稳定性、丰富的 Java 生态。
负面/风险: 启动时间较长、需要 Java 21+ 运行环境。
需要团队熟悉 Spring Boot 3.x 和 Spring AI 的最佳实践。

## 相关决策

- ADR-002

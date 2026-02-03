# Design: Async MCP autostart with automatic local document preload

## Context
- 现有 `add-mcp-server-autostart` 仅描述一次性的异步构建+启动，但实际仍可能需要手动拉起 MCP，且本地文档未在启动流程中保证加载。
- `project-startup` 是当前承载启动体验/前置条件的能力入口。

## Goals / Non-Goals
- Goals: 保证主程序启动后自动（异步、非阻塞）拉起 MCP；在 MCP 可用时自动完成本地文档加载/向量化；提供可关闭开关与关键日志。
- Non-Goals: 不做复杂的子进程守护/多次重启策略；不强制启动流程等待 MCP 或文档完成；不调整 RAG 语义逻辑。

## Decisions
- 用 ApplicationReady/SmartLifecycle 异步触发构建+stdio MCP 拉起，失败仅告警。
- 增加本地文档预加载触发点：在启动后即尝试一次；若依赖 MCP 或向量存储未就绪，可在 MCP ready 后重试一次，避免长轮询。
- 保留配置开关：`app.mcp.autostart` 控制 MCP 自启；新增/复用本地文档加载开关与可选重试间隔。
- 观测性：关键日志包含构建命令、产物路径、MCP 启动状态、文档加载/重试结果；不新增重型监控。

## Risks / Trade-offs
- 构建耗时可能与文档加载并行，需限制并发或分阶段触发以免启动期资源尖峰。
- 若依赖外部 Postgres/向量服务不可用，文档加载重试需有限次数避免噪音日志。

## Open Questions
- 文档预加载是否必须等 MCP 工具可用（还是仅依赖向量存储）？
- 重试策略的默认值（次数/间隔）需要多激进还是保守？

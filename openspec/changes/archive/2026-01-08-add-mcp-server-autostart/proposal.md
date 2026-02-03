# Change: Auto-start local MCP server from main app

## Why
当前本项目的 MCP server（`mcp-servers` 模块）需要单独启动；当开发者只启动主程序时，MCP 工具经常处于不可用状态，导致 Manus/LoveApp 的 MCP 能力只能降级。

你希望把 MCP 服务集成进主程序启动流程：
- 主程序启动时自动构建 `mcp-servers`（开发态便捷）
- 主程序启动时自动拉起 MCP server（stdio 方式）
- **不阻塞主程序启动**：MCP 不可用时继续降级（现有代码已支持降级）
- 你明确选择切换到 `stdio` 模式。

## What Changes
- 主程序启动时异步执行：
  1) `mvn -pl mcp-servers -DskipTests package` 构建 jar（失败不阻塞，只记录日志并放弃本轮）
  2) 构建成功后，触发 Spring AI MCP client 的延迟初始化，让 MCP client 按 `mcp-servers.json` 以 stdio 方式拉起 `mcp-servers` 进程
- 提供最小配置开关：允许禁用自动拉起（例如在生产环境或外部已有 MCP server 时）。
- 增加可观察性：主程序启动日志输出“构建命令/产物路径/延迟初始化触发结果/失败原因”。

## Impact
- Affected specs: `project-startup`（新增“可选的本地 MCP autostart”要求）
- Affected code (apply 阶段才改，提案阶段不改代码):
  - 主程序新增一个 bootstrap 组件（ApplicationReadyEvent/SmartLifecycle）
  - 可能补充 README / start.bat|start.sh 使用说明

## Alternatives Considered
- **SSE（主程序拉起 HTTP server 并做端口探测）**：实现简单，但需要额外管理端口、profile 与子进程生命周期；且与现有 `mcp-servers.json`（stdio）配置不一致。
- **同 JVM 内嵌启动**：最复杂，超出学习项目的最小变更范围。

## Non-Goals
- 不强制主程序启动等待 MCP ready（保持不阻塞）。
- 不实现生产级 supervisor/复杂重启策略（只做最小可用：一次尝试 + 简单重试）。

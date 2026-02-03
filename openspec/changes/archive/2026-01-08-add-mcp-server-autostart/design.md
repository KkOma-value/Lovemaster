# Design: Main app auto-builds MCP server and enables stdio MCP client (non-blocking)

## Current State (grounded)
- 主程序已通过 `spring.ai.mcp.client.sse.connections.server1.url=http://localhost:8127` 连接 MCP。
- 为避免 MCP 不可达导致启动崩溃，配置中存在 `spring.ai.mcp.client.initialized=false`。
- 业务侧已经支持降级：
  - `LoveApp.doChatWithMcp` 在 `ToolCallbackProvider` 不可用或调用异常时降级。
  - `AiController` 在每次请求时 new `KkomaManus(...)`，因此只要 `ToolCallbackProvider` 后续可用，新请求即可获得 MCP 工具（无需重启主程序）。

## Constraints (from user)
- 方案 B（主程序负责拉起 MCP server 子进程）
- 使用 stdio（由 MCP client 按 `mcp-servers.json` 拉起进程）
- 主程序启动不阻塞；MCP 不可用就降级
- 启动时自动构建 `mcp-servers`

## Proposed Approach
### 1) Async build
- 在主程序启动完成后（`ApplicationReadyEvent`）：异步执行 `mvn -pl mcp-servers -DskipTests package`。
- 若 Maven 不存在/构建失败：记录 warn/error 日志并退出本轮，不影响主程序。

### 2) Trigger stdio MCP (deferred init)
- 主程序保持 `spring.ai.mcp.client.initialized=false`，确保主程序启动不被 MCP 影响。
- 当 `mcp-servers` jar 构建成功后：
  - 在后台线程将 Spring AI MCP client 的 `initialized` 切为 `true`
  - 主动触发 MCP client 的相关 bean 初始化（一次性触发即可）
  - MCP client 将根据 `spring.ai.mcp.client.stdio.servers-configuration=classpath:mcp-servers.json` 以 stdio 方式拉起 `mcp-servers` 进程
- 若 stdio 启动失败或 jar 不存在：记录日志，业务侧继续降级。

### 3) Lifecycle
- 本方案不直接管理 stdio 子进程的 PID（由 MCP client/transport 管理）。
- 主程序只负责：构建 + 触发初始化；不做复杂守护；可提供简单的定时重试（可选，默认关闭）。

## Observability
- 启动日志输出：
  - build command、build output path、触发初始化结果/失败原因。
- 若 MCP 不可用：业务侧继续降级，不把异常扩散到主流程。

## Notes
- `mcp-servers.json` 内对 jar 的相对路径依赖主程序的工作目录；推荐使用本仓库根目录启动（或后续把 jar 路径改为可配置的绝对路径）。


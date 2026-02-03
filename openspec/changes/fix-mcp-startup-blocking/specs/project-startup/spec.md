# project-startup Delta Specification

## MODIFIED Requirements

### Requirement: Optional MCP server autostart (non-blocking)
系统 SHALL 支持在主程序启动后**异步、非阻塞**地自动拉起本地 `mcp-servers`（stdio），默认无需用户手动启动；当 MCP 不可用时系统 SHALL 保持可运行并降级相关能力。

此外，系统 SHALL 确保 Spring 上下文启动期不会因 MCP 工具发现（例如 `listTools()`）的阻塞/超时而失败；MCP 工具发现 SHOULD 仅在业务真正需要 MCP 工具时按需发生。

#### Scenario: ApplicationContext starts even when MCP servers are not ready
- **GIVEN** `spring.ai.mcp.client.enabled=true`（或默认启用）
- **AND** MCP server 尚未构建/未启动/不可达（例如缺少 jar、`npx` 尚未安装或网络较慢）
- **WHEN** 主程序启动并刷新 Spring ApplicationContext
- **THEN** 主程序启动过程 SHALL 不因 MCP tool discovery 的超时而失败
- **AND** 主程序 HTTP 服务可用

#### Scenario: MCP tool discovery happens on-demand and can degrade
- **GIVEN** 主程序已启动
- **AND** MCP tool discovery 在首次使用时触发（例如业务请求进入 MCP 工具模式）
- **WHEN** MCP server 不可达或 `listTools()` 超时
- **THEN** 系统返回可读错误并降级（例如回退到非 MCP 模式）
- **AND** 不影响主程序继续对外服务

#### Scenario: Timeout is configurable for slow cold-start
- **GIVEN** MCP server 冷启动可能需要较长时间（例如首次 `npx -y` 安装或本地构建）
- **WHEN** 开发者配置 `spring.ai.mcp.client.request-timeout` 为更大的值
- **THEN** MCP 请求超时阈值按配置生效
- **AND** 主程序启动仍不被阻塞

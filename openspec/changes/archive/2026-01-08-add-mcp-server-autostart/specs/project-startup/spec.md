## ADDED Requirements

### Requirement: Optional MCP server autostart (non-blocking)
系统 SHALL 支持在主程序启动后自动拉起本地 `mcp-servers`，以便开发者无需手动启动 MCP server 即可使用 MCP 工具；该能力 SHALL 不阻塞主程序启动，当 MCP 不可用时系统 SHALL 保持可运行并降级相关能力。

#### Scenario: Main app starts and MCP autostart succeeds
- **GIVEN** 开发者仅启动主程序
- **AND** 本地具备 Maven/Java 环境且 `mcp-servers` 可构建
- **WHEN** 主程序启动完成
- **THEN** 系统在后台构建并启动 `mcp-servers`
- **AND** 不影响主程序对外提供 HTTP 服务

#### Scenario: MCP autostart fails and app degrades gracefully
- **GIVEN** 开发者仅启动主程序
- **AND** Maven 不可用或 `mcp-servers` 构建失败或端口不可用
- **WHEN** 主程序启动完成
- **THEN** 主程序仍可正常启动并提供核心功能
- **AND** MCP 相关能力降级并输出可读日志

#### Scenario: Autostart can be disabled
- **GIVEN** 配置 `app.mcp.autostart=false`
- **WHEN** 主程序启动
- **THEN** 系统不尝试构建或启动 `mcp-servers`

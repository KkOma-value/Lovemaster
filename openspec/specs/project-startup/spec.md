# project-startup Specification

## Purpose
TBD - created by archiving change fix-local-startup. Update Purpose after archive.
## Requirements
### Requirement: Local build prerequisites
The system SHALL document and require Java 21+ for building and running the application.

#### Scenario: Developer uses unsupported Java
- **WHEN** a developer runs the project with Java < 21
- **THEN** the startup script SHALL fail fast with a clear message indicating Java 21+ is required

### Requirement: Local configuration template
The repository SHALL provide a checked-in local configuration template for required runtime settings.

#### Scenario: Fresh clone local setup
- **WHEN** a developer clones the repository
- **THEN** the repository SHALL include `application-local.yml.example` with placeholders for required keys

### Requirement: No committed secrets
The repository SHALL NOT include hard-coded credentials or API keys in tracked configuration files.

#### Scenario: Secrets are required for runtime
- **WHEN** runtime credentials/API keys are needed
- **THEN** configuration SHALL reference environment variables or local-only config not committed to VCS

### Requirement: Project compiles cleanly
The project SHALL compile successfully with the documented prerequisite JDK.

#### Scenario: Maven compile
- **WHEN** a developer runs `mvn clean compile` under Java 21
- **THEN** compilation SHALL succeed without syntax errors

### Requirement: Optional MCP server autostart (non-blocking)
系统 SHALL 支持在主程序启动后**异步、非阻塞**地自动拉起本地 `mcp-servers`（stdio），默认无需用户手动启动；当 MCP 不可用时系统 SHALL 保持可运行并降级相关能力。

#### Scenario: Main app starts and MCP autostart succeeds
- **GIVEN** 开发者仅启动主程序且已启用 autostart
- **AND** 本地具备 Maven/Java 环境且 `mcp-servers` 可构建
- **WHEN** 主程序完成启动
- **THEN** 系统在后台异步构建并启动 `mcp-servers`
- **AND** 主程序 HTTP 服务不被阻塞，日志输出构建/启动结果

#### Scenario: MCP autostart fails and app degrades gracefully
- **GIVEN** 开发者仅启动主程序且已启用 autostart
- **AND** Maven 不可用或 `mcp-servers` 构建失败或进程启动失败
- **WHEN** 主程序完成启动
- **THEN** 主程序仍可正常对外提供核心功能
- **AND** MCP 相关能力降级并输出可读日志，autostart 失败不阻塞启动流程

#### Scenario: Autostart can be disabled
- **GIVEN** 配置 `app.mcp.autostart=false`
- **WHEN** 主程序启动
- **THEN** 系统不尝试构建或启动 `mcp-servers`

### Requirement: Local documents autoload after startup
系统 SHALL 在主程序启动后自动尝试加载/向量化本地文档，使默认情况下无需手动操作即可完成文档准备；该流程 SHALL 不阻塞主程序并在依赖就绪后可重试一次。

#### Scenario: Startup triggers document autoload
- **GIVEN** 开发者仅启动主程序且本地文档路径可用
- **WHEN** 主程序启动完成（无论 MCP 是否异步自启）
- **THEN** 系统自动触发一次本地文档加载与向量化
- **AND** 无需额外手工命令即可让文档对聊天/RAG 可用

#### Scenario: Autoload retries after dependencies become ready
- **GIVEN** 初次文档加载因 MCP/向量存储不可用而失败
- **WHEN** MCP/依赖变为可用（或经过配置的短暂延迟）
- **THEN** 系统在后台再尝试一次加载/向量化
- **AND** 若仍失败则记录可读日志但不影响主程序对外服务

#### Scenario: Autoload can be disabled
- **GIVEN** 配置关闭文档自动加载
- **WHEN** 主程序启动
- **THEN** 系统不触发文档加载/向量化流程


# Design: fix-mcp-startup-blocking

## Problem Summary
Spring Boot 启动期 `ApplicationContext#refresh()` 会 eager 创建 singleton beans。Spring AI MCP Client autoconfigure 中的 `toolCallbacksDeprecated` bean 在创建时同步执行 `listTools()`，当 MCP server 尚未就绪、外部 `npx` 安装缓慢或命令不可用时，会触发 20s 超时并使整个应用启动失败。

## Goals
- 主程序启动必须成功，即便 MCP server 未构建/未启动/不可达。
- MCP 能力在后台自启完成后可被按需使用；失败时可降级，不影响核心 HTTP 服务。
- 改动最小、范围可控，不引入全局 lazy-init。

## Key Observations
- `toolCallbacks`（provider）bean 的创建本身不会触发 `listTools()`，但 `toolCallbacksDeprecated` 会。
- `spring.ai.mcp.client.initialized` 仅影响 client `initialize()` 调用，不会阻止 deprecated bean 在创建时触发工具发现。
- 因为 `McpClientAutoConfiguration` 对 `enabled` 的 `matchIfMissing=true`，若未显式设为 `false`，自动装配默认启用；因此项目层面必须对启动期副作用做隔离。

## Proposed Approach (default)
### 1) Disable eager creation of deprecated tool-callback beans
在应用侧新增一个 `BeanFactoryPostProcessor`（或 `BeanDefinitionRegistryPostProcessor`），在 BeanDefinition 注册后将以下 bean 的 `lazyInit` 设为 `true`（或直接移除定义）：
- `toolCallbacksDeprecated`
- `asyncToolCallbacksDeprecated`

这样在容器启动期不会实例化这些 bean，从而避免启动期同步 `listTools()`。

### 2) Keep provider-based tool callbacks for runtime usage
业务侧继续使用 `ToolCallbackProvider`（例如 bean `toolCallbacks`）。它可以在实际执行工具调用前才进行工具发现；如果发现失败，业务可捕获异常并降级（现有 LoveApp 已有 try/catch 降级逻辑）。

### 3) Make MCP request timeout configurable (and choose safer defaults)
使用 `spring.ai.mcp.client.request-timeout` 调整 MCP 请求超时，默认值建议 > 20s（例如 60s-120s），以覆盖首次启动的冷启动/安装成本。

## Alternatives Considered
- **Global** `spring.main.lazy-initialization=true`：能规避问题但影响面大，可能改变其他 bean 的启动行为，不建议。
- Exclude `McpClientAutoConfiguration`：能彻底避免启动期失败，但会失去 Spring AI MCP 的自动装配能力，且无法在运行时再“开启”。
- Fork/patch 上游：维护成本高，不符合本仓库的学习项目定位。

## Risks
- 若有第三方或未来代码显式依赖 `toolCallbacksDeprecated`，在首次访问时仍可能触发 `listTools()` 并抛错；但这将不再导致应用启动失败。
- 仅设置 `lazyInit` 不会改变 provider 在运行期的超时行为；需要通过 timeout 配置/拆分 MCP servers 配置来降低不确定性。

## Telemetry / Diagnostics
- 启动日志中应明确记录：deprecated MCP tool callbacks 被延迟初始化（或被禁用），以及当前 MCP request-timeout 值。
- 当运行期 MCP 调用失败，应输出：连接方式（stdio/sse）、server 配置来源、超时阈值与错误摘要。

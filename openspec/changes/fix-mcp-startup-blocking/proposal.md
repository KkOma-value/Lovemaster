# Change: fix-mcp-startup-blocking

## Why
当前主程序在启动阶段会间歇/稳定失败，错误栈指向 Spring AI MCP Client 的自动装配在创建 bean `toolCallbacksDeprecated` 时同步执行了 MCP `listTools()`，并在 20s 内未得到响应而抛出 `TimeoutException`，导致 `ApplicationContext` 启动失败。

从反编译结果可确认：
- `org.springframework.ai.autoconfigure.mcp.client.McpClientAutoConfiguration#toolCallbacksDeprecated(...)` 会在 bean 创建阶段直接调用 `SyncMcpToolCallbackProvider.getToolCallbacks()`，进而触发 `McpSyncClient.listTools()` 的阻塞调用。
- 该 bean 默认是 eager singleton，因此即使业务代码只使用 `toolCallbacks`（provider 版本）也仍会在容器 refresh 时被实例化并触发超时。

这与现有 spec（project-startup: “Optional MCP server autostart (non-blocking)”）要求的“主程序启动不应因 MCP 不可用而失败”相冲突。

## What Changes
- 在应用侧引入一个**最小范围**的 Spring bean 定义后处理（`BeanFactoryPostProcessor` / `BeanDefinitionRegistryPostProcessor`），将 `toolCallbacksDeprecated`（以及对称的 `asyncToolCallbacksDeprecated`）标记为 `lazyInit=true` 或直接移除其 BeanDefinition，使其不在启动期被 eager 创建。
- 保持 `toolCallbacks`（`ToolCallbackProvider` 版本）可用：仅在业务真正需要 MCP 工具时才触发工具发现与连接；失败时由上层降级处理。
- 将 MCP 请求超时暴露为可配置项并在本项目默认配置中给出更合理的值（例如 `spring.ai.mcp.client.request-timeout`），避免首次启动时 `npx` 安装或本地 `mcp-servers` 冷启动导致不必要的超时。
- （可选，默认不做，需你确认）将 `mcp-servers.json` 拆分为“本地最小集”和“包含 npx 外部服务”的扩展集，默认使用最小集，避免隐式引入 `npx`/外部网络依赖。

## Non-Goals
- 不改 Spring AI MCP Client 依赖版本或 fork 上游实现。
- 不保证 MCP 工具在主程序启动完成的瞬间立即可用（按需初始化 + 可失败降级）。

## Impact
- Affected specs: `project-startup`（强化“启动不阻塞/不失败”的边界条件与场景）。
- Affected code: MCP 启动/装配相关（新增一个后处理类；可能调整默认配置的 timeout；可选拆分配置文件）。
- Compatibility: 仅影响 deprecated 的 `List<ToolCallback>` bean 的启动期行为；业务侧继续使用 `ToolCallbackProvider`。

## Open Questions (need your confirmation)
1. `mcp-servers.json` 里的 `amap-maps`（npx）是否希望默认启用？如果不需要，我建议默认改为最小集以降低启动不确定性。
2. 你期望 MCP 工具在主程序启动后多久内“必须可用”？（这会影响默认 `request-timeout` 与是否要做重试/健康探测）

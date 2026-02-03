## 1. Investigation
- [x] 1.1 确认主程序切换到 stdio 配置：`spring.ai.mcp.client.stdio.servers-configuration=classpath:mcp-servers.json`。
- [x] 1.2 确认在 `spring.ai.mcp.client.initialized=false` 情况下：在运行时把 initialized 切为 true 并触发一次 bean 初始化，后续请求能加载 MCP 工具。

## 2. Implementation (apply 阶段)
- [x] 2.1 新增主程序启动 bootstrap 组件：监听 `ApplicationReadyEvent`，异步触发"构建 + 启动"。
- [x] 2.2 异步构建 `mcp-servers`：调用 `mvn -pl mcp-servers -DskipTests package`，失败不阻塞。
- [x] 2.3 构建成功后触发 stdio MCP：把 MCP client 的 `initialized` 切为 true 并触发一次相关 bean 初始化。
- [x] 2.4 确保 stdio 配置启用（使用现有 `mcp-servers.json`），并透传 `APP_FILE_SAVE_DIR` 等环境变量（如需）。
- [x] 2.6 增加配置开关：允许禁用 autostart（例如 `app.mcp.autostart=false`）。

## 3. Validation
- [x] 3.1 单测（最小）：对"构建命令生成/禁用开关/触发初始化调用链（mock runner）"做单元测试（不真正启动 mcp-servers）。
- [ ] 3.2 手工验证：仅启动主程序，确认后台能拉起 mcp-servers；随后调用 Manus 触发 MCP 工具（若 MCP 不可用则应降级）。
- [x] 3.3 文档/脚本：补充 README 或 start 脚本说明（Maven 依赖、禁用开关、APP_FILE_SAVE_DIR）。
- [ ] 3.4 `openspec validate add-mcp-server-autostart --strict` 通过。

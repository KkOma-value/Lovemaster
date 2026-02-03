## 1. Investigation
- [x] 1.1 审查现有 MCP 自启实现与配置（包括 `spring.ai.mcp.client.*`、`app.mcp.autostart` 开关）及启动日志链路。
- [x] 1.2 确认本地文档加载/向量化流程的触发点与依赖（是否依赖 MCP、向量存储、环境变量）。

## 2. Implementation
- [x] 2.1 调整启动 bootstrap：在 ApplicationReady/Lifecycle 中异步触发 MCP 构建+stdio 自启，确保主流程不阻塞并保留禁用开关。
- [x] 2.2 为 MCP 自启增加可观察性（构建命令、产物路径、启动结果/失败原因），并确保失败不影响主程序。
- [x] 2.3 补充本地文档预加载触发：启动后尝试一次；必要时在 MCP/依赖 ready 后再重试一次；可配置开关/间隔。
- [x] 2.4 确保文档加载失败时的降级与日志清晰，不影响主程序对外服务。

## 3. Validation
- [x] 3.1 添加/更新单测覆盖：自启开关、非阻塞触发、日志/命令生成、文档加载触发与重试行为（使用 mock）。
- [ ] 3.2 手动验证：仅启动主程序即可异步拉起 MCP；无需手动操作即可完成本地文档加载；失败场景降级但主程序可用。
- [x] 3.3 运行 `openspec validate update-mcp-autostart-doc-load --strict` 确认变更通过。

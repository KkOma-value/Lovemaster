# Change: Async MCP autostart and local document preload

## Why
- 主程序当前仍需要开发者手动拉起 MCP，或存在启动顺序/阻塞的不确定性，违背"默认可用"预期。
- 本地文档在启动流程中未可靠预加载（或受 MCP 初始化时序影响），导致 RAG 工能需要额外手动操作。

## What Changes
- 主程序启动完成后，异步且非阻塞地构建并拉起 MCP（stdio），保证默认无需手动启动；失败只记录日志并继续运行。
- 在 MCP 启动/恢复后自动触发本地文档的加载与向量化（可重试），确保默认情况下 RAG 数据可用。
- 提供可关闭/跳过的开关和最小可观察性（关键日志、状态标记），避免生产或特殊环境误触发。

## Impact
- Affected specs: project-startup（MCP 自启动与本地文档预加载）
- Affected code: 主程序 bootstrap（ApplicationReady/Lifecycle）、MCP 客户端初始化流程、文档加载/向量化触发、配置与日志

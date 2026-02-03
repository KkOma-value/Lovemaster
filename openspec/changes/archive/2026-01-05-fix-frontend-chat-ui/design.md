# Design: fix-frontend-chat-ui

## Goals
- 前端可构建、可启动、两种聊天页面可用。
- 最小改动补齐缺失模块，并让接口地址与后端真实配置一致。

## Non-goals
- 不重做 UI/交互，不增加新的页面/功能。
- 不更改后端接口语义（仅对齐路径/端口配置）。

## Key Decisions

### 1) SSE 统一封装到 `src/services/api.js`
原因：目前组件已依赖 `createLoveAppSSE/createManusSSE`，但服务文件缺失。

设计要点：
- 使用原生 `EventSource`；在 `message` 事件中将 `event.data` 透传给组件。
- 对结束标记：后端 LoveApp 会发送 `[DONE]`；封装层检测到后主动 `close()` 并触发 `onComplete()`。
- 错误处理：监听 `error`，触发 `onError(err)`，并保证连接关闭避免泄漏。

### 2) 接口地址策略（需你确认）
推荐默认使用相对路径 `/api/ai/...`，并通过 Vite dev server proxy 将 `/api` 转发到后端 `http://localhost:8088`。

优点：
- 前端代码不硬编码端口；生产部署更接近同域模式。
- 本地开发减少 CORS 干扰。

备选：使用 `.env` 注入 base URL（例如 `VITE_API_BASE_URL=http://localhost:8088/api`），api.js 拼接时使用该值。

### 3) 主题切换修复
问题根因：`HomePage.vue` 里 `el-switch v-model` 已经直接改写注入的 `isDarkTheme`，再调用 `toggleTheme()` 会再次反转。

最小修复：
- 移除 `@change="toggleTheme"`，仅保留 `v-model`。
- 或将 `toggleTheme` 改为 setter（接收布尔值而不是 flip）。

## Compatibility Notes
- LoveApp SSE：`text/event-stream`（WebFlux），EventSource 兼容。
- Manus SSE：`SseEmitter`（Servlet），EventSource 也兼容，但事件分片可能不同；api.js 不做拼接假设，仅转发 data。

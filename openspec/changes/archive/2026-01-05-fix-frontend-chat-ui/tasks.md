# Tasks: fix-frontend-chat-ui

## 1. Implementation
- [x] 补齐缺失页面：新增 `springAI-front/src/views/LoveAppPage.vue`，页面结构与 `ManusPage.vue` 保持一致（返回首页按钮 + 内嵌 `LoveAppChat`）。
- [x] 补齐缺失服务层：新增 `springAI-front/src/services/api.js`，提供：
  - [x] `createLoveAppSSE(message, chatId, onData, onError, onComplete)`
  - [x] `createManusSSE(message, onData, onError, onComplete)`
  - [x] 统一处理 URL 拼接、参数编码、EventSource 事件监听与 close。
- [x] 修复主题开关：消除 `HomePage.vue` 中 `v-model` 与 `toggleTheme()` 的"双反转"问题。
- [x] 对齐/可配置后端地址：
  - [x] 选择并实现一种策略（Vite 代理 或 `.env` 可配置 base URL）
  - [x] 保持默认与后端 `application.yml` 一致（`http://localhost:8088/api`）。
- [x] 更新 `springAI-front/README.md`：修正后端端口/路径说明，补充"先启动后端、再启动前端"的联调步骤。

## 2. Validation
- [x] `springAI-front` 能执行 `npm install` / `npm run build` 无模块缺失错误。
- [ ] 本地联调：
  - [ ] 打开首页，主题开关可正常切换
  - [ ] LoveApp 页面可建立 SSE 连接并持续显示流式回复
  - [ ] Manus 页面可建立 SSE 连接并持续显示流式回复
- [ ] 浏览器控制台无持续报错（允许少量样式/无关 warning）。

# Change: fix-frontend-chat-ui

## Why
目前 `springAI-front/` 存在明显的缺失文件与引用不一致，导致前端无法正常构建/运行或功能表现异常；同时前端文档中的后端地址/端口与项目实际配置不一致，容易造成“页面能打开但聊天无输出/请求失败”。

## What Changes
- 补齐缺失文件：
  - 新增 `springAI-front/src/views/LoveAppPage.vue`（与 `ManusPage.vue` 对齐的页面壳）
  - 新增 `springAI-front/src/services/api.js`（集中管理 SSE 连接创建与关闭）
- 修复主题切换逻辑：避免 `v-model` 与 `@change` 双重反转造成开关无法切换。
- 对齐后端地址默认值与本地开发体验：
  - 将前端默认接口前缀与后端实际配置对齐（后端为 `http://localhost:8088/api`，接口为 `/api/ai/...`）
  - 可选：增加 Vite dev server 代理，使前端在开发时使用相对路径 `/api/...`，减少 CORS 与端口耦合。
- 更新前端 README：修正后端默认端口/地址说明，并补充本地联调步骤。

## Impact
- Affected code:
  - `springAI-front/src/router/index.js`
  - `springAI-front/src/views/HomePage.vue`
  - `springAI-front/src/views/*`
  - `springAI-front/src/components/LoveAppChat.vue`
  - `springAI-front/src/components/ManusChat.vue`
  - `springAI-front/vite.config.js`
  - `springAI-front/README.md`
- Affected backend assumptions:
  - `src/main/resources/application.yml` 设置 `server.port=8088` 且 `server.servlet.context-path=/api`
  - `src/main/java/.../controller/AiController.java` 路由前缀为 `/ai`，因此完整路径为 `/api/ai/...`

## Risks
- SSE 行为差异：LoveApp 使用 WebFlux `text/event-stream`，Manus 使用 `SseEmitter`，前端的 EventSource 处理需要兼容两者的事件分片/结束标记。
- 本地端口差异：如果你本地实际以 8123 或其他端口运行后端，需要明确“默认值”与“可配置”策略。

## Open Questions (need your confirmation before apply)
1. 你当初说的“输出有问题”更接近哪一种？
   - A) `npm run dev` 直接报错（模块/文件找不到）
   - B) 页面能开但主题切换无效
   - C) 页面能开但聊天无输出/一直转圈/报 CORS 或 404
2. 你本地后端最终希望跑在哪个地址？
   - 默认脚本提示是 `http://localhost:8088/api`，你是否想保持这个默认？
3. 对于开发联调，你更偏好哪种方式？
   - A) Vite 代理（前端只请求相对路径 `/api/...`）
   - B) 直接写绝对地址（例如 `http://localhost:8088/api`，可用 `.env` 配置）

# Change: Add background chat runs

## Why

当前聊天生成的生命周期绑定在 `ChatPage` 页面组件及其 SSE 连接上。用户一旦切换路由、返回首页，前端就会关闭连接并丢失进行中状态，导致长请求无法稳定后台运行。对于 Coach 这类可能持续数十秒的任务，这会直接破坏多会话使用体验。

## What Changes

- 为聊天生成引入显式的 `run` 概念，将“执行一次生成”和“订阅一次流”解耦
- 新增后端运行时状态与查询能力，支持识别 active runs、查询 run 状态，并为后续恢复机制提供基础
- 新增前端应用级聊天运行时层，使进行中请求不再依赖单个聊天页面是否挂载
- 保持本次变更不包含 UI/UX 视觉实现，不改动页面样式与交互设计细节

## Impact

- Affected specs: `frontend-chat-ui`, `chat-run-lifecycle`
- Affected code:
  - `springai-front-react/src/App.jsx`
  - `springai-front-react/src/pages/Chat/ChatPage.jsx`
  - `springai-front-react/src/hooks/*`
  - `src/main/java/org/example/springai_learn/controller/*`
  - `src/main/java/org/example/springai_learn/ai/orchestrator/*`
  - `src/main/java/org/example/springai_learn/ai/service/*`
  - `src/main/java/org/example/springai_learn/ChatMemory/*`

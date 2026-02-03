# Change: Refactor React chat UI for simplicity and robustness

## Why
当前 React 聊天页面包含重复的 SSE 处理逻辑、状态更新分散且容易产生边界问题（如会话未初始化、错误状态未完全清理）。这影响代码可维护性与运行稳定性。

## What Changes
- 精简并统一 SSE 连接创建与关闭流程，确保完成/错误时一致清理状态
- 规整 `ChatPage` 的状态更新逻辑，避免使用陈旧状态与重复设置
- 修正细节上的健壮性问题（例如删除会话后的选择逻辑、发送时无会话保护）
- 降低冗余日志与重复代码，提升可读性

## Impact
- Affected specs: frontend-chat-ui
- Affected code: springai-front-react/src/pages/Chat/ChatPage.jsx, springai-front-react/src/services/chatApi.js, springai-front-react/src/components/Sidebar/ChatSidebar.jsx

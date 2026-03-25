# Change: 增强恋爱教练面板 UI 体验

## Why
当前"恋爱教练"功能的右侧 Manus 面板存在以下用户体验问题：
1. **布局遮挡问题**：当右侧终端面板展开时，底部消息输入框被面板遮挡，用户无法正常输入消息
2. **配色不一致**：终端面板使用深色主题 (#0d1117)，与聊天界面的粉色浅色主题严重不协调，视觉上突兀
3. **资源预览缺失**：智能体下载的图片等资源无法在"预览"标签页中正常显示，前后端通信链路存在问题

## What Changes

### 前端改动
- **MODIFIED** ChatPage 布局逻辑：聊天区域根据右侧面板开关状态自动调整宽度，确保消息输入框始终可见
- **MODIFIED** ManusPanel 配色：将终端面板从深色主题改为粉色浅色主题，与主聊天界面风格一致
- **MODIFIED** ManusPanel 预览渲染：增强 `file_created` 事件的处理和图片预览渲染逻辑

### 后端改动
- **MODIFIED** ToolCallAgent：确保 `file_created` 事件在资源下载成功后正确发送，包含可访问的 URL

## Impact
- **受影响的规范**: `frontend-chat-ui`
- **受影响的代码**:
  - `springai-front-react/src/pages/Chat/ChatPage.jsx` - 布局调整
  - `springai-front-react/src/components/ManusPanel/ManusPanel.jsx` - 组件渲染
  - `springai-front-react/src/components/ManusPanel/ManusPanel.module.css` - 样式主题
  - `springai-front-react/src/components/Chat/ChatArea.jsx` - 可能需要调整边距
  - `src/main/java/org/example/springai_learn/agent/ToolCallAgent.java` - 事件发送逻辑验证

## Dependencies
- 后端 FileController 已实现 `/api/files/{type}/{fileName}` 接口（已验证存在）
- 后端 ToolCallAgent 已实现 `file_created` 事件发送逻辑（已验证存在）

# Design: 增强恋爱教练面板 UI 体验

## Context
恋爱教练（coach）模式在 React 前端中包含一个右侧的 "Manus 电脑" 面板，用于显示任务执行状态、终端输出和文件预览。当前存在布局、配色和功能性问题需要解决。

### 约束
- 保持与现有 LoveApp 聊天界面的视觉一致性（粉色浅色主题）
- 不影响 LoveApp 模式（无 ManusPanel）的正常使用
- 后端 API 已存在，主要是前端适配和验证工作

## Goals / Non-Goals

### Goals
1. 解决面板展开时消息输入框被遮挡的布局问题
2. 统一终端面板与聊天界面的配色方案
3. 实现智能体下载资源的正确预览展示

### Non-Goals
- 不重构整体布局架构
- 不新增后端 API
- 不改变 ManusPanel 的功能逻辑

## Decisions

### 布局方案
**决定**：采用 Flex 自适应布局，ChatArea 宽度随 ManusPanel 展开/收起自动调整

**理由**：
- 避免固定宽度导致的布局冲突
- 保持响应式设计
- 最小化代码改动

**替代方案考虑**：
- 覆盖模式（Panel 覆盖在聊天区域上）：用户体验较差，无法同时查看聊天和任务状态
- 固定分屏比例：灵活性不足

### 配色方案
**决定**：统一使用粉色浅色主题，终端内容区改为浅粉底色配深色文字

**配色映射**：
| 元素 | 原深色值 | 新浅色值 |
|------|----------|----------|
| 终端背景 | #0d1117 | #FFF5F7 |
| 终端 header | #161b22 | #FFE4E6 |
| 边框 | #30363d | #FECDD3 |
| 命令文本 | #58a6ff | #BE123C |
| 输出文本 | #c9d1d9 | #374151 |
| 成功标记 | #7ee787 | #059669 |
| 错误文本 | #f85149 | #DC2626 |

**理由**：
- 与聊天界面完全一致
- 用户视觉体验流畅
- 终端仍保持足够的可读性和对比度

### 资源预览方案
**决定**：验证并修复现有事件链路，确保 `file_created` 事件正确传递和渲染

**事件流**：
```
ToolCallAgent.checkAndSendFileEvent()
    ↓
SseEmitter → JSON { type: "file_created", data: { type, name, path, url } }
    ↓
ChatPage.handlePanelMessage() → setPanelData({ files: [...] })
    ↓
ManusPanel.renderPreview() → <img src={file.url} />
```

**关键验证点**：
1. 后端 `currentEmitter` 是否正确设置
2. 文件是否实际存在于 `tmp/download/` 目录
3. 前端是否正确解析嵌套的 JSON data 字段
4. 图片 URL `/api/files/download/{fileName}` 是否可访问

## Risks / Trade-offs

### 终端可读性风险
- **风险**：浅色背景上的终端输出可能不如深色背景有"终端感"
- **缓解**：使用足够的颜色对比度，保留语法高亮差异化

### 图片加载失败风险
- **风险**：网络问题或文件不存在导致图片加载失败
- **缓解**：添加 fallback 占位图和错误提示

## Open Questions
- 是否需要为图片预览添加放大/全屏查看功能？（建议后续迭代）

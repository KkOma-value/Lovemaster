# Lovemaster Frontend Redesign Architecture

## 1. 整体架构
本重构主要集中在前端展示层（React/Vite）。保持现有后端的 Spring Boot + MCP Server 不变，前后端通信契约（REST API + SSE）不动。

## 2. 技术栈确认
- **框架**：React 18 + Vite
- **CSS 方案**：CSS Modules + 全局 Theme Variables (原生 CSS)。
- **动画库**：Framer Motion (用于状态切换和平滑过渡)。
- **图标**：Lucide React / Heroicons (不使用 Emoji，使用现代 SVG 图标)。
- **路由**：React Router (若需要)。

## 3. 组件树结构设计

```text
src/
 ├── components/
 │    ├── Layout/
 │    │    ├── AppLayout.jsx (承载动态模糊背景)
 │    │    └── Sidebar.jsx (历史记录、用户信息)
 │    ├── Home/
 │    │    ├── HeroGreetings.jsx (爱心图标、问候语)
 │    │    ├── QuickInputBox.jsx (图一的胶囊输入框)
 │    │    └── ContextPrompt.jsx (图二的功能引导)
 │    ├── Chat/
 │    │    ├── MessageList.jsx (对话流)
 │    │    ├── MessageBubble.jsx
 │    │    └── RichInputPanel.jsx (图二底部的大复合输入框)
 │    └── Common/
 │         ├── Avatar.jsx
 │         ├── Button.jsx
 │         └── IconButton.jsx
 ├── styles/
 │    ├── variables.css (定义调色板、圆角、阴影、中文字体簇)
 │    ├── global.css
 │    └── transitions.css
```

## 4. 样式与主题系统 (Design Tokens)
- `--color-bg-base`: `#fdfbf9` (参考图基础偏裸色/奶油色)
- `--color-glass`: `rgba(255, 255, 255, 0.6)`
- `--blur-radius`: `blur(40px)`
- `--color-text-primary`: `#332b27` (深褐/深灰黑)
- `--font-elegant`: `'Noto Serif SC', 'Songti SC', serif` (用于大标题的主标字体)
- `--font-sans`: `'Inter', 'PingFang SC', sans-serif` (用于正文)

## 5. 组件拆分与复用策略
由于图一和图二是两种不同的首页引导状态，我们需要在 `Home` 视图中设计状态切换，或者根据用户的选择（如是否点击某个功能卡片）在 `QuickInputBox` 和 `RichInputPanel` 之间进行平滑过渡。

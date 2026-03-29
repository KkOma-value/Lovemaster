# Proposal: Frontend Redesign

## 目标
根据用户提供的两张参考图，全面升级 Lovemaster 的前端 React 界面，消除原先“丑到难以理解”的痛点，打造一个高品质、通透感（毛玻璃）、温和感、优雅感的现代 Web 应用。

## 变更范围
- **只涉及前端代码** `springai-front-react/`，不触碰后端 API 逻辑。
- 重修 `App.css` / `index.css` 主题变量。
- 替换现有粗糙的 `Layout`, `Sidebar`, `ChatArea`, `MessageInput` 视觉实现。

## 设计原则
1. **无硬边框**：全面弃用 `border: 1px solid xxx`，使用 `box-shadow` 和 `opacity` 实现层级分离。
2. **极简大圆角**：采用 `9999px` 的胶囊圆角和 `24px` 的卡片圆角。
3. **沉浸式背景**：底层添加大面积的模糊情感图片，上层 UI 全局套用 `backdrop-filter: blur`。

## 分步计划
参见 `tasks.md` 中的 5 个阶段路线图。首要任务是确立颜色词典与基础组件，然后自外向内完成重构。

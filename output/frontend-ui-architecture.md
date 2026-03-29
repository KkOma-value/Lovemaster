# 前端改造技术架构设计

## 1. 动效实现方案选型
本项目前端建立在 React 基础上。对于需要实现的动效（Hover 状态与 Stagger 入场动效），有以下技术选项：
1. **纯 CSS 方案**：对于 hover，使用 `transform: translateY(-8px)` 和 `box-shadow` 配合 `transition: all 0.3s ease`。对于交错动效，可通过设定 `:nth-child` 选择器指定不同的 `animation-delay`。
2. **Framer Motion 方案**（推荐）：因为先前的需求与现有的规范（从以往架构可见）已经引入了 Framer Motion 库，实现复杂 Stagger（交错）和 scale/opacity入场 会更加优雅且可维护。

**架构决议**：
- **入场动效**：使用 `framer-motion` 的 `variants` 和 `staggerChildren` 来实现 0.05s 延迟的网格入场。
- **Hover 动效**：可以直接利用 Tailwind (或项目中现有的 CSS/Framer motion `whileHover`) 完成上移 8px 与阴影加深的设计。

## 2. 组件改造策略
### 2.1 Navbar/Dropdown
- 定位对应组件的下拉层容器（如 `UserDropdown.jsx` 或类似 NavBar 下的菜单层）。
- 缩减其 `width` 参数（例如由 w-64 改为 w-48），缩减内边距 `p` 与字体 `text-sm`。

### 2.2 首页 Grid/卡片布局
- **文本层提炼**：去除硬编码的长文案，替换成配置化的简洁对象（图二文字）。
- **卡片组件封装**：把提取出复用的内容包上 `motion.div`（如果使用 Framer motion）或挂载对应的 CSS animation class 保持动画统一样式。

## 3. 风险与回退
- **交互冲突风险**：如果部分卡片身上本已有点击或拖拽等冲突的交互态，需确保 CSS transform 或 `whileHover` 的 `scale`/`y` 参数不打破原有操作预期。
